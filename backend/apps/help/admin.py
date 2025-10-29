from django.contrib import admin
from .models import HelpRequest, MGptAnalysis, HelpResponse


@admin.register(HelpRequest)
class HelpRequestAdmin(admin.ModelAdmin):
    list_display = ['user', 'subtask', 'request_type', 'status', 'created_at']
    list_filter = ['request_type', 'status', 'created_at']
    search_fields = ['user__name', 'subtask__title']


@admin.register(MGptAnalysis)
class MGptAnalysisAdmin(admin.ModelAdmin):
    list_display = ['help_request', 'confidence_score', 'created_at']
    search_fields = ['problem_diagnosis']


@admin.register(HelpResponse)
class HelpResponseAdmin(admin.ModelAdmin):
    list_display = ['help_request', 'help_type', 'feedback_rating', 'displayed_at']
    list_filter = ['help_type', 'feedback_rating']
