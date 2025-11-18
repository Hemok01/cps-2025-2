# CLAUDE.md - AI Assistant Guide for MobileGPT

**Last Updated**: 2025-11-18
**Project**: MobileGPT - Senior Digital Education Service
**Purpose**: Educational platform for teaching seniors how to use mobile apps

---

## 📋 Table of Contents

1. [Project Overview](#project-overview)
2. [Architecture & Tech Stack](#architecture--tech-stack)
3. [Codebase Structure](#codebase-structure)
4. [Development Workflows](#development-workflows)
5. [Key Conventions](#key-conventions)
6. [Common Tasks](#common-tasks)
7. [Testing & Quality](#testing--quality)
8. [Troubleshooting](#troubleshooting)
9. [Important Context](#important-context)

---

## 🎯 Project Overview

### What is MobileGPT?

MobileGPT is an AI-based real-time digital education service designed for senior users. It combines:

- **Real-time Teaching Sessions**: Instructors and students connect in live sessions with step-by-step guidance
- **Activity Monitoring**: AccessibilityService captures user interactions (clicks, scrolls, screen changes)
- **AI Assistance**: Analyzes user behavior to provide contextual help (currently basic infrastructure, AI integration simplified)
- **WebSocket Communication**: Real-time bidirectional communication between instructors and students
- **Recording Feature**: Instructors can record demonstrations for later use

### Core User Flows

1. **Instructor Creates Session** → Generates 6-char code → Students join → Instructor guides through steps
2. **Student Uses App** → AccessibilityService logs actions → Sent via Kafka → Stored in DB → Available for analysis
3. **Recording Mode** → Instructor demonstrates task → Events captured → Can be converted to lecture

---

## 🏗️ Architecture & Tech Stack

### System Architecture

```
┌──────────────────────┐
│  Android App (학생)   │  ← Kotlin, Jetpack Compose, AccessibilityService
│  - UI event capture  │
│  - Real-time session │
│  - WebSocket client  │
└──────────┬───────────┘
           │
           │ REST API / WebSocket
           ▼
┌──────────────────────┐
│  Backend (Django)    │  ← Django 5.0, DRF, Channels
│  - Session mgmt      │
│  - Log collection    │
│  - WebSocket server  │
└──────────┬───────────┘
           │
           │ Kafka / WebSocket
           ▼
┌──────────────────────┐
│  Infrastructure      │
│  - PostgreSQL (DB)   │
│  - Redis (Cache/WS)  │
│  - Kafka (Logs)      │
│  - Celery (Tasks)    │
└──────────────────────┘
           ▲
           │
┌──────────┴───────────┐
│  Frontend (React)    │  ← Vite, TypeScript, Radix UI
│  - Instructor dash   │
│  - Live monitoring   │
└──────────────────────┘
```

### Technology Stack

#### Backend
- **Framework**: Django 5.0.1, Django REST Framework 3.14
- **WebSocket**: Django Channels 4.0, channels-redis 4.1
- **Database**: PostgreSQL 15 (psycopg2-binary)
- **Caching**: Redis 7 (redis-py 5.0.1)
- **Message Queue**: Apache Kafka (confluent-kafka 2.3.0, kafka-python 2.0.2)
- **Task Queue**: Celery 5.3.4
- **Auth**: JWT (djangorestframework-simplejwt 5.3.1)
- **ASGI Server**: Daphne 4.0.0
- **WSGI Server**: Gunicorn 21.2.0

#### Frontend
- **Build Tool**: Vite 6.3.5
- **Framework**: React 18.3.1
- **Language**: TypeScript
- **UI Library**: Radix UI (comprehensive component library)
- **Styling**: Tailwind CSS (via tailwind-merge)
- **Routing**: React Router
- **Charts**: Recharts 2.15.2
- **State Management**: React Context API

#### Android
- **Language**: Kotlin
- **Min SDK**: 30 (Android 11.0)
- **Target SDK**: 34
- **Architecture**: MVVM + Clean Architecture
- **UI**: Jetpack Compose (Material3)
- **DI**: Hilt 2.52 (with KSP)
- **Networking**: Retrofit 2.9.0 + OkHttp 4.12.0
- **WebSocket**: Scarlet 0.1.12
- **Async**: Kotlin Coroutines 1.7.3
- **Image Loading**: Coil 2.5.0
- **Data Storage**: DataStore Preferences

---

## 📁 Codebase Structure

```
cps-2025-2/
├── backend/                          # Django backend
│   ├── apps/                         # Django apps (modular design)
│   │   ├── accounts/                 # User management & authentication
│   │   ├── lectures/                 # Lecture CRUD
│   │   ├── sessions/                 # Real-time session management
│   │   │   ├── models.py             # LectureSession, SessionParticipant, RecordingSession
│   │   │   ├── consumers.py          # WebSocket consumer for live sessions
│   │   │   └── routing.py            # WebSocket URL patterns
│   │   ├── tasks/                    # Task & Subtask models (lecture steps)
│   │   ├── progress/                 # Student progress tracking
│   │   │   └── consumers.py          # WebSocket for progress updates
│   │   ├── logs/                     # Activity log collection
│   │   │   ├── models.py             # ActivityLog model
│   │   │   ├── kafka_producer.py     # Kafka producer for async log processing
│   │   │   ├── views.py              # REST API for log submission
│   │   │   └── management/commands/
│   │   │       └── consume_activity_logs.py  # Kafka consumer
│   │   ├── help/                     # Help request management
│   │   ├── dashboard/                # Instructor dashboard API
│   │   │   └── consumers.py          # WebSocket for dashboard updates
│   │   ├── students/                 # Student-specific API endpoints
│   │   └── health/                   # Health check endpoints
│   ├── config/                       # Django configuration
│   │   ├── settings.py               # Main settings (DB, Kafka, Redis, etc.)
│   │   ├── urls.py                   # REST API routing
│   │   ├── routing.py                # WebSocket routing
│   │   ├── asgi.py                   # ASGI application
│   │   ├── wsgi.py                   # WSGI application
│   │   └── celery.py                 # Celery configuration
│   ├── core/                         # Shared utilities
│   │   ├── utils/                    # Common utilities
│   │   └── management/commands/      # Management commands
│   ├── requirements.txt              # Python dependencies
│   ├── Dockerfile                    # Backend container
│   ├── .env                          # Environment variables (not in git)
│   └── manage.py                     # Django management script
│
├── frontend/                         # React frontend (instructor dashboard)
│   ├── src/
│   │   ├── components/               # UI components (Radix UI based)
│   │   │   └── ui/                   # Reusable UI primitives
│   │   ├── pages/                    # Page components
│   │   │   ├── login-page.tsx        # Login page
│   │   │   ├── dashboard-page.tsx    # Main dashboard
│   │   │   ├── lectures-page.tsx     # Lecture management
│   │   │   ├── live-session-page.tsx # Live session monitoring
│   │   │   └── statistics-page.tsx   # Analytics
│   │   ├── lib/                      # Core logic
│   │   │   ├── api-service.ts        # REST API client
│   │   │   ├── live-session-service.ts # WebSocket client
│   │   │   ├── auth-context.tsx      # Auth state management
│   │   │   └── types.ts              # TypeScript types
│   │   ├── styles/                   # Global styles
│   │   └── main.tsx                  # Entry point
│   ├── package.json                  # Node dependencies
│   ├── vite.config.ts                # Vite configuration
│   ├── Dockerfile                    # Frontend container
│   └── index.html                    # HTML entry point
│
├── android-student/                  # Android student app
│   └── app/
│       ├── build.gradle.kts          # Gradle build configuration
│       └── src/main/java/com/mobilegpt/student/
│           ├── data/                 # Data layer
│           │   ├── api/              # Retrofit API interfaces
│           │   ├── repository/       # Repository implementations
│           │   └── local/            # DataStore (token, session prefs)
│           ├── domain/               # Domain layer
│           │   └── model/            # Domain models
│           ├── presentation/         # Presentation layer (UI)
│           │   ├── screen/           # Compose screens
│           │   ├── viewmodel/        # ViewModels
│           │   └── navigation/       # Navigation graph
│           ├── service/              # Android services
│           │   └── MobileGPTAccessibilityService.kt
│           ├── di/                   # Hilt dependency injection
│           └── MobileGPTApplication.kt
│
├── design/                           # Design documents
├── docker-compose.yml                # Multi-container setup
├── .gitignore                        # Git ignore rules
├── .claudeignore                     # Claude ignore rules
├── README.md                         # User-facing documentation
├── PROGRESS.md                       # Development progress tracker
├── plan.md                           # Original project plan
└── CLAUDE.md                         # This file
```

---

## 🔧 Development Workflows

### Local Development Setup

#### Option 1: Docker Compose (Recommended)

```bash
# 1. Clone repository
git clone <repo-url>
cd cps-2025-2

# 2. Set up environment variables
cp backend/.env.example backend/.env
# Edit backend/.env as needed

# 3. Start all services
docker-compose up -d

# 4. Check service health
docker-compose ps

# 5. Create superuser
docker-compose exec backend python manage.py createsuperuser

# 6. View logs
docker-compose logs -f backend
docker-compose logs -f consumer
```

**Services Started:**
- `db`: PostgreSQL on port 5432
- `redis`: Redis on port 6379
- `zookeeper`: Zookeeper on port 2181
- `kafka`: Kafka on ports 9092 (external), 29092 (internal)
- `backend`: Django/Daphne on port 8000
- `consumer`: Kafka consumer for activity logs
- `celery_worker`: Celery worker for background tasks
- `celery_beat`: Celery beat for scheduled tasks
- `frontend`: Vite dev server on port 5173

**Access Points:**
- Backend API: http://localhost:8000/api
- Django Admin: http://localhost:8000/admin
- Frontend Dashboard: http://localhost:5173
- WebSocket: ws://localhost:8000/ws

#### Option 2: Local Development (Without Docker)

**Backend:**
```bash
cd backend

# Create virtual environment
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Setup database (PostgreSQL must be running)
python manage.py migrate

# Create superuser
python manage.py createsuperuser

# Run development server (with WebSocket support)
daphne -b 0.0.0.0 -p 8000 config.asgi:application

# In separate terminals:
# - Kafka consumer
python manage.py consume_activity_logs

# - Celery worker
celery -A config worker --loglevel=info

# - Celery beat
celery -A config beat --loglevel=info
```

**Frontend:**
```bash
cd frontend

# Install dependencies
npm install

# Run dev server
npm run dev
# Access at http://localhost:5173
```

**Android:**
```bash
cd android-student

# Build
./gradlew build

# Install to device/emulator
./gradlew installDebug

# Or open in Android Studio
```

### Git Workflow

**Branch Strategy:**
- `main`: Production-ready code
- `develop`: Development branch (if exists)
- `claude/<session-id>`: AI assistant working branches
- Feature branches: `feature/<feature-name>`

**Commit Conventions:**
```bash
# Format: <type>: <description>

# Types:
feat:     # New feature
fix:      # Bug fix
refactor: # Code refactoring
docs:     # Documentation changes
test:     # Test additions/changes
chore:    # Build/config changes
```

**Examples:**
- `feat: Add recording session management`
- `fix: Resolve WebSocket reconnection issue`
- `refactor: Simplify Kafka producer logic`

### Database Migrations

```bash
# Create migrations
python manage.py makemigrations

# Apply migrations
python manage.py migrate

# View migration SQL (optional)
python manage.py sqlmigrate <app_name> <migration_number>

# Check migration status
python manage.py showmigrations

# In Docker:
docker-compose exec backend python manage.py migrate
```

### Managing Dependencies

**Backend:**
```bash
# Add new package
pip install <package>
pip freeze > requirements.txt

# Or manually add to requirements.txt and:
pip install -r requirements.txt
```

**Frontend:**
```bash
# Add new package
npm install <package>
# Automatically updates package.json
```

**Android:**
```kotlin
// Edit app/build.gradle.kts
dependencies {
    implementation("group:artifact:version")
}
```

---

## 🎨 Key Conventions

### Code Style

#### Backend (Python/Django)
- **Style Guide**: PEP 8
- **Line Length**: 88 characters (Black formatter default)
- **Imports**: Organized with isort (stdlib → third-party → local)
- **Formatting**: Use Black for auto-formatting
- **Linting**: flake8 for code quality

**Example:**
```python
# Good
from django.db import models
from django.conf import settings

from apps.lectures.models import Lecture


class LectureSession(models.Model):
    """실시간 강의방 모델"""

    lecture = models.ForeignKey(
        Lecture,
        on_delete=models.CASCADE,
        related_name='sessions'
    )
```

#### Frontend (TypeScript/React)
- **Style**: Functional components with hooks
- **Naming**:
  - Components: PascalCase (`DashboardPage.tsx`)
  - Functions: camelCase (`fetchLectures()`)
  - Constants: UPPER_SNAKE_CASE (`API_BASE_URL`)
- **File Structure**: One component per file

**Example:**
```typescript
// Good
export function DashboardPage() {
  const [lectures, setLectures] = useState<Lecture[]>([]);

  useEffect(() => {
    fetchLectures();
  }, []);

  return <div>...</div>;
}
```

#### Android (Kotlin)
- **Architecture**: MVVM + Clean Architecture
- **Naming**:
  - Classes: PascalCase
  - Functions: camelCase
  - Constants: UPPER_SNAKE_CASE
- **Compose**: Prefer composable functions over views
- **DI**: Use Hilt for dependency injection

**Example:**
```kotlin
// Good
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Initial)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    fun joinSession(code: String) {
        viewModelScope.launch {
            // ...
        }
    }
}
```

### Database Conventions

#### Model Naming
- **Models**: Singular, PascalCase (e.g., `LectureSession`, `ActivityLog`)
- **Tables**: Plural snake_case via `db_table` (e.g., `lecture_sessions`, `activity_logs`)
- **Fields**: snake_case (e.g., `created_at`, `session_code`)

#### Foreign Keys
- Always specify `related_name` for reverse lookups
- Use `on_delete` explicitly (CASCADE, SET_NULL, PROTECT)
- Add database indexes for foreign keys

**Example:**
```python
user = models.ForeignKey(
    settings.AUTH_USER_MODEL,
    on_delete=models.CASCADE,
    related_name='activity_logs',
    verbose_name='사용자'
)
```

#### Indexes
Add indexes for:
- Foreign keys (automatic in PostgreSQL)
- Frequently queried fields (status, timestamps)
- Fields used in filtering/sorting

### API Conventions

#### REST API Structure
```
/api/
├── token/                    # Authentication
│   ├── POST /                # Get JWT token
│   └── POST /refresh/        # Refresh token
├── lectures/                 # Lecture management
│   ├── GET /                 # List lectures
│   ├── POST /                # Create lecture
│   ├── GET /{id}/            # Get lecture detail
│   ├── PUT /{id}/            # Update lecture
│   └── DELETE /{id}/         # Delete lecture
├── sessions/                 # Session management
│   ├── GET /                 # List sessions
│   ├── POST /                # Create session
│   └── GET /{id}/            # Get session detail
├── students/                 # Student-specific endpoints
│   ├── POST /sessions/join/  # Join session
│   └── GET /sessions/my_sessions/
└── logs/                     # Activity logging
    └── POST /activity/       # Submit activity log
```

#### Response Format
```json
// Success
{
  "id": 1,
  "title": "Example",
  "created_at": "2025-11-18T10:00:00Z"
}

// List
{
  "count": 100,
  "next": "http://api/lectures/?page=2",
  "previous": null,
  "results": [...]
}

// Error
{
  "error": "Error message",
  "details": {...}  // Optional
}
```

#### Authentication
- Use JWT tokens (Authorization: Bearer <token>)
- Token refresh endpoint: `/api/token/refresh/`
- Tokens stored in Android DataStore, browser localStorage

### WebSocket Conventions

#### URL Pattern
```
ws://localhost:8000/ws/session/{session_code}/
ws://localhost:8000/ws/dashboard/{session_id}/
ws://localhost:8000/ws/progress/{user_id}/
```

#### Message Format
```json
// Client → Server
{
  "type": "step.change",
  "data": {
    "subtask_id": 5
  }
}

// Server → Client
{
  "type": "step.changed",
  "data": {
    "subtask_id": 5,
    "subtask_title": "카카오톡 열기"
  }
}
```

#### Message Types
- Session consumer: `step.change`, `participant.join`, `participant.leave`
- Dashboard consumer: `help.request`, `participant.status`
- Progress consumer: `progress.update`

### Environment Variables

**Backend (.env):**
```bash
# Django
SECRET_KEY=your-secret-key
DEBUG=True
ALLOWED_HOSTS=localhost,127.0.0.1

# Database
DB_NAME=mobilegpt_db
DB_USER=postgres
DB_PASSWORD=postgres
DB_HOST=db  # 'db' for Docker, 'localhost' for local
DB_PORT=5432

# Redis
REDIS_HOST=redis  # 'redis' for Docker, 'localhost' for local
REDIS_URL=redis://redis:6379/0

# Kafka
KAFKA_BOOTSTRAP_SERVERS=kafka:29092  # 'kafka:29092' for Docker, 'localhost:9092' for local

# Channels
USE_REDIS_CHANNELS=True

# Optional: AI/OpenAI (currently not actively used)
# OPENAI_API_KEY=sk-...
```

**Frontend (Vite):**
```bash
VITE_API_BASE_URL=http://localhost:8000/api
VITE_WS_BASE_URL=ws://localhost:8000/ws
```

**Android (BuildConfig):**
```kotlin
buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8000/api/\"")
buildConfigField("String", "WS_BASE_URL", "\"ws://10.0.2.2:8000/ws/\"")
// Note: 10.0.2.2 is Android emulator's host machine
```

---

## 🛠️ Common Tasks

### Task 1: Add a New Backend API Endpoint

1. **Create/Update Serializer** (`apps/<app>/serializers.py`):
```python
from rest_framework import serializers

class MyModelSerializer(serializers.ModelSerializer):
    class Meta:
        model = MyModel
        fields = '__all__'
```

2. **Create View** (`apps/<app>/views.py`):
```python
from rest_framework import viewsets

class MyModelViewSet(viewsets.ModelViewSet):
    queryset = MyModel.objects.all()
    serializer_class = MyModelSerializer
    permission_classes = [IsAuthenticated]
```

3. **Add URL** (`apps/<app>/urls.py`):
```python
from rest_framework.routers import DefaultRouter

router = DefaultRouter()
router.register(r'mymodel', MyModelViewSet)

urlpatterns = router.urls
```

4. **Include in main URLs** (`config/urls.py`):
```python
urlpatterns = [
    path('api/<app>/', include('apps.<app>.urls')),
]
```

### Task 2: Add a WebSocket Consumer

1. **Create Consumer** (`apps/<app>/consumers.py`):
```python
from channels.generic.websocket import AsyncJsonWebsocketConsumer

class MyConsumer(AsyncJsonWebsocketConsumer):
    async def connect(self):
        self.room_name = self.scope['url_route']['kwargs']['room_id']
        self.room_group_name = f'my_group_{self.room_name}'

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

    async def receive_json(self, content):
        message_type = content.get('type')
        if message_type == 'my.message':
            await self.handle_my_message(content)
```

2. **Create Routing** (`apps/<app>/routing.py`):
```python
from django.urls import re_path
from . import consumers

websocket_urlpatterns = [
    re_path(r'ws/myapp/(?P<room_id>\w+)/$', consumers.MyConsumer.as_asgi()),
]
```

3. **Register in Main Routing** (`config/routing.py`):
```python
from apps.myapp.routing import websocket_urlpatterns as myapp_ws

websocket_urlpatterns = [...] + myapp_ws
```

### Task 3: Add Frontend Page/Component

1. **Create Component** (`src/pages/my-page.tsx`):
```typescript
import { useState, useEffect } from 'react';
import { Button } from '@/components/ui/button';

export function MyPage() {
  const [data, setData] = useState([]);

  useEffect(() => {
    // Fetch data
  }, []);

  return (
    <div>
      <h1>My Page</h1>
      {/* ... */}
    </div>
  );
}
```

2. **Add Route** (in main routing file or `App.tsx`):
```typescript
import { MyPage } from './pages/my-page';

// In router configuration
{
  path: '/my-page',
  element: <MyPage />
}
```

### Task 4: Add Android Screen

1. **Create Screen** (`presentation/screen/MyScreen.kt`):
```kotlin
@Composable
fun MyScreen(
    viewModel: MyViewModel = hiltViewModel(),
    navController: NavController
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text("My Screen")
        // ...
    }
}
```

2. **Create ViewModel** (`presentation/viewmodel/MyViewModel.kt`):
```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val repository: MyRepository
) : ViewModel() {

    private val _state = MutableStateFlow<MyState>(MyState.Initial)
    val state: StateFlow<MyState> = _state.asStateFlow()

    fun loadData() {
        viewModelScope.launch {
            // ...
        }
    }
}
```

3. **Add to Navigation** (`presentation/navigation/NavGraph.kt`):
```kotlin
composable("my_screen") {
    MyScreen(navController = navController)
}
```

### Task 5: Send Activity Log from Android to Backend

**Android Side:**
```kotlin
// In AccessibilityService or ViewModel
val log = ActivityLogRequest(
    eventType = "CLICK",
    eventData = mapOf("text" to nodeText),
    screenInfo = mapOf("packageName" to packageName),
    // ...
)

// Via repository
viewModelScope.launch {
    try {
        val response = studentApi.submitActivityLog(log)
        // Success
    } catch (e: Exception) {
        // Error handling
    }
}
```

**Backend receives:**
1. `POST /api/logs/activity/` → `ActivityLogCreateView`
2. View validates and sends to Kafka via `ActivityLogProducer`
3. Kafka consumer (`consume_activity_logs`) processes and saves to DB
4. Fallback: If Kafka fails, saves directly to DB

### Task 6: Run Tests

**Backend:**
```bash
# Run all tests
pytest

# Run with coverage
pytest --cov=apps

# Run specific app
pytest apps/sessions/tests/

# In Docker
docker-compose exec backend pytest --cov=apps
```

**Android:**
```bash
# Unit tests
./gradlew test

# Instrumentation tests
./gradlew connectedAndroidTest
```

---

## 🧪 Testing & Quality

### Backend Testing

**Test Structure:**
```
apps/<app>/
├── tests/
│   ├── __init__.py
│   ├── test_models.py
│   ├── test_views.py
│   ├── test_serializers.py
│   └── test_websockets.py
```

**Example Test:**
```python
import pytest
from django.contrib.auth import get_user_model
from apps.lectures.models import Lecture

User = get_user_model()

@pytest.mark.django_db
def test_create_lecture():
    user = User.objects.create_user(
        username='instructor',
        password='testpass123'
    )
    lecture = Lecture.objects.create(
        title='Test Lecture',
        instructor=user
    )
    assert lecture.title == 'Test Lecture'
    assert lecture.instructor == user
```

**Running Tests:**
```bash
# All tests
pytest

# Specific file
pytest apps/lectures/tests/test_models.py

# With coverage report
pytest --cov=apps --cov-report=html
```

### Code Quality Tools

**Backend:**
```bash
# Format code
black .

# Sort imports
isort .

# Lint
flake8

# All at once (if configured in pre-commit or Makefile)
make lint
```

**Frontend:**
```bash
# Build check
npm run build

# Type check (if configured)
npm run type-check
```

### Manual Testing Scenarios

#### 1. Session Creation & Join Flow
1. Create instructor account via Django admin
2. Create lecture via admin or API
3. Create session → Note 6-char code
4. On Android app: Join with code
5. Verify WebSocket connection in backend logs

#### 2. Activity Logging Flow
1. Enable AccessibilityService on Android device
2. Use any app (e.g., Settings, Chrome)
3. Check Logcat: `adb logcat | grep MobileGPT`
4. Check backend Kafka consumer logs
5. Verify logs in DB: `docker-compose exec backend python manage.py shell`
   ```python
   from apps.logs.models import ActivityLog
   ActivityLog.objects.all()[:5]
   ```

#### 3. WebSocket Real-time Updates
1. Instructor creates session and moves to step 1
2. Student joins session via Android
3. Instructor changes step → Student should receive update
4. Check browser DevTools → Network → WS tab

---

## 🐛 Troubleshooting

### Common Issues

#### Issue 1: Docker Compose Services Not Starting

**Symptoms:**
- Services show "unhealthy" or keep restarting
- Database connection errors

**Solutions:**
```bash
# Check service status
docker-compose ps

# Check logs
docker-compose logs backend
docker-compose logs db

# Rebuild containers
docker-compose down -v
docker-compose up --build

# Check for port conflicts
sudo lsof -i :5432  # PostgreSQL
sudo lsof -i :6379  # Redis
sudo lsof -i :9092  # Kafka
```

#### Issue 2: Kafka Consumer Not Processing Logs

**Symptoms:**
- Logs sent from Android but not appearing in DB
- Consumer shows connection errors

**Solutions:**
```bash
# Check Kafka consumer logs
docker-compose logs -f consumer

# Verify Kafka topics
docker-compose exec kafka kafka-topics --list --bootstrap-server localhost:9092

# Manually test Kafka
docker-compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic activity-logs \
  --from-beginning
```

#### Issue 3: WebSocket Connection Failed

**Symptoms:**
- Frontend/Android can't connect to WebSocket
- "WebSocket connection failed" errors

**Solutions:**
1. Verify backend is using Daphne (not Gunicorn for WebSocket)
   ```bash
   # Should see Daphne in docker-compose.yml
   command: daphne -b 0.0.0.0 -p 8000 config.asgi:application
   ```

2. Check CORS and ALLOWED_HOSTS settings:
   ```python
   # backend/config/settings.py
   ALLOWED_HOSTS = ['localhost', '127.0.0.1', '10.0.2.2']
   CORS_ALLOWED_ORIGINS = [
       'http://localhost:5173',
       'http://127.0.0.1:5173',
   ]
   ```

3. Check Redis connection for Channels:
   ```bash
   docker-compose logs redis
   ```

4. Test WebSocket manually:
   ```bash
   # Use websocat or browser console
   websocat ws://localhost:8000/ws/session/ABC123/
   ```

#### Issue 4: Android App Can't Connect to Backend

**Symptoms:**
- Network errors in Android app
- "Unable to resolve host" errors

**Solutions:**
1. Verify API URL in `app/build.gradle.kts`:
   ```kotlin
   // For emulator
   buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8000/api/\"")

   // For physical device on same network
   buildConfigField("String", "API_BASE_URL", "\"http://192.168.x.x:8000/api/\"")
   ```

2. Add network security config (`res/xml/network_security_config.xml`):
   ```xml
   <?xml version="1.0" encoding="utf-8"?>
   <network-security-config>
       <base-config cleartextTrafficPermitted="true">
           <trust-anchors>
               <certificates src="system" />
           </trust-anchors>
       </base-config>
   </network-security-config>
   ```

3. Check AndroidManifest.xml:
   ```xml
   <uses-permission android:name="android.permission.INTERNET" />
   <application
       android:networkSecurityConfig="@xml/network_security_config"
       ...>
   ```

#### Issue 5: Database Migration Errors

**Symptoms:**
- `python manage.py migrate` fails
- "Relation does not exist" errors

**Solutions:**
```bash
# Reset migrations (DANGER: loses data)
docker-compose exec backend python manage.py migrate --fake <app> zero
docker-compose exec backend python manage.py migrate

# Or start fresh
docker-compose down -v  # Removes volumes
docker-compose up -d
docker-compose exec backend python manage.py migrate

# Check migration status
docker-compose exec backend python manage.py showmigrations
```

#### Issue 6: Frontend Build Errors

**Symptoms:**
- `npm run dev` or `npm run build` fails
- Module not found errors

**Solutions:**
```bash
# Clear node_modules and reinstall
rm -rf node_modules package-lock.json
npm install

# Clear Vite cache
rm -rf node_modules/.vite
npm run dev
```

---

## 📚 Important Context

### Project History & Simplifications

**Original Vision:**
- Full M-GPT AI integration for analyzing user behavior
- Automatic help suggestions based on AI analysis
- Complex prompt engineering and context management

**Current State (Simplified):**
- Basic infrastructure in place (Kafka, logging, sessions)
- AI analysis features **removed/simplified** for educational purposes
- Focus on core functionality: logging, sessions, WebSocket communication
- OpenAI integration exists in requirements but is **not actively used**

**Why Simplified:**
This is an educational/school project. The full AI integration was deemed too complex for the scope, so the project focuses on demonstrating:
1. Microservices architecture (Django + React + Android)
2. Real-time communication (WebSocket)
3. Async message processing (Kafka)
4. Modern mobile development (Jetpack Compose, Kotlin)

### Key Design Decisions

1. **Kafka for Activity Logs**
   - **Why**: Decouple log collection from processing
   - **Benefit**: Backend can handle bursts of logs without blocking
   - **Fallback**: If Kafka fails, logs save directly to DB

2. **Django Channels with Redis**
   - **Why**: Real-time instructor-student communication
   - **Alternative considered**: Socket.IO (rejected for Django integration)

3. **Separate Frontend & Backend**
   - **Why**: Decoupled development, scalability
   - **Alternative**: Django templates (rejected for modern UI needs)

4. **AccessibilityService on Android**
   - **Why**: Capture user interactions without root
   - **Limitation**: Requires user permission, can't access all apps

5. **JWT Authentication**
   - **Why**: Stateless, works well with mobile + web
   - **Consideration**: Tokens stored in DataStore (Android) and localStorage (web)

### File Naming Patterns

**Backend:**
- Models: `models.py`
- Views: `views.py`
- Serializers: `serializers.py`
- URLs: `urls.py`
- WebSocket: `consumers.py`, `routing.py`
- Tests: `test_*.py`
- Management commands: `management/commands/<command_name>.py`

**Frontend:**
- Pages: `<name>-page.tsx` (kebab-case)
- Components: `<name>.tsx` (PascalCase folders in ui/)
- Services: `<name>-service.ts`
- Types: `types.ts` or `<feature>-types.ts`

**Android:**
- Activities: `<Name>Activity.kt`
- Fragments: `<Name>Fragment.kt`
- ViewModels: `<Name>ViewModel.kt`
- Composables: `<Name>Screen.kt`
- Repositories: `<Name>Repository.kt`
- API interfaces: `<Name>Api.kt`

### Important Files to Know

**Backend:**
- `config/settings.py`: Django configuration (DB, Kafka, Redis, apps)
- `config/routing.py`: WebSocket routing
- `apps/sessions/consumers.py`: Session WebSocket logic
- `apps/logs/kafka_producer.py`: Kafka producer singleton
- `apps/logs/management/commands/consume_activity_logs.py`: Kafka consumer
- `docker-compose.yml`: Full infrastructure setup

**Frontend:**
- `src/lib/api-service.ts`: REST API client
- `src/lib/live-session-service.ts`: WebSocket client
- `src/lib/auth-context.tsx`: Authentication state
- `vite.config.ts`: Build configuration

**Android:**
- `service/MobileGPTAccessibilityService.kt`: Captures UI events
- `di/NetworkModule.kt`: Hilt DI for network dependencies
- `data/api/`: Retrofit API interfaces
- `app/build.gradle.kts`: Dependencies and configuration

### Environment-Specific Notes

**Docker Environment:**
- Backend connects to `db`, `redis`, `kafka` (service names)
- Kafka uses internal listener: `kafka:29092`
- Frontend uses `VITE_API_BASE_URL=http://localhost:8000/api`

**Local Development:**
- Backend connects to `localhost` for DB, Redis, Kafka
- Kafka uses external listener: `localhost:9092`
- Must ensure PostgreSQL, Redis, Kafka running locally

**Android Emulator:**
- Backend at `10.0.2.2` (host machine)
- WebSocket: `ws://10.0.2.2:8000/ws`

**Android Physical Device:**
- Backend at `<computer-ip>` (e.g., `192.168.1.100`)
- Ensure firewall allows connections on port 8000

### Performance Considerations

1. **Database Indexing**:
   - All foreign keys have indexes
   - Add indexes to frequently queried fields (status, timestamps)

2. **Kafka Batch Processing**:
   - ActivityLogProducer supports batch sending
   - Consumer processes in batches for efficiency

3. **WebSocket Scalability**:
   - Redis backend allows horizontal scaling
   - Consider load balancing for production

4. **Android Battery**:
   - AccessibilityService is battery-intensive
   - Consider throttling log submission in production

### Security Notes

⚠️ **Important Security Considerations:**

1. **DEBUG=True in .env**
   - Only for development
   - Set `DEBUG=False` in production

2. **SECRET_KEY**
   - Change default secret key
   - Use strong random key in production

3. **ALLOWED_HOSTS**
   - Restrict to actual domains in production

4. **CORS**
   - Restrict CORS origins in production
   - Currently allows localhost (dev only)

5. **AccessibilityService**
   - Captures sensitive data (passwords, etc.)
   - Use `is_sensitive_data` flag to filter
   - Consider data retention policies

6. **API Authentication**
   - All endpoints should require authentication except login/register
   - JWT tokens expire (configure in settings)

---

## 📝 Quick Reference

### Useful Commands

**Docker:**
```bash
docker-compose up -d                    # Start all services
docker-compose down                     # Stop all services
docker-compose down -v                  # Stop and remove volumes
docker-compose logs -f <service>        # Follow logs
docker-compose exec <service> <cmd>     # Run command in service
docker-compose ps                       # Check status
docker-compose restart <service>        # Restart service
```

**Django:**
```bash
python manage.py migrate                # Run migrations
python manage.py makemigrations         # Create migrations
python manage.py createsuperuser        # Create admin user
python manage.py shell                  # Django shell
python manage.py runserver              # Dev server (no WebSocket)
daphne config.asgi:application          # Dev server (with WebSocket)
python manage.py consume_activity_logs  # Run Kafka consumer
```

**Database:**
```bash
# Access PostgreSQL in Docker
docker-compose exec db psql -U postgres -d mobilegpt_db

# Common queries
SELECT COUNT(*) FROM activity_logs;
SELECT * FROM lecture_sessions WHERE status = 'IN_PROGRESS';
```

**Kafka:**
```bash
# List topics
docker-compose exec kafka kafka-topics --list --bootstrap-server localhost:9092

# Read from topic
docker-compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic activity-logs \
  --from-beginning
```

**Android:**
```bash
./gradlew build                         # Build
./gradlew installDebug                  # Install debug APK
./gradlew clean                         # Clean build
adb logcat | grep MobileGPT             # View logs
adb devices                             # List devices
```

### Key URLs

**Development:**
- Backend API: http://localhost:8000/api
- Django Admin: http://localhost:8000/admin
- API Docs: http://localhost:8000/api/docs (if configured)
- Frontend: http://localhost:5173
- Health Check: http://localhost:8000/api/health/

**WebSocket:**
- Session: `ws://localhost:8000/ws/session/<code>/`
- Dashboard: `ws://localhost:8000/ws/dashboard/<session_id>/`
- Progress: `ws://localhost:8000/ws/progress/<user_id>/`

### When to Ask for Clarification

**Always ask the user before:**
1. Deleting or significantly refactoring existing code
2. Changing database schema (migrations)
3. Modifying Docker configuration
4. Changing authentication/security settings
5. Adding new external dependencies
6. Modifying WebSocket message formats (breaks clients)

**You can proceed directly for:**
1. Bug fixes in implementation
2. Code formatting/style improvements
3. Adding comments/documentation
4. Writing tests
5. Adding new endpoints (if architecture is clear)
6. Updating documentation files

---

## 🎓 Learning Resources

**Django & DRF:**
- Official Docs: https://docs.djangoproject.com/
- DRF: https://www.django-rest-framework.org/

**Django Channels:**
- Docs: https://channels.readthedocs.io/

**Kafka:**
- Confluent Kafka Python: https://docs.confluent.io/kafka-clients/python/

**React & TypeScript:**
- React Docs: https://react.dev/
- TypeScript: https://www.typescriptlang.org/docs/

**Android (Jetpack Compose):**
- Compose Docs: https://developer.android.com/jetpack/compose
- Kotlin: https://kotlinlang.org/docs/

**Radix UI:**
- Components: https://www.radix-ui.com/primitives

---

## 📞 Getting Help

**For AI Assistants:**
- Refer to this file (CLAUDE.md) for all project-specific context
- Check README.md for user-facing setup instructions
- Review PROGRESS.md for recent changes and completion status
- Read plan.md for original project vision

**For Developers:**
- Check existing code in similar apps/features
- Review Django/React/Android official docs
- Search for error messages in GitHub issues
- Check Docker logs for service issues

---

**Last Updated**: 2025-11-18
**Maintained By**: Development Team
**Project Status**: Active Development (70% complete per PROGRESS.md)

---

_This file is specifically designed for AI assistants to understand the MobileGPT codebase. For user-facing documentation, see README.md._
