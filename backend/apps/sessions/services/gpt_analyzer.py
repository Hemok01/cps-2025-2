"""
GPT Analysis Service for Recording Sessions
녹화된 이벤트를 분석하여 학습 단계(Step)를 생성하는 서비스

Ported from MobEdu instructor app's Flask server.py
"""
import json
import logging
from typing import List, Dict, Any, Optional

from django.conf import settings
from openai import OpenAI
from decouple import config

logger = logging.getLogger(__name__)


class GPTAnalyzer:
    """녹화된 이벤트를 분석하여 단계(Step)를 생성하는 GPT 서비스"""

    # 필요한 필드 (Flask server.py의 ESSENTIAL_FIELDS)
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

    def __init__(self):
        api_key = config('OPENAI_API_KEY', default=None)
        if not api_key:
            raise ValueError("OPENAI_API_KEY is not configured in environment variables")
        self.client = OpenAI(api_key=api_key)
        self.model = config('OPENAI_MODEL', default='gpt-4o-mini')

    def _minimize_events(self, events: List[Dict]) -> List[Dict]:
        """이벤트에서 필수 필드만 추출하여 토큰 사용량 최소화"""
        return [
            {k: ev.get(k) for k in self.ESSENTIAL_FIELDS}
            for ev in events
        ]

    def _build_analysis_prompt(self, minimized_events: List[Dict]) -> str:
        """GPT 분석 프롬프트 생성"""
        return f"""
너는 반드시 JSON 배열만 출력해야 한다.

각 JSON은 다음 필드를 포함해야 한다:
step, title, description, time, eventType, package, className, text, contentDescription, viewId, bounds

- step: 단계 번호 (1부터 시작하는 정수)
- title: 사용자가 이해하기 쉬운 단계 제목 (예: "설정 앱 열기", "Wi-Fi 버튼 클릭")
- description: 이 단계에서 무엇을 해야 하는지 상세 설명 (고령자 교육용으로 친절하게)
- 나머지 필드: 해당 이벤트의 원본 값

중요:
- 비슷한 이벤트들을 하나의 의미 있는 단계로 그룹화하라
- 불필요한 중간 이벤트(화면 전환 대기 등)는 제외하라
- 각 단계는 사용자가 실제로 수행해야 할 행동을 나타내야 한다

아래 이벤트 로그를 보고 단계를 생성하라:
{json.dumps(minimized_events, ensure_ascii=False, indent=2)}
"""

    def _parse_gpt_response(self, response_text: str) -> List[Dict]:
        """GPT 응답 파싱"""
        text = response_text.strip()

        # 코드 블록 제거 (GPT가 ```json ... ``` 형식으로 응답할 경우)
        if text.startswith("```"):
            text = text.replace("```json", "").replace("```", "").strip()

        try:
            return json.loads(text)
        except json.JSONDecodeError as e:
            logger.error(f"Failed to parse GPT response: {e}")
            logger.error(f"Response text: {text[:500]}...")
            raise ValueError(f"Invalid JSON response from GPT: {e}")

    def analyze_events(self, events: List[Dict]) -> List[Dict]:
        """
        이벤트 목록을 분석하여 단계 목록 반환

        Args:
            events: ActivityLog에서 가져온 이벤트 목록

        Returns:
            분석된 단계 목록 (step, title, description 등 포함)
        """
        if not events:
            raise ValueError("No events to analyze")

        # 이벤트 최소화 (토큰 절약)
        minimized = self._minimize_events(events)

        # 프롬프트 생성
        prompt = self._build_analysis_prompt(minimized)

        logger.info(f"Analyzing {len(events)} events with GPT ({self.model})...")

        try:
            response = self.client.chat.completions.create(
                model=self.model,
                messages=[{"role": "user", "content": prompt}],
                temperature=0.0
            )

            response_text = response.choices[0].message.content
            steps = self._parse_gpt_response(response_text)

            logger.info(f"GPT analysis completed: {len(steps)} steps generated")
            return steps

        except Exception as e:
            logger.error(f"GPT analysis failed: {e}")
            raise

    def format_events_from_activity_logs(self, activity_logs) -> List[Dict]:
        """
        ActivityLog QuerySet을 GPT 분석용 이벤트 형식으로 변환

        Args:
            activity_logs: ActivityLog QuerySet

        Returns:
            GPT 분석용 이벤트 목록
        """
        events = []
        for log in activity_logs:
            event_data = log.event_data or {}
            event = {
                'time': int(log.timestamp.timestamp() * 1000) if log.timestamp else None,
                'eventType': log.event_type or event_data.get('eventType', ''),
                'package': event_data.get('package', ''),
                'className': event_data.get('className', ''),
                'text': event_data.get('text', ''),
                'contentDescription': log.content_description or event_data.get('contentDescription', ''),
                'viewId': log.view_id_resource_name or event_data.get('viewId', ''),
                'bounds': log.bounds or event_data.get('bounds', ''),
            }
            events.append(event)
        return events


# Singleton instance
_analyzer_instance: Optional[GPTAnalyzer] = None


def get_gpt_analyzer() -> GPTAnalyzer:
    """GPT Analyzer 싱글톤 인스턴스 반환"""
    global _analyzer_instance
    if _analyzer_instance is None:
        _analyzer_instance = GPTAnalyzer()
    return _analyzer_instance
