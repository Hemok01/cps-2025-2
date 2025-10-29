"""
Help Request Serializers
"""
from rest_framework import serializers
from .models import HelpRequest, MGptAnalysis, HelpResponse
from apps.accounts.serializers import UserSerializer
from apps.tasks.serializers import SubtaskSerializer


class HelpRequestSerializer(serializers.ModelSerializer):
    """Help request serializer"""
    user = UserSerializer(read_only=True)
    subtask = SubtaskSerializer(read_only=True)

    class Meta:
        model = HelpRequest
        fields = [
            'id', 'user', 'subtask', 'session', 'request_type',
            'context_data', 'status', 'created_at', 'resolved_at'
        ]
        read_only_fields = ['id', 'created_at']


class HelpRequestCreateSerializer(serializers.ModelSerializer):
    """Help request creation serializer"""

    class Meta:
        model = HelpRequest
        fields = ['subtask', 'session', 'request_type', 'context_data']


class MGptAnalysisSerializer(serializers.ModelSerializer):
    """M-GPT analysis serializer"""

    class Meta:
        model = MGptAnalysis
        fields = [
            'id', 'help_request', 'analysis_input', 'analysis_output',
            'problem_diagnosis', 'suggested_help', 'confidence_score',
            'created_at'
        ]
        read_only_fields = ['id', 'created_at']


class HelpResponseSerializer(serializers.ModelSerializer):
    """Help response serializer"""

    class Meta:
        model = HelpResponse
        fields = [
            'id', 'help_request', 'mgpt_analysis', 'help_type',
            'help_content', 'displayed_at', 'feedback_rating',
            'feedback_text', 'feedback_at'
        ]
        read_only_fields = ['id', 'displayed_at']
