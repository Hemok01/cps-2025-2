"""
Lecture Serializers
"""
from rest_framework import serializers
from .models import Lecture, UserLectureEnrollment
from apps.accounts.serializers import UserSerializer


class LectureSerializer(serializers.ModelSerializer):
    """Lecture serializer"""
    instructor = UserSerializer(read_only=True)
    enrolled_count = serializers.SerializerMethodField()

    class Meta:
        model = Lecture
        fields = ['id', 'instructor', 'title', 'description', 'thumbnail_url',
                  'is_active', 'created_at', 'updated_at', 'enrolled_count']
        read_only_fields = ['id', 'created_at', 'updated_at']

    def get_enrolled_count(self, obj):
        """Get enrollment count"""
        return obj.enrollments.count()


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
