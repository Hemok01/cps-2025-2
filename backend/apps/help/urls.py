from django.urls import path
from .views import (
    HelpRequestCreateView,
    HelpRequestDetailView,
    HelpFeedbackView
)

app_name = 'help'

urlpatterns = [
    path('request/', HelpRequestCreateView.as_view(), name='help-request-create'),
    path('request/<int:help_request_id>/', HelpRequestDetailView.as_view(), name='help-request-detail'),
    path('feedback/', HelpFeedbackView.as_view(), name='help-feedback'),
]
