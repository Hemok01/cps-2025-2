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
    RecordingSessionListSerializer,
    RecordingSessionAnalysisSerializer,
    RecordingConvertSerializer
)
from .tasks import analyze_recording_task
from apps.logs.models import ActivityLog
from apps.logs.serializers import ActivityLogSerializer
from apps.tasks.models import Subtask
from apps.tasks.serializers import SubtaskSerializer


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

    @action(detail=True, methods=['post'], url_path='save-events-batch')
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
        for event in events_data:
            # Android에서 event_data 객체 안에 중첩하여 전송하므로 추출
            event_data = event.get('event_data', {})

            # event_data 구조화 (JSON 키 이름에 맞춤: class_name, package 등)
            event_obj = {
                'package': event_data.get('package', ''),
                'className': event_data.get('class_name', ''),  # Android: class_name
                'text': event_data.get('text', []),  # Android에서 List로 전송
            }

            activity_log = ActivityLog(
                user=request.user,
                recording_session=recording,
                event_type=event.get('event_type', 'CLICK'),  # Android: event_type
                event_data=event_obj,
                view_id_resource_name=event_data.get('view_id', ''),  # Android: view_id
                content_description=event_data.get('content_description', ''),  # Android: content_description
                bounds=event_data.get('bounds', ''),
                is_clickable=event_data.get('is_clickable', False),
                is_editable=event_data.get('is_editable', False),
                is_enabled=event_data.get('is_enabled', True),
                is_focused=event_data.get('is_focused', False),
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

    @action(detail=True, methods=['post'])
    def analyze(self, request, pk=None):
        """
        POST /api/sessions/recordings/{id}/analyze/
        녹화 세션을 AI로 분석하여 단계를 생성 (비동기)
        """
        recording = self.get_object()

        # 상태 확인: COMPLETED 또는 FAILED만 분석 가능
        if recording.status not in ['COMPLETED', 'FAILED']:
            return Response(
                {'error': f'녹화가 완료된 상태에서만 분석할 수 있습니다. 현재 상태: {recording.status}'},
                status=status.HTTP_400_BAD_REQUEST
            )

        # 이벤트가 있는지 확인
        event_count = ActivityLog.objects.filter(recording_session=recording).count()
        if event_count == 0:
            return Response(
                {'error': '분석할 이벤트가 없습니다. 녹화된 이벤트가 필요합니다.'},
                status=status.HTTP_400_BAD_REQUEST
            )

        # 비동기 태스크 시작
        analyze_recording_task.delay(recording.id)

        # 상태를 PROCESSING으로 변경
        recording.status = 'PROCESSING'
        recording.analysis_error = ''
        recording.save(update_fields=['status', 'analysis_error', 'updated_at'])

        return Response({
            'message': '분석이 시작되었습니다.',
            'recording_id': recording.id,
            'status': 'PROCESSING'
        }, status=status.HTTP_202_ACCEPTED)

    @action(detail=True, methods=['get'], url_path='analysis-status')
    def analysis_status(self, request, pk=None):
        """
        GET /api/sessions/recordings/{id}/analysis-status/
        분석 상태 및 결과 조회
        """
        recording = self.get_object()
        serializer = RecordingSessionAnalysisSerializer(recording)
        return Response(serializer.data)

    @action(detail=True, methods=['post'], url_path='convert-to-task')
    def convert_to_task(self, request, pk=None):
        """
        POST /api/recordings/{id}/convert-to-task/
        분석된 녹화를 과제(Task)로 변환 (동기 - DB 작업만 수행)

        Request Body:
        {
            "title": "과제 제목",
            "description": "과제 설명 (선택)",
            "lecture_id": 1  // 연결할 강의 ID (선택)
        }
        """
        from .services import TaskConversionService

        recording = self.get_object()

        # 상태 확인: ANALYZED만 변환 가능
        if recording.status != 'ANALYZED':
            return Response(
                {'error': f'분석이 완료된 녹화만 변환할 수 있습니다. 현재 상태: {recording.status}'},
                status=status.HTTP_400_BAD_REQUEST
            )

        # 이미 변환된 과제가 있는지 확인
        if recording.task:
            return Response(
                {'error': '이미 변환된 과제가 있습니다.', 'task_id': recording.task.id},
                status=status.HTTP_400_BAD_REQUEST
            )

        # 요청 데이터 검증
        serializer = RecordingConvertSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        title = serializer.validated_data['title']
        description = serializer.validated_data.get('description', '')
        lecture_id = serializer.validated_data.get('lecture_id')

        # 동기 변환 (DB 작업만이므로 빠름)
        service = TaskConversionService()
        result = service.convert_to_task(
            recording_session_id=recording.id,
            title=title,
            description=description,
            lecture_id=lecture_id
        )

        if result.get('success'):
            return Response({
                'message': '과제 변환이 완료되었습니다.',
                'task_id': result['task_id'],
                'task_title': result['task_title'],
                'subtask_count': result['subtask_count'],
                'lecture_id': result.get('lecture_id')
            }, status=status.HTTP_201_CREATED)
        else:
            return Response(
                {'error': result.get('error', '변환 실패')},
                status=status.HTTP_400_BAD_REQUEST
            )

    @action(detail=True, methods=['get'])
    def subtasks(self, request, pk=None):
        """
        GET /api/recordings/{id}/subtasks/
        녹화에서 생성된 Subtask 목록 조회

        녹화가 과제(Task)로 변환된 경우에만 Subtask를 반환합니다.
        변환되지 않은 경우 적절한 안내 메시지를 반환합니다.
        """
        recording = self.get_object()

        # 녹화가 과제로 변환되었는지 확인
        if not recording.task:
            # 분석 상태에 따라 다른 메시지 반환
            if recording.status == 'ANALYZED':
                return Response({
                    'recording_id': recording.id,
                    'error': '녹화가 아직 과제로 변환되지 않았습니다.',
                    'message': 'POST /api/recordings/{id}/convert-to-task/ 를 호출하여 과제로 변환하세요.',
                    'recording_status': recording.status,
                    'subtasks': []
                }, status=status.HTTP_404_NOT_FOUND)
            elif recording.status in ['RECORDING', 'COMPLETED']:
                return Response({
                    'recording_id': recording.id,
                    'error': '녹화가 아직 분석되지 않았습니다.',
                    'message': 'POST /api/recordings/{id}/analyze/ 를 먼저 호출하세요.',
                    'recording_status': recording.status,
                    'subtasks': []
                }, status=status.HTTP_404_NOT_FOUND)
            else:
                return Response({
                    'recording_id': recording.id,
                    'error': '녹화가 과제로 변환되지 않았습니다.',
                    'recording_status': recording.status,
                    'subtasks': []
                }, status=status.HTTP_404_NOT_FOUND)

        # 변환된 Task에서 Subtask 조회
        task = recording.task
        subtasks = task.subtasks.all().order_by('order_index')

        serializer = SubtaskSerializer(subtasks, many=True)

        return Response({
            'recording_id': recording.id,
            'task_id': task.id,
            'task_title': task.title,
            'subtask_count': subtasks.count(),
            'subtasks': serializer.data
        })
