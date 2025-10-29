"""
Lecture Session Models (실시간 강의방)
"""
from django.db import models
from django.conf import settings
from apps.lectures.models import Lecture
from apps.tasks.models import Subtask
import random
import string


def generate_session_code():
    """Generate unique 6-character session code"""
    chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'  # Exclude similar chars
    return ''.join(random.choice(chars) for _ in range(6))


class LectureSession(models.Model):
    """실시간 강의방 모델"""

    STATUS_CHOICES = [
        ('WAITING', '대기실'),
        ('IN_PROGRESS', '진행 중'),
        ('ENDED', '종료'),
        ('REVIEW_MODE', '복습 모드'),
    ]

    lecture = models.ForeignKey(
        Lecture,
        on_delete=models.CASCADE,
        related_name='sessions',
        verbose_name='강의'
    )
    instructor = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name='teaching_sessions',
        verbose_name='강사'
    )
    title = models.CharField(max_length=255, verbose_name='세션 제목')
    session_code = models.CharField(
        max_length=20,
        unique=True,
        default=generate_session_code,
        verbose_name='입장 코드'
    )
    status = models.CharField(
        max_length=20,
        choices=STATUS_CHOICES,
        default='WAITING',
        verbose_name='상태'
    )
    current_subtask = models.ForeignKey(
        Subtask,
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name='current_in_sessions',
        verbose_name='현재 진행 단계'
    )
    qr_code_url = models.URLField(max_length=500, blank=True, verbose_name='QR 코드 URL')
    scheduled_at = models.DateTimeField(null=True, blank=True, verbose_name='예정 시각')
    started_at = models.DateTimeField(null=True, blank=True, verbose_name='시작 시각')
    ended_at = models.DateTimeField(null=True, blank=True, verbose_name='종료 시각')
    created_at = models.DateTimeField(auto_now_add=True, verbose_name='생성일시')
    updated_at = models.DateTimeField(auto_now=True, verbose_name='수정일시')

    class Meta:
        db_table = 'lecture_sessions'
        verbose_name = '강의 세션'
        verbose_name_plural = '강의 세션'
        indexes = [
            models.Index(fields=['lecture']),
            models.Index(fields=['instructor']),
            models.Index(fields=['session_code']),
            models.Index(fields=['status']),
            models.Index(fields=['scheduled_at']),
        ]

    def __str__(self):
        return f"{self.lecture.title} - {self.title}"


class SessionParticipant(models.Model):
    """세션 참가자 모델"""

    STATUS_CHOICES = [
        ('WAITING', '대기 중'),
        ('ACTIVE', '활성'),
        ('COMPLETED', '완료'),
        ('DISCONNECTED', '연결 끊김'),
    ]

    session = models.ForeignKey(
        LectureSession,
        on_delete=models.CASCADE,
        related_name='participants',
        verbose_name='세션'
    )
    user = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name='session_participations',
        verbose_name='사용자'
    )
    status = models.CharField(
        max_length=20,
        choices=STATUS_CHOICES,
        default='WAITING',
        verbose_name='상태'
    )
    current_subtask = models.ForeignKey(
        Subtask,
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name='participant_progress',
        verbose_name='현재 단계'
    )
    joined_at = models.DateTimeField(auto_now_add=True, verbose_name='입장 시각')
    last_active_at = models.DateTimeField(auto_now=True, verbose_name='마지막 활동')
    completed_at = models.DateTimeField(null=True, blank=True, verbose_name='완료 시각')

    class Meta:
        db_table = 'session_participants'
        verbose_name = '세션 참가자'
        verbose_name_plural = '세션 참가자'
        unique_together = ['session', 'user']
        indexes = [
            models.Index(fields=['session']),
            models.Index(fields=['user']),
            models.Index(fields=['status']),
            models.Index(fields=['last_active_at']),
        ]

    def __str__(self):
        return f"{self.user.name} in {self.session.title}"


class SessionStepControl(models.Model):
    """강사의 단계 제어 기록"""

    ACTION_CHOICES = [
        ('START_STEP', '단계 시작'),
        ('END_STEP', '단계 종료'),
        ('PAUSE', '일시 정지'),
        ('RESUME', '재개'),
        ('SKIP', '건너뛰기'),
    ]

    session = models.ForeignKey(
        LectureSession,
        on_delete=models.CASCADE,
        related_name='step_controls',
        verbose_name='세션'
    )
    subtask = models.ForeignKey(
        Subtask,
        on_delete=models.CASCADE,
        related_name='step_controls',
        verbose_name='단계'
    )
    instructor = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name='step_controls',
        verbose_name='강사'
    )
    action = models.CharField(
        max_length=20,
        choices=ACTION_CHOICES,
        verbose_name='액션'
    )
    message = models.TextField(blank=True, verbose_name='메시지')
    created_at = models.DateTimeField(auto_now_add=True, verbose_name='기록 시각')

    class Meta:
        db_table = 'session_step_control'
        verbose_name = '단계 제어 기록'
        verbose_name_plural = '단계 제어 기록'
        indexes = [
            models.Index(fields=['session']),
            models.Index(fields=['subtask']),
            models.Index(fields=['created_at']),
        ]

    def __str__(self):
        return f"{self.session.title} - {self.action} at {self.created_at}"
