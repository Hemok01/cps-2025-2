"""
Task and Subtask Views
"""
from rest_framework import generics, status
from rest_framework.response import Response
from rest_framework.permissions import IsAuthenticated
from django.shortcuts import get_object_or_404

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
