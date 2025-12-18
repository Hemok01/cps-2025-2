"""
Recording URLs (녹화 API)
- 기존 /api/sessions/recordings/ 에서 /api/recordings/ 로 분리
"""
from django.urls import path, include
from rest_framework.routers import DefaultRouter
from .recordings import RecordingSessionViewSet

app_name = 'recordings'

# Router for Recording ViewSet
router = DefaultRouter()
router.register(r'', RecordingSessionViewSet, basename='recording')

urlpatterns = [
    path('', include(router.urls)),
]
