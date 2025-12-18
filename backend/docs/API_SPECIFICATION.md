# MobileGPT Backend API Specification

> **Version**: 1.0.0
> **Base URL**: `http://localhost:8000/api`
> **인증**: JWT Bearer Token
> **Last Updated**: 2024-12-07

---

## 목차

1. [인증 (Authentication)](#1-인증-authentication)
2. [강의 (Lectures)](#2-강의-lectures)
3. [과제 (Tasks)](#3-과제-tasks)
4. [세션 (Sessions)](#4-세션-sessions)
5. [진행상태 (Progress)](#5-진행상태-progress)
6. [활동 로그 (Logs)](#6-활동-로그-logs)
7. [도움 요청 (Help)](#7-도움-요청-help)
8. [대시보드 (Dashboard)](#8-대시보드-dashboard)
9. [학생 (Students)](#9-학생-students)
10. [헬스체크 (Health)](#10-헬스체크-health)
11. [WebSocket](#11-websocket)
12. [에러 코드](#12-에러-코드)

---

## 공통 사항

### 인증 헤더
```
Authorization: Bearer <access_token>
```

### 응답 형식
```json
{
  "data": { ... },
  "message": "성공 메시지",
  "status": "success"
}
```

### 에러 응답 형식
```json
{
  "error": "에러 메시지",
  "code": "ERROR_CODE",
  "detail": { ... }
}
```

---

## 1. 인증 (Authentication)

**Base Path**: `/api/auth`

### 1.1 회원가입
```
POST /api/auth/register/
```

**Request Body**:
```json
{
  "email": "user@example.com",
  "password": "securePassword123",
  "password_confirm": "securePassword123",
  "name": "홍길동",
  "role": "STUDENT",
  "digital_level": "BEGINNER",
  "phone": "010-1234-5678",
  "age": 65
}
```

**Response** (201 Created):
```json
{
  "user": {
    "id": 1,
    "email": "user@example.com",
    "name": "홍길동",
    "role": "STUDENT",
    "digital_level": "BEGINNER"
  },
  "access": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refresh": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**에러**:
- `400`: 이메일 중복, 비밀번호 불일치
- `422`: 유효성 검증 실패

---

### 1.2 로그인
```
POST /api/auth/login/
```

**Request Body**:
```json
{
  "email": "user@example.com",
  "password": "securePassword123"
}
```

**Response** (200 OK):
```json
{
  "access": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refresh": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

---

### 1.3 토큰 갱신
```
POST /api/auth/refresh/
```

**Request Body**:
```json
{
  "refresh": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response** (200 OK):
```json
{
  "access": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

---

### 1.4 로그아웃
```
POST /api/auth/logout/
```
**인증 필요**: Yes

**Request Body** (선택):
```json
{
  "refresh": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response** (200 OK):
```json
{
  "detail": "로그아웃되었습니다."
}
```

---

### 1.5 내 정보 조회/수정
```
GET /api/auth/me/
PUT /api/auth/me/
PATCH /api/auth/me/
```
**인증 필요**: Yes

**Response** (GET):
```json
{
  "id": 1,
  "email": "user@example.com",
  "name": "홍길동",
  "phone": "010-1234-5678",
  "age": 65,
  "role": "STUDENT",
  "digital_level": "BEGINNER",
  "created_at": "2024-01-01T00:00:00Z",
  "last_login_at": "2024-12-07T10:00:00Z"
}
```

---

## 2. 강의 (Lectures)

**Base Path**: `/api/lectures`

### 2.1 강의 목록
```
GET /api/lectures/
```
**인증 필요**: Yes

**Query Parameters**:
- `page`: 페이지 번호 (default: 1)
- `page_size`: 페이지당 항목 수 (default: 20)
- `is_active`: 활성 강의만 필터링 (true/false)

**Response**:
```json
{
  "count": 50,
  "next": "http://localhost:8000/api/lectures/?page=2",
  "previous": null,
  "results": [
    {
      "id": 1,
      "title": "스마트폰 기초 사용법",
      "description": "스마트폰 초보자를 위한 기초 강의",
      "thumbnail_url": "https://example.com/thumb.jpg",
      "instructor": {
        "id": 1,
        "name": "김강사",
        "email": "instructor@example.com"
      },
      "is_active": true,
      "created_at": "2024-01-01T00:00:00Z",
      "task_count": 5
    }
  ]
}
```

---

### 2.2 강의 생성
```
POST /api/lectures/
```
**인증 필요**: Yes
**권한**: INSTRUCTOR만

**Request Body**:
```json
{
  "title": "스마트폰 기초 사용법",
  "description": "스마트폰 초보자를 위한 기초 강의",
  "thumbnail_url": "https://example.com/thumb.jpg",
  "is_active": true
}
```

---

### 2.3 강의 상세
```
GET /api/lectures/{id}/
PUT /api/lectures/{id}/
PATCH /api/lectures/{id}/
DELETE /api/lectures/{id}/
```
**인증 필요**: Yes
**수정/삭제 권한**: 강의 소유자만

---

### 2.4 수강 신청
```
POST /api/lectures/{id}/enroll/
```
**인증 필요**: Yes

**Response** (201 Created):
```json
{
  "message": "수강 신청이 완료되었습니다.",
  "enrollment": {
    "id": 1,
    "lecture_id": 1,
    "enrolled_at": "2024-12-07T10:00:00Z"
  }
}
```

---

### 2.5 강의별 과제 목록
```
GET /api/lectures/{lecture_id}/tasks/
```

---

### 2.6 과제 생성
```
POST /api/lectures/{lecture_id}/tasks/create/
```

**Request Body**:
```json
{
  "title": "유튜브 앱 사용하기",
  "description": "유튜브 앱을 설치하고 동영상을 검색하는 방법",
  "order_index": 1
}
```

---

### 2.7 세션 생성
```
POST /api/lectures/{lecture_id}/sessions/create/
```

**Request Body**:
```json
{
  "title": "12월 7일 오전 강의"
}
```

**Response**:
```json
{
  "id": 1,
  "title": "12월 7일 오전 강의",
  "session_code": "ABC123",
  "qr_code_url": "data:image/png;base64,...",
  "status": "WAITING",
  "created_at": "2024-12-07T09:00:00Z"
}
```

---

## 3. 과제 (Tasks)

**Base Path**: `/api/tasks`

### 3.1 과제 상세
```
GET /api/tasks/{id}/
PUT /api/tasks/{id}/
PATCH /api/tasks/{id}/
DELETE /api/tasks/{id}/
```

---

### 3.2 세부단계 생성
```
POST /api/tasks/{task_id}/subtasks/create/
```

**Request Body**:
```json
{
  "title": "유튜브 앱 열기",
  "order_index": 1,
  "target_action": "CLICK",
  "target_package": "com.google.android.youtube",
  "target_class": "android.widget.ImageView",
  "ui_hint": "빨간색 재생 버튼 아이콘을 찾아 터치하세요",
  "guide_text": "유튜브 앱을 열어주세요",
  "voice_guide_text": "유튜브 앱을 찾아서 터치해주세요"
}
```

**target_action 값**:
- `CLICK`: 클릭
- `LONG_CLICK`: 길게 누르기
- `SCROLL`: 스크롤
- `INPUT`: 텍스트 입력
- `NAVIGATE`: 네비게이션

---

### 3.3 세부단계 상세
```
GET /api/tasks/subtasks/{id}/
PUT /api/tasks/subtasks/{id}/
PATCH /api/tasks/subtasks/{id}/
DELETE /api/tasks/subtasks/{id}/
```

---

## 4. 세션 (Sessions)

**Base Path**: `/api/sessions`

### 4.1 세션 코드로 조회
```
GET /api/sessions/{session_code}/
```

---

### 4.2 익명 세션 참가
```
POST /api/sessions/join/
```
**인증 필요**: No

**Request Body**:
```json
{
  "session_code": "ABC123",
  "device_id": "android-device-uuid-12345",
  "name": "홍길동"
}
```

**Response**:
```json
{
  "participant_id": 1,
  "session": {
    "id": 1,
    "title": "12월 7일 오전 강의",
    "status": "WAITING"
  },
  "message": "세션에 참가했습니다."
}
```

---

### 4.3 학생 세션 입장 (인증)
```
POST /api/sessions/{session_id}/join/
```
**인증 필요**: Yes

---

### 4.4 내 활성 세션
```
GET /api/sessions/my-active/
```
**인증 필요**: Yes

---

### 4.5 강사 활성 세션
```
GET /api/sessions/instructor-active/
```
**인증 필요**: Yes

---

### 4.6 참가자 목록
```
GET /api/sessions/{session_id}/participants/
```

**Response**:
```json
{
  "participants": [
    {
      "id": 1,
      "user": { "id": 1, "name": "홍길동" },
      "device_id": null,
      "display_name": "홍길동",
      "status": "ACTIVE",
      "joined_at": "2024-12-07T09:30:00Z",
      "completed_subtasks": [1, 2, 3]
    },
    {
      "id": 2,
      "user": null,
      "device_id": "android-device-uuid-12345",
      "display_name": "김철수",
      "status": "ACTIVE",
      "joined_at": "2024-12-07T09:31:00Z",
      "completed_subtasks": [1, 2]
    }
  ]
}
```

---

### 4.7 세션 제어 (강사 전용)

#### 세션 시작
```
POST /api/sessions/{session_id}/start/
```

#### 다음 단계
```
POST /api/sessions/{session_id}/next-step/
```

**Response**:
```json
{
  "message": "다음 단계로 진행합니다.",
  "current_subtask": {
    "id": 2,
    "title": "검색창 터치하기",
    "order_index": 2,
    "target_action": "CLICK",
    "guide_text": "화면 상단의 검색창을 터치하세요"
  }
}
```

#### 일시정지
```
POST /api/sessions/{session_id}/pause/
```

#### 재개
```
POST /api/sessions/{session_id}/resume/
```

#### 종료
```
POST /api/sessions/{session_id}/end/
```

#### 브로드캐스트
```
POST /api/sessions/{session_id}/broadcast/
```

**Request Body**:
```json
{
  "message": "잠시 화면을 주목해주세요."
}
```

---

### 4.8 단계 완료 보고
```
POST /api/sessions/{session_id}/report-completion/
```
**인증 필요**: No (device_id로 식별)

**Request Body**:
```json
{
  "device_id": "android-device-uuid-12345",
  "subtask_id": 3
}
```

**Response**:
```json
{
  "message": "단계 완료가 기록되었습니다.",
  "completed_subtasks": [1, 2, 3],
  "total_completed": 3
}
```

---

### 4.9 완료 상태 조회
```
GET /api/sessions/{session_id}/completion-status/
```

**Response**:
```json
{
  "total_subtasks": 10,
  "participants": [
    {
      "id": 1,
      "name": "홍길동",
      "completed_count": 5,
      "completion_rate": 50.0,
      "status": "ACTIVE"
    }
  ]
}
```

---

### 4.10 스크린샷

#### 업로드
```
POST /api/sessions/{session_id}/screenshots/upload/
```
**인증 필요**: No

**Request Body**:
```json
{
  "device_id": "android-device-uuid-12345",
  "image": "data:image/png;base64,iVBORw0KGgoAAAANS...",
  "captured_at": "2024-12-07T10:30:00Z"
}
```

#### 목록 조회
```
GET /api/sessions/{session_id}/screenshots/
GET /api/sessions/{session_id}/screenshots/{participant_id}/
GET /api/sessions/{session_id}/screenshots/by-device/{device_id}/
```

---

### 4.11 녹화 (Recording)

#### 녹화 시작
```
POST /api/sessions/recordings/
```

**Request Body**:
```json
{
  "title": "유튜브 사용법 녹화",
  "description": "유튜브 앱 사용 시연 녹화"
}
```

#### 녹화 종료
```
POST /api/sessions/recordings/{id}/stop/
```

#### 이벤트 일괄 저장
```
POST /api/sessions/recordings/{id}/save-events-batch/
```

**Request Body**:
```json
{
  "events": [
    {
      "event_type": "VIEW_CLICKED",
      "timestamp": "2024-12-07T10:30:00Z",
      "event_data": {
        "class_name": "android.widget.Button",
        "text": "검색"
      }
    }
  ]
}
```

#### 이벤트 조회
```
GET /api/sessions/recordings/{id}/events/
```

---

## 5. 진행상태 (Progress)

**Base Path**: `/api/progress`

### 5.1 내 진행상태
```
GET /api/progress/me/
```

**Response**:
```json
{
  "lectures": [
    {
      "lecture_id": 1,
      "lecture_title": "스마트폰 기초",
      "total_subtasks": 20,
      "completed_subtasks": 15,
      "progress_rate": 75.0,
      "status": "IN_PROGRESS"
    }
  ]
}
```

---

### 5.2 진행상태 업데이트
```
POST /api/progress/update/
```

**Request Body**:
```json
{
  "subtask_id": 5,
  "session_id": 1,
  "status": "COMPLETED"
}
```

**status 값**:
- `NOT_STARTED`: 시작 안함
- `IN_PROGRESS`: 진행 중
- `COMPLETED`: 완료
- `HELP_NEEDED`: 도움 필요

---

### 5.3 학생별 진행상태 (강사용)
```
GET /api/progress/users/{user_id}/lectures/{lecture_id}/
```

---

## 6. 활동 로그 (Logs)

**Base Path**: `/api/logs`

### 6.1 활동 로그 전송
```
POST /api/logs/activity/
```
**인증 필요**: Yes

**Request Body**:
```json
{
  "event_type": "VIEW_CLICKED",
  "session_id": 1,
  "subtask_id": 3,
  "timestamp": "2024-12-07T10:30:00Z",
  "event_data": {
    "x": 540,
    "y": 960,
    "text": "검색"
  },
  "screen_info": {
    "package_name": "com.google.android.youtube",
    "activity_name": "MainActivity"
  },
  "node_info": {
    "class_name": "android.widget.Button",
    "resource_id": "com.google.android.youtube:id/search_button",
    "bounds": "[0,0][1080,1920]",
    "is_clickable": true
  }
}
```

**event_type 값**:
- `CLICK`: 터치/클릭
- `LONG_CLICK`: 길게 누르기
- `SCROLL`: 스크롤
- `TEXT_INPUT`: 텍스트 입력
- `SCREEN_CHANGE`: 화면 변경
- `FOCUS`: 포커스 변경
- `SELECTION`: 선택 변경
- `WINDOW_CHANGE`: 윈도우 변경
- `VIEW_CLICKED`: 뷰 클릭
- `VIEW_TEXT_CHANGED`: 텍스트 변경
- `NOTIFICATION`: 알림
- `GESTURE`: 제스처
- `ANNOUNCEMENT`: 접근성 알림

**Response** (202 Accepted):
```json
{
  "message": "로그가 성공적으로 전송되었습니다."
}
```

---

### 6.2 익명 활동 로그
```
POST /api/logs/activity/anonymous/
```
**인증 필요**: No

**Request Body**:
```json
{
  "device_id": "android-device-uuid-12345",
  "event_type": "VIEW_CLICKED",
  ...
}
```

---

### 6.3 배치 로그 전송
```
POST /api/logs/batch/
```

**Request Body**:
```json
{
  "logs": [
    { "event_type": "CLICK", ... },
    { "event_type": "SCROLL", ... }
  ]
}
```

---

## 7. 도움 요청 (Help)

**Base Path**: `/api/help`

### 7.1 도움 요청 생성
```
POST /api/help/request/
```

**Request Body**:
```json
{
  "session_id": 1,
  "subtask_id": 5,
  "request_type": "MANUAL",
  "context_data": {
    "current_screen": "검색 화면",
    "last_action": "검색창 터치",
    "error_message": null
  }
}
```

**request_type 값**:
- `MANUAL`: 수동 요청 (학생이 직접)
- `AUTO`: 자동 감지 (시스템)

**Response** (201 Created):
```json
{
  "id": 1,
  "status": "PENDING",
  "created_at": "2024-12-07T10:35:00Z"
}
```

---

### 7.2 도움 요청 상태 조회
```
GET /api/help/request/{id}/
```

**Response**:
```json
{
  "id": 1,
  "status": "ANALYZING",
  "request_type": "MANUAL",
  "context_data": { ... },
  "mgpt_analysis": {
    "problem_diagnosis": "검색창을 찾지 못하고 있습니다.",
    "recommended_help": "화면 상단의 돋보기 아이콘을 찾아주세요.",
    "confidence_score": 0.85
  },
  "created_at": "2024-12-07T10:35:00Z"
}
```

---

### 7.3 도움 요청 해결 (강사)
```
POST /api/help/request/{id}/resolve/
```

**Request Body**:
```json
{
  "resolution_note": "화면 공유로 직접 안내함"
}
```

---

### 7.4 피드백 제출
```
POST /api/help/feedback/
```

**Request Body**:
```json
{
  "help_request_id": 1,
  "rating": 5,
  "feedback_text": "도움이 많이 되었습니다."
}
```

---

## 8. 대시보드 (Dashboard)

**Base Path**: `/api/dashboard`

### 8.1 강의별 학생 목록
```
GET /api/dashboard/lectures/{lecture_id}/students/
```

**Response**:
```json
{
  "students": [
    {
      "user_id": 1,
      "name": "홍길동",
      "progress_rate": 75.0,
      "last_active": "2024-12-07T10:30:00Z",
      "help_request_count": 2
    }
  ]
}
```

---

### 8.2 대기중 도움 요청
```
GET /api/dashboard/help-requests/pending/
```

**Response**:
```json
{
  "help_requests": [
    {
      "id": 1,
      "student_name": "홍길동",
      "subtask_title": "유튜브 검색하기",
      "created_at": "2024-12-07T10:35:00Z",
      "status": "PENDING"
    }
  ]
}
```

---

### 8.3 강의 통계
```
GET /api/dashboard/statistics/lecture/{lecture_id}/
```

**Response**:
```json
{
  "total_students": 20,
  "active_students": 15,
  "average_progress": 68.5,
  "difficult_subtasks": [
    {
      "subtask_id": 5,
      "title": "검색어 입력하기",
      "help_request_count": 8
    }
  ],
  "completion_distribution": {
    "completed": 5,
    "in_progress": 10,
    "not_started": 5
  }
}
```

---

### 8.4 세션 진도 통계
```
GET /api/dashboard/sessions/{session_id}/progress-stats/
```

---

## 9. 학생 (Students)

**Base Path**: `/api/students`

### 9.1 강의 목록
```
GET /api/students/lectures/
```

---

### 9.2 내 수강 강의
```
GET /api/students/lectures/my_lectures/
```

---

### 9.3 수강 신청/취소
```
POST /api/students/lectures/{id}/enroll/
POST /api/students/lectures/{id}/unenroll/
```

---

### 9.4 세션 참가
```
POST /api/students/sessions/join/
```

**Request Body**:
```json
{
  "session_code": "ABC123"
}
```

---

### 9.5 내 세션/활성 세션
```
GET /api/students/sessions/my_sessions/
GET /api/students/sessions/active_sessions/
```

---

### 9.6 세션 나가기
```
POST /api/students/sessions/{id}/leave/
```

---

## 10. 헬스체크 (Health)

**Base Path**: `/api/health`

### 10.1 기본 헬스체크
```
GET /api/health/
```
**인증 필요**: No

**Response**:
```json
{
  "status": "ok",
  "timestamp": "2024-12-07T10:00:00Z"
}
```

---

### 10.2 상세 헬스체크
```
GET /api/health/detailed/
```

**Response**:
```json
{
  "status": "ok",
  "database": "connected",
  "cache": "connected",
  "timestamp": "2024-12-07T10:00:00Z"
}
```

---

## 11. WebSocket

### 11.1 세션 WebSocket
```
ws://localhost:8000/ws/sessions/{session_code}/?token={jwt_access_token}
```

#### 수신 메시지 타입

| Type | 설명 | 수신자 |
|------|------|--------|
| `step_changed` | 단계 변경 알림 | 모든 참가자 |
| `session_status_changed` | 세션 상태 변경 | 모든 참가자 |
| `instructor_message` | 강사 메시지 | 모든 참가자 |
| `participant_joined` | 참가자 입장 | 다른 참가자 |
| `participant_left` | 참가자 퇴장 | 다른 참가자 |
| `student_completion` | 학생 단계 완료 | 강사만 |
| `screenshot_updated` | 스크린샷 업데이트 | 강사만 |
| `help_requested` | 도움 요청 | 강사만 |

#### 전송 메시지 타입

| Type | 설명 | 발신자 |
|------|------|--------|
| `next_step` | 다음 단계 진행 | 강사 |
| `pause_session` | 세션 일시정지 | 강사 |
| `resume_session` | 세션 재개 | 강사 |
| `end_session` | 세션 종료 | 강사 |
| `join` | 세션 참가 | 학생 |
| `heartbeat` | 연결 유지 | 모두 |
| `step_complete` | 단계 완료 | 학생 |
| `request_help` | 도움 요청 | 학생 |

#### 메시지 예시

**step_changed 수신**:
```json
{
  "type": "step_changed",
  "subtask": {
    "id": 3,
    "title": "동영상 검색하기",
    "order_index": 3,
    "target_action": "INPUT",
    "guide_text": "검색창에 '트로트'를 입력하세요",
    "voice_guide_text": "검색창에 트로트라고 입력해주세요"
  }
}
```

**step_complete 전송**:
```json
{
  "type": "step_complete",
  "subtask_id": 3
}
```

---

### 11.2 대시보드 WebSocket
```
ws://localhost:8000/ws/dashboard/lectures/{lecture_id}/?token={jwt_access_token}
```

---

### 11.3 진행상태 WebSocket
```
ws://localhost:8000/ws/progress/{user_id}/?token={jwt_access_token}
```

---

## 12. 에러 코드

### HTTP 상태 코드

| 코드 | 설명 |
|------|------|
| 200 | 성공 |
| 201 | 생성 성공 |
| 202 | 요청 수락 (비동기 처리) |
| 400 | 잘못된 요청 |
| 401 | 인증 실패 |
| 403 | 권한 없음 |
| 404 | 리소스 없음 |
| 409 | 충돌 (중복 등) |
| 422 | 유효성 검증 실패 |
| 429 | 요청 한도 초과 |
| 500 | 서버 오류 |

### 인증 에러

| 에러 메시지 | 원인 |
|------------|------|
| `Token is invalid or expired` | JWT 토큰 만료/무효 |
| `Token is blacklisted` | 블랙리스트 처리된 토큰 |
| `Authentication credentials were not provided` | 토큰 미제공 |

### 세션 에러

| 에러 메시지 | 원인 |
|------------|------|
| `세션을 찾을 수 없습니다` | 잘못된 세션 코드 |
| `세션이 시작되지 않았습니다` | WAITING 상태에서 작업 시도 |
| `이미 종료된 세션입니다` | ENDED 세션에 접근 |
| `이 세션의 강사가 아닙니다` | 권한 없는 제어 시도 |

---

## 부록: 데이터 모델 요약

### User (사용자)
- role: `INSTRUCTOR` | `STUDENT`
- digital_level: `BEGINNER` | `INTERMEDIATE` | `ADVANCED`

### LectureSession (세션)
- status: `WAITING` | `IN_PROGRESS` | `PAUSED` | `REVIEW_MODE` | `ENDED`

### SessionParticipant (참가자)
- status: `WAITING` | `ACTIVE` | `COMPLETED` | `DISCONNECTED`

### UserProgress (진행상태)
- status: `NOT_STARTED` | `IN_PROGRESS` | `COMPLETED` | `HELP_NEEDED`

### HelpRequest (도움요청)
- status: `PENDING` | `ANALYZING` | `RESOLVED`
- request_type: `MANUAL` | `AUTO`

### Subtask (세부단계)
- target_action: `CLICK` | `LONG_CLICK` | `SCROLL` | `INPUT` | `NAVIGATE`
