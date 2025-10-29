"""
Progress Serializers
"""
from rest_framework import serializers
from .models import UserProgress
from apps.tasks.serializers import SubtaskSerializer
from apps.accounts.serializers import UserSerializer


class UserProgressSerializer(serializers.ModelSerializer):
    """User progress serializer"""
    user = UserSerializer(read_only=True)
    subtask = SubtaskSerializer(read_only=True)

    class Meta:
        model = UserProgress
        fields = [
            'id', 'user', 'subtask', 'session', 'status',
            'started_at', 'completed_at', 'attempts', 'help_count',
            'created_at', 'updated_at'
        ]
        read_only_fields = ['id', 'created_at', 'updated_at']


class UserProgressUpdateSerializer(serializers.ModelSerializer):
    """Progress update serializer"""

    class Meta:
        model = UserProgress
        fields = ['status', 'completed_at']
