"""
ASGI config for MobileGPT project.

It exposes the ASGI callable as a module-level variable named ``application``.

For more information on this file, see
https://docs.djangoproject.com/en/5.0/howto/deployment/asgi/
"""

import os

from django.core.asgi import get_asgi_application
from channels.routing import ProtocolTypeRouter, URLRouter
from channels.auth import AuthMiddlewareStack
from channels.security.websocket import AllowedHostsOriginValidator

os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'config.settings')

django_asgi_app = get_asgi_application()

# Import JWT middleware for WebSocket authentication
from config.middleware import JWTWebSocketMiddlewareStack

# Import websocket routing after Django setup
from apps.sessions import routing as session_routing
from apps.dashboard import routing as dashboard_routing
from apps.progress import routing as progress_routing

application = ProtocolTypeRouter({
    "http": django_asgi_app,
    "websocket": JWTWebSocketMiddlewareStack(
        URLRouter(
            session_routing.websocket_urlpatterns +
            dashboard_routing.websocket_urlpatterns +
            progress_routing.websocket_urlpatterns
        )
    ),
})
