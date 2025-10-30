"""
Activity Log Models
"""
from django.db import models
from django.conf import settings
from apps.tasks.models import Subtask
from apps.sessions.models import LectureSession


class ActivityLog(models.Model):
    """활동 로그 모델 (AccessibilityService에서 수집한 이벤트)"""

    EVENT_TYPE_CHOICES = [
        ('CLICK', '클릭'),
        ('LONG_CLICK', '길게 누르기'),
        ('SCROLL', '스크롤'),
        ('TEXT_INPUT', '텍스트 입력'),
        ('SCREEN_CHANGE', '화면 전환'),
        ('FOCUS', '포커스'),
        ('SELECTION', '선택'),
    ]

    user = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name='activity_logs',
        verbose_name='사용자'
    )
    subtask = models.ForeignKey(
        Subtask,
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name='activity_logs',
        verbose_name='세부 단계'
    )
    session = models.ForeignKey(
        LectureSession,
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name='activity_logs',
        verbose_name='세션'
    )
    event_type = models.CharField(
        max_length=50,
        choices=EVENT_TYPE_CHOICES,
        verbose_name='이벤트 타입'
    )
    event_data = models.JSONField(blank=True, null=True, verbose_name='이벤트 데이터')
    screen_info = models.JSONField(blank=True, null=True, verbose_name='화면 정보')
    node_info = models.JSONField(blank=True, null=True, verbose_name='노드 정보')
    parent_node_info = models.JSONField(blank=True, null=True, verbose_name='부모 노드 정보')
    view_id_resource_name = models.CharField(
        max_length=255,
        blank=True,
        verbose_name='뷰 ID 리소스 이름'
    )
    content_description = models.TextField(blank=True, verbose_name='콘텐츠 설명')
    is_sensitive_data = models.BooleanField(default=False, verbose_name='민감 정보 여부')
    timestamp = models.DateTimeField(auto_now_add=True, verbose_name='타임스탬프')

    class Meta:
        db_table = 'activity_logs'
        verbose_name = '활동 로그'
        verbose_name_plural = '활동 로그'
        indexes = [
            models.Index(fields=['user']),
            models.Index(fields=['subtask']),
            models.Index(fields=['session']),
            models.Index(fields=['timestamp']),
            models.Index(fields=['view_id_resource_name']),
            models.Index(fields=['event_type']),
            models.Index(fields=['is_sensitive_data']),
        ]
        ordering = ['-timestamp']

    def __str__(self):
        return f"{self.user.name} - {self.event_type} at {self.timestamp}"
