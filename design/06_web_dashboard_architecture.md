# 강사용 웹 Dashboard 아키텍처 설계

## 개요

강사용 Dashboard는 PC 웹 브라우저에서 사용하는 Django Template 기반 웹 서비스입니다.
실시간으로 수강생의 학습 진행 상황을 모니터링하고, 강의를 관리하며, 도움 요청에 대응할 수 있습니다.

**대상 사용자**: 강사 (PC 데스크톱 환경)
**접근 방식**: 웹 브라우저 (Chrome, Edge, Firefox 등)

---

## 기술 스택

### Backend
- **Web Framework**: Django 4.x
- **WebSocket**: Django Channels 4.x + Redis
- **Database**: PostgreSQL 14+
- **Cache**: Redis 7.x
- **Authentication**: Django Session + JWT (API용)

### Frontend
- **Template Engine**: Django Template Language
- **Partial Updates**: HTMX 1.9+ (페이지 새로고침 없이 부분 업데이트)
- **JS Framework**: Alpine.js 3.x (가벼운 인터랙션)
- **CSS Framework**: Bootstrap 5.3
- **Charts**: Chart.js 4.x / ApexCharts
- **Icons**: Bootstrap Icons

---

## 프로젝트 구조

```
dashboard/                           # Django App
├── __init__.py
├── apps.py
├── urls.py                         # URL 라우팅
├── views.py                        # Class-Based Views
├── forms.py                        # Django Forms
├── consumers.py                    # WebSocket Consumers
├── routing.py                      # WebSocket 라우팅
├── templatetags/                   # 커스텀 템플릿 태그
│   └── dashboard_tags.py
├── templates/
│   └── dashboard/
│       ├── base.html               # 기본 레이아웃
│       ├── home.html               # 홈 (강의 목록)
│       ├── lecture/
│       │   ├── list.html           # 강의 목록
│       │   ├── detail.html         # 강의 상세
│       │   ├── create.html         # 강의 생성
│       │   └── edit.html           # 강의 수정
│       ├── student/
│       │   ├── list.html           # 수강생 목록
│       │   ├── detail.html         # 수강생 상세
│       │   └── progress.html       # 진행 상황 상세
│       ├── help/
│       │   ├── list.html           # 도움 요청 목록
│       │   ├── detail.html         # 도움 요청 상세
│       │   └── notification.html   # 실시간 알림
│       ├── statistics/
│       │   ├── overview.html       # 통계 개요
│       │   └── report.html         # 상세 리포트
│       └── partials/               # HTMX 부분 템플릿
│           ├── student_card.html
│           ├── progress_bar.html
│           └── help_notification.html
├── static/
│   └── dashboard/
│       ├── css/
│       │   └── custom.css
│       ├── js/
│       │   ├── dashboard.js
│       │   ├── charts.js
│       │   └── websocket.js
│       └── img/
└── migrations/
```

---

## URL 구조

```python
# dashboard/urls.py

from django.urls import path
from . import views

app_name = 'dashboard'

urlpatterns = [
    # 홈
    path('', views.DashboardHomeView.as_view(), name='home'),

    # 강의 관리
    path('lectures/', views.LectureListView.as_view(), name='lecture_list'),
    path('lectures/create/', views.LectureCreateView.as_view(), name='lecture_create'),
    path('lectures/<int:pk>/', views.LectureDetailView.as_view(), name='lecture_detail'),
    path('lectures/<int:pk>/edit/', views.LectureUpdateView.as_view(), name='lecture_edit'),
    path('lectures/<int:pk>/delete/', views.LectureDeleteView.as_view(), name='lecture_delete'),

    # Task/Subtask 관리
    path('lectures/<int:lecture_id>/tasks/create/', views.TaskCreateView.as_view(), name='task_create'),
    path('tasks/<int:pk>/edit/', views.TaskUpdateView.as_view(), name='task_edit'),
    path('tasks/<int:task_id>/subtasks/create/', views.SubtaskCreateView.as_view(), name='subtask_create'),
    path('subtasks/<int:pk>/edit/', views.SubtaskUpdateView.as_view(), name='subtask_edit'),

    # 수강생 모니터링
    path('lectures/<int:lecture_id>/students/', views.StudentListView.as_view(), name='student_list'),
    path('students/<int:pk>/', views.StudentDetailView.as_view(), name='student_detail'),
    path('students/<int:pk>/progress/', views.StudentProgressView.as_view(), name='student_progress'),

    # 도움 요청
    path('help-requests/', views.HelpRequestListView.as_view(), name='help_request_list'),
    path('help-requests/<int:pk>/', views.HelpRequestDetailView.as_view(), name='help_request_detail'),

    # 통계
    path('statistics/', views.StatisticsOverviewView.as_view(), name='statistics_overview'),
    path('statistics/lecture/<int:lecture_id>/', views.LectureStatisticsView.as_view(), name='lecture_statistics'),

    # HTMX 부분 업데이트 엔드포인트
    path('htmx/students/<int:lecture_id>/', views.htmx_student_list, name='htmx_student_list'),
    path('htmx/help-notifications/', views.htmx_help_notifications, name='htmx_help_notifications'),
    path('htmx/progress-chart/<int:student_id>/', views.htmx_progress_chart, name='htmx_progress_chart'),
]
```

**WebSocket URL**:
```python
# dashboard/routing.py

from django.urls import path
from . import consumers

websocket_urlpatterns = [
    path('ws/dashboard/lecture/<int:lecture_id>/', consumers.LectureDashboardConsumer.as_asgi()),
    path('ws/dashboard/help-requests/', consumers.HelpRequestConsumer.as_asgi()),
]
```

---

## View 설계

### Class-Based Views 예시

```python
# dashboard/views.py

from django.views.generic import ListView, DetailView, CreateView, UpdateView, DeleteView
from django.contrib.auth.mixins import LoginRequiredMixin, UserPassesTestMixin
from django.shortcuts import render
from django.http import JsonResponse
from .models import Lecture, Task, Subtask, User, UserProgress, HelpRequest


class InstructorRequiredMixin(UserPassesTestMixin):
    """강사 권한 체크"""
    def test_func(self):
        return self.request.user.is_authenticated and self.request.user.role == 'INSTRUCTOR'


class DashboardHomeView(LoginRequiredMixin, InstructorRequiredMixin, ListView):
    """Dashboard 홈 - 강의 목록 및 요약"""
    model = Lecture
    template_name = 'dashboard/home.html'
    context_object_name = 'lectures'

    def get_queryset(self):
        return Lecture.objects.filter(
            instructor=self.request.user,
            is_active=True
        ).prefetch_related('tasks')

    def get_context_data(self, **kwargs):
        context = super().get_context_data(**kwargs)

        # 요약 통계
        context['total_students'] = User.objects.filter(
            role='STUDENT',
            user_lecture_enrollments__lecture__instructor=self.request.user
        ).distinct().count()

        context['pending_help_requests'] = HelpRequest.objects.filter(
            subtask__task__lecture__instructor=self.request.user,
            status='PENDING'
        ).count()

        return context


class LectureDetailView(LoginRequiredMixin, InstructorRequiredMixin, DetailView):
    """강의 상세 - 수강생 목록 및 진행률"""
    model = Lecture
    template_name = 'dashboard/lecture/detail.html'
    context_object_name = 'lecture'

    def get_context_data(self, **kwargs):
        context = super().get_context_data(**kwargs)
        lecture = self.object

        # 수강생 목록 및 진행률
        students = User.objects.filter(
            role='STUDENT',
            user_lecture_enrollments__lecture=lecture
        ).prefetch_related('user_progress')

        student_data = []
        total_subtasks = Subtask.objects.filter(task__lecture=lecture).count()

        for student in students:
            completed = UserProgress.objects.filter(
                user=student,
                subtask__task__lecture=lecture,
                status='COMPLETED'
            ).count()

            student_data.append({
                'student': student,
                'completed': completed,
                'total': total_subtasks,
                'progress_rate': (completed / total_subtasks * 100) if total_subtasks > 0 else 0,
                'current_subtask': UserProgress.objects.filter(
                    user=student,
                    status='IN_PROGRESS'
                ).select_related('subtask').first()
            })

        context['student_data'] = student_data
        return context


class StudentDetailView(LoginRequiredMixin, InstructorRequiredMixin, DetailView):
    """수강생 상세 - 학습 히스토리 및 로그"""
    model = User
    template_name = 'dashboard/student/detail.html'
    context_object_name = 'student'

    def get_context_data(self, **kwargs):
        context = super().get_context_data(**kwargs)
        student = self.object

        # 진행 상황
        context['progress_list'] = UserProgress.objects.filter(
            user=student
        ).select_related('subtask__task__lecture').order_by('-updated_at')

        # 도움 요청 히스토리
        context['help_requests'] = HelpRequest.objects.filter(
            user=student
        ).select_related('subtask').order_by('-created_at')[:20]

        # 학습 시간 통계
        context['total_learning_time'] = self._calculate_learning_time(student)

        return context

    def _calculate_learning_time(self, student):
        # 총 학습 시간 계산 로직
        pass


class HelpRequestListView(LoginRequiredMixin, InstructorRequiredMixin, ListView):
    """도움 요청 목록"""
    model = HelpRequest
    template_name = 'dashboard/help/list.html'
    context_object_name = 'help_requests'
    paginate_by = 20

    def get_queryset(self):
        return HelpRequest.objects.filter(
            subtask__task__lecture__instructor=self.request.user
        ).select_related(
            'user',
            'subtask__task__lecture'
        ).order_by('-created_at')
```

---

## HTMX 부분 업데이트

### 수강생 목록 자동 갱신

**Template (lecture/detail.html)**:
```html
<div id="student-list"
     hx-get="{% url 'dashboard:htmx_student_list' lecture.id %}"
     hx-trigger="every 5s"
     hx-swap="outerHTML">
    {% include 'dashboard/partials/student_list.html' %}
</div>
```

**View**:
```python
def htmx_student_list(request, lecture_id):
    """HTMX: 수강생 목록 부분 업데이트"""
    lecture = get_object_or_404(Lecture, id=lecture_id, instructor=request.user)

    students = User.objects.filter(
        role='STUDENT',
        user_lecture_enrollments__lecture=lecture
    )

    # ... (student_data 계산)

    return render(request, 'dashboard/partials/student_list.html', {
        'student_data': student_data
    })
```

---

## WebSocket 실시간 통신

### Consumer 구현

```python
# dashboard/consumers.py

import json
from channels.generic.websocket import AsyncWebsocketConsumer
from channels.db import database_sync_to_async


class LectureDashboardConsumer(AsyncWebsocketConsumer):
    """강의 Dashboard WebSocket Consumer"""

    async def connect(self):
        self.lecture_id = self.scope['url_route']['kwargs']['lecture_id']
        self.room_group_name = f'lecture_{self.lecture_id}'

        # 그룹에 추가
        await self.channel_layer.group_add(
            self.room_group_name,
            self.channel_name
        )

        await self.accept()

    async def disconnect(self, close_code):
        # 그룹에서 제거
        await self.channel_layer.group_discard(
            self.room_group_name,
            self.channel_name
        )

    async def receive(self, text_data):
        # 클라이언트로부터 메시지 수신 (필요시)
        pass

    async def progress_update(self, event):
        """진행 상태 업데이트 알림"""
        await self.send(text_data=json.dumps({
            'type': 'progress_update',
            'data': event['data']
        }))

    async def help_request(self, event):
        """도움 요청 알림"""
        await self.send(text_data=json.dumps({
            'type': 'help_request',
            'data': event['data']
        }))


class HelpRequestConsumer(AsyncWebsocketConsumer):
    """도움 요청 전용 WebSocket Consumer"""

    async def connect(self):
        self.instructor_id = self.scope['user'].id
        self.room_group_name = f'instructor_{self.instructor_id}_help'

        await self.channel_layer.group_add(
            self.room_group_name,
            self.channel_name
        )

        await self.accept()

    async def disconnect(self, close_code):
        await self.channel_layer.group_discard(
            self.room_group_name,
            self.channel_name
        )

    async def new_help_request(self, event):
        """새 도움 요청 알림"""
        await self.send(text_data=json.dumps({
            'type': 'new_help_request',
            'data': event['data']
        }))
```

### Kafka Consumer에서 WebSocket 전송

```python
# backend/kafka_consumers.py

from channels.layers import get_channel_layer
from asgiref.sync import async_to_sync


class ProgressUpdateConsumer:
    """Kafka → WebSocket 전달"""

    def process_progress_update(self, message):
        data = json.loads(message.value)

        # WebSocket 그룹에 메시지 전송
        channel_layer = get_channel_layer()
        async_to_sync(channel_layer.group_send)(
            f"lecture_{data['lecture_id']}",
            {
                'type': 'progress_update',
                'data': {
                    'user_id': data['user_id'],
                    'subtask_id': data['subtask_id'],
                    'status': data['new_status']
                }
            }
        )
```

---

## Frontend JavaScript

### WebSocket 연결

```javascript
// static/dashboard/js/websocket.js

class DashboardWebSocket {
    constructor(lectureId) {
        this.lectureId = lectureId;
        this.socket = null;
        this.connect();
    }

    connect() {
        const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        const wsUrl = `${wsProtocol}//${window.location.host}/ws/dashboard/lecture/${this.lectureId}/`;

        this.socket = new WebSocket(wsUrl);

        this.socket.onopen = () => {
            console.log('WebSocket connected');
        };

        this.socket.onmessage = (event) => {
            const data = JSON.parse(event.data);
            this.handleMessage(data);
        };

        this.socket.onclose = () => {
            console.log('WebSocket disconnected');
            // 재연결 시도
            setTimeout(() => this.connect(), 3000);
        };

        this.socket.onerror = (error) => {
            console.error('WebSocket error:', error);
        };
    }

    handleMessage(data) {
        switch (data.type) {
            case 'progress_update':
                this.handleProgressUpdate(data.data);
                break;
            case 'help_request':
                this.handleHelpRequest(data.data);
                break;
        }
    }

    handleProgressUpdate(data) {
        // 진행률 UI 업데이트
        const progressBar = document.querySelector(`#progress-${data.user_id}`);
        if (progressBar) {
            // HTMX로 해당 부분만 다시 로드
            htmx.trigger(`#student-${data.user_id}`, 'refresh');
        }
    }

    handleHelpRequest(data) {
        // 도움 요청 알림 표시
        this.showNotification('새로운 도움 요청', data);

        // 알림 카운트 업데이트
        const badge = document.querySelector('#help-request-badge');
        if (badge) {
            badge.textContent = parseInt(badge.textContent) + 1;
        }
    }

    showNotification(title, data) {
        // Bootstrap Toast 알림
        const toast = `
            <div class="toast" role="alert">
                <div class="toast-header">
                    <strong class="me-auto">${title}</strong>
                    <button type="button" class="btn-close" data-bs-dismiss="toast"></button>
                </div>
                <div class="toast-body">
                    ${data.user_name}님이 "${data.subtask_title}" 단계에서 도움을 요청했습니다.
                </div>
            </div>
        `;

        document.querySelector('#toast-container').insertAdjacentHTML('beforeend', toast);

        // 오디오 알림
        const audio = new Audio('/static/dashboard/sounds/notification.mp3');
        audio.play();
    }
}

// 페이지 로드 시 WebSocket 연결
document.addEventListener('DOMContentLoaded', () => {
    const lectureId = document.querySelector('[data-lecture-id]')?.dataset.lectureId;
    if (lectureId) {
        new DashboardWebSocket(lectureId);
    }
});
```

---

## Chart.js 통계 시각화

```javascript
// static/dashboard/js/charts.js

function renderProgressChart(canvasId, data) {
    const ctx = document.getElementById(canvasId).getContext('2d');

    new Chart(ctx, {
        type: 'bar',
        data: {
            labels: data.labels,  // 수강생 이름
            datasets: [{
                label: '진행률 (%)',
                data: data.progress_rates,
                backgroundColor: 'rgba(54, 162, 235, 0.5)',
                borderColor: 'rgba(54, 162, 235, 1)',
                borderWidth: 1
            }]
        },
        options: {
            scales: {
                y: {
                    beginAtZero: true,
                    max: 100
                }
            }
        }
    });
}

function renderHelpRequestTrendChart(canvasId, data) {
    const ctx = document.getElementById(canvasId).getContext('2d');

    new Chart(ctx, {
        type: 'line',
        data: {
            labels: data.dates,  // 날짜
            datasets: [{
                label: '도움 요청 수',
                data: data.help_counts,
                fill: false,
                borderColor: 'rgb(255, 99, 132)',
                tension: 0.1
            }]
        }
    });
}
```

---

## Base Template

```html
<!-- templates/dashboard/base.html -->

<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>{% block title %}강사 Dashboard{% endblock %} - 시니어 교육</title>

    <!-- Bootstrap CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <!-- Bootstrap Icons -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css" rel="stylesheet">
    <!-- Custom CSS -->
    <link href="{% static 'dashboard/css/custom.css' %}" rel="stylesheet">

    {% block extra_css %}{% endblock %}
</head>
<body>
    <!-- Navigation -->
    <nav class="navbar navbar-expand-lg navbar-dark bg-dark">
        <div class="container-fluid">
            <a class="navbar-brand" href="{% url 'dashboard:home' %}">
                <i class="bi bi-mortarboard-fill"></i> 시니어 교육 Dashboard
            </a>

            <div class="collapse navbar-collapse">
                <ul class="navbar-nav me-auto">
                    <li class="nav-item">
                        <a class="nav-link" href="{% url 'dashboard:lecture_list' %}">강의 관리</a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" href="{% url 'dashboard:help_request_list' %}">
                            도움 요청
                            <span id="help-request-badge" class="badge bg-danger">0</span>
                        </a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" href="{% url 'dashboard:statistics_overview' %}">통계</a>
                    </li>
                </ul>

                <div class="d-flex">
                    <span class="navbar-text me-3">
                        {{ request.user.name }} 강사님
                    </span>
                    <a href="{% url 'logout' %}" class="btn btn-outline-light btn-sm">로그아웃</a>
                </div>
            </div>
        </div>
    </nav>

    <!-- Main Content -->
    <div class="container-fluid mt-4">
        {% block content %}{% endblock %}
    </div>

    <!-- Toast Container -->
    <div id="toast-container" class="position-fixed bottom-0 end-0 p-3"></div>

    <!-- Bootstrap JS -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <!-- HTMX -->
    <script src="https://unpkg.com/htmx.org@1.9.10"></script>
    <!-- Alpine.js -->
    <script defer src="https://cdn.jsdelivr.net/npm/alpinejs@3.x.x/dist/cdn.min.js"></script>
    <!-- Chart.js -->
    <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.js"></script>
    <!-- Custom JS -->
    <script src="{% static 'dashboard/js/dashboard.js' %}"></script>
    <script src="{% static 'dashboard/js/websocket.js' %}"></script>
    <script src="{% static 'dashboard/js/charts.js' %}"></script>

    {% block extra_js %}{% endblock %}
</body>
</html>
```

---

## 주요 페이지 예시

### 강의 상세 페이지

```html
<!-- templates/dashboard/lecture/detail.html -->

{% extends 'dashboard/base.html' %}

{% block title %}{{ lecture.title }} - 강의 상세{% endblock %}

{% block content %}
<div class="row" data-lecture-id="{{ lecture.id }}">
    <div class="col-md-12">
        <div class="d-flex justify-content-between align-items-center mb-4">
            <h2>{{ lecture.title }}</h2>
            <div>
                <a href="{% url 'dashboard:lecture_edit' lecture.id %}" class="btn btn-primary">
                    <i class="bi bi-pencil"></i> 수정
                </a>
                <a href="{% url 'dashboard:task_create' lecture.id %}" class="btn btn-success">
                    <i class="bi bi-plus"></i> Task 추가
                </a>
            </div>
        </div>

        <p class="lead">{{ lecture.description }}</p>

        <!-- 통계 요약 -->
        <div class="row mb-4">
            <div class="col-md-3">
                <div class="card">
                    <div class="card-body">
                        <h5 class="card-title">수강생 수</h5>
                        <p class="display-6">{{ student_data|length }}</p>
                    </div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="card">
                    <div class="card-body">
                        <h5 class="card-title">평균 진행률</h5>
                        <p class="display-6">{{ average_progress }}%</p>
                    </div>
                </div>
            </div>
            <!-- 추가 통계... -->
        </div>

        <!-- 수강생 목록 (HTMX 자동 갱신) -->
        <h3>수강생 목록</h3>
        <div id="student-list"
             hx-get="{% url 'dashboard:htmx_student_list' lecture.id %}"
             hx-trigger="every 5s"
             hx-swap="outerHTML">
            {% include 'dashboard/partials/student_list.html' %}
        </div>
    </div>
</div>
{% endblock %}
```

---

## 배포 고려사항

### 1. Django Channels 설정

**settings.py**:
```python
INSTALLED_APPS = [
    'daphne',  # 맨 위에
    'channels',
    # ...
]

ASGI_APPLICATION = 'myproject.asgi.application'

CHANNEL_LAYERS = {
    'default': {
        'BACKEND': 'channels_redis.core.RedisChannelLayer',
        'CONFIG': {
            'hosts': [('127.0.0.1', 6379)],
        },
    },
}
```

### 2. ASGI 설정

**asgi.py**:
```python
import os
from django.core.asgi import get_asgi_application
from channels.routing import ProtocolTypeRouter, URLRouter
from channels.auth import AuthMiddlewareStack
from dashboard.routing import websocket_urlpatterns

os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'myproject.settings')

application = ProtocolTypeRouter({
    'http': get_asgi_application(),
    'websocket': AuthMiddlewareStack(
        URLRouter(websocket_urlpatterns)
    ),
})
```

### 3. 실행 명령

```bash
# 개발 서버
daphne -b 0.0.0.0 -p 8000 myproject.asgi:application

# 프로덕션 (Supervisor + Nginx)
daphne -u /tmp/daphne.sock myproject.asgi:application
```

---

## 성능 최적화

1. **Lazy Loading**: 페이지네이션 및 무한 스크롤
2. **Caching**: Redis로 자주 조회하는 데이터 캐싱
3. **Database Indexing**: 주요 쿼리에 인덱스 추가
4. **WebSocket Grouping**: 불필요한 메시지 전송 최소화

---

이 웹 Dashboard는 Django Template의 빠른 개발 속도와 HTMX/WebSocket의 실시간 기능을 결합하여 강사가 효율적으로 수강생을 관리할 수 있도록 설계되었습니다.
