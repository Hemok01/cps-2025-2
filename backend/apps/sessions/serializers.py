"""
Lecture Session Serializers
"""
import base64
import uuid
from django.core.files.base import ContentFile
from rest_framework import serializers
from .models import LectureSession, SessionParticipant, SessionStepControl, RecordingSession, StudentScreenshot
from apps.lectures.serializers import LectureSerializer
from apps.accounts.serializers import UserSerializer
from apps.tasks.serializers import SubtaskSerializer


class Base64ImageField(serializers.ImageField):
    """
    Base64 인코딩된 이미지를 처리하는 커스텀 필드
    Android 앱에서 Base64로 인코딩된 스크린샷을 받아 이미지 파일로 변환
    """

    def to_internal_value(self, data):
        # Base64 문자열인 경우 처리
        if isinstance(data, str) and data.startswith('data:image'):
            # data:image/jpeg;base64,xxxx 형식 처리
            format, imgstr = data.split(';base64,')
            ext = format.split('/')[-1]
            data = ContentFile(
                base64.b64decode(imgstr),
                name=f'{uuid.uuid4()}.{ext}'
            )
        elif isinstance(data, str):
            # 순수 Base64 문자열 (data: 접두어 없음)
            try:
                data = ContentFile(
                    base64.b64decode(data),
                    name=f'{uuid.uuid4()}.jpg'
                )
            except Exception:
                raise serializers.ValidationError('Invalid base64 image data')

        return super().to_internal_value(data)


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
    # 익명 참가자 지원을 위한 추가 필드
    name = serializers.SerializerMethodField()
    is_active = serializers.SerializerMethodField()
    has_pending_help_request = serializers.SerializerMethodField()

    class Meta:
        model = SessionParticipant
        fields = [
            'id', 'session', 'user', 'device_id', 'display_name',
            'name', 'is_active', 'status', 'current_subtask',
            'joined_at', 'last_active_at', 'completed_at',
            'has_pending_help_request'
        ]
        read_only_fields = ['id', 'joined_at', 'last_active_at']

    def get_name(self, obj):
        """사용자 이름 반환 (익명 참가자는 display_name 사용)"""
        # 1. 인증된 사용자의 이름
        if obj.user and obj.user.name:
            return obj.user.name
        # 2. 익명 참가자의 display_name
        if obj.display_name:
            return obj.display_name
        # 3. device_id로 식별
        if obj.device_id:
            return f"기기-{obj.device_id[:8]}"
        # 4. 최후의 수단
        return f"참가자-{obj.id}"

    def get_is_active(self, obj):
        """활성 상태 여부"""
        return obj.status in ['WAITING', 'ACTIVE']

    def get_has_pending_help_request(self, obj):
        """대기 중인 도움 요청이 있는지 확인"""
        from apps.help.models import HelpRequest

        # user가 있으면 user 기반으로 조회
        # 익명 참가자(device_id만 있는 경우)는 현재 HelpRequest 모델에서
        # device_id 필드가 없어 추적 불가 - 추후 마이그레이션 필요
        if obj.user:
            return HelpRequest.objects.filter(
                user=obj.user,
                status__in=['PENDING', 'ANALYZING']
            ).exists()
        return False


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


class RecordingSessionAnalysisSerializer(serializers.ModelSerializer):
    """분석 결과를 포함한 녹화 세션 시리얼라이저"""
    step_count = serializers.SerializerMethodField()

    class Meta:
        model = RecordingSession
        fields = [
            'id', 'title', 'status',
            'analysis_result', 'analyzed_at', 'analysis_error',
            'step_count', 'event_count', 'created_at'
        ]
        read_only_fields = fields

    def get_step_count(self, obj):
        """분석된 단계 수 반환"""
        if obj.analysis_result and isinstance(obj.analysis_result, list):
            return len(obj.analysis_result)
        return 0


class RecordingConvertSerializer(serializers.Serializer):
    """녹화 → 과제 변환 요청 시리얼라이저"""
    title = serializers.CharField(max_length=255, help_text='생성할 강의 제목')
    description = serializers.CharField(
        required=False,
        allow_blank=True,
        default='',
        help_text='강의 설명 (선택)'
    )


# ==================== Screenshot Serializers ====================

class StudentScreenshotSerializer(serializers.ModelSerializer):
    """학생 스크린샷 조회용 serializer"""
    participant_name = serializers.SerializerMethodField()
    image_url = serializers.SerializerMethodField()

    class Meta:
        model = StudentScreenshot
        fields = [
            'id', 'session', 'participant', 'device_id',
            'participant_name', 'image_url', 'captured_at', 'created_at'
        ]
        read_only_fields = fields

    def get_participant_name(self, obj):
        if obj.participant:
            # user가 있으면 user.name 사용
            if obj.participant.user and obj.participant.user.name:
                return obj.participant.user.name
            # display_name 사용
            if obj.participant.display_name:
                return obj.participant.display_name
        # device_id로 식별
        if obj.device_id:
            return f"기기-{obj.device_id[:8]}"
        return f"참가자-{obj.participant.id if obj.participant else 'Unknown'}"

    def get_image_url(self, obj):
        request = self.context.get('request')
        if obj.image and request:
            return request.build_absolute_uri(obj.image.url)
        elif obj.image:
            return obj.image.url
        return None


class StudentScreenshotUploadSerializer(serializers.Serializer):
    """학생 스크린샷 업로드용 serializer (Android 앱에서 사용)"""
    device_id = serializers.CharField(max_length=255)
    image_data = Base64ImageField()
    captured_at = serializers.IntegerField(help_text='Unix timestamp in milliseconds')

    def validate_captured_at(self, value):
        """밀리초 타임스탬프를 datetime으로 변환"""
        from datetime import datetime
        try:
            # 밀리초를 초로 변환
            return datetime.fromtimestamp(value / 1000)
        except (ValueError, OSError):
            raise serializers.ValidationError('Invalid timestamp')


class StudentScreenshotListSerializer(serializers.ModelSerializer):
    """세션 내 모든 학생의 최신 스크린샷 목록"""
    participant_id = serializers.IntegerField(source='participant.id', allow_null=True)
    participant_name = serializers.SerializerMethodField()
    image_url = serializers.SerializerMethodField()

    class Meta:
        model = StudentScreenshot
        fields = [
            'id', 'participant_id', 'device_id', 'participant_name',
            'image_url', 'captured_at'
        ]

    def get_participant_name(self, obj):
        if obj.participant:
            # user가 있으면 user.name 사용
            if obj.participant.user and obj.participant.user.name:
                return obj.participant.user.name
            # display_name 사용
            if obj.participant.display_name:
                return obj.participant.display_name
        # device_id로 식별
        if obj.device_id:
            return f"기기-{obj.device_id[:8]}"
        return f"참가자-{obj.participant.id if obj.participant else 'Unknown'}"

    def get_image_url(self, obj):
        request = self.context.get('request')
        if obj.image and request:
            return request.build_absolute_uri(obj.image.url)
        elif obj.image:
            return obj.image.url
        return None
