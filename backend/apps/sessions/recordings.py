"""
Recording Session Views (녹화 기능)
"""
from rest_framework import viewsets, status
from rest_framework.decorators import action
from rest_framework.response import Response
from rest_framework.permissions import IsAuthenticated
from django.utils import timezone
from django.db.models import Prefetch

from .models import RecordingSession
from .serializers import (
    RecordingSessionSerializer,
    RecordingSessionCreateSerializer,
    RecordingSessionListSerializer
)
from apps.logs.models import ActivityLog
from apps.logs.serializers import ActivityLogSerializer


class RecordingSessionViewSet(viewsets.ModelViewSet):
    """녹화 세션 ViewSet"""
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        """강의자 본인의 녹화 세션만 조회 가능"""
        return RecordingSession.objects.filter(
            instructor=self.request.user
        ).select_related('instructor', 'lecture')

    def get_serializer_class(self):
        """액션에 따라 적절한 serializer 반환"""
        if self.action == 'list':
            return RecordingSessionListSerializer
        elif self.action == 'create':
            return RecordingSessionCreateSerializer
        return RecordingSessionSerializer

    def perform_create(self, serializer):
        """녹화 세션 생성 (녹화 시작)"""
        recording = serializer.save(
            instructor=self.request.user,
            status='RECORDING',
            started_at=timezone.now()
        )
        return recording

    def create(self, request, *args, **kwargs):
        """POST /api/recordings/ - 녹화 시작"""
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        recording = self.perform_create(serializer)

        # 생성된 녹화 세션 정보를 상세 serializer로 반환
        response_serializer = RecordingSessionSerializer(recording)
        return Response(response_serializer.data, status=status.HTTP_201_CREATED)

    @action(detail=True, methods=['post'])
    def stop(self, request, pk=None):
        """POST /api/recordings/{id}/stop/ - 녹화 종료"""
        recording = self.get_object()

        # 이미 종료된 녹화인지 확인
        if recording.status != 'RECORDING':
            return Response(
                {'error': '이미 종료된 녹화입니다.'},
                status=status.HTTP_400_BAD_REQUEST
            )

        # 녹화 종료 처리
        recording.status = 'COMPLETED'
        recording.ended_at = timezone.now()

        # 녹화된 이벤트 수 계산
        recording.event_count = ActivityLog.objects.filter(
            recording_session=recording
        ).count()

        # 녹화 시간 계산 (초 단위)
        if recording.started_at and recording.ended_at:
            duration = (recording.ended_at - recording.started_at).total_seconds()
            recording.duration_seconds = int(duration)

        recording.save()

        serializer = RecordingSessionSerializer(recording)
        return Response(serializer.data)

    @action(detail=True, methods=['get'])
    def events(self, request, pk=None):
        """GET /api/recordings/{id}/events/ - 녹화된 이벤트 목록 조회"""
        recording = self.get_object()

        # 해당 녹화 세션의 모든 이벤트 조회
        events = ActivityLog.objects.filter(
            recording_session=recording
        ).select_related('user').order_by('timestamp')

        # 페이지네이션 적용 (옵션)
        page = self.paginate_queryset(events)
        if page is not None:
            serializer = ActivityLogSerializer(page, many=True)
            return self.get_paginated_response(serializer.data)

        serializer = ActivityLogSerializer(events, many=True)
        return Response(serializer.data)

    @action(detail=True, methods=['post'])
    def save_events_batch(self, request, pk=None):
        """
        POST /api/recordings/{id}/save-events-batch/ - 녹화 이벤트 일괄 저장

        Android 앱에서 녹화 중지 시 버퍼링된 모든 이벤트를 한 번에 전송
        """
        recording = self.get_object()

        # 녹화 중인 세션인지 확인
        if recording.status not in ['RECORDING', 'COMPLETED']:
            return Response(
                {'error': '녹화 중이거나 완료된 세션만 이벤트를 저장할 수 있습니다.'},
                status=status.HTTP_400_BAD_REQUEST
            )

        events_data = request.data.get('events', [])

        if not events_data or not isinstance(events_data, list):
            return Response(
                {'error': 'events 배열이 필요합니다.'},
                status=status.HTTP_400_BAD_REQUEST
            )

        # 이벤트 객체 생성 (bulk create 사용)
        activity_logs = []
        for event_data in events_data:
            # event_data 구조화
            event_obj = {
                'package': event_data.get('package', ''),
                'className': event_data.get('className', ''),
                'text': event_data.get('text', ''),
            }

            activity_log = ActivityLog(
                user=request.user,
                recording_session=recording,
                event_type=event_data.get('eventType', 'CLICK'),
                event_data=event_obj,
                view_id_resource_name=event_data.get('viewId', ''),
                content_description=event_data.get('contentDescription', ''),
                bounds=event_data.get('bounds', ''),
                is_clickable=event_data.get('isClickable', False),
                is_editable=event_data.get('isEditable', False),
                is_enabled=event_data.get('isEnabled', True),
                is_focused=event_data.get('isFocused', False),
            )
            activity_logs.append(activity_log)

        # Bulk insert
        created_logs = ActivityLog.objects.bulk_create(activity_logs)

        # 녹화 세션의 이벤트 수 업데이트
        recording.event_count = ActivityLog.objects.filter(
            recording_session=recording
        ).count()
        recording.save(update_fields=['event_count'])

        return Response({
            'message': f'{len(created_logs)}개의 이벤트가 저장되었습니다.',
            'saved_count': len(created_logs),
            'recording': RecordingSessionSerializer(recording).data
        }, status=status.HTTP_201_CREATED)
