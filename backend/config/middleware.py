"""
Custom middleware for WebSocket authentication
"""
import logging
from urllib.parse import parse_qs
from channels.middleware import BaseMiddleware
from channels.db import database_sync_to_async
from django.contrib.auth.models import AnonymousUser
from django.contrib.auth import get_user_model
from rest_framework_simplejwt.tokens import AccessToken
from rest_framework_simplejwt.exceptions import InvalidToken, TokenError

logger = logging.getLogger(__name__)
User = get_user_model()


class JWTWebSocketMiddleware(BaseMiddleware):
    """
    JWT 기반 WebSocket 인증 미들웨어

    WebSocket 연결 시 쿼리 파라미터에서 JWT 토큰을 추출하여 인증합니다.
    - 유효한 토큰: 해당 사용자로 설정
    - 무효한 토큰: AnonymousUser로 설정 (Consumer에서 처리)

    사용법:
    ws://localhost:8000/ws/sessions/{session_code}/?token={jwt_token}
    """

    async def __call__(self, scope, receive, send):
        # 쿼리 파라미터에서 토큰 추출
        query_string = scope.get('query_string', b'').decode('utf-8')
        query_params = parse_qs(query_string)
        token = query_params.get('token', [None])[0]

        if token:
            # 토큰 검증 및 사용자 설정
            user = await self.get_user_from_token(token)
            if user:
                scope['user'] = user
                logger.info(f"[WS Auth] Authenticated user: {user.id} ({getattr(user, 'name', 'N/A')})")
            else:
                scope['user'] = AnonymousUser()
                logger.warning("[WS Auth] Invalid token, using AnonymousUser")
        else:
            scope['user'] = AnonymousUser()
            logger.info("[WS Auth] No token provided, using AnonymousUser")

        return await super().__call__(scope, receive, send)

    @database_sync_to_async
    def get_user_from_token(self, token: str):
        """JWT 토큰에서 사용자 조회"""
        try:
            # AccessToken 검증
            access_token = AccessToken(token)
            user_id = access_token.get('user_id')

            if user_id:
                return User.objects.get(id=user_id)
            return None
        except (InvalidToken, TokenError) as e:
            logger.warning(f"[WS Auth] Token validation failed: {e}")
            return None
        except User.DoesNotExist:
            logger.warning(f"[WS Auth] User not found for token")
            return None
        except Exception as e:
            logger.error(f"[WS Auth] Unexpected error: {e}")
            return None


def JWTWebSocketMiddlewareStack(inner):
    """
    JWT 인증 미들웨어 스택
    """
    return JWTWebSocketMiddleware(inner)


# 하위 호환성을 위해 기존 이름도 유지
class AllowAnonymousWebSocketMiddleware(BaseMiddleware):
    """
    [DEPRECATED] 테스트용 익명 접근 미들웨어
    JWTWebSocketMiddleware 사용을 권장합니다.
    """

    async def __call__(self, scope, receive, send):
        scope['user'] = AnonymousUser()
        return await super().__call__(scope, receive, send)


def AllowAnonymousWebSocketMiddlewareStack(inner):
    """
    [DEPRECATED] 테스트용 익명 접근 스택
    JWTWebSocketMiddlewareStack 사용을 권장합니다.
    """
    return AllowAnonymousWebSocketMiddleware(inner)