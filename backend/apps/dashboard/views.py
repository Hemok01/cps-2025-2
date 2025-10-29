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
