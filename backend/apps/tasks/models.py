"""
Task and Subtask Models
"""
from django.db import models
from apps.lectures.models import Lecture


class Task(models.Model):
    """과제 모델 (강의 내의 큰 단위 작업)"""
    lecture = models.ForeignKey(
        Lecture,
        on_delete=models.CASCADE,
        related_name='tasks',
        verbose_name='강의'
    )
    title = models.CharField(max_length=255, verbose_name='제목')
    description = models.TextField(blank=True, verbose_name='설명')
    order_index = models.IntegerField(verbose_name='순서')
    created_at = models.DateTimeField(auto_now_add=True, verbose_name='생성일시')
    updated_at = models.DateTimeField(auto_now=True, verbose_name='수정일시')

    class Meta:
        db_table = 'tasks'
        verbose_name = '과제'
        verbose_name_plural = '과제'
        unique_together = ['lecture', 'order_index']
        indexes = [
            models.Index(fields=['lecture']),
        ]
        ordering = ['order_index']

    def __str__(self):
        return f"{self.lecture.title} - {self.title}"


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
