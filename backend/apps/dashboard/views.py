"""
Dashboard Views (강사용 모니터링)
"""
from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework.permissions import IsAuthenticated
from django.shortcuts import get_object_or_404
from django.db.models import Count, Q, Avg

from apps.lectures.models import Lecture
from apps.progress.models import UserProgress
from apps.help.models import HelpRequest
from apps.sessions.models import SessionParticipant
from apps.progress.serializers import UserProgressSerializer
from apps.help.serializers import HelpRequestSerializer


class LectureStudentsView(APIView):
    """수강생 목록 및 진행률 (강사용)"""
    permission_classes = [IsAuthenticated]

    def get(self, request, lecture_id):
        lecture = get_object_or_404(Lecture, pk=lecture_id)
        
        # 강사 권한 확인
        if lecture.instructor != request.user:
            return Response(
                {'error': '강사만 수강생 목록을 조회할 수 있습니다.'},
                status=403
            )
        
        # 수강생 목록 및 진행 상태
        enrollments = lecture.enrollments.select_related('user').all()
        students = []
        
        for enrollment in enrollments:
            user = enrollment.user
            # 진행 상태 통계
            progress = UserProgress.objects.filter(
                user=user,
                subtask__task__lecture=lecture
            )
            
            total_subtasks = lecture.tasks.aggregate(
                count=Count('subtasks')
            )['count'] or 0
            
            completed_subtasks = progress.filter(status='COMPLETED').count()
            
            # 현재 진행 중인 subtask
            current_progress = progress.filter(status='IN_PROGRESS').first()
            
            # 도움 요청 횟수
            help_count = HelpRequest.objects.filter(
                user=user,
                subtask__task__lecture=lecture
            ).count()
            
            students.append({
                'user_id': user.id,
                'name': user.name,
                'email': user.email,
                'progress_rate': completed_subtasks / total_subtasks if total_subtasks > 0 else 0,
                'completed_subtasks': completed_subtasks,
                'total_subtasks': total_subtasks,
                'current_subtask': {
                    'id': current_progress.subtask.id if current_progress else None,
                    'title': current_progress.subtask.title if current_progress else None,
                    'status': current_progress.status if current_progress else None
                } if current_progress else None,
                'help_count': help_count,
                'last_activity': progress.order_by('-updated_at').first().updated_at if progress.exists() else None,
                'enrolled_at': enrollment.enrolled_at
            })
        
        return Response({
            'lecture_id': lecture_id,
            'students': students
        })


class PendingHelpRequestsView(APIView):
    """대기 중인 도움 요청 (실시간 알림용)"""
    permission_classes = [IsAuthenticated]

    def get(self, request):
        # 강사의 강의들
        lecture_ids = Lecture.objects.filter(instructor=request.user).values_list('id', flat=True)
        
        # 대기 중인 도움 요청
        pending_requests = HelpRequest.objects.filter(
            subtask__task__lecture_id__in=lecture_ids,
            status__in=['PENDING', 'ANALYZING']
        ).select_related('user', 'subtask').order_by('-created_at')
        
        serializer = HelpRequestSerializer(pending_requests, many=True)
        
        return Response({
            'pending_requests': serializer.data
        })


class LectureStatisticsView(APIView):
    """강의 통계 (차트용)"""
    permission_classes = [IsAuthenticated]

    def get(self, request, lecture_id):
        lecture = get_object_or_404(Lecture, pk=lecture_id)
        
        # 강사 권한 확인
        if lecture.instructor != request.user:
            return Response(
                {'error': '강사만 통계를 조회할 수 있습니다.'},
                status=403
            )
        
        # 전체 수강생 수
        total_students = lecture.enrollments.count()
        
        # 평균 진행률
        total_subtasks = lecture.tasks.aggregate(count=Count('subtasks'))['count'] or 0
        
        # 도움 요청 통계
        total_help_requests = HelpRequest.objects.filter(
            subtask__task__lecture=lecture
        ).count()
        
        # 어려운 단계 (도움 요청이 많은 순)
        common_difficulties = HelpRequest.objects.filter(
            subtask__task__lecture=lecture
        ).values('subtask_id', 'subtask__title').annotate(
            help_count=Count('id')
        ).order_by('-help_count')[:5]
        
        return Response({
            'lecture_id': lecture_id,
            'total_students': total_students,
            'total_help_requests': total_help_requests,
            'common_difficulties': list(common_difficulties)
        })


class SessionProgressStatsView(APIView):
    """
    세션별 진도 통계 (실시간 모니터링용)
    GET /api/dashboard/sessions/{session_id}/progress-stats/
    """
    permission_classes = [IsAuthenticated]

    def get(self, request, session_id):
        from apps.sessions.models import LectureSession
        from apps.tasks.models import Task, Subtask
        from django.utils import timezone
        from datetime import timedelta

        session = get_object_or_404(LectureSession, pk=session_id)

        # 강사 권한 확인
        if session.instructor != request.user:
            return Response(
                {'error': '강사만 세션 진도 통계를 조회할 수 있습니다.'},
                status=403
            )

        # 세션의 모든 참가자 조회
        participants = session.participants.select_related('current_subtask', 'user').all()
        total_students = participants.count()

        if total_students == 0:
            return Response({
                'session_id': session_id,
                'total_students': 0,
                'groups': [
                    {'name': '완료', 'count': 0, 'percentage': 0},
                    {'name': '진행중', 'count': 0, 'percentage': 0},
                    {'name': '지연', 'count': 0, 'percentage': 0},
                    {'name': '미시작', 'count': 0, 'percentage': 0},
                ],
                'progress_data': []
            })

        # 강의의 전체 서브태스크 수 계산
        total_subtasks = 0
        if session.lecture:
            total_subtasks = Subtask.objects.filter(
                task__lecture=session.lecture
            ).count()

        # 현재 세션의 현재 단계 order_index 가져오기
        current_session_step_index = 0
        if session.current_subtask:
            current_session_step_index = session.current_subtask.order_index

        # 참가자별 진도 상태 분류
        completed_count = 0
        in_progress_count = 0
        delayed_count = 0
        not_started_count = 0

        # 지연 판단 기준: 마지막 활동이 5분 이상 전
        delay_threshold = timezone.now() - timedelta(minutes=5)

        progress_data = []
        for participant in participants:
            # 참가자의 현재 단계
            participant_step_index = 0
            if participant.current_subtask:
                participant_step_index = participant.current_subtask.order_index

            # 상태 판단
            if participant.status == 'COMPLETED':
                status = 'completed'
                completed_count += 1
            elif participant.status == 'WAITING':
                status = 'not_started'
                not_started_count += 1
            elif participant.last_active_at and participant.last_active_at < delay_threshold:
                # 마지막 활동이 5분 이상 전이면 지연
                status = 'delayed'
                delayed_count += 1
            else:
                status = 'in_progress'
                in_progress_count += 1

            # 진행률 계산
            progress_percentage = 0
            if total_subtasks > 0:
                progress_percentage = int((participant_step_index / total_subtasks) * 100)

            progress_data.append({
                'user_id': participant.user.id if participant.user else None,
                'device_id': participant.device_id,
                'username': participant.display_name or (participant.user.name if participant.user else 'Anonymous'),
                'current_subtask': {
                    'id': participant.current_subtask.id if participant.current_subtask else None,
                    'title': participant.current_subtask.title if participant.current_subtask else None,
                    'order_index': participant.current_subtask.order_index if participant.current_subtask else 0,
                } if participant.current_subtask else None,
                'progress_percentage': progress_percentage,
                'status': status,
                'last_active_at': participant.last_active_at.isoformat() if participant.last_active_at else None,
            })

        # 그룹별 비율 계산
        groups = [
            {
                'name': '완료',
                'count': completed_count,
                'percentage': int((completed_count / total_students) * 100) if total_students > 0 else 0,
            },
            {
                'name': '진행중',
                'count': in_progress_count,
                'percentage': int((in_progress_count / total_students) * 100) if total_students > 0 else 0,
            },
            {
                'name': '지연',
                'count': delayed_count,
                'percentage': int((delayed_count / total_students) * 100) if total_students > 0 else 0,
            },
            {
                'name': '미시작',
                'count': not_started_count,
                'percentage': int((not_started_count / total_students) * 100) if total_students > 0 else 0,
            },
        ]

        return Response({
            'session_id': session_id,
            'total_students': total_students,
            'total_subtasks': total_subtasks,
            'current_session_step': current_session_step_index,
            'groups': groups,
            'progress_data': progress_data,
        })
