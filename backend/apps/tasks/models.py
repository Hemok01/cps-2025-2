"""
Task and Subtask Models
"""
from django.db import models
from apps.lectures.models import Lecture


class Task(models.Model):
    """과제 모델 (강의 내의 큰 단위 작업, 또는 녹화에서 독립 생성)"""
    lecture = models.ForeignKey(
        Lecture,
        on_delete=models.CASCADE,
        related_name='tasks',
        verbose_name='강의',
        null=True,
        blank=True  # 녹화에서 직접 생성 시 lecture 없이 생성 가능
    )
    source_task = models.ForeignKey(
        'self',
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name='copies',
        verbose_name='원본 과제',
        help_text='이 과제가 복사된 원본 과제 (템플릿)'
    )
    title = models.CharField(max_length=255, verbose_name='제목')
    description = models.TextField(blank=True, verbose_name='설명')
    order_index = models.IntegerField(verbose_name='순서', default=0)
    created_at = models.DateTimeField(auto_now_add=True, verbose_name='생성일시')
    updated_at = models.DateTimeField(auto_now=True, verbose_name='수정일시')

    class Meta:
        db_table = 'tasks'
        verbose_name = '과제'
        verbose_name_plural = '과제'
        indexes = [
            models.Index(fields=['lecture']),
            models.Index(fields=['source_task']),
        ]
        ordering = ['order_index']

    def __str__(self):
        if self.lecture:
            return f"{self.lecture.title} - {self.title}"
        return self.title

    def copy_to_lecture(self, lecture, order_index=0):
        """
        Task와 Subtask를 복사하여 지정된 Lecture에 연결합니다.

        Args:
            lecture: 연결할 Lecture 인스턴스
            order_index: 복사본의 순서 인덱스

        Returns:
            복사된 Task 인스턴스
        """
        task_copy = Task.objects.create(
            lecture=lecture,
            title=self.title,
            description=self.description,
            order_index=order_index,
            source_task=self
        )

        # Subtask 일괄 복사 (bulk_create로 성능 최적화)
        subtasks_to_create = []
        for s in self.subtasks.all():
            subtasks_to_create.append(Subtask(
                task=task_copy,
                title=s.title,
                description=s.description,
                order_index=s.order_index,
                target_action=s.target_action,
                target_element_hint=s.target_element_hint,
                guide_text=s.guide_text,
                voice_guide_text=s.voice_guide_text,
                time=s.time,
                text=s.text,
                content_description=s.content_description,
                view_id=s.view_id,
                bounds=s.bounds,
                target_package=s.target_package,
                target_class=s.target_class,
            ))

        if subtasks_to_create:
            Subtask.objects.bulk_create(subtasks_to_create)

        return task_copy


class Subtask(models.Model):
    """세부 단계 모델 (Task를 구성하는 작은 단위)"""

    ACTION_CHOICES = [
        ('CLICK', '클릭'),
        ('LONG_CLICK', '길게 누르기'),
        ('SCROLL', '스크롤'),
        ('INPUT', '입력'),
        ('NAVIGATE', '화면 이동'),
    ]

    task = models.ForeignKey(
        Task,
        on_delete=models.CASCADE,
        related_name='subtasks',
        verbose_name='과제'
    )
    title = models.CharField(max_length=255, verbose_name='제목')
    description = models.TextField(blank=True, verbose_name='설명')
    order_index = models.IntegerField(verbose_name='순서')
    target_action = models.CharField(
        max_length=100,
        choices=ACTION_CHOICES,
        null=True,
        blank=True,
        verbose_name='목표 액션'
    )
    target_element_hint = models.TextField(
        blank=True,
        verbose_name='UI 요소 힌트'
    )
    guide_text = models.TextField(
        blank=True,
        verbose_name='안내 문구'
    )
    voice_guide_text = models.TextField(
        blank=True,
        verbose_name='음성 안내 문구'
    )

    # === Flask 원본 서버와 동기화를 위한 추가 필드 ===
    time = models.BigIntegerField(
        null=True, blank=True,
        verbose_name='이벤트 시간',
        help_text='이벤트 발생 시간 (Unix 밀리초)'
    )
    text = models.TextField(
        blank=True, default='',
        verbose_name='UI 텍스트',
        help_text='UI 요소의 표시 텍스트'
    )
    content_description = models.CharField(
        max_length=500, blank=True, default='',
        verbose_name='접근성 설명',
        help_text='contentDescription 값'
    )
    view_id = models.CharField(
        max_length=255, blank=True, default='',
        verbose_name='뷰 ID',
        help_text='viewId 리소스 이름'
    )
    bounds = models.CharField(
        max_length=100, blank=True, default='',
        verbose_name='화면 좌표',
        help_text='UI 요소 위치 [x1,y1][x2,y2]'
    )
    target_package = models.CharField(
        max_length=255, blank=True, default='',
        verbose_name='패키지명',
        help_text='앱 패키지 이름'
    )
    target_class = models.CharField(
        max_length=255, blank=True, default='',
        verbose_name='클래스명',
        help_text='UI 요소 클래스명'
    )

    created_at = models.DateTimeField(auto_now_add=True, verbose_name='생성일시')
    updated_at = models.DateTimeField(auto_now=True, verbose_name='수정일시')

    class Meta:
        db_table = 'subtasks'
        verbose_name = '세부 단계'
        verbose_name_plural = '세부 단계'
        unique_together = ['task', 'order_index']
        indexes = [
            models.Index(fields=['task']),
        ]
        ordering = ['order_index']

    def __str__(self):
        return f"{self.task.title} - {self.title}"
