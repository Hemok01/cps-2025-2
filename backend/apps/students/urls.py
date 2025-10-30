"""
Student URLs
"""
from django.urls import path, include
from rest_framework.routers import DefaultRouter
from .views import StudentLectureViewSet, StudentSessionViewSet

app_name = 'students'

router = DefaultRouter()
router.register(r'lectures', StudentLectureViewSet, basename='lecture')
router.register(r'sessions', StudentSessionViewSet, basename='session')

urlpatterns = [
    path('', include(router.urls)),
]
