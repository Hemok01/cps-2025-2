from django.contrib import admin
from .models import UserProgress


@admin.register(UserProgress)
class UserProgressAdmin(admin.ModelAdmin):
    list_display = ['user', 'subtask', 'status', 'attempts', 'help_count', 'updated_at']
    list_filter = ['status', 'updated_at']
    search_fields = ['user__name', 'subtask__title']
