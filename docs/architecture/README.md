# MobEdu 시스템 아키텍처 문서

**최종 업데이트**: 2025-11-19
**프로젝트**: AI 기반 시니어 디지털 교육 서비스

이 문서는 MobEdu 프로젝트의 시스템 아키텍처를 설명합니다.
다이어그램은 [Draw.io](https://app.diagrams.net) 또는 VS Code의 Draw.io Integration 확장으로 열 수 있습니다.

---

## 📑 다이어그램 목록

### 빠른 이해를 위한 간략 버전 (권장)

| 다이어그램 | 파일 | 설명 |
|-----------|------|------|
| **시스템 구조 (간략)** | [`system-architecture-simple.drawio`](./system-architecture-simple.drawio) | 전체 시스템 구성 요소와 흐름을 한눈에 파악 |
| **세션 제어 플로우** | [`session-control-flow-simple.drawio`](./session-control-flow-simple.drawio) | 강사가 세션을 만들고 학생이 참가하는 과정 |
| **활동 로그 파이프라인** | [`activity-log-pipeline-simple.drawio`](./activity-log-pipeline-simple.drawio) | 학생 활동 데이터가 수집되고 저장되는 과정 |
| **녹화→과제 생성** | [`recording-to-task-simple.drawio`](./recording-to-task-simple.drawio) | 강사 시연을 AI가 분석하여 학습 과제로 만드는 과정 |
| **도움 요청 플로우** | [`help-request-simple.drawio`](./help-request-simple.drawio) | 학생이 도움을 요청하면 AI가 맞춤 힌트를 제공하는 과정 |

### 상세 버전 (개발자용)

| 다이어그램 | 파일 | 설명 |
|-----------|------|------|
| **시스템 구조 (상세)** | [`system-architecture.drawio`](./system-architecture.drawio) | 모든 컴포넌트와 모듈의 세부 구조 |
| **세션 제어 플로우 (상세)** | [`session-control-flow.drawio`](./session-control-flow.drawio) | WebSocket 통신 등 기술적 세부 사항 포함 |
| **활동 로그 파이프라인 (상세)** | [`activity-log-pipeline.drawio`](./activity-log-pipeline.drawio) | Kafka 설정, 에러 처리 등 상세 내용 |
| **녹화→과제 생성 (상세)** | [`recording-to-task-flow.drawio`](./recording-to-task-flow.drawio) | 알고리즘과 GPT 프롬프트 상세 |
| **도움 요청 플로우 (상세)** | [`help-request-flow.drawio`](./help-request-flow.drawio) | M-GPT 분석 프로세스 상세 |

---

## 🏗️ 시스템 개요

### 핵심 컴포넌트

```
┌─────────────┐     ┌──────────────┐     ┌─────────────┐
│  강사용 웹  │ ←→ │  백엔드 서버  │ ←→ │ 학생용 앱    │
│  (React)    │     │  (Django)    │     │ (Android)   │
└─────────────┘     └──────────────┘     └─────────────┘
                            ↕
                    ┌───────────────┐
                    │  데이터베이스  │
                    │  Redis + Kafka│
                    └───────────────┘
```

### 기술 스택

| 영역 | 기술 |
|-----|------|
| **강사용 웹** | React 18 + TypeScript + Vite + Radix UI |
| **학생용 앱** | Android (Kotlin + Jetpack Compose + MVVM) |
| **백엔드** | Django 4.2 + REST Framework + Channels (WebSocket) |
| **데이터베이스** | PostgreSQL 15 |
| **캐시/메시징** | Redis 7 + Apache Kafka 3.5 |
| **백그라운드 작업** | Celery + Beat |
| **AI 분석 (예정)** | OpenAI GPT-4 |
| **인프라** | Docker Compose (9개 컨테이너) |

---

## 🎯 주요 기능 설명

### 1. 실시간 강의 세션

**작동 방식**:
1. 강사가 세션 생성 → 6자리 코드 발급 (예: `ABC123`)
2. 학생들이 앱에서 코드 입력하여 참가
3. WebSocket으로 실시간 연결
4. 강사가 단계별로 진행 (시작/일시정지/다음 단계/종료)
5. 모든 학생 화면에 즉시 반영

**기술**: Django Channels + Redis Channel Layer

**참고**: [`session-control-flow-simple.drawio`](./session-control-flow-simple.drawio)

---

### 2. 활동 로그 수집

**작동 방식**:
1. Android `AccessibilityService`가 학생의 UI 이벤트 감지 (클릭, 스크롤, 입력 등)
2. 5초마다 배치로 백엔드에 전송
3. Kafka를 통해 비동기 처리
4. Consumer가 PostgreSQL에 저장
5. 필요시 AI 분석 트리거

**장점**:
- API 응답 지연 없음 (즉시 202 응답)
- 초당 1000+ 이벤트 처리 가능
- 트래픽 급증에도 안정적

**참고**: [`activity-log-pipeline-simple.drawio`](./activity-log-pipeline-simple.drawio)

---

### 3. 녹화 → 과제 자동 생성

**작동 방식**:

| 단계 | 설명 | 방법 |
|-----|------|------|
| **1. 녹화** | 강사가 스마트폰에서 시연 | AccessibilityService |
| **2. 규칙 기반 분석** | 이벤트를 Task/Subtask로 자동 분류 | 앱 전환, 시간 간격 등 |
| **3. AI 정제 (선택)** | 사용자 친화적 설명 생성 | OpenAI GPT-4 |
| **4. 강의 생성** | DB에 Lecture, Task, Subtask 저장 | Django ORM |

**예시 출력**:
```
강의: "YouTube에서 동영상 검색하는 방법"
├─ Task 1: YouTube 앱 실행 (쉬움)
│  └─ 1.1. YouTube 아이콘 터치
├─ Task 2: 동영상 검색 (보통)
│  ├─ 2.1. 검색 아이콘 터치
│  ├─ 2.2. 검색어 입력
│  └─ 2.3. 검색 버튼 터치
└─ Task 3: 동영상 재생 (쉬움)
   └─ 3.1. 원하는 동영상 터치
```

**하이브리드 접근법의 장점**:
- 규칙 기반: 빠르고 비용 없음, 오프라인 작동
- AI 정제: 초보자도 이해하기 쉬운 설명, 난이도 추정, 맞춤 힌트
- 비용: 녹화당 $0.01-0.05
- 자동화율: 80%+ (강사는 최종 검토만)

**참고**: [`recording-to-task-simple.drawio`](./recording-to-task-simple.drawio)

---

### 4. 도움 요청 시스템

**작동 방식**:
1. 학생이 '도움 요청' 버튼 클릭
2. 백엔드가 `HelpRequest` 생성, 강사에게 알림
3. (선택) AI가 최근 활동 로그 분석하여 힌트 생성
4. 강사가 AI 제안을 수락/편집하거나 직접 작성
5. 학생에게 도움말 전달 (텍스트 + 음성 + UI 하이라이트)

**AI 분석 입력 데이터**:
- 현재 학습 단계 정보
- 최근 20개 활동 로그
- 학생의 디지털 수준 (초급/중급/고급)
- 이전 도움 요청 이력

**도움말 유형**:
- 텍스트 힌트 (다이얼로그)
- 음성 안내 (TTS)
- UI 요소 하이라이트 (화살표 + 오버레이)
- 동영상 데모

**향후 기능**:
- 자동 감지 (반복 실패, 30초 이상 비활동)
- 실시간 영상 통화

**참고**: [`help-request-simple.drawio`](./help-request-simple.drawio)

---

## 🔧 인프라 구성

### Docker Compose 서비스 (9개)

```yaml
services:
  # 애플리케이션
  - django         # Django + Daphne ASGI
  - frontend       # React (개발 서버)

  # 데이터베이스
  - postgres       # PostgreSQL 15

  # 메시징/캐싱
  - redis          # Redis 7
  - zookeeper      # Kafka 조정 서비스
  - kafka          # Kafka 브로커

  # 백그라운드
  - celery-worker  # 비동기 작업 처리
  - celery-beat    # 스케줄 작업
  - kafka-consumer # 활동 로그 처리
```

### 주요 통신 방식

| 연결 | 프로토콜 | 용도 |
|-----|---------|------|
| 웹/앱 ↔ 백엔드 | REST API (HTTP) | CRUD 작업 |
| 웹/앱 ↔ 백엔드 | WebSocket | 실시간 업데이트 |
| 백엔드 → Kafka | Kafka Protocol | 이벤트 스트리밍 |
| 백엔드 ↔ Redis | Redis Protocol | 캐시, Channel Layer |
| 백엔드 ↔ PostgreSQL | SQL (Django ORM) | 데이터 저장 |

---

## 📊 확장성 고려사항

### 수평 확장 전략

| 컴포넌트 | 확장 방법 |
|---------|----------|
| **Django** | 로드 밸런서 뒤에 여러 인스턴스 배포 (Stateless) |
| **Kafka Consumer** | Consumer Group으로 병렬 처리, 파티션 추가 |
| **PostgreSQL** | Read Replica 추가, Connection Pooling |
| **Redis** | Redis Cluster 구성 |
| **Kafka** | 파티션/Broker 추가 |

### 성능 목표

| 지표 | 목표 |
|-----|------|
| API 응답 시간 (p95) | < 200ms |
| 활동 로그 처리량 | 1000+ events/sec |
| WebSocket 동시 연결 | 500+ users/session |
| End-to-End 로그 지연 | ~1-2초 |

---

## 🚀 OpenAI API 통합 (예정)

### 환경 변수 설정

```bash
# backend/.env
OPENAI_API_KEY=sk-...
OPENAI_MODEL=gpt-4
OPENAI_TEMPERATURE=0.3
OPENAI_MAX_TOKENS=1500
```

### 사용 케이스

1. **녹화 분석**: 이벤트 시퀀스를 분석하여 Task/Subtask 생성
2. **도움 요청 분석**: 학생의 어려움을 진단하고 맞춤 힌트 제공
3. **학습 패턴 분석**: 난이도 자동 조정, 추천 학습 경로 생성

### 비용 최적화

- 캐싱으로 중복 요청 방지
- Temperature 0.2-0.4로 낮춰서 일관성↑, 토큰 사용↓
- GPT-3.5-turbo로 일부 작업 대체 (비용 1/10)
- Max Tokens 제한으로 불필요한 응답 방지

---

## 📚 참고 자료

### 공식 문서

- [Django Channels](https://channels.readthedocs.io/)
- [Kafka Python Client](https://kafka-python.readthedocs.io/)
- [OpenAI API](https://platform.openai.com/docs/api-reference)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [AccessibilityService](https://developer.android.com/guide/topics/ui/accessibility/service)

### 다이어그램 편집

1. [Draw.io 웹](https://app.diagrams.net)에서 파일 열기
2. 또는 VS Code에서 "Draw.io Integration" 확장 설치 후 `.drawio` 파일 편집
3. 변경 사항을 이 README에도 반영
4. Pull Request 생성

---

**문서 작성**: Claude Code
**최종 업데이트**: 2025-11-19
**버전**: 1.0 (간결화)
