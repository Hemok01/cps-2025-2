"""
Dashboard Views (강사용 모니터링)
"""
from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework.permissions import IsAuthenticated
from django.shortcuts import get_object_or_404
from django.db.models import Count, Q, Avg, F, ExpressionWrapper, DurationField
from django.utils import timezone

from apps.lectures.models import Lecture
from apps.progress.models import UserProgress
from apps.help.models import HelpRequest
from apps.sessions.models import SessionParticipant, LectureSession
from apps.tasks.models import Task, Subtask
from apps.progress.serializers import UserProgressSerializer
from apps.help.serializers import HelpRequestSerializer


class LectureStudentsView(APIView):
    """수강생 목록 및 진행률 (강사용)"""
    permission_classes = [IsAuthenticated]

    def get(self, request, lecture_id):
        lecture = get_object_or_404(Lecture, pk=lecture_id)
        
        # 강사 권한 확인
        if lecture.instructor != request.user:
            return Response(
                {'error': '강사만 수강생 목록을 조회할 수 있습니다.'},
                status=403
            )
        
        # 수강생 목록 및 진행 상태
        enrollments = lecture.enrollments.select_related('user').all()
        students = []
        
        for enrollment in enrollments:
            user = enrollment.user
            # 진행 상태 통계
            progress = UserProgress.objects.filter(
                user=user,
                subtask__task__lecture=lecture
            )
            
            total_subtasks = lecture.tasks.aggregate(
                count=Count('subtasks')
            )['count'] or 0
            
            completed_subtasks = progress.filter(status='COMPLETED').count()
            
            # 현재 진행 중인 subtask
            current_progress = progress.filter(status='IN_PROGRESS').first()
            
            # 도움 요청 횟수
            help_count = HelpRequest.objects.filter(
                user=user,
                subtask__task__lecture=lecture
            ).count()
            
            students.append({
                'user_id': user.id,
                'name': user.name,
                'email': user.email,
                'progress_rate': completed_subtasks / total_subtasks if total_subtasks > 0 else 0,
                'completed_subtasks': completed_subtasks,
                'total_subtasks': total_subtasks,
                'current_subtask': {
                    'id': current_progress.subtask.id if current_progress else None,
                    'title': current_progress.subtask.title if current_progress else None,
                    'status': current_progress.status if current_progress else None
                } if current_progress else None,
                'help_count': help_count,
                'last_activity': progress.order_by('-updated_at').first().updated_at if progress.exists() else None,
                'enrolled_at': enrollment.enrolled_at
            })
        
        return Response({
            'lecture_id': lecture_id,
            'students': students
        })


class PendingHelpRequestsView(APIView):
    """대기 중인 도움 요청 (실시간 알림용)"""
    permission_classes = [IsAuthenticated]

    def get(self, request):
        # 강사의 강의들
        lecture_ids = Lecture.objects.filter(instructor=request.user).values_list('id', flat=True)
        
        # 대기 중인 도움 요청
        pending_requests = HelpRequest.objects.filter(
            subtask__task__lecture_id__in=lecture_ids,
            status__in=['PENDING', 'ANALYZING']
        ).select_related('user', 'subtask').order_by('-created_at')
        
        serializer = HelpRequestSerializer(pending_requests, many=True)
        
        return Response({
            'pending_requests': serializer.data
        })


class LectureStatisticsView(APIView):
    """강의 통계 (차트용)"""
    permission_classes = [IsAuthenticated]

    def get(self, request, lecture_id):
        lecture = get_object_or_404(Lecture, pk=lecture_id)

        # 강사 권한 확인
        if lecture.instructor != request.user:
            return Response(
                {'error': '강사만 통계를 조회할 수 있습니다.'},
                status=403
            )

        # 전체 수강생 수
        total_students = lecture.enrollments.count()

        # 전체 서브태스크 수
        total_subtasks = lecture.tasks.aggregate(count=Count('subtasks'))['count'] or 0

        # 도움 요청 통계
        total_help_requests = HelpRequest.objects.filter(
            subtask__task__lecture=lecture
        ).count()

        # 평균 진행률 및 완료율 계산
        average_progress = 0
        completion_rate = 0

        if total_students > 0 and total_subtasks > 0:
            # 각 수강생의 완료된 서브태스크 수를 계산
            enrollments = lecture.enrollments.all()
            total_progress = 0
            completed_students = 0

            for enrollment in enrollments:
                user = enrollment.user
                completed_subtasks = UserProgress.objects.filter(
                    user=user,
                    subtask__task__lecture=lecture,
                    status='COMPLETED'
                ).count()

                user_progress = (completed_subtasks / total_subtasks) * 100
                total_progress += user_progress

                if completed_subtasks == total_subtasks:
                    completed_students += 1

            average_progress = round(total_progress / total_students)
            completion_rate = round((completed_students / total_students) * 100)

        # 어려운 단계 (도움 요청이 많은 순) - 상세 정보 포함
        difficult_steps_raw = HelpRequest.objects.filter(
            subtask__task__lecture=lecture
        ).values('subtask_id', 'subtask__title').annotate(
            help_count=Count('id')
        ).order_by('-help_count')[:10]

        difficult_steps = []
        for step in difficult_steps_raw:
            subtask_id = step['subtask_id']

            # 평균 소요 시간 계산
            progress_data = UserProgress.objects.filter(
                subtask_id=subtask_id,
                status='COMPLETED',
                started_at__isnull=False,
                completed_at__isnull=False
            ).annotate(
                time_spent=ExpressionWrapper(
                    F('completed_at') - F('started_at'),
                    output_field=DurationField()
                )
            ).aggregate(avg_time=Avg('time_spent'))

            avg_time = progress_data['avg_time']
            avg_time_seconds = avg_time.total_seconds() if avg_time else 0

            # 해당 단계를 시작한 학생 수
            student_count = UserProgress.objects.filter(subtask_id=subtask_id).count()

            difficult_steps.append({
                'subtask_name': step['subtask__title'],
                'help_request_count': step['help_count'],
                'avg_time_spent': round(avg_time_seconds),
                'student_count': student_count,
            })

        return Response({
            'lecture_id': lecture_id,
            'lecture_name': lecture.title,
            'total_students': total_students,
            'total_help_requests': total_help_requests,
            'average_progress': average_progress,
            'completion_rate': completion_rate,
            'difficult_steps': difficult_steps,
            'last_updated': timezone.now().isoformat(),
        })


class SessionProgressStatsView(APIView):
    """
    세션별 진도 통계 (실시간 모니터링용)
    GET /api/dashboard/sessions/{session_id}/progress-stats/
    """
    permission_classes = [IsAuthenticated]

    def get(self, request, session_id):
        from apps.sessions.models import LectureSession
        from apps.tasks.models import Task, Subtask
        from django.utils import timezone
        from datetime import timedelta

        session = get_object_or_404(LectureSession, pk=session_id)

        # 강사 권한 확인
        if session.instructor != request.user:
            return Response(
                {'error': '강사만 세션 진도 통계를 조회할 수 있습니다.'},
                status=403
            )

        # 세션의 모든 참가자 조회
        participants = session.participants.select_related('current_subtask', 'user').all()
        total_students = participants.count()

        if total_students == 0:
            return Response({
                'session_id': session_id,
                'total_students': 0,
                'groups': [
                    {'name': '완료', 'count': 0, 'percentage': 0},
                    {'name': '진행중', 'count': 0, 'percentage': 0},
                    {'name': '지연', 'count': 0, 'percentage': 0},
                    {'name': '미시작', 'count': 0, 'percentage': 0},
                ],
                'progress_data': []
            })

        # 강의의 전체 서브태스크 수 계산
        total_subtasks = 0
        if session.lecture:
            total_subtasks = Subtask.objects.filter(
                task__lecture=session.lecture
            ).count()

        # 현재 세션의 현재 단계 order_index 가져오기
        current_session_step_index = 0
        if session.current_subtask:
            current_session_step_index = session.current_subtask.order_index

        # 참가자별 진도 상태 분류
        completed_count = 0
        in_progress_count = 0
        delayed_count = 0
        not_started_count = 0

        # 지연 판단 기준: 마지막 활동이 5분 이상 전
        delay_threshold = timezone.now() - timedelta(minutes=5)

        progress_data = []
        for participant in participants:
            # 참가자의 현재 단계
            participant_step_index = 0
            if participant.current_subtask:
                participant_step_index = participant.current_subtask.order_index

            # 상태 판단
            if participant.status == 'COMPLETED':
                status = 'completed'
                completed_count += 1
            elif participant.status == 'WAITING':
                status = 'not_started'
                not_started_count += 1
            elif participant.last_active_at and participant.last_active_at < delay_threshold:
                # 마지막 활동이 5분 이상 전이면 지연
                status = 'delayed'
                delayed_count += 1
            else:
                status = 'in_progress'
                in_progress_count += 1

            # 진행률 계산
            progress_percentage = 0
            if total_subtasks > 0:
                progress_percentage = int((participant_step_index / total_subtasks) * 100)

            progress_data.append({
                'user_id': participant.user.id if participant.user else None,
                'device_id': participant.device_id,
                'username': participant.display_name or (participant.user.name if participant.user else 'Anonymous'),
                'current_subtask': {
                    'id': participant.current_subtask.id if participant.current_subtask else None,
                    'title': participant.current_subtask.title if participant.current_subtask else None,
                    'order_index': participant.current_subtask.order_index if participant.current_subtask else 0,
                } if participant.current_subtask else None,
                'progress_percentage': progress_percentage,
                'status': status,
                'last_active_at': participant.last_active_at.isoformat() if participant.last_active_at else None,
            })

        # 그룹별 비율 계산
        groups = [
            {
                'name': '완료',
                'count': completed_count,
                'percentage': int((completed_count / total_students) * 100) if total_students > 0 else 0,
            },
            {
                'name': '진행중',
                'count': in_progress_count,
                'percentage': int((in_progress_count / total_students) * 100) if total_students > 0 else 0,
            },
            {
                'name': '지연',
                'count': delayed_count,
                'percentage': int((delayed_count / total_students) * 100) if total_students > 0 else 0,
            },
            {
                'name': '미시작',
                'count': not_started_count,
                'percentage': int((not_started_count / total_students) * 100) if total_students > 0 else 0,
            },
        ]

        return Response({
            'session_id': session_id,
            'total_students': total_students,
            'total_subtasks': total_subtasks,
            'current_session_step': current_session_step_index,
            'groups': groups,
            'progress_data': progress_data,
        })


class StepAnalysisView(APIView):
    """
    단계별 병목 분석
    GET /api/dashboard/statistics/lecture/{lecture_id}/step-analysis/

    각 단계별로 지체율, 도움요청 횟수, 평균 소요시간, 병목 점수를 제공합니다.
    """
    permission_classes = [IsAuthenticated]

    def get(self, request, lecture_id):
        lecture = get_object_or_404(Lecture, pk=lecture_id)

        # 강사 권한 확인
        if lecture.instructor != request.user:
            return Response(
                {'error': '강사만 통계를 조회할 수 있습니다.'},
                status=403
            )

        # 강의의 모든 서브태스크 가져오기
        subtasks = Subtask.objects.filter(
            task__lecture=lecture
        ).select_related('task').order_by('task__order_index', 'order_index')

        step_analysis = []
        max_help_count = 0
        most_delayed_step = None
        most_help_requested_step = None
        total_delay_rate = 0

        for subtask in subtasks:
            # 해당 단계의 진행 데이터 조회
            progress_queryset = UserProgress.objects.filter(subtask=subtask)

            # 완료된 진행 데이터 (시간 계산용)
            completed_progress = progress_queryset.filter(
                status='COMPLETED',
                started_at__isnull=False,
                completed_at__isnull=False
            )

            # 평균 소요 시간 계산
            avg_time_data = completed_progress.annotate(
                time_spent=ExpressionWrapper(
                    F('completed_at') - F('started_at'),
                    output_field=DurationField()
                )
            ).aggregate(avg_time=Avg('time_spent'))

            avg_time = avg_time_data['avg_time']
            avg_time_seconds = avg_time.total_seconds() if avg_time else 0

            # 지체율 계산 (평균의 2배 이상 소요한 학생 비율)
            completed_count = completed_progress.count()
            delay_rate = 0

            if avg_time and completed_count > 0:
                # 평균의 2배 이상 소요한 학생 수 계산
                delayed_count = completed_progress.annotate(
                    time_spent=ExpressionWrapper(
                        F('completed_at') - F('started_at'),
                        output_field=DurationField()
                    )
                ).filter(time_spent__gte=avg_time * 2).count()

                delay_rate = delayed_count / completed_count if completed_count > 0 else 0

            # 도움 요청 횟수
            help_count = HelpRequest.objects.filter(subtask=subtask).count()
            if help_count > max_help_count:
                max_help_count = help_count
                most_help_requested_step = subtask.title

            # 전체 학생 수 (해당 단계를 시작한 학생)
            student_count = progress_queryset.count()

            # 완료율
            completion_rate = completed_count / student_count if student_count > 0 else 0

            # 병목 점수 계산 (0-1, 높을수록 병목)
            # 가중치: 지체율 40%, 도움요청 비율 40%, 미완료율 20%
            help_rate = min(help_count / student_count, 1) if student_count > 0 else 0
            bottleneck_score = (
                (delay_rate * 0.4) +
                (help_rate * 0.4) +
                ((1 - completion_rate) * 0.2)
            )
            bottleneck_score = round(min(bottleneck_score, 1), 2)

            # 가장 지체율이 높은 단계 추적
            if delay_rate > 0 and (most_delayed_step is None or delay_rate > total_delay_rate):
                most_delayed_step = subtask.title

            total_delay_rate += delay_rate

            step_analysis.append({
                'subtask_id': subtask.id,
                'subtask_name': subtask.title,
                'task_name': subtask.task.title,
                'order_index': subtask.order_index,
                'avg_time_spent': round(avg_time_seconds),
                'delay_rate': round(delay_rate, 2),
                'help_request_count': help_count,
                'student_count': student_count,
                'completion_rate': round(completion_rate, 2),
                'bottleneck_score': bottleneck_score,
            })

        # 전체 평균 지체율 계산
        avg_overall_delay_rate = total_delay_rate / len(step_analysis) if step_analysis else 0

        return Response({
            'lecture_id': lecture_id,
            'lecture_name': lecture.title,
            'total_subtasks': len(step_analysis),
            'step_analysis': step_analysis,
            'summary': {
                'most_delayed_step': most_delayed_step,
                'most_help_requested_step': most_help_requested_step,
                'avg_overall_delay_rate': round(avg_overall_delay_rate, 2),
            },
            'last_updated': timezone.now().isoformat(),
        })


class SessionTrendsView(APIView):
    """
    세션 간 추이 비교
    GET /api/dashboard/statistics/lecture/{lecture_id}/session-trends/

    여러 세션의 결과를 비교하여 강의 개선 추이를 확인합니다.
    """
    permission_classes = [IsAuthenticated]

    def get(self, request, lecture_id):
        lecture = get_object_or_404(Lecture, pk=lecture_id)

        # 강사 권한 확인
        if lecture.instructor != request.user:
            return Response(
                {'error': '강사만 통계를 조회할 수 있습니다.'},
                status=403
            )

        # 종료된 세션만 조회 (시간순)
        sessions = LectureSession.objects.filter(
            lecture=lecture,
            status='ENDED'
        ).order_by('started_at')

        session_data = []
        for session in sessions:
            participants = session.participants.all()
            participant_count = participants.count()

            if participant_count == 0:
                continue

            # 완료율 계산
            completed_count = participants.filter(status='COMPLETED').count()
            completion_rate = completed_count / participant_count

            # 평균 완료 시간 계산
            completed_participants = participants.filter(
                completed_at__isnull=False,
                joined_at__isnull=False
            )

            avg_completion_time = 0
            if completed_participants.exists():
                total_time = 0
                count = 0
                for p in completed_participants:
                    if p.completed_at and p.joined_at:
                        time_diff = (p.completed_at - p.joined_at).total_seconds()
                        total_time += time_diff
                        count += 1
                avg_completion_time = total_time / count if count > 0 else 0

            # 도움 요청 수
            help_requests = HelpRequest.objects.filter(
                subtask__task__lecture=lecture,
                created_at__gte=session.started_at if session.started_at else timezone.now(),
                created_at__lte=session.ended_at if session.ended_at else timezone.now()
            ).count()

            session_data.append({
                'session_id': session.id,
                'session_title': session.title or f'세션 {session.id}',
                'session_date': session.started_at.date().isoformat() if session.started_at else None,
                'started_at': session.started_at.isoformat() if session.started_at else None,
                'ended_at': session.ended_at.isoformat() if session.ended_at else None,
                'participant_count': participant_count,
                'completion_rate': round(completion_rate, 2),
                'avg_completion_time': round(avg_completion_time),
                'total_help_requests': help_requests,
                'help_request_rate': round(help_requests / participant_count, 2) if participant_count > 0 else 0,
            })

        # 추세 계산
        trend_summary = self._calculate_trends(session_data)

        return Response({
            'lecture_id': lecture_id,
            'lecture_name': lecture.title,
            'sessions': session_data,
            'trend_summary': trend_summary,
            'last_updated': timezone.now().isoformat(),
        })

    def _calculate_trends(self, session_data):
        """추세 계산"""
        if len(session_data) < 2:
            return {
                'completion_rate_trend': 'insufficient_data',
                'help_request_trend': 'insufficient_data',
                'avg_completion_time_trend': 'insufficient_data',
            }

        # 최근 세션과 이전 세션 비교
        recent_count = min(3, len(session_data))
        recent = session_data[-recent_count:]
        earlier = session_data[:-recent_count] if len(session_data) > recent_count else []

        if not earlier:
            # 데이터가 부족하면 최근 2개만 비교
            if len(session_data) >= 2:
                recent = [session_data[-1]]
                earlier = [session_data[-2]]
            else:
                return {
                    'completion_rate_trend': 'insufficient_data',
                    'help_request_trend': 'insufficient_data',
                    'avg_completion_time_trend': 'insufficient_data',
                }

        # 완료율 추세
        recent_completion = sum(s['completion_rate'] for s in recent) / len(recent)
        earlier_completion = sum(s['completion_rate'] for s in earlier) / len(earlier)
        completion_diff = recent_completion - earlier_completion

        if completion_diff > 0.05:
            completion_trend = 'improving'
        elif completion_diff < -0.05:
            completion_trend = 'declining'
        else:
            completion_trend = 'stable'

        # 도움 요청 추세 (낮을수록 좋음)
        recent_help = sum(s['help_request_rate'] for s in recent) / len(recent)
        earlier_help = sum(s['help_request_rate'] for s in earlier) / len(earlier)
        help_diff = recent_help - earlier_help

        if help_diff < -0.1:
            help_trend = 'improving'  # 도움 요청 감소
        elif help_diff > 0.1:
            help_trend = 'declining'  # 도움 요청 증가
        else:
            help_trend = 'stable'

        # 완료 시간 추세 (낮을수록 좋음)
        recent_time = sum(s['avg_completion_time'] for s in recent) / len(recent)
        earlier_time = sum(s['avg_completion_time'] for s in earlier) / len(earlier)

        if earlier_time > 0:
            time_diff_ratio = (recent_time - earlier_time) / earlier_time
            if time_diff_ratio < -0.1:
                time_trend = 'improving'  # 시간 단축
            elif time_diff_ratio > 0.1:
                time_trend = 'declining'  # 시간 증가
            else:
                time_trend = 'stable'
        else:
            time_trend = 'stable'

        return {
            'completion_rate_trend': completion_trend,
            'help_request_trend': help_trend,
            'avg_completion_time_trend': time_trend,
        }
