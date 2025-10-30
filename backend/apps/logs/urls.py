from django.urls import path
from .views import ActivityLogCreateView, ActivityLogBatchView

app_name = 'logs'

urlpatterns = [
    path('activity/', ActivityLogCreateView.as_view(), name='activity-log-create'),
    path('batch/', ActivityLogBatchView.as_view(), name='activity-log-batch'),
]
