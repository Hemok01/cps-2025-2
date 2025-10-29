from django.apps import AppConfig


class SessionsConfig(AppConfig):
    default_auto_field = 'django.db.models.BigAutoField'
    name = 'apps.sessions'
    label = 'lecture_sessions'  # Avoid conflict with django.contrib.sessions
    verbose_name = 'Lecture Sessions'
