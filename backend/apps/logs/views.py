"""
Activity Log Views
"""
from rest_framework import generics, status
from rest_framework.response import Response
from rest_framework.permissions import IsAuthenticated
from rest_framework.views import APIView

from .models import ActivityLog
from .serializers import ActivityLogSerializer, ActivityLogCreateSerializer


class ActivityLogCreateView(generics.CreateAPIView):
    """행동 로그 전송 (클라이언트 → 서버)"""
    serializer_class = ActivityLogCreateSerializer
    permission_classes = [IsAuthenticated]

    def create(self, request, *args, **kwargs):
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        log = serializer.save(user=request.user)
        
        # TODO: Kafka Producer로 메시지 전송 (향후 구현)
        
        return Response({
            'log_id': log.id,
            'message': 'Log saved successfully'
        }, status=status.HTTP_201_CREATED)


class ActivityLogBatchView(APIView):
    """배치 로그 전송 (네트워크 효율성)"""
    permission_classes = [IsAuthenticated]

    def post(self, request):
        logs_data = request.data.get('logs', [])
        
        if not logs_data:
            return Response(
                {'error': 'logs 배열이 필요합니다.'},
                status=status.HTTP_400_BAD_REQUEST
            )
        
        created_logs = []
        for log_data in logs_data:
            serializer = ActivityLogCreateSerializer(data=log_data)
            if serializer.is_valid():
                log = serializer.save(user=request.user)
                created_logs.append(log.id)
        
        return Response({
            'created_count': len(created_logs),
            'log_ids': created_logs,
            'message': f'{len(created_logs)} logs saved successfully'
        }, status=status.HTTP_201_CREATED)
