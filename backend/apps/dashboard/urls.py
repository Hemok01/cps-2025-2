from django.urls import path
from .views import (
    LectureStudentsView,
    PendingHelpRequestsView,
    LectureStatisticsView,
    SessionProgressStatsView,
    StepAnalysisView,
    SessionTrendsView,
)

app_name = 'dashboard'

urlpatterns = [
    path('lectures/<int:lecture_id>/students/', LectureStudentsView.as_view(), name='lecture-students'),
    path('help-requests/pending/', PendingHelpRequestsView.as_view(), name='pending-help-requests'),
    path('statistics/lecture/<int:lecture_id>/', LectureStatisticsView.as_view(), name='lecture-statistics'),
    path('sessions/<int:session_id>/progress-stats/', SessionProgressStatsView.as_view(), name='session-progress-stats'),
    # 통계 분석 API
    path('statistics/lecture/<int:lecture_id>/step-analysis/', StepAnalysisView.as_view(), name='step-analysis'),
    path('statistics/lecture/<int:lecture_id>/session-trends/', SessionTrendsView.as_view(), name='session-trends'),
]
