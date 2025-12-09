"""
Task and Subtask Serializers
"""
from rest_framework import serializers
from .models import Task, Subtask


class SubtaskSerializer(serializers.ModelSerializer):
    """Subtask serializer"""

    class Meta:
        model = Subtask
        fields = [
            'id', 'task', 'title', 'description', 'order_index',
            'target_action', 'target_element_hint', 'guide_text',
            'voice_guide_text', 'created_at', 'updated_at',
            # Flask 원본 서버 동기화 필드
            'time', 'text', 'content_description', 'view_id',
            'bounds', 'target_package', 'target_class'
        ]
        read_only_fields = ['id', 'created_at', 'updated_at']


class SubtaskCreateUpdateSerializer(serializers.ModelSerializer):
    """Subtask creation/update serializer"""

    class Meta:
        model = Subtask
        fields = [
            'title', 'description', 'order_index', 'target_action',
            'target_element_hint', 'guide_text', 'voice_guide_text',
            # Flask 원본 서버 동기화 필드
            'time', 'text', 'content_description', 'view_id',
            'bounds', 'target_package', 'target_class'
        ]


class TaskSerializer(serializers.ModelSerializer):
    """Task serializer with subtasks"""
    subtasks = SubtaskSerializer(many=True, read_only=True)
    subtask_count = serializers.SerializerMethodField()

    class Meta:
        model = Task
        fields = [
            'id', 'lecture', 'title', 'description', 'order_index',
            'subtasks', 'subtask_count', 'created_at', 'updated_at'
        ]
        read_only_fields = ['id', 'created_at', 'updated_at']

    def get_subtask_count(self, obj):
        """Get subtask count"""
        return obj.subtasks.count()


class TaskCreateUpdateSerializer(serializers.ModelSerializer):
    """Task creation/update serializer"""

    class Meta:
        model = Task
        fields = ['title', 'description', 'order_index']


class TaskDetailSerializer(serializers.ModelSerializer):
    """Detailed task serializer"""
    subtasks = SubtaskSerializer(many=True, read_only=True)
    lecture_title = serializers.CharField(source='lecture.title', read_only=True)

    class Meta:
        model = Task
        fields = [
            'id', 'lecture', 'lecture_title', 'title', 'description',
            'order_index', 'subtasks', 'created_at', 'updated_at'
        ]
        read_only_fields = ['id', 'created_at', 'updated_at']
