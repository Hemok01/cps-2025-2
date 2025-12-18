"""
Lecture URLs
"""
from django.urls import path
from .views import (
    LectureListCreateView,
    LectureDetailView,
    LectureEnrollView,
)
from apps.tasks.views import TaskCreateView, TaskListView, AttachTasksToLectureView
from apps.sessions.views import SessionCreateView

app_name = 'lectures'

urlpatterns = [
    path('', LectureListCreateView.as_view(), name='lecture-list-create'),
    path('<int:pk>/', LectureDetailView.as_view(), name='lecture-detail'),
    path('<int:pk>/enroll/', LectureEnrollView.as_view(), name='lecture-enroll'),

    # Task endpoints under lectures
    path('<int:lecture_pk>/tasks/', TaskListView.as_view(), name='task-list'),
    path('<int:lecture_pk>/tasks/create/', TaskCreateView.as_view(), name='task-create'),
    path('<int:lecture_pk>/tasks/attach/', AttachTasksToLectureView.as_view(), name='task-attach'),

    # Session creation under lectures
    path('<int:lecture_pk>/sessions/create/', SessionCreateView.as_view(), name='session-create'),
]
