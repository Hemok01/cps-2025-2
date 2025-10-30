from django.contrib import admin
from .models import ActivityLog


@admin.register(ActivityLog)
class ActivityLogAdmin(admin.ModelAdmin):
    list_display = ['user', 'event_type', 'subtask', 'session', 'timestamp']
    list_filter = ['event_type', 'is_sensitive_data', 'timestamp']
    search_fields = ['user__name', 'view_id_resource_name']
    ordering = ['-timestamp']
