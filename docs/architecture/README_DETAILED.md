# Architecture Documentation

**MobileGPT Senior Digital Education Service**

이 문서는 MobileGPT 프로젝트의 전체 아키텍처를 설명합니다. 각 다이어그램은 [Draw.io](https://app.diagrams.net) 또는 VS Code의 Draw.io Integration 확장으로 열어 편집할 수 있습니다.

---

## 📋 목차

1. [시스템 전체 구조도](#1-시스템-전체-구조도)
2. [세션 제어 플로우](#2-세션-제어-플로우)
3. [Activity Log 파이프라인](#3-activity-log-파이프라인)
4. [녹화→Task 자동 생성](#4-녹화task-자동-생성)
5. [도움 요청 플로우](#5-도움-요청-플로우)
6. [기술 스택](#6-기술-스택)
7. [확장성 고려사항](#7-확장성-고려사항)
8. [OpenAI API 통합 가이드](#8-openai-api-통합-가이드)

---

## 1. 시스템 전체 구조도

**파일**: [`system-architecture.drawio`](./system-architecture.drawio)

### 개요

전체 시스템은 **3개의 클라이언트 애플리케이션**과 **1개의 백엔드 서버**, 그리고 **4개의 인프라 컴포넌트**로 구성됩니다.

### 주요 컴포넌트

#### Frontend (Web - 강사용)
- **기술 스택**: React 18.3 + Vite + TypeScript
- **UI 라이브러리**: Radix UI + Tailwind CSS
- **상태 관리**: React Context API
- **통신**: Axios (REST API) + WebSocket Client
- **주요 페이지**:
  - Dashboard - 전체 강의 및 세션 현황
  - Lectures - 강의 목록 및 관리
  - Live Session Control - 실시간 세션 제어
  - Statistics - 학습 분석 및 통계

#### Android Student App
- **기술 스택**: Kotlin + Jetpack Compose
- **아키텍처**: MVVM + Clean Architecture
- **DI**: Hilt
- **네트워킹**: Retrofit (REST) + Scarlet (WebSocket)
- **핵심 기능**:
  - **AccessibilityService**: 시스템 레벨 UI 이벤트 캡처
  - 세션 코드 입력 및 참가
  - 실시간 단계별 학습 가이드
  - 도움 요청 및 힌트 수신

#### Backend (Django + Daphne ASGI)
- **Django Apps 구조**:
  - `accounts` - 사용자 인증 (JWT), 역할 관리 (강사/학생)
  - `lectures` - 강의 및 등록 관리
  - `sessions` - 실시간 세션 관리, 6자리 코드 생성
  - `tasks` - Task/Subtask 커리큘럼 구조
  - `progress` - 학생별 진도 추적
  - `logs` - ActivityLog 수집 및 저장
  - `help` - 도움 요청 및 M-GPT 분석
  - `dashboard` - 강사 대시보드 API
  - `students` - 학생 전용 API

- **WebSocket Layer (Django Channels)**:
  - `SessionConsumer` - 세션 참가자 간 실시간 통신
  - `DashboardConsumer` - 강사 대시보드 실시간 업데이트
  - `ProgressConsumer` - 학생 진도 실시간 모니터링

- **Kafka Integration**:
  - `ActivityLogProducer` - 비동기 로그 전송 (Singleton)
  - Kafka Consumer (Management Command) - 로그 DB 저장

#### Infrastructure

**PostgreSQL 15**
- 주 데이터베이스
- 모든 Django 모델 저장
- 트랜잭션 보장

**Redis 7**
- Django Channels의 Channel Layer (WebSocket 상태 관리)
- 캐시 백엔드 (세션, 쿼리 결과)
- Celery 브로커

**Kafka + Zookeeper**
- **Topics**:
  - `activity-logs` - UI 이벤트 스트리밍
  - `help-requests` - 도움 요청 이벤트
  - `mgpt-analysis` - M-GPT 분석 결과 (계획)
- **특징**:
  - 고처리량 (1000+ events/sec)
  - At-least-once delivery
  - Fallback to direct DB save

**Docker Compose**
- 모든 서비스 오케스트레이션
- 개발 및 프로덕션 환경 일관성

#### External Services

**OpenAI GPT API (M-GPT)**
- 녹화 분석 및 Task/Subtask 자동 생성
- 도움 요청 분석 및 맞춤형 힌트 생성
- 학습 패턴 분석 및 난이도 조정

### 통신 방식

| 연결 | 프로토콜 | 용도 |
|------|---------|------|
| Frontend ↔ Backend | REST API (HTTP/HTTPS) | 강의/세션 CRUD, 통계 조회 |
| Frontend ↔ Backend | WebSocket | 실시간 세션 제어, 진도 업데이트 |
| Android ↔ Backend | REST API (HTTP/HTTPS) | 세션 참가, 로그 배치 전송 |
| Android ↔ Backend | WebSocket | 실시간 단계 동기화, 도움말 수신 |
| Backend ↔ PostgreSQL | Django ORM (SQL) | 데이터 저장/조회 |
| Backend ↔ Redis | Redis Protocol | 캐시, Channel Layer |
| Backend ↔ Kafka | Kafka Protocol | 이벤트 스트리밍 |
| Backend ↔ OpenAI | HTTP API | GPT 분석 요청 |

---

## 2. 세션 제어 플로우

**파일**: [`session-control-flow.drawio`](./session-control-flow.drawio)

### 개요

강사가 실시간 강의 세션을 생성하고, 학생들이 참가하며, 강사가 단계별로 진행을 제어하는 전체 프로세스를 시퀀스 다이어그램으로 표현합니다.

### Phase 1: Session Creation

1. 강사가 `POST /api/lectures/{id}/sessions/create/`로 세션 생성
2. Backend가 **6자리 세션 코드** (예: `ABC123`) 생성
3. `LectureSession` 생성 (`status=WAITING`)
4. 강사가 학생들에게 코드 공유

### Phase 2: Students Join

1. 학생이 Android 앱에 세션 코드 입력
2. `POST /api/students/sessions/join/` 요청
3. Backend가 코드 검증 후 `SessionParticipant` 생성
4. WebSocket 연결 (`ws://server/ws/sessions/ABC123/`)
5. Redis Channel Layer에 연결 정보 저장
6. 다른 참가자들에게 `participant_joined` 브로드캐스트

### Phase 3: Session Control

**강사 제어 명령**:
- `POST /api/sessions/{id}/start/` → `status=IN_PROGRESS`, 첫 번째 Subtask 설정
- WebSocket을 통해 모든 학생에게 `step_changed` 브로드캐스트
- 학생 앱이 현재 단계 UI 업데이트

**학생 진행 보고**:
- 학생이 단계 완료 시 `step_complete` WebSocket 메시지 전송
- Backend가 `UserProgress` 업데이트
- **강사에게만** `progress_updated` 전송 (다른 학생에게는 비공개)

**추가 제어**:
- `POST /api/sessions/{id}/next-step/` - 다음 단계로 이동
- `POST /api/sessions/{id}/pause/` - 일시 정지 (`status=PAUSED`)
- `POST /api/sessions/{id}/resume/` - 재개 (`status=IN_PROGRESS`)
- `POST /api/sessions/{id}/end/` - 종료 (`status=ENDED`)

### 세션 상태 흐름

```
WAITING → IN_PROGRESS → PAUSED → IN_PROGRESS → ENDED
```

### 실시간 동기화

- **Channel Layer (Redis)**를 통해 모든 WebSocket 연결 간 메시지 브로드캐스트
- 네트워크 끊김 시 재연결 후 현재 상태 자동 복원
- Heartbeat 메커니즘으로 연결 상태 모니터링

---

## 3. Activity Log 파이프라인

**파일**: [`activity-log-pipeline.drawio`](./activity-log-pipeline.drawio)

### 개요

Android AccessibilityService에서 캡처한 UI 이벤트가 Kafka를 거쳐 PostgreSQL에 저장되고, 선택적으로 M-GPT 분석이 트리거되는 **이벤트 드리븐 아키텍처**를 보여줍니다.

### 전체 파이프라인 단계

#### Step 1: Event Capture (Android)

**AccessibilityService**가 다음 이벤트를 캡처:
- `CLICK` - 요소 클릭
- `LONG_CLICK` - 길게 누르기
- `SCROLL` - 스크롤
- `TEXT_INPUT` - 텍스트 입력
- `SCREEN_CHANGE` - 화면 전환

**추출 데이터**:
- Element bounds (위치 좌표)
- Package name (앱 식별자)
- View ID, Content description
- `is_clickable`, `is_editable`, `is_focused`
- Timestamp

**로컬 배치**: 5초마다 또는 10개 이벤트마다 배치 전송

#### Step 2: API Request

**Endpoint**: `POST /api/logs/batch/`

**Request Body**:
```json
{
  "logs": [
    {
      "event_type": "CLICK",
      "package_name": "com.google.android.youtube",
      "view_id": "search_button",
      "bounds": "100,200,300,250",
      "timestamp": "2025-01-19T12:34:56.789Z",
      "session_id": 42,
      ...
    },
    ...
  ]
}
```

**인증**: JWT Bearer Token

#### Step 3: Backend Processing

1. **Django ViewSet** (`ActivityLogViewSet`)가 요청 수신
2. 데이터 검증 및 사용자 식별 (JWT에서 추출)
3. **ActivityLogProducer** (Singleton)에게 전달
4. Kafka Producer가 비동기로 `activity-logs` 토픽에 전송
5. **즉시 `202 ACCEPTED` 응답** (처리를 기다리지 않음)

**Fallback**: Kafka 불가용 시 직접 DB 저장

#### Step 4: Message Queue (Kafka)

**Topic**: `activity-logs`
- **Partitions**: 3
- **Replication**: 1 (개발), 2+ (프로덕션)

**특징**:
- 고처리량 (1000+ events/sec)
- 내구성 (디스크 저장)
- 순서 보장 (파티션 내)
- 장애 복구

#### Step 5: Consumer Processing

**Management Command**:
```bash
python manage.py consume_activity_logs
```

**처리 로직**:
1. Kafka에서 메시지 Poll
2. JSON 역직렬화
3. `ActivityLog` 객체 생성
4. PostgreSQL에 배치 INSERT
5. 오프셋 커밋

**에러 처리**:
- 실패 시 재시도
- Dead Letter Queue
- 로깅 및 알림

#### Step 6: Data Persistence (PostgreSQL)

**Table**: `logs_activitylog`

**주요 필드**:
- `id`, `user_id`, `session_id`, `recording_session_id`
- `event_type`, `package_name`, `view_id`
- `bounds`, `content_description`
- `is_clickable`, `is_editable`, `is_focused`
- `timestamp`, `created_at`

#### Step 7: Optional M-GPT Analysis

**트리거 조건**:
- 도움 요청 감지
- 녹화 분석
- 이상 패턴 감지 (반복된 실패 등)

**분석 프로세스**:
1. 최근 ActivityLog 조회
2. OpenAI GPT API 전송
3. 인사이트 수신 (사용자 어려움, 추천 도움말, 학습 패턴)
4. `MGptAnalysis` 테이블에 저장

### 성능 지표

| Metric | Value |
|--------|-------|
| **처리량** | 1000+ events/sec |
| **배치 크기** | 10-50 events |
| **API 응답 시간** | < 100ms |
| **End-to-End 지연** | ~1-2초 |
| **신뢰성** | At-least-once delivery |

### Why Kafka?

- **디커플링**: 로깅과 API 응답 분리
- **트래픽 스파이크 처리**: 버퍼 역할
- **실시간 분석 가능**: 여러 Consumer 연결
- **수평 확장**: 파티션 추가로 처리량 증가

---

## 4. 녹화→Task 자동 생성

**파일**: [`recording-to-task-flow.drawio`](./recording-to-task-flow.drawio)

### 개요

강사의 앱 시연 녹화를 **하이브리드 알고리즘 (규칙 기반 + AI)**으로 분석하여 Lecture, Task, Subtask를 자동 생성하는 프로세스입니다.

### Phase 1: Recording

1. 강사가 `POST /api/sessions/recordings/` 시작
2. `RecordingSession` 생성 (`status=RECORDING`)
3. 강사가 앱 조작 (예: YouTube 열기 → 검색 → 재생)
4. AccessibilityService가 모든 액션 캡처 (`recording_session_id` 링크)
5. Kafka 파이프라인을 통해 DB 저장
6. 강사가 `POST /api/sessions/recordings/{id}/stop/` 종료
7. Backend가 `event_count`, `duration` 계산 및 `status=COMPLETED`

### Phase 2: Rule-based Segmentation

**알고리즘 목표**: 이벤트 시퀀스를 의미 있는 Task/Subtask로 그룹화

#### Rule 1: Task Boundaries (대작업 구분)

**분할 조건**:
- 앱 전환 (`package_name` 변경)
- 시간 간격 > 10초
- 다른 앱으로의 `SCREEN_CHANGE`

**예시**:
```
Task 1: "YouTube 열기"
Task 2: "동영상 검색"
Task 3: "동영상 재생"
```

#### Rule 2: Subtask Boundaries (세부 단계 구분)

**분할 조건**:
- 같은 앱 내 `SCREEN_CHANGE`
- 시간 간격 3-10초
- 명확한 액션 시퀀스 (예: `TEXT_INPUT` → `CLICK`)

**예시 (Task 2 내부)**:
```
Subtask 2.1: "검색창 클릭"
Subtask 2.2: "검색어 입력"
Subtask 2.3: "검색 버튼 클릭"
```

#### Rule 3: Event Type Classification

이벤트 타입에 따른 Subtask 분류:
- `CLICK` → "Click on {view_id}"
- `TEXT_INPUT` → "Enter text"
- `SCROLL` → "Scroll to find..."

**출력 (Preliminary Structure)**:
```json
[
  {
    "task_title": "YouTube 열기",
    "events": [...],
    "subtasks": [
      {
        "title": "YouTube 아이콘 클릭",
        "action_type": "CLICK",
        "events": [...]
      }
    ]
  },
  ...
]
```

### Phase 3: AI Refinement (OpenAI GPT)

#### GPT Prompt 구성

**System Message**:
```
"You are an expert at analyzing user interactions and creating
learning tasks for senior citizens learning mobile apps."
```

**User Message**:
```
"Analyze this sequence of mobile app events and refine the
task/subtask structure. Provide clear, beginner-friendly titles
and descriptions. Estimate difficulty level."

Input: [Preliminary structure + event details]
```

#### GPT가 수행하는 정제 작업

1. **사용자 친화적 제목**:
   - "Click search box" → "화면 상단의 검색 아이콘을 터치하세요"

2. **상세 설명 추가**:
   - "터치하세요" → "화면 오른쪽 위에 있는 돋보기 모양 아이콘을 터치하세요"

3. **난이도 추정**:
   - Task 1: `EASY` (1 step, 익숙한 앱)
   - Task 2: `MEDIUM` (3 steps, 타이핑 필요)

4. **액션 타입 검증**:
   - 잘못 분류된 이벤트 수정

5. **중복 단계 병합**:
   - "Scroll down" + "Scroll down" → "동영상을 찾을 때까지 스크롤하세요"

6. **도움말 힌트 추가**:
   - "Look for the magnifying glass icon"

**GPT 응답 형식**:
```json
{
  "lecture_title": "YouTube에서 동영상 검색하는 방법",
  "tasks": [
    {
      "title": "YouTube 앱 실행하기",
      "difficulty": "EASY",
      "subtasks": [
        {
          "title": "YouTube 아이콘 터치",
          "description": "홈 화면에서 빨간색 재생 버튼 모양의 YouTube 아이콘을 찾아서 터치하세요.",
          "action_type": "CLICK",
          "hint": "아이콘은 보통 첫 페이지 또는 앱 목록에 있습니다."
        }
      ]
    }
  ]
}
```

### Phase 4: Create Lecture Structure

#### Database 객체 생성

**1. Lecture**:
```python
Lecture.objects.create(
    title=gpt_response["lecture_title"],
    description=gpt_response.get("description"),
    instructor=current_user,
    created_from_recording=recording_session
)
```

**2. Tasks**:
```python
for task_data in gpt_response["tasks"]:
    Task.objects.create(
        lecture=lecture,
        title=task_data["title"],
        description=task_data.get("description"),
        order=index,
        difficulty=task_data["difficulty"]
    )
```

**3. Subtasks**:
```python
for subtask_data in task["subtasks"]:
    Subtask.objects.create(
        task=task,
        title=subtask_data["title"],
        description=subtask_data["description"],
        action_type=subtask_data["action_type"],
        order=index,
        hint=subtask_data.get("hint"),
        metadata={"event_ids": [...]}  # 원본 이벤트 링크
    )
```

**4. RecordingSession 업데이트**:
```python
recording_session.generated_lecture = lecture
recording_session.save()
```

**5. 강사 알림**:
```
"녹화가 처리되었습니다!
새로운 강의가 {task_count}개의 과제와 {subtask_count}개의 단계로 생성되었습니다."

[강의 보기] [과제 수정]
```

### 하이브리드 접근법의 장점

| 규칙 기반 (Phase 2) | AI 기반 (Phase 3) |
|-------------------|------------------|
| ✓ 빠르고 결정적 | ✓ 의미론적 이해 |
| ✓ API 비용 없음 | ✓ 사용자 친화적 언어 |
| ✓ 오프라인 작동 | ✓ 문맥적 힌트 |
| ✓ 명확한 경우 처리 (앱 전환, 시간 간격) | ✓ 난이도 추정 |
|  | ✓ 오류 수정 (병합, 재명명, 재정렬) |

### 비용 추정

- **GPT-4 API 비용**: $0.01 - $0.05 per recording (50-200 events)
- **자동화율**: 80%+ (강사가 검토 후 게시)

---

## 5. 도움 요청 플로우

**파일**: [`help-request-flow.drawio`](./help-request-flow.drawio)

### 개요

학생이 도움을 요청하면 M-GPT가 ActivityLog를 분석하여 맞춤형 힌트를 제공하는 **AI 지원 학습 지원** 시스템입니다.

### Scenario 1: Manual Help Request

#### 1-7단계: 도움 요청 생성

1. 학생이 "도움 요청" 버튼 클릭
2. WebSocket으로 `{type: "request_help", subtask_id: 42, message: "..."}` 전송
3. `SessionConsumer`가 메시지 처리
4. Backend가 `HelpRequest` 생성 (`status=PENDING`, `type=MANUAL`)
5. PostgreSQL에 저장
6. **강사에게만** `help_requested` WebSocket 메시지 전송
7. M-GPT 분석 트리거 (비동기 백그라운드 작업)

#### 8-12단계: M-GPT Analysis

**8. Context Data 수집**:
- 최근 20개 ActivityLog
- 현재 Subtask 정보
- 이전 도움 요청 이력
- 사용자 `digital_level`

**9. GPT Prompt 구성**:

```
System: "You are a patient digital literacy instructor
helping senior citizens learn mobile apps."

User: "The student is trying to: {subtask.description}

Their recent actions:
{activity_logs}

They requested help saying: {help_message}

Their skill level: {digital_level}

Analyze:
1. What is the student struggling with?
2. Provide a simple, step-by-step hint
3. Suggest difficulty adjustment if needed"
```

**10. OpenAI API 호출**:
```
POST https://api.openai.com/v1/chat/completions
{
  "model": "gpt-4",
  "messages": [...],
  "temperature": 0.3,
  "response_format": {"type": "json_object"}
}
```

**11. GPT 응답 예시**:
```json
{
  "diagnosis": {
    "issue": "Student clicked the wrong icon",
    "confidence": 0.85
  },
  "hint": {
    "text": "Look for the magnifying glass icon at the top-right corner. It's usually next to your profile picture.",
    "voice_text": "검색 아이콘은 화면 오른쪽 위에 있는 돋보기 모양입니다."
  },
  "difficulty_adjustment": {
    "recommended": "EASY",
    "reason": "Multiple failed attempts"
  },
  "overlay_instructions": {
    "highlight_bounds": "800,100,900,150",
    "arrow_direction": "top-right"
  }
}
```

**12. MGptAnalysis 저장**: PostgreSQL에 분석 결과 저장

#### 13-16단계: 도움말 전달

13. `HelpResponse` 생성 (`response_type=TEXT`, `content=...`)
14. PostgreSQL에 저장
15. WebSocket으로 학생에게 `help_response` 전송
16. 학생 앱이 도움말 UI 표시:
    - 텍스트 힌트 다이얼로그
    - 음성 재생 (TTS)
    - 타겟 요소 오버레이 하이라이트

### Scenario 2: Auto-detected Help (계획)

**자동 트리거 조건**:
1. **반복된 실패**: 같은 액션 3회 이상
2. **장시간 비활동**: 30초 이상 액션 없음
3. **Off-task 행동**: 다른 앱으로 전환
4. **오류 패턴**: 잘못된 요소 클릭

**프로세스**:
- Backend가 ActivityLog 분석으로 감지
- `type=AUTO`인 `HelpRequest` 자동 생성
- M-GPT 플로우 동일하게 진행

### Scenario 3: Instructor Manual Response

**강사 옵션**:
1. **M-GPT 제안 수락** → 학생에게 자동 전송
2. **M-GPT 힌트 편집** → 수정 후 전송
3. **직접 메시지 작성** → M-GPT 무시
4. **영상 통화** → 실시간 도움 (향후 기능)
5. **나중에 도움** → 상태만 업데이트

### Help Response Types

| Type | Description | Implementation |
|------|-------------|----------------|
| **TEXT** | 텍스트 힌트 다이얼로그 | 기본 구현 |
| **VOICE** | TTS 음성 재생 (한국어) | Android TTS API |
| **OVERLAY** | 타겟 요소 하이라이트 + 화살표 | AccessibilityService Overlay |
| **VIDEO** | 미리 녹화된 데모 영상 | Video player |
| **VIDEO_CALL** | 실시간 강사 통화 | WebRTC (향후 기능) |

---

## 6. 기술 스택

### Backend

| Category | Technology | Version |
|----------|-----------|---------|
| **Framework** | Django | 4.2+ |
| **ASGI Server** | Daphne | Latest |
| **API** | Django REST Framework | 3.14+ |
| **Database** | PostgreSQL | 15 |
| **Cache** | Redis | 7 |
| **Message Queue** | Apache Kafka | 3.5+ |
| **WebSocket** | Django Channels | 4.0+ |
| **Authentication** | JWT (djangorestframework-simplejwt) | Latest |
| **Task Queue** | Celery + Beat | 5.3+ |
| **CORS** | django-cors-headers | Latest |

### Frontend

| Category | Technology | Version |
|----------|-----------|---------|
| **Framework** | React | 18.3.1 |
| **Build Tool** | Vite | 6.3.5 |
| **Language** | TypeScript | 5.x |
| **UI Library** | Radix UI | Latest |
| **Styling** | Tailwind CSS | 3.x |
| **Routing** | React Router DOM | 6.x |
| **HTTP Client** | Axios | Latest |
| **WebSocket** | Native WebSocket API | - |
| **Forms** | React Hook Form | Latest |
| **Charts** | Recharts | Latest |

### Android

| Category | Technology | Version |
|----------|-----------|---------|
| **Language** | Kotlin | 1.9+ |
| **UI** | Jetpack Compose | Latest |
| **Architecture** | MVVM + Clean | - |
| **DI** | Hilt | 2.48+ |
| **Networking** | Retrofit + OkHttp | 2.9+ |
| **WebSocket** | Scarlet | 0.1.12 |
| **Async** | Coroutines + Flow | 1.7+ |
| **Local Storage** | DataStore | 1.0+ |
| **Min SDK** | API 30 (Android 11.0) | - |
| **Target SDK** | API 34 | - |

### Infrastructure

| Category | Technology | Version |
|----------|-----------|---------|
| **Containerization** | Docker + Docker Compose | Latest |
| **Orchestration** | Docker Compose (dev), K8s (prod plan) | - |
| **CI/CD** | GitHub Actions (planned) | - |

### External APIs

| Service | Purpose | Pricing |
|---------|---------|---------|
| **OpenAI GPT-4** | M-GPT 분석 (녹화, 도움 요청) | ~$0.01-0.05/request |

---

## 7. 확장성 고려사항

### 수평 확장 전략

#### Backend

**Django Application**:
- Stateless 설계 (세션 상태는 Redis/DB에 저장)
- Load Balancer 뒤에 여러 인스턴스 배포
- Auto-scaling 기준: CPU > 70%, Memory > 80%

**Kafka Consumer**:
- Consumer Group으로 여러 인스턴스 실행
- 파티션 수 = 최대 병렬 Consumer 수
- 처리량 증가 시 파티션 추가

**Celery Workers**:
- Queue별 전용 Worker 풀
- 우선순위 Queue (High/Medium/Low)

#### Database

**PostgreSQL**:
- Read Replica 추가 (읽기 부하 분산)
- Connection Pooling (PgBouncer)
- Partitioning (ActivityLog 테이블 - 월별)
- Indexing 최적화

**Redis**:
- Redis Cluster (샤딩)
- Sentinel (고가용성)

**Kafka**:
- 파티션 증가로 처리량 확대
- Broker 추가로 리더 분산
- Replication Factor 증가 (내결함성)

### 성능 최적화

**API**:
- 쿼리 최적화 (`select_related`, `prefetch_related`)
- 응답 캐싱 (Redis)
- 페이지네이션 (Cursor-based)
- 압축 (gzip)

**WebSocket**:
- Connection Pooling
- Heartbeat 간격 조정
- 메시지 배치 전송

**ActivityLog**:
- 배치 삽입 (bulk_create)
- 비동기 처리 (Kafka)
- 아카이빙 정책 (6개월 이상 cold storage)

### 모니터링

**필수 메트릭**:
- API 응답 시간 (p50, p95, p99)
- WebSocket 연결 수
- Kafka Consumer Lag
- Database Connection Pool Usage
- Error Rate (5xx)

**도구** (계획):
- Prometheus + Grafana
- Sentry (에러 추적)
- ELK Stack (로그 분석)

---

## 8. OpenAI API 통합 가이드

### API Key 설정

**환경 변수** (`backend/.env`):
```bash
OPENAI_API_KEY=sk-...
OPENAI_MODEL=gpt-4
OPENAI_TEMPERATURE=0.3
OPENAI_MAX_TOKENS=1500
```

### 코드 구현 예시

**M-GPT Service** (`backend/apps/help/services/mgpt_service.py`):

```python
import openai
from django.conf import settings

openai.api_key = settings.OPENAI_API_KEY

def analyze_help_request(help_request):
    """Analyze help request using GPT-4"""

    # 1. Fetch context data
    recent_logs = help_request.user.activitylog_set.filter(
        session=help_request.session
    ).order_by('-timestamp')[:20]

    # 2. Build prompt
    system_msg = (
        "You are a patient digital literacy instructor "
        "helping senior citizens learn mobile apps."
    )

    user_msg = f"""
    The student is trying to: {help_request.subtask.description}

    Their recent actions:
    {format_logs(recent_logs)}

    They requested help saying: {help_request.message}

    Their skill level: {help_request.user.digital_level}

    Analyze:
    1. What is the student struggling with?
    2. Provide a simple, step-by-step hint
    3. Suggest difficulty adjustment if needed

    Respond in JSON format.
    """

    # 3. Call OpenAI API
    response = openai.ChatCompletion.create(
        model=settings.OPENAI_MODEL,
        messages=[
            {"role": "system", "content": system_msg},
            {"role": "user", "content": user_msg}
        ],
        temperature=settings.OPENAI_TEMPERATURE,
        max_tokens=settings.OPENAI_MAX_TOKENS,
        response_format={"type": "json_object"}
    )

    # 4. Parse response
    result = json.loads(response.choices[0].message.content)

    # 5. Save MGptAnalysis
    analysis = MGptAnalysis.objects.create(
        help_request=help_request,
        diagnosis=result.get("diagnosis"),
        hint=result.get("hint"),
        difficulty_adjustment=result.get("difficulty_adjustment"),
        raw_response=result
    )

    return analysis
```

### Recording 분석

**녹화→Task 생성** (`backend/apps/sessions/services/recording_analyzer.py`):

```python
def generate_tasks_from_recording(recording_session):
    """Generate Tasks/Subtasks from recording using hybrid algorithm"""

    # Phase 1: Fetch events
    events = recording_session.activitylog_set.order_by('timestamp')

    # Phase 2: Rule-based segmentation
    preliminary_structure = segment_by_rules(events)

    # Phase 3: AI refinement
    gpt_response = refine_with_gpt(preliminary_structure, events)

    # Phase 4: Create database objects
    lecture = Lecture.objects.create(
        title=gpt_response["lecture_title"],
        instructor=recording_session.instructor,
        created_from_recording=recording_session
    )

    for task_data in gpt_response["tasks"]:
        task = Task.objects.create(
            lecture=lecture,
            title=task_data["title"],
            difficulty=task_data["difficulty"]
        )

        for subtask_data in task_data["subtasks"]:
            Subtask.objects.create(
                task=task,
                title=subtask_data["title"],
                description=subtask_data["description"],
                action_type=subtask_data["action_type"],
                hint=subtask_data.get("hint")
            )

    recording_session.generated_lecture = lecture
    recording_session.save()

    return lecture

def refine_with_gpt(preliminary_structure, events):
    """Use GPT to refine task structure"""

    system_msg = (
        "You are an expert at analyzing user interactions and "
        "creating learning tasks for senior citizens."
    )

    user_msg = f"""
    Analyze this sequence of mobile app events and refine the
    task/subtask structure. Provide clear, beginner-friendly
    titles and descriptions. Estimate difficulty level.

    Preliminary structure:
    {json.dumps(preliminary_structure, indent=2)}

    Event details:
    {format_events_for_gpt(events)}

    Respond in this JSON format:
    {{
      "lecture_title": "...",
      "description": "...",
      "tasks": [
        {{
          "title": "...",
          "difficulty": "EASY|MEDIUM|HARD",
          "subtasks": [
            {{
              "title": "...",
              "description": "...",
              "action_type": "...",
              "hint": "..."
            }}
          ]
        }}
      ]
    }}
    """

    response = openai.ChatCompletion.create(
        model="gpt-4",
        messages=[
            {"role": "system", "content": system_msg},
            {"role": "user", "content": user_msg}
        ],
        temperature=0.3,
        response_format={"type": "json_object"}
    )

    return json.loads(response.choices[0].message.content)
```

### 비용 최적화 팁

1. **캐싱**: 유사한 요청 결과 재사용
2. **배치 처리**: 여러 분석을 하나의 API 호출로 결합
3. **Temperature 조정**: 0.2-0.4로 낮춰서 일관성 ↑, 비용 ↓
4. **Max Tokens 제한**: 불필요하게 긴 응답 방지
5. **모델 선택**: GPT-3.5-turbo로 일부 작업 대체 (비용 1/10)

### 에러 처리

```python
try:
    response = openai.ChatCompletion.create(...)
except openai.error.RateLimitError:
    # Rate limit exceeded - retry with exponential backoff
    time.sleep(2 ** retry_count)
except openai.error.APIError as e:
    # API error - log and fallback
    logger.error(f"OpenAI API error: {e}")
    return fallback_response()
except Exception as e:
    # Unexpected error
    logger.exception(f"Unexpected error in GPT analysis: {e}")
    raise
```

---

## 📚 추가 참고 자료

- **Django Channels 문서**: https://channels.readthedocs.io/
- **Kafka Python Client**: https://kafka-python.readthedocs.io/
- **OpenAI API Reference**: https://platform.openai.com/docs/api-reference
- **Jetpack Compose**: https://developer.android.com/jetpack/compose
- **AccessibilityService Guide**: https://developer.android.com/guide/topics/ui/accessibility/service

---

## 🤝 기여 가이드

아키텍처 다이어그램을 수정하려면:

1. Draw.io에서 파일 열기: https://app.diagrams.net
2. 또는 VS Code에서 "Draw.io Integration" 확장 설치
3. `.drawio` 파일 편집
4. 변경 사항을 README에도 반영
5. Pull Request 생성

---

**문서 작성일**: 2025-01-19
**최종 업데이트**: 2025-01-19
**작성자**: Claude Code
