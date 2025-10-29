# WebSocket 사용 가이드

## 개요

MobileGPT 시니어 디지털 교육 서비스는 실시간 통신을 위해 WebSocket을 사용합니다. 이 문서는 WebSocket 엔드포인트와 사용법을 설명합니다.

## WebSocket 엔드포인트

### 1. 세션 WebSocket (Session Sync)

**URL**: `ws://localhost:8000/ws/sessions/<session_code>/`

**용도**: 실시간 강의 세션 동기화

**연결 요구사항**:
- 인증된 사용자
- 세션에 등록된 학생 또는 세션의 강사

**클라이언트 → 서버 메시지**:

```json
// 강사: 다음 단계로 이동
{
  "type": "next_step",
  "subtask_id": 123
}

// 강사: 세션 일시정지
{
  "type": "pause_session"
}

// 강사: 세션 재개
{
  "type": "resume_session"
}

// 강사: 세션 종료
{
  "type": "end_session"
}

// 학생: 진행 상황 업데이트
{
  "type": "progress_update",
  "subtask_id": 123,
  "status": "COMPLETED"
}

// 학생: 도움 요청
{
  "type": "help_request",
  "subtask_id": 123,
  "message": "이 단계가 어렵습니다"
}
```

**서버 → 클라이언트 메시지**:

```json
// 단계 변경 알림 (모든 참여자)
{
  "type": "step_changed",
  "subtask": {
    "id": 123,
    "title": "앱 설치하기",
    "order": 1,
    "target_action": "CLICK",
    "guide_text": "설치 버튼을 눌러주세요",
    "voice_guide_text": "화면의 설치 버튼을 찾아 눌러주세요"
  }
}

// 세션 상태 변경 (모든 참여자)
{
  "type": "session_status_changed",
  "status": "PAUSED",
  "message": "세션이 일시정지되었습니다"
}

// 참여자 입장 (모든 참여자)
{
  "type": "participant_joined",
  "user_id": 456,
  "user_name": "김철수",
  "role": "STUDENT"
}

// 참여자 퇴장 (모든 참여자)
{
  "type": "participant_left",
  "user_id": 456,
  "user_name": "김철수"
}

// 진행 상황 업데이트 (강사만)
{
  "type": "progress_updated",
  "user_id": 456,
  "user_name": "김철수",
  "subtask_id": 123,
  "status": "COMPLETED"
}

// 도움 요청 (강사만)
{
  "type": "help_requested",
  "user_id": 456,
  "user_name": "김철수",
  "subtask_id": 123,
  "message": "이 단계가 어렵습니다"
}
```

---

### 2. 대시보드 WebSocket (Instructor Dashboard)

**URL**: `ws://localhost:8000/ws/dashboard/lectures/<lecture_id>/`

**용도**: 강사용 실시간 모니터링 대시보드

**연결 요구사항**:
- 인증된 강사
- 해당 강의의 담당 강사

**클라이언트 → 서버 메시지**:

```json
// 현재 통계 요청
{
  "type": "request_statistics"
}

// 학생 목록 요청
{
  "type": "request_student_list"
}
```

**서버 → 클라이언트 메시지**:

```json
// 초기 대시보드 데이터
{
  "type": "initial_data",
  "data": {
    "lecture": {
      "id": 1,
      "title": "스마트폰 기초",
      "description": "스마트폰 사용법 배우기"
    },
    "students": [
      {
        "user_id": 456,
        "user_name": "김철수",
        "email": "kim@example.com",
        "progress_rate": 75.5,
        "completed_subtasks": 15,
        "total_subtasks": 20,
        "pending_help": 1,
        "enrolled_at": "2025-01-15T10:00:00"
      }
    ],
    "total_students": 1,
    "total_subtasks": 20,
    "pending_help_requests": 1
  }
}

// 진행 상황 업데이트
{
  "type": "progress_update",
  "user_id": 456,
  "user_name": "김철수",
  "subtask_id": 16,
  "status": "COMPLETED",
  "progress_rate": 80.0
}

// 도움 요청
{
  "type": "help_request",
  "request_id": 789,
  "user_id": 456,
  "user_name": "김철수",
  "subtask_id": 17,
  "message": "이 단계가 어렵습니다",
  "request_type": "MANUAL"
}

// 참여자 상태 변경
{
  "type": "participant_status",
  "user_id": 456,
  "user_name": "김철수",
  "status": "joined",
  "session_code": "ABC123"
}

// 통계 업데이트
{
  "type": "statistics_update",
  "statistics": {
    "difficult_subtasks": [
      {
        "subtask__id": 5,
        "subtask__title": "앱 다운로드",
        "help_count": 12
      }
    ],
    "average_completion_rate": 65.5,
    "total_enrollments": 25,
    "total_subtasks": 20
  }
}
```

---

### 3. 진행 상황 WebSocket (Student Progress)

**URL**: `ws://localhost:8000/ws/progress/<user_id>/`

**용도**: 학생의 실시간 진행 상황 추적

**연결 요구사항**:
- 인증된 사용자
- 자신의 user_id로만 연결 가능

**클라이언트 → 서버 메시지**:

```json
// 진행 상황 요청
{
  "type": "get_my_progress"
}

// 단계 완료 표시
{
  "type": "mark_complete",
  "subtask_id": 123
}
```

**서버 → 클라이언트 메시지**:

```json
// 초기 진행 상황
{
  "type": "initial_progress",
  "data": {
    "user": {
      "id": 456,
      "name": "김철수",
      "email": "kim@example.com"
    },
    "lectures": [
      {
        "lecture_id": 1,
        "lecture_title": "스마트폰 기초",
        "total_subtasks": 20,
        "completed_subtasks": 15,
        "progress_rate": 75.0,
        "current_subtask": {
          "id": 16,
          "title": "앱 설치하기",
          "order": 16,
          "guide_text": "설치 버튼을 눌러주세요"
        }
      }
    ]
  }
}

// 단계 완료 확인
{
  "type": "subtask_completed",
  "subtask_id": 16,
  "next_subtask": {
    "id": 17,
    "title": "앱 열기",
    "order": 17,
    "target_action": "CLICK",
    "guide_text": "앱 아이콘을 눌러주세요",
    "voice_guide_text": "설치된 앱의 아이콘을 찾아 눌러주세요"
  }
}

// 진행 상황 업데이트
{
  "type": "progress_updated",
  "subtask_id": 16,
  "status": "COMPLETED",
  "message": "단계를 완료했습니다!"
}

// 업적 달성
{
  "type": "achievement_unlocked",
  "achievements": [
    {
      "type": "milestone",
      "title": "10개 단계 완료!",
      "description": "축하합니다! 10개의 단계를 완료하셨습니다.",
      "icon": "trophy"
    }
  ]
}

// 격려 메시지
{
  "type": "encouragement",
  "message": "잘하고 계세요! 조금만 더 힘내세요!",
  "progress_rate": 75.0
}
```

---

## 연결 예제

### JavaScript (브라우저)

```javascript
// JWT 토큰 가져오기
const token = localStorage.getItem('access_token');

// WebSocket 연결
const ws = new WebSocket('ws://localhost:8000/ws/sessions/ABC123/');

// 연결 성공
ws.onopen = function(event) {
  console.log('WebSocket connected');

  // 메시지 전송
  ws.send(JSON.stringify({
    type: 'progress_update',
    subtask_id: 123,
    status: 'COMPLETED'
  }));
};

// 메시지 수신
ws.onmessage = function(event) {
  const data = JSON.parse(event.data);
  console.log('Received:', data);

  if (data.type === 'step_changed') {
    // 새 단계로 이동
    updateCurrentStep(data.subtask);
  }
};

// 에러 처리
ws.onerror = function(error) {
  console.error('WebSocket error:', error);
};

// 연결 종료
ws.onclose = function(event) {
  console.log('WebSocket closed');
};
```

### Python (클라이언트)

```python
import asyncio
import websockets
import json

async def connect_to_session():
    uri = "ws://localhost:8000/ws/sessions/ABC123/"

    async with websockets.connect(uri) as websocket:
        # 연결 성공
        print("Connected to session WebSocket")

        # 메시지 전송
        await websocket.send(json.dumps({
            "type": "progress_update",
            "subtask_id": 123,
            "status": "COMPLETED"
        }))

        # 메시지 수신
        while True:
            response = await websocket.recv()
            data = json.loads(response)
            print(f"Received: {data}")

            if data['type'] == 'step_changed':
                print(f"New step: {data['subtask']['title']}")

asyncio.run(connect_to_session())
```

---

## 인증

WebSocket 연결은 Django Channels의 `AuthMiddlewareStack`을 사용하여 인증됩니다.

**브라우저**: 쿠키 기반 세션 인증이 자동으로 작동합니다.

**네이티브 앱**: JWT 토큰을 쿼리 파라미터로 전달:
```
ws://localhost:8000/ws/sessions/ABC123/?token=<jwt_access_token>
```

---

## 개발 환경 설정

### Channel Layer

**개발 환경** (InMemory):
```bash
# .env 파일에서
USE_REDIS_CHANNELS=False
```

**프로덕션 환경** (Redis):
```bash
# .env 파일에서
USE_REDIS_CHANNELS=True
REDIS_HOST=localhost
REDIS_PORT=6379
```

### 서버 실행

```bash
# ASGI 서버 실행 (Daphne 사용)
daphne -b 0.0.0.0 -p 8000 config.asgi:application

# 또는 Django 개발 서버 (Django 3.0+는 ASGI 지원)
python manage.py runserver
```

---

## 테스트

### 기본 연결 테스트

```bash
python test_websocket.py
```

### 수동 테스트 (wscat)

```bash
# wscat 설치
npm install -g wscat

# 세션 WebSocket 연결
wscat -c ws://localhost:8000/ws/sessions/ABC123/

# 메시지 전송
> {"type": "progress_update", "subtask_id": 123, "status": "COMPLETED"}
```

---

## 문제 해결

### 연결이 즉시 닫힘
- 사용자가 인증되지 않았습니다
- 사용자에게 해당 리소스에 대한 접근 권한이 없습니다
- 세션 코드나 강의 ID가 존재하지 않습니다

### 메시지가 전송되지 않음
- Channel Layer 설정을 확인하세요 (Redis 또는 InMemory)
- 메시지 형식이 올바른지 확인하세요 (JSON)

### Redis 연결 오류
```bash
# Redis 시작
redis-server

# 또는 InMemory로 전환
USE_REDIS_CHANNELS=False
```

---

## 프로덕션 고려사항

1. **Redis 사용**: InMemory는 단일 서버에서만 작동합니다. 프로덕션에서는 Redis를 사용하세요.

2. **HTTPS/WSS**: 프로덕션에서는 `wss://` (WebSocket Secure)를 사용하세요.

3. **로드 밸런싱**: Nginx나 HAProxy를 사용하여 WebSocket 연결을 로드 밸런싱하세요.

4. **연결 제한**: 각 사용자당 동시 연결 수를 제한하세요.

5. **메시지 크기 제한**: 큰 메시지는 분할하여 전송하세요.

---

## 추가 정보

- Django Channels 문서: https://channels.readthedocs.io/
- WebSocket RFC: https://tools.ietf.org/html/rfc6455
