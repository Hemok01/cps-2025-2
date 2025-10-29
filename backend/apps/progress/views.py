"""
Progress Views
"""
from rest_framework import generics, status
from rest_framework.response import Response
from rest_framework.permissions import IsAuthenticated
from rest_framework.views import APIView
from django.shortcuts import get_object_or_404
from django.utils import timezone

from apps.tasks.models import Subtask, Task
from apps.lectures.models import Lecture
from .models import UserProgress
from .serializers import UserProgressSerializer, UserProgressUpdateSerializer


class MyProgressView(APIView):
    """내 진행 상태 조회 (학생용)"""
    permission_classes = [IsAuthenticated]

    def get(self, request):
        lecture_id = request.query_params.get('lecture_id')
        
        if lecture_id:
            # 특정 강의의 진행 상태
            progress = UserProgress.objects.filter(
                user=request.user,
                subtask__task__lecture_id=lecture_id
            ).select_related('subtask', 'subtask__task').order_by('-updated_at')
        else:
            # 전체 진행 상태
            progress = UserProgress.objects.filter(
                user=request.user
            ).select_related('subtask', 'subtask__task').order_by('-updated_at')[:20]
        
        serializer = UserProgressSerializer(progress, many=True)
        
        # 현재 진행 중인 subtask 찾기
        current_subtask = progress.filter(status='IN_PROGRESS').first()
        
        # 통계 계산
        if lecture_id:
            total_subtasks = Subtask.objects.filter(task__lecture_id=lecture_id).count()
            completed_subtasks = progress.filter(status='COMPLETED').count()
        else:
            total_subtasks = progress.count()
            completed_subtasks = progress.filter(status='COMPLETED').count()
        
        return Response({
            'current_subtask': UserProgressSerializer(current_subtask).data if current_subtask else None,
            'progress_summary': {
                'completed_subtasks': completed_subtasks,
                'total_subtasks': total_subtasks,
                'completion_rate': completed_subtasks / total_subtasks if total_subtasks > 0 else 0
            },
            'progress': serializer.data
        })


class ProgressUpdateView(APIView):
    """진행 상태 업데이트"""
    permission_classes = [IsAuthenticated]

    def post(self, request):
        subtask_id = request.data.get('subtask_id')
        new_status = request.data.get('status')
        session_id = request.data.get('session_id')
        
        if not subtask_id or not new_status:
            return Response(
                {'error': 'subtask_id와 status가 필요합니다.'},
                status=status.HTTP_400_BAD_REQUEST
            )
        
        subtask = get_object_or_404(Subtask, pk=subtask_id)
        
        # UserProgress 가져오거나 생성
        progress, created = UserProgress.objects.get_or_create(
            user=request.user,
            subtask=subtask,
            session_id=session_id,
            defaults={
                'status': new_status,
                'started_at': timezone.now() if new_status == 'IN_PROGRESS' else None
            }
        )
        
        if not created:
            # 기존 progress 업데이트
            progress.status = new_status
            progress.attempts += 1
            
            if new_status == 'COMPLETED' and not progress.completed_at:
                progress.completed_at = timezone.now()
            elif new_status == 'IN_PROGRESS' and not progress.started_at:
                progress.started_at = timezone.now()
            
            progress.save()
        
        # 다음 subtask 찾기
        next_subtask = None
        if new_status == 'COMPLETED':
            next_subtask = Subtask.objects.filter(
                task=subtask.task,
                order_index__gt=subtask.order_index
            ).order_by('order_index').first()
        
        return Response({
            'subtask_id': subtask.id,
            'status': progress.status,
            'completed_at': progress.completed_at,
            'next_subtask': {
                'id': next_subtask.id,
                'title': next_subtask.title
            } if next_subtask else None
        })


class UserProgressDetailView(APIView):
    """특정 학생의 강의 진행 상태 조회 (강사용)"""
    permission_classes = [IsAuthenticated]

    def get(self, request, user_id, lecture_id):
        # 강의 조회 및 강사 권한 확인
        lecture = get_object_or_404(Lecture, pk=lecture_id)
        
        if lecture.instructor != request.user:
            return Response(
                {'error': '강사만 학생의 진행 상태를 조회할 수 있습니다.'},
                status=status.HTTP_403_FORBIDDEN
            )
        
        # 진행 상태 조회
        progress = UserProgress.objects.filter(
            user_id=user_id,
            subtask__task__lecture=lecture
        ).select_related('subtask', 'subtask__task').order_by('subtask__task__order_index', 'subtask__order_index')
        
        serializer = UserProgressSerializer(progress, many=True)
        
        return Response({
            'user_id': user_id,
            'lecture_id': lecture_id,
            'lecture_title': lecture.title,
            'progress': serializer.data
        })
