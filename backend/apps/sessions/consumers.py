"""
WebSocket Consumers for Real-time Session Communication
"""
import json
from channels.generic.websocket import AsyncWebsocketConsumer
from channels.db import database_sync_to_async
from django.contrib.auth import get_user_model
from django.utils import timezone
from .models import LectureSession, SessionParticipant, SessionStepControl

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
        import logging
        from django.conf import settings
        logger = logging.getLogger(__name__)

        try:
            self.session_code = self.scope['url_route']['kwargs']['session_code']
            self.session_group_name = f'session_{self.session_code}'
            self.user = self.scope.get('user')

            logger.info(f"WebSocket connection attempt - Session: {self.session_code}, User: {self.user}")

            # JWT 미들웨어가 이미 인증을 처리함
            # 인증된 사용자 또는 AnonymousUser가 scope['user']에 설정됨
            is_authenticated = (
                self.user and
                hasattr(self.user, 'is_authenticated') and
                self.user.is_authenticated
            )

            if is_authenticated:
                # 인증된 사용자: 세션 및 접근 권한 확인
                logger.info(f"Authenticated user: {self.user.id} ({getattr(self.user, 'name', 'N/A')})")

                # Verify session exists
                session = await self.get_session()
                if not session:
                    logger.warning(f"Session not found: {self.session_code}")
                    await self.close()
                    return

                # Check if user is instructor or enrolled participant
                is_valid = await self.check_user_access(session)
                if not is_valid:
                    logger.warning(f"User {self.user.id} does not have access to session {self.session_code}")
                    await self.close()
                    return
            else:
                # 익명 사용자: DEBUG 모드에서만 허용
                if getattr(settings, 'DEBUG', False):
                    logger.info("Anonymous user allowed in DEBUG mode")
                    self.user = await self.get_anonymous_user()
                    if not self.user:
                        logger.error("Failed to create anonymous user")
                        await self.close()
                        return
                else:
                    # 프로덕션 모드에서는 인증 필수
                    logger.warning("Anonymous connection rejected in production mode")
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
                    'user_id': getattr(self.user, 'id', 0),
                    'user_name': getattr(self.user, 'name', 'Anonymous'),
                    'role': getattr(self.user, 'role', 'student')
                }
            )

            logger.info(f"WebSocket connected successfully - Session: {self.session_code}, User: {getattr(self.user, 'id', 'anon')}")

        except Exception as e:
            logger.error(f"WebSocket connection failed - Error: {e}")
            await self.close()

    async def disconnect(self, close_code):
        """Handle WebSocket disconnection"""
        # Update participant status to DISCONNECTED
        await self.update_participant_on_disconnect()

        # Notify others that user left (if we have a valid user)
        if hasattr(self, 'user') and self.user and hasattr(self, 'session_group_name'):
            await self.channel_layer.group_send(
                self.session_group_name,
                {
                    'type': 'participant_left',
                    'user_id': getattr(self.user, 'id', 0),
                    'user_name': getattr(self.user, 'name', 'Anonymous')
                }
            )

        # Leave session group
        if hasattr(self, 'session_group_name'):
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

            # Student messages (Android app compatible)
            elif message_type == 'join':
                await self.handle_join(data)
            elif message_type == 'heartbeat':
                await self.handle_heartbeat(data)
            elif message_type == 'step_complete':
                await self.handle_step_complete(data)
            elif message_type == 'request_help':
                await self.handle_help_request(data)

            # Legacy student messages (backward compatibility)
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

        # Log step control action
        await self.log_step_control(
            action='START_STEP',
            subtask_id=subtask_id,
            message=data.get('message', '다음 단계로 이동')
        )

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

        # Log pause action
        await self.log_step_control(
            action='PAUSE',
            message='세션이 일시정지되었습니다'
        )

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

        # Log resume action
        await self.log_step_control(
            action='RESUME',
            message='세션이 재개되었습니다'
        )

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

        # Log end session action
        await self.log_step_control(
            action='END_STEP',
            message='세션이 종료되었습니다'
        )

        await self.channel_layer.group_send(
            self.session_group_name,
            {
                'type': 'session_status_changed',
                'status': 'ENDED',
                'message': '세션이 종료되었습니다'
            }
        )

    # Student message handlers
    async def handle_join(self, data):
        """Handle explicit join message from student"""
        # Update participant status to ACTIVE
        await self.update_participant_status('ACTIVE')

        # Send confirmation
        await self.send(text_data=json.dumps({
            'type': 'join_confirmed',
            'session_code': self.session_code,
            'user_id': self.user.id,
            'message': '세션에 참가했습니다'
        }))

    async def handle_heartbeat(self, data):
        """Handle heartbeat message to keep connection alive"""
        # Update last_active_at timestamp
        await self.update_participant_last_active()

        # Send heartbeat response (optional)
        await self.send(text_data=json.dumps({
            'type': 'heartbeat_ack',
            'timestamp': data.get('timestamp')
        }))

    async def handle_step_complete(self, data):
        """Handle student completing a step"""
        subtask_id = data.get('subtask_id')

        if not subtask_id:
            await self.send(text_data=json.dumps({
                'error': 'subtask_id is required'
            }))
            return

        # Update participant's current subtask
        await self.update_participant_subtask(subtask_id)

        # Send progress update to instructor(s) only
        await self.channel_layer.group_send(
            self.session_group_name,
            {
                'type': 'progress_updated',
                'user_id': self.user.id,
                'user_name': self.user.name,
                'subtask_id': subtask_id,
                'status': 'completed',
                'role_filter': 'INSTRUCTOR'  # Only send to instructors
            }
        )

        # Send confirmation to student
        await self.send(text_data=json.dumps({
            'type': 'step_complete_confirmed',
            'subtask_id': subtask_id,
            'message': '단계를 완료했습니다'
        }))

    async def handle_progress_update(self, data):
        """Handle student progress update (legacy)"""
        subtask_id = data.get('subtask_id')
        status = data.get('status', 'in_progress')

        # Update participant's current subtask if provided
        if subtask_id:
            await self.update_participant_subtask(subtask_id)

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
        subtask = event['subtask']
        # Android 앱 호환성: data 필드에 subtask 정보 포함
        await self.send(text_data=json.dumps({
            'type': 'step_changed',
            'subtask': subtask,
            'data': {
                'id': subtask.get('id'),
                'title': subtask.get('title'),
                'order': subtask.get('order_index'),
                'order_index': subtask.get('order_index'),
                'target_action': subtask.get('target_action'),
                'guide_text': subtask.get('guide_text'),
                'voice_guide_text': subtask.get('voice_guide_text')
            }
        }))

    async def session_status_changed(self, event):
        """Send session status changed notification to client"""
        import logging
        logger = logging.getLogger(__name__)
        logger.info(f"[Consumer] Sending session_status_changed to client: {event['status']}")

        # Android 앱 호환성: data 필드 안에 status와 message 포함
        await self.send(text_data=json.dumps({
            'type': 'session_status_changed',
            'status': event['status'],
            'message': event['message'],
            'data': {
                'status': event['status'],
                'message': event['message']
            }
        }))
        logger.info(f"[Consumer] Message sent successfully")

    async def participant_joined(self, event):
        """Send participant joined notification to client"""
        # Don't send to self
        if event['user_id'] == getattr(self.user, 'id', 0):
            return

        await self.send(text_data=json.dumps({
            'type': 'participant_joined',
            'user_id': event['user_id'],
            'user_name': event['user_name'],
            'role': event['role'],
            # Frontend compatibility: data wrapper
            'data': {
                'user_id': event['user_id'],
                'username': event['user_name'],
                'role': event['role']
            }
        }))

    async def participant_left(self, event):
        """Send participant left notification to client"""
        # Don't send to self
        if event['user_id'] == getattr(self.user, 'id', 0):
            return

        await self.send(text_data=json.dumps({
            'type': 'participant_left',
            'user_id': event['user_id'],
            'user_name': event['user_name'],
            # Frontend compatibility: data wrapper
            'data': {
                'user_id': event['user_id'],
                'username': event['user_name']
            }
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

    async def instructor_message(self, event):
        """Send instructor broadcast message to all participants"""
        await self.send(text_data=json.dumps({
            'type': 'instructor_message',
            'message': event['message'],
            'from': event['from'],
            'timestamp': event['timestamp'],
            'data': {
                'message': event['message'],
                'from': event['from'],
                'timestamp': event['timestamp']
            }
        }))

    async def screenshot_updated(self, event):
        """Send screenshot update notification to instructors only"""
        # Only send to instructors (강사 대시보드)
        if not hasattr(self.user, 'role') or self.user.role != 'INSTRUCTOR':
            return

        await self.send(text_data=json.dumps({
            'type': 'screenshot_updated',
            'data': {
                'participant_id': event.get('participant_id'),
                'device_id': event.get('device_id'),
                'participant_name': event.get('participant_name'),
                'image_url': event.get('image_url'),
                'captured_at': event.get('captured_at'),
            }
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
    def get_anonymous_user(self):
        """Get or create anonymous test user for WebSocket testing"""
        try:
            # For testing, return a mock user object
            from types import SimpleNamespace
            user = SimpleNamespace()
            user.id = 0
            user.is_authenticated = True
            user.role = 'student'
            user.name = f'Anonymous_{self.session_code}'
            return user
        except Exception:
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
                'order_index': subtask.order_index,
                'target_action': subtask.target_action,
                'guide_text': subtask.guide_text,
                'voice_guide_text': subtask.voice_guide_text
            }
        except Subtask.DoesNotExist:
            return None

    # SessionParticipant update methods
    @database_sync_to_async
    def update_participant_status(self, status):
        """Update participant status"""
        try:
            # Skip for anonymous users during testing
            if hasattr(self.user, 'id') and self.user.id == 0:
                return True

            session = LectureSession.objects.get(session_code=self.session_code)
            participant, created = SessionParticipant.objects.get_or_create(
                session=session,
                user=self.user,
                defaults={'status': status}
            )
            if not created:
                participant.status = status
                participant.last_active_at = timezone.now()
                participant.save()
            return True
        except Exception:
            return False

    @database_sync_to_async
    def update_participant_last_active(self):
        """Update participant last_active_at timestamp"""
        try:
            # Skip for anonymous users during testing
            if hasattr(self.user, 'id') and self.user.id == 0:
                return True

            session = LectureSession.objects.get(session_code=self.session_code)
            SessionParticipant.objects.filter(
                session=session,
                user=self.user
            ).update(last_active_at=timezone.now())
            return True
        except Exception:
            return False

    @database_sync_to_async
    def update_participant_subtask(self, subtask_id):
        """Update participant current subtask"""
        try:
            # Skip for anonymous users during testing
            if hasattr(self.user, 'id') and self.user.id == 0:
                return True

            from apps.tasks.models import Subtask
            session = LectureSession.objects.get(session_code=self.session_code)
            subtask = Subtask.objects.get(id=subtask_id)

            participant = SessionParticipant.objects.get(
                session=session,
                user=self.user
            )
            participant.current_subtask = subtask
            participant.last_active_at = timezone.now()
            participant.save()
            return True
        except Exception:
            return False

    @database_sync_to_async
    def update_participant_on_disconnect(self):
        """Update participant status on disconnect"""
        try:
            # Skip for anonymous users during testing
            if hasattr(self.user, 'id') and self.user.id == 0:
                return True

            session = LectureSession.objects.get(session_code=self.session_code)
            SessionParticipant.objects.filter(
                session=session,
                user=self.user
            ).update(
                status='DISCONNECTED',
                last_active_at=timezone.now()
            )
            return True
        except Exception:
            return False

    # SessionStepControl logging methods
    @database_sync_to_async
    def log_step_control(self, action, subtask_id=None, message=''):
        """Log instructor step control action to SessionStepControl"""
        try:
            from apps.tasks.models import Subtask
            session = LectureSession.objects.get(session_code=self.session_code)

            # Get subtask if provided
            subtask = None
            if subtask_id:
                try:
                    subtask = Subtask.objects.get(id=subtask_id)
                except Subtask.DoesNotExist:
                    pass

            # Create step control record
            SessionStepControl.objects.create(
                session=session,
                subtask=subtask,
                instructor=self.user,
                action=action,
                message=message
            )
            return True
        except Exception as e:
            print(f"Error logging step control: {e}")
            return False
