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
            'id', 'user', 'subtask', 'session', 'recording_session', 'event_type',
            'event_data', 'screen_info', 'node_info', 'parent_node_info',
            'view_id_resource_name', 'content_description', 'is_sensitive_data',
            'bounds', 'is_clickable', 'is_editable', 'is_enabled', 'is_focused',
            'timestamp', 'server_received_at'
        ]
        read_only_fields = ['id', 'timestamp', 'server_received_at']


class ActivityLogCreateSerializer(serializers.ModelSerializer):
    """Activity log creation serializer"""

    class Meta:
        model = ActivityLog
        fields = [
            'subtask', 'session', 'recording_session', 'event_type', 'event_data',
            'screen_info', 'node_info', 'parent_node_info',
            'view_id_resource_name', 'content_description', 'is_sensitive_data',
            'bounds', 'is_clickable', 'is_editable', 'is_enabled', 'is_focused'
        ]


class AnonymousActivityLogCreateSerializer(serializers.ModelSerializer):
    """익명 사용자용 Activity log creation serializer (수강자 앱용)"""
    device_id = serializers.CharField(max_length=255, required=True)

    class Meta:
        model = ActivityLog
        fields = [
            'device_id', 'subtask', 'session', 'recording_session', 'event_type', 'event_data',
            'screen_info', 'node_info', 'parent_node_info',
            'view_id_resource_name', 'content_description', 'is_sensitive_data',
            'bounds', 'is_clickable', 'is_editable', 'is_enabled', 'is_focused'
        ]


class RecordedEventSerializer(serializers.ModelSerializer):
    """녹화된 이벤트 전용 serializer (Android 앱에서 전송용)"""

    # Android에서 전송하는 필드명을 직접 매핑
    time = serializers.IntegerField(source='timestamp', write_only=True, help_text='이벤트 발생 시각 (ms)')
    package = serializers.CharField(source='event_data.package', write_only=True, required=False)
    className = serializers.CharField(source='event_data.className', write_only=True, required=False)
    text = serializers.CharField(source='event_data.text', write_only=True, required=False)

    class Meta:
        model = ActivityLog
        fields = [
            'time', 'package', 'className', 'text', 'content_description',
            'view_id_resource_name', 'bounds', 'is_clickable', 'is_editable',
            'is_enabled', 'is_focused', 'event_type'
        ]
