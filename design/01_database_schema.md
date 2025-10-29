# 데이터베이스 스키마 설계

## ERD 개요

전체 시스템의 핵심 엔티티는 다음과 같습니다:
- 사용자 관리: User
- 강의 관리: Lecture, Task, Subtask
- **실시간 강의방**: LectureSession, SessionParticipant, SessionStepControl
- 로그 및 분석: ActivityLog, MGptAnalysis, HelpRequest
- 진행 상태: UserProgress

---

## 1. User (사용자)

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(20) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    age INT,
    role VARCHAR(20) NOT NULL, -- 'INSTRUCTOR', 'STUDENT'
    digital_level VARCHAR(20), -- 'BEGINNER', 'INTERMEDIATE', 'ADVANCED'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP
);

CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_email ON users(email);
```

**필드 설명**:
- `role`: 강사(INSTRUCTOR) vs 학생(STUDENT)
- `digital_level`: 학생의 디지털 숙련도

---

## 2. Lecture (강의)

```sql
CREATE TABLE lectures (
    id BIGSERIAL PRIMARY KEY,
    instructor_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    thumbnail_url VARCHAR(500),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_lectures_instructor ON lectures(instructor_id);
CREATE INDEX idx_lectures_active ON lectures(is_active);
```

**설명**: 강사가 생성한 강의 정보

---

## 3. Task (과제)

```sql
CREATE TABLE tasks (
    id BIGSERIAL PRIMARY KEY,
    lecture_id BIGINT NOT NULL REFERENCES lectures(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    order_index INT NOT NULL, -- 강의 내 순서
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(lecture_id, order_index)
);

CREATE INDEX idx_tasks_lecture ON tasks(lecture_id);
```

**설명**: 강의 내의 과제 (예: "카카오톡으로 메시지 보내기")

---

## 4. Subtask (세부 단계)

```sql
CREATE TABLE subtasks (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    order_index INT NOT NULL, -- 과제 내 순서
    target_action VARCHAR(100), -- 'CLICK', 'SCROLL', 'INPUT', 'NAVIGATE'
    target_element_hint TEXT, -- UI 요소 힌트 (예: "카카오톡 아이콘")
    guide_text TEXT, -- Overlay에 표시할 안내 문구
    voice_guide_text TEXT, -- TTS용 음성 안내 문구
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(task_id, order_index)
);

CREATE INDEX idx_subtasks_task ON subtasks(task_id);
```

**설명**: Task를 구성하는 세부 단계 (예: "1. 카카오톡 앱 열기", "2. 친구 선택하기")

---

## 5. UserLectureEnrollment (수강 등록)

```sql
CREATE TABLE user_lecture_enrollments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    lecture_id BIGINT NOT NULL REFERENCES lectures(id) ON DELETE CASCADE,
    enrolled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    UNIQUE(user_id, lecture_id)
);

CREATE INDEX idx_enrollment_user ON user_lecture_enrollments(user_id);
CREATE INDEX idx_enrollment_lecture ON user_lecture_enrollments(lecture_id);
```

**설명**: 학생이 어떤 강의를 수강 중인지 기록

---

## 6. LectureSession (실시간 강의방)

```sql
CREATE TABLE lecture_sessions (
    id BIGSERIAL PRIMARY KEY,
    lecture_id BIGINT NOT NULL REFERENCES lectures(id) ON DELETE CASCADE,
    instructor_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    session_code VARCHAR(20) UNIQUE NOT NULL, -- QR 코드용 짧은 코드 (예: "ABC123")
    status VARCHAR(20) NOT NULL DEFAULT 'WAITING', -- 'WAITING', 'IN_PROGRESS', 'ENDED', 'REVIEW_MODE'
    current_subtask_id BIGINT REFERENCES subtasks(id) ON DELETE SET NULL, -- 현재 진행 중인 단계
    qr_code_url VARCHAR(500), -- QR 코드 이미지 URL (선택적)
    scheduled_at TIMESTAMP, -- 예정 시각
    started_at TIMESTAMP, -- 실제 시작 시각
    ended_at TIMESTAMP, -- 실제 종료 시각
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sessions_lecture ON lecture_sessions(lecture_id);
CREATE INDEX idx_sessions_instructor ON lecture_sessions(instructor_id);
CREATE INDEX idx_sessions_code ON lecture_sessions(session_code);
CREATE INDEX idx_sessions_status ON lecture_sessions(status);
CREATE INDEX idx_sessions_scheduled ON lecture_sessions(scheduled_at);
```

**필드 설명**:
- `session_code`: QR 코드에 포함될 짧은 입장 코드 (예: "ABC123")
- `status`:
  - `WAITING`: 대기실 (학생들이 입장 중, 강사가 아직 시작 안 함)
  - `IN_PROGRESS`: 강의 진행 중
  - `ENDED`: 강의 종료
  - `REVIEW_MODE`: 복습 모드 (강의는 끝났지만 학생들이 복습 가능)
- `current_subtask_id`: 강사가 현재 진행 중인 단계 (동기식 제어)

**사용 예시**:
```sql
-- 강의방 생성
INSERT INTO lecture_sessions (lecture_id, instructor_id, title, session_code)
VALUES (1, 10, '2024-03-01 오전반', 'ABC123');

-- 강의 시작 (상태 변경 + 첫 번째 단계 설정)
UPDATE lecture_sessions
SET status = 'IN_PROGRESS', started_at = NOW(), current_subtask_id = 100
WHERE id = 1;

-- 다음 단계로 이동
UPDATE lecture_sessions
SET current_subtask_id = 101, updated_at = NOW()
WHERE id = 1;

-- 강의 종료 후 복습 모드로 전환
UPDATE lecture_sessions
SET status = 'REVIEW_MODE', ended_at = NOW()
WHERE id = 1;
```

---

## 7. SessionParticipant (세션 참가자)

```sql
CREATE TABLE session_participants (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES lecture_sessions(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'WAITING', -- 'WAITING', 'ACTIVE', 'COMPLETED', 'DISCONNECTED'
    current_subtask_id BIGINT REFERENCES subtasks(id) ON DELETE SET NULL, -- 참가자의 현재 단계
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 입장 시각
    last_active_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 마지막 활동 시각 (연결 상태 체크)
    completed_at TIMESTAMP, -- 모든 단계 완료 시각
    UNIQUE(session_id, user_id)
);

CREATE INDEX idx_participants_session ON session_participants(session_id);
CREATE INDEX idx_participants_user ON session_participants(user_id);
CREATE INDEX idx_participants_status ON session_participants(status);
CREATE INDEX idx_participants_last_active ON session_participants(last_active_at);
```

**필드 설명**:
- `status`:
  - `WAITING`: 대기실에서 강사 시작 대기 중
  - `ACTIVE`: 현재 학습 진행 중
  - `COMPLETED`: 모든 단계 완료
  - `DISCONNECTED`: 연결 끊김 (5분 이상 활동 없음)
- `current_subtask_id`: 강사가 지정한 단계와 동일 (동기식)
- `last_active_at`: WebSocket 연결 상태 체크용 (heartbeat)

**사용 예시**:
```sql
-- QR 입장 시 참가자 추가
INSERT INTO session_participants (session_id, user_id, status)
VALUES (1, 20, 'WAITING');

-- 강의 시작 시 모든 참가자 상태 변경
UPDATE session_participants
SET status = 'ACTIVE', current_subtask_id = 100
WHERE session_id = 1 AND status = 'WAITING';

-- 강사가 다음 단계로 진행 시 모든 참가자 동기화
UPDATE session_participants
SET current_subtask_id = 101
WHERE session_id = 1 AND status = 'ACTIVE';

-- 비활성 참가자 자동 표시 (5분 이상 활동 없음)
UPDATE session_participants
SET status = 'DISCONNECTED'
WHERE last_active_at < NOW() - INTERVAL '5 minutes'
  AND status = 'ACTIVE';
```

---

## 8. SessionStepControl (강사의 단계 제어 기록)

```sql
CREATE TABLE session_step_control (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES lecture_sessions(id) ON DELETE CASCADE,
    subtask_id BIGINT NOT NULL REFERENCES subtasks(id) ON DELETE CASCADE,
    instructor_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    action VARCHAR(20) NOT NULL, -- 'START_STEP', 'END_STEP', 'PAUSE', 'RESUME', 'SKIP'
    message TEXT, -- 강사의 추가 메시지 (선택적, 학생들에게 표시)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_step_control_session ON session_step_control(session_id);
CREATE INDEX idx_step_control_subtask ON session_step_control(subtask_id);
CREATE INDEX idx_step_control_created ON session_step_control(created_at);
```

**필드 설명**:
- `action`:
  - `START_STEP`: 새 단계 시작
  - `END_STEP`: 현재 단계 종료 (다음 단계로 이동 전)
  - `PAUSE`: 일시 정지 (학생들 대기)
  - `RESUME`: 재개
  - `SKIP`: 단계 건너뛰기
- `message`: 강사가 학생들에게 보내는 메시지 (예: "이 단계는 천천히 따라해보세요")

**사용 예시**:
```sql
-- 강사가 1단계 시작
INSERT INTO session_step_control (session_id, subtask_id, instructor_id, action, message)
VALUES (1, 100, 10, 'START_STEP', '첫 번째 단계입니다. 천천히 따라해보세요.');

-- 강사가 다음 단계로 진행
INSERT INTO session_step_control (session_id, subtask_id, instructor_id, action)
VALUES (1, 101, 10, 'START_STEP');

-- 강사가 일시 정지
INSERT INTO session_step_control (session_id, subtask_id, instructor_id, action, message)
VALUES (1, 101, 10, 'PAUSE', '잠시 기다려주세요. 질문 받겠습니다.');
```

**분석 쿼리**:
```sql
-- 특정 세션의 단계 진행 기록 조회
SELECT ssc.*, s.title, u.name as instructor_name
FROM session_step_control ssc
JOIN subtasks s ON ssc.subtask_id = s.id
JOIN users u ON ssc.instructor_id = u.id
WHERE ssc.session_id = :session_id
ORDER BY ssc.created_at;
```

---

## 9. UserProgress (진행 상태)

```sql
CREATE TABLE user_progress (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subtask_id BIGINT NOT NULL REFERENCES subtasks(id) ON DELETE CASCADE,
    session_id BIGINT REFERENCES lecture_sessions(id) ON DELETE SET NULL, -- 어느 세션에서의 진행인지
    status VARCHAR(20) NOT NULL, -- 'NOT_STARTED', 'IN_PROGRESS', 'COMPLETED', 'HELP_NEEDED'
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    attempts INT DEFAULT 0, -- 시도 횟수
    help_count INT DEFAULT 0, -- 도움 요청 횟수
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, subtask_id, session_id)
);

CREATE INDEX idx_progress_user ON user_progress(user_id);
CREATE INDEX idx_progress_subtask ON user_progress(subtask_id);
CREATE INDEX idx_progress_session ON user_progress(session_id);
CREATE INDEX idx_progress_status ON user_progress(status);
```

**설명**: 학생이 각 Subtask를 어디까지 진행했는지 추적

**변경사항 (실시간 강의방 지원)**:
- `session_id` 추가: 어느 강의 세션에서의 진행인지 기록
- UNIQUE 제약 조건 변경: `(user_id, subtask_id)` → `(user_id, subtask_id, session_id)`
  - 이유: 한 학생이 여러 세션에서 동일한 subtask를 다시 학습할 수 있음

---

## 10. ActivityLog (행동 로그)

```sql
CREATE TABLE activity_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subtask_id BIGINT REFERENCES subtasks(id) ON DELETE SET NULL,
    session_id BIGINT REFERENCES lecture_sessions(id) ON DELETE SET NULL, -- 어느 세션에서의 로그인지
    event_type VARCHAR(50) NOT NULL, -- 'CLICK', 'LONG_CLICK', 'SCROLL', 'TEXT_INPUT', 'SCREEN_CHANGE', 'FOCUS', 'SELECTION'
    event_data JSONB, -- 이벤트 상세 정보 (클릭 좌표, 입력 텍스트 등)
    screen_info JSONB, -- 현재 화면 정보 (패키지명, 액티비티명 등)
    node_info JSONB, -- AccessibilityNodeInfo 정보 (viewId, className, bounds 등)
    parent_node_info JSONB, -- 부모 노드 정보
    view_id_resource_name VARCHAR(255), -- 뷰 ID 리소스 이름 (빠른 검색용)
    content_description TEXT, -- 접근성 설명
    is_sensitive_data BOOLEAN DEFAULT false, -- 민감 정보 여부 (비밀번호 필드 등)
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 기본 인덱스
CREATE INDEX idx_activity_logs_user ON activity_logs(user_id);
CREATE INDEX idx_activity_logs_subtask ON activity_logs(subtask_id);
CREATE INDEX idx_activity_logs_session ON activity_logs(session_id);
CREATE INDEX idx_activity_logs_timestamp ON activity_logs(timestamp);

-- JSONB GIN 인덱스
CREATE INDEX idx_activity_logs_event_data ON activity_logs USING GIN(event_data);
CREATE INDEX idx_activity_logs_screen_info ON activity_logs USING GIN(screen_info);
CREATE INDEX idx_activity_logs_node_info ON activity_logs USING GIN(node_info);

-- 특정 JSONB 필드에 대한 표현식 인덱스 (자주 조회하는 필드)
CREATE INDEX idx_activity_logs_package
    ON activity_logs ((screen_info->>'package_name'));

CREATE INDEX idx_activity_logs_element_class
    ON activity_logs ((event_data->>'element_class'));

-- viewId로 빠른 검색
CREATE INDEX idx_activity_logs_view_id ON activity_logs(view_id_resource_name);

-- 이벤트 타입별 인덱스
CREATE INDEX idx_activity_logs_event_type ON activity_logs(event_type);

-- 민감 데이터 필터링용
CREATE INDEX idx_activity_logs_sensitive ON activity_logs(is_sensitive_data);
```

**설명**: AccessibilityService가 수집한 모든 UI 이벤트 저장

**확장된 필드**:
- `node_info`: AccessibilityNodeInfo에서 추출한 상세 정보
  ```json
  {
    "view_id_resource_name": "btn_send",
    "class_name": "android.widget.Button",
    "text": "전송",
    "content_description": "전송 버튼",
    "is_clickable": true,
    "is_enabled": true,
    "is_focused": false,
    "is_selected": false,
    "is_scrollable": false,
    "is_password": false,
    "bounds": {"left": 100, "top": 200, "right": 300, "bottom": 250},
    "child_count": 0
  }
  ```

- `parent_node_info`: 부모 노드의 구조 정보 (UI 계층 분석용)

- `view_id_resource_name`: 빠른 검색을 위한 별도 컬럼 (node_info에도 있지만 인덱싱 최적화)

- `is_sensitive_data`: 비밀번호 필드, 은행 앱 등 민감 정보 마킹

**Event Types (확장)**:
- `CLICK`: 클릭
- `LONG_CLICK`: 길게 누르기
- `SCROLL`: 스크롤
- `TEXT_INPUT`: 텍스트 입력
- `SCREEN_CHANGE`: 화면 전환
- `FOCUS`: 포커스 변경
- `SELECTION`: 항목 선택 (스피너, 리스트 등)

---

## 11. HelpRequest (도움 요청)

```sql
CREATE TABLE help_requests (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subtask_id BIGINT REFERENCES subtasks(id) ON DELETE SET NULL,
    session_id BIGINT REFERENCES lecture_sessions(id) ON DELETE SET NULL, -- 어느 세션에서의 도움 요청인지
    request_type VARCHAR(20) NOT NULL, -- 'MANUAL' (사용자 직접), 'AUTO' (시스템 감지)
    context_data JSONB, -- 요청 시점의 컨텍스트 (현재 화면, 최근 로그 등)
    status VARCHAR(20) NOT NULL, -- 'PENDING', 'ANALYZING', 'RESOLVED'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP
);

CREATE INDEX idx_help_requests_user ON help_requests(user_id);
CREATE INDEX idx_help_requests_session ON help_requests(session_id);
CREATE INDEX idx_help_requests_status ON help_requests(status);
CREATE INDEX idx_help_requests_created ON help_requests(created_at);
```

**설명**: 학생의 도움 요청 기록

**변경사항 (실시간 강의방 지원)**:
- `session_id` 추가: 어느 강의 세션에서의 도움 요청인지 기록
- 강사는 실시간으로 어느 세션에서 도움 요청이 많은지 확인 가능

---

## 12. MGptAnalysis (M-GPT 분석 결과)

```sql
CREATE TABLE mgpt_analyses (
    id BIGSERIAL PRIMARY KEY,
    help_request_id BIGINT NOT NULL REFERENCES help_requests(id) ON DELETE CASCADE,
    analysis_input JSONB NOT NULL, -- M-GPT에 전달한 입력 (로그, 컨텍스트)
    analysis_output JSONB NOT NULL, -- M-GPT의 분석 결과
    problem_diagnosis TEXT, -- 문제 진단 (예: "사용자가 버튼을 찾지 못함")
    suggested_help TEXT, -- 추천 도움말
    confidence_score FLOAT, -- 분석 신뢰도 (0.0 ~ 1.0)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_mgpt_analyses_help_request ON mgpt_analyses(help_request_id);
```

**설명**: M-GPT가 분석한 결과 저장

---

## 13. HelpResponse (제공된 도움)

```sql
CREATE TABLE help_responses (
    id BIGSERIAL PRIMARY KEY,
    help_request_id BIGINT NOT NULL REFERENCES help_requests(id) ON DELETE CASCADE,
    mgpt_analysis_id BIGINT REFERENCES mgpt_analyses(id) ON DELETE SET NULL,
    help_type VARCHAR(20) NOT NULL, -- 'TEXT', 'VOICE', 'OVERLAY', 'VIDEO'
    help_content TEXT NOT NULL, -- 실제 제공된 도움 내용
    displayed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    feedback_rating INT, -- 1-5 점수
    feedback_text TEXT,
    feedback_at TIMESTAMP
);

CREATE INDEX idx_help_responses_request ON help_responses(help_request_id);
```

**설명**: 학생에게 제공된 도움과 피드백

---

## 14. InstructorNotification (강사 알림)

```sql
CREATE TABLE instructor_notifications (
    id BIGSERIAL PRIMARY KEY,
    instructor_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    notification_type VARCHAR(50) NOT NULL, -- 'HELP_REQUEST', 'STUDENT_COMPLETED', etc.
    related_user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    related_help_request_id BIGINT REFERENCES help_requests(id) ON DELETE CASCADE,
    message TEXT NOT NULL,
    is_read BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP
);

CREATE INDEX idx_notifications_instructor ON instructor_notifications(instructor_id);
CREATE INDEX idx_notifications_read ON instructor_notifications(is_read);
CREATE INDEX idx_notifications_created ON instructor_notifications(created_at);
```

**설명**: 강사에게 보낼 실시간 알림

---

## 15. SystemLog (시스템 로그)

```sql
CREATE TABLE system_logs (
    id BIGSERIAL PRIMARY KEY,
    log_level VARCHAR(20) NOT NULL, -- 'INFO', 'WARNING', 'ERROR'
    component VARCHAR(50) NOT NULL, -- 'KAFKA_CONSUMER', 'MGPT_SERVICE', etc.
    message TEXT NOT NULL,
    metadata JSONB,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_system_logs_level ON system_logs(log_level);
CREATE INDEX idx_system_logs_timestamp ON system_logs(timestamp);
```

**설명**: 시스템 운영 로그

---

## 관계도 요약

```
[강의 구조]
User (강사) 1:N → Lecture
Lecture 1:N → Task
Task 1:N → Subtask

[실시간 강의방] ⭐ NEW
Lecture 1:N → LectureSession (강의방)
User (강사) 1:N → LectureSession
LectureSession 1:N → SessionParticipant (참가자)
User (학생) 1:N → SessionParticipant
LectureSession 1:N → SessionStepControl (단계 제어 기록)

[학습 진행]
User (학생) M:N → Lecture (via UserLectureEnrollment)
User + Subtask + Session → UserProgress (진행 상태)

[로그 및 도움]
User + Subtask + Session → ActivityLog (행동 로그)
User + Subtask + Session → HelpRequest
HelpRequest 1:1 → MGptAnalysis
HelpRequest 1:N → HelpResponse

[알림]
User (강사) 1:N → InstructorNotification
```

---

## 주요 쿼리 시나리오

### 1. 강의방 생성 시 QR 코드 생성
```sql
INSERT INTO lecture_sessions (lecture_id, instructor_id, title, session_code, status)
VALUES (:lecture_id, :instructor_id, :title, :generated_code, 'WAITING')
RETURNING id, session_code;
```

### 2. QR 코드로 입장 (세션 조회)
```sql
SELECT ls.*, l.title as lecture_title, u.name as instructor_name
FROM lecture_sessions ls
JOIN lectures l ON ls.lecture_id = l.id
JOIN users u ON ls.instructor_id = u.id
WHERE ls.session_code = :session_code
  AND ls.status IN ('WAITING', 'IN_PROGRESS', 'REVIEW_MODE');
```

### 3. 대기실 참가자 실시간 목록
```sql
SELECT sp.*, u.name, u.email
FROM session_participants sp
JOIN users u ON sp.user_id = u.id
WHERE sp.session_id = :session_id
  AND sp.status = 'WAITING'
ORDER BY sp.joined_at;
```

### 4. 강의 시작 (모든 참가자 활성화 + 첫 단계 설정)
```sql
-- 세션 상태 변경
UPDATE lecture_sessions
SET status = 'IN_PROGRESS', started_at = NOW(), current_subtask_id = :first_subtask_id
WHERE id = :session_id;

-- 모든 대기 중인 참가자 활성화
UPDATE session_participants
SET status = 'ACTIVE', current_subtask_id = :first_subtask_id
WHERE session_id = :session_id AND status = 'WAITING';
```

### 5. 강사가 다음 단계로 진행
```sql
-- 세션의 현재 단계 업데이트
UPDATE lecture_sessions
SET current_subtask_id = :next_subtask_id, updated_at = NOW()
WHERE id = :session_id;

-- 모든 활성 참가자 동기화
UPDATE session_participants
SET current_subtask_id = :next_subtask_id
WHERE session_id = :session_id AND status = 'ACTIVE';

-- 제어 기록 생성
INSERT INTO session_step_control (session_id, subtask_id, instructor_id, action, message)
VALUES (:session_id, :next_subtask_id, :instructor_id, 'START_STEP', :message);
```

### 6. 실시간 강의방 모니터링 (강사용)
```sql
SELECT
    sp.user_id,
    u.name,
    sp.status,
    sp.current_subtask_id,
    s.title as current_step,
    sp.last_active_at,
    (SELECT COUNT(*) FROM help_requests hr
     WHERE hr.user_id = sp.user_id
       AND hr.session_id = :session_id
       AND hr.status = 'PENDING') as pending_help_count
FROM session_participants sp
JOIN users u ON sp.user_id = u.id
LEFT JOIN subtasks s ON sp.current_subtask_id = s.id
WHERE sp.session_id = :session_id
ORDER BY sp.joined_at;
```

### 7. 강의 종료 후 복습 모드 전환
```sql
UPDATE lecture_sessions
SET status = 'REVIEW_MODE', ended_at = NOW()
WHERE id = :session_id;
```

### 8. 특정 세션 내 특정 학생의 진행 상태 조회
```sql
SELECT up.*, s.title, s.guide_text, t.title as task_title
FROM user_progress up
JOIN subtasks s ON up.subtask_id = s.id
JOIN tasks t ON s.task_id = t.id
WHERE up.user_id = :user_id
  AND up.session_id = :session_id
  AND up.status = 'IN_PROGRESS'
ORDER BY up.updated_at DESC;
```

### 9. M-GPT 분석을 위한 최근 로그 조회 (세션별)
```sql
SELECT event_type, event_data, screen_info, node_info, timestamp
FROM activity_logs
WHERE user_id = :user_id
  AND session_id = :session_id
  AND subtask_id = :subtask_id
  AND timestamp > NOW() - INTERVAL '5 minutes'
  AND is_sensitive_data = false
ORDER BY timestamp DESC
LIMIT 50;
```

### 10. 세션별 도움 요청 통계
```sql
SELECT
    s.title as step_title,
    COUNT(*) as help_request_count,
    COUNT(DISTINCT hr.user_id) as unique_users
FROM help_requests hr
JOIN subtasks s ON hr.subtask_id = s.id
WHERE hr.session_id = :session_id
GROUP BY s.id, s.title
ORDER BY help_request_count DESC;
```

---

## 확장 고려사항

1. **데이터 파티셔닝**:
   - `activity_logs`: 시간 기반 파티셔닝 (월별/주별)
   - `lecture_sessions`: status별 파티셔닝 (ENDED/REVIEW_MODE는 별도 파티션)

2. **인덱스 최적화**:
   - JSONB 필드의 GIN 인덱스 활용
   - 세션 ID 복합 인덱스 추가 (user_id, session_id)

3. **캐싱**:
   - 강의/Task/Subtask는 Redis 캐싱
   - 현재 진행 중인 세션 정보는 Redis에 캐싱 (빠른 조회)
   - 대기실 참가자 목록은 Redis Set으로 관리

4. **아카이빙**:
   - 오래된 ActivityLog는 별도 스토리지로 이관
   - 종료된 세션(status='ENDED')은 일정 기간 후 아카이브

5. **세션 코드 생성**:
   - 짧고 입력하기 쉬운 코드 (6자리: 예 "ABC123")
   - 중복 방지 및 유효 기간 설정

6. **실시간 연결 관리**:
   - `session_participants.last_active_at` 주기적 업데이트 (heartbeat)
   - 5분 이상 활동 없으면 자동 DISCONNECTED 처리

이 스키마는 **실시간 동기식 강의방 시스템**을 위한 설계이며, 운영하면서 추가 최적화가 필요할 수 있습니다.
