"""
Student Screenshot API Views
학생 화면 스크린샷 업로드 및 조회 API
"""
from rest_framework import status
from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework.permissions import IsAuthenticated, AllowAny
from django.shortcuts import get_object_or_404
from django.db.models import Max
from channels.layers import get_channel_layer
from asgiref.sync import async_to_sync

from .models import LectureSession, SessionParticipant, StudentScreenshot
from .serializers import (
    StudentScreenshotSerializer,
    StudentScreenshotUploadSerializer,
    StudentScreenshotListSerializer,
)


class ScreenshotUploadView(APIView):
    """
    스크린샷 업로드 API (Android 앱에서 호출)

    POST /api/sessions/{session_id}/screenshots/upload/

    익명 참가자도 업로드 가능하도록 AllowAny 권한 사용
    device_id로 참가자를 식별
    """
    permission_classes = [AllowAny]

    def post(self, request, session_id):
        # 세션 확인
        session = get_object_or_404(LectureSession, id=session_id)

        # 세션이 진행 중인지 확인
        if session.status not in ['IN_PROGRESS', 'WAITING']:
            return Response(
                {'error': '세션이 진행 중이 아닙니다.'},
                status=status.HTTP_400_BAD_REQUEST
            )

        # 요청 데이터 검증
        serializer = StudentScreenshotUploadSerializer(data=request.data)
        if not serializer.is_valid():
            return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

        device_id = serializer.validated_data['device_id']
        image_file = serializer.validated_data['image_data']
        captured_at = serializer.validated_data['captured_at']

        # 참가자 조회 (device_id로)
        participant = SessionParticipant.objects.filter(
            session=session,
            device_id=device_id
        ).first()

        # 이전 스크린샷 삭제 (최신 1개만 유지)
        StudentScreenshot.objects.filter(
            session=session,
            device_id=device_id
        ).delete()

        # 새 스크린샷 저장
        screenshot = StudentScreenshot.objects.create(
            session=session,
            participant=participant,
            device_id=device_id,
            image=image_file,
            captured_at=captured_at
        )

        # WebSocket으로 대시보드에 알림 전송
        self._notify_screenshot_updated(session, screenshot)

        # 응답 반환
        response_serializer = StudentScreenshotSerializer(
            screenshot,
            context={'request': request}
        )
        return Response(response_serializer.data, status=status.HTTP_201_CREATED)

    def _notify_screenshot_updated(self, session, screenshot):
        """WebSocket을 통해 강사 대시보드에 스크린샷 업데이트 알림"""
        try:
            channel_layer = get_channel_layer()
            group_name = f'session_{session.session_code}'

            participant_name = "Unknown"
            participant_id = None
            if screenshot.participant:
                participant_name = screenshot.participant.participant_name
                participant_id = screenshot.participant.id
            elif screenshot.device_id:
                participant_name = f"익명-{screenshot.device_id[:8]}"

            async_to_sync(channel_layer.group_send)(
                group_name,
                {
                    'type': 'screenshot_updated',
                    'participant_id': participant_id,
                    'device_id': screenshot.device_id,
                    'participant_name': participant_name,
                    'image_url': screenshot.image.url if screenshot.image else None,
                    'captured_at': screenshot.captured_at.isoformat(),
                }
            )
        except Exception as e:
            # WebSocket 알림 실패해도 업로드는 성공으로 처리
            print(f"Failed to send WebSocket notification: {e}")


class StudentScreenshotListView(APIView):
    """
    세션 내 모든 학생의 최신 스크린샷 목록 조회 (강사용)

    GET /api/sessions/{session_id}/screenshots/
    """
    permission_classes = [IsAuthenticated]

    def get(self, request, session_id):
        session = get_object_or_404(LectureSession, id=session_id)

        # 강사 권한 확인
        if session.instructor != request.user:
            return Response(
                {'error': '권한이 없습니다.'},
                status=status.HTTP_403_FORBIDDEN
            )

        # 각 참가자/device_id별 최신 스크린샷만 조회
        # Subquery를 사용하여 최신 스크린샷 ID를 찾음
        latest_screenshot_ids = StudentScreenshot.objects.filter(
            session=session
        ).values('device_id').annotate(
            latest_id=Max('id')
        ).values_list('latest_id', flat=True)

        screenshots = StudentScreenshot.objects.filter(
            id__in=latest_screenshot_ids
        ).select_related('participant')

        serializer = StudentScreenshotListSerializer(
            screenshots,
            many=True,
            context={'request': request}
        )
        return Response(serializer.data)


class StudentScreenshotDetailView(APIView):
    """
    특정 참가자의 최신 스크린샷 조회 (강사용)

    GET /api/sessions/{session_id}/screenshots/{participant_id}/
    """
    permission_classes = [IsAuthenticated]

    def get(self, request, session_id, participant_id):
        session = get_object_or_404(LectureSession, id=session_id)

        # 강사 권한 확인
        if session.instructor != request.user:
            return Response(
                {'error': '권한이 없습니다.'},
                status=status.HTTP_403_FORBIDDEN
            )

        # 참가자의 최신 스크린샷 조회
        screenshot = StudentScreenshot.objects.filter(
            session=session,
            participant_id=participant_id
        ).order_by('-captured_at').first()

        if not screenshot:
            return Response(
                {'error': '스크린샷이 없습니다.'},
                status=status.HTTP_404_NOT_FOUND
            )

        serializer = StudentScreenshotSerializer(
            screenshot,
            context={'request': request}
        )
        return Response(serializer.data)


class StudentScreenshotByDeviceView(APIView):
    """
    device_id로 최신 스크린샷 조회 (강사용)

    GET /api/sessions/{session_id}/screenshots/by-device/{device_id}/
    """
    permission_classes = [IsAuthenticated]

    def get(self, request, session_id, device_id):
        session = get_object_or_404(LectureSession, id=session_id)

        # 강사 권한 확인
        if session.instructor != request.user:
            return Response(
                {'error': '권한이 없습니다.'},
                status=status.HTTP_403_FORBIDDEN
            )

        # device_id의 최신 스크린샷 조회
        screenshot = StudentScreenshot.objects.filter(
            session=session,
            device_id=device_id
        ).order_by('-captured_at').first()

        if not screenshot:
            return Response(
                {'error': '스크린샷이 없습니다.'},
                status=status.HTTP_404_NOT_FOUND
            )

        serializer = StudentScreenshotSerializer(
            screenshot,
            context={'request': request}
        )
        return Response(serializer.data)
