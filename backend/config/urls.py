"""
URL Configuration for MobileGPT Backend
"""
from django.contrib import admin
from django.urls import path, include, re_path
from django.conf import settings
from django.conf.urls.static import static
from rest_framework_simplejwt.views import (
    TokenObtainPairView,
    TokenRefreshView,
)
from core.views import assetlinks, serve_media_with_cors

urlpatterns = [
    # Admin
    path('admin/', admin.site.urls),

    # Android App Links verification
    path('.well-known/assetlinks.json', assetlinks, name='assetlinks'),

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

# Serve media files with CORS headers (for cross-origin image loading)
if settings.DEBUG:
    # Use custom view for media files to add CORS headers
    urlpatterns += [
        re_path(r'^media/(?P<path>.*)$', serve_media_with_cors, name='media'),
    ]
    urlpatterns += static(settings.STATIC_URL, document_root=settings.STATIC_ROOT)
