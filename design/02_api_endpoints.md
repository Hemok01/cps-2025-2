# API 엔드포인트 설계

## 개요

Backend는 Django REST Framework 기반으로 구축하며, 다음과 같은 API 그룹으로 구성됩니다:

- **Auth**: 인증 및 사용자 관리
- **Lectures**: 강의 관리
- **Sessions**: 실시간 강의방 관리 ⭐ NEW
- **Tasks**: 과제/단계 관리
- **Progress**: 학습 진행 상태
- **Logs**: 행동 로그 수집
- **Help**: 도움 요청 및 제공
- **Dashboard**: 강사용 모니터링
- **Admin**: 시스템 관리

---

## 1. Auth (인증/사용자 관리)

### POST /api/auth/register
회원가입

**Request**:
```json
{
  "email": "student@example.com",
  "phone": "01012345678",
  "password": "securePassword123",
  "name": "김학생",
  "age": 65,
  "role": "STUDENT",
  "digital_level": "BEGINNER"
}
```

**Response** (201 Created):
```json
{
  "id": 1,
  "email": "student@example.com",
  "name": "김학생",
  "role": "STUDENT",
  "digital_level": "BEGINNER",
  "created_at": "2025-10-28T10:00:00Z"
}
```

---

### POST /api/auth/login
로그인

**Request**:
```json
{
  "email": "student@example.com",
  "password": "securePassword123"
}
```

**Response** (200 OK):
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "email": "student@example.com",
    "name": "김학생",
    "role": "STUDENT"
  }
}
```

---

### POST /api/auth/refresh
토큰 갱신

**Request**:
```json
{
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response** (200 OK):
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

---

### GET /api/auth/me
현재 사용자 정보 조회

**Headers**: `Authorization: Bearer {access_token}`

**Response** (200 OK):
```json
{
  "id": 1,
  "email": "student@example.com",
  "name": "김학생",
  "role": "STUDENT",
  "digital_level": "BEGINNER",
  "age": 65
}
```

---

## 2. Lectures (강의 관리)

### GET /api/lectures
강의 목록 조회

**Query Parameters**:
- `instructor_id`: 특정 강사의 강의만 조회
- `is_active`: true/false

**Response** (200 OK):
```json
{
  "lectures": [
    {
      "id": 1,
      "title": "카카오톡 기초",
      "description": "카카오톡 사용법을 배웁니다",
      "instructor": {
        "id": 10,
        "name": "박강사"
      },
      "thumbnail_url": "https://...",
      "created_at": "2025-10-01T00:00:00Z"
    }
  ]
}
```

---

### POST /api/lectures
강의 생성 (강사 전용)

**Request**:
```json
{
  "title": "카카오톡 기초",
  "description": "카카오톡 사용법을 배웁니다",
  "thumbnail_url": "https://..."
}
```

**Response** (201 Created):
```json
{
  "id": 1,
  "title": "카카오톡 기초",
  "instructor_id": 10,
  "created_at": "2025-10-28T10:00:00Z"
}
```

---

### GET /api/lectures/{lecture_id}
강의 상세 조회

**Response** (200 OK):
```json
{
  "id": 1,
  "title": "카카오톡 기초",
  "description": "...",
  "instructor": {
    "id": 10,
    "name": "박강사"
  },
  "tasks": [
    {
      "id": 100,
      "title": "메시지 보내기",
      "order_index": 1
    }
  ],
  "enrolled_count": 25
}
```

---

### PUT /api/lectures/{lecture_id}
강의 수정 (강사 전용)

---

### DELETE /api/lectures/{lecture_id}
강의 삭제 (강사 전용)

---

### POST /api/lectures/{lecture_id}/enroll
강의 수강 신청 (학생 전용)

**Response** (201 Created):
```json
{
  "enrollment_id": 500,
  "lecture_id": 1,
  "user_id": 1,
  "enrolled_at": "2025-10-28T10:00:00Z"
}
```

---

## 3. Sessions (실시간 강의방 관리) ⭐ NEW

### POST /api/lectures/{lecture_id}/sessions
강의방 생성 (강사 전용)

**Description**: 강사가 실시간 강의방을 만들고 QR 코드를 생성합니다.

**Request**:
```json
{
  "title": "2025-10-28 오전반",
  "scheduled_at": "2025-10-28T10:00:00Z"
}
```

**Response** (201 Created):
```json
{
  "session_id": 1,
  "session_code": "ABC123",
  "qr_code_url": "https://api.example.com/qr/ABC123.png",
  "status": "WAITING",
  "created_at": "2025-10-28T09:50:00Z",
  "lecture": {
    "id": 1,
    "title": "카카오톡 기초"
  }
}
```

---

### GET /api/sessions/{session_code}
QR 코드로 세션 조회 (학생용)

**Description**: 학생이 QR 코드를 스캔한 후 세션 정보를 조회합니다.

**Response** (200 OK):
```json
{
  "session_id": 1,
  "session_code": "ABC123",
  "title": "2025-10-28 오전반",
  "status": "WAITING",
  "lecture": {
    "id": 1,
    "title": "카카오톡 기초",
    "description": "카카오톡 사용법을 배웁니다"
  },
  "instructor": {
    "id": 10,
    "name": "박강사"
  },
  "participant_count": 12,
  "scheduled_at": "2025-10-28T10:00:00Z"
}
```

---

### POST /api/sessions/{session_id}/join
세션 입장 (학생용)

**Description**: 학생이 세션에 참가합니다 (대기실로 입장).

**Request**:
```json
{
  "user_id": 1
}
```

**Response** (201 Created):
```json
{
  "participant_id": 100,
  "session_id": 1,
  "user_id": 1,
  "status": "WAITING",
  "joined_at": "2025-10-28T09:55:00Z",
  "message": "대기실에 입장했습니다. 강사가 수업을 시작할 때까지 기다려주세요."
}
```

---

### GET /api/sessions/{session_id}/participants
참가자 목록 조회 (강사용)

**Description**: 현재 세션에 입장한 모든 참가자 목록을 조회합니다.

**Response** (200 OK):
```json
{
  "session_id": 1,
  "participants": [
    {
      "user_id": 1,
      "name": "김학생",
      "email": "student1@example.com",
      "status": "WAITING",
      "joined_at": "2025-10-28T09:55:00Z"
    },
    {
      "user_id": 2,
      "name": "이학생",
      "email": "student2@example.com",
      "status": "WAITING",
      "joined_at": "2025-10-28T09:56:00Z"
    }
  ],
  "total_count": 2
}
```

---

### POST /api/sessions/{session_id}/start
강의 시작 (강사 전용)

**Description**: 강사가 대기실에서 수업을 시작합니다. 모든 대기 중인 참가자가 활성화됩니다.

**Request**:
```json
{
  "first_subtask_id": 1000,
  "message": "수업을 시작하겠습니다. 첫 번째 단계를 함께 진행해봅시다."
}
```

**Response** (200 OK):
```json
{
  "session_id": 1,
  "status": "IN_PROGRESS",
  "started_at": "2025-10-28T10:00:00Z",
  "current_subtask": {
    "id": 1000,
    "title": "카카오톡 앱 열기"
  },
  "active_participants": 12,
  "message": "수업이 시작되었습니다"
}
```

**Side Effects**:
- `lecture_sessions.status` → `IN_PROGRESS`
- `lecture_sessions.current_subtask_id` → `first_subtask_id`
- 모든 `session_participants.status` → `ACTIVE` (WAITING → ACTIVE)
- WebSocket 메시지 전송 (모든 학생에게 시작 알림)

---

### POST /api/sessions/{session_id}/next-step
다음 단계로 진행 (강사 전용)

**Description**: 강사가 현재 단계를 완료하고 다음 단계로 이동합니다. 모든 학생이 동기화됩니다.

**Request**:
```json
{
  "next_subtask_id": 1001,
  "message": "잘하셨습니다! 이제 다음 단계로 넘어갑니다."
}
```

**Response** (200 OK):
```json
{
  "session_id": 1,
  "previous_subtask": {
    "id": 1000,
    "title": "카카오톡 앱 열기"
  },
  "current_subtask": {
    "id": 1001,
    "title": "친구 선택하기"
  },
  "timestamp": "2025-10-28T10:05:00Z"
}
```

**Side Effects**:
- `lecture_sessions.current_subtask_id` → `next_subtask_id`
- 모든 활성 `session_participants.current_subtask_id` → `next_subtask_id`
- `session_step_control` 테이블에 기록 추가
- WebSocket 메시지 전송 (모든 학생에게 단계 변경 알림)

---

### POST /api/sessions/{session_id}/pause
강의 일시 정지 (강사 전용)

**Description**: 강사가 수업을 일시 정지합니다 (예: 질문 받기, 휴식).

**Request**:
```json
{
  "message": "잠시 쉬는 시간입니다. 5분 후 다시 시작하겠습니다."
}
```

**Response** (200 OK):
```json
{
  "session_id": 1,
  "action": "PAUSE",
  "message": "수업이 일시 정지되었습니다",
  "timestamp": "2025-10-28T10:15:00Z"
}
```

---

### POST /api/sessions/{session_id}/resume
강의 재개 (강사 전용)

**Description**: 일시 정지된 수업을 재개합니다.

**Request**:
```json
{
  "message": "다시 시작하겠습니다."
}
```

**Response** (200 OK):
```json
{
  "session_id": 1,
  "action": "RESUME",
  "current_subtask": {
    "id": 1001,
    "title": "친구 선택하기"
  },
  "timestamp": "2025-10-28T10:20:00Z"
}
```

---

### POST /api/sessions/{session_id}/end
강의 종료 (강사 전용)

**Description**: 강사가 수업을 종료하고 복습 모드로 전환합니다.

**Request**:
```json
{
  "message": "수고하셨습니다! 복습은 언제든 가능합니다."
}
```

**Response** (200 OK):
```json
{
  "session_id": 1,
  "status": "REVIEW_MODE",
  "ended_at": "2025-10-28T11:00:00Z",
  "duration_minutes": 60,
  "completed_participants": 10,
  "total_participants": 12,
  "message": "수업이 종료되었습니다"
}
```

**Side Effects**:
- `lecture_sessions.status` → `REVIEW_MODE`
- `lecture_sessions.ended_at` → 현재 시각
- WebSocket 메시지 전송 (모든 학생에게 종료 알림)
- 학생들은 복습 모드로 계속 학습 가능

---

### GET /api/sessions/{session_id}/current
현재 세션 상태 조회

**Description**: 세션의 현재 상태를 조회합니다 (학생/강사 모두 사용).

**Response** (200 OK):
```json
{
  "session_id": 1,
  "status": "IN_PROGRESS",
  "current_subtask": {
    "id": 1001,
    "title": "친구 선택하기",
    "guide_text": "친구 목록에서 메시지를 보낼 친구를 선택하세요",
    "voice_guide_text": "친구 목록에서 메시지를 보낼 친구를 선택하세요"
  },
  "started_at": "2025-10-28T10:00:00Z",
  "active_participants": 12,
  "instructor_message": "천천히 따라해보세요"
}
```

---

### GET /api/sessions/my-active
내가 참가 중인 활성 세션 조회 (학생용)

**Description**: 학생이 현재 참가 중인 세션을 조회합니다.

**Response** (200 OK):
```json
{
  "active_session": {
    "session_id": 1,
    "session_code": "ABC123",
    "lecture": {
      "id": 1,
      "title": "카카오톡 기초"
    },
    "status": "IN_PROGRESS",
    "current_subtask": {
      "id": 1001,
      "title": "친구 선택하기"
    },
    "my_status": "ACTIVE",
    "joined_at": "2025-10-28T09:55:00Z"
  }
}
```

**Response** (404 Not Found - 참가 중인 세션 없음):
```json
{
  "active_session": null,
  "message": "참가 중인 활성 세션이 없습니다"
}
```

---

## 4. Tasks & Subtasks (과제/단계 관리)

### POST /api/lectures/{lecture_id}/tasks
Task 생성 (강사 전용)

**Request**:
```json
{
  "title": "메시지 보내기",
  "description": "친구에게 카카오톡 메시지를 보냅니다",
  "order_index": 1
}
```

---

### GET /api/tasks/{task_id}
Task 상세 조회

**Response** (200 OK):
```json
{
  "id": 100,
  "lecture_id": 1,
  "title": "메시지 보내기",
  "description": "...",
  "order_index": 1,
  "subtasks": [
    {
      "id": 1000,
      "title": "카카오톡 앱 열기",
      "order_index": 1,
      "target_action": "CLICK",
      "guide_text": "홈 화면에서 노란색 카카오톡 아이콘을 눌러주세요",
      "voice_guide_text": "홈 화면에서 노란색 카카오톡 아이콘을 눌러주세요"
    },
    {
      "id": 1001,
      "title": "친구 선택하기",
      "order_index": 2,
      "target_action": "CLICK",
      "guide_text": "친구 목록에서 메시지를 보낼 친구를 선택하세요"
    }
  ]
}
```

---

### POST /api/tasks/{task_id}/subtasks
Subtask 생성 (강사 전용)

**Request**:
```json
{
  "title": "카카오톡 앱 열기",
  "description": "홈 화면에서 카카오톡 앱을 찾아 엽니다",
  "order_index": 1,
  "target_action": "CLICK",
  "target_element_hint": "com.kakao.talk",
  "guide_text": "홈 화면에서 노란색 카카오톡 아이콘을 눌러주세요",
  "voice_guide_text": "홈 화면에서 노란색 카카오톡 아이콘을 눌러주세요"
}
```

---

### PUT /api/subtasks/{subtask_id}
Subtask 수정 (강사 전용)

---

### DELETE /api/subtasks/{subtask_id}
Subtask 삭제 (강사 전용)

---

## 5. Progress (학습 진행 상태)

### GET /api/progress/me
내 진행 상태 조회 (학생용)

**Query Parameters**:
- `lecture_id`: 특정 강의의 진행 상태만 조회

**Response** (200 OK):
```json
{
  "current_subtask": {
    "id": 1000,
    "task_id": 100,
    "title": "카카오톡 앱 열기",
    "guide_text": "홈 화면에서 노란색 카카오톡 아이콘을 눌러주세요",
    "status": "IN_PROGRESS"
  },
  "progress_summary": {
    "completed_subtasks": 5,
    "total_subtasks": 20,
    "completion_rate": 0.25
  }
}
```

---

### POST /api/progress/update
진행 상태 업데이트

**Request**:
```json
{
  "subtask_id": 1000,
  "status": "COMPLETED"
}
```

**Response** (200 OK):
```json
{
  "subtask_id": 1000,
  "status": "COMPLETED",
  "completed_at": "2025-10-28T10:05:00Z",
  "next_subtask": {
    "id": 1001,
    "title": "친구 선택하기"
  }
}
```

---

### GET /api/progress/users/{user_id}/lectures/{lecture_id}
특정 학생의 강의 진행 상태 조회 (강사용)

**Response** (200 OK):
```json
{
  "user": {
    "id": 1,
    "name": "김학생"
  },
  "lecture": {
    "id": 1,
    "title": "카카오톡 기초"
  },
  "progress": [
    {
      "task_title": "메시지 보내기",
      "subtask_title": "카카오톡 앱 열기",
      "status": "COMPLETED",
      "completed_at": "2025-10-28T10:05:00Z"
    },
    {
      "task_title": "메시지 보내기",
      "subtask_title": "친구 선택하기",
      "status": "IN_PROGRESS",
      "started_at": "2025-10-28T10:06:00Z"
    }
  ]
}
```

---

## 6. Logs (행동 로그 수집)

### POST /api/logs/activity
행동 로그 전송 (클라이언트 → 서버)

**Request**:
```json
{
  "user_id": 1,
  "subtask_id": 1000,
  "event_type": "CLICK",
  "event_data": {
    "x": 150,
    "y": 300,
    "element": "Button",
    "text": "확인"
  },
  "screen_info": {
    "package_name": "com.kakao.talk",
    "activity_name": "MainActivity",
    "screen_title": "친구 목록"
  },
  "timestamp": "2025-10-28T10:05:30Z"
}
```

**Response** (201 Created):
```json
{
  "log_id": 50000,
  "message": "Log saved successfully"
}
```

**Note**: 이 API는 Kafka Producer로 메시지를 전송하고, Consumer가 DB에 저장합니다.

---

### POST /api/logs/batch
배치 로그 전송 (네트워크 효율성)

**Request**:
```json
{
  "logs": [
    { "event_type": "CLICK", "event_data": {...}, "timestamp": "..." },
    { "event_type": "SCROLL", "event_data": {...}, "timestamp": "..." }
  ]
}
```

---

## 7. Help (도움 요청 및 제공)

### POST /api/help/request
도움 요청

**Request**:
```json
{
  "user_id": 1,
  "subtask_id": 1001,
  "request_type": "MANUAL",
  "context_data": {
    "current_screen": "com.kakao.talk.MainActivity",
    "recent_actions": ["CLICK", "SCROLL"],
    "stuck_duration_seconds": 30
  }
}
```

**Response** (201 Created):
```json
{
  "help_request_id": 5000,
  "status": "ANALYZING",
  "message": "분석 중입니다. 잠시만 기다려주세요."
}
```

**Note**: 이 API는 Kafka를 통해 M-GPT 분석 요청을 비동기로 처리합니다.

---

### GET /api/help/request/{help_request_id}
도움 요청 상태 조회

**Response** (200 OK):
```json
{
  "help_request_id": 5000,
  "status": "RESOLVED",
  "help_response": {
    "help_type": "TEXT",
    "help_content": "화면 상단의 '친구' 탭을 눌러보세요. 메시지를 보낼 친구를 찾을 수 있습니다.",
    "voice_guide_text": "화면 상단의 친구 탭을 눌러보세요"
  },
  "analysis": {
    "problem_diagnosis": "사용자가 친구 탭을 찾지 못하고 있습니다",
    "confidence_score": 0.85
  }
}
```

---

### POST /api/help/feedback
도움에 대한 피드백 제출

**Request**:
```json
{
  "help_response_id": 3000,
  "rating": 5,
  "feedback_text": "매우 도움이 되었습니다"
}
```

---

## 8. Dashboard (강사용 모니터링 - 웹 전용)

**Note**: 강사용 Dashboard는 PC 웹 서비스로 제공됩니다 ([06_web_dashboard_architecture.md](./06_web_dashboard_architecture.md) 참조).

### GET /api/dashboard/lectures/{lecture_id}/students
수강생 목록 및 진행률 (웹 Dashboard에서 사용)

**Response** (200 OK):
```json
{
  "lecture_id": 1,
  "students": [
    {
      "user_id": 1,
      "name": "김학생",
      "email": "student@example.com",
      "progress_rate": 0.25,
      "completed_subtasks": 5,
      "total_subtasks": 20,
      "current_subtask": {
        "id": 1001,
        "title": "친구 선택하기",
        "status": "IN_PROGRESS"
      },
      "help_count": 3,
      "last_activity": "2025-10-28T10:06:00Z",
      "enrolled_at": "2025-10-20T09:00:00Z"
    },
    {
      "user_id": 2,
      "name": "이학생",
      "email": "student2@example.com",
      "progress_rate": 0.60,
      "completed_subtasks": 12,
      "total_subtasks": 20,
      "current_subtask": {
        "id": 1012,
        "title": "메시지 입력하기",
        "status": "IN_PROGRESS"
      },
      "help_count": 1,
      "last_activity": "2025-10-28T10:08:00Z",
      "enrolled_at": "2025-10-20T09:00:00Z"
    }
  ]
}
```

---

### GET /api/dashboard/help-requests/pending
대기 중인 도움 요청 (실시간 알림용)

**Response** (200 OK):
```json
{
  "pending_requests": [
    {
      "help_request_id": 5001,
      "user": {
        "id": 1,
        "name": "김학생"
      },
      "subtask": {
        "id": 1001,
        "title": "친구 선택하기"
      },
      "created_at": "2025-10-28T10:10:00Z",
      "status": "ANALYZING"
    }
  ]
}
```

---

### GET /api/dashboard/statistics/lecture/{lecture_id}
강의 통계 (웹 Dashboard 차트용)

**Response** (200 OK):
```json
{
  "lecture_id": 1,
  "total_students": 25,
  "average_progress_rate": 0.42,
  "total_help_requests": 150,
  "average_completion_time_minutes": 45,
  "common_difficulties": [
    {
      "subtask_id": 1001,
      "subtask_title": "친구 선택하기",
      "help_count": 35,
      "average_stuck_time_seconds": 120
    }
  ],
  "daily_activity": [
    {
      "date": "2025-10-21",
      "active_students": 20,
      "help_requests": 35
    },
    {
      "date": "2025-10-22",
      "active_students": 22,
      "help_requests": 28
    }
  ],
  "progress_distribution": {
    "0-25%": 5,
    "26-50%": 8,
    "51-75%": 7,
    "76-100%": 5
  }
}
```

---

### GET /api/dashboard/chart-data/progress-trend
진행률 추이 데이터 (Chart.js용)

**Query Parameters**:
- `lecture_id`: 강의 ID
- `days`: 조회 기간 (기본 7일)

**Response** (200 OK):
```json
{
  "labels": ["10/21", "10/22", "10/23", "10/24", "10/25", "10/26", "10/27"],
  "datasets": [
    {
      "label": "평균 진행률",
      "data": [25, 30, 35, 38, 42, 45, 48]
    },
    {
      "label": "도움 요청 수",
      "data": [35, 28, 30, 25, 22, 20, 18]
    }
  ]
}
```

---

## 9. Notifications (알림)

### GET /api/notifications
내 알림 목록

**Response** (200 OK):
```json
{
  "notifications": [
    {
      "id": 1000,
      "type": "HELP_REQUEST",
      "message": "김학생님이 도움을 요청했습니다",
      "related_user": {
        "id": 1,
        "name": "김학생"
      },
      "is_read": false,
      "created_at": "2025-10-28T10:10:00Z"
    }
  ]
}
```

---

### POST /api/notifications/{notification_id}/read
알림 읽음 처리

---

## 10. WebSocket (실시간 통신)

**Note**: WebSocket은 Django Channels 기반으로 구현됩니다.

### 10.1 실시간 강의방용 WebSocket ⭐ NEW

### WS /ws/session/{session_id}/student/
학생용 세션 WebSocket (실시간 단계 변경 수신)

**Description**: 학생이 세션에 입장하면 자동으로 연결되며, 강사의 단계 제어 명령을 실시간으로 수신합니다.

**연결 시**:
- 학생 인증 확인
- `session_{session_id}_students` 그룹에 자동 가입
- 현재 세션 상태 전송

**수신 메시지 타입**:

#### 1. 강의 시작
```json
{
  "type": "session_started",
  "data": {
    "session_id": 1,
    "started_at": "2025-10-28T10:00:00Z",
    "current_subtask": {
      "id": 1000,
      "title": "카카오톡 앱 열기",
      "guide_text": "홈 화면에서 노란색 카카오톡 아이콘을 눌러주세요",
      "voice_guide_text": "홈 화면에서 노란색 카카오톡 아이콘을 눌러주세요"
    },
    "instructor_message": "수업을 시작하겠습니다"
  }
}
```

#### 2. 단계 변경
```json
{
  "type": "step_changed",
  "data": {
    "previous_subtask_id": 1000,
    "current_subtask": {
      "id": 1001,
      "title": "친구 선택하기",
      "guide_text": "친구 목록에서 메시지를 보낼 친구를 선택하세요",
      "voice_guide_text": "친구 목록에서 메시지를 보낼 친구를 선택하세요"
    },
    "instructor_message": "잘하셨습니다! 다음 단계로 넘어갑니다",
    "timestamp": "2025-10-28T10:05:00Z"
  }
}
```

#### 3. 강의 일시 정지
```json
{
  "type": "session_paused",
  "data": {
    "session_id": 1,
    "instructor_message": "잠시 쉬는 시간입니다",
    "timestamp": "2025-10-28T10:15:00Z"
  }
}
```

#### 4. 강의 재개
```json
{
  "type": "session_resumed",
  "data": {
    "session_id": 1,
    "instructor_message": "다시 시작하겠습니다",
    "timestamp": "2025-10-28T10:20:00Z"
  }
}
```

#### 5. 강의 종료
```json
{
  "type": "session_ended",
  "data": {
    "session_id": 1,
    "ended_at": "2025-10-28T11:00:00Z",
    "instructor_message": "수고하셨습니다!",
    "review_mode_enabled": true
  }
}
```

---

### WS /ws/session/{session_id}/instructor/
강사용 세션 WebSocket (학생 상태 실시간 모니터링)

**Description**: 강사가 세션의 학생 상태를 실시간으로 모니터링합니다.

**연결 시**:
- 강사 인증 확인
- `session_{session_id}_instructor` 그룹에 자동 가입
- 현재 참가자 목록 전송

**수신 메시지 타입**:

#### 1. 학생 입장
```json
{
  "type": "student_joined",
  "data": {
    "user_id": 1,
    "name": "김학생",
    "email": "student@example.com",
    "joined_at": "2025-10-28T09:55:00Z",
    "total_participants": 12
  }
}
```

#### 2. 학생 진행 상태 업데이트
```json
{
  "type": "student_progress",
  "data": {
    "user_id": 1,
    "name": "김학생",
    "subtask_id": 1000,
    "subtask_title": "카카오톡 앱 열기",
    "status": "COMPLETED",
    "completed_at": "2025-10-28T10:05:00Z"
  }
}
```

#### 3. 학생 도움 요청
```json
{
  "type": "student_help_request",
  "data": {
    "help_request_id": 5001,
    "user": {
      "id": 1,
      "name": "김학생"
    },
    "subtask": {
      "id": 1001,
      "title": "친구 선택하기"
    },
    "stuck_duration_seconds": 45,
    "timestamp": "2025-10-28T10:06:00Z"
  }
}
```

#### 4. 학생 연결 끊김
```json
{
  "type": "student_disconnected",
  "data": {
    "user_id": 2,
    "name": "이학생",
    "last_active_at": "2025-10-28T10:10:00Z",
    "active_participants": 11
  }
}
```

---

### 10.2 강사 Dashboard용 WebSocket (기존)

### WS /ws/dashboard/lecture/{lecture_id}/
특정 강의의 실시간 업데이트 구독

**연결 시**:
- 강사 인증 확인
- `lecture_{lecture_id}` 그룹에 자동 가입

**수신 메시지 타입**:

#### 1. 진행 상태 업데이트
```json
{
  "type": "progress_update",
  "data": {
    "user_id": 1,
    "user_name": "김학생",
    "subtask_id": 1001,
    "subtask_title": "친구 선택하기",
    "old_status": "IN_PROGRESS",
    "new_status": "COMPLETED",
    "timestamp": "2025-10-28T10:15:00Z"
  }
}
```

#### 2. 도움 요청
```json
{
  "type": "help_request",
  "data": {
    "help_request_id": 5001,
    "user": {
      "id": 1,
      "name": "김학생"
    },
    "subtask": {
      "id": 1001,
      "title": "친구 선택하기"
    },
    "request_type": "MANUAL",
    "stuck_duration_seconds": 45,
    "timestamp": "2025-10-28T10:15:00Z"
  }
}
```

#### 3. 학생 활동 (로그인/로그아웃)
```json
{
  "type": "student_activity",
  "data": {
    "user_id": 1,
    "user_name": "김학생",
    "activity": "STARTED_LEARNING",
    "timestamp": "2025-10-28T10:00:00Z"
  }
}
```

---

### WS /ws/dashboard/help-requests/
강사의 모든 도움 요청 실시간 수신

**연결 시**:
- `instructor_{instructor_id}_help` 그룹에 자동 가입
- 강사가 담당하는 모든 강의의 도움 요청 수신

**수신 메시지**:
```json
{
  "type": "new_help_request",
  "data": {
    "help_request_id": 5002,
    "lecture": {
      "id": 1,
      "title": "카카오톡 기초"
    },
    "user": {
      "id": 2,
      "name": "이학생"
    },
    "subtask": {
      "id": 1005,
      "title": "메시지 전송하기"
    },
    "priority": "NORMAL",
    "timestamp": "2025-10-28T10:20:00Z"
  }
}
```

---

### WS /ws/help/{user_id}
학생 앱의 실시간 도움 수신

**받는 메시지**:
```json
{
  "type": "HELP_AVAILABLE",
  "help_response": {
    "help_content": "화면 상단의 '친구' 탭을 눌러보세요",
    "voice_guide_text": "화면 상단의 친구 탭을 눌러보세요"
  }
}
```

---

## API 공통 규칙

### 인증
- 대부분의 API는 `Authorization: Bearer {token}` 헤더 필요
- 토큰 만료 시 401 Unauthorized 반환

### 에러 응답 형식
```json
{
  "error": {
    "code": "INVALID_REQUEST",
    "message": "Subtask not found",
    "details": {}
  }
}
```

### 페이지네이션
- Query Parameters: `page`, `page_size`
- 기본값: `page=1`, `page_size=20`

**Response**:
```json
{
  "results": [...],
  "pagination": {
    "page": 1,
    "page_size": 20,
    "total_count": 100,
    "total_pages": 5
  }
}
```

---

## 성능 최적화

1. **로그 전송**: 배치 전송 사용 (`/api/logs/batch`)
2. **실시간 업데이트**: WebSocket 활용
3. **캐싱**: 강의/Task 정보는 Redis 캐싱
4. **비동기 처리**: Kafka로 로그 저장 및 M-GPT 분석 비동기 처리
