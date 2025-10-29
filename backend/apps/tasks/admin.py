"""
Task and Subtask Admin Configuration
"""
from django.contrib import admin
from .models import Task, Subtask


class SubtaskInline(admin.TabularInline):
    """Subtask inline for Task admin"""
    model = Subtask
    extra = 1
    fields = ['title', 'order_index', 'target_action', 'guide_text']


@admin.register(Task)
class TaskAdmin(admin.ModelAdmin):
    """Task Admin"""
    list_display = ['title', 'lecture', 'order_index', 'created_at']
    list_filter = ['lecture', 'created_at']
    search_fields = ['title', 'lecture__title']
    ordering = ['lecture', 'order_index']
    inlines = [SubtaskInline]


@admin.register(Subtask)
class SubtaskAdmin(admin.ModelAdmin):
    """Subtask Admin"""
    list_display = ['title', 'task', 'order_index', 'target_action', 'created_at']
    list_filter = ['task__lecture', 'target_action', 'created_at']
    search_fields = ['title', 'task__title']
    ordering = ['task', 'order_index']
