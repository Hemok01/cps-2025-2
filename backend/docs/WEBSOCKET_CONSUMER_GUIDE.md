# WebSocket Consumer 구현 및 테스트 가이드

## 개요

MobileGPT 프로젝트의 WebSocket Consumer가 성공적으로 개선되었습니다. 이 문서는 구현된 기능과 테스트 방법을 설명합니다.

## 구현된 개선사항

### 1. Android 앱 메시지 타입 정렬 ✅

Android 앱과 호환되는 메시지 타입 핸들러 추가:

#### 새로 추가된 메시지 타입

**학생 → 서버 (Android 앱 호환)**
- `join` - 세션 명시적 참가
- `heartbeat` - 연결 유지 (last_active_at 자동 업데이트)
- `step_complete` - 단계 완료 알림
- `request_help` - 도움 요청

**기존 메시지 타입 (하위 호환성 유지)**
- `progress_update` - 진행상황 업데이트
- `help_request` - 도움 요청 (별칭)

#### 메시지 타입 매핑

| Android 앱 | 백엔드 핸들러 | 설명 |
|-----------|-------------|------|
| `join` | `handle_join()` | 세션 참가 확인 및 상태 업데이트 |
| `heartbeat` | `handle_heartbeat()` | 연결 유지 및 활동 시간 업데이트 |
| `step_complete` | `handle_step_complete()` | 단계 완료, SessionParticipant 업데이트 |
| `request_help` | `handle_help_request()` | 도움 요청, 강사에게 전달 |

### 2. SessionParticipant 자동 업데이트 ✅

학생의 진행 상황과 연결 상태가 자동으로 SessionParticipant 모델에 반영됩니다.

#### 추가된 메서드

```python
# apps/sessions/consumers.py

@database_sync_to_async
def update_participant_status(self, status)
    """참가자 상태 업데이트 (ACTIVE, WAITING, COMPLETED, DISCONNECTED)"""

@database_sync_to_async
def update_participant_last_active(self)
    """last_active_at 타임스탬프 업데이트"""

@database_sync_to_async
def update_participant_subtask(self, subtask_id)
    """참가자의 현재 단계(current_subtask) 업데이트"""

@database_sync_to_async
def update_participant_on_disconnect(self)
    """연결 해제 시 상태를 DISCONNECTED로 업데이트"""
```

#### 자동 업데이트 시점

| 이벤트 | 업데이트 항목 |
|--------|-------------|
| WebSocket 연결 | 참가자 생성 또는 조회 |
| `join` 메시지 | `status` → ACTIVE |
| `heartbeat` 메시지 | `last_active_at` → 현재 시간 |
| `step_complete` 메시지 | `current_subtask`, `last_active_at` |
| WebSocket 연결 해제 | `status` → DISCONNECTED |

### 3. SessionStepControl 로깅 ✅

강사의 세션 제어 액션이 SessionStepControl 모델에 자동으로 기록됩니다.

#### 로깅되는 액션

```python
# apps/sessions/consumers.py

@database_sync_to_async
def log_step_control(self, action, subtask_id=None, message='')
    """강사 액션을 SessionStepControl에 기록"""
```

| 강사 액션 | SessionStepControl.action | 설명 |
|----------|-------------------------|------|
| `next_step` | `START_STEP` | 다음 단계로 이동 |
| `pause_session` | `PAUSE` | 세션 일시정지 |
| `resume_session` | `RESUME` | 세션 재개 |
| `end_session` | `END_STEP` | 세션 종료 |

#### 기록되는 정보

- `session` - 해당 세션
- `subtask` - 관련 단계 (있는 경우)
- `instructor` - 액션을 수행한 강사
- `action` - 액션 타입
- `message` - 액션 메시지
- `created_at` - 액션 시간 (자동)

## WebSocket API 문서

### 연결

```
ws://localhost:8000/ws/sessions/<session_code>/
```

**인증**: Django Channels의 `AuthMiddlewareStack` 사용
- 쿠키 기반 세션 인증
- 인증되지 않은 사용자는 연결 거부

### 메시지 형식

모든 메시지는 JSON 형식입니다:

```json
{
  "type": "message_type",
  "data": {
    // 메시지별 데이터
  }
}
```

### 학생 메시지

#### 1. Join (세션 참가)

```json
{
  "type": "join",
  "data": {}
}
```

**응답:**
```json
{
  "type": "join_confirmed",
  "session_code": "TEST001",
  "user_id": 1,
  "message": "세션에 참가했습니다"
}
```

#### 2. Heartbeat (연결 유지)

```json
{
  "type": "heartbeat",
  "data": {
    "timestamp": 1234567890
  }
}
```

**응답:**
```json
{
  "type": "heartbeat_ack",
  "timestamp": 1234567890
}
```

#### 3. Step Complete (단계 완료)

```json
{
  "type": "step_complete",
  "data": {
    "subtask_id": 1
  }
}
```

**응답:**
```json
{
  "type": "step_complete_confirmed",
  "subtask_id": 1,
  "message": "단계를 완료했습니다"
}
```

**강사에게 전송 (role_filter: INSTRUCTOR):**
```json
{
  "type": "progress_updated",
  "user_id": 2,
  "user_name": "Test Student",
  "subtask_id": 1,
  "status": "completed"
}
```

#### 4. Request Help (도움 요청)

```json
{
  "type": "request_help",
  "data": {
    "subtask_id": 1,
    "message": "이 단계를 이해하지 못했습니다"
  }
}
```

**강사에게 전송 (role_filter: INSTRUCTOR):**
```json
{
  "type": "help_requested",
  "user_id": 2,
  "user_name": "Test Student",
  "subtask_id": 1,
  "message": "이 단계를 이해하지 못했습니다"
}
```

### 강사 메시지

#### 1. Next Step (다음 단계로 이동)

```json
{
  "type": "next_step",
  "data": {
    "subtask_id": 2,
    "message": "다음 단계입니다"
  }
}
```

**모든 참가자에게 브로드캐스트:**
```json
{
  "type": "step_changed",
  "subtask": {
    "id": 2,
    "title": "Step 2: Enable Notifications",
    "order_index": 2,
    "target_action": "CLICK",
    "guide_text": "Please enable notifications",
    "voice_guide_text": null
  }
}
```

#### 2. Pause Session (세션 일시정지)

```json
{
  "type": "pause_session",
  "data": {}
}
```

**브로드캐스트:**
```json
{
  "type": "session_status_changed",
  "status": "PAUSED",
  "message": "세션이 일시정지되었습니다"
}
```

#### 3. Resume Session (세션 재개)

```json
{
  "type": "resume_session",
  "data": {}
}
```

**브로드캐스트:**
```json
{
  "type": "session_status_changed",
  "status": "IN_PROGRESS",
  "message": "세션이 재개되었습니다"
}
```

#### 4. End Session (세션 종료)

```json
{
  "type": "end_session",
  "data": {}
}
```

**브로드캐스트:**
```json
{
  "type": "session_status_changed",
  "status": "ENDED",
  "message": "세션이 종료되었습니다"
}
```

### 자동 브로드캐스트 메시지

#### Participant Joined

다른 참가자가 세션에 참가할 때:

```json
{
  "type": "participant_joined",
  "user_id": 3,
  "user_name": "New Student",
  "role": "STUDENT"
}
```

#### Participant Left

참가자가 연결을 해제할 때:

```json
{
  "type": "participant_left",
  "user_id": 3,
  "user_name": "Student Name"
}
```

## 테스트 시나리오

### 시나리오 1: 학생 세션 참가 및 진행

1. **WebSocket 연결**
   ```javascript
   const ws = new WebSocket('ws://localhost:8000/ws/sessions/TEST001/');
   ```

2. **Join 메시지 전송**
   ```javascript
   ws.send(JSON.stringify({
     type: 'join',
     data: {}
   }));
   ```

3. **Heartbeat 전송 (30초마다)**
   ```javascript
   setInterval(() => {
     ws.send(JSON.stringify({
       type: 'heartbeat',
       data: { timestamp: Date.now() }
     }));
   }, 30000);
   ```

4. **단계 완료**
   ```javascript
   ws.send(JSON.stringify({
     type: 'step_complete',
     data: { subtask_id: 1 }
   }));
   ```

### 시나리오 2: 강사 세션 제어

1. **WebSocket 연결** (강사 계정)
   ```javascript
   const ws = new WebSocket('ws://localhost:8000/ws/sessions/TEST001/');
   ```

2. **다음 단계로 이동**
   ```javascript
   ws.send(JSON.stringify({
     type: 'next_step',
     data: {
       subtask_id: 2,
       message: '다음 단계로 이동합니다'
     }
   }));
   ```

3. **세션 일시정지**
   ```javascript
   ws.send(JSON.stringify({
     type: 'pause_session',
     data: {}
   }));
   ```

4. **세션 재개**
   ```javascript
   ws.send(JSON.stringify({
     type: 'resume_session',
     data: {}
   }));
   ```

### 시나리오 3: Python 테스트 스크립트

```python
# test_websocket_consumer.py
import asyncio
import websockets
import json

async def test_student_session():
    uri = "ws://localhost:8000/ws/sessions/TEST001/"

    async with websockets.connect(uri) as websocket:
        # Join session
        await websocket.send(json.dumps({
            'type': 'join',
            'data': {}
        }))

        response = await websocket.recv()
        print(f"Join response: {response}")

        # Send heartbeat
        await websocket.send(json.dumps({
            'type': 'heartbeat',
            'data': {'timestamp': int(time.time())}
        }))

        response = await websocket.recv()
        print(f"Heartbeat response: {response}")

        # Complete step
        await websocket.send(json.dumps({
            'type': 'step_complete',
            'data': {'subtask_id': 1}
        }))

        response = await websocket.recv()
        print(f"Step complete response: {response}")

# Run test
asyncio.run(test_student_session())
```

## 데이터베이스 확인

### SessionParticipant 업데이트 확인

```python
# Django shell
from apps.sessions.models import SessionParticipant

# 모든 참가자 조회
participants = SessionParticipant.objects.all()
for p in participants:
    print(f"User: {p.user.name}")
    print(f"Status: {p.status}")
    print(f"Current Subtask: {p.current_subtask}")
    print(f"Last Active: {p.last_active_at}")
    print("---")
```

### SessionStepControl 로그 확인

```python
# Django shell
from apps.sessions.models import SessionStepControl

# 최근 제어 액션 조회
controls = SessionStepControl.objects.all().order_by('-created_at')[:10]
for c in controls:
    print(f"Action: {c.action}")
    print(f"Instructor: {c.instructor.name}")
    print(f"Subtask: {c.subtask.title if c.subtask else 'N/A'}")
    print(f"Message: {c.message}")
    print(f"Time: {c.created_at}")
    print("---")
```

## 문제 해결

### 1. WebSocket 연결 실패

**증상**: `Connection refused` 또는 `403 Forbidden`

**해결방법**:
- Django Channels가 설치되어 있는지 확인
- ASGI 서버 (Daphne) 실행 확인
- 인증 쿠키가 올바르게 전송되는지 확인

```bash
# Daphne로 서버 실행
daphne -b 0.0.0.0 -p 8000 config.asgi:application
```

### 2. 메시지 핸들러 오류

**증상**: `Unknown message type` 오류

**해결방법**:
- 메시지 타입 철자 확인
- JSON 형식 확인
- 서버 로그 확인

### 3. SessionParticipant 업데이트 안됨

**증상**: last_active_at이 업데이트되지 않음

**해결방법**:
- 학생이 실제로 세션 참가자인지 확인
- Django shell에서 직접 확인:
  ```python
  from apps.sessions.models import SessionParticipant
  SessionParticipant.objects.filter(user_id=2, session__session_code='TEST001')
  ```

## 성능 고려사항

### Heartbeat 간격

- 권장: 30초
- 너무 짧으면 서버 부하 증가
- 너무 길면 연결 끊김 감지 지연

### 브로드캐스트 최적화

- `role_filter` 사용으로 불필요한 메시지 전송 방지
- 강사전용 메시지는 INSTRUCTOR에게만 전송

### 데이터베이스 쿼리 최적화

- `@database_sync_to_async` 데코레이터로 async/await 패턴 사용
- 필요한 경우에만 DB 업데이트 (heartbeat는 제외)

## 다음 단계

Consumer 개선이 완료되었으니 다음 단계를 진행할 수 있습니다:

1. **Android 앱 WebSocket 통합**
   - `WebSocketApi` 인터페이스 업데이트
   - ViewModel에서 Consumer 사용

2. **실시간 UI 업데이트**
   - Jetpack Compose Flow로 WebSocket 메시지 처리
   - 단계 변경 시 UI 자동 업데이트

3. **에러 처리 및 재연결**
   - 네트워크 끊김 시 자동 재연결
   - 연결 상태 표시

4. **테스트 작성**
   - Django Channels 테스트 케이스
   - Mock WebSocket 테스트

---

모든 Consumer 개선사항이 성공적으로 구현되었습니다! 🎉
