"""
Lecture Session Views
"""
from rest_framework import generics, status
from rest_framework.response import Response
from rest_framework.permissions import IsAuthenticated, AllowAny
from rest_framework.views import APIView
from django.shortcuts import get_object_or_404
from django.utils import timezone
from django.db.models import Q
from channels.layers import get_channel_layer
from asgiref.sync import async_to_sync

from apps.lectures.models import Lecture
from apps.tasks.models import Subtask
from .models import LectureSession, SessionParticipant, SessionStepControl
from .serializers import (
    LectureSessionSerializer,
    LectureSessionCreateSerializer,
    SessionParticipantSerializer
)


def broadcast_session_status(session_code: str, session_status: str, message: str = ''):
    """
    WebSocket을 통해 세션 상태 변경을 모든 참가자에게 브로드캐스트
    """
    import logging
    logger = logging.getLogger(__name__)

    try:
        channel_layer = get_channel_layer()
        if channel_layer is None:
            logger.error("Channel layer is None!")
            return

        group_name = f'session_{session_code}'
        logger.info(f"Broadcasting session_status_changed to {group_name}: {session_status}")

        async_to_sync(channel_layer.group_send)(
            group_name,
            {
                'type': 'session_status_changed',
                'status': session_status,
                'message': message
            }
        )
        logger.info(f"Broadcast successful to {group_name}")
    except Exception as e:
        logger.error(f"Broadcast failed: {e}", exc_info=True)


def broadcast_step_changed(session_code: str, subtask: dict):
    """
    WebSocket을 통해 단계 변경을 모든 참가자에게 브로드캐스트
    """
    import logging
    logger = logging.getLogger(__name__)

    try:
        channel_layer = get_channel_layer()
        if channel_layer is None:
            logger.error("Channel layer is None!")
            return

        group_name = f'session_{session_code}'
        logger.info(f"Broadcasting step_changed to {group_name}: {subtask.get('title', 'unknown')}")

        async_to_sync(channel_layer.group_send)(
            group_name,
            {
                'type': 'step_changed',
                'subtask': subtask
            }
        )
        logger.info(f"Step broadcast successful to {group_name}")
    except Exception as e:
        logger.error(f"Step broadcast failed: {e}", exc_info=True)


def broadcast_instructor_message(session_code: str, message: str, instructor_name: str = '강사'):
    """
    WebSocket을 통해 강사 메시지를 모든 참가자에게 브로드캐스트
    """
    import logging
    logger = logging.getLogger(__name__)

    try:
        channel_layer = get_channel_layer()
        if channel_layer is None:
            logger.error("Channel layer is None!")
            return

        group_name = f'session_{session_code}'
        logger.info(f"Broadcasting instructor_message to {group_name}: {message[:50]}...")

        async_to_sync(channel_layer.group_send)(
            group_name,
            {
                'type': 'instructor_message',
                'message': message,
                'from': instructor_name,
                'timestamp': timezone.now().isoformat()
            }
        )
        logger.info(f"Instructor message broadcast successful to {group_name}")
    except Exception as e:
        logger.error(f"Instructor message broadcast failed: {e}", exc_info=True)


def broadcast_student_completion(
    session_code: str,
    device_id: str,
    subtask_id: int,
    student_name: str = '',
    participant_id: int = None,
    completed_subtasks: list = None,
):
    """
    WebSocket을 통해 학생의 단계 완료를 강사에게 브로드캐스트
    """
    import logging
    logger = logging.getLogger(__name__)

    try:
        channel_layer = get_channel_layer()
        if channel_layer is None:
            logger.error("Channel layer is None!")
            return

        group_name = f'session_{session_code}'
        logger.info(f"Broadcasting student_completion to {group_name}: device={device_id}, subtask={subtask_id}")

        completed_list = completed_subtasks or []
        async_to_sync(channel_layer.group_send)(
            group_name,
            {
                'type': 'student_completion',
                'device_id': device_id,
                'participant_id': participant_id,
                'student_name': student_name,
                'subtask_id': subtask_id,
                'completed_subtasks': completed_list,
                'total_completed': len(completed_list),
                'timestamp': timezone.now().isoformat()
            }
        )
        logger.info(f"Student completion broadcast successful to {group_name}")
    except Exception as e:
        logger.error(f"Student completion broadcast failed: {e}", exc_info=True)


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

        # 이미 진행 중인 세션이면 현재 상태 반환
        if session.status == 'IN_PROGRESS':
            return Response({
                'session_id': session.id,
                'status': session.status,
                'started_at': session.started_at,
                'current_subtask': {
                    'id': session.current_subtask.id,
                    'title': session.current_subtask.title
                } if session.current_subtask else None,
                'active_participants': session.participants.filter(status='ACTIVE').count(),
                'message': '이미 진행 중인 수업입니다'
            })

        if session.status not in ['WAITING', 'CREATED']:
            return Response(
                {'error': '시작할 수 없는 세션 상태입니다.'},
                status=status.HTTP_400_BAD_REQUEST
            )

        first_subtask_id = request.data.get('first_subtask_id')
        message = request.data.get('message', '')

        # first_subtask_id가 없으면 강의의 첫 번째 subtask를 자동으로 찾기
        first_subtask = None
        if first_subtask_id:
            first_subtask = get_object_or_404(Subtask, pk=first_subtask_id)
        else:
            # 강의에 연결된 Task의 첫 번째 Subtask 찾기
            from apps.tasks.models import Task
            first_task = Task.objects.filter(lecture=session.lecture).order_by('order_index').first()
            if first_task:
                first_subtask = first_task.subtasks.order_by('order_index').first()

        # subtask가 없어도 세션은 시작 가능 (subtask 없이 진행)
        if not first_subtask:
            # subtask 없이 세션만 시작
            session.status = 'IN_PROGRESS'
            session.started_at = timezone.now()
            session.save()

            # 모든 대기 중인 참가자를 활성화
            session.participants.filter(status='WAITING').update(status='ACTIVE')

            # WebSocket 브로드캐스트 - 세션 시작 알림
            broadcast_session_status(
                session.session_code,
                'IN_PROGRESS',
                '수업이 시작되었습니다'
            )

            return Response({
                'session_id': session.id,
                'status': session.status,
                'started_at': session.started_at,
                'current_subtask': None,
                'active_participants': session.participants.filter(status='ACTIVE').count(),
                'message': '수업이 시작되었습니다 (단계 정보 없음)'
            })
        
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

        # WebSocket 브로드캐스트 - 세션 시작 + 첫 번째 단계 알림
        broadcast_session_status(
            session.session_code,
            'IN_PROGRESS',
            '수업이 시작되었습니다'
        )
        broadcast_step_changed(
            session.session_code,
            {
                'id': first_subtask.id,
                'title': first_subtask.title,
                'order_index': first_subtask.order_index,
                'target_action': first_subtask.target_action,
                'guide_text': first_subtask.guide_text,
                'voice_guide_text': first_subtask.voice_guide_text
            }
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

        # WebSocket 브로드캐스트 - 단계 변경 알림
        broadcast_step_changed(
            session.session_code,
            {
                'id': next_subtask.id,
                'title': next_subtask.title,
                'order_index': next_subtask.order_index,
                'target_action': next_subtask.target_action,
                'guide_text': next_subtask.guide_text,
                'voice_guide_text': next_subtask.voice_guide_text
            }
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

        if session.status != 'IN_PROGRESS':
            return Response(
                {'error': '진행 중인 세션만 일시 정지할 수 있습니다.'},
                status=status.HTTP_400_BAD_REQUEST
            )

        message = request.data.get('message', '')

        # 세션 상태 업데이트
        session.status = 'PAUSED'
        session.save()

        # 제어 기록 생성
        if session.current_subtask:
            SessionStepControl.objects.create(
                session=session,
                subtask=session.current_subtask,
                instructor=request.user,
                action='PAUSE',
                message=message
            )

        # WebSocket 브로드캐스트 - 일시 정지 알림
        broadcast_session_status(
            session.session_code,
            'PAUSED',
            '수업이 일시 정지되었습니다'
        )

        return Response({
            'session_id': session.id,
            'status': session.status,
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

        if session.status != 'PAUSED':
            return Response(
                {'error': '일시 정지된 세션만 재개할 수 있습니다.'},
                status=status.HTTP_400_BAD_REQUEST
            )

        message = request.data.get('message', '')

        # 세션 상태 업데이트
        session.status = 'IN_PROGRESS'
        session.save()

        # 제어 기록 생성
        if session.current_subtask:
            SessionStepControl.objects.create(
                session=session,
                subtask=session.current_subtask,
                instructor=request.user,
                action='RESUME',
                message=message
            )

        # WebSocket 브로드캐스트 - 재개 알림
        broadcast_session_status(
            session.session_code,
            'IN_PROGRESS',
            '수업이 재개되었습니다'
        )

        return Response({
            'session_id': session.id,
            'status': session.status,
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

        # WebSocket 브로드캐스트 - 종료 알림
        broadcast_session_status(
            session.session_code,
            'REVIEW_MODE',
            '수업이 종료되었습니다'
        )

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


class InstructorActiveSessionView(APIView):
    """강사가 진행 중인 활성 세션 조회"""
    permission_classes = [IsAuthenticated]

    def get(self, request):
        # 강사가 진행 중인 세션 조회 (WAITING 또는 IN_PROGRESS 상태)
        active_sessions = LectureSession.objects.filter(
            instructor=request.user,
            status__in=['WAITING', 'IN_PROGRESS']
        ).select_related('lecture', 'current_subtask').order_by('-created_at')

        if not active_sessions.exists():
            return Response({
                'active_sessions': [],
                'message': '진행 중인 세션이 없습니다'
            })

        sessions_data = []
        for session in active_sessions:
            sessions_data.append({
                'id': session.id,
                'title': session.title,
                'session_code': session.session_code,
                'status': session.status,
                'lecture': {
                    'id': session.lecture.id,
                    'title': session.lecture.title
                } if session.lecture else None,
                'current_subtask': {
                    'id': session.current_subtask.id,
                    'title': session.current_subtask.title
                } if session.current_subtask else None,
                'participant_count': session.participants.count(),
                'created_at': session.created_at,
                'started_at': session.started_at,
            })

        return Response({
            'active_sessions': sessions_data,
            'count': len(sessions_data)
        })


class AnonymousSessionJoinView(APIView):
    """
    익명 세션 참가 (학생 앱용)
    인증 없이 device_id와 name으로 세션에 참가합니다.
    """
    permission_classes = [AllowAny]

    def post(self, request):
        session_code = request.data.get('session_code')
        device_id = request.data.get('device_id')
        name = request.data.get('name', '익명')

        if not session_code:
            return Response(
                {'error': 'session_code가 필요합니다.'},
                status=status.HTTP_400_BAD_REQUEST
            )

        if not device_id:
            return Response(
                {'error': 'device_id가 필요합니다.'},
                status=status.HTTP_400_BAD_REQUEST
            )

        # 세션 조회
        try:
            session = LectureSession.objects.select_related(
                'lecture', 'instructor', 'current_subtask'
            ).get(
                session_code=session_code.upper(),
                status__in=['WAITING', 'IN_PROGRESS', 'REVIEW_MODE']
            )
        except LectureSession.DoesNotExist:
            return Response(
                {'error': '세션을 찾을 수 없거나 이미 종료되었습니다.'},
                status=status.HTTP_404_NOT_FOUND
            )

        # 이미 참가 중인지 확인 (device_id로)
        participant, created = SessionParticipant.objects.get_or_create(
            session=session,
            device_id=device_id,
            defaults={
                'display_name': name,
                'status': 'WAITING' if session.status == 'WAITING' else 'ACTIVE',
                'current_subtask': session.current_subtask
            }
        )

        if not created:
            # 이미 참가 중이면 정보 업데이트
            participant.display_name = name
            if session.status == 'IN_PROGRESS' and participant.status == 'WAITING':
                participant.status = 'ACTIVE'
                participant.current_subtask = session.current_subtask
            participant.save()

        # 현재 단계 정보
        current_subtask_data = None
        if session.current_subtask:
            current_subtask_data = {
                'id': session.current_subtask.id,
                'title': session.current_subtask.title,
                'description': session.current_subtask.description,
                'order_index': session.current_subtask.order_index
            }

        # 전체 서브태스크 목록 (진행도 표시용)
        subtasks = []
        if session.lecture:
            from apps.tasks.models import Task
            tasks = Task.objects.filter(lecture=session.lecture).prefetch_related('subtasks')
            for task in tasks:
                for subtask in task.subtasks.all().order_by('order_index'):
                    subtasks.append({
                        'id': subtask.id,
                        'title': subtask.title,
                        'order_index': subtask.order_index
                    })

        return Response({
            'participant_id': participant.id,
            'session': {
                'id': session.id,
                'session_code': session.session_code,
                'title': session.title,
                'status': session.status,
                'lecture': {
                    'id': session.lecture.id,
                    'title': session.lecture.title
                } if session.lecture else None,
                'instructor': {
                    'id': session.instructor.id,
                    'name': session.instructor.name
                } if session.instructor else None,
                'current_subtask': current_subtask_data,
                'currentSubtaskDetail': current_subtask_data,  # Android 앱 호환성
                'subtasks': subtasks,
                'total_steps': len(subtasks)
            },
            'my_status': participant.status,
            'joined_at': participant.joined_at,
            'message': '세션에 참가했습니다.' if created else '세션에 다시 연결되었습니다.'
        }, status=status.HTTP_201_CREATED if created else status.HTTP_200_OK)


class SessionBroadcastView(APIView):
    """
    전체 학생에게 메시지 브로드캐스트 (강사 전용)
    POST /api/sessions/{session_id}/broadcast/
    """
    permission_classes = [IsAuthenticated]

    def post(self, request, session_id):
        session = get_object_or_404(LectureSession, pk=session_id)

        # 강사 권한 확인
        if session.instructor != request.user:
            return Response(
                {'error': '강사만 메시지를 브로드캐스트할 수 있습니다.'},
                status=status.HTTP_403_FORBIDDEN
            )

        # 메시지 검증
        message = request.data.get('message', '').strip()
        if not message:
            return Response(
                {'error': '메시지 내용이 필요합니다.'},
                status=status.HTTP_400_BAD_REQUEST
            )

        if len(message) > 500:
            return Response(
                {'error': '메시지는 500자를 초과할 수 없습니다.'},
                status=status.HTTP_400_BAD_REQUEST
            )

        # WebSocket으로 브로드캐스트
        broadcast_instructor_message(
            session.session_code,
            message,
            request.user.name if hasattr(request.user, 'name') else '강사'
        )

        return Response({
            'session_id': session.id,
            'message': message,
            'broadcast_to': session.participants.filter(status__in=['WAITING', 'ACTIVE']).count(),
            'timestamp': timezone.now().isoformat(),
            'success': True
        })


class SessionSwitchLectureView(APIView):
    """
    세션의 강의 전환 (강사 전용)
    POST /api/sessions/{session_id}/switch-lecture/
    """
    permission_classes = [IsAuthenticated]

    def post(self, request, session_id):
        session = get_object_or_404(LectureSession, pk=session_id)

        # 강사 권한 확인
        if session.instructor != request.user:
            return Response(
                {'error': '강사만 강의를 전환할 수 있습니다.'},
                status=status.HTTP_403_FORBIDDEN
            )

        # 진행 중인 세션만 강의 전환 가능
        if session.status not in ['IN_PROGRESS', 'PAUSED']:
            return Response(
                {'error': '진행 중이거나 일시정지된 세션만 강의를 전환할 수 있습니다.'},
                status=status.HTTP_400_BAD_REQUEST
            )

        lecture_id = request.data.get('lecture_id')
        if not lecture_id:
            return Response(
                {'error': 'lecture_id가 필요합니다.'},
                status=status.HTTP_400_BAD_REQUEST
            )

        # 새 강의 조회 및 권한 확인
        new_lecture = get_object_or_404(Lecture, pk=lecture_id)
        if new_lecture.instructor != request.user:
            return Response(
                {'error': '본인의 강의만 전환할 수 있습니다.'},
                status=status.HTTP_403_FORBIDDEN
            )

        previous_lecture = session.lecture

        # 강의 변경
        session.lecture = new_lecture
        session.current_subtask = None  # 새 강의로 전환 시 현재 단계 초기화
        session.save()

        # 참가자들의 현재 단계도 초기화
        session.participants.filter(status__in=['WAITING', 'ACTIVE']).update(
            current_subtask=None
        )

        # WebSocket으로 강의 전환 알림 브로드캐스트
        broadcast_session_status(
            session.session_code,
            session.status,
            f'강의가 "{new_lecture.title}"(으)로 전환되었습니다'
        )

        return Response({
            'session_id': session.id,
            'previous_lecture': {
                'id': previous_lecture.id if previous_lecture else None,
                'title': previous_lecture.title if previous_lecture else None,
            },
            'new_lecture': {
                'id': new_lecture.id,
                'title': new_lecture.title,
            },
            'timestamp': timezone.now().isoformat(),
            'success': True
        })


class ReportCompletionView(APIView):
    """
    학생 단계 완료 보고 (익명 사용자용)
    POST /api/sessions/{session_id}/report-completion/

    Request Body:
    - device_id: 기기 ID (필수)
    - subtask_id: 완료한 단계 ID (필수)
    - is_completed: 완료 여부 (기본: true)
    - completed_at: 완료 시각 (ISO 8601, 선택)
    """
    permission_classes = [AllowAny]

    def post(self, request, session_id):
        import logging
        logger = logging.getLogger(__name__)

        device_id = request.data.get('device_id')
        subtask_id = request.data.get('subtask_id')
        is_completed = request.data.get('is_completed', True)
        completed_at_str = request.data.get('completed_at')

        # 필수 파라미터 검증
        if not device_id:
            return Response(
                {'success': False, 'message': 'device_id가 필요합니다.'},
                status=status.HTTP_400_BAD_REQUEST
            )

        if not subtask_id:
            return Response(
                {'success': False, 'message': 'subtask_id가 필요합니다.'},
                status=status.HTTP_400_BAD_REQUEST
            )

        # 세션 조회
        session = get_object_or_404(LectureSession, pk=session_id)

        # 참가자 조회
        try:
            participant = SessionParticipant.objects.get(
                session=session,
                device_id=device_id
            )
        except SessionParticipant.DoesNotExist:
            return Response(
                {'success': False, 'message': '세션 참가자를 찾을 수 없습니다.'},
                status=status.HTTP_404_NOT_FOUND
            )

        # Subtask 존재 확인
        try:
            subtask = Subtask.objects.get(pk=subtask_id)
        except Subtask.DoesNotExist:
            return Response(
                {'success': False, 'message': '단계를 찾을 수 없습니다.'},
                status=status.HTTP_404_NOT_FOUND
            )

        # 완료된 단계 목록 업데이트
        completed_list = participant.completed_subtasks or []

        if is_completed:
            # 완료 추가 (중복 방지)
            if subtask_id not in completed_list:
                completed_list.append(subtask_id)
                participant.completed_subtasks = completed_list
                participant.last_completed_at = timezone.now()
                participant.save()

                logger.info(f"Step completion recorded: device={device_id}, subtask={subtask_id}")

                # WebSocket으로 강사에게 브로드캐스트
                broadcast_student_completion(
                    session_code=session.session_code,
                    device_id=device_id,
                    subtask_id=subtask_id,
                    student_name=participant.display_name or participant.participant_name,
                    participant_id=participant.id,
                    completed_subtasks=completed_list,
                )
            else:
                logger.info(f"Step already completed: device={device_id}, subtask={subtask_id}")
        else:
            # 완료 취소
            if subtask_id in completed_list:
                completed_list.remove(subtask_id)
                participant.completed_subtasks = completed_list
                participant.save()
                logger.info(f"Step completion removed: device={device_id}, subtask={subtask_id}")

        return Response({
            'success': True,
            'message': '단계 완료 상태가 업데이트되었습니다.',
            'completed_subtasks': participant.completed_subtasks,
            'last_completed_at': participant.last_completed_at.isoformat() if participant.last_completed_at else None
        })


class SessionCompletionStatusView(APIView):
    """
    세션 참가자들의 완료 상태 조회 (강사용)
    GET /api/sessions/{session_id}/completion-status/
    """
    permission_classes = [IsAuthenticated]

    def get(self, request, session_id):
        session = get_object_or_404(LectureSession, pk=session_id)

        # 강사 권한 확인
        if session.instructor != request.user:
            return Response(
                {'error': '강사만 완료 상태를 조회할 수 있습니다.'},
                status=status.HTTP_403_FORBIDDEN
            )

        # 참가자 목록과 완료 상태
        participants = session.participants.all()

        completion_data = []
        for p in participants:
            completion_data.append({
                'device_id': p.device_id,
                'display_name': p.display_name or p.participant_name,
                'status': p.status,
                'completed_subtasks': p.completed_subtasks or [],
                'completed_count': len(p.completed_subtasks or []),
                'last_completed_at': p.last_completed_at.isoformat() if p.last_completed_at else None,
                'current_subtask_id': p.current_subtask.id if p.current_subtask else None
            })

        # 단계별 완료 통계
        from collections import Counter
        all_completed = []
        for p in participants:
            all_completed.extend(p.completed_subtasks or [])

        subtask_stats = dict(Counter(all_completed))

        return Response({
            'session_id': session.id,
            'total_participants': participants.count(),
            'participants': completion_data,
            'subtask_completion_stats': subtask_stats
        })
