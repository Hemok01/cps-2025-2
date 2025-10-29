"""
User Progress Models
"""
from django.db import models
from django.conf import settings
from apps.tasks.models import Subtask
from apps.sessions.models import LectureSession


class UserProgress(models.Model):
    """사용자 진행 상태 모델"""

    STATUS_CHOICES = [
        ('NOT_STARTED', '시작 안함'),
        ('IN_PROGRESS', '진행 중'),
        ('COMPLETED', '완료'),
        ('HELP_NEEDED', '도움 필요'),
    ]

    user = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name='progress',
        verbose_name='사용자'
    )
    subtask = models.ForeignKey(
        Subtask,
        on_delete=models.CASCADE,
        related_name='user_progress',
        verbose_name='세부 단계'
    )
    session = models.ForeignKey(
        LectureSession,
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name='progress',
        verbose_name='세션'
    )
    status = models.CharField(
        max_length=20,
        choices=STATUS_CHOICES,
        verbose_name='상태'
    )
    started_at = models.DateTimeField(null=True, blank=True, verbose_name='시작 시각')
    completed_at = models.DateTimeField(null=True, blank=True, verbose_name='완료 시각')
    attempts = models.IntegerField(default=0, verbose_name='시도 횟수')
    help_count = models.IntegerField(default=0, verbose_name='도움 요청 횟수')
    created_at = models.DateTimeField(auto_now_add=True, verbose_name='생성일시')
    updated_at = models.DateTimeField(auto_now=True, verbose_name='수정일시')

    class Meta:
        db_table = 'user_progress'
        verbose_name = '진행 상태'
        verbose_name_plural = '진행 상태'
        unique_together = ['user', 'subtask', 'session']
        indexes = [
            models.Index(fields=['user']),
            models.Index(fields=['subtask']),
            models.Index(fields=['session']),
            models.Index(fields=['status']),
        ]

    def __str__(self):
        return f"{self.user.name} - {self.subtask.title}"
