"""
Django management command to create test data for development
"""
from django.core.management.base import BaseCommand
from django.contrib.auth import get_user_model
from django.utils import timezone
from apps.lectures.models import Lecture
from apps.sessions.models import LectureSession
from apps.tasks.models import Task, Subtask

User = get_user_model()


class Command(BaseCommand):
    help = 'Create test users, lectures, and sessions for development'

    def handle(self, *args, **kwargs):
        self.stdout.write('Creating test data...\n')

        # Create superuser if not exists
        if not User.objects.filter(email='admin@example.com').exists():
            admin = User.objects.create_superuser(
                email='admin@example.com',
                password='admin123',
                name='Admin User',
                role='INSTRUCTOR'
            )
            self.stdout.write(self.style.SUCCESS(f'✓ Created superuser: admin@example.com / admin123'))
        else:
            admin = User.objects.get(email='admin@example.com')
            self.stdout.write(self.style.WARNING('⚠ Superuser already exists: admin@example.com'))

        # Create test student if not exists
        if not User.objects.filter(email='student1@example.com').exists():
            student = User.objects.create_user(
                email='student1@example.com',
                password='student123',
                name='Test Student',
                role='STUDENT'
            )
            self.stdout.write(self.style.SUCCESS(f'✓ Created student: student1@example.com / student123'))
        else:
            student = User.objects.get(email='student1@example.com')
            self.stdout.write(self.style.WARNING('⚠ Student already exists: student1@example.com'))

        # Create test lecture if not exists
        lecture, created = Lecture.objects.get_or_create(
            title='Test Lecture - Mobile App Basics',
            defaults={
                'instructor': admin,
                'description': 'Test lecture for mobile app development',
                'is_active': True
            }
        )
        if created:
            self.stdout.write(self.style.SUCCESS(f'✓ Created lecture: {lecture.title}'))
        else:
            self.stdout.write(self.style.WARNING(f'⚠ Lecture already exists: {lecture.title}'))

        # Create test task if not exists
        task, created = Task.objects.get_or_create(
            lecture=lecture,
            title='Task 1: Setup and Configuration',
            defaults={
                'description': 'Learn how to configure the mobile app',
                'order_index': 1
            }
        )
        if created:
            self.stdout.write(self.style.SUCCESS(f'✓ Created task: {task.title}'))
        else:
            self.stdout.write(self.style.WARNING(f'⚠ Task already exists: {task.title}'))

        # Create subtasks if not exist
        subtask1, created = Subtask.objects.get_or_create(
            task=task,
            title='Step 1: Open Settings',
            defaults={
                'description': 'Navigate to app settings',
                'order_index': 1,
                'target_action': 'CLICK',
                'target_element_hint': 'Settings button',
                'guide_text': 'Please tap on the Settings button'
            }
        )
        if created:
            self.stdout.write(self.style.SUCCESS(f'✓ Created subtask: {subtask1.title}'))

        subtask2, created = Subtask.objects.get_or_create(
            task=task,
            title='Step 2: Enable Notifications',
            defaults={
                'description': 'Turn on notification settings',
                'order_index': 2,
                'target_action': 'CLICK',
                'target_element_hint': 'Notification toggle',
                'guide_text': 'Please enable notifications'
            }
        )
        if created:
            self.stdout.write(self.style.SUCCESS(f'✓ Created subtask: {subtask2.title}'))

        # Create test session if not exists
        session, created = LectureSession.objects.get_or_create(
            session_code='TEST001',
            defaults={
                'lecture': lecture,
                'instructor': admin,
                'title': 'Test Session 1',
                'scheduled_at': timezone.now(),
                'status': 'WAITING'
            }
        )
        if created:
            self.stdout.write(self.style.SUCCESS(f'✓ Created session: {session.session_code}'))
        else:
            self.stdout.write(self.style.WARNING(f'⚠ Session already exists: {session.session_code}'))

        self.stdout.write('\n' + '=' * 60)
        self.stdout.write(self.style.SUCCESS('\n✓ Test data creation complete!\n'))
        self.stdout.write('=' * 60 + '\n')
        self.stdout.write('\nTest accounts:')
        self.stdout.write(f'  Instructor: admin@example.com / admin123')
        self.stdout.write(f'  Student: student1@example.com / student123')
        self.stdout.write(f'\nTest session code: TEST001')
        self.stdout.write(f'\nBackend API: http://localhost:8000/api/')
        self.stdout.write(f'Admin panel: http://localhost:8000/admin/')
