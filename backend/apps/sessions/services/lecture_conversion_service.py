"""
Lecture Conversion Service - 분석된 Step을 Lecture/Task/Subtask로 변환
"""
import logging
from typing import Dict, Optional
from django.db import transaction

logger = logging.getLogger(__name__)

# eventType → target_action 매핑
EVENT_TYPE_MAPPING = {
    '1': 'CLICK',           # TYPE_VIEW_CLICKED
    '2': 'LONG_CLICK',      # TYPE_VIEW_LONG_CLICKED
    '4': 'CLICK',           # TYPE_VIEW_SELECTED
    '8': 'NAVIGATE',        # TYPE_VIEW_FOCUSED
    '16': 'INPUT',          # TYPE_VIEW_TEXT_CHANGED
    '32': 'NAVIGATE',       # TYPE_WINDOW_STATE_CHANGED
    '64': 'NAVIGATE',       # TYPE_NOTIFICATION_STATE_CHANGED
    '128': 'SCROLL',        # TYPE_VIEW_SCROLLED
    '256': 'INPUT',         # TYPE_VIEW_TEXT_SELECTION_CHANGED
    '2048': 'NAVIGATE',     # TYPE_WINDOWS_CHANGED
    '4096': 'SCROLL',       # TYPE_VIEW_SCROLLED (alternate)
    'CLICK': 'CLICK',
    'LONG_CLICK': 'LONG_CLICK',
    'SCROLL': 'SCROLL',
    'INPUT': 'INPUT',
    'TEXT_INPUT': 'INPUT',
    'NAVIGATE': 'NAVIGATE',
    'SCREEN_CHANGE': 'NAVIGATE',
}


class LectureConversionService:
    """
    분석된 녹화 결과를 Lecture/Task/Subtask 구조로 변환하는 서비스
    """

    def convert_to_lecture(
        self,
        recording_session_id: int,
        title: str,
        description: str = ''
    ) -> Dict:
        """
        녹화 분석 결과를 강의로 변환

        Args:
            recording_session_id: RecordingSession ID
            title: 강의 제목
            description: 강의 설명 (선택)

        Returns:
            Dict containing:
                - success: bool
                - lecture_id: Created Lecture ID (if success)
                - task_id: Created Task ID (if success)
                - subtask_count: Number of Subtasks created (if success)
                - error: Error message (if failed)
        """
        from apps.sessions.models import RecordingSession
        from apps.lectures.models import Lecture
        from apps.tasks.models import Task, Subtask

        try:
            # 1. RecordingSession 조회
            recording = RecordingSession.objects.get(id=recording_session_id)

            # 2. 분석 완료 상태 확인
            if recording.status != 'ANALYZED':
                raise ValueError(f"녹화 분석이 완료되지 않았습니다. 현재 상태: {recording.status}")

            if not recording.analysis_result:
                raise ValueError("분석 결과가 없습니다.")

            steps = recording.analysis_result

            if not isinstance(steps, list) or len(steps) == 0:
                raise ValueError("유효한 분석 결과가 없습니다.")

            # 3. 트랜잭션으로 Lecture, Task, Subtask 생성
            with transaction.atomic():
                # Lecture 생성
                lecture = Lecture.objects.create(
                    instructor=recording.instructor,
                    title=title,
                    description=description or recording.description,
                    is_active=True
                )

                # 단일 Task 생성 (계획대로)
                task = Task.objects.create(
                    lecture=lecture,
                    title=title,
                    description=f"{recording.title} 녹화에서 생성됨",
                    order_index=0
                )

                # 각 step을 Subtask로 변환
                subtasks_created = []
                for idx, step in enumerate(steps):
                    subtask = self._create_subtask_from_step(task, step, idx)
                    subtasks_created.append(subtask)

                # RecordingSession에 생성된 강의 연결
                recording.lecture = lecture
                recording.save(update_fields=['lecture', 'updated_at'])

            logger.info(
                f"Recording {recording_session_id} converted to Lecture {lecture.id}: "
                f"1 Task, {len(subtasks_created)} Subtasks"
            )

            return {
                'success': True,
                'lecture_id': lecture.id,
                'task_id': task.id,
                'subtask_count': len(subtasks_created),
                'lecture_title': lecture.title
            }

        except RecordingSession.DoesNotExist:
            error_msg = f"RecordingSession {recording_session_id} not found"
            logger.error(error_msg)
            return {'success': False, 'error': error_msg}

        except Exception as e:
            error_msg = str(e)
            logger.error(f"Error converting recording {recording_session_id}: {error_msg}")
            return {'success': False, 'error': error_msg}

    def _create_subtask_from_step(self, task, step: Dict, index: int):
        """
        분석된 step을 Subtask로 변환

        Args:
            task: 부모 Task 객체
            step: GPT 분석 결과의 step 딕셔너리
            index: 순서 인덱스

        Returns:
            생성된 Subtask 객체
        """
        from apps.tasks.models import Subtask

        # eventType을 target_action으로 매핑
        event_type = str(step.get('eventType', ''))
        target_action = EVENT_TYPE_MAPPING.get(event_type, 'CLICK')

        # target_element_hint 생성 (viewId + bounds)
        view_id = step.get('viewId', '') or ''
        bounds = step.get('bounds', '') or ''
        target_element_hint = f"{view_id}"
        if bounds:
            target_element_hint += f" [{bounds}]"

        # guide_text 생성
        text = step.get('text', '') or ''
        description = step.get('description', '') or ''
        guide_text = description if description else text

        # voice_guide_text 생성
        content_desc = step.get('contentDescription', '') or ''
        voice_guide_text = content_desc if content_desc else guide_text

        subtask = Subtask.objects.create(
            task=task,
            title=step.get('title', f'단계 {index + 1}'),
            description=description,
            order_index=index,
            target_action=target_action,
            target_element_hint=target_element_hint.strip(),
            guide_text=guide_text,
            voice_guide_text=voice_guide_text,
            # Flask 원본 서버 동기화 필드
            time=step.get('time'),
            text=step.get('text', ''),
            content_description=step.get('contentDescription', ''),
            view_id=step.get('viewId', ''),
            bounds=step.get('bounds', ''),
            target_package=step.get('package', ''),
            target_class=step.get('className', ''),
        )

        return subtask

    def update_analysis_result(
        self,
        recording_session_id: int,
        steps: list
    ) -> Dict:
        """
        분석 결과(steps) 수정

        Args:
            recording_session_id: RecordingSession ID
            steps: 수정된 step 목록

        Returns:
            Dict containing success status
        """
        from apps.sessions.models import RecordingSession

        try:
            recording = RecordingSession.objects.get(id=recording_session_id)

            if recording.status not in ['ANALYZED', 'COMPLETED']:
                raise ValueError(f"분석 결과를 수정할 수 없는 상태입니다: {recording.status}")

            recording.analysis_result = steps
            recording.save(update_fields=['analysis_result', 'updated_at'])

            return {
                'success': True,
                'step_count': len(steps)
            }

        except RecordingSession.DoesNotExist:
            return {'success': False, 'error': f"RecordingSession {recording_session_id} not found"}

        except Exception as e:
            return {'success': False, 'error': str(e)}
