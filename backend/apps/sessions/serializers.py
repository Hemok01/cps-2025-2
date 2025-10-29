"""
Lecture Session Serializers
"""
from rest_framework import serializers
from .models import LectureSession, SessionParticipant, SessionStepControl
from apps.lectures.serializers import LectureSerializer
from apps.accounts.serializers import UserSerializer
from apps.tasks.serializers import SubtaskSerializer


class LectureSessionSerializer(serializers.ModelSerializer):
    """Lecture session serializer"""
    lecture = LectureSerializer(read_only=True)
    instructor = UserSerializer(read_only=True)
    current_subtask = SubtaskSerializer(read_only=True)
    participant_count = serializers.SerializerMethodField()

    class Meta:
        model = LectureSession
        fields = [
            'id', 'lecture', 'instructor', 'title', 'session_code',
            'status', 'current_subtask', 'qr_code_url', 'scheduled_at',
            'started_at', 'ended_at', 'participant_count', 'created_at'
        ]
        read_only_fields = ['id', 'session_code', 'created_at']

    def get_participant_count(self, obj):
        return obj.participants.count()


class LectureSessionCreateSerializer(serializers.ModelSerializer):
    """Session creation serializer"""

    class Meta:
        model = LectureSession
        fields = ['title', 'scheduled_at']


class SessionParticipantSerializer(serializers.ModelSerializer):
    """Session participant serializer"""
    user = UserSerializer(read_only=True)
    current_subtask = SubtaskSerializer(read_only=True)

    class Meta:
        model = SessionParticipant
        fields = [
            'id', 'session', 'user', 'status', 'current_subtask',
            'joined_at', 'last_active_at', 'completed_at'
        ]
        read_only_fields = ['id', 'joined_at', 'last_active_at']


class SessionStepControlSerializer(serializers.ModelSerializer):
    """Step control serializer"""

    class Meta:
        model = SessionStepControl
        fields = ['id', 'session', 'subtask', 'instructor', 'action', 'message', 'created_at']
        read_only_fields = ['id', 'created_at']
