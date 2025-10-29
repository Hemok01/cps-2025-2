"""
Lecture Session Views
"""
from rest_framework import generics, status
from rest_framework.response import Response
from rest_framework.permissions import IsAuthenticated
from rest_framework.views import APIView
from django.shortcuts import get_object_or_404
from django.utils import timezone
from django.db.models import Q

from apps.lectures.models import Lecture
from apps.tasks.models import Subtask
from .models import LectureSession, SessionParticipant, SessionStepControl
from .serializers import (
    LectureSessionSerializer,
    LectureSessionCreateSerializer,
    SessionParticipantSerializer
)


class SessionCreateView(generics.CreateAPIView):
    """강의방 생성 (강사 전용)"""
    serializer_class = LectureSessionCreateSerializer
    permission_classes = [IsAuthenticated]

    def create(self, request, *args, **kwargs):
        lecture_id = kwargs.get('lecture_pk')
        lecture = get_object_or_404(Lecture, pk=lecture_id)
        
        # 강사 권한 확인
        if lecture.instructor != request.user:
            return Response(
                {'error': '강의 강사만 세션을 생성할 수 있습니다.'},
                status=status.HTTP_403_FORBIDDEN
            )
        
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        session = serializer.save(lecture=lecture, instructor=request.user)
        
        return Response(
            LectureSessionSerializer(session).data,
            status=status.HTTP_201_CREATED
        )


class SessionByCodeView(APIView):
    """QR 코드로 세션 조회 (학생용)"""
    permission_classes = [IsAuthenticated]

    def get(self, request, session_code):
        session = get_object_or_404(
            LectureSession,
            session_code=session_code,
            status__in=['WAITING', 'IN_PROGRESS', 'REVIEW_MODE']
        )
        return Response(LectureSessionSerializer(session).data)


class SessionJoinView(APIView):
    """세션 입장 (학생용)"""
    permission_classes = [IsAuthenticated]

    def post(self, request, session_id):
        session = get_object_or_404(LectureSession, pk=session_id)
        
        # 이미 참가 중인지 확인
        participant, created = SessionParticipant.objects.get_or_create(
            session=session,
            user=request.user,
            defaults={'status': 'WAITING'}
        )
        
        if not created:
            return Response(
                {'message': '이미 참가 중인 세션입니다.'},
                status=status.HTTP_400_BAD_REQUEST
            )
        
        return Response(
            {
                'participant_id': participant.id,
                'session_id': session.id,
                'user_id': request.user.id,
                'status': participant.status,
                'joined_at': participant.joined_at,
                'message': '대기실에 입장했습니다. 강사가 수업을 시작할 때까지 기다려주세요.'
            },
            status=status.HTTP_201_CREATED
        )


class SessionParticipantsView(APIView):
    """참가자 목록 조회 (강사용)"""
    permission_classes = [IsAuthenticated]

    def get(self, request, session_id):
        session = get_object_or_404(LectureSession, pk=session_id)
        
        # 강사 권한 확인
        if session.instructor != request.user:
            return Response(
                {'error': '강사만 참가자 목록을 조회할 수 있습니다.'},
                status=status.HTTP_403_FORBIDDEN
            )
        
        participants = session.participants.select_related('user').all()
        serializer = SessionParticipantSerializer(participants, many=True)
        
        return Response({
            'session_id': session.id,
            'participants': serializer.data,
            'total_count': participants.count()
        })


class SessionStartView(APIView):
    """강의 시작 (강사 전용)"""
    permission_classes = [IsAuthenticated]

    def post(self, request, session_id):
        session = get_object_or_404(LectureSession, pk=session_id)
        
        # 강사 권한 확인
        if session.instructor != request.user:
            return Response(
                {'error': '강사만 강의를 시작할 수 있습니다.'},
                status=status.HTTP_403_FORBIDDEN
            )
        
        if session.status != 'WAITING':
            return Response(
                {'error': '이미 시작된 세션입니다.'},
                status=status.HTTP_400_BAD_REQUEST
            )
        
        first_subtask_id = request.data.get('first_subtask_id')
        message = request.data.get('message', '')
        
        if not first_subtask_id:
            return Response(
                {'error': 'first_subtask_id가 필요합니다.'},
                status=status.HTTP_400_BAD_REQUEST
            )
        
        first_subtask = get_object_or_404(Subtask, pk=first_subtask_id)
        
        # 세션 상태 업데이트
        session.status = 'IN_PROGRESS'
        session.started_at = timezone.now()
        session.current_subtask = first_subtask
        session.save()
        
        # 모든 대기 중인 참가자를 활성화
        session.participants.filter(status='WAITING').update(
            status='ACTIVE',
            current_subtask=first_subtask
        )
        
        # 제어 기록 생성
        SessionStepControl.objects.create(
            session=session,
            subtask=first_subtask,
            instructor=request.user,
            action='START_STEP',
            message=message
        )
        
        return Response({
            'session_id': session.id,
            'status': session.status,
            'started_at': session.started_at,
            'current_subtask': {
                'id': first_subtask.id,
                'title': first_subtask.title
            },
            'active_participants': session.participants.filter(status='ACTIVE').count(),
            'message': '수업이 시작되었습니다'
        })


class SessionNextStepView(APIView):
    """다음 단계로 진행 (강사 전용)"""
    permission_classes = [IsAuthenticated]

    def post(self, request, session_id):
        session = get_object_or_404(LectureSession, pk=session_id)
        
        # 강사 권한 확인
        if session.instructor != request.user:
            return Response(
                {'error': '강사만 단계를 진행할 수 있습니다.'},
                status=status.HTTP_403_FORBIDDEN
            )
        
        if session.status != 'IN_PROGRESS':
            return Response(
                {'error': '진행 중인 세션만 단계를 변경할 수 있습니다.'},
                status=status.HTTP_400_BAD_REQUEST
            )
        
        next_subtask_id = request.data.get('next_subtask_id')
        message = request.data.get('message', '')
        
        if not next_subtask_id:
            return Response(
                {'error': 'next_subtask_id가 필요합니다.'},
                status=status.HTTP_400_BAD_REQUEST
            )
        
        next_subtask = get_object_or_404(Subtask, pk=next_subtask_id)
        previous_subtask = session.current_subtask
        
        # 세션의 현재 단계 업데이트
        session.current_subtask = next_subtask
        session.save()
        
        # 모든 활성 참가자 동기화
        session.participants.filter(status='ACTIVE').update(
            current_subtask=next_subtask
        )
        
        # 제어 기록 생성
        SessionStepControl.objects.create(
            session=session,
            subtask=next_subtask,
            instructor=request.user,
            action='START_STEP',
            message=message
        )
        
        return Response({
            'session_id': session.id,
            'previous_subtask': {
                'id': previous_subtask.id if previous_subtask else None,
                'title': previous_subtask.title if previous_subtask else None
            },
            'current_subtask': {
                'id': next_subtask.id,
                'title': next_subtask.title
            },
            'timestamp': timezone.now()
        })


class SessionPauseView(APIView):
    """강의 일시 정지 (강사 전용)"""
    permission_classes = [IsAuthenticated]

    def post(self, request, session_id):
        session = get_object_or_404(LectureSession, pk=session_id)
        
        if session.instructor != request.user:
            return Response(
                {'error': '강사만 강의를 일시 정지할 수 있습니다.'},
                status=status.HTTP_403_FORBIDDEN
            )
        
        message = request.data.get('message', '')
        
        # 제어 기록 생성
        if session.current_subtask:
            SessionStepControl.objects.create(
                session=session,
                subtask=session.current_subtask,
                instructor=request.user,
                action='PAUSE',
                message=message
            )
        
        return Response({
            'session_id': session.id,
            'action': 'PAUSE',
            'message': '수업이 일시 정지되었습니다',
            'timestamp': timezone.now()
        })


class SessionResumeView(APIView):
    """강의 재개 (강사 전용)"""
    permission_classes = [IsAuthenticated]

    def post(self, request, session_id):
        session = get_object_or_404(LectureSession, pk=session_id)
        
        if session.instructor != request.user:
            return Response(
                {'error': '강사만 강의를 재개할 수 있습니다.'},
                status=status.HTTP_403_FORBIDDEN
            )
        
        message = request.data.get('message', '')
        
        # 제어 기록 생성
        if session.current_subtask:
            SessionStepControl.objects.create(
                session=session,
                subtask=session.current_subtask,
                instructor=request.user,
                action='RESUME',
                message=message
            )
        
        return Response({
            'session_id': session.id,
            'action': 'RESUME',
            'current_subtask': {
                'id': session.current_subtask.id if session.current_subtask else None,
                'title': session.current_subtask.title if session.current_subtask else None
            },
            'timestamp': timezone.now()
        })


class SessionEndView(APIView):
    """강의 종료 (강사 전용)"""
    permission_classes = [IsAuthenticated]

    def post(self, request, session_id):
        session = get_object_or_404(LectureSession, pk=session_id)
        
        if session.instructor != request.user:
            return Response(
                {'error': '강사만 강의를 종료할 수 있습니다.'},
                status=status.HTTP_403_FORBIDDEN
            )
        
        message = request.data.get('message', '')
        
        # 세션 종료
        session.status = 'REVIEW_MODE'
        session.ended_at = timezone.now()
        session.save()
        
        # 통계 계산
        total_participants = session.participants.count()
        completed_participants = session.participants.filter(status='COMPLETED').count()
        duration_minutes = (session.ended_at - session.started_at).total_seconds() / 60 if session.started_at else 0
        
        return Response({
            'session_id': session.id,
            'status': session.status,
            'ended_at': session.ended_at,
            'duration_minutes': int(duration_minutes),
            'completed_participants': completed_participants,
            'total_participants': total_participants,
            'message': '수업이 종료되었습니다'
        })


class SessionCurrentView(APIView):
    """현재 세션 상태 조회"""
    permission_classes = [IsAuthenticated]

    def get(self, request, session_id):
        session = get_object_or_404(LectureSession, pk=session_id)
        return Response(LectureSessionSerializer(session).data)


class MyActiveSessionView(APIView):
    """내가 참가 중인 활성 세션 조회 (학생용)"""
    permission_classes = [IsAuthenticated]

    def get(self, request):
        participation = SessionParticipant.objects.filter(
            user=request.user,
            status__in=['WAITING', 'ACTIVE'],
            session__status__in=['WAITING', 'IN_PROGRESS']
        ).select_related('session', 'session__lecture', 'current_subtask').first()
        
        if not participation:
            return Response({
                'active_session': None,
                'message': '참가 중인 활성 세션이 없습니다'
            }, status=status.HTTP_404_NOT_FOUND)
        
        session = participation.session
        return Response({
            'active_session': {
                'session_id': session.id,
                'session_code': session.session_code,
                'lecture': {
                    'id': session.lecture.id,
                    'title': session.lecture.title
                },
                'status': session.status,
                'current_subtask': {
                    'id': participation.current_subtask.id if participation.current_subtask else None,
                    'title': participation.current_subtask.title if participation.current_subtask else None
                } if participation.current_subtask else None,
                'my_status': participation.status,
                'joined_at': participation.joined_at
            }
        })
