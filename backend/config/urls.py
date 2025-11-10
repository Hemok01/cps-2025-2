"""
URL Configuration for MobileGPT Backend
"""
from django.contrib import admin
from django.urls import path, include
from django.conf import settings
from django.conf.urls.static import static
from rest_framework_simplejwt.views import (
    TokenObtainPairView,
    TokenRefreshView,
)

urlpatterns = [
    # Admin
    path('admin/', admin.site.urls),

    # Health Check (no authentication required)
    path('api/health/', include('apps.health.urls')),

    # API v1
    path('api/auth/', include('apps.accounts.urls')),
    path('api/lectures/', include('apps.lectures.urls')),
    path('api/sessions/', include('apps.sessions.urls')),
    path('api/tasks/', include('apps.tasks.urls')),
    path('api/progress/', include('apps.progress.urls')),
    path('api/logs/', include('apps.logs.urls')),
    path('api/help/', include('apps.help.urls')),
    path('api/dashboard/', include('apps.dashboard.urls')),
    path('api/students/', include('apps.students.urls')),

    # JWT Token endpoints
    path('api/token/', TokenObtainPairView.as_view(), name='token_obtain_pair'),
    path('api/token/refresh/', TokenRefreshView.as_view(), name='token_refresh'),
]

# Serve media files in development
if settings.DEBUG:
    urlpatterns += static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)
    urlpatterns += static(settings.STATIC_URL, document_root=settings.STATIC_ROOT)
