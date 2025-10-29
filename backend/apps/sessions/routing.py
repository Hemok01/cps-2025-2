"""
WebSocket routing for lecture sessions
"""
from django.urls import path
from .consumers import SessionConsumer

websocket_urlpatterns = [
    path('ws/sessions/<str:session_code>/', SessionConsumer.as_asgi()),
]
