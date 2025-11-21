"""
Lecture Views
"""
from rest_framework import generics, status
from rest_framework.response import Response
from rest_framework.permissions import IsAuthenticated
from django.shortcuts import get_object_or_404

from .models import Lecture, UserLectureEnrollment
from .serializers import (
    LectureSerializer,
    LectureCreateUpdateSerializer,
    EnrollmentSerializer
)


class LectureListCreateView(generics.ListCreateAPIView):
    """List all lectures or create new lecture"""
    queryset = Lecture.objects.filter(is_active=True).select_related('instructor')
    permission_classes = [IsAuthenticated]

    def get_serializer_class(self):
        if self.request.method == 'POST':
            return LectureCreateUpdateSerializer
        return LectureSerializer

    def perform_create(self, serializer):
        """Set instructor to current user"""
        serializer.save(instructor=self.request.user)

    def create(self, request, *args, **kwargs):
        """Override create to return full lecture data"""
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        self.perform_create(serializer)

        # Return with full LectureSerializer
        lecture = serializer.instance
        output_serializer = LectureSerializer(lecture)
        headers = self.get_success_headers(output_serializer.data)
        return Response(output_serializer.data, status=status.HTTP_201_CREATED, headers=headers)


class LectureDetailView(generics.RetrieveUpdateDestroyAPIView):
    """Retrieve, update or delete a lecture"""
    queryset = Lecture.objects.all()
    permission_classes = [IsAuthenticated]

    def get_serializer_class(self):
        if self.request.method in ['PUT', 'PATCH']:
            return LectureCreateUpdateSerializer
        return LectureSerializer


class LectureEnrollView(generics.CreateAPIView):
    """Enroll in a lecture"""
    serializer_class = EnrollmentSerializer
    permission_classes = [IsAuthenticated]

    def create(self, request, *args, **kwargs):
        """Enroll user in lecture"""
        lecture_id = kwargs.get('pk')
        lecture = get_object_or_404(Lecture, pk=lecture_id)

        enrollment, created = UserLectureEnrollment.objects.get_or_create(
            user=request.user,
            lecture=lecture
        )

        if not created:
            return Response(
                {'message': '이미 등록된 강의입니다.'},
                status=status.HTTP_400_BAD_REQUEST
            )

        serializer = self.get_serializer(enrollment)
        return Response(serializer.data, status=status.HTTP_201_CREATED)
