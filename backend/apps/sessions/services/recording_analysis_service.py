"""
Recording Analysis Service - GPT를 이용한 접근성 로그 분석
mobilegpt2의 Flask 서버 로직을 Django로 이식
"""
import json
import logging
from typing import List, Dict, Optional
from django.conf import settings
from django.utils import timezone
import openai

logger = logging.getLogger(__name__)

# 분석에 사용할 필수 필드 (mobilegpt2에서 이식)
ESSENTIAL_FIELDS = [
    "time",
    "eventType",
    "package",
    "className",
    "text",
    "contentDescription",
    "viewId",
    "bounds"
]


class RecordingAnalysisService:
    """
    녹화된 접근성 이벤트를 GPT로 분석하여 단계(Step)를 생성하는 서비스
    """

    def __init__(self):
        """OpenAI API 초기화"""
        self.api_key = getattr(settings, 'OPENAI_API_KEY', None)
        self.model = getattr(settings, 'OPENAI_MODEL', 'gpt-4o-mini')

        if not self.api_key:
            logger.warning("OpenAI API key is not configured")
        else:
            openai.api_key = self.api_key

    def analyze_recording(self, recording_session_id: int) -> Dict:
        """
        녹화 세션의 이벤트를 분석하여 단계 생성

        Args:
            recording_session_id: RecordingSession ID

        Returns:
            Dict containing:
                - success: bool
                - steps: List of step objects (if success)
                - error: Error message (if failed)
        """
        from apps.sessions.models import RecordingSession
        from apps.logs.models import ActivityLog

        try:
            # 1. RecordingSession 조회
            recording = RecordingSession.objects.get(id=recording_session_id)

            # 2. 상태 업데이트 (PROCESSING)
            recording.status = 'PROCESSING'
            recording.analysis_error = ''
            recording.save(update_fields=['status', 'analysis_error', 'updated_at'])

            # 3. ActivityLog에서 이벤트 조회
            events = ActivityLog.objects.filter(
                recording_session=recording
            ).order_by('timestamp')

            if not events.exists():
                raise ValueError("녹화된 이벤트가 없습니다.")

            # 4. 이벤트 데이터 추출 및 최소화
            minimized_events = self._minimize_events(events)

            if not minimized_events:
                raise ValueError("유효한 이벤트 데이터가 없습니다.")

            # 5. GPT 분석 호출
            steps = self._call_gpt_analysis(minimized_events)

            # 6. 결과 저장
            recording.analysis_result = steps
            recording.analyzed_at = timezone.now()
            recording.status = 'ANALYZED'
            recording.save(update_fields=[
                'analysis_result', 'analyzed_at', 'status', 'updated_at'
            ])

            logger.info(f"Recording {recording_session_id} analyzed successfully: {len(steps)} steps")

            return {
                'success': True,
                'steps': steps,
                'step_count': len(steps)
            }

        except RecordingSession.DoesNotExist:
            error_msg = f"RecordingSession {recording_session_id} not found"
            logger.error(error_msg)
            return {'success': False, 'error': error_msg}

        except Exception as e:
            error_msg = str(e)
            logger.error(f"Error analyzing recording {recording_session_id}: {error_msg}")

            # 실패 상태 저장
            try:
                recording = RecordingSession.objects.get(id=recording_session_id)
                recording.status = 'FAILED'
                recording.analysis_error = error_msg
                recording.save(update_fields=['status', 'analysis_error', 'updated_at'])
            except Exception:
                pass

            return {'success': False, 'error': error_msg}

    def _minimize_events(self, events) -> List[Dict]:
        """
        이벤트 데이터를 GPT 분석에 필요한 필수 필드만 추출

        Args:
            events: ActivityLog QuerySet

        Returns:
            List of minimized event dictionaries
        """
        minimized = []

        for event in events:
            # ActivityLog 모델에서 필드 추출
            event_data = event.event_data or {}

            minimized_event = {
                'time': int(event.timestamp.timestamp() * 1000) if event.timestamp else 0,
                'eventType': event.event_type,
                'package': event_data.get('package', ''),
                'className': event_data.get('className', ''),
                'text': event_data.get('text', ''),
                'contentDescription': event.content_description or '',
                'viewId': event.view_id_resource_name or '',
                'bounds': event.bounds or ''
            }
            minimized.append(minimized_event)

        return minimized

    def _call_gpt_analysis(self, events: List[Dict]) -> List[Dict]:
        """
        GPT API를 호출하여 이벤트 분석

        Args:
            events: 최소화된 이벤트 목록

        Returns:
            List of step objects
        """
        if not self.api_key:
            raise ValueError("OpenAI API key is not configured")

        # 프롬프트 생성 (mobilegpt2에서 이식)
        prompt = self._build_prompt(events)

        try:
            # OpenAI API 호출
            response = openai.ChatCompletion.create(
                model=self.model,
                messages=[
                    {"role": "user", "content": prompt}
                ],
                temperature=0.0  # 결정적 출력
            )

            # 응답 파싱
            text = response.choices[0].message.content.strip()

            # 마크다운 코드블록 제거 (mobilegpt2 로직)
            if text.startswith("```"):
                text = text.replace("```json", "").replace("```", "").strip()

            # JSON 파싱
            steps = json.loads(text)

            # 유효성 검증
            if not isinstance(steps, list):
                raise ValueError("GPT response is not a list")

            return steps

        except json.JSONDecodeError as e:
            logger.error(f"JSON parsing error: {e}, response: {text[:200]}")
            raise ValueError(f"GPT 응답을 JSON으로 파싱할 수 없습니다: {e}")

        except Exception as e:
            logger.error(f"GPT API call failed: {e}")
            raise

    def _build_prompt(self, events: List[Dict]) -> str:
        """
        GPT 분석용 프롬프트 생성 (mobilegpt2에서 이식)

        Args:
            events: 이벤트 목록

        Returns:
            프롬프트 문자열
        """
        events_json = json.dumps(events, ensure_ascii=False, indent=2)

        prompt = f"""
너는 반드시 JSON 배열만 출력해야 한다.

각 JSON은 다음 필드를 포함해야 한다:
step, title, description, time, eventType, package, className, text, contentDescription, viewId, bounds

아래 이벤트 로그를 보고 사용자의 행동을 분석하여 의미 있는 단계들로 정리하라.
각 단계는 사용자가 수행한 하나의 의미 있는 작업을 나타낸다.
title은 한글로 간결하게 작성하고, description은 해당 단계에서 사용자가 무엇을 했는지 설명하라.

이벤트 로그:
{events_json}
"""
        return prompt

    def get_analysis_status(self, recording_session_id: int) -> Dict:
        """
        분석 상태 조회

        Args:
            recording_session_id: RecordingSession ID

        Returns:
            Dict containing status information
        """
        from apps.sessions.models import RecordingSession

        try:
            recording = RecordingSession.objects.get(id=recording_session_id)

            result = {
                'status': recording.status,
                'analyzed_at': recording.analyzed_at.isoformat() if recording.analyzed_at else None,
                'error': recording.analysis_error or None,
            }

            # 분석 완료 시 결과도 포함
            if recording.status == 'ANALYZED' and recording.analysis_result:
                result['steps'] = recording.analysis_result
                result['step_count'] = len(recording.analysis_result)

            return result

        except RecordingSession.DoesNotExist:
            return {'error': f"RecordingSession {recording_session_id} not found"}
