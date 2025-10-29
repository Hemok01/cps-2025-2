"""
M-GPT Service for AI-powered help analysis using OpenAI API
"""
import openai
from django.conf import settings
from typing import Dict, Optional, List
import json
import logging

logger = logging.getLogger(__name__)


class MGptService:
    """
    Mobile GPT Service for analyzing student difficulties
    and providing personalized help using OpenAI API
    """

    def __init__(self):
        """Initialize OpenAI API with settings"""
        self.api_key = settings.OPENAI_API_KEY
        self.model = settings.OPENAI_MODEL

        if not self.api_key:
            logger.warning("OpenAI API key is not configured")
        else:
            openai.api_key = self.api_key

    def analyze_help_request(
        self,
        subtask_title: str,
        subtask_description: str,
        target_action: str,
        user_digital_level: str,
        activity_logs: Optional[List[Dict]] = None,
        error_message: Optional[str] = None
    ) -> Dict:
        """
        Analyze a help request and provide diagnosis and suggestions

        Args:
            subtask_title: Title of the subtask student is stuck on
            subtask_description: Detailed description of the subtask
            target_action: Expected action (CLICK, SCROLL, INPUT, etc.)
            user_digital_level: Student's digital literacy level
            activity_logs: Recent activity logs (optional)
            error_message: Any error message from the app (optional)

        Returns:
            Dict containing:
                - problem_diagnosis: Analysis of what went wrong
                - step_by_step_solution: Detailed steps to solve the problem
                - alternative_approaches: Alternative ways to complete the task
                - confidence_score: Confidence in the diagnosis (0-100)
                - estimated_difficulty: Estimated difficulty of the problem
        """
        if not self.api_key:
            return self._get_fallback_response()

        try:
            # Build the prompt for GPT
            prompt = self._build_analysis_prompt(
                subtask_title=subtask_title,
                subtask_description=subtask_description,
                target_action=target_action,
                user_digital_level=user_digital_level,
                activity_logs=activity_logs,
                error_message=error_message
            )

            # Call OpenAI API
            response = openai.ChatCompletion.create(
                model=self.model,
                messages=[
                    {
                        "role": "system",
                        "content": self._get_system_prompt()
                    },
                    {
                        "role": "user",
                        "content": prompt
                    }
                ],
                temperature=0.7,
                max_tokens=1000,
                response_format={"type": "json_object"}
            )

            # Parse response
            result = json.loads(response.choices[0].message.content)

            # Validate and normalize response
            return self._normalize_response(result)

        except Exception as e:
            logger.error(f"Error analyzing help request with M-GPT: {e}")
            return self._get_fallback_response()

    def _get_system_prompt(self) -> str:
        """Get the system prompt for M-GPT"""
        return """당신은 시니어를 위한 디지털 교육 전문 AI 도우미입니다.
학생들이 스마트폰이나 앱 사용 중 겪는 어려움을 분석하고,
이해하기 쉽고 따라하기 쉬운 해결 방법을 제공합니다.

응답은 반드시 다음 JSON 형식으로 제공해야 합니다:
{
  "problem_diagnosis": "문제 분석 (한글로 간단명료하게)",
  "step_by_step_solution": [
    "1단계: ...",
    "2단계: ...",
    "3단계: ..."
  ],
  "alternative_approaches": [
    "대안 1: ...",
    "대안 2: ..."
  ],
  "confidence_score": 85,
  "estimated_difficulty": "중급"
}

주의사항:
- 시니어가 이해하기 쉬운 용어 사용
- 구체적이고 단계별 설명
- 긍정적이고 격려하는 톤
- 전문 용어는 쉽게 풀어서 설명
"""

    def _build_analysis_prompt(
        self,
        subtask_title: str,
        subtask_description: str,
        target_action: str,
        user_digital_level: str,
        activity_logs: Optional[List[Dict]] = None,
        error_message: Optional[str] = None
    ) -> str:
        """Build the analysis prompt for GPT"""

        action_descriptions = {
            'CLICK': '클릭 (터치)',
            'LONG_CLICK': '길게 누르기',
            'SCROLL': '화면 스크롤',
            'INPUT': '텍스트 입력',
            'NAVIGATE': '화면 이동',
        }

        action_desc = action_descriptions.get(target_action, target_action)

        prompt = f"""
학생이 다음 단계에서 도움을 요청했습니다:

**단계 제목**: {subtask_title}
**단계 설명**: {subtask_description}
**예상 동작**: {action_desc}
**학생의 디지털 수준**: {user_digital_level}
"""

        if error_message:
            prompt += f"\n**에러 메시지**: {error_message}"

        if activity_logs:
            prompt += "\n\n**최근 활동 로그**:\n"
            for log in activity_logs[-5:]:  # Last 5 logs
                event_type = log.get('event_type', 'UNKNOWN')
                prompt += f"- {event_type}: {log.get('view_id_resource_name', 'N/A')}\n"

        prompt += """

위 정보를 바탕으로 학생이 겪고 있는 문제를 분석하고,
해결 방법을 제시해주세요. JSON 형식으로 응답해주세요.
"""

        return prompt

    def _normalize_response(self, result: Dict) -> Dict:
        """Normalize and validate the response from GPT"""
        return {
            'problem_diagnosis': result.get('problem_diagnosis', '문제를 분석 중입니다.'),
            'step_by_step_solution': result.get('step_by_step_solution', []),
            'alternative_approaches': result.get('alternative_approaches', []),
            'confidence_score': min(100, max(0, int(result.get('confidence_score', 70)))),
            'estimated_difficulty': result.get('estimated_difficulty', '중급'),
        }

    def _get_fallback_response(self) -> Dict:
        """Get fallback response when API is not available"""
        return {
            'problem_diagnosis': '현재 AI 분석 서비스를 이용할 수 없습니다. 강사에게 직접 문의해주세요.',
            'step_by_step_solution': [
                '1. 화면을 천천히 확인해보세요',
                '2. 이전 단계로 돌아가 다시 시도해보세요',
                '3. 강사에게 도움을 요청하세요'
            ],
            'alternative_approaches': [
                '강사에게 직접 문의하기',
                '동료 학생에게 물어보기'
            ],
            'confidence_score': 50,
            'estimated_difficulty': '알 수 없음',
        }

    def generate_encouragement(
        self,
        user_name: str,
        progress_rate: float,
        completed_subtasks: int
    ) -> str:
        """
        Generate personalized encouragement message

        Args:
            user_name: Student's name
            progress_rate: Completion percentage (0-100)
            completed_subtasks: Number of completed subtasks

        Returns:
            Encouragement message
        """
        if not self.api_key:
            return self._get_fallback_encouragement(user_name, progress_rate)

        try:
            prompt = f"""
{user_name}님의 학습 진행 상황:
- 진행률: {progress_rate:.1f}%
- 완료한 단계: {completed_subtasks}개

위 정보를 바탕으로 격려와 동기부여가 되는 짧은 메시지를 작성해주세요.
(2-3문장, 친근하고 따뜻한 톤)
"""

            response = openai.ChatCompletion.create(
                model=self.model,
                messages=[
                    {
                        "role": "system",
                        "content": "당신은 시니어 학습자를 격려하는 친절한 강사입니다."
                    },
                    {
                        "role": "user",
                        "content": prompt
                    }
                ],
                temperature=0.8,
                max_tokens=150
            )

            return response.choices[0].message.content.strip()

        except Exception as e:
            logger.error(f"Error generating encouragement: {e}")
            return self._get_fallback_encouragement(user_name, progress_rate)

    def _get_fallback_encouragement(self, user_name: str, progress_rate: float) -> str:
        """Get fallback encouragement message"""
        if progress_rate < 25:
            return f"{user_name}님, 좋은 시작입니다! 천천히 하나씩 배워가세요."
        elif progress_rate < 50:
            return f"{user_name}님, 벌써 반의 반을 달성하셨네요! 잘하고 계십니다!"
        elif progress_rate < 75:
            return f"{user_name}님, 절반 이상 완료하셨어요! 조금만 더 힘내세요!"
        else:
            return f"{user_name}님, 거의 다 완료하셨네요! 정말 훌륭합니다!"

    def summarize_common_issues(
        self,
        help_requests: List[Dict]
    ) -> Dict:
        """
        Analyze common issues from multiple help requests

        Args:
            help_requests: List of help request data

        Returns:
            Dict with common patterns and recommendations
        """
        if not self.api_key or not help_requests:
            return {
                'common_patterns': [],
                'recommendations': [],
                'difficult_subtasks': []
            }

        try:
            # Prepare summary of help requests
            summary = []
            for req in help_requests[:10]:  # Limit to 10 most recent
                summary.append({
                    'subtask': req.get('subtask_title', 'Unknown'),
                    'issue': req.get('problem_diagnosis', 'Unknown')
                })

            prompt = f"""
다음은 최근 학생들의 도움 요청 내역입니다:

{json.dumps(summary, ensure_ascii=False, indent=2)}

위 데이터를 분석하여 다음을 JSON 형식으로 제공해주세요:
{{
  "common_patterns": ["패턴 1", "패턴 2"],
  "recommendations": ["추천사항 1", "추천사항 2"],
  "difficult_subtasks": ["어려운 단계 1", "어려운 단계 2"]
}}
"""

            response = openai.ChatCompletion.create(
                model=self.model,
                messages=[
                    {
                        "role": "system",
                        "content": "당신은 교육 데이터를 분석하는 전문가입니다."
                    },
                    {
                        "role": "user",
                        "content": prompt
                    }
                ],
                temperature=0.7,
                max_tokens=500,
                response_format={"type": "json_object"}
            )

            return json.loads(response.choices[0].message.content)

        except Exception as e:
            logger.error(f"Error summarizing common issues: {e}")
            return {
                'common_patterns': [],
                'recommendations': [],
                'difficult_subtasks': []
            }
