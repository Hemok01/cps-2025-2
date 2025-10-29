"""
WebSocket Consumers for Real-time Session Communication
"""
import json
from channels.generic.websocket import AsyncWebsocketConsumer
from channels.db import database_sync_to_async
from django.contrib.auth import get_user_model
from .models import LectureSession, SessionParticipant

User = get_user_model()


class SessionConsumer(AsyncWebsocketConsumer):
    """
    WebSocket Consumer for real-time session synchronization

    URL: ws://localhost:8000/ws/sessions/<session_code>/

    Messages:
    - From Instructor:
      - next_step: Move all participants to next subtask
      - pause_session: Pause the session
      - resume_session: Resume the session
      - end_session: End the session

    - From Student:
      - progress_update: Update student progress
      - help_request: Request help from instructor

    - Broadcast to all:
      - step_changed: Notify step change
      - session_status_changed: Notify status change
      - participant_joined: New participant joined
      - participant_left: Participant left
    """

    async def connect(self):
        """Handle WebSocket connection"""
        self.session_code = self.scope['url_route']['kwargs']['session_code']
        self.session_group_name = f'session_{self.session_code}'
        self.user = self.scope['user']

        # Check if user is authenticated
        if not self.user.is_authenticated:
            await self.close()
            return

        # Verify session exists and user is participant or instructor
        session = await self.get_session()
        if not session:
            await self.close()
            return

        # Check if user is instructor or enrolled participant
        is_valid = await self.check_user_access(session)
        if not is_valid:
            await self.close()
            return

        # Join session group
        await self.channel_layer.group_add(
            self.session_group_name,
            self.channel_name
        )

        await self.accept()

        # Notify others that user joined
        await self.channel_layer.group_send(
            self.session_group_name,
            {
                'type': 'participant_joined',
                'user_id': self.user.id,
                'user_name': self.user.name,
                'role': self.user.role
            }
        )

    async def disconnect(self, close_code):
        """Handle WebSocket disconnection"""
        # Notify others that user left
        await self.channel_layer.group_send(
            self.session_group_name,
            {
                'type': 'participant_left',
                'user_id': self.user.id,
                'user_name': self.user.name
            }
        )

        # Leave session group
        await self.channel_layer.group_discard(
            self.session_group_name,
            self.channel_name
        )

    async def receive(self, text_data):
        """Handle incoming WebSocket messages"""
        try:
            data = json.loads(text_data)
            message_type = data.get('type')

            # Instructor commands
            if message_type == 'next_step':
                await self.handle_next_step(data)
            elif message_type == 'pause_session':
                await self.handle_pause_session()
            elif message_type == 'resume_session':
                await self.handle_resume_session()
            elif message_type == 'end_session':
                await self.handle_end_session()

            # Student messages
            elif message_type == 'progress_update':
                await self.handle_progress_update(data)
            elif message_type == 'help_request':
                await self.handle_help_request(data)

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

    # Instructor command handlers
    async def handle_next_step(self, data):
        """Handle instructor moving to next step"""
        if self.user.role != 'INSTRUCTOR':
            await self.send(text_data=json.dumps({
                'error': 'Only instructors can move to next step'
            }))
            return

        subtask_id = data.get('subtask_id')
        if not subtask_id:
            await self.send(text_data=json.dumps({
                'error': 'subtask_id is required'
            }))
            return

        # Update session current subtask
        success = await self.update_session_subtask(subtask_id)
        if not success:
            await self.send(text_data=json.dumps({
                'error': 'Failed to update session subtask'
            }))
            return

        # Get subtask details
        subtask = await self.get_subtask_details(subtask_id)

        # Broadcast to all participants
        await self.channel_layer.group_send(
            self.session_group_name,
            {
                'type': 'step_changed',
                'subtask': subtask
            }
        )

    async def handle_pause_session(self):
        """Handle instructor pausing session"""
        if self.user.role != 'INSTRUCTOR':
            await self.send(text_data=json.dumps({
                'error': 'Only instructors can pause session'
            }))
            return

        await self.update_session_status('PAUSED')

        await self.channel_layer.group_send(
            self.session_group_name,
            {
                'type': 'session_status_changed',
                'status': 'PAUSED',
                'message': '세션이 일시정지되었습니다'
            }
        )

    async def handle_resume_session(self):
        """Handle instructor resuming session"""
        if self.user.role != 'INSTRUCTOR':
            await self.send(text_data=json.dumps({
                'error': 'Only instructors can resume session'
            }))
            return

        await self.update_session_status('IN_PROGRESS')

        await self.channel_layer.group_send(
            self.session_group_name,
            {
                'type': 'session_status_changed',
                'status': 'IN_PROGRESS',
                'message': '세션이 재개되었습니다'
            }
        )

    async def handle_end_session(self):
        """Handle instructor ending session"""
        if self.user.role != 'INSTRUCTOR':
            await self.send(text_data=json.dumps({
                'error': 'Only instructors can end session'
            }))
            return

        await self.update_session_status('ENDED')

        await self.channel_layer.group_send(
            self.session_group_name,
            {
                'type': 'session_status_changed',
                'status': 'ENDED',
                'message': '세션이 종료되었습니다'
            }
        )

    # Student message handlers
    async def handle_progress_update(self, data):
        """Handle student progress update"""
        subtask_id = data.get('subtask_id')
        status = data.get('status')

        # Send progress update to instructor(s) only
        await self.channel_layer.group_send(
            self.session_group_name,
            {
                'type': 'progress_updated',
                'user_id': self.user.id,
                'user_name': self.user.name,
                'subtask_id': subtask_id,
                'status': status,
                'role_filter': 'INSTRUCTOR'  # Only send to instructors
            }
        )

    async def handle_help_request(self, data):
        """Handle student help request"""
        subtask_id = data.get('subtask_id')
        message = data.get('message', '')

        # Send help request to instructor(s)
        await self.channel_layer.group_send(
            self.session_group_name,
            {
                'type': 'help_requested',
                'user_id': self.user.id,
                'user_name': self.user.name,
                'subtask_id': subtask_id,
                'message': message,
                'role_filter': 'INSTRUCTOR'
            }
        )

    # Broadcast message handlers
    async def step_changed(self, event):
        """Send step changed notification to client"""
        await self.send(text_data=json.dumps({
            'type': 'step_changed',
            'subtask': event['subtask']
        }))

    async def session_status_changed(self, event):
        """Send session status changed notification to client"""
        await self.send(text_data=json.dumps({
            'type': 'session_status_changed',
            'status': event['status'],
            'message': event['message']
        }))

    async def participant_joined(self, event):
        """Send participant joined notification to client"""
        # Don't send to self
        if event['user_id'] == self.user.id:
            return

        await self.send(text_data=json.dumps({
            'type': 'participant_joined',
            'user_id': event['user_id'],
            'user_name': event['user_name'],
            'role': event['role']
        }))

    async def participant_left(self, event):
        """Send participant left notification to client"""
        # Don't send to self
        if event['user_id'] == self.user.id:
            return

        await self.send(text_data=json.dumps({
            'type': 'participant_left',
            'user_id': event['user_id'],
            'user_name': event['user_name']
        }))

    async def progress_updated(self, event):
        """Send progress update to instructors only"""
        # Filter by role - only send to instructors
        if event.get('role_filter') == 'INSTRUCTOR' and self.user.role != 'INSTRUCTOR':
            return

        await self.send(text_data=json.dumps({
            'type': 'progress_updated',
            'user_id': event['user_id'],
            'user_name': event['user_name'],
            'subtask_id': event['subtask_id'],
            'status': event['status']
        }))

    async def help_requested(self, event):
        """Send help request to instructors only"""
        # Filter by role - only send to instructors
        if event.get('role_filter') == 'INSTRUCTOR' and self.user.role != 'INSTRUCTOR':
            return

        await self.send(text_data=json.dumps({
            'type': 'help_requested',
            'user_id': event['user_id'],
            'user_name': event['user_name'],
            'subtask_id': event['subtask_id'],
            'message': event['message']
        }))

    # Database queries
    @database_sync_to_async
    def get_session(self):
        """Get session by code"""
        try:
            return LectureSession.objects.get(session_code=self.session_code)
        except LectureSession.DoesNotExist:
            return None

    @database_sync_to_async
    def check_user_access(self, session):
        """Check if user has access to session"""
        # Instructor always has access to their own sessions
        if session.instructor_id == self.user.id:
            return True

        # Check if student is a participant
        return SessionParticipant.objects.filter(
            session=session,
            user=self.user,
            status='ACTIVE'
        ).exists()

    @database_sync_to_async
    def update_session_subtask(self, subtask_id):
        """Update session current subtask"""
        try:
            session = LectureSession.objects.get(session_code=self.session_code)
            session.current_subtask_id = subtask_id
            session.save()
            return True
        except Exception:
            return False

    @database_sync_to_async
    def update_session_status(self, status):
        """Update session status"""
        try:
            session = LectureSession.objects.get(session_code=self.session_code)
            session.status = status
            session.save()
            return True
        except Exception:
            return False

    @database_sync_to_async
    def get_subtask_details(self, subtask_id):
        """Get subtask details for broadcast"""
        from apps.tasks.models import Subtask
        try:
            subtask = Subtask.objects.get(id=subtask_id)
            return {
                'id': subtask.id,
                'title': subtask.title,
                'order': subtask.order,
                'target_action': subtask.target_action,
                'guide_text': subtask.guide_text,
                'voice_guide_text': subtask.voice_guide_text
            }
        except Subtask.DoesNotExist:
            return None
