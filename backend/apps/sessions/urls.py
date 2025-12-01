"""
Session URLs
"""
from django.urls import path, include
from rest_framework.routers import DefaultRouter
from .views import (
    SessionByCodeView,
    SessionJoinView,
    SessionParticipantsView,
    SessionStartView,
    SessionNextStepView,
    SessionPauseView,
    SessionResumeView,
    SessionEndView,
    SessionCurrentView,
    MyActiveSessionView,
    InstructorActiveSessionView,
    AnonymousSessionJoinView,
    SessionBroadcastView,
    SessionSwitchLectureView,
    ReportCompletionView,
    SessionCompletionStatusView,
)
from .recordings import RecordingSessionViewSet
from .screenshot_views import (
    ScreenshotUploadView,
    StudentScreenshotListView,
    StudentScreenshotDetailView,
    StudentScreenshotByDeviceView,
)

app_name = 'sessions'

# Router for Recording ViewSet
router = DefaultRouter()
router.register(r'recordings', RecordingSessionViewSet, basename='recording')

urlpatterns = [
    # Recording endpoints (router) - Must come before <str:session_code> pattern!
    path('', include(router.urls)),

    # Anonymous join (학생 앱용) - Must come before <str:session_code> pattern!
    path('join/', AnonymousSessionJoinView.as_view(), name='anonymous-session-join'),

    # Instructor active sessions - Must come before <str:session_code> pattern!
    path('instructor-active/', InstructorActiveSessionView.as_view(), name='instructor-active-sessions'),

    # Student endpoints - Must come before <str:session_code> pattern!
    path('my-active/', MyActiveSessionView.as_view(), name='my-active-session'),

    # Session management by ID (more specific patterns first)
    path('<int:session_id>/join/', SessionJoinView.as_view(), name='session-join'),
    path('<int:session_id>/participants/', SessionParticipantsView.as_view(), name='session-participants'),
    path('<int:session_id>/start/', SessionStartView.as_view(), name='session-start'),
    path('<int:session_id>/next-step/', SessionNextStepView.as_view(), name='session-next-step'),
    path('<int:session_id>/pause/', SessionPauseView.as_view(), name='session-pause'),
    path('<int:session_id>/resume/', SessionResumeView.as_view(), name='session-resume'),
    path('<int:session_id>/end/', SessionEndView.as_view(), name='session-end'),
    path('<int:session_id>/current/', SessionCurrentView.as_view(), name='session-current'),
    path('<int:session_id>/broadcast/', SessionBroadcastView.as_view(), name='session-broadcast'),
    path('<int:session_id>/switch-lecture/', SessionSwitchLectureView.as_view(), name='session-switch-lecture'),

    # Step completion endpoints (단계 완료 보고)
    path('<int:session_id>/report-completion/', ReportCompletionView.as_view(), name='session-report-completion'),
    path('<int:session_id>/completion-status/', SessionCompletionStatusView.as_view(), name='session-completion-status'),

    # Screenshot endpoints (학생 화면 스크린샷)
    path('<int:session_id>/screenshots/', StudentScreenshotListView.as_view(), name='session-screenshots'),
    path('<int:session_id>/screenshots/upload/', ScreenshotUploadView.as_view(), name='screenshot-upload'),
    path('<int:session_id>/screenshots/<int:participant_id>/', StudentScreenshotDetailView.as_view(), name='student-screenshot'),
    path('<int:session_id>/screenshots/by-device/<str:device_id>/', StudentScreenshotByDeviceView.as_view(), name='student-screenshot-by-device'),

    # Session by code (most generic pattern - MUST be last!)
    # <str:session_code> matches any string, so it must come after all specific patterns
    path('<str:session_code>/', SessionByCodeView.as_view(), name='session-by-code'),
]
