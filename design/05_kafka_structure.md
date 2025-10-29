# Kafka 메시지 구조 설계

## 개요

Kafka는 다음과 같은 비동기 작업을 처리합니다:
- 행동 로그 수집
- 도움 요청 처리
- M-GPT 분석 요청
- 실시간 알림 전달
- 진행 상태 업데이트

---

## Kafka 클러스터 구성

```
┌─────────────────────────────────────────────┐
│  Kafka Cluster                              │
│                                             │
│  ┌──────────────┐  ┌──────────────┐        │
│  │  Broker 1    │  │  Broker 2    │  ...   │
│  └──────────────┘  └──────────────┘        │
│                                             │
│  Topics:                                    │
│  - activity-logs (3 partitions)             │
│  - help-requests (2 partitions)             │
│  - help-responses (2 partitions)            │
│  - progress-updates (3 partitions)          │
│  - instructor-notifications (1 partition)   │
└─────────────────────────────────────────────┘
```

---

## Topic 목록 및 용도

| Topic | Partitions | Replication | Retention | 용도 |
|-------|-----------|-------------|-----------|------|
| `activity-logs` | 3 | 2 | 7 days | 학생의 모든 UI 이벤트 로그 |
| `help-requests` | 2 | 2 | 30 days | 도움 요청 (M-GPT 분석 트리거) |
| `help-responses` | 2 | 2 | 30 days | M-GPT 분석 결과 및 도움 응답 |
| `progress-updates` | 3 | 2 | 30 days | 학습 진행 상태 변경 |
| `instructor-notifications` | 1 | 2 | 7 days | 강사 실시간 알림 |
| `system-events` | 1 | 2 | 7 days | 시스템 이벤트 (오류, 경고 등) |

---

## Topic 1: `activity-logs`

**용도**: AccessibilityService가 수집한 모든 사용자 행동 로그

**Partitioning Key**: `user_id` (같은 사용자의 로그는 순서 보장)

**Message Schema (v2.0 - AccessibilityNodeInfo 추가)**:
```json
{
  "schema_version": "2.0",
  "message_id": "uuid-1234",
  "timestamp": "2025-10-28T10:05:30.123Z",
  "user_id": 1,
  "subtask_id": 1000,
  "event_type": "CLICK",
  "event_data": {
    "element_text": "확인",
    "element_class": "android.widget.Button",
    "view_id_resource_name": "com.kakao.talk:id/btn_confirm",
    "content_description": "확인 버튼",
    "is_clickable": true,
    "is_enabled": true,
    "bounds": {
      "left": 100,
      "top": 200,
      "right": 300,
      "bottom": 250
    }
  },
  "node_info": {
    "view_id_resource_name": "com.kakao.talk:id/btn_confirm",
    "class_name": "android.widget.Button",
    "text": "확인",
    "content_description": "확인 버튼",
    "is_clickable": true,
    "is_enabled": true,
    "is_focused": false,
    "is_selected": false,
    "is_scrollable": false,
    "is_password": false,
    "bounds": {
      "left": 100,
      "top": 200,
      "right": 300,
      "bottom": 250
    },
    "child_count": 0
  },
  "parent_node_info": {
    "class_name": "android.widget.LinearLayout",
    "child_count": 3,
    "is_scrollable": false
  },
  "screen_info": {
    "package_name": "com.kakao.talk",
    "class_name": "com.kakao.talk.activity.MainActivity",
    "window_title": "친구 목록"
  },
  "device_info": {
    "device_id": "device-uuid-5678",
    "os_version": "Android 13",
    "app_version": "1.0.0"
  },
  "is_sensitive_data": false,
  "session_id": "session-uuid-9012"
}
```

**Event Types (확장)**:
- `CLICK`: 클릭 이벤트
- `LONG_CLICK`: 길게 누르기
- `SCROLL`: 스크롤 이벤트
- `TEXT_INPUT`: 텍스트 입력
- `SCREEN_CHANGE`: 화면 전환
- `FOCUS`: 포커스 변경
- `SELECTION`: 항목 선택 (스피너, 리스트 등)

**새로 추가된 필드**:
- `node_info`: AccessibilityNodeInfo에서 추출한 상세 UI 정보
  - `view_id_resource_name`: 안드로이드 리소스 ID (예: com.kakao.talk:id/btn_send)
  - `content_description`: 접근성 설명
  - `is_clickable`, `is_enabled`, `is_focused` 등: UI 상태
  - `bounds`: 화면 상의 위치 및 크기
  - `child_count`: 자식 뷰 개수

- `parent_node_info`: 부모 노드 정보 (UI 계층 구조 분석용)

- `is_sensitive_data`: 민감 정보 여부 (비밀번호 필드, 은행 앱 등)

**이벤트 타입별 상세 스키마**:

### CLICK / LONG_CLICK
```json
{
  "event_type": "CLICK",
  "event_data": {
    "element_text": "전송",
    "element_class": "android.widget.Button",
    "view_id_resource_name": "btn_send",
    "is_clickable": true,
    "is_enabled": true
  }
}
```

### SCROLL
```json
{
  "event_type": "SCROLL",
  "event_data": {
    "scroll_x": 0,
    "scroll_y": 150,
    "max_scroll_x": 0,
    "max_scroll_y": 500,
    "element_class": "androidx.recyclerview.widget.RecyclerView"
  }
}
```

### TEXT_INPUT
```json
{
  "event_type": "TEXT_INPUT",
  "event_data": {
    "input_text": "안녕하세요",  // 민감 정보는 "[REDACTED]"로 마스킹
    "element_class": "android.widget.EditText",
    "view_id_resource_name": "edit_message",
    "is_password": false
  },
  "is_sensitive_data": false
}
```

### FOCUS
```json
{
  "event_type": "FOCUS",
  "event_data": {
    "element_text": "메시지 입력",
    "element_class": "android.widget.EditText",
    "view_id_resource_name": "edit_message",
    "is_focused": true
  }
}
```

### SELECTION
```json
{
  "event_type": "SELECTION",
  "event_data": {
    "element_text": "항목 2",
    "element_class": "android.widget.Spinner",
    "selected_index": 1
  }
}
```

**보안 고려사항**:
- 비밀번호 필드: 텍스트 수집하지 않음, `is_sensitive_data: true`로 마킹
- 은행 앱 등 민감 패키지: 전체 이벤트 수집 제외
- 민감 정보는 `[REDACTED]`로 마스킹

**Consumer**:
- `ActivityLogConsumer`: DB에 저장 및 분석용 전처리
- 로그 저장 시 `is_sensitive_data`가 true인 경우 추가 암호화 또는 즉시 삭제

---

## Topic 2: `help-requests`

**용도**: 학생의 도움 요청 (M-GPT 분석 트리거)

**Partitioning Key**: `help_request_id` (분석 작업 분산)

**Message Schema**:
```json
{
  "schema_version": "1.0",
  "message_id": "uuid-help-1234",
  "timestamp": "2025-10-28T10:10:00.000Z",
  "help_request_id": 5000,
  "user_id": 1,
  "subtask_id": 1001,
  "request_type": "MANUAL",
  "context_data": {
    "current_screen": {
      "package_name": "com.kakao.talk",
      "activity_name": "MainActivity",
      "screen_title": "친구 목록"
    },
    "recent_actions": [
      {
        "event_type": "CLICK",
        "timestamp": "2025-10-28T10:09:50.000Z",
        "element_text": "메시지"
      },
      {
        "event_type": "SCROLL",
        "timestamp": "2025-10-28T10:09:55.000Z"
      }
    ],
    "stuck_duration_seconds": 45,
    "previous_help_count": 2
  },
  "priority": "NORMAL"
}
```

**Request Types**:
- `MANUAL`: 사용자가 직접 "도움" 버튼 클릭
- `AUTO`: 시스템이 자동으로 감지 (예: 30초 이상 진행 없음)

**Priority Levels**:
- `HIGH`: 긴급 (1분 이상 진행 없음)
- `NORMAL`: 일반
- `LOW`: 자동 감지 (참고용)

**Consumer**:
- `MGPTAnalysisConsumer`: M-GPT 분석 수행 및 결과 저장

---

## Topic 3: `help-responses`

**용도**: M-GPT 분석 결과 및 도움 응답

**Partitioning Key**: `help_request_id`

**Message Schema**:
```json
{
  "schema_version": "1.0",
  "message_id": "uuid-response-1234",
  "timestamp": "2025-10-28T10:10:15.000Z",
  "help_request_id": 5000,
  "user_id": 1,
  "subtask_id": 1001,
  "analysis_result": {
    "problem_diagnosis": "사용자가 '친구' 탭을 찾지 못하고 있습니다",
    "root_cause": "UI 요소 인식 실패",
    "confidence_score": 0.85
  },
  "help_content": {
    "help_type": "TEXT",
    "text": "화면 상단의 '친구' 탭을 눌러보세요. 메시지를 보낼 친구를 찾을 수 있습니다.",
    "voice_guide_text": "화면 상단의 친구 탭을 눌러보세요",
    "highlight_element": {
      "type": "AREA",
      "coordinates": {"x": 100, "y": 50, "width": 80, "height": 60}
    }
  },
  "mgpt_metadata": {
    "model": "gpt-4",
    "tokens_used": 350,
    "processing_time_ms": 1200
  }
}
```

**Help Types**:
- `TEXT`: 텍스트 안내
- `VOICE`: 음성 안내
- `OVERLAY`: Overlay 하이라이트
- `VIDEO`: 비디오 가이드 (향후 확장)

**Consumer**:
- `HelpResponseConsumer`: DB 저장
- `WebSocketNotifier`: 실시간으로 앱에 전달

---

## Topic 4: `progress-updates`

**용도**: 학습 진행 상태 변경 (강사 Dashboard 실시간 업데이트용)

**Partitioning Key**: `lecture_id` (같은 강의의 업데이트는 순서 보장)

**Message Schema**:
```json
{
  "schema_version": "1.0",
  "message_id": "uuid-progress-1234",
  "timestamp": "2025-10-28T10:15:00.000Z",
  "user_id": 1,
  "lecture_id": 1,
  "task_id": 100,
  "subtask_id": 1001,
  "previous_status": "IN_PROGRESS",
  "new_status": "COMPLETED",
  "completion_time_seconds": 120,
  "help_count_for_this_subtask": 1,
  "attempts": 2
}
```

**Status Values**:
- `NOT_STARTED`: 시작 전
- `IN_PROGRESS`: 진행 중
- `COMPLETED`: 완료
- `HELP_NEEDED`: 도움 필요

**Consumer**:
- `ProgressUpdateConsumer`: DB 업데이트
- `InstructorNotificationConsumer`: 강사 Dashboard WebSocket 알림

---

## Topic 5: `instructor-notifications`

**용도**: 강사에게 전달할 실시간 알림

**Partitioning Key**: `instructor_id`

**Message Schema**:
```json
{
  "schema_version": "1.0",
  "message_id": "uuid-notif-1234",
  "timestamp": "2025-10-28T10:10:00.000Z",
  "instructor_id": 10,
  "notification_type": "HELP_REQUEST",
  "priority": "HIGH",
  "data": {
    "user_id": 1,
    "user_name": "김학생",
    "lecture_id": 1,
    "lecture_title": "카카오톡 기초",
    "subtask_id": 1001,
    "subtask_title": "친구 선택하기",
    "help_request_id": 5000
  },
  "message": "김학생님이 '친구 선택하기' 단계에서 도움을 요청했습니다"
}
```

**Notification Types**:
- `HELP_REQUEST`: 도움 요청
- `STUDENT_COMPLETED`: 학생이 강의 완료
- `STUDENT_STUCK`: 학생이 오랫동안 진행 안 함 (30분 이상)
- `SYSTEM_ALERT`: 시스템 경고

**Consumer**:
- `InstructorNotificationConsumer`: DB 저장 및 WebSocket 전송
- `PushNotificationService`: 모바일 푸시 알림 (선택사항)

---

## Topic 6: `system-events`

**용도**: 시스템 이벤트 및 오류 로그

**Partitioning Key**: `event_level`

**Message Schema**:
```json
{
  "schema_version": "1.0",
  "message_id": "uuid-system-1234",
  "timestamp": "2025-10-28T10:20:00.000Z",
  "event_level": "ERROR",
  "component": "MGPT_SERVICE",
  "event_type": "API_TIMEOUT",
  "message": "OpenAI API timeout after 30s",
  "metadata": {
    "help_request_id": 5000,
    "retry_count": 3,
    "error_code": "TIMEOUT"
  },
  "stack_trace": "..."
}
```

**Event Levels**:
- `INFO`: 정보
- `WARNING`: 경고
- `ERROR`: 오류
- `CRITICAL`: 심각한 오류

**Consumer**:
- `SystemLogConsumer`: DB 저장
- `AlertingService`: 임계값 초과 시 관리자 알림

---

## Producer/Consumer 구조

### Producers

1. **Android App (Kafka Producer)**
   - Topics: `activity-logs`, `help-requests`
   - 역할: 클라이언트 이벤트를 Kafka로 전송

2. **Backend API (Kafka Producer)**
   - Topics: `progress-updates`, `instructor-notifications`, `system-events`
   - 역할: API 이벤트를 Kafka로 전송

3. **M-GPT Service (Kafka Producer)**
   - Topics: `help-responses`, `system-events`
   - 역할: 분석 결과를 Kafka로 전송

---

### Consumers

1. **ActivityLogConsumer**
   - Subscribes: `activity-logs`
   - 역할: 로그를 PostgreSQL에 저장
   - 처리량: 1000 msg/sec

2. **MGPTAnalysisConsumer**
   - Subscribes: `help-requests`
   - 역할: M-GPT 분석 수행 및 결과 저장
   - 처리량: 50 msg/sec (OpenAI API 호출 포함)

3. **HelpResponseConsumer**
   - Subscribes: `help-responses`
   - 역할: 응답을 DB에 저장 및 WebSocket 전송
   - 처리량: 100 msg/sec

4. **ProgressUpdateConsumer**
   - Subscribes: `progress-updates`
   - 역할: 진행 상태 DB 업데이트
   - 처리량: 500 msg/sec

5. **InstructorNotificationConsumer**
   - Subscribes: `instructor-notifications`, `progress-updates`
   - 역할: 강사 Dashboard WebSocket 실시간 전송
   - 처리량: 200 msg/sec

6. **SystemLogConsumer**
   - Subscribes: `system-events`
   - 역할: 시스템 로그 저장 및 모니터링
   - 처리량: 500 msg/sec

---

## Kafka Consumer Group 설정

```yaml
# activity-logs consumer
group.id: activity-log-processor
enable.auto.commit: false
max.poll.records: 100
auto.offset.reset: earliest

# help-requests consumer
group.id: mgpt-analyzer
enable.auto.commit: false
max.poll.records: 10  # OpenAI API 호출 부담으로 적게 설정
auto.offset.reset: earliest
```

---

## Message Format 규칙

1. **모든 메시지는 JSON 형식**
2. **필수 필드**:
   - `schema_version`: 스키마 버전 (하위 호환성 관리)
   - `message_id`: 고유 메시지 ID (중복 방지)
   - `timestamp`: ISO 8601 형식 타임스탬프
3. **Key**: Partitioning을 위한 key (user_id, lecture_id 등)
4. **Compression**: Snappy 압축 사용

---

## 에러 처리 및 재시도

### Dead Letter Queue (DLQ)

처리 실패한 메시지를 별도 Topic으로 전송:

- `activity-logs-dlq`
- `help-requests-dlq`
- `help-responses-dlq`

### Retry Policy

```python
# MGPTAnalysisConsumer 예시
class MGPTAnalysisConsumer:
    MAX_RETRIES = 3
    RETRY_DELAY_SECONDS = [5, 10, 30]  # Exponential backoff

    def process_message(self, message):
        for attempt in range(self.MAX_RETRIES):
            try:
                result = self.analyze_with_mgpt(message)
                self.save_to_db(result)
                return  # Success
            except Exception as e:
                if attempt < self.MAX_RETRIES - 1:
                    time.sleep(self.RETRY_DELAY_SECONDS[attempt])
                else:
                    # Send to DLQ
                    self.send_to_dlq(message, error=str(e))
```

---

## 모니터링 및 알림

### Kafka Metrics

- **Lag**: Consumer lag 모니터링 (임계값: 1000 messages)
- **Throughput**: 초당 메시지 처리량
- **Error Rate**: 에러 발생률 (임계값: 5%)

### Alerts

- Consumer lag > 1000: 경고
- Consumer lag > 10000: 심각
- Error rate > 5%: 경고
- DLQ에 메시지 100개 이상: 심각

---

## 확장성 고려사항

1. **Partition 증가**: 트래픽 증가 시 파티션 추가
2. **Consumer 스케일링**: Consumer 인스턴스 추가 (Kubernetes 기반)
3. **Retention Policy**: 오래된 로그는 S3로 아카이빙
4. **Compaction**: `instructor-notifications`는 log compaction 활용

---

## Kafka 클러스터 설정 예시

```properties
# Broker 설정
num.partitions=3
default.replication.factor=2
min.insync.replicas=2
compression.type=snappy
log.retention.hours=168  # 7 days
log.segment.bytes=1073741824  # 1GB

# Topic별 설정 오버라이드
# activity-logs
retention.ms=604800000  # 7 days
compression.type=snappy

# help-requests
retention.ms=2592000000  # 30 days
cleanup.policy=delete

# instructor-notifications
retention.ms=604800000  # 7 days
cleanup.policy=delete
```

---

## 요약

Kafka는 다음을 담당합니다:
1. **비동기 로그 수집**: 대량의 행동 로그를 효율적으로 저장
2. **M-GPT 분석 파이프라인**: 도움 요청 → 분석 → 응답 전달
3. **실시간 알림**: 강사 Dashboard 실시간 업데이트
4. **시스템 모니터링**: 오류 및 이벤트 로깅

이 구조는 확장 가능하고, 장애에 강하며, 실시간 처리가 가능합니다.
