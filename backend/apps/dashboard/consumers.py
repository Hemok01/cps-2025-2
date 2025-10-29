"""
WebSocket Consumer for Instructor Dashboard
"""
import json
from channels.generic.websocket import AsyncWebsocketConsumer
from channels.db import database_sync_to_async
from django.contrib.auth import get_user_model

User = get_user_model()


class DashboardConsumer(AsyncWebsocketConsumer):
    """
    WebSocket Consumer for instructor dashboard real-time updates

    URL: ws://localhost:8000/ws/dashboard/lectures/<lecture_id>/

    Messages to client:
    - progress_update: Student progress updated
    - help_request: New help request from student
    - participant_status: Student joined/left session
    - statistics_update: Lecture statistics updated

    Messages from client:
    - request_statistics: Request current statistics
    - request_student_list: Request current student list
    """

    async def connect(self):
        """Handle WebSocket connection"""
        self.lecture_id = self.scope['url_route']['kwargs']['lecture_id']
        self.lecture_group_name = f'dashboard_lecture_{self.lecture_id}'
        self.user = self.scope['user']

        # Check if user is authenticated and is an instructor
        if not self.user.is_authenticated or self.user.role != 'INSTRUCTOR':
            await self.close()
            return

        # Verify lecture exists and user is the instructor
        lecture = await self.get_lecture()
        if not lecture or lecture.instructor_id != self.user.id:
            await self.close()
            return

        # Join lecture dashboard group
        await self.channel_layer.group_add(
            self.lecture_group_name,
            self.channel_name
        )

        await self.accept()

        # Send initial data
        initial_data = await self.get_initial_dashboard_data()
        await self.send(text_data=json.dumps({
            'type': 'initial_data',
            'data': initial_data
        }))

    async def disconnect(self, close_code):
        """Handle WebSocket disconnection"""
        # Leave lecture dashboard group
        await self.channel_layer.group_discard(
            self.lecture_group_name,
            self.channel_name
        )

    async def receive(self, text_data):
        """Handle incoming WebSocket messages"""
        try:
            data = json.loads(text_data)
            message_type = data.get('type')

            if message_type == 'request_statistics':
                await self.handle_request_statistics()
            elif message_type == 'request_student_list':
                await self.handle_request_student_list()
            else:
                await self.send(text_data=json.dumps({
                    'error': 'Unknown message type'
                }))

        except json.JSONDecodeError:
            await self.send(text_data=json.dumps({
                'error': 'Invalid JSON'
            }))
        except Exception as e:
            await self.send(text_data=json.dumps({
                'error': str(e)
            }))

    async def handle_request_statistics(self):
        """Send current lecture statistics"""
        statistics = await self.get_lecture_statistics()
        await self.send(text_data=json.dumps({
            'type': 'statistics',
            'data': statistics
        }))

    async def handle_request_student_list(self):
        """Send current student list with progress"""
        students = await self.get_student_list()
        await self.send(text_data=json.dumps({
            'type': 'student_list',
            'data': students
        }))

    # Broadcast message handlers
    async def progress_update(self, event):
        """Send progress update notification to instructor"""
        await self.send(text_data=json.dumps({
            'type': 'progress_update',
            'user_id': event['user_id'],
            'user_name': event['user_name'],
            'subtask_id': event['subtask_id'],
            'status': event['status'],
            'progress_rate': event.get('progress_rate')
        }))

    async def help_request(self, event):
        """Send help request notification to instructor"""
        await self.send(text_data=json.dumps({
            'type': 'help_request',
            'request_id': event['request_id'],
            'user_id': event['user_id'],
            'user_name': event['user_name'],
            'subtask_id': event['subtask_id'],
            'message': event.get('message', ''),
            'request_type': event.get('request_type', 'MANUAL')
        }))

    async def participant_status(self, event):
        """Send participant status change notification"""
        await self.send(text_data=json.dumps({
            'type': 'participant_status',
            'user_id': event['user_id'],
            'user_name': event['user_name'],
            'status': event['status'],  # 'joined' or 'left'
            'session_code': event.get('session_code')
        }))

    async def statistics_update(self, event):
        """Send statistics update notification"""
        await self.send(text_data=json.dumps({
            'type': 'statistics_update',
            'statistics': event['statistics']
        }))

    # Database queries
    @database_sync_to_async
    def get_lecture(self):
        """Get lecture by ID"""
        from apps.lectures.models import Lecture
        try:
            return Lecture.objects.get(id=self.lecture_id)
        except Lecture.DoesNotExist:
            return None

    @database_sync_to_async
    def get_initial_dashboard_data(self):
        """Get initial dashboard data"""
        from apps.lectures.models import Lecture, UserLectureEnrollment
        from apps.progress.models import UserProgress
        from apps.tasks.models import Task, Subtask
        from apps.help.models import HelpRequest

        try:
            lecture = Lecture.objects.get(id=self.lecture_id)

            # Get all enrolled students
            enrollments = UserLectureEnrollment.objects.filter(
                lecture=lecture
            ).select_related('user')

            # Get all subtasks for this lecture
            total_subtasks = Subtask.objects.filter(
                task__lecture=lecture
            ).count()

            students = []
            for enrollment in enrollments:
                user = enrollment.user

                # Get completed subtasks count
                completed_count = UserProgress.objects.filter(
                    user=user,
                    subtask__task__lecture=lecture,
                    status='COMPLETED'
                ).count()

                # Get pending help requests
                pending_help = HelpRequest.objects.filter(
                    user=user,
                    subtask__task__lecture=lecture,
                    status__in=['PENDING', 'ANALYZING']
                ).count()

                progress_rate = (completed_count / total_subtasks * 100) if total_subtasks > 0 else 0

                students.append({
                    'user_id': user.id,
                    'user_name': user.name,
                    'email': user.email,
                    'progress_rate': round(progress_rate, 2),
                    'completed_subtasks': completed_count,
                    'total_subtasks': total_subtasks,
                    'pending_help': pending_help,
                    'enrolled_at': enrollment.enrolled_at.isoformat()
                })

            # Get pending help requests count
            pending_help_total = HelpRequest.objects.filter(
                subtask__task__lecture=lecture,
                status__in=['PENDING', 'ANALYZING']
            ).count()

            return {
                'lecture': {
                    'id': lecture.id,
                    'title': lecture.title,
                    'description': lecture.description
                },
                'students': students,
                'total_students': len(students),
                'total_subtasks': total_subtasks,
                'pending_help_requests': pending_help_total
            }

        except Lecture.DoesNotExist:
            return None

    @database_sync_to_async
    def get_lecture_statistics(self):
        """Get lecture statistics"""
        from apps.lectures.models import Lecture
        from apps.progress.models import UserProgress
        from apps.help.models import HelpRequest
        from django.db.models import Count, Q

        try:
            lecture = Lecture.objects.get(id=self.lecture_id)

            # Get subtasks with difficulty (high help request count)
            difficult_subtasks = HelpRequest.objects.filter(
                subtask__task__lecture=lecture
            ).values(
                'subtask__id',
                'subtask__title'
            ).annotate(
                help_count=Count('id')
            ).order_by('-help_count')[:5]

            # Get average completion rate
            from apps.lectures.models import UserLectureEnrollment
            from apps.tasks.models import Subtask

            total_subtasks = Subtask.objects.filter(task__lecture=lecture).count()
            enrollments = UserLectureEnrollment.objects.filter(lecture=lecture).count()

            if enrollments > 0 and total_subtasks > 0:
                completed_progresses = UserProgress.objects.filter(
                    subtask__task__lecture=lecture,
                    status='COMPLETED'
                ).count()
                avg_completion_rate = (completed_progresses / (enrollments * total_subtasks) * 100)
            else:
                avg_completion_rate = 0

            return {
                'difficult_subtasks': list(difficult_subtasks),
                'average_completion_rate': round(avg_completion_rate, 2),
                'total_enrollments': enrollments,
                'total_subtasks': total_subtasks
            }

        except Lecture.DoesNotExist:
            return None

    @database_sync_to_async
    def get_student_list(self):
        """Get current student list with progress"""
        from apps.lectures.models import Lecture, UserLectureEnrollment
        from apps.progress.models import UserProgress
        from apps.tasks.models import Subtask

        try:
            lecture = Lecture.objects.get(id=self.lecture_id)
            total_subtasks = Subtask.objects.filter(task__lecture=lecture).count()

            enrollments = UserLectureEnrollment.objects.filter(
                lecture=lecture
            ).select_related('user')

            students = []
            for enrollment in enrollments:
                user = enrollment.user
                completed_count = UserProgress.objects.filter(
                    user=user,
                    subtask__task__lecture=lecture,
                    status='COMPLETED'
                ).count()

                progress_rate = (completed_count / total_subtasks * 100) if total_subtasks > 0 else 0

                # Get current subtask
                current_progress = UserProgress.objects.filter(
                    user=user,
                    subtask__task__lecture=lecture,
                    status='IN_PROGRESS'
                ).select_related('subtask').first()

                current_subtask = None
                if current_progress:
                    current_subtask = {
                        'id': current_progress.subtask.id,
                        'title': current_progress.subtask.title,
                        'order': current_progress.subtask.order
                    }

                students.append({
                    'user_id': user.id,
                    'user_name': user.name,
                    'progress_rate': round(progress_rate, 2),
                    'completed_subtasks': completed_count,
                    'current_subtask': current_subtask
                })

            return students

        except Lecture.DoesNotExist:
            return []
