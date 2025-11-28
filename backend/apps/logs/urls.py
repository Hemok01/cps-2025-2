from django.urls import path
from .views import ActivityLogCreateView, ActivityLogBatchView, AnonymousActivityLogCreateView

app_name = 'logs'

urlpatterns = [
    path('activity/', ActivityLogCreateView.as_view(), name='activity-log-create'),
    path('activity/anonymous/', AnonymousActivityLogCreateView.as_view(), name='activity-log-anonymous'),
    path('batch/', ActivityLogBatchView.as_view(), name='activity-log-batch'),
]
