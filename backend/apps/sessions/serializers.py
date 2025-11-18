"""
Lecture Session Serializers
"""
from rest_framework import serializers
from .models import LectureSession, SessionParticipant, SessionStepControl, RecordingSession
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


class RecordingSessionSerializer(serializers.ModelSerializer):
    """녹화 세션 상세 조회 serializer"""
    instructor = UserSerializer(read_only=True)
    lecture = LectureSerializer(read_only=True)

    class Meta:
        model = RecordingSession
        fields = [
            'id', 'instructor', 'title', 'description', 'status',
            'event_count', 'duration_seconds', 'started_at', 'ended_at',
            'lecture', 'created_at', 'updated_at'
        ]
        read_only_fields = ['id', 'instructor', 'event_count', 'created_at', 'updated_at']


class RecordingSessionCreateSerializer(serializers.ModelSerializer):
    """녹화 시작 serializer"""

    class Meta:
        model = RecordingSession
        fields = ['title', 'description']

    def create(self, validated_data):
        # instructor는 view에서 request.user로 자동 설정
        return super().create(validated_data)


class RecordingSessionListSerializer(serializers.ModelSerializer):
    """녹화 세션 목록 조회 serializer"""
    instructor_name = serializers.CharField(source='instructor.name', read_only=True)

    class Meta:
        model = RecordingSession
        fields = [
            'id', 'title', 'instructor_name', 'status',
            'event_count', 'duration_seconds', 'started_at', 'ended_at',
            'created_at'
        ]
        read_only_fields = fields
