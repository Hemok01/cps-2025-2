"""
Student Serializers
수강생 관련 Serializer
"""
from rest_framework import serializers
from apps.sessions.models import LectureSession, SessionParticipant
from apps.lectures.models import Lecture, UserLectureEnrollment
from apps.tasks.models import Task, Subtask
from apps.accounts.serializers import UserSerializer


class LectureListSerializer(serializers.ModelSerializer):
    """강의 목록 Serializer (수강생용)"""
    instructor_name = serializers.CharField(source='instructor.name', read_only=True)
    is_enrolled = serializers.SerializerMethodField()

    class Meta:
        model = Lecture
        fields = ['id', 'title', 'description', 'thumbnail_url', 'instructor_name', 'is_enrolled', 'created_at']

    def get_is_enrolled(self, obj):
        request = self.context.get('request')
        if request and request.user.is_authenticated:
            return UserLectureEnrollment.objects.filter(user=request.user, lecture=obj).exists()
        return False


class SessionJoinSerializer(serializers.Serializer):
    """세션 참가 요청 Serializer"""
    session_code = serializers.CharField(max_length=20)

    def validate_session_code(self, value):
        """세션 코드 유효성 검사"""
        try:
            session = LectureSession.objects.get(session_code=value.upper())
            if session.status == 'ENDED':
                raise serializers.ValidationError("이미 종료된 세션입니다.")
            return value.upper()
        except LectureSession.DoesNotExist:
            raise serializers.ValidationError("유효하지 않은 세션 코드입니다.")


class SubtaskDetailSerializer(serializers.ModelSerializer):
    """단계 상세 Serializer"""
    task_title = serializers.CharField(source='task.title', read_only=True)

    class Meta:
        model = Subtask
        fields = ['id', 'task', 'task_title', 'title', 'description', 'order', 'target_app', 'target_action']


class SessionParticipantSerializer(serializers.ModelSerializer):
    """세션 참가자 Serializer"""
    current_subtask_detail = SubtaskDetailSerializer(source='current_subtask', read_only=True)
    session_title = serializers.CharField(source='session.title', read_only=True)
    session_code = serializers.CharField(source='session.session_code', read_only=True)
    session_status = serializers.CharField(source='session.status', read_only=True)

    class Meta:
        model = SessionParticipant
        fields = [
            'id', 'session', 'session_title', 'session_code', 'session_status',
            'status', 'current_subtask', 'current_subtask_detail',
            'joined_at', 'last_active_at', 'completed_at'
        ]
        read_only_fields = ['id', 'joined_at', 'last_active_at', 'completed_at']


class MySessionSerializer(serializers.ModelSerializer):
    """내 세션 목록 Serializer"""
    lecture_title = serializers.CharField(source='lecture.title', read_only=True)
    instructor_name = serializers.CharField(source='instructor.name', read_only=True)
    current_subtask_detail = SubtaskDetailSerializer(source='current_subtask', read_only=True)
    my_status = serializers.SerializerMethodField()

    class Meta:
        model = LectureSession
        fields = [
            'id', 'lecture', 'lecture_title', 'instructor_name', 'title',
            'session_code', 'status', 'current_subtask', 'current_subtask_detail',
            'my_status', 'scheduled_at', 'started_at', 'ended_at'
        ]

    def get_my_status(self, obj):
        """내 참가 상태"""
        request = self.context.get('request')
        if request and request.user.is_authenticated:
            participant = SessionParticipant.objects.filter(session=obj, user=request.user).first()
            if participant:
                return participant.status
        return None
