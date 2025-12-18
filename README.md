# MobEdu - AI 기반 모바일 교육 콘텐츠 자동 생성 시스템

**강의자 시연 녹화 → AI 분석 → 학습 과제 자동 생성 → 학생 실시간 학습**

[![Python](https://img.shields.io/badge/Python-3.11+-blue.svg)](https://www.python.org/)
[![Django](https://img.shields.io/badge/Django-4.2-green.svg)](https://www.djangoproject.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-purple.svg)](https://kotlinlang.org/)
[![React](https://img.shields.io/badge/React-18-61dafb.svg)](https://react.dev/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED.svg)](https://www.docker.com/)

---

## 프로젝트 개요

**MobEdu**는 시니어 사용자를 위한 AI 기반 모바일 교육 플랫폼입니다.

강의자가 스마트폰에서 앱 사용법을 시연하면, AI가 자동으로 분석하여 학습 과제로 변환하고, 학생들이 실시간으로 따라할 수 있는 교육 시스템을 제공합니다.

### 핵심 가치

```
┌─────────────────────────────────────────────────────────────────────┐
│                        MobEdu 워크플로우                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   [강의자 녹화]      [AI 분석]        [과제 생성]      [학생 학습]     │
│   ┌──────────┐      ┌──────────┐      ┌──────────┐     ┌──────────┐ │
│   │ 앱 시연   │  →   │ GPT-4o   │  →   │ Task +   │  →  │ 세션 참가 ││
│   │ UI 캡처  │      │  분석     │      │ Subtasks │     │ 단계 학습 ││
│   └──────────┘      └──────────┘      └──────────┘     └──────────┘ │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 핵심 기능 (완성됨)

| 기능 | 설명 | 상태 |
|------|------|------|
| **녹화 기능** | AccessibilityService로 UI 이벤트 자동 캡처 | ✅ |
| **AI 분석** | GPT-4o-mini 기반 단계별 가이드 자동 생성 | ✅ |
| **과제 변환** | 분석 결과 → Task/Subtask 구조로 변환 | ✅ |
| **실시간 세션** | WebSocket 기반 강사-학생 동기화 | ✅ |
| **웹 대시보드** | 강의 관리, 세션 제어, 진도 모니터링 | ✅ |
| **학생 앱** | 세션 참가, 단계별 학습, 도움 요청 | ✅ |
| **학습 통계** | 완료율, 도움 요청 분석, 세션 요약 | ✅ |

---

## 시스템 아키텍처

```
┌────────────────────────┐     ┌─────────────────────┐     ┌────────────────────────┐
│ Android Instructor App │     │   Web Dashboard     │     │  Android Student App   │
│  - 시연 녹화           │     │  - 강의/세션 관리    │     │  - 세션 참가           │
│  - 단계 편집           │     │  - 실시간 모니터링   │     │  - 단계별 학습         │
└──────────┬─────────────┘     └──────────┬──────────┘     └──────────┬─────────────┘
           │ REST API                     │                           │ WebSocket
           └──────────────────────────────┼───────────────────────────┘
                                          ▼
                               ┌─────────────────────┐
                               │   Django Backend    │
                               │  - JWT 인증         │
                               │  - 세션/녹화 관리    │
                               │  - GPT 분석 연동     │
                               │  - WebSocket 서버    │
                               │  - Celery 태스크     │
                               └──────────┬──────────┘
                                          │
                      ┌───────────────────┼───────────────────┐
                      ▼                   ▼                   ▼
                ┌──────────┐       ┌──────────┐       ┌──────────┐
                │PostgreSQL│       │  Redis   │       │  Kafka   │
                │ Database │       │  Cache   │       │  Logs    │
                └──────────┘       └──────────┘       └──────────┘
```

---

## 프로젝트 구조

```
cps 2025-2/
├── backend/                    # Django REST API 서버
│   ├── apps/
│   │   ├── accounts/          # JWT 인증 및 사용자 관리
│   │   ├── sessions/          # 녹화/세션 관리 (핵심 모듈)
│   │   ├── tasks/             # 과제/단계(Subtask) 관리
│   │   ├── lectures/          # 강의 관리
│   │   ├── logs/              # 활동 로그 (Kafka 연동)
│   │   ├── progress/          # 학습 진도 추적
│   │   ├── help/              # 도움 요청 시스템
│   │   ├── dashboard/         # 대시보드 API
│   │   ├── students/          # 수강생 API
│   │   └── health/            # 헬스체크
│   ├── config/                # Django 설정
│   └── docker-compose.yml     # 풀스택 인프라 (9개 서비스)
│
├── android-instructor/         # 강의자용 Android 앱 (Kotlin)
│   └── app/src/main/java/com/example/mobilegpt/
│       ├── recording/         # 녹화 기능
│       ├── subtask/           # 단계 편집
│       ├── viewmodel/         # MVVM ViewModel
│       └── data/              # API 클라이언트
│
├── android-student/            # 학생용 Android 앱 (Kotlin)
│   └── app/src/main/java/com/mobilegpt/student/
│       ├── presentation/      # UI (Jetpack Compose)
│       ├── domain/            # 비즈니스 로직
│       ├── data/              # Repository, API
│       └── service/           # AccessibilityService
│
├── frontend/                   # 웹 대시보드 (React + TypeScript)
│   └── src/
│       ├── pages/             # 페이지 컴포넌트
│       ├── components/        # UI 컴포넌트 (Radix UI)
│       ├── lib/               # API 서비스, 타입
│       └── styles/            # Tailwind CSS
│
├── CLAUDE.md                   # 개발 가이드 (용어, API 상세)
└── README.md                   # 이 파일
```

---

## 핵심 워크플로우

### 녹화 → 과제 변환 프로세스

```
┌─────────────────────────────────────────────────────────────────┐
│                    Recording Status Flow                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   RECORDING ──→ COMPLETED ──→ PROCESSING ──→ ANALYZED          │
│       │             │              │             │              │
│    녹화 중       녹화 완료      GPT 분석 중    분석 완료         │
│                                                  │              │
│                                                  ▼              │
│                                          convert-to-task        │
│                                                  │              │
│                                                  ▼              │
│                                      Task + Subtasks 생성       │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### API 호출 순서

```bash
# 1. 녹화 시작
POST /api/recordings/

# 2. UI 이벤트 배치 저장 (녹화 중 반복)
POST /api/recordings/{id}/save-events-batch/

# 3. 녹화 종료
POST /api/recordings/{id}/stop/

# 4. GPT 분석 시작 (비동기)
POST /api/recordings/{id}/analyze/

# 5. 분석 상태 확인
GET /api/recordings/{id}/analysis-status/

# 6. 과제로 변환
POST /api/recordings/{id}/convert-to-task/

# 7. 생성된 단계 조회
GET /api/recordings/{id}/subtasks/
```

---

## API 엔드포인트

### 인증 API
| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/token/` | JWT 토큰 발급 |
| POST | `/api/token/refresh/` | 토큰 갱신 |
| POST | `/api/auth/register/` | 회원가입 |
| GET | `/api/auth/me/` | 현재 사용자 정보 |

### 녹화 API (`/api/recordings/`)
| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/recordings/` | 녹화 시작 |
| GET | `/api/recordings/` | 녹화 목록 |
| POST | `/api/recordings/{id}/stop/` | 녹화 종료 |
| POST | `/api/recordings/{id}/save-events-batch/` | 이벤트 배치 저장 |
| POST | `/api/recordings/{id}/analyze/` | GPT 분석 시작 |
| GET | `/api/recordings/{id}/analysis-status/` | 분석 상태 조회 |
| POST | `/api/recordings/{id}/convert-to-task/` | 과제로 변환 |
| GET | `/api/recordings/{id}/subtasks/` | 단계 목록 조회 |

### 강의 API (`/api/lectures/`)
| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/lectures/` | 강의 목록 |
| POST | `/api/lectures/` | 강의 생성 |
| GET | `/api/lectures/{id}/tasks/` | 강의의 과제 목록 |

### 세션 API (`/api/sessions/`)
| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/sessions/{lecture_pk}/create/` | 세션 생성 |
| POST | `/api/sessions/{id}/start/` | 세션 시작 |
| POST | `/api/sessions/{id}/next-step/` | 다음 단계로 이동 |
| POST | `/api/sessions/{id}/pause/` | 세션 일시정지 |
| POST | `/api/sessions/{id}/resume/` | 세션 재개 |
| POST | `/api/sessions/{id}/end/` | 세션 종료 |
| GET | `/api/sessions/{id}/summary/` | 세션 요약 |

### 수강생 API (`/api/students/`)
| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/students/sessions/join/` | 세션 참가 |
| GET | `/api/students/sessions/my_sessions/` | 내 세션 목록 |
| POST | `/api/students/sessions/{id}/leave/` | 세션 나가기 |

### WebSocket
```
ws://localhost:8001/ws/sessions/{session_code}/
```

---

## 주요 기술 스택

### Backend
| 기술 | 버전 | 용도 |
|------|------|------|
| Django | 4.2 | 웹 프레임워크 |
| Django REST Framework | 3.14 | REST API |
| Django Channels | 4.0 | WebSocket |
| Celery | 5.3 | 비동기 태스크 |
| PostgreSQL | 15 | 데이터베이스 |
| Redis | 7 | 캐시/메시지 브로커 |
| Kafka | 7.5 | 로그 스트리밍 |
| OpenAI API | GPT-4o-mini | AI 분석 |

### Frontend
| 기술 | 버전 | 용도 |
|------|------|------|
| React | 18 | UI 프레임워크 |
| TypeScript | 5 | 타입 안전성 |
| Vite | 6 | 빌드 도구 |
| Radix UI | - | 컴포넌트 라이브러리 |
| Tailwind CSS | 3 | 스타일링 |
| Recharts | - | 차트 |

### Android
| 기술 | 버전 | 용도 |
|------|------|------|
| Kotlin | 1.9 | 언어 |
| Jetpack Compose | - | UI |
| Hilt | - | DI |
| Retrofit | 2 | HTTP 클라이언트 |
| OkHttp | 4 | 네트워크 |
| Coroutines | - | 비동기 처리 |

### Infrastructure
| 서비스 | 설명 |
|--------|------|
| Docker Compose | 컨테이너 오케스트레이션 |
| PostgreSQL | 메인 데이터베이스 |
| Redis | 캐시 및 Celery 브로커 |
| Kafka + Zookeeper | 이벤트 스트리밍 |
| Daphne | ASGI 서버 (WebSocket) |
| Celery Worker/Beat | 백그라운드 태스크 |

---

## 시작하기

### 사전 요구사항

- Docker & Docker Compose
- (Android 빌드 시) Android Studio + JDK 17

### 1. Docker Compose로 전체 시스템 실행

```bash
# 저장소 클론
git clone <repository-url>
cd cps\ 2025-2

# 환경 변수 설정
cp backend/.env.example backend/.env
# .env 파일에서 OPENAI_API_KEY 설정

# 전체 서비스 시작 (9개 컨테이너)
cd backend
docker-compose up -d --build

# 로그 확인
docker-compose logs -f

# 슈퍼유저 생성
docker-compose exec backend python manage.py createsuperuser

# 샘플 데이터 생성 (선택)
docker-compose exec backend python manage.py generate_sample_data
```

### 2. 서비스 접속 정보

| 서비스 | URL | 설명 |
|--------|-----|------|
| 웹 대시보드 | http://localhost:5173 | React 프론트엔드 |
| REST API | http://localhost:8000/api | Django API |
| WebSocket | ws://localhost:8001/ws | 실시간 통신 |
| Django Admin | http://localhost:8000/admin | 관리자 페이지 |

### 3. Android 앱 빌드

```bash
# 강의자 앱
cd android-instructor
./gradlew installDebug

# 학생 앱
cd android-student
./gradlew installDebug
```

> **참고**: `local.properties`에서 서버 URL 설정 필요

---

## 데이터 모델

### 핵심 모델 관계

```
RecordingSession (녹화)
├── task (FK → Task)           # 변환된 과제
├── lecture (FK → Lecture)     # 연결된 강의 (선택)
├── status                     # RECORDING|COMPLETED|PROCESSING|ANALYZED
└── analysis_result            # GPT 분석 결과 (JSON)

Task (과제)
├── lecture (FK → Lecture)     # 소속 강의 (선택)
├── title, description
└── subtasks (reverse FK)      # 단계들

Subtask (단계)
├── task (FK → Task)
├── order_index               # 순서
├── target_action             # CLICK|LONG_CLICK|SCROLL|INPUT|NAVIGATE
├── guide_text                # 가이드 텍스트
└── voice_guide_text          # 음성 가이드

LectureSession (실시간 세션)
├── lecture (FK → Lecture)
├── status                    # WAITING|IN_PROGRESS|ENDED
├── session_code              # 6자리 참가 코드
└── current_subtask           # 현재 진행 단계
```

---

## 완성된 기능 체크리스트

### Backend
- [x] JWT 인증 시스템
- [x] 녹화 API (시작/중지/이벤트 저장)
- [x] GPT 분석 연동 (Celery 비동기)
- [x] 과제 변환 서비스
- [x] 실시간 세션 관리
- [x] WebSocket 통신
- [x] Kafka 로그 수집
- [x] 세션 진행률 추적

### Frontend
- [x] 강의 CRUD
- [x] 세션 제어 (시작/일시정지/종료)
- [x] 실시간 모니터링 대시보드
- [x] 학습 통계 차트
- [x] QR 코드 세션 참가
- [x] WebSocket 실시간 업데이트

### Android (강의자)
- [x] 녹화 시작/종료
- [x] AccessibilityService 이벤트 캡처
- [x] 이벤트 배치 전송
- [x] 녹화 목록 조회
- [x] 단계 편집 UI
- [x] GPT 분석 요청
- [x] 과제 변환

### Android (학생)
- [x] 세션 코드 참가
- [x] 단계별 학습 UI
- [x] 진행률 표시
- [x] WebSocket 실시간 동기화
- [x] 도움 요청 기능

### Infrastructure
- [x] Docker Compose (9개 서비스)
- [x] PostgreSQL 데이터베이스
- [x] Redis 캐시/브로커
- [x] Kafka 로그 스트리밍
- [x] Celery Worker/Beat
- [x] Daphne WebSocket 서버

---

## 향후 도전과제 (Future Challenges)

개발이 완료된 MVP를 기반으로, 다음 기능들은 향후 확장을 위한 도전과제로 남겨둡니다.

### Challenge 1: 오프라인 모드
- **현재**: 온라인 연결 필수
- **목표**: 녹화 데이터 로컬 저장 후 나중에 동기화
- **난이도**: ★★☆☆☆

### Challenge 2: 테스트 커버리지 향상
- **현재**: 기본 테스트만 존재
- **목표**: pytest, Vitest로 70% 이상 커버리지
- **난이도**: ★★☆☆☆

### Challenge 3: AWS 프로덕션 배포
- **현재**: 로컬 Docker 환경
- **목표**: EC2 + RDS + ElastiCache + CloudFront 배포
- **난이도**: ★★★☆☆

### Challenge 4: AI 분석 정확도 향상
- **현재**: 기본 GPT-4o-mini 프롬프트
- **목표**: 프롬프트 엔지니어링, Few-shot learning, Fine-tuning
- **난이도**: ★★★★☆

### Challenge 5: 실시간 화면 공유
- **현재**: 텍스트 기반 진행 동기화
- **목표**: WebRTC 기반 실시간 화면 스트리밍
- **난이도**: ★★★★★

---

## 문제 해결

### Docker 서비스가 시작되지 않음
```bash
# 로그 확인
docker-compose logs backend

# 마이그레이션 수동 실행
docker-compose exec backend python manage.py migrate
```

### WebSocket 연결 실패
- REST API: 포트 8000 (Django)
- WebSocket: 포트 8001 (Daphne)
- Android 에뮬레이터: `ws://10.0.2.2:8001/ws/...` 사용

### Android 빌드 실패
```bash
./gradlew clean build --refresh-dependencies
```

---

## 참고 문서

- [CLAUDE.md](CLAUDE.md) - 개발 가이드 (용어 체계, API 상세)
- [backend/README.md](backend/README.md) - 백엔드 상세
- [frontend/README.md](frontend/README.md) - 프론트엔드 상세

---

## 라이선스

이 프로젝트는 교육 목적으로 개발되었습니다.

**CPS 2025-2 프로젝트** | 최종 업데이트: 2025-12-18
