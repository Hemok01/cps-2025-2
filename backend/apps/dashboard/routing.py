"""
WebSocket routing for instructor dashboard
"""
from django.urls import path
from .consumers import DashboardConsumer

websocket_urlpatterns = [
    path('ws/dashboard/lectures/<int:lecture_id>/', DashboardConsumer.as_asgi()),
]
