import os
from celery import Celery

# Django 설정 모듈 지정
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'config.settings')

# Celery 앱 생성
app = Celery('config')

# Django settings를 사용하여 Celery 설정
app.config_from_object('django.conf:settings', namespace='CELERY')

# Django 앱에서 tasks 자동 발견
app.autodiscover_tasks()


@app.task(bind=True, ignore_result=True)
def debug_task(self):
    print(f'Request: {self.request!r}')
