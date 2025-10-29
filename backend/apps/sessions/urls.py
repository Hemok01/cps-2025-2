"""
Session URLs
"""
from django.urls import path
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
)

app_name = 'sessions'

urlpatterns = [
    # Session management
    path('<str:session_code>/', SessionByCodeView.as_view(), name='session-by-code'),
    path('<int:session_id>/join/', SessionJoinView.as_view(), name='session-join'),
    path('<int:session_id>/participants/', SessionParticipantsView.as_view(), name='session-participants'),
    path('<int:session_id>/start/', SessionStartView.as_view(), name='session-start'),
    path('<int:session_id>/next-step/', SessionNextStepView.as_view(), name='session-next-step'),
    path('<int:session_id>/pause/', SessionPauseView.as_view(), name='session-pause'),
    path('<int:session_id>/resume/', SessionResumeView.as_view(), name='session-resume'),
    path('<int:session_id>/end/', SessionEndView.as_view(), name='session-end'),
    path('<int:session_id>/current/', SessionCurrentView.as_view(), name='session-current'),
    
    # Student endpoints
    path('my-active/', MyActiveSessionView.as_view(), name='my-active-session'),
]
