"""
Celery Tasks for Recording Session Analysis
"""
import logging
from celery import shared_task

logger = logging.getLogger(__name__)


@shared_task(bind=True, max_retries=3, default_retry_delay=60)
def analyze_recording_task(self, recording_session_id: int):
    """
    비동기 녹화 분석 태스크

    Args:
        recording_session_id: RecordingSession ID

    Returns:
        Dict with analysis result
    """
    from apps.sessions.services import RecordingAnalysisService

    logger.info(f"Starting analysis task for recording {recording_session_id}")

    try:
        service = RecordingAnalysisService()
        result = service.analyze_recording(recording_session_id)

        if result.get('success'):
            logger.info(
                f"Analysis completed for recording {recording_session_id}: "
                f"{result.get('step_count', 0)} steps generated"
            )
        else:
            logger.error(
                f"Analysis failed for recording {recording_session_id}: "
                f"{result.get('error', 'Unknown error')}"
            )

        return result

    except Exception as exc:
        logger.error(f"Analysis task error for recording {recording_session_id}: {exc}")

        # 재시도 가능한 에러인 경우 재시도
        if self.request.retries < self.max_retries:
            logger.info(f"Retrying analysis task (attempt {self.request.retries + 1})")
            raise self.retry(exc=exc)

        # 최대 재시도 초과 시 실패 상태로 저장
        from apps.sessions.models import RecordingSession
        try:
            recording = RecordingSession.objects.get(id=recording_session_id)
            recording.status = 'FAILED'
            recording.analysis_error = f"분석 실패 (재시도 {self.max_retries}회 초과): {str(exc)}"
            recording.save(update_fields=['status', 'analysis_error', 'updated_at'])
        except Exception:
            pass

        return {'success': False, 'error': str(exc)}


@shared_task
def convert_recording_to_lecture_task(
    recording_session_id: int,
    title: str,
    description: str = ''
):
    """
    비동기 강의 변환 태스크 (필요시 사용)

    Args:
        recording_session_id: RecordingSession ID
        title: 강의 제목
        description: 강의 설명

    Returns:
        Dict with conversion result
    """
    from apps.sessions.services import LectureConversionService

    logger.info(f"Starting lecture conversion for recording {recording_session_id}")

    service = LectureConversionService()
    result = service.convert_to_lecture(recording_session_id, title, description)

    if result.get('success'):
        logger.info(
            f"Lecture conversion completed: Lecture {result.get('lecture_id')}, "
            f"{result.get('subtask_count')} subtasks"
        )
    else:
        logger.error(f"Lecture conversion failed: {result.get('error')}")

    return result
