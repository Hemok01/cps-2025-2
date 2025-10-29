"""
Help Request Models
"""
from django.db import models
from django.conf import settings
from apps.tasks.models import Subtask
from apps.sessions.models import LectureSession


class HelpRequest(models.Model):
    """도움 요청 모델"""

    REQUEST_TYPE_CHOICES = [
        ('MANUAL', '수동 요청'),
        ('AUTO', '자동 감지'),
    ]

    STATUS_CHOICES = [
        ('PENDING', '대기 중'),
        ('ANALYZING', '분석 중'),
        ('RESOLVED', '해결됨'),
    ]

    user = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name='help_requests',
        verbose_name='사용자'
    )
    subtask = models.ForeignKey(
        Subtask,
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name='help_requests',
        verbose_name='세부 단계'
    )
    session = models.ForeignKey(
        LectureSession,
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name='help_requests',
        verbose_name='세션'
    )
    request_type = models.CharField(
        max_length=20,
        choices=REQUEST_TYPE_CHOICES,
        verbose_name='요청 타입'
    )
    context_data = models.JSONField(blank=True, null=True, verbose_name='컨텍스트 데이터')
    status = models.CharField(
        max_length=20,
        choices=STATUS_CHOICES,
        default='PENDING',
        verbose_name='상태'
    )
    created_at = models.DateTimeField(auto_now_add=True, verbose_name='요청 시각')
    resolved_at = models.DateTimeField(null=True, blank=True, verbose_name='해결 시각')

    class Meta:
        db_table = 'help_requests'
        verbose_name = '도움 요청'
        verbose_name_plural = '도움 요청'
        indexes = [
            models.Index(fields=['user']),
            models.Index(fields=['session']),
            models.Index(fields=['status']),
            models.Index(fields=['created_at']),
        ]
        ordering = ['-created_at']

    def __str__(self):
        return f"{self.user.name} - {self.request_type} at {self.created_at}"


class MGptAnalysis(models.Model):
    """M-GPT 분석 결과 모델"""

    help_request = models.OneToOneField(
        HelpRequest,
        on_delete=models.CASCADE,
        related_name='mgpt_analysis',
        verbose_name='도움 요청'
    )
    analysis_input = models.JSONField(verbose_name='분석 입력')
    analysis_output = models.JSONField(verbose_name='분석 출력')
    problem_diagnosis = models.TextField(blank=True, verbose_name='문제 진단')
    suggested_help = models.TextField(blank=True, verbose_name='추천 도움말')
    confidence_score = models.FloatField(null=True, blank=True, verbose_name='신뢰도')
    created_at = models.DateTimeField(auto_now_add=True, verbose_name='분석 시각')

    class Meta:
        db_table = 'mgpt_analyses'
        verbose_name = 'M-GPT 분석'
        verbose_name_plural = 'M-GPT 분석'
        indexes = [
            models.Index(fields=['help_request']),
        ]

    def __str__(self):
        return f"Analysis for {self.help_request.id}"


class HelpResponse(models.Model):
    """제공된 도움 모델"""

    HELP_TYPE_CHOICES = [
        ('TEXT', '텍스트'),
        ('VOICE', '음성'),
        ('OVERLAY', '오버레이'),
        ('VIDEO', '비디오'),
    ]

    help_request = models.ForeignKey(
        HelpRequest,
        on_delete=models.CASCADE,
        related_name='responses',
        verbose_name='도움 요청'
    )
    mgpt_analysis = models.ForeignKey(
        MGptAnalysis,
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name='responses',
        verbose_name='M-GPT 분석'
    )
    help_type = models.CharField(
        max_length=20,
        choices=HELP_TYPE_CHOICES,
        verbose_name='도움 타입'
    )
    help_content = models.TextField(verbose_name='도움 내용')
    displayed_at = models.DateTimeField(auto_now_add=True, verbose_name='표시 시각')
    feedback_rating = models.IntegerField(null=True, blank=True, verbose_name='평점')
    feedback_text = models.TextField(blank=True, verbose_name='피드백 텍스트')
    feedback_at = models.DateTimeField(null=True, blank=True, verbose_name='피드백 시각')

    class Meta:
        db_table = 'help_responses'
        verbose_name = '도움 응답'
        verbose_name_plural = '도움 응답'
        indexes = [
            models.Index(fields=['help_request']),
        ]

    def __str__(self):
        return f"Response for {self.help_request.id}"
