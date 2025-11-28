from django.urls import path
from .views import (
    LectureStudentsView,
    PendingHelpRequestsView,
    LectureStatisticsView,
    SessionProgressStatsView,
)

app_name = 'dashboard'

urlpatterns = [
    path('lectures/<int:lecture_id>/students/', LectureStudentsView.as_view(), name='lecture-students'),
    path('help-requests/pending/', PendingHelpRequestsView.as_view(), name='pending-help-requests'),
    path('statistics/lecture/<int:lecture_id>/', LectureStatisticsView.as_view(), name='lecture-statistics'),
    path('sessions/<int:session_id>/progress-stats/', SessionProgressStatsView.as_view(), name='session-progress-stats'),
]
