# MobEdu 시니어 디지털 교육 서비스 - 설계 문서

## 📋 문서 개요

이 폴더는 **MobEdu 시니어 디지털 교육 서비스**의 전체 설계 문서를 포함합니다.

**🆕 핵심 특징: 실시간 동기식 강의방 시스템**
- 강사가 QR 코드로 강의방을 생성하고 학생들이 입장
- 강사가 단계별로 진행을 제어하며 모든 학생이 동기화
- 강의 종료 후에도 복습 모드로 자율 학습 가능

---

## 📚 설계 문서 목록

### 1. [데이터베이스 스키마 설계](./01_database_schema.md)
- **내용**: PostgreSQL 데이터베이스의 전체 테이블 구조 및 관계
- **주요 테이블**:
  - `users`: 사용자 정보 (강사/학생)
  - `lectures`: 강의
  - `tasks`: 과제
  - `subtasks`: 세부 단계
  - `activity_logs`: 행동 로그
  - `help_requests`: 도움 요청
  - `mgpt_analyses`: M-GPT 분석 결과
  - `user_progress`: 학습 진행 상태
- **ERD**: 테이블 간 관계도 포함
- **주요 쿼리**: 자주 사용되는 쿼리 예시

---

### 2. [API 엔드포인트 설계](./02_api_endpoints.md)
- **내용**: Django REST API의 모든 엔드포인트 정의
- **API 그룹**:
  - **Auth**: 회원가입, 로그인, 토큰 갱신
  - **Lectures**: 강의 CRUD, 수강 신청
  - **Tasks**: 과제/단계 관리
  - **Progress**: 학습 진행 상태 조회/업데이트
  - **Logs**: 행동 로그 전송
  - **Help**: 도움 요청 및 응답
  - **Dashboard**: 강사용 모니터링
  - **WebSocket**: 실시간 통신
- **Request/Response 예시**: 각 API의 상세 스펙

---

### 3. [Android 앱 아키텍처 (학생용)](./03_android_architecture.md)
- **대상 사용자**: 학생 (시니어 학습자)
- **내용**: Android 앱의 전체 구조 및 컴포넌트 설계
- **아키텍처 패턴**: Clean Architecture + MVVM
- **기술 스택**:
  - Kotlin, Hilt (DI), Coroutines, Flow
  - Retrofit, Room, Kafka Producer
  - AccessibilityService, Overlay (WindowManager)
- **주요 컴포넌트**:
  - **AccessibilityService**: UI 이벤트 감지 (Android 공식 가이드 기반)
  - **Overlay UI**: 실시간 가이드 표시
  - **Kafka Producer**: 로그 전송
  - **ViewModel**: 비즈니스 로직
  - **Repository**: 데이터 소스 추상화
- **패키지 구조**: 전체 디렉토리 구조 및 파일 설명
- **NEW**: XML 설정, AndroidManifest, AccessibilityNodeInfo 활용 상세 구현

---

### 3-1. [AccessibilityService 상세 가이드](./03-1_accessibility_service_details.md)
- **내용**: AccessibilityService 구현 상세 가이드 (Android 공식 문서 기반)
- **주요 내용**:
  - XML 설정 파일 및 AndroidManifest 권한
  - 서비스 활성화 체크 및 유도 UI
  - AccessibilityNodeInfo를 통한 UI 요소 탐색
  - 목표 요소 찾기 및 자동 완료 로직
  - 보안 및 개인정보 보호 (민감 정보 필터링)
  - 디버깅 및 테스트 방법
  - 성능 최적화 (이벤트 쓰로틀링, 배치 전송)
  - 사용자 가이드 및 주의사항

---

### 4. [핵심 플로우 시퀀스 다이어그램](./04_sequence_diagrams.md)
- **내용**: 주요 사용자 플로우의 상세 시퀀스 다이어그램 (Mermaid)
- **플로우 목록**:
  1. **학습 시작 플로우**: 강의 선택 → Subtask 로드 → Overlay 표시
  2. **로그 수집 플로우**: AccessibilityService → Kafka → DB 저장
  3. **도움 요청 플로우**: 버튼 클릭 → M-GPT 분석 → 응답 표시
  4. **진행 상태 업데이트**: Subtask 완료 → 다음 단계 로드
  5. **강사 모니터링**: WebSocket 실시간 업데이트
  6. **강의 생성**: 강사가 강의/Task/Subtask 등록
  7. **배치 동기화**: 로컬 로그 → 서버 재전송
  8. **M-GPT 파이프라인**: Kafka → OpenAI → 응답 저장

---

### 5. [Kafka 메시지 구조](./05_kafka_structure.md)
- **내용**: Kafka Topic 구조 및 메시지 스키마
- **Topic 목록**:
  - `activity-logs`: 행동 로그 수집
  - `help-requests`: 도움 요청
  - `help-responses`: M-GPT 분석 결과
  - `progress-updates`: 학습 진행 업데이트
  - `instructor-notifications`: 강사 알림
  - `system-events`: 시스템 이벤트
- **Producer/Consumer 구조**: 각 컴포넌트의 역할
- **에러 처리**: DLQ(Dead Letter Queue) 및 재시도 정책
- **모니터링**: Lag, 처리량, 에러율 알림

---

### 6. [강사용 웹 Dashboard 아키텍처](./06_web_dashboard_architecture.md) 🆕
- **대상 사용자**: 강사 (PC 웹 브라우저)
- **내용**: React + TypeScript 기반 웹 Dashboard 설계
- **기술 스택**:
  - **Frontend**: React 19 + TypeScript + Vite
  - **UI Library**: Material-UI (MUI) v7
  - **Real-time**: WebSocket (Native WebSocket API)
  - **Charts**: Recharts
  - **HTTP Client**: Axios
- **주요 기능**:
  - 실시간 수강생 모니터링 (WebSocket)
  - 진행률 차트 및 통계
  - 도움 요청 알림 및 상세 정보
  - 강의/Task/Subtask CRUD
  - 수강생별 학습 히스토리
- **URL 구조**: SPA 라우팅 (React Router v7)
- **아키텍처**: Component-based Architecture

---

### 7. [UI 디자인 시스템](./07_ui_design_system.md) 🎨 NEW
- **내용**: Android 앱과 웹 대시보드의 통합 UI 디자인 시스템
- **주요 내용**:
  - **색상 시스템**: Material Design 3 기반 팔레트
  - **타이포그래피**: 시니어 친화적 폰트 크기 (최소 16px)
  - **간격 시스템**: 8dp/px 기반 일관된 spacing
  - **컴포넌트 스타일**: 버튼, 카드, 입력 필드, 테이블 등
  - **플랫폼별 구현**: Kotlin Compose, React MUI 코드 예시
- **접근성**: WCAG AA 준수, 고대비 색상, 명확한 터치 영역

---

### 8. [Android 앱 화면 흐름도](./08_android_user_flow.md) 📱 NEW
- **내용**: 학생 앱의 모든 화면 플로우 및 인터랙션 정의
- **주요 내용**:
  - **화면 목록**: LoginScreen, SessionCodeScreen, SessionScreen
  - **User Flow**: Mermaid 다이어그램으로 시각화
  - **WebSocket 통신**: 실시간 메시지 송수신 흐름
  - **AccessibilityService**: 동작 원리 및 로그 수집
  - **에러 처리**: 네트워크, 인증, 권한 에러 시나리오

---

### 9. [웹 대시보드 화면 흐름도](./09_web_dashboard_user_flow.md) 💻 NEW
- **내용**: 강사 대시보드의 모든 페이지 플로우 및 기능 정의
- **주요 내용**:
  - **페이지 목록**: LoginPage, DashboardPage, **LiveSessionPage**, StatisticsPage
  - **User Flow**: 로그인 → 세션 생성 → 실시간 강의 → 통계
  - **LiveSessionPage ⭐**: Zoom 스타일 통합 실시간 강의 화면 (신규)
  - **WebSocket 통신**: 3개 연결 동시 사용 (세션 제어, 모니터링, 화면 미러링)
  - **세션 제어**: 시작/일시정지/재개/다음 단계/종료

---

### 10. [실시간 강의 화면 상세 설계](./10_live_session_page.md) ⭐ NEW
- **내용**: Zoom 스타일 통합 실시간 강의 화면 (LiveSessionPage) 상세 설계
- **레이아웃**: 3-Column Layout (좌측 패널 + 중앙 화면 + 우측 패널)
- **주요 기능**:
  - **좌측 패널**: 강의 정보, 수강생 목록, 연령대별 진도 차트
  - **중앙 영역**: 선택한 학생의 Android 화면 실시간 미러링
  - **우측 패널**: 참가자 현황, 그룹별 학습 내용, 실시간 알림 (도움 요청)
  - **상단 컨트롤 바**: 세션 제어 버튼, 경과 시간 타이머
- **WebSocket**: 3개 동시 연결로 실시간 데이터 통합
- **화면 미러링**: 5초마다 학생 화면 이미지 업데이트

---

### 14. [WebSocket 메시지 프로토콜](./14_websocket_protocol.md) 🔌 NEW
- **내용**: WebSocket 실시간 통신 프로토콜 명세
- **주요 내용**:
  - **연결 엔드포인트**: 세션 WebSocket, 대시보드 WebSocket, 화면 미러링 WebSocket
  - **메시지 포맷**: JSON 기반, 타입별 스키마 정의
  - **학생→서버**: heartbeat, step_completed, help_request
  - **서버→학생**: session_start, session_pause, next_step, notification
  - **서버→강사**: progress_update, help_request, participant_joined
  - **연결 관리**: 재연결 로직 (지수 백오프), 하트비트
  - **보안**: JWT 인증, Rate Limiting, 메시지 검증
- **코드 예시**: TypeScript, Kotlin WebSocket 클라이언트

---

## 🏗️ 전체 시스템 아키텍처

```
┌──────────────────────────────────────────────────────────────┐
│              Android Client (학생용 - 모바일 앱)              │
│                                                              │
│  ┌────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │ Accessibility  │→│  Event Collector │→│ Kafka        │ │
│  │ Service        │  │                 │  │ Producer     │ │
│  └────────────────┘  └─────────────────┘  └──────┬───────┘ │
│                                                    │         │
│  ┌────────────────┐  ┌─────────────────┐         │         │
│  │ Overlay UI     │←│  ViewModel       │         │         │
│  │ (실시간 가이드) │  │  (학습 로직)     │         │         │
│  └────────────────┘  └─────────────────┘         │         │
└──────────────────────────────────────────────────┼─────────┘
                                                    │
                                                    ↓
┌──────────────────────────────────────────────────────────────┐
│                      Kafka Cluster                           │
│                                                              │
│  Topics: activity-logs, help-requests, help-responses,       │
│          progress-updates, instructor-notifications          │
└───────────────┬──────────────────────────┬───────────────────┘
                │                          │
        ┌───────↓────────┐         ┌───────↓────────┐
        │  Log Consumer  │         │  M-GPT Consumer│
        │                │         │                │
        │  → PostgreSQL  │         │  → OpenAI API  │
        └────────────────┘         └───────┬────────┘
                                           │
                                           ↓
┌──────────────────────────────────────────────────────────────┐
│                    Backend (Django)                          │
│                                                              │
│  ┌─────────────────┐  ┌──────────────────┐  ┌────────────┐ │
│  │  REST API       │  │  Django Channels │  │ PostgreSQL │ │
│  │  (Auth, CRUD)   │  │  (WebSocket)     │  │  + Redis   │ │
│  └─────────────────┘  └──────────────────┘  └────────────┘ │
└──────────────────────────────────────────────────────────────┘
                    │                      │
                    ↓                      ↓ (WebSocket)
┌──────────────────────────────────────────────────────────────┐
│           Web Dashboard (강사용 - PC 웹 브라우저)             │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  Django Template + HTMX + Bootstrap 5 + Chart.js     │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                              │
│  - 실시간 수강생 모니터링 (WebSocket)                         │
│  - 진행률 차트 및 통계 (Chart.js)                            │
│  - 도움 요청 알림 (Toast + 오디오)                           │
│  - 강의/Task/Subtask CRUD                                   │
│  - 수강생별 학습 히스토리                                     │
└──────────────────────────────────────────────────────────────┘

📱 학생: Android 앱으로 학습
💻 강사: PC 웹 브라우저로 모니터링 및 관리
```

---

## 🔑 핵심 기능 요약

### 1. 실시간 강의방 (Live Session) ⭐ NEW
- **QR 코드 입장**: 강사가 생성한 QR 코드를 스캔하여 강의방 입장
- **대기실**: 강사가 시작하기 전까지 대기, 참가자 실시간 확인
- **동기식 진행**: 강사가 "다음 단계" 누르면 모든 학생이 동시에 이동
- **강사 제어**: 강의 시작/일시정지/재개/종료, 단계별 메시지 전송
- **복습 모드**: 강의 종료 후에도 학생들이 자유롭게 복습 가능

### 2. Accessibility 기반 행동 감지
- Android AccessibilityService로 모든 UI 이벤트 수집
- 클릭, 스크롤, 텍스트 입력, 화면 전환 등 자동 로깅
- Kafka를 통한 비동기 전송으로 성능 영향 최소화

### 3. Overlay 기반 실시간 가이드
- 현재 학습 단계를 Overlay로 항상 표시
- 목표 안내, 도움 버튼, TTS 음성 안내 제공
- 다른 앱 위에 표시 가능 (TYPE_ACCESSIBILITY_OVERLAY)
- WebSocket을 통해 강사의 단계 변경 즉시 반영

### 4. M-GPT 기반 도움 제공
- 학생이 막혔을 때 AI가 로그 분석
- 문제 진단 + 맞춤형 도움말 생성
- OpenAI API 활용, Redis 캐싱으로 성능 최적화

### 5. 강사 Dashboard (PC 웹)
- 실시간 수강생 모니터링 (WebSocket)
- 강의방 생성 및 QR 코드 생성
- 단계별 진행 제어 (시작/다음 단계/일시정지/종료)
- 참가자 상태 실시간 확인 (대기/활성/연결 끊김)
- 진행률, 도움 요청, 완료 상태 시각화
- 통계 리포트 (평균 완료 시간, 자주 막히는 단계 등)

---

## 🚀 개발 우선순위 (MVP) - 실시간 강의방 시스템

### Phase 1: 핵심 인프라 + 실시간 강의방 (3주)
1. PostgreSQL 스키마 생성 (실시간 강의방 테이블 포함)
2. Django REST API 기본 구조 (Auth, Lecture, Session CRUD)
3. Kafka 클러스터 설정 및 Topic 생성 (세션 이벤트 포함)
4. Android 앱 기본 구조 (Hilt, Navigation, WebSocket)
5. **강의방 생성 및 QR 코드 생성 기능**
6. **QR 스캔 및 세션 입장 기능**

### Phase 2: 대기실 및 동기식 진행 (2주)
1. 대기실 UI (Android + Web Dashboard)
2. WebSocket 실시간 통신 (학생 ↔ 강사)
3. 강사의 단계 제어 API 및 WebSocket 메시지 전송
4. 학생 앱의 WebSocket 메시지 수신 및 Overlay 업데이트

### Phase 3: 핵심 학습 기능 (3주)
1. AccessibilityService 구현 및 로그 수집
2. Overlay UI 및 가이드 표시
3. Kafka Producer/Consumer 구현
4. Progress 관리 API 및 앱 연동 (세션 기반)
5. 복습 모드 전환 기능

### Phase 4: AI 도움 기능 (2주)
1. M-GPT Service 구현 (OpenAI API 연동)
2. 도움 요청 플로우 (API + Kafka + Consumer)
3. 앱에서 도움 응답 표시 (Overlay + TTS)
4. 강사 Dashboard에 도움 요청 실시간 알림

### Phase 5: 강사 Dashboard 고도화 (2주)
1. 실시간 참가자 모니터링 (대기실 포함)
2. 단계 제어 UI (시작/다음/일시정지/종료)
3. 참가자별 상태 시각화 (Chart.js)
4. 통계 및 리포트 기능

### Phase 6: 테스트 및 최적화 (1주)
1. 통합 테스트 (특히 WebSocket 동기화)
2. 성능 최적화 (DB 인덱스, Kafka 파티셔닝, WebSocket 부하)
3. 사용자 테스트 (실제 강의 시뮬레이션)
4. 피드백 반영

---

## 🛠️ 기술 스택 요약

### Frontend

#### Android App (학생용)
- **언어**: Kotlin
- **UI**: XML Layouts (또는 Jetpack Compose)
- **DI**: Hilt
- **비동기**: Coroutines + Flow
- **네트워크**: Retrofit + OkHttp
- **로컬 DB**: Room
- **핵심**: AccessibilityService, Overlay (WindowManager), Kafka Producer

#### Web Dashboard (강사용)
- **Backend**: Django 4.x + Django Channels
- **Template**: Django Template Language
- **JS**: HTMX 1.9+ + Alpine.js 3.x
- **CSS**: Bootstrap 5.3
- **Charts**: Chart.js 4.x
- **Real-time**: WebSocket (Django Channels)

### Backend
- **API**: Django 4.x + Django REST Framework
- **Database**: PostgreSQL 14+
- **Message Queue**: Apache Kafka 3.x
- **Cache**: Redis 7.x (Django Channels + 캐싱)
- **WebSocket**: Django Channels 4.x

### AI
- **M-GPT**: OpenAI API (GPT-4)
- **TTS**: Android TextToSpeech API (학생 앱)

### Infrastructure
- **Deployment**: Docker + Docker Compose
- **WebSocket Server**: Daphne (ASGI)
- **Monitoring**: Prometheus + Grafana, Kafka Manager
- **CI/CD**: GitHub Actions

---

## 📊 예상 성능 지표

| 항목 | 목표 |
|------|------|
| 로그 처리량 | 1000 logs/sec |
| M-GPT 응답 시간 | < 5초 (90 percentile) |
| Overlay 표시 지연 | < 100ms |
| API 응답 시간 | < 200ms (95 percentile) |
| WebSocket 실시간 지연 | < 1초 |
| DB 동시 연결 | 100+ connections |

---

## 📝 다음 단계

1. **설계 검토**: 팀과 함께 설계 문서 리뷰
2. **프로토타입**: 핵심 플로우 프로토타입 구현
3. **환경 구축**: 개발/스테이징/프로덕션 환경 설정
4. **개발 시작**: Phase 1부터 순차적 개발
5. **사용자 테스트**: 시니어 사용자와 함께 파일럿 테스트

---

## ❓ 질문 및 피드백

설계에 대한 질문이나 피드백이 있으면 언제든 공유해주세요:
- 추가 기능 제안
- 기술 스택 변경 제안
- 아키텍처 개선 아이디어
- 성능 최적화 아이디어

이 설계는 MVP 기준이며, 운영하면서 지속적으로 개선할 예정입니다.
