# Android 학생 앱 화면 흐름도

MobileGPT Android 학생 앱의 화면 구조, 사용자 플로우, 주요 기능을 정의하는 문서입니다.

## 목차
1. [개요](#개요)
2. [화면 구조](#화면-구조)
3. [사용자 플로우](#사용자-플로우)
4. [화면별 상세 설명](#화면별-상세-설명)
5. [WebSocket 통신 흐름](#websocket-통신-흐름)
6. [AccessibilityService 동작](#accessibilityservice-동작)
7. [에러 처리](#에러-처리)

---

## 개요

### 앱 정보
- **대상 사용자**: 60대 이상 시니어 학습자
- **플랫폼**: Android (최소 API 30, Android 11.0 이상)
- **기술 스택**: Kotlin, Jetpack Compose, Material Design 3
- **아키텍처**: MVVM + Clean Architecture

### 주요 기능
1. JWT 기반 로그인
2. 세션 코드로 강의 참가
3. WebSocket 실시간 통신
4. UI 이벤트 자동 수집 (AccessibilityService)
5. 단계별 학습 진행

---

## 화면 구조

### 화면 목록

```mermaid
graph TD
    A[앱 시작] --> B[LoginScreen]
    B -->|로그인 성공| C[SessionCodeScreen]
    C -->|세션 참가 성공| D[SessionScreen]
    D -->|세션 종료| C
    C -->|로그아웃| B

    style B fill:#E3F2FD
    style C fill:#E8F5E9
    style D fill:#FFF3E0
```

### 내비게이션 구조

| 화면 | Route | 설명 |
|------|-------|------|
| LoginScreen | `/login` | 초기 로그인 화면 |
| SessionCodeScreen | `/session-code` | 세션 코드 입력 화면 |
| SessionScreen | `/session/{sessionId}` | 세션 진행 화면 |

---

## 사용자 플로우

### 전체 사용자 여정

```mermaid
flowchart TD
    Start([앱 실행]) --> CheckAuth{인증 상태 확인}

    CheckAuth -->|토큰 없음| Login[LoginScreen]
    CheckAuth -->|토큰 있음| SessionCode[SessionCodeScreen]

    Login --> InputCreds[이메일/비밀번호 입력]
    InputCreds --> SubmitLogin[로그인 버튼 클릭]
    SubmitLogin --> ValidateLogin{인증 성공?}

    ValidateLogin -->|실패| ShowError1[에러 메시지 표시]
    ShowError1 --> Login
    ValidateLogin -->|성공| SaveToken[JWT 토큰 저장]
    SaveToken --> SessionCode

    SessionCode --> InputCode[6자리 세션 코드 입력]
    InputCode --> SubmitCode[참가 버튼 클릭]
    SubmitCode --> ValidateCode{세션 참가 성공?}

    ValidateCode -->|실패| ShowError2[에러 메시지 표시]
    ShowError2 --> SessionCode
    ValidateCode -->|성공| SaveSession[세션 정보 저장]
    SaveSession --> Session[SessionScreen]

    Session --> ConnectWS[WebSocket 연결]
    ConnectWS --> EnableAccessibility[AccessibilityService 활성화]
    EnableAccessibility --> Learning[학습 진행]

    Learning --> Heartbeat[하트비트 전송]
    Learning --> StepComplete[단계 완료]
    Learning --> HelpRequest[도움 요청]
    Learning --> SessionEnd{세션 종료?}

    SessionEnd -->|종료| Disconnect[WebSocket 연결 해제]
    Disconnect --> SessionCode
    SessionEnd -->|계속| Learning

    style Login fill:#E3F2FD
    style SessionCode fill:#E8F5E9
    style Session fill:#FFF3E0
    style Learning fill:#FFE0B2
```

---

## 화면별 상세 설명

### 1. LoginScreen (로그인 화면)

#### 목적
- 학생 인증 및 JWT 토큰 획득
- 최초 1회 로그인 후 토큰 저장

#### UI 구성

```mermaid
graph TB
    subgraph LoginScreen
        Title[앱 타이틀<br/>MobileGPT Student]
        EmailField[이메일 입력 필드<br/>TextField]
        PasswordField[비밀번호 입력 필드<br/>TextField - Password Type]
        LoginButton[로그인 버튼<br/>Primary Button]
        ErrorText[에러 메시지 영역<br/>Optional, Red Text]
        LoadingIndicator[로딩 인디케이터<br/>Optional, CircularProgress]
    end

    Title --> EmailField
    EmailField --> PasswordField
    PasswordField --> LoginButton
    LoginButton --> ErrorText
    LoginButton --> LoadingIndicator
```

#### 필드 정보

| 요소 | 타입 | 검증 규칙 | 기본값 |
|------|------|-----------|--------|
| 이메일 | TextField | 이메일 형식, 필수 | student1@test.com |
| 비밀번호 | TextField (Password) | 최소 8자, 필수 | TestStudent123!@# |

#### 주요 액션

1. **로그인 버튼 클릭**
   ```
   입력값 검증 → API 요청 → 응답 처리
   ```
   - API: `POST /api/token/`
   - Body: `{ "email": "...", "password": "..." }`
   - 성공 시: JWT 토큰 저장 → SessionCodeScreen 이동
   - 실패 시: 에러 메시지 표시

2. **입력 필드 변경**
   - 실시간 에러 메시지 제거

#### 상태 관리 (LoginViewModel)

```kotlin
data class LoginUiState(
    val email: String = "student1@test.com",
    val password: String = "TestStudent123!@#",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoginSuccessful: Boolean = false
)
```

#### 에러 상태
- 네트워크 오류: "네트워크 연결을 확인해주세요"
- 인증 실패: "이메일 또는 비밀번호가 올바르지 않습니다"
- 서버 오류: "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요"

---

### 2. SessionCodeScreen (세션 코드 입력 화면)

#### 목적
- 강사가 생성한 세션에 참가
- 6자리 세션 코드로 세션 검색 및 조인

#### UI 구성

```mermaid
graph TB
    subgraph SessionCodeScreen
        Welcome[환영 메시지<br/>사용자 이름 표시]
        Instruction[안내 문구<br/>강사가 알려준 세션 코드를 입력하세요]
        CodeField[세션 코드 입력 필드<br/>6자리 숫자, 대형 TextField]
        JoinButton[참가 버튼<br/>Primary Button, 큰 크기]
        LogoutButton[로그아웃 버튼<br/>Text Button, 상단 우측]
        ErrorText[에러 메시지 영역<br/>Optional]
        LoadingIndicator[로딩 인디케이터<br/>Optional]
    end

    LogoutButton --> Welcome
    Welcome --> Instruction
    Instruction --> CodeField
    CodeField --> JoinButton
    JoinButton --> ErrorText
    JoinButton --> LoadingIndicator
```

#### 필드 정보

| 요소 | 타입 | 검증 규칙 | 예시 |
|------|------|-----------|------|
| 세션 코드 | TextField (Number) | 정확히 6자리 숫자, 필수 | 123456 |

#### 주요 액션

1. **참가 버튼 클릭**
   ```
   코드 검증 → API 요청 → 세션 참가
   ```
   - API: `POST /api/students/sessions/join/`
   - Body: `{ "session_code": "123456" }`
   - Headers: `Authorization: Bearer {token}`
   - 성공 시: 세션 ID 저장 → SessionScreen 이동
   - 실패 시: 에러 메시지 표시

2. **로그아웃 버튼 클릭**
   ```
   토큰 삭제 → LoginScreen 이동
   ```

3. **코드 입력 중**
   - 6자리 입력 완료 시 자동 활성화

#### 상태 관리

```kotlin
data class SessionCodeUiState(
    val sessionCode: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val sessionId: Int? = null
)
```

#### 에러 상태
- 코드 형식 오류: "6자리 숫자를 입력해주세요"
- 세션 없음: "유효하지 않은 세션 코드입니다"
- 세션 종료됨: "이미 종료된 세션입니다"
- 권한 없음: "이 세션에 참가할 권한이 없습니다"

---

### 3. SessionScreen (세션 진행 화면)

#### 목적
- 실시간 학습 세션 진행
- WebSocket을 통한 강사와의 실시간 통신
- 단계 완료, 도움 요청 등 학습 액션

#### UI 구성

```mermaid
graph TB
    subgraph SessionScreen
        Header[헤더 영역]
        Status[WebSocket 연결 상태 표시]
        MessageList[실시간 메시지 목록<br/>LazyColumn]
        ActionButtons[액션 버튼 그룹]
    end

    subgraph Header
        SessionTitle[세션 제목]
        BackButton[뒤로가기 버튼]
    end

    subgraph ActionButtons
        HeartbeatBtn[하트비트 전송<br/>Secondary Button]
        CompleteBtn[단계 완료<br/>Primary Button]
        HelpBtn[도움 요청<br/>Warning Button]
    end

    Header --> Status
    Status --> MessageList
    MessageList --> ActionButtons
```

#### 화면 구성 요소

| 요소 | 설명 | 스타일 |
|------|------|--------|
| 세션 제목 | 현재 참가 중인 세션 이름 | H3, Primary Color |
| 연결 상태 | WebSocket 연결 상태 표시 | Chip (연결됨: 초록, 연결 안 됨: 빨강) |
| 메시지 목록 | 강사로부터 받은 실시간 메시지 | Card 형태, 시간 순 정렬 |
| 하트비트 버튼 | 주기적으로 활동 상태 전송 | Secondary Button, 파란색 |
| 단계 완료 버튼 | 현재 단계 완료 알림 | Primary Button, 큰 크기 |
| 도움 요청 버튼 | 강사에게 도움 요청 전송 | Warning Button, 주황색 |

#### 주요 액션

1. **WebSocket 연결**
   ```mermaid
   sequenceDiagram
       participant Screen as SessionScreen
       participant WS as WebSocket
       participant Server as Backend Server

       Screen->>WS: 연결 시도 (ws://server/ws/session/{code}/)
       WS->>Server: Connection Request
       Server-->>WS: Connection Accepted
       WS-->>Screen: onOpen() 이벤트
       Screen->>Screen: 연결 상태 업데이트 (연결됨)

       loop 메시지 수신
           Server->>WS: 메시지 전송
           WS->>Screen: onMessage() 이벤트
           Screen->>Screen: 메시지 목록 업데이트
       end
   ```

2. **하트비트 전송**
   ```json
   {
     "type": "heartbeat",
     "timestamp": "2025-11-14T12:00:00Z"
   }
   ```
   - 버튼 클릭 시 또는 자동 주기 전송 (30초마다)
   - 서버에 학생이 활동 중임을 알림

3. **단계 완료 알림**
   ```json
   {
     "type": "step_completed",
     "step_id": 1,
     "timestamp": "2025-11-14T12:05:00Z"
   }
   ```
   - 학습 단계 완료 시 강사에게 알림

4. **도움 요청**
   ```json
   {
     "type": "help_request",
     "message": "수동 도움 요청",
     "timestamp": "2025-11-14T12:10:00Z"
   }
   ```
   - 수동 도움 요청 전송
   - 강사 대시보드에 즉시 표시

5. **뒤로가기**
   ```
   WebSocket 연결 해제 → SessionCodeScreen 이동
   ```

#### 상태 관리 (SessionViewModel)

```kotlin
data class SessionUiState(
    val sessionId: Int? = null,
    val sessionTitle: String = "",
    val isWebSocketConnected: Boolean = false,
    val messages: List<WebSocketMessage> = emptyList(),
    val errorMessage: String? = null
)

data class WebSocketMessage(
    val id: String,
    val type: String,
    val content: String,
    val timestamp: Long
)
```

#### 메시지 타입

| 타입 | 발신 | 설명 | 표시 방식 |
|------|------|------|-----------|
| `session_start` | 서버 → 클라이언트 | 세션 시작 알림 | 정보 카드 (파란색) |
| `session_pause` | 서버 → 클라이언트 | 세션 일시정지 | 경고 카드 (주황색) |
| `session_resume` | 서버 → 클라이언트 | 세션 재개 | 정보 카드 (파란색) |
| `session_end` | 서버 → 클라이언트 | 세션 종료 알림 | 종료 카드 (빨간색) |
| `next_step` | 서버 → 클라이언트 | 다음 단계 진행 | 성공 카드 (초록색) |
| `notification` | 서버 → 클라이언트 | 일반 알림 | 일반 카드 (회색) |

---

## WebSocket 통신 흐름

### 연결 생명주기

```mermaid
stateDiagram-v2
    [*] --> Disconnected: 초기 상태

    Disconnected --> Connecting: connect() 호출
    Connecting --> Connected: 연결 성공
    Connecting --> Error: 연결 실패

    Connected --> Disconnected: disconnect() 호출
    Connected --> Error: 연결 끊김

    Error --> Connecting: 재연결 시도
    Error --> Disconnected: 재연결 포기

    Connected --> Connected: 메시지 송수신
```

### 메시지 송수신 흐름

```mermaid
sequenceDiagram
    participant Student as 학생 앱
    participant WS as WebSocket
    participant Server as Django Server
    participant Instructor as 강사 대시보드

    Note over Student,Instructor: 세션 시작

    Instructor->>Server: 세션 시작 요청
    Server->>WS: session_start 메시지
    WS->>Student: session_start 수신
    Student->>Student: "세션이 시작되었습니다" 표시

    Note over Student,Instructor: 하트비트 전송

    loop 30초마다
        Student->>WS: heartbeat 전송
        WS->>Server: heartbeat 전달
        Server->>Server: 학생 활동 기록
    end

    Note over Student,Instructor: 단계 완료

    Student->>WS: step_completed 전송
    WS->>Server: 단계 완료 이벤트
    Server->>Server: 진행 상황 업데이트
    Server->>Instructor: 대시보드 업데이트

    Note over Student,Instructor: 도움 요청

    Student->>WS: help_request 전송
    WS->>Server: 도움 요청 이벤트
    Server->>Server: HelpRequest 생성
    Server->>Instructor: 대기 중 도움 요청에 추가
    Instructor->>Instructor: 알림 표시

    Note over Student,Instructor: 다음 단계

    Instructor->>Server: 다음 단계 요청
    Server->>WS: next_step 메시지
    WS->>Student: next_step 수신
    Student->>Student: "다음 단계로 진행하세요" 표시

    Note over Student,Instructor: 세션 종료

    Instructor->>Server: 세션 종료 요청
    Server->>WS: session_end 메시지
    WS->>Student: session_end 수신
    Student->>Student: "세션이 종료되었습니다" 표시
    Student->>WS: 연결 해제
```

### 재연결 로직

```mermaid
flowchart TD
    Start([WebSocket 연결 끊김]) --> Wait[3초 대기]
    Wait --> Retry{재시도 횟수 < 5?}

    Retry -->|Yes| Connect[연결 재시도]
    Connect --> Success{연결 성공?}

    Success -->|Yes| Connected[연결됨 상태]
    Success -->|No| IncCounter[재시도 횟수 증가]
    IncCounter --> Wait

    Retry -->|No| Failed[재연결 실패]
    Failed --> ShowError[에러 메시지 표시]
    ShowError --> BackToCode[SessionCodeScreen으로 이동]

    Connected --> End([정상 동작])

    style Connected fill:#4CAF50
    style Failed fill:#F44336
```

---

## AccessibilityService 동작

### 개요
- **목적**: 학생의 UI 인터랙션을 자동으로 수집하여 분석
- **권한**: 접근성 서비스 권한 필요 (시스템 설정에서 수동 활성화)
- **동작**: 백그라운드에서 지속적으로 UI 이벤트 감지

### 수집 이벤트 타입

| 이벤트 타입 | AccessibilityEvent | 설명 |
|-------------|-------------------|------|
| View Clicked | TYPE_VIEW_CLICKED | 버튼, 링크 등 클릭 |
| View Focused | TYPE_VIEW_FOCUSED | 입력 필드 포커스 |
| Text Changed | TYPE_VIEW_TEXT_CHANGED | 텍스트 입력 변경 |
| Scrolled | TYPE_VIEW_SCROLLED | 화면 스크롤 |
| Window State Changed | TYPE_WINDOW_STATE_CHANGED | 화면 전환 |

### 로그 수집 흐름

```mermaid
sequenceDiagram
    participant User as 학생 (사용자)
    participant App as 다른 앱
    participant Service as AccessibilityService
    participant Kafka as Kafka Producer
    participant Server as Backend Server

    User->>App: UI 인터랙션 (클릭, 입력 등)
    App->>Service: AccessibilityEvent 발생
    Service->>Service: 이벤트 필터링 및 파싱

    alt 유효한 이벤트
        Service->>Service: ActivityLog 객체 생성
        Service->>Kafka: POST /api/logs/activity/
        Note over Service,Kafka: JSON 페이로드 전송
        Kafka->>Server: Kafka 메시지로 전달
        Server->>Server: DB 저장 및 분석
        Server-->>Service: 200 OK
    else 무효한 이벤트
        Service->>Service: 이벤트 무시
    end
```

### ActivityLog 데이터 구조

```json
{
  "student_id": 1,
  "session_id": 5,
  "event_type": "VIEW_CLICKED",
  "package_name": "com.example.app",
  "class_name": "MainActivity",
  "content_description": "확인 버튼",
  "text": "확인",
  "timestamp": "2025-11-14T12:00:00Z",
  "metadata": {
    "x": 540,
    "y": 1200,
    "is_password": false
  }
}
```

### 활성화 프로세스

```mermaid
flowchart TD
    Start([앱 실행]) --> Check{AccessibilityService 활성화?}

    Check -->|Yes| Running[백그라운드 실행 중]
    Check -->|No| ShowPrompt[활성화 안내 다이얼로그]

    ShowPrompt --> UserAction{사용자 선택}
    UserAction -->|나중에| Skip[서비스 없이 계속]
    UserAction -->|설정하기| OpenSettings[시스템 설정 열기]

    OpenSettings --> UserEnable[사용자가 수동으로 활성화]
    UserEnable --> BackToApp[앱으로 돌아옴]
    BackToApp --> Running

    Running --> CollectEvents[이벤트 수집 시작]
    Skip --> LimitedMode[제한된 모드로 실행]

    style Running fill:#4CAF50
    style LimitedMode fill:#FF9800
```

### 프라이버시 고려사항

1. **필터링**
   - 비밀번호 입력 필드는 텍스트 수집 안 함 (`isPassword` 체크)
   - 민감한 패키지 제외 (은행 앱, 결제 앱 등)

2. **투명성**
   - 사용자에게 수집 목적 명확히 안내
   - 설정에서 수집 범위 제어 가능

3. **데이터 보안**
   - HTTPS 통신
   - JWT 인증
   - 암호화된 로그 저장

---

## 에러 처리

### 네트워크 에러

```mermaid
flowchart TD
    Request[API 요청] --> Success{성공?}

    Success -->|Yes| HandleSuccess[정상 처리]
    Success -->|No| CheckError{에러 타입}

    CheckError -->|네트워크 없음| ShowNetworkError[네트워크 오류 메시지]
    CheckError -->|타임아웃| ShowTimeoutError[타임아웃 메시지]
    CheckError -->|401 Unauthorized| HandleAuth[토큰 만료]
    CheckError -->|404 Not Found| ShowNotFound[리소스 없음 메시지]
    CheckError -->|500 Server Error| ShowServerError[서버 오류 메시지]

    HandleAuth --> ClearToken[토큰 삭제]
    ClearToken --> RedirectLogin[LoginScreen으로 이동]

    ShowNetworkError --> Retry{재시도?}
    ShowTimeoutError --> Retry
    ShowServerError --> Retry
    ShowNotFound --> Back[이전 화면으로]

    Retry -->|Yes| Request
    Retry -->|No| Back

    style HandleSuccess fill:#4CAF50
    style ShowNetworkError fill:#F44336
    style ShowTimeoutError fill:#F44336
    style ShowServerError fill:#F44336
```

### WebSocket 에러

| 에러 코드 | 상황 | 처리 방법 |
|-----------|------|-----------|
| 1000 | 정상 종료 | 연결 종료 메시지 표시 |
| 1001 | 서버 종료 | "서버 점검 중입니다" 메시지 |
| 1006 | 비정상 종료 | 자동 재연결 시도 (최대 5회) |
| 1008 | 정책 위반 | "세션 권한이 없습니다" 메시지 |

### 사용자 입력 검증 에러

| 화면 | 필드 | 검증 규칙 | 에러 메시지 |
|------|------|-----------|-------------|
| LoginScreen | 이메일 | 이메일 형식 | "올바른 이메일 형식을 입력하세요" |
| LoginScreen | 비밀번호 | 최소 8자 | "비밀번호는 8자 이상이어야 합니다" |
| SessionCodeScreen | 세션 코드 | 6자리 숫자 | "6자리 숫자를 입력하세요" |

### 권한 에러

```mermaid
flowchart TD
    Start([기능 사용 시도]) --> CheckPerm{권한 있음?}

    CheckPerm -->|Yes| Execute[기능 실행]
    CheckPerm -->|No| RequestPerm[권한 요청 다이얼로그]

    RequestPerm --> UserResponse{사용자 응답}

    UserResponse -->|허용| GrantPerm[권한 부여]
    UserResponse -->|거부| DenyPerm[권한 거부]

    GrantPerm --> Execute
    DenyPerm --> ShowRationale[권한 필요 이유 설명]

    ShowRationale --> UserDecision{재요청?}
    UserDecision -->|Yes| RequestPerm
    UserDecision -->|No| LimitedMode[제한된 모드로 계속]

    Execute --> End([완료])
    LimitedMode --> End

    style Execute fill:#4CAF50
    style DenyPerm fill:#FF9800
```

---

## 상태 지속성

### 저장되는 데이터

| 데이터 | 저장 위치 | 용도 | 만료 |
|--------|-----------|------|------|
| JWT Access Token | DataStore (암호화) | API 인증 | 1시간 |
| JWT Refresh Token | DataStore (암호화) | 토큰 갱신 | 7일 |
| 세션 ID | ViewModel (메모리) | 현재 참가 중인 세션 | 앱 종료 시 |
| 사용자 정보 | DataStore | 사용자 이름, 이메일 | 로그아웃 시 |

### 앱 생명주기와 상태 관리

```mermaid
stateDiagram-v2
    [*] --> Created: 앱 실행

    Created --> Started: onStart()
    Started --> Resumed: onResume()

    Resumed --> Paused: onPause() (홈 버튼, 화면 끔)
    Paused --> Resumed: onResume()
    Paused --> Stopped: onStop() (다른 앱 전환)

    Stopped --> Started: onRestart()
    Stopped --> Destroyed: onDestroy() (시스템 종료)

    Resumed --> Resumed: WebSocket 연결 유지
    Paused --> Paused: WebSocket 일시 중단 (선택)

    Destroyed --> [*]

    note right of Resumed
        AccessibilityService는
        백그라운드에서 계속 실행
    end note
```

---

## 성능 최적화

### 화면 렌더링
- Jetpack Compose의 `remember`, `derivedStateOf` 활용
- 불필요한 리컴포지션 방지

### 네트워크
- Retrofit 캐싱 활용
- 이미지 로딩 시 Coil 라이브러리 사용 (필요 시)

### WebSocket
- 메시지 버퍼링 (1초에 최대 10개)
- 중복 메시지 필터링

### AccessibilityService
- 이벤트 디바운싱 (0.5초)
- 배치 전송 (10개씩 묶어서 전송)

---

## 테스트 시나리오

### 주요 테스트 케이스

1. **로그인 플로우**
   - [ ] 유효한 자격 증명으로 로그인 성공
   - [ ] 잘못된 자격 증명으로 로그인 실패
   - [ ] 네트워크 오류 시 에러 메시지 표시
   - [ ] 토큰 저장 확인

2. **세션 참가**
   - [ ] 유효한 세션 코드로 참가 성공
   - [ ] 잘못된 세션 코드로 참가 실패
   - [ ] 종료된 세션 참가 시도 시 에러

3. **WebSocket 통신**
   - [ ] 세션 시작 메시지 수신
   - [ ] 하트비트 전송 성공
   - [ ] 단계 완료 메시지 전송
   - [ ] 도움 요청 전송
   - [ ] 연결 끊김 시 재연결

4. **AccessibilityService**
   - [ ] 서비스 활성화
   - [ ] UI 이벤트 수집
   - [ ] 로그 전송
   - [ ] 비밀번호 필드 필터링

---

## 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|-----------|
| 1.0  | 2025-11-14 | 초안 작성 |

---

## 참고 자료

- [03_android_architecture.md](./03_android_architecture.md) - Android 앱 아키텍처
- [04_sequence_diagrams.md](./04_sequence_diagrams.md) - 시퀀스 다이어그램
- [14_websocket_protocol.md](./14_websocket_protocol.md) - WebSocket 프로토콜
