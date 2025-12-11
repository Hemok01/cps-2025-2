"""
Task and Subtask Views
"""
from rest_framework import generics, status
from rest_framework.response import Response
from rest_framework.views import APIView
from rest_framework.permissions import IsAuthenticated
from django.shortcuts import get_object_or_404
from django.db import transaction

from apps.lectures.models import Lecture
from .models import Task, Subtask
from .serializers import (
    TaskSerializer,
    TaskCreateUpdateSerializer,
    TaskDetailSerializer,
    SubtaskSerializer,
    SubtaskCreateUpdateSerializer
)


class TaskCreateView(generics.CreateAPIView):
    """Create a task for a lecture"""
    serializer_class = TaskCreateUpdateSerializer
    permission_classes = [IsAuthenticated]

    def create(self, request, *args, **kwargs):
        """Create task under a lecture"""
        lecture_id = kwargs.get('lecture_pk')
        lecture = get_object_or_404(Lecture, pk=lecture_id)
        
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        task = serializer.save(lecture=lecture)
        
        return Response(
            TaskSerializer(task).data,
            status=status.HTTP_201_CREATED
        )


class TaskDetailView(generics.RetrieveUpdateDestroyAPIView):
    """Retrieve, update or delete a task"""
    queryset = Task.objects.all().prefetch_related('subtasks')
    permission_classes = [IsAuthenticated]

    def get_serializer_class(self):
        if self.request.method in ['PUT', 'PATCH']:
            return TaskCreateUpdateSerializer
        return TaskDetailSerializer


class SubtaskCreateView(generics.CreateAPIView):
    """Create a subtask for a task"""
    serializer_class = SubtaskCreateUpdateSerializer
    permission_classes = [IsAuthenticated]

    def create(self, request, *args, **kwargs):
        """Create subtask under a task"""
        task_id = kwargs.get('task_pk')
        task = get_object_or_404(Task, pk=task_id)
        
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        subtask = serializer.save(task=task)
        
        return Response(
            SubtaskSerializer(subtask).data,
            status=status.HTTP_201_CREATED
        )


class SubtaskDetailView(generics.RetrieveUpdateDestroyAPIView):
    """Retrieve, update or delete a subtask"""
    queryset = Subtask.objects.all()
    permission_classes = [IsAuthenticated]

    def get_serializer_class(self):
        if self.request.method in ['PUT', 'PATCH']:
            return SubtaskCreateUpdateSerializer
        return SubtaskSerializer


class TaskListView(generics.ListAPIView):
    """List all tasks for a lecture"""
    serializer_class = TaskSerializer
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        """Filter tasks by lecture"""
        lecture_id = self.kwargs.get('lecture_pk')
        return Task.objects.filter(lecture_id=lecture_id).prefetch_related('subtasks')


class IndependentTaskListView(generics.ListAPIView):
    """
    Lecture에 연결되지 않은 독립 Task 목록 조회
    GET /api/tasks/available/

    안드로이드 앱에서 녹화 → 분석 → 변환으로 생성된 Task들 중
    아직 강의에 연결되지 않은 것들을 반환합니다.
    """
    serializer_class = TaskSerializer
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        return Task.objects.filter(
            lecture__isnull=True
        ).prefetch_related('subtasks').order_by('-created_at')


class AttachTasksToLectureView(APIView):
    """
    기존 독립 Task들을 Lecture에 연결
    POST /api/lectures/{lecture_pk}/tasks/attach/

    Request Body:
    {
        "task_ids": [1, 2, 3]
    }

    Response:
    {
        "message": "3개의 과제가 연결되었습니다",
        "lecture_id": 1,
        "attached_task_ids": [1, 2, 3]
    }
    """
    permission_classes = [IsAuthenticated]

    def post(self, request, lecture_pk):
        from django.db import models as db_models

        lecture = get_object_or_404(Lecture, pk=lecture_pk)
        task_ids = request.data.get('task_ids', [])

        if not task_ids:
            return Response(
                {'error': 'task_ids가 필요합니다'},
                status=status.HTTP_400_BAD_REQUEST
            )

        with transaction.atomic():
            # 기존 order_index 최대값 조회
            max_order = lecture.tasks.aggregate(
                max_order=db_models.Max('order_index')
            )['max_order'] or -1

            copied_tasks = []
            for idx, task_id in enumerate(task_ids):
                # 원본 Task 조회 (템플릿만 - lecture가 NULL인 것)
                source_task = Task.objects.filter(
                    id=task_id,
                    lecture__isnull=True
                ).prefetch_related('subtasks').first()

                if source_task:
                    # 복사본 생성 (원본은 유지)
                    task_copy = source_task.copy_to_lecture(
                        lecture=lecture,
                        order_index=max_order + idx + 1
                    )
                    copied_tasks.append(task_copy.id)

        return Response({
            'message': f'{len(copied_tasks)}개의 과제가 복사되어 연결되었습니다',
            'lecture_id': lecture.id,
            'attached_task_ids': copied_tasks  # 하위 호환성 유지
        })


class SubtaskBulkUpdateView(APIView):
    """
    Bulk update subtasks for a task
    PUT /api/tasks/{task_pk}/subtasks/bulk/

    기존 Subtask를 모두 삭제하고 새로운 Subtask로 교체합니다.
    강의자 앱에서 단계 편집 후 일괄 저장할 때 사용합니다.
    """
    permission_classes = [IsAuthenticated]

    def put(self, request, task_pk=None):
        """
        Request Body:
        {
            "subtasks": [
                {
                    "title": "단계 제목",
                    "description": "설명",
                    "target_action": "CLICK",
                    "time": 1731207000,
                    "text": "홈",
                    "content_description": "홈버튼",
                    "view_id": "com.sec.android:id/home_btn",
                    "bounds": "[0,100][200,150]",
                    "target_package": "com.example.app",
                    "target_class": "Button",
                    ...
                },
                ...
            ]
        }
        """
        task = get_object_or_404(Task, pk=task_pk)
        subtasks_data = request.data.get('subtasks', [])

        if not isinstance(subtasks_data, list):
            return Response(
                {'error': 'subtasks must be a list'},
                status=status.HTTP_400_BAD_REQUEST
            )

        with transaction.atomic():
            # 기존 Subtask 삭제
            deleted_count = task.subtasks.all().delete()[0]

            # 새로운 Subtask 생성
            created_subtasks = []
            for idx, data in enumerate(subtasks_data):
                # task 필드 제거 (직접 설정)
                data.pop('task', None)
                data.pop('id', None)
                data.pop('created_at', None)
                data.pop('updated_at', None)

                subtask = Subtask.objects.create(
                    task=task,
                    order_index=idx,
                    title=data.get('title', f'단계 {idx + 1}'),
                    description=data.get('description', ''),
                    target_action=data.get('target_action', ''),
                    target_element_hint=data.get('target_element_hint', ''),
                    guide_text=data.get('guide_text', ''),
                    voice_guide_text=data.get('voice_guide_text', ''),
                    # Flask 동기화 필드
                    time=data.get('time'),
                    text=data.get('text', ''),
                    content_description=data.get('content_description', ''),
                    view_id=data.get('view_id', ''),
                    bounds=data.get('bounds', ''),
                    target_package=data.get('target_package', ''),
                    target_class=data.get('target_class', ''),
                )
                created_subtasks.append(subtask)

        return Response({
            'status': 'updated',
            'deleted_count': deleted_count,
            'created_count': len(created_subtasks),
            'subtasks': SubtaskSerializer(created_subtasks, many=True).data
        })
