"""
WebSocket URL routing for Progress app
"""
from django.urls import path
from .consumers import ProgressConsumer

websocket_urlpatterns = [
    path('ws/progress/<int:user_id>/', ProgressConsumer.as_asgi()),
]
