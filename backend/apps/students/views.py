"""
Student Views
수강생용 API Views
"""
from rest_framework import viewsets, status
from rest_framework.decorators import action
from rest_framework.response import Response
from rest_framework.permissions import IsAuthenticated
from django.shortcuts import get_object_or_404
from django.utils import timezone

from apps.lectures.models import Lecture, UserLectureEnrollment
from apps.sessions.models import LectureSession, SessionParticipant
from .serializers import (
    LectureListSerializer,
    SessionJoinSerializer,
    MySessionSerializer,
    SessionParticipantSerializer
)


class StudentLectureViewSet(viewsets.ReadOnlyModelViewSet):
    """
    수강생용 강의 ViewSet
    - 강의 목록 조회
    - 수강 신청/취소
    """
    permission_classes = [IsAuthenticated]
    serializer_class = LectureListSerializer

    def get_queryset(self):
        """활성화된 강의만 조회"""
        return Lecture.objects.filter(is_active=True).select_related('instructor')

    @action(detail=True, methods=['post'])
    def enroll(self, request, pk=None):
        """수강 신청"""
        lecture = self.get_object()
        enrollment, created = UserLectureEnrollment.objects.get_or_create(
            user=request.user,
            lecture=lecture
        )

        if created:
            return Response({
                'message': '수강 신청이 완료되었습니다.',
                'enrollment_id': enrollment.id
            }, status=status.HTTP_201_CREATED)
        else:
            return Response({
                'message': '이미 수강 중인 강의입니다.',
                'enrollment_id': enrollment.id
            }, status=status.HTTP_200_OK)

    @action(detail=True, methods=['post'])
    def unenroll(self, request, pk=None):
        """수강 취소"""
        lecture = self.get_object()
        try:
            enrollment = UserLectureEnrollment.objects.get(
                user=request.user,
                lecture=lecture
            )
            enrollment.delete()
            return Response({
                'message': '수강 취소가 완료되었습니다.'
            }, status=status.HTTP_200_OK)
        except UserLectureEnrollment.DoesNotExist:
            return Response({
                'error': '수강 중이 아닌 강의입니다.'
            }, status=status.HTTP_400_BAD_REQUEST)

    @action(detail=False, methods=['get'])
    def my_lectures(self, request):
        """내 수강 강의 목록"""
        enrollments = UserLectureEnrollment.objects.filter(
            user=request.user
        ).select_related('lecture__instructor')

        lectures = [enrollment.lecture for enrollment in enrollments]
        serializer = self.get_serializer(lectures, many=True)
        return Response(serializer.data)


class StudentSessionViewSet(viewsets.GenericViewSet):
    """
    수강생용 세션 ViewSet
    - 세션 코드로 참가
    - 내 세션 목록 조회
    """
    permission_classes = [IsAuthenticated]
    serializer_class = MySessionSerializer

    @action(detail=False, methods=['post'])
    def join(self, request):
        """세션 코드로 참가"""
        serializer = SessionJoinSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        session_code = serializer.validated_data['session_code']
        session = get_object_or_404(LectureSession, session_code=session_code)

        # 참가자 생성 또는 조회
        participant, created = SessionParticipant.objects.get_or_create(
            session=session,
            user=request.user,
            defaults={'status': 'WAITING'}
        )

        # 이미 참가 중이면 상태 업데이트
        if not created:
            if participant.status == 'DISCONNECTED':
                participant.status = 'ACTIVE'
                participant.save()

        participant_serializer = SessionParticipantSerializer(participant)

        return Response({
            'message': '세션에 참가했습니다.' if created else '세션에 재참가했습니다.',
            'session': MySessionSerializer(session, context={'request': request}).data,
            'participant': participant_serializer.data
        }, status=status.HTTP_201_CREATED if created else status.HTTP_200_OK)

    @action(detail=False, methods=['get'])
    def my_sessions(self, request):
        """내가 참가한 세션 목록"""
        participants = SessionParticipant.objects.filter(
            user=request.user
        ).select_related(
            'session__lecture',
            'session__instructor',
            'current_subtask'
        ).order_by('-joined_at')

        sessions = [p.session for p in participants]
        serializer = self.get_serializer(sessions, many=True, context={'request': request})
        return Response(serializer.data)

    @action(detail=False, methods=['get'])
    def active_sessions(self, request):
        """진행 중인 세션 목록"""
        participants = SessionParticipant.objects.filter(
            user=request.user,
            session__status__in=['WAITING', 'IN_PROGRESS']
        ).select_related(
            'session__lecture',
            'session__instructor',
            'current_subtask'
        ).order_by('-last_active_at')

        sessions = [p.session for p in participants]
        serializer = self.get_serializer(sessions, many=True, context={'request': request})
        return Response(serializer.data)

    @action(detail=True, methods=['post'])
    def leave(self, request, pk=None):
        """세션 나가기"""
        session = get_object_or_404(LectureSession, pk=pk)

        try:
            participant = SessionParticipant.objects.get(
                session=session,
                user=request.user
            )
            participant.status = 'DISCONNECTED'
            participant.save()

            return Response({
                'message': '세션에서 나갔습니다.'
            }, status=status.HTTP_200_OK)
        except SessionParticipant.DoesNotExist:
            return Response({
                'error': '참가하지 않은 세션입니다.'
            }, status=status.HTTP_400_BAD_REQUEST)
