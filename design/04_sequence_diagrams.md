# 핵심 플로우 시퀀스 다이어그램

## 1. 학습 시작 플로우

사용자가 강의를 선택하고 학습을 시작하는 과정

```mermaid
sequenceDiagram
    participant User as 학생
    participant App as Android App
    participant ViewModel as LearningViewModel
    participant Repo as ProgressRepository
    participant API as Backend API
    participant Overlay as OverlayManager
    participant Access as AccessibilityService

    User->>App: 강의 선택 및 "학습 시작" 클릭
    App->>ViewModel: loadCurrentSubtask(lectureId)
    ViewModel->>Repo: getCurrentSubtask(lectureId)

    alt 로컬 DB에 캐시 존재
        Repo->>ViewModel: Return cached subtask
        ViewModel->>App: Update UI (빠른 응답)
    end

    Repo->>API: GET /api/progress/me?lecture_id={id}
    API-->>Repo: CurrentSubtask + Progress Info
    Repo->>Repo: Save to Local DB
    Repo-->>ViewModel: Return Subtask

    ViewModel->>App: Update currentSubtask state
    App->>Overlay: showGuide(subtask)
    Overlay-->>User: Overlay 표시 (안내 문구)

    App->>Access: 확인 및 활성화 체크

    alt AccessibilityService 비활성화
        Access-->>User: 활성화 안내 다이얼로그
        User->>Access: 설정에서 활성화
    end

    Access-->>App: 활성화 완료
    App-->>User: 학습 준비 완료 안내
```

---

## 2. 로그 수집 및 Kafka 전송 플로우

사용자의 UI 이벤트를 감지하고 서버로 전송하는 과정

```mermaid
sequenceDiagram
    participant User as 학생
    participant Access as AccessibilityService
    participant Collector as EventCollector
    participant Repo as LogRepository
    participant Kafka as Kafka Producer
    participant Server as Backend
    participant DB as PostgreSQL
    participant Consumer as Kafka Consumer

    User->>User: UI 액션 (클릭, 스크롤 등)
    Access->>Access: onAccessibilityEvent() 트리거
    Access->>Collector: collectClickEvent(event)

    Collector->>Collector: Extract event data
    Note over Collector: - 좌표 (x, y)<br/>- 요소 정보<br/>- 화면 정보<br/>- 타임스탬프

    Collector-->>Access: ActivityLog 객체 반환
    Access->>Repo: sendActivityLog(log)

    alt 네트워크 연결 가능
        Repo->>Kafka: send(topic: "activity-logs", log)
        Kafka-->>Server: Kafka Topic으로 전송
        Server->>Consumer: Consume message
        Consumer->>DB: INSERT INTO activity_logs
        DB-->>Consumer: Success
        Consumer-->>Kafka: Commit offset
        Kafka-->>Repo: Success (ACK)
    else 네트워크 연결 불가
        Repo->>Repo: Save to Local DB
        Note over Repo: 나중에 재전송
        Repo-->>Access: Queued for retry
    end
```

---

## 3. 도움 요청 및 M-GPT 분석 플로우

사용자가 도움을 요청하고 AI 분석 결과를 받는 과정

```mermaid
sequenceDiagram
    participant User as 학생
    participant Overlay as Overlay UI
    participant ViewModel as LearningViewModel
    participant HelpRepo as HelpRepository
    participant API as Backend API
    participant Kafka as Kafka
    participant MGPTService as M-GPT Service
    participant OpenAI as OpenAI API
    participant DB as PostgreSQL

    User->>Overlay: "도움" 버튼 클릭
    Overlay->>ViewModel: requestHelp()

    ViewModel->>ViewModel: Get current context
    Note over ViewModel: - 현재 Subtask<br/>- 최근 로그<br/>- 화면 정보

    ViewModel->>HelpRepo: requestHelp(subtaskId, context)
    HelpRepo->>API: POST /api/help/request

    API->>DB: INSERT INTO help_requests
    DB-->>API: help_request_id

    API->>Kafka: Publish to "help-requests" topic
    Note over Kafka: {<br/>  help_request_id,<br/>  user_id,<br/>  subtask_id,<br/>  context_data<br/>}

    API-->>HelpRepo: {help_request_id, status: "ANALYZING"}
    HelpRepo-->>ViewModel: help_request_id

    Note over ViewModel: 폴링 시작 (2초마다)

    Kafka->>MGPTService: Consume "help-requests"
    MGPTService->>DB: SELECT recent logs
    DB-->>MGPTService: Activity logs

    MGPTService->>MGPTService: Build prompt
    Note over MGPTService: - Subtask 목표<br/>- 최근 로그<br/>- 현재 화면

    MGPTService->>OpenAI: Request analysis
    OpenAI-->>MGPTService: Analysis result
    Note over OpenAI: {<br/>  problem: "사용자가 버튼을 찾지 못함",<br/>  suggestion: "화면 상단의 ...",<br/>  confidence: 0.85<br/>}

    MGPTService->>DB: INSERT INTO mgpt_analyses
    MGPTService->>DB: INSERT INTO help_responses
    MGPTService->>DB: UPDATE help_requests SET status='RESOLVED'
    DB-->>MGPTService: Success

    loop 폴링 (2초마다)
        ViewModel->>HelpRepo: getHelpResponse(help_request_id)
        HelpRepo->>API: GET /api/help/request/{id}
        API->>DB: SELECT help_response

        alt 분석 완료
            DB-->>API: HelpResponse
            API-->>HelpRepo: {status: "RESOLVED", help_content: "..."}
            HelpRepo-->>ViewModel: HelpResponse

            ViewModel->>Overlay: showHelpResponse(content)
            Overlay-->>User: 도움말 표시 + TTS 음성 안내
            Note over ViewModel: 폴링 종료
        else 아직 분석 중
            DB-->>API: {status: "ANALYZING"}
            API-->>HelpRepo: Still analyzing
            Note over ViewModel: 계속 폴링
        end
    end

    User->>Overlay: 도움말 확인
    Overlay->>ViewModel: submitFeedback(rating)
    ViewModel->>HelpRepo: submitFeedback(help_response_id, rating)
    HelpRepo->>API: POST /api/help/feedback
    API->>DB: UPDATE help_responses SET feedback_rating
```

---

## 4. 진행 상태 업데이트 플로우

사용자가 Subtask를 완료하는 과정

```mermaid
sequenceDiagram
    participant User as 학생
    participant Access as AccessibilityService
    participant ViewModel as LearningViewModel
    participant Repo as ProgressRepository
    participant API as Backend API
    participant DB as PostgreSQL
    participant Overlay as OverlayManager
    participant Notif as Notification Service

    User->>User: Subtask 목표 달성 (예: 버튼 클릭 성공)
    Access->>Access: Detect target action
    Access->>ViewModel: onTargetActionDetected()

    Note over ViewModel: 자동 완료 또는<br/>사용자가 "다음" 클릭

    alt 사용자가 "다음" 버튼 클릭
        User->>Overlay: "다음 단계" 클릭
        Overlay->>ViewModel: completeCurrentSubtask()
    end

    ViewModel->>Repo: updateProgress(subtaskId, COMPLETED)
    Repo->>API: POST /api/progress/update

    API->>DB: UPDATE user_progress SET status='COMPLETED'
    API->>DB: SELECT next subtask
    DB-->>API: NextSubtask

    API-->>Repo: {status: "COMPLETED", next_subtask: {...}}
    Repo-->>ViewModel: NextSubtask

    ViewModel->>ViewModel: loadCurrentSubtask()
    ViewModel->>Overlay: showGuide(nextSubtask)
    Overlay-->>User: 다음 단계 안내 표시

    opt 강사에게 알림
        API->>Notif: Push notification to instructor
        Notif-->>Notif: "{학생} completed {subtask}"
    end
```

---

## 5. 강사 웹 Dashboard 실시간 모니터링 플로우

강사가 PC 웹 브라우저에서 수강생의 진행 상태를 실시간으로 확인하는 과정

```mermaid
sequenceDiagram
    participant Instructor as 강사 (PC 브라우저)
    participant Dashboard as 웹 Dashboard (Django)
    participant WS as WebSocket (Django Channels)
    participant Backend as Backend API
    participant Kafka as Kafka
    participant DB as PostgreSQL
    participant Consumer as Kafka Consumer

    Instructor->>Dashboard: 강의 Dashboard 페이지 접속
    Dashboard->>Backend: GET /api/dashboard/lectures/{id}/students
    Backend->>DB: SELECT students and progress
    DB-->>Backend: Student list + progress
    Backend-->>Dashboard: Initial data
    Dashboard-->>Instructor: 수강생 목록 및 진행률 표시 (HTML)

    Dashboard->>WS: Connect WebSocket /ws/dashboard/lecture/{lecture_id}/
    WS-->>Dashboard: Connection established
    Note over WS: 강사를 lecture_{id} 그룹에 추가

    Note over Dashboard,WS: HTMX로 5초마다 부분 업데이트

    par Student Progress Activity
        Note over Kafka: 학생이 Subtask 완료
        Consumer->>Kafka: Consume "progress-updates" topic
        Consumer->>DB: UPDATE user_progress SET status='COMPLETED'
        DB-->>Consumer: Success

        Consumer->>WS: channel_layer.group_send("lecture_1", {...})
        WS->>Dashboard: WebSocket message: {type: "progress_update", data: {...}}

        Dashboard-->>Instructor: 실시간 UI 업데이트 (진행률 바 갱신)
        Note over Dashboard: JavaScript로 Chart.js 차트 업데이트

    and Help Request Activity
        Consumer->>Kafka: Consume "help-requests" topic
        Consumer->>DB: INSERT INTO help_requests
        DB-->>Consumer: help_request_id

        Consumer->>WS: channel_layer.group_send("instructor_10_help", {...})
        WS->>Dashboard: {type: "help_request", data: {...}}

        Dashboard-->>Instructor: 알림 토스트 표시 + 오디오 알림
        Note over Dashboard: Bootstrap Toast + 알림음

        Instructor->>Dashboard: 알림 클릭
        Dashboard->>Backend: GET /api/help/request/{id}
        Backend->>DB: SELECT help_request + context + logs
        DB-->>Backend: Full context
        Backend-->>Dashboard: Help request details
        Dashboard-->>Instructor: 모달로 학생 상황 상세 표시
        Note over Dashboard: - 현재 화면 정보<br/>- 최근 행동 로그<br/>- M-GPT 분석 결과
    end

    Note over Dashboard: HTMX로 자동 갱신
    Dashboard->>Backend: (5초마다) GET /api/dashboard/htmx/students/{lecture_id}
    Backend->>DB: SELECT latest progress
    DB-->>Backend: Updated data
    Backend-->>Dashboard: HTML partial
    Dashboard-->>Instructor: 부분 DOM 교체 (새로고침 없이)
```

---

## 6. 강의 생성 플로우 (강사용 - 웹 Dashboard)

강사가 PC 웹에서 새로운 강의를 생성하고 Task/Subtask를 등록하는 과정

```mermaid
sequenceDiagram
    participant Instructor as 강사 (PC 브라우저)
    participant Dashboard as 웹 Dashboard
    participant API as Backend API
    participant DB as PostgreSQL

    Instructor->>Dashboard: 강의 목록 페이지에서 "새 강의 만들기" 클릭
    Dashboard-->>Instructor: 강의 생성 폼 페이지 렌더링 (Django Template)

    Instructor->>Dashboard: 강의 정보 입력<br/>- 제목<br/>- 설명<br/>- 카테고리<br/>- 난이도<br/>- 예상 소요 시간
    Dashboard->>API: POST /api/lectures
    API->>DB: INSERT INTO lectures
    DB-->>API: lecture_id
    API-->>Dashboard: {lecture_id, ...}
    Dashboard-->>Instructor: 강의 상세 페이지로 리다이렉트

    loop 각 Task 추가
        Instructor->>Dashboard: "Task 추가" 버튼 클릭
        Dashboard-->>Instructor: Task 입력 폼 (모달 또는 인라인)

        Instructor->>Dashboard: Task 정보 입력<br/>- 제목<br/>- 설명<br/>- 순서
        Dashboard->>API: POST /api/lectures/{id}/tasks
        API->>DB: INSERT INTO tasks
        DB-->>API: task_id
        API-->>Dashboard: {task_id, ...}
        Dashboard-->>Instructor: Task 카드 추가 (HTMX로 부분 업데이트)

        loop 각 Subtask 추가
            Instructor->>Dashboard: Task 카드에서 "Subtask 추가" 클릭
            Dashboard-->>Instructor: Subtask 입력 폼 표시

            Note over Instructor: 입력 항목:<br/>- 제목/설명<br/>- 목표 액션 (CLICK, INPUT 등)<br/>- 목표 요소 힌트<br/>- 안내 문구 (텍스트)<br/>- 음성 안내 문구<br/>- 검증 방식 (AUTO/MANUAL)

            Instructor->>Dashboard: Subtask 정보 입력
            Dashboard->>API: POST /api/tasks/{id}/subtasks
            API->>DB: INSERT INTO subtasks
            DB-->>API: subtask_id
            API-->>Dashboard: {subtask_id, ...}
            Dashboard-->>Instructor: Subtask 리스트에 추가 (HTMX)
        end

        Note over Dashboard: Task 내의 Subtask 순서는<br/>드래그 앤 드롭으로 조정 가능
    end

    Instructor->>Dashboard: "강의 미리보기" 버튼 클릭
    Dashboard-->>Instructor: Task/Subtask 구조 미리보기

    Instructor->>Dashboard: "강의 공개" 버튼 클릭
    Dashboard->>Dashboard: Form validation (필수 항목 체크)

    Dashboard->>API: PUT /api/lectures/{id}<br/>{is_active: true}
    API->>DB: UPDATE lectures SET is_active=true
    DB-->>API: Success
    API-->>Dashboard: Success response

    Dashboard-->>Instructor: 성공 메시지 + 강의 목록으로 이동
    Note over Dashboard: "강의가 공개되었습니다.<br/>학생들이 수강 신청할 수 있습니다."
```

---

## 7. 배치 로그 동기화 플로우

네트워크가 끊겼다가 복구된 후 로컬 DB의 로그를 서버로 전송

```mermaid
sequenceDiagram
    participant App as Android App
    participant SyncService as LogSyncService
    participant LocalDB as Room DB
    participant Kafka as Kafka Producer
    participant Backend as Backend Server

    App->>App: Network state change (CONNECTED)
    App->>SyncService: startSync()

    SyncService->>LocalDB: SELECT unsent logs
    LocalDB-->>SyncService: List<ActivityLog>

    alt 로그가 없음
        SyncService-->>App: No logs to sync
    else 로그 존재
        loop 배치 처리 (50개씩)
            SyncService->>Kafka: sendBatch(logs)

            alt 전송 성공
                Kafka-->>Backend: Logs delivered
                Backend-->>Kafka: ACK
                Kafka-->>SyncService: Success
                SyncService->>LocalDB: DELETE synced logs
                LocalDB-->>SyncService: Deleted
            else 전송 실패
                Kafka-->>SyncService: Error
                SyncService->>LocalDB: Mark as retry
                Note over SyncService: 다음 동기화 때 재시도
            end
        end

        SyncService-->>App: Sync completed
    end
```

---

## 8. M-GPT 분석 파이프라인 (Backend)

Kafka Consumer가 도움 요청을 받아 M-GPT로 분석하는 과정

```mermaid
sequenceDiagram
    participant Kafka as Kafka Topic
    participant Consumer as M-GPT Consumer
    participant DB as PostgreSQL
    participant MGPTService as M-GPT Service
    participant OpenAI as OpenAI API
    participant Cache as Redis Cache

    Kafka->>Consumer: New message in "help-requests"
    Consumer->>Consumer: Deserialize message

    Consumer->>DB: SELECT help_request details
    DB-->>Consumer: {user_id, subtask_id, context_data}

    Consumer->>DB: SELECT subtask goal and description
    DB-->>Consumer: Subtask details

    Consumer->>DB: SELECT recent activity_logs (last 5 min)
    DB-->>Consumer: List<ActivityLog>

    Consumer->>MGPTService: analyze(subtask, logs, context)

    MGPTService->>MGPTService: Build prompt
    Note over MGPTService: System: "당신은 시니어를 돕는 AI입니다"<br/>User: "목표: {subtask.title}<br/>최근 행동: {logs}<br/>현재 상황: {context}<br/>문제를 진단하고 도움말을 제공하세요"

    alt Cache에 유사한 케이스 존재
        MGPTService->>Cache: GET similar_case_hash
        Cache-->>MGPTService: Cached response
        MGPTService-->>Consumer: Use cached response
    else 새로운 케이스
        MGPTService->>OpenAI: POST /v1/chat/completions
        OpenAI-->>MGPTService: {problem, suggestion, confidence}

        MGPTService->>Cache: SET similar_case_hash
        Cache-->>MGPTService: Cached

        MGPTService-->>Consumer: Analysis result
    end

    Consumer->>DB: INSERT INTO mgpt_analyses
    Consumer->>DB: INSERT INTO help_responses
    Consumer->>DB: UPDATE help_requests SET status='RESOLVED'
    DB-->>Consumer: Success

    Consumer->>Kafka: Publish to "help-responses" topic
    Note over Kafka: WebSocket 서버가 consume하여<br/>실시간으로 앱에 전달

    Consumer->>Consumer: Commit offset
```

---

## 플로우 요약

1. **학습 시작**: 강의 선택 → 현재 Subtask 로드 → Overlay 표시 → AccessibilityService 활성화
2. **로그 수집**: UI 이벤트 감지 → Kafka 전송 → DB 저장 (실패 시 로컬 저장 후 재전송)
3. **도움 요청**: 버튼 클릭 → API 요청 → Kafka → M-GPT 분석 → 응답 폴링 → Overlay 표시
4. **진행 업데이트**: Subtask 완료 → API 업데이트 → 다음 Subtask 로드 → Overlay 갱신
5. **강사 모니터링**: WebSocket 연결 → 실시간 업데이트 수신 → Dashboard 갱신
6. **강의 생성**: 강의 생성 → Task 추가 → Subtask 추가 → 공개
7. **배치 동기화**: 네트워크 복구 → 로컬 DB 조회 → 배치 전송 → 삭제
8. **M-GPT 파이프라인**: Kafka consume → 로그 조회 → OpenAI 분석 → 결과 저장 → 응답 전달

---

이 시퀀스 다이어그램들은 Mermaid로 렌더링할 수 있으며, 각 플로우의 흐름을 명확히 보여줍니다.
