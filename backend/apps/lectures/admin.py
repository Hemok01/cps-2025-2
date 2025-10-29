"""
Lecture Admin Configuration
"""
from django.contrib import admin
from .models import Lecture, UserLectureEnrollment


@admin.register(Lecture)
class LectureAdmin(admin.ModelAdmin):
    """Lecture Admin"""
    list_display = ['title', 'instructor', 'is_active', 'created_at']
    list_filter = ['is_active', 'created_at']
    search_fields = ['title', 'instructor__name']
    ordering = ['-created_at']


@admin.register(UserLectureEnrollment)
class EnrollmentAdmin(admin.ModelAdmin):
    """Enrollment Admin"""
    list_display = ['user', 'lecture', 'enrolled_at', 'completed_at']
    list_filter = ['enrolled_at']
    search_fields = ['user__name', 'lecture__title']
    ordering = ['-enrolled_at']
