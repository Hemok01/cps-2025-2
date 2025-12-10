"""
Task and Subtask URLs
"""
from django.urls import path
from .views import (
    TaskCreateView,
    TaskDetailView,
    TaskListView,
    IndependentTaskListView,
    AttachTasksToLectureView,
    SubtaskCreateView,
    SubtaskDetailView,
    SubtaskBulkUpdateView,
)

app_name = 'tasks'

urlpatterns = [
    # 독립 Task 목록 (Lecture에 연결되지 않은 Task들)
    path('available/', IndependentTaskListView.as_view(), name='task-available-list'),

    # Task endpoints
    path('<int:pk>/', TaskDetailView.as_view(), name='task-detail'),
    path('<int:task_pk>/subtasks/create/', SubtaskCreateView.as_view(), name='subtask-create'),
    path('<int:task_pk>/subtasks/bulk/', SubtaskBulkUpdateView.as_view(), name='subtask-bulk-update'),

    # Subtask endpoints
    path('subtasks/<int:pk>/', SubtaskDetailView.as_view(), name='subtask-detail'),
]
