# MobileGPT - 시니어 디지털 교육 서비스

AI 기반 실시간 디지털 교육 도우미 시스템

## 🎯 프로젝트 개요

MobileGPT는 시니어 사용자를 위한 디지털 교육 서비스로, AccessibilityService를 통해 사용자의 앱 사용 행동을 감지하고, M-GPT(Mobile GPT)를 활용하여 실시간으로 도움을 제공하는 시스템입니다.

### 핵심 기능

- 📱 **실시간 강의 세션** - 강사와 수강생이 동시에 연결되어 단계별 학습
- 🤖 **AI 기반 도움** - M-GPT가 사용자 행동을 분석하여 맞춤형 가이드 제공
- 👁️ **행동 감지** - AccessibilityService로 UI 이벤트 자동 수집
- 💬 **실시간 통신** - WebSocket 기반 강사-수강생 양방향 소통

## 🏗️ 시스템 아키텍처

```
┌─────────────────────┐
│  Android App (학생)  │ ← Kotlin, AccessibilityService
│  - UI 이벤트 감지    │
│  - 실시간 세션 참가  │
└──────────┬──────────┘
           │
           │ REST API / WebSocket
           ▼
┌─────────────────────┐
│  Backend (Django)   │
│  - 세션 관리        │
│  - 로그 수집        │
│  - M-GPT 연동       │
└──────────┬──────────┘
           │
           │ Kafka / API
           ▼
┌─────────────────────┐
│   M-GPT (AI Layer)  │
│  - 로그 분석        │
│  - 도움말 생성      │
└─────────────────────┘
```

## 📁 프로젝트 구조

```
cps 2025-2/
├── backend/                    # Django 백엔드
│   ├── apps/
│   │   ├── accounts/          # 사용자 관리
│   │   ├── lectures/          # 강의 관리
│   │   ├── sessions/          # 실시간 세션
│   │   ├── tasks/             # 과제 및 단계
│   │   ├── progress/          # 학습 진행률
│   │   ├── logs/              # 활동 로그
│   │   ├── help/              # 도움 요청
│   │   ├── dashboard/         # 강사 대시보드 API
│   │   └── students/          # 수강생 API
│   └── config/                # Django 설정
│
├── frontend/                   # 강의자 대시보드 (Vite + React)
│   ├── src/
│   │   ├── components/        # UI 컴포넌트 (Radix UI)
│   │   ├── lib/               # API 서비스, 타입 정의
│   │   ├── pages/             # 페이지 컴포넌트
│   │   └── styles/            # 전역 스타일
│   ├── index.html
│   ├── package.json
│   └── vite.config.ts
│
├── android-student/            # Android 수강생 앱
│   └── app/
│       └── src/main/java/com/mobilegpt/student/
│           ├── data/          # API, Repository
│           ├── domain/        # Models, UseCase
│           ├── presentation/  # UI, ViewModel
│           ├── service/       # AccessibilityService
│           └── di/            # Hilt DI
│
└── design/                     # 디자인 문서
```

## 🚀 시작하기

### 사전 요구사항

#### Docker (권장)
- Docker & Docker Compose

#### Backend (로컬 개발 시)
- Python 3.11+
- Django 4.2+
- PostgreSQL 15+
- Redis
- Kafka

#### Frontend (로컬 개발 시)
- Node.js 20+
- npm

#### Android App
- Android Studio Hedgehog (2023.1.1)+
- JDK 17
- Android SDK 34
- Gradle 8.2

### 1. Docker Compose로 전체 시스템 실행 (권장)

```bash
# 환경 변수 설정
cp backend/.env.example backend/.env

# backend 폴더에서 모든 서비스 시작
cd backend
docker-compose up -d

# 로그 확인
docker-compose logs -f

# 슈퍼유저 생성
docker-compose exec backend python manage.py createsuperuser
```

서비스 접속:
| 서비스 | URL | 설명 |
|--------|-----|------|
| **프론트엔드** | http://localhost:5173 | React 대시보드 |
| **백엔드 API** | http://localhost:8000/api | REST API |
| **WebSocket** | ws://localhost:8001/ws | 실시간 통신 (Daphne) |
| **Django Admin** | http://localhost:8000/admin | 관리자 페이지 |

### 2. 로컬 개발 (Docker 없이)

#### Backend

```bash
cd backend

# 가상환경 생성 및 활성화
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# 의존성 설치
pip install -r requirements.txt

# 환경 변수 설정
cp .env.example .env
# .env 파일 수정 (DB_HOST=localhost 등)

# 데이터베이스 마이그레이션
python manage.py migrate

# 슈퍼유저 생성
python manage.py createsuperuser

# 개발 서버 실행 (WebSocket 지원)
daphne -b 0.0.0.0 -p 8000 config.asgi:application
```

별도 터미널에서 Kafka Consumer 실행:
```bash
cd backend
source venv/bin/activate
python manage.py consume_activity_logs
```

#### Frontend

```bash
cd frontend

# 의존성 설치
npm install

# 개발 서버 실행
npm run dev
```

프론트엔드: http://localhost:3000 (로컬 개발 시)

### 3. Android 앱 빌드 및 실행

```bash
cd android-student

# Android Studio에서 프로젝트 열기
# 또는

# 커맨드라인에서 빌드
./gradlew build

# 에뮬레이터/기기에 설치
./gradlew installDebug
```

자세한 내용은 [android-student/README.md](android-student/README.md) 참고

## 📡 API 엔드포인트

### 인증
```
POST /api/token/                  # JWT 토큰 발급
POST /api/token/refresh/          # 토큰 갱신
POST /api/auth/register/          # 회원가입
```

### 수강생 API (NEW)
```
POST /api/students/sessions/join/              # 세션 참가
GET  /api/students/sessions/my_sessions/       # 내 세션 목록
GET  /api/students/sessions/active_sessions/   # 진행 중인 세션
POST /api/students/sessions/{id}/leave/        # 세션 나가기
GET  /api/students/lectures/                   # 강의 목록
POST /api/students/lectures/{id}/enroll/       # 수강 신청
```

### 강의 및 세션
```
GET  /api/lectures/               # 강의 목록
POST /api/lectures/               # 강의 생성 (강사)
GET  /api/sessions/               # 세션 목록
POST /api/sessions/               # 세션 생성 (강사)
```

### WebSocket
```
ws://localhost:8001/ws/sessions/{session_code}/
```

> **참고**: WebSocket은 Daphne 서버(포트 8001)에서 처리됩니다. REST API(포트 8000)와 분리되어 있습니다.

## 🔑 주요 기술 스택

### Backend
- **Framework**: Django 4.2, Django REST Framework
- **WebSocket**: Django Channels
- **Database**: PostgreSQL 15
- **Caching**: Redis
- **Message Queue**: Kafka
- **Task Queue**: Celery
- **AI**: OpenAI API (M-GPT)

### Frontend
- **Build Tool**: Vite
- **Framework**: React 18
- **Language**: TypeScript
- **UI**: Radix UI, Tailwind CSS
- **Routing**: React Router
- **State**: React Context API

### Android
- **Language**: Kotlin
- **Architecture**: MVVM + Clean Architecture
- **UI**: Jetpack Compose
- **DI**: Hilt
- **Networking**: Retrofit, OkHttp, Scarlet (WebSocket)
- **Async**: Coroutines, Flow

## 📊 데이터베이스 스키마

주요 모델:
- `User` - 사용자 (강사/수강생)
- `Lecture` - 강의
- `LectureSession` - 실시간 강의 세션
- `Task` - 과제
- `Subtask` - 세부 단계
- `SessionParticipant` - 세션 참가자
- `ActivityLog` - 사용자 활동 로그
- `HelpRequest` - 도움 요청

자세한 스키마는 `backend/apps/*/models.py` 참고

## 🧪 테스트 시나리오

### 1. 세션 생성 및 참가 (기본 플로우)

**강사 측**:
1. Django Admin에서 로그인
2. 강의 생성
3. 세션 생성 → 6자리 코드 생성 (예: ABC123)

**수강생 측**:
1. Android 앱 실행
2. "세션 참가" 버튼 클릭
3. 코드 입력 (ABC123)
4. WebSocket 연결 확인

### 2. AccessibilityService 테스트

1. Android 설정 → 접근성 → "MobileGPT 학습 도우미" 활성화
2. 다른 앱 사용 (예: 카카오톡)
3. Logcat 확인:
```bash
adb logcat | grep "MobileGPT_A11y"
```

### 3. 실시간 단계 동기화

1. 강사가 단계 전환
2. WebSocket으로 수강생에게 메시지 전송
3. 수강생 앱에서 단계 업데이트 수신

## 📖 개발 문서

- [백엔드 README](backend/README.md)
- [프론트엔드 README](frontend/README.md)
- [Android 앱 README](android-student/README.md)
- [WebSocket 가이드](backend/WEBSOCKET_GUIDE.md)
- [기획 문서](plan.md)

## 🐛 문제 해결

### Django 서버가 시작되지 않음
```bash
# 마이그레이션 확인
python manage.py makemigrations
python manage.py migrate

# 로그 확인
tail -f backend/logs/django.log
```

### Android 앱 빌드 실패
```bash
cd android-student
./gradlew clean build --refresh-dependencies
```

### WebSocket 연결 실패
- **포트 확인**: WebSocket은 8001 포트 (Daphne), REST API는 8000 포트
- Daphne 서버 상태 확인: `docker-compose logs daphne`
- CORS 설정 확인 (`backend/config/settings.py`)
- Android 에뮬레이터: `ws://10.0.2.2:8001/ws/...` 사용

## 🔐 보안 고려사항

- JWT 토큰 기반 인증
- AccessibilityService 권한 관리
- 로그 데이터 익명화
- HTTPS/WSS 사용 (프로덕션)

## 📝 TODO

### Backend
- [x] REST API 구현
- [x] WebSocket 통신
- [x] Kafka 연동
- [ ] M-GPT 통합
- [ ] 테스트 코드 작성

### Frontend
- [x] 강의자 대시보드 UI
- [x] 실시간 세션 모니터링
- [ ] 실제 API 연동 (현재 Mock)
- [ ] WebSocket 실시간 업데이트
- [ ] 테스트 코드 작성

### Android
- [ ] UI 화면 구현 (Jetpack Compose)
- [ ] ViewModel 및 상태 관리
- [ ] 오버레이 도움말 UI
- [ ] 푸시 알림
- [ ] 오프라인 모드
- [ ] 테스트 코드 작성

## 👥 팀

교육 목적 프로젝트

## 📄 라이선스

이 프로젝트는 교육 목적으로 개발되었습니다.
