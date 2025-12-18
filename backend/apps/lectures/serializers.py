"""
Lecture Serializers
"""
from rest_framework import serializers
from django.db.models import Count, Sum
from .models import Lecture, UserLectureEnrollment
from apps.accounts.serializers import UserSerializer


class LectureSerializer(serializers.ModelSerializer):
    """Lecture serializer"""
    instructor = UserSerializer(read_only=True)
    enrolled_count = serializers.SerializerMethodField()
    session_count = serializers.SerializerMethodField()
    task_count = serializers.SerializerMethodField()
    subtask_count = serializers.SerializerMethodField()

    class Meta:
        model = Lecture
        fields = ['id', 'instructor', 'title', 'description', 'thumbnail_url',
                  'is_active', 'created_at', 'updated_at',
                  'enrolled_count', 'session_count', 'task_count', 'subtask_count']
        read_only_fields = ['id', 'created_at', 'updated_at']

    def get_enrolled_count(self, obj):
        """Get enrollment count (학생 수)"""
        return obj.enrollments.count()

    def get_session_count(self, obj):
        """Get session count (세션 수)"""
        return obj.sessions.count()

    def get_task_count(self, obj):
        """Get task count (과제 수)"""
        return obj.tasks.count()

    def get_subtask_count(self, obj):
        """Get total subtask count across all tasks (총 단계 수)"""
        from apps.tasks.models import Subtask
        return Subtask.objects.filter(task__lecture=obj).count()


class LectureCreateUpdateSerializer(serializers.ModelSerializer):
    """Lecture creation/update serializer"""

    class Meta:
        model = Lecture
        fields = ['title', 'description', 'thumbnail_url', 'is_active']


class EnrollmentSerializer(serializers.ModelSerializer):
    """Enrollment serializer"""
    user = UserSerializer(read_only=True)
    lecture = LectureSerializer(read_only=True)

    class Meta:
        model = UserLectureEnrollment
        fields = ['id', 'user', 'lecture', 'enrolled_at', 'completed_at']
        read_only_fields = ['id', 'enrolled_at']
