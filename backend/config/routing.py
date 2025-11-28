"""
ASGI routing configuration for WebSocket support
"""
from channels.auth import AuthMiddlewareStack
from channels.routing import ProtocolTypeRouter, URLRouter
from django.core.asgi import get_asgi_application

# Import JWT middleware for WebSocket authentication
from config.middleware import JWTWebSocketMiddlewareStack

# Import app routing patterns
from apps.sessions.routing import websocket_urlpatterns as sessions_ws
from apps.dashboard.routing import websocket_urlpatterns as dashboard_ws
from apps.progress.routing import websocket_urlpatterns as progress_ws

# Combine all websocket URL patterns
websocket_urlpatterns = sessions_ws + dashboard_ws + progress_ws

application = ProtocolTypeRouter({
    "http": get_asgi_application(),
    "websocket": JWTWebSocketMiddlewareStack(
        URLRouter(
            websocket_urlpatterns
        )
    ),
})
