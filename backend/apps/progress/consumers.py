"""
WebSocket Consumer for Student Progress Tracking
"""
import json
from channels.generic.websocket import AsyncWebsocketConsumer
from channels.db import database_sync_to_async
from django.contrib.auth import get_user_model

User = get_user_model()


class ProgressConsumer(AsyncWebsocketConsumer):
    """
    WebSocket Consumer for student progress tracking

    URL: ws://localhost:8000/ws/progress/<user_id>/

    Messages to client:
    - progress_updated: Progress status updated
    - next_subtask: Information about next subtask
    - achievement_unlocked: Achievement notification
    - encouragement: Encouragement message based on progress

    Messages from client:
    - get_my_progress: Request current progress summary
    - mark_complete: Mark current subtask as complete
    """

    async def connect(self):
        """Handle WebSocket connection"""
        self.user_id = self.scope['url_route']['kwargs']['user_id']
        self.user_group_name = f'progress_user_{self.user_id}'
        self.user = self.scope['user']

        # Check if user is authenticated and accessing their own progress
        if not self.user.is_authenticated or str(self.user.id) != str(self.user_id):
            await self.close()
            return

        # Join user progress group
        await self.channel_layer.group_add(
            self.user_group_name,
            self.channel_name
        )

        await self.accept()

        # Send initial progress data
        progress_data = await self.get_progress_summary()
        await self.send(text_data=json.dumps({
            'type': 'initial_progress',
            'data': progress_data
        }))

    async def disconnect(self, close_code):
        """Handle WebSocket disconnection"""
        # Leave user progress group
        await self.channel_layer.group_discard(
            self.user_group_name,
            self.channel_name
        )

    async def receive(self, text_data):
        """Handle incoming WebSocket messages"""
        try:
            data = json.loads(text_data)
            message_type = data.get('type')

            if message_type == 'get_my_progress':
                await self.handle_get_progress()
            elif message_type == 'mark_complete':
                await self.handle_mark_complete(data)
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

    async def handle_get_progress(self):
        """Send current progress summary"""
        progress_data = await self.get_progress_summary()
        await self.send(text_data=json.dumps({
            'type': 'progress_summary',
            'data': progress_data
        }))

    async def handle_mark_complete(self, data):
        """Handle marking subtask as complete"""
        subtask_id = data.get('subtask_id')
        if not subtask_id:
            await self.send(text_data=json.dumps({
                'error': 'subtask_id is required'
            }))
            return

        # Update progress
        result = await self.mark_subtask_complete(subtask_id)
        if not result['success']:
            await self.send(text_data=json.dumps({
                'error': result['error']
            }))
            return

        # Send completion confirmation
        await self.send(text_data=json.dumps({
            'type': 'subtask_completed',
            'subtask_id': subtask_id,
            'next_subtask': result.get('next_subtask')
        }))

        # Notify instructor dashboard
        await self.notify_dashboard_progress_update(subtask_id, 'COMPLETED')

        # Check for achievements
        achievements = await self.check_achievements()
        if achievements:
            await self.send(text_data=json.dumps({
                'type': 'achievement_unlocked',
                'achievements': achievements
            }))

    # Broadcast message handlers
    async def progress_updated(self, event):
        """Send progress update notification to client"""
        await self.send(text_data=json.dumps({
            'type': 'progress_updated',
            'subtask_id': event['subtask_id'],
            'status': event['status'],
            'message': event.get('message', '')
        }))

    async def next_subtask(self, event):
        """Send next subtask information to client"""
        await self.send(text_data=json.dumps({
            'type': 'next_subtask',
            'subtask': event['subtask']
        }))

    async def achievement_unlocked(self, event):
        """Send achievement notification to client"""
        await self.send(text_data=json.dumps({
            'type': 'achievement_unlocked',
            'achievement': event['achievement']
        }))

    async def encouragement(self, event):
        """Send encouragement message to client"""
        await self.send(text_data=json.dumps({
            'type': 'encouragement',
            'message': event['message'],
            'progress_rate': event.get('progress_rate')
        }))

    # Helper methods
    async def notify_dashboard_progress_update(self, subtask_id, status):
        """Notify instructor dashboard about progress update"""
        # Get lecture ID for this subtask
        lecture_id = await self.get_lecture_id_for_subtask(subtask_id)
        if not lecture_id:
            return

        # Get progress rate
        progress_rate = await self.get_user_progress_rate(lecture_id)

        # Send to dashboard
        dashboard_group_name = f'dashboard_lecture_{lecture_id}'
        await self.channel_layer.group_send(
            dashboard_group_name,
            {
                'type': 'progress_update',
                'user_id': self.user.id,
                'user_name': self.user.name,
                'subtask_id': subtask_id,
                'status': status,
                'progress_rate': progress_rate
            }
        )

    # Database queries
    @database_sync_to_async
    def get_progress_summary(self):
        """Get user progress summary across all lectures"""
        from apps.lectures.models import UserLectureEnrollment
        from apps.progress.models import UserProgress
        from apps.tasks.models import Subtask

        enrollments = UserLectureEnrollment.objects.filter(
            user=self.user
        ).select_related('lecture')

        summary = []
        for enrollment in enrollments:
            lecture = enrollment.lecture

            # Get total subtasks for this lecture
            total_subtasks = Subtask.objects.filter(
                task__lecture=lecture
            ).count()

            # Get completed subtasks
            completed = UserProgress.objects.filter(
                user=self.user,
                subtask__task__lecture=lecture,
                status='COMPLETED'
            ).count()

            # Get in-progress subtasks
            in_progress = UserProgress.objects.filter(
                user=self.user,
                subtask__task__lecture=lecture,
                status='IN_PROGRESS'
            ).select_related('subtask').first()

            current_subtask = None
            if in_progress:
                current_subtask = {
                    'id': in_progress.subtask.id,
                    'title': in_progress.subtask.title,
                    'order': in_progress.subtask.order,
                    'guide_text': in_progress.subtask.guide_text
                }

            progress_rate = (completed / total_subtasks * 100) if total_subtasks > 0 else 0

            summary.append({
                'lecture_id': lecture.id,
                'lecture_title': lecture.title,
                'total_subtasks': total_subtasks,
                'completed_subtasks': completed,
                'progress_rate': round(progress_rate, 2),
                'current_subtask': current_subtask
            })

        return {
            'user': {
                'id': self.user.id,
                'name': self.user.name,
                'email': self.user.email
            },
            'lectures': summary
        }

    @database_sync_to_async
    def mark_subtask_complete(self, subtask_id):
        """Mark subtask as complete and get next subtask"""
        from apps.progress.models import UserProgress
        from apps.tasks.models import Subtask

        try:
            # Get or create progress
            subtask = Subtask.objects.get(id=subtask_id)
            progress, created = UserProgress.objects.get_or_create(
                user=self.user,
                subtask=subtask,
                defaults={'status': 'COMPLETED'}
            )

            if not created:
                progress.status = 'COMPLETED'
                progress.save()

            # Get next subtask
            next_subtask = Subtask.objects.filter(
                task=subtask.task,
                order__gt=subtask.order
            ).order_by('order').first()

            next_subtask_data = None
            if next_subtask:
                # Create progress for next subtask
                UserProgress.objects.get_or_create(
                    user=self.user,
                    subtask=next_subtask,
                    defaults={'status': 'IN_PROGRESS'}
                )

                next_subtask_data = {
                    'id': next_subtask.id,
                    'title': next_subtask.title,
                    'order': next_subtask.order,
                    'target_action': next_subtask.target_action,
                    'guide_text': next_subtask.guide_text,
                    'voice_guide_text': next_subtask.voice_guide_text
                }

            return {
                'success': True,
                'next_subtask': next_subtask_data
            }

        except Subtask.DoesNotExist:
            return {
                'success': False,
                'error': 'Subtask not found'
            }
        except Exception as e:
            return {
                'success': False,
                'error': str(e)
            }

    @database_sync_to_async
    def check_achievements(self):
        """Check for any new achievements"""
        from apps.progress.models import UserProgress

        achievements = []

        # Check total completed subtasks
        total_completed = UserProgress.objects.filter(
            user=self.user,
            status='COMPLETED'
        ).count()

        # Achievement milestones
        milestones = [10, 25, 50, 100, 200]
        for milestone in milestones:
            if total_completed == milestone:
                achievements.append({
                    'type': 'milestone',
                    'title': f'{milestone}개 단계 완료!',
                    'description': f'축하합니다! {milestone}개의 단계를 완료하셨습니다.',
                    'icon': 'trophy'
                })

        return achievements

    @database_sync_to_async
    def get_lecture_id_for_subtask(self, subtask_id):
        """Get lecture ID for a given subtask"""
        from apps.tasks.models import Subtask

        try:
            subtask = Subtask.objects.select_related('task__lecture').get(id=subtask_id)
            return subtask.task.lecture.id
        except Subtask.DoesNotExist:
            return None

    @database_sync_to_async
    def get_user_progress_rate(self, lecture_id):
        """Get user's progress rate for a lecture"""
        from apps.progress.models import UserProgress
        from apps.tasks.models import Subtask

        total_subtasks = Subtask.objects.filter(task__lecture_id=lecture_id).count()
        if total_subtasks == 0:
            return 0

        completed = UserProgress.objects.filter(
            user=self.user,
            subtask__task__lecture_id=lecture_id,
            status='COMPLETED'
        ).count()

        return round((completed / total_subtasks * 100), 2)
