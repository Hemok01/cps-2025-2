"""
Help Request Views
"""
from rest_framework import generics, status
from rest_framework.response import Response
from rest_framework.permissions import IsAuthenticated
from rest_framework.views import APIView
from django.shortcuts import get_object_or_404

from .models import HelpRequest, MGptAnalysis, HelpResponse
from .serializers import (
    HelpRequestSerializer,
    HelpRequestCreateSerializer,
    HelpResponseSerializer
)


class HelpRequestCreateView(generics.CreateAPIView):
    """도움 요청"""
    serializer_class = HelpRequestCreateSerializer
    permission_classes = [IsAuthenticated]

    def create(self, request, *args, **kwargs):
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        help_request = serializer.save(user=request.user, status='PENDING')
        
        # TODO: Kafka를 통해 M-GPT 분석 요청 (향후 구현)
        
        return Response({
            'help_request_id': help_request.id,
            'status': help_request.status,
            'message': '분석 중입니다. 잠시만 기다려주세요.'
        }, status=status.HTTP_201_CREATED)


class HelpRequestDetailView(APIView):
    """도움 요청 상태 조회"""
    permission_classes = [IsAuthenticated]

    def get(self, request, help_request_id):
        help_request = get_object_or_404(HelpRequest, pk=help_request_id)
        
        # 자신의 요청인지 확인
        if help_request.user != request.user:
            return Response(
                {'error': '자신의 도움 요청만 조회할 수 있습니다.'},
                status=status.HTTP_403_FORBIDDEN
            )
        
        response_data = {
            'help_request_id': help_request.id,
            'status': help_request.status,
        }
        
        # M-GPT 분석 결과가 있으면 포함
        if hasattr(help_request, 'mgpt_analysis'):
            analysis = help_request.mgpt_analysis
            response_data['analysis'] = {
                'problem_diagnosis': analysis.problem_diagnosis,
                'confidence_score': analysis.confidence_score
            }
        
        # 도움 응답이 있으면 포함
        help_response = help_request.responses.first()
        if help_response:
            response_data['help_response'] = {
                'help_type': help_response.help_type,
                'help_content': help_response.help_content,
                'voice_guide_text': help_response.help_content  # TTS용
            }
        
        return Response(response_data)


class HelpRequestResolveView(APIView):
    """도움 요청 해결 처리 (강사용)"""
    permission_classes = [IsAuthenticated]

    def post(self, request, help_request_id):
        help_request = get_object_or_404(HelpRequest, pk=help_request_id)

        # 강사 권한 확인
        if help_request.subtask and help_request.subtask.task.lecture.instructor != request.user:
            return Response(
                {'error': '해당 강의의 강사만 도움 요청을 해결할 수 있습니다.'},
                status=status.HTTP_403_FORBIDDEN
            )

        # 도움 요청 해결 처리
        from django.utils import timezone
        help_request.status = 'RESOLVED'
        help_request.resolved_at = timezone.now()
        help_request.save()

        serializer = HelpRequestSerializer(help_request)
        return Response(serializer.data)


class HelpFeedbackView(APIView):
    """도움에 대한 피드백 제출"""
    permission_classes = [IsAuthenticated]

    def post(self, request):
        help_response_id = request.data.get('help_response_id')
        rating = request.data.get('rating')
        feedback_text = request.data.get('feedback_text', '')

        if not help_response_id:
            return Response(
                {'error': 'help_response_id가 필요합니다.'},
                status=status.HTTP_400_BAD_REQUEST
            )

        help_response = get_object_or_404(HelpResponse, pk=help_response_id)

        # 자신이 받은 도움인지 확인
        if help_response.help_request.user != request.user:
            return Response(
                {'error': '자신이 받은 도움에만 피드백을 남길 수 있습니다.'},
                status=status.HTTP_403_FORBIDDEN
            )

        help_response.feedback_rating = rating
        help_response.feedback_text = feedback_text
        from django.utils import timezone
        help_response.feedback_at = timezone.now()
        help_response.save()

        return Response({
            'message': '피드백이 저장되었습니다.',
            'help_response_id': help_response_id
        })
