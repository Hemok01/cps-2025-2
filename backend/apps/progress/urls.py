from django.urls import path
from .views import MyProgressView, ProgressUpdateView, UserProgressDetailView

app_name = 'progress'

urlpatterns = [
    path('me/', MyProgressView.as_view(), name='my-progress'),
    path('update/', ProgressUpdateView.as_view(), name='progress-update'),
    path('users/<int:user_id>/lectures/<int:lecture_id>/', UserProgressDetailView.as_view(), name='user-progress-detail'),
]
