"""
Lecture Models
"""
from django.db import models
from django.conf import settings


class Lecture(models.Model):
    """강의 모델"""
    instructor = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name='lectures',
        verbose_name='강사'
    )
    title = models.CharField(max_length=255, verbose_name='제목')
    description = models.TextField(blank=True, verbose_name='설명')
    thumbnail_url = models.URLField(max_length=500, blank=True, verbose_name='썸네일 URL')
    is_active = models.BooleanField(default=True, verbose_name='활성 여부')
    created_at = models.DateTimeField(auto_now_add=True, verbose_name='생성일시')
    updated_at = models.DateTimeField(auto_now=True, verbose_name='수정일시')

    class Meta:
        db_table = 'lectures'
        verbose_name = '강의'
        verbose_name_plural = '강의'
        indexes = [
            models.Index(fields=['instructor']),
            models.Index(fields=['is_active']),
        ]

    def __str__(self):
        return self.title


class UserLectureEnrollment(models.Model):
    """수강 등록 모델"""
    user = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name='enrollments',
        verbose_name='사용자'
    )
    lecture = models.ForeignKey(
        Lecture,
        on_delete=models.CASCADE,
        related_name='enrollments',
        verbose_name='강의'
    )
    enrolled_at = models.DateTimeField(auto_now_add=True, verbose_name='등록일시')
    completed_at = models.DateTimeField(null=True, blank=True, verbose_name='완료일시')

    class Meta:
        db_table = 'user_lecture_enrollments'
        verbose_name = '수강 등록'
        verbose_name_plural = '수강 등록'
        unique_together = ['user', 'lecture']
        indexes = [
            models.Index(fields=['user']),
            models.Index(fields=['lecture']),
        ]

    def __str__(self):
        return f"{self.user.name} - {self.lecture.title}"
