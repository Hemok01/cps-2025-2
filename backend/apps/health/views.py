"""
Health Check Views
"""
from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import AllowAny
from rest_framework.response import Response
from django.db import connection
from django.core.cache import cache
import time


@api_view(['GET'])
@permission_classes([AllowAny])
def health_check(request):
    """
    기본 헬스 체크 엔드포인트
    Docker healthcheck에서 사용
    """
    return Response({
        'status': 'healthy',
        'service': 'mobilegpt-backend'
    })


@api_view(['GET'])
@permission_classes([AllowAny])
def health_detailed(request):
    """
    상세 헬스 체크 (DB, Cache 연결 확인)
    """
    health_status = {
        'status': 'healthy',
        'service': 'mobilegpt-backend',
        'timestamp': time.time(),
        'checks': {}
    }

    # Database check
    try:
        with connection.cursor() as cursor:
            cursor.execute("SELECT 1")
        health_status['checks']['database'] = 'healthy'
    except Exception as e:
        health_status['checks']['database'] = f'unhealthy: {str(e)}'
        health_status['status'] = 'unhealthy'

    # Redis/Cache check
    try:
        cache.set('health_check', 'ok', 10)
        if cache.get('health_check') == 'ok':
            health_status['checks']['cache'] = 'healthy'
        else:
            health_status['checks']['cache'] = 'unhealthy: cache test failed'
            health_status['status'] = 'unhealthy'
    except Exception as e:
        health_status['checks']['cache'] = f'unhealthy: {str(e)}'
        health_status['status'] = 'unhealthy'

    return Response(health_status)
