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
    # 인증된 사용자 (선택적 - 익명 참가 지원)
    user = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name='session_participations',
        verbose_name='사용자',
        null=True,
        blank=True
    )
    # 익명 참가자 식별용 필드
    device_id = models.CharField(
        max_length=255,
        blank=True,
        null=True,
        verbose_name='기기 ID',
        help_text='익명 참가자의 기기 고유 식별자'
    )
    display_name = models.CharField(
        max_length=100,
        blank=True,
        default='',
        verbose_name='표시 이름',
        help_text='화면에 표시될 참가자 이름'
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
    # 완료된 단계 추적 (JSON Array of subtask IDs)
    completed_subtasks = models.JSONField(
        default=list,
        verbose_name='완료된 단계 목록',
        help_text='완료한 단계 ID 목록 (JSON Array)'
    )
    last_completed_at = models.DateTimeField(
        null=True,
        blank=True,
        verbose_name='마지막 완료 시각'
    )
    joined_at = models.DateTimeField(auto_now_add=True, verbose_name='입장 시각')
    last_active_at = models.DateTimeField(auto_now=True, verbose_name='마지막 활동')
    completed_at = models.DateTimeField(null=True, blank=True, verbose_name='완료 시각')

    class Meta:
        db_table = 'session_participants'
        verbose_name = '세션 참가자'
        verbose_name_plural = '세션 참가자'
        indexes = [
            models.Index(fields=['session']),
            models.Index(fields=['user']),
            models.Index(fields=['device_id']),
            models.Index(fields=['status']),
            models.Index(fields=['last_active_at']),
        ]
        # 세션당 device_id 또는 user 중 하나로 유니크 (둘 다 가능)
        constraints = [
            models.UniqueConstraint(
                fields=['session', 'device_id'],
                condition=models.Q(device_id__isnull=False),
                name='unique_session_device'
            ),
            models.UniqueConstraint(
                fields=['session', 'user'],
                condition=models.Q(user__isnull=False),
                name='unique_session_user'
            ),
        ]

    def __str__(self):
        name = self.display_name or (self.user.name if self.user else 'Unknown')
        return f"{name} in {self.session.title}"

    @property
    def participant_name(self):
        """참가자 이름 반환"""
        if self.display_name:
            return self.display_name
        if self.user:
            return self.user.name
        return '익명'


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


class StudentScreenshot(models.Model):
    """학생 화면 스크린샷 모델"""

    session = models.ForeignKey(
        LectureSession,
        on_delete=models.CASCADE,
        related_name='screenshots',
        verbose_name='세션'
    )
    participant = models.ForeignKey(
        'SessionParticipant',
        on_delete=models.CASCADE,
        related_name='screenshots',
        verbose_name='참가자',
        null=True,
        blank=True
    )
    device_id = models.CharField(
        max_length=255,
        blank=True,
        verbose_name='기기 ID',
        help_text='익명 참가자 식별용'
    )
    image = models.ImageField(
        upload_to='screenshots/%Y/%m/%d/',
        verbose_name='스크린샷 이미지'
    )
    captured_at = models.DateTimeField(
        verbose_name='캡처 시각'
    )
    created_at = models.DateTimeField(
        auto_now_add=True,
        verbose_name='업로드 시각'
    )

    class Meta:
        db_table = 'student_screenshots'
        verbose_name = '학생 스크린샷'
        verbose_name_plural = '학생 스크린샷'
        indexes = [
            models.Index(fields=['session', 'participant']),
            models.Index(fields=['session', 'device_id']),
            models.Index(fields=['captured_at']),
        ]
        ordering = ['-captured_at']

    def __str__(self):
        name = self.participant.participant_name if self.participant else self.device_id[:8]
        return f"{name} - {self.captured_at}"


class RecordingSession(models.Model):
    """강의 녹화 세션 모델 (강의자의 시연 녹화)"""

    STATUS_CHOICES = [
        ('RECORDING', '녹화 중'),
        ('COMPLETED', '완료'),
        ('PROCESSING', '처리 중'),
        ('FAILED', '실패'),
    ]

    instructor = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name='recording_sessions',
        verbose_name='강의자'
    )
    title = models.CharField(max_length=200, verbose_name='녹화 제목')
    description = models.TextField(blank=True, verbose_name='설명')
    status = models.CharField(
        max_length=20,
        choices=STATUS_CHOICES,
        default='RECORDING',
        verbose_name='상태'
    )
    event_count = models.IntegerField(default=0, verbose_name='이벤트 수')
    duration_seconds = models.IntegerField(
        default=0,
        verbose_name='녹화 시간(초)',
        help_text='녹화 시작부터 종료까지의 시간'
    )
    started_at = models.DateTimeField(null=True, blank=True, verbose_name='시작 시각')
    ended_at = models.DateTimeField(null=True, blank=True, verbose_name='종료 시각')
    created_at = models.DateTimeField(auto_now_add=True, verbose_name='생성일시')
    updated_at = models.DateTimeField(auto_now=True, verbose_name='수정일시')

    # 연결된 강의 (녹화로부터 강의 생성 후)
    lecture = models.ForeignKey(
        Lecture,
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name='recordings',
        verbose_name='생성된 강의'
    )

    class Meta:
        db_table = 'recording_sessions'
        verbose_name = '녹화 세션'
        verbose_name_plural = '녹화 세션'
        indexes = [
            models.Index(fields=['instructor']),
            models.Index(fields=['status']),
            models.Index(fields=['created_at']),
            models.Index(fields=['lecture']),
        ]
        ordering = ['-created_at']

    def __str__(self):
        return f"{self.title} - {self.instructor.name}"
