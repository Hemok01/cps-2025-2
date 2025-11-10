"""
Activity Log Views
"""
import logging
from rest_framework import generics, status
from rest_framework.response import Response
from rest_framework.permissions import IsAuthenticated
from rest_framework.views import APIView

from .models import ActivityLog
from .serializers import ActivityLogSerializer, ActivityLogCreateSerializer
from .kafka_producer import producer

logger = logging.getLogger(__name__)


def _prepare_kafka_data(validated_data):
    """Convert Django model instances to IDs for Kafka serialization"""
    kafka_data = validated_data.copy()

    # Convert ForeignKey objects to IDs
    if 'session' in kafka_data and kafka_data['session']:
        kafka_data['session'] = kafka_data['session'].id
    if 'subtask' in kafka_data and kafka_data['subtask']:
        kafka_data['subtask'] = kafka_data['subtask'].id

    return kafka_data


class ActivityLogCreateView(generics.CreateAPIView):
    """행동 로그 전송 (클라이언트 → 서버)"""
    serializer_class = ActivityLogCreateSerializer
    permission_classes = [IsAuthenticated]

    def create(self, request, *args, **kwargs):
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        # Kafka Producer로 메시지 전송 (객체를 ID로 변환)
        log_data = _prepare_kafka_data(serializer.validated_data)
        kafka_success = producer.send_log(log_data, request.user.id)

        if kafka_success:
            # Kafka 전송 성공
            logger.info(f"Activity log queued to Kafka for user {request.user.id}")
            return Response({
                'status': 'queued',
                'message': 'Log queued for processing'
            }, status=status.HTTP_202_ACCEPTED)
        else:
            # Kafka 실패 시 DB에 직접 저장 (Fallback)
            logger.warning(f"Kafka unavailable, saving log directly to DB for user {request.user.id}")
            log = serializer.save(user=request.user)
            return Response({
                'log_id': log.id,
                'status': 'saved',
                'message': 'Log saved directly to database'
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

        # Validate all logs first
        validated_logs = []
        for log_data in logs_data:
            serializer = ActivityLogCreateSerializer(data=log_data)
            if serializer.is_valid():
                # Convert objects to IDs for Kafka
                kafka_data = _prepare_kafka_data(serializer.validated_data)
                validated_logs.append(kafka_data)
            else:
                logger.warning(f"Invalid log data in batch: {serializer.errors}")

        if not validated_logs:
            return Response(
                {'error': '유효한 로그가 없습니다.'},
                status=status.HTTP_400_BAD_REQUEST
            )

        # Kafka Producer로 배치 전송
        kafka_success = producer.send_logs_batch(validated_logs, request.user.id)

        if kafka_success:
            # Kafka 전송 성공
            logger.info(f"Batch of {len(validated_logs)} logs queued to Kafka for user {request.user.id}")
            return Response({
                'status': 'queued',
                'queued_count': len(validated_logs),
                'message': f'{len(validated_logs)} logs queued for processing'
            }, status=status.HTTP_202_ACCEPTED)
        else:
            # Kafka 실패 시 DB에 직접 저장 (Fallback)
            logger.warning(f"Kafka unavailable, saving batch logs directly to DB for user {request.user.id}")
            created_logs = []
            for log_data in validated_logs:
                serializer = ActivityLogCreateSerializer(data=log_data)
                if serializer.is_valid():
                    log = serializer.save(user=request.user)
                    created_logs.append(log.id)

            return Response({
                'status': 'saved',
                'created_count': len(created_logs),
                'log_ids': created_logs,
                'message': f'{len(created_logs)} logs saved directly to database'
            }, status=status.HTTP_201_CREATED)
