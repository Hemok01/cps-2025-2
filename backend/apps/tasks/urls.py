"""
Task and Subtask URLs
"""
from django.urls import path
from .views import (
    TaskCreateView,
    TaskDetailView,
    TaskListView,
    SubtaskCreateView,
    SubtaskDetailView,
)

app_name = 'tasks'

urlpatterns = [
    # Task endpoints
    path('<int:pk>/', TaskDetailView.as_view(), name='task-detail'),
    path('<int:task_pk>/subtasks/create/', SubtaskCreateView.as_view(), name='subtask-create'),

    # Subtask endpoints
    path('subtasks/<int:pk>/', SubtaskDetailView.as_view(), name='subtask-detail'),
]
