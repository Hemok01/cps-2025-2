"""
Activity Log Serializers
"""
from rest_framework import serializers
from .models import ActivityLog


class ActivityLogSerializer(serializers.ModelSerializer):
    """Activity log serializer"""

    class Meta:
        model = ActivityLog
        fields = [
            'id', 'user', 'subtask', 'session', 'event_type',
            'event_data', 'screen_info', 'node_info', 'parent_node_info',
            'view_id_resource_name', 'content_description', 'is_sensitive_data',
            'timestamp'
        ]
        read_only_fields = ['id', 'timestamp']


class ActivityLogCreateSerializer(serializers.ModelSerializer):
    """Activity log creation serializer"""

    class Meta:
        model = ActivityLog
        fields = [
            'subtask', 'session', 'event_type', 'event_data',
            'screen_info', 'node_info', 'parent_node_info',
            'view_id_resource_name', 'content_description', 'is_sensitive_data'
        ]
