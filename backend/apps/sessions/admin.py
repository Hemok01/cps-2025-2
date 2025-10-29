from django.contrib import admin
from .models import LectureSession, SessionParticipant, SessionStepControl


@admin.register(LectureSession)
class LectureSessionAdmin(admin.ModelAdmin):
    list_display = ['title', 'lecture', 'instructor', 'session_code', 'status', 'created_at']
    list_filter = ['status', 'created_at']
    search_fields = ['title', 'session_code', 'lecture__title']
    ordering = ['-created_at']


@admin.register(SessionParticipant)
class SessionParticipantAdmin(admin.ModelAdmin):
    list_display = ['user', 'session', 'status', 'joined_at']
    list_filter = ['status', 'joined_at']
    search_fields = ['user__name', 'session__title']


@admin.register(SessionStepControl)
class SessionStepControlAdmin(admin.ModelAdmin):
    list_display = ['session', 'subtask', 'action', 'instructor', 'created_at']
    list_filter = ['action', 'created_at']
    search_fields = ['session__title']
