"""
Recording Session Views (녹화 기능)
Extended with GPT analysis, step editing, and lecture conversion
"""
import logging
from rest_framework import viewsets, status
from rest_framework.decorators import action
from rest_framework.response import Response
from rest_framework.permissions import IsAuthenticated
from django.utils import timezone
from django.db import transaction

from .models import RecordingSession, RecordingStep
from .serializers import (
    RecordingSessionSerializer,
    RecordingSessionCreateSerializer,
    RecordingSessionListSerializer,
    RecordingSessionDetailSerializer,
    RecordingStepSerializer,
    ConvertToLectureSerializer,
)
from apps.logs.models import ActivityLog
from apps.logs.serializers import ActivityLogSerializer
from apps.lectures.models import Lecture
from apps.tasks.models import Task, Subtask

logger = logging.getLogger(__name__)


class RecordingSessionViewSet(viewsets.ModelViewSet):
    """
    녹화 세션 ViewSet

    Extended with:
    - GPT analysis (analyze action)
    - Step CRUD (steps, update_steps, update_single_step, delete_step actions)
    - Lecture conversion (convert_to_lecture action)
    """
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        """강의자 본인의 녹화 세션만 조회 가능"""
        return RecordingSession.objects.filter(
            instructor=self.request.user
        ).select_related('instructor', 'lecture').prefetch_related('steps')

    def get_serializer_class(self):
        """액션에 따라 적절한 serializer 반환"""
        if self.action == 'list':
            return RecordingSessionListSerializer
        elif self.action == 'create':
            return RecordingSessionCreateSerializer
        elif self.action == 'retrieve':
            return RecordingSessionDetailSerializer
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

    # ================================================================
    # GPT 분석 액션
    # ================================================================
    @action(detail=True, methods=['post'])
    def analyze(self, request, pk=None):
        """
        POST /api/sessions/recordings/{id}/analyze/ - GPT 분석 실행

        녹화된 이벤트를 GPT로 분석하여 RecordingStep 생성
        """
        recording = self.get_object()

        # 분석 가능한 상태인지 확인
        if recording.status not in ['COMPLETED', 'FAILED', 'ANALYZED']:
            return Response(
                {'error': f'분석 가능한 상태가 아닙니다. 현재 상태: {recording.status}'},
                status=status.HTTP_400_BAD_REQUEST
            )

        # 녹화된 이벤트 조회
        activity_logs = ActivityLog.objects.filter(
            recording_session=recording
        ).order_by('timestamp')

        if not activity_logs.exists():
            return Response(
                {'error': '분석할 이벤트가 없습니다.'},
                status=status.HTTP_400_BAD_REQUEST
            )

        # 분석 중 상태로 변경
        recording.status = 'PROCESSING'
        recording.analysis_error = ''
        recording.save(update_fields=['status', 'analysis_error'])

        try:
            # GPT 분석 실행
            from .services.gpt_analyzer import get_gpt_analyzer

            analyzer = get_gpt_analyzer()
            events = analyzer.format_events_from_activity_logs(activity_logs)
            analyzed_steps = analyzer.analyze_events(events)

            # 기존 단계 삭제 및 새로 생성
            with transaction.atomic():
                RecordingStep.objects.filter(recording_session=recording).delete()

                created_steps = []
                for step_data in analyzed_steps:
                    step = RecordingStep.objects.create(
                        recording_session=recording,
                        step_number=step_data.get('step', len(created_steps) + 1),
                        title=step_data.get('title', ''),
                        description=step_data.get('description', ''),
                        event_time=step_data.get('time'),
                        event_type=str(step_data.get('eventType', '')),
                        package_name=step_data.get('package', ''),
                        class_name=step_data.get('className', ''),
                        text=str(step_data.get('text', '')),
                        content_description=step_data.get('contentDescription') or '',
                        view_id=step_data.get('viewId') or '',
                        bounds=step_data.get('bounds') or '',
                    )
                    created_steps.append(step)

                recording.status = 'ANALYZED'
                recording.save(update_fields=['status'])

            return Response({
                'message': f'{len(created_steps)}개의 단계가 생성되었습니다.',
                'step_count': len(created_steps),
                'steps': RecordingStepSerializer(created_steps, many=True).data,
                'recording': RecordingSessionDetailSerializer(recording).data
            })

        except Exception as e:
            logger.error(f"GPT analysis failed for recording {recording.id}: {e}")
            recording.status = 'FAILED'
            recording.analysis_error = str(e)
            recording.save(update_fields=['status', 'analysis_error'])

            return Response(
                {'error': f'분석 실패: {str(e)}'},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )

    # ================================================================
    # Step 조회/수정 액션
    # ================================================================
    @action(detail=True, methods=['get'], url_path='steps')
    def steps(self, request, pk=None):
        """
        GET /api/sessions/recordings/{id}/steps/ - 녹화 단계 목록 조회
        """
        recording = self.get_object()
        steps = recording.steps.all().order_by('step_number')
        serializer = RecordingStepSerializer(steps, many=True)
        return Response({
            'recording_id': recording.id,
            'steps': serializer.data
        })

    @action(detail=True, methods=['post'], url_path='steps/update')
    def update_steps(self, request, pk=None):
        """
        POST /api/sessions/recordings/{id}/steps/update/ - 단계 일괄 수정

        Request Body:
        {
            "steps": [
                {"title": "...", "description": "...", "text": "...", ...},
                ...
            ]
        }
        """
        recording = self.get_object()
        steps_data = request.data.get('steps', [])

        if not steps_data:
            return Response(
                {'error': 'steps 배열이 필요합니다.'},
                status=status.HTTP_400_BAD_REQUEST
            )

        updated_steps = []
        with transaction.atomic():
            # 기존 단계 삭제
            RecordingStep.objects.filter(recording_session=recording).delete()

            # 새 순서대로 단계 생성
            for i, step_data in enumerate(steps_data, start=1):
                step = RecordingStep.objects.create(
                    recording_session=recording,
                    step_number=i,
                    title=step_data.get('title', ''),
                    description=step_data.get('description', ''),
                    event_time=step_data.get('event_time'),
                    event_type=step_data.get('event_type', ''),
                    package_name=step_data.get('package_name', ''),
                    class_name=step_data.get('class_name', ''),
                    text=step_data.get('text', ''),
                    content_description=step_data.get('content_description', ''),
                    view_id=step_data.get('view_id', ''),
                    bounds=step_data.get('bounds', ''),
                )
                updated_steps.append(step)

        return Response({
            'message': f'{len(updated_steps)}개의 단계가 업데이트되었습니다.',
            'steps': RecordingStepSerializer(updated_steps, many=True).data
        })

    @action(detail=True, methods=['post'], url_path='steps/(?P<step_index>[0-9]+)/update')
    def update_single_step(self, request, pk=None, step_index=None):
        """
        POST /api/sessions/recordings/{id}/steps/{step_index}/update/ - 개별 단계 수정

        step_index는 0-based index
        """
        recording = self.get_object()
        step_index = int(step_index)

        steps = list(recording.steps.all().order_by('step_number'))

        if step_index < 0 or step_index >= len(steps):
            return Response(
                {'error': '잘못된 단계 인덱스입니다.'},
                status=status.HTTP_400_BAD_REQUEST
            )

        step = steps[step_index]

        # 수정 가능한 필드만 업데이트
        title = request.data.get('title')
        description = request.data.get('description')
        text = request.data.get('text')

        if title is not None:
            step.title = title
        if description is not None:
            step.description = description
        if text is not None:
            step.text = text

        step.save()

        return Response({
            'message': '단계가 수정되었습니다.',
            'step': RecordingStepSerializer(step).data
        })

    @action(detail=True, methods=['post'], url_path='steps/(?P<step_index>[0-9]+)/delete')
    def delete_step(self, request, pk=None, step_index=None):
        """
        POST /api/sessions/recordings/{id}/steps/{step_index}/delete/ - 단계 삭제

        삭제 후 나머지 단계들의 step_number 재정렬
        """
        recording = self.get_object()
        step_index = int(step_index)

        steps = list(recording.steps.all().order_by('step_number'))

        if step_index < 0 or step_index >= len(steps):
            return Response(
                {'error': '잘못된 단계 인덱스입니다.'},
                status=status.HTTP_400_BAD_REQUEST
            )

        with transaction.atomic():
            # 단계 삭제
            steps[step_index].delete()

            # 나머지 단계들 재정렬
            remaining_steps = recording.steps.all().order_by('step_number')
            for i, step in enumerate(remaining_steps, start=1):
                if step.step_number != i:
                    step.step_number = i
                    step.save(update_fields=['step_number'])

        return Response({
            'message': '단계가 삭제되었습니다.',
            'steps': RecordingStepSerializer(
                recording.steps.all().order_by('step_number'),
                many=True
            ).data
        })

    # ================================================================
    # Lecture 변환 액션
    # ================================================================
    @action(detail=True, methods=['post'], url_path='convert-to-lecture')
    def convert_to_lecture(self, request, pk=None):
        """
        POST /api/sessions/recordings/{id}/convert-to-lecture/ - 강의로 변환

        RecordingStep들을 Lecture -> Task -> Subtask 구조로 변환

        Request Body:
        {
            "lecture_title": "강의 제목",
            "lecture_description": "강의 설명 (선택)",
            "task_title": "과제 제목 (선택, 기본값: 녹화 제목)"
        }
        """
        recording = self.get_object()

        # 분석 완료 상태인지 확인
        if recording.status not in ['ANALYZED', 'CONVERTED']:
            return Response(
                {'error': f'강의 변환이 가능한 상태가 아닙니다. 현재 상태: {recording.status}'},
                status=status.HTTP_400_BAD_REQUEST
            )

        steps = recording.steps.all().order_by('step_number')
        if not steps.exists():
            return Response(
                {'error': '변환할 단계가 없습니다. 먼저 분석을 실행하세요.'},
                status=status.HTTP_400_BAD_REQUEST
            )

        serializer = ConvertToLectureSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        lecture_title = serializer.validated_data['lecture_title']
        lecture_description = serializer.validated_data.get('lecture_description', '')
        task_title = serializer.validated_data.get('task_title') or recording.title

        try:
            with transaction.atomic():
                # 1. Lecture 생성
                lecture = Lecture.objects.create(
                    instructor=request.user,
                    title=lecture_title,
                    description=lecture_description,
                    is_active=True
                )

                # 2. Task 생성 (녹화당 하나의 Task)
                task = Task.objects.create(
                    lecture=lecture,
                    title=task_title,
                    description=f'{recording.title} 녹화에서 변환됨',
                    order_index=1
                )

                # 3. RecordingStep -> Subtask 변환
                created_subtasks = []
                for step in steps:
                    # eventType을 target_action으로 매핑
                    action_mapping = {
                        'CLICK': 'CLICK',
                        'VIEW_CLICKED': 'CLICK',
                        '1': 'CLICK',  # TYPE_VIEW_CLICKED
                        'LONG_CLICK': 'LONG_CLICK',
                        '2': 'LONG_CLICK',
                        'SCROLL': 'SCROLL',
                        'VIEW_SCROLLED': 'SCROLL',
                        '4096': 'SCROLL',  # TYPE_VIEW_SCROLLED
                        'TEXT_INPUT': 'INPUT',
                        'VIEW_TEXT_CHANGED': 'INPUT',
                        '16': 'INPUT',  # TYPE_VIEW_TEXT_CHANGED
                        'SCREEN_CHANGE': 'NAVIGATE',
                        'WINDOW_STATE_CHANGED': 'NAVIGATE',
                        '32': 'NAVIGATE',  # TYPE_WINDOW_STATE_CHANGED
                        '2048': 'NAVIGATE',  # TYPE_WINDOW_CONTENT_CHANGED
                    }
                    target_action = action_mapping.get(str(step.event_type), 'CLICK')

                    # target_element_hint 구성
                    element_hints = []
                    if step.view_id:
                        element_hints.append(f"viewId: {step.view_id}")
                    if step.content_description:
                        element_hints.append(f"contentDescription: {step.content_description}")
                    if step.text:
                        element_hints.append(f"text: {step.text}")
                    if step.bounds:
                        element_hints.append(f"bounds: {step.bounds}")

                    subtask = Subtask.objects.create(
                        task=task,
                        title=step.title,
                        description=step.description,
                        order_index=step.step_number,
                        target_action=target_action,
                        target_element_hint='\n'.join(element_hints),
                        guide_text=step.description,
                        voice_guide_text=step.description,
                    )

                    # RecordingStep에 연결된 subtask 저장
                    step.subtask = subtask
                    step.save(update_fields=['subtask'])

                    created_subtasks.append(subtask)

                # Recording 상태 및 연결된 lecture 업데이트
                recording.status = 'CONVERTED'
                recording.lecture = lecture
                recording.save(update_fields=['status', 'lecture'])

            # Serializer import (circular import 방지)
            from apps.lectures.serializers import LectureSerializer
            from apps.tasks.serializers import TaskDetailSerializer

            return Response({
                'message': f'강의가 생성되었습니다. {len(created_subtasks)}개의 단계가 변환되었습니다.',
                'lecture': LectureSerializer(lecture).data,
                'task': TaskDetailSerializer(task).data,
                'recording': RecordingSessionDetailSerializer(recording).data
            }, status=status.HTTP_201_CREATED)

        except Exception as e:
            logger.error(f"Lecture conversion failed for recording {recording.id}: {e}")
            return Response(
                {'error': f'강의 변환 실패: {str(e)}'},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )
