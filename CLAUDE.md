# CPS 2025-2 프로젝트 가이드

## 프로젝트 개요

강의자가 안드로이드 기기에서 시연을 녹화하고, AI가 분석하여 학생들이 따라할 수 있는 과제로 변환하는 교육 시스템.

## 용어 체계 (Terminology)

| 영문 (Code) | 한글 (UI) | 설명 |
|-------------|----------|------|
| **Recording** | 녹화 | 강의자가 시연을 기록하는 행위/데이터 |
| **Task** | 과제 | 녹화가 변환된 학습 콘텐츠 (핵심 단위) |
| **Subtask** | 단계 | 과제를 구성하는 개별 단계들 |
| **Lecture** | 강의 | 과제들의 컨테이너 (선택적 연결) |
| **Session** | 세션 | 학생들과 실시간으로 진행하는 수업 |

### 핵심 플로우
```
녹화(Recording) → 분석(GPT) → 과제(Task) + 단계들(Subtasks) 생성
                                    ↓
                               강의(Lecture)에 연결 (선택적)
```

**중요**: Recording은 Task를 직접 참조합니다 (`recording.task`). Lecture 연결은 선택적입니다.

### 녹화 상태 (Recording Status)
| 상태 | 설명 | 다음 액션 |
|------|------|----------|
| `RECORDING` | 녹화 진행 중 | stop/ |
| `COMPLETED` | 녹화 완료 | analyze/ |
| `PROCESSING` | GPT 분석 중 | (대기) |
| `ANALYZED` | 분석 완료 | convert-to-task/ |
| `FAILED` | 분석 실패 | analyze/ (재시도) |

### 변환 완료 판별
- **변환 전**: `recording.task == null`
- **변환 후**: `recording.task != null` (Task ID 존재)

## 프로젝트 구조

```
cps 2025-2/
├── backend/                 # Django REST API 서버
│   ├── apps/
│   │   ├── lectures/        # 강의 관리
│   │   ├── tasks/           # 과제 및 단계 관리
│   │   ├── sessions/        # 세션 및 녹화 관리
│   │   └── logs/            # 활동 로그
│   └── config/              # Django 설정
│
├── android-instructor/      # 강의자용 안드로이드 앱
│   └── app/src/main/java/com/example/mobilegpt/
│       ├── recording/       # 녹화 관련 화면
│       ├── subtask/         # 단계 관련 화면
│       ├── viewmodel/       # ViewModel
│       └── data/remote/     # API 클라이언트
│
└── frontend/                # 웹 대시보드 (React)
```

## API 경로 (Endpoints)

### 녹화 API (`/api/recordings/`)
| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/recordings/` | 녹화 목록 |
| POST | `/api/recordings/` | 녹화 시작 |
| GET | `/api/recordings/{id}/` | 녹화 상세 |
| POST | `/api/recordings/{id}/stop/` | 녹화 종료 |
| POST | `/api/recordings/{id}/save-events-batch/` | 이벤트 배치 저장 |
| POST | `/api/recordings/{id}/analyze/` | GPT 분석 시작 |
| GET | `/api/recordings/{id}/analysis-status/` | 분석 상태 조회 |
| POST | `/api/recordings/{id}/convert-to-task/` | 과제로 변환 |
| GET | `/api/recordings/{id}/subtasks/` | 단계 목록 조회 |

### 과제 변환 API 상세

**POST `/api/recordings/{id}/convert-to-task/`**
```json
// Request
{
    "title": "과제 제목",
    "description": "과제 설명 (선택)",
    "lecture_id": 1  // 연결할 강의 ID (선택)
}

// Response (202 Accepted)
{
    "message": "과제 변환이 시작되었습니다.",
    "recording_id": 37,
    "title": "과제 제목"
}
```

**GET `/api/recordings/{id}/subtasks/`**
```json
// Response (변환 완료 시)
{
    "recording_id": 37,
    "task_id": 7,
    "task_title": "과제 제목",
    "subtask_count": 26,
    "subtasks": [...]
}

// Response (변환 전, 404)
{
    "recording_id": 37,
    "error": "녹화가 아직 과제로 변환되지 않았습니다.",
    "message": "POST /api/recordings/{id}/convert-to-task/ 를 호출하여 과제로 변환하세요.",
    "recording_status": "ANALYZED",
    "subtasks": []
}
```

### 강의 API (`/api/lectures/`)
| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/lectures/` | 강의 목록 |
| POST | `/api/lectures/` | 강의 생성 |
| GET | `/api/lectures/{id}/tasks/` | 강의의 과제 목록 |

### 과제 API (`/api/tasks/`)
| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/tasks/{id}/subtasks/` | 과제의 단계 목록 |
| PUT | `/api/tasks/subtasks/{id}/` | 단계 수정 |
| DELETE | `/api/tasks/subtasks/{id}/` | 단계 삭제 |

## 데이터 모델 관계

```
RecordingSession
├── task (FK → Task, nullable)      # 변환된 과제 (핵심!)
├── lecture (FK → Lecture, nullable) # 연결된 강의 (선택적)
├── status: RECORDING|COMPLETED|PROCESSING|ANALYZED|FAILED
└── analysis_result: JSONField       # GPT 분석 결과

Task
├── lecture (FK → Lecture, nullable) # 소속 강의 (선택적)
├── title, description
└── subtasks (reverse FK)

Subtask
├── task (FK → Task)
├── order_index: int
├── title, description
├── target_action: CLICK|LONG_CLICK|SCROLL|INPUT|NAVIGATE
└── ... (기타 UI 힌트 필드)
```

## Android 네비게이션 라우트

| Route | 화면 | 설명 |
|-------|------|------|
| `login` | LoginScreen | 로그인 |
| `recording` | RecordingScreen | 메인 (녹화 시작) |
| `recordingList` | RecordingListScreen | 녹화 목록 |
| `subtaskList/{recordingId}` | SubtaskListScreen | 단계 목록 |
| `subtaskDetail/{recordingId}/{index}` | SubtaskDetailScreen | 단계 수정 |

## Android DTO 구조

### RecordingResponse
```kotlin
data class RecordingResponse(
    val id: Long,
    val title: String,
    val status: String,
    val task: TaskRef?,      // 변환 완료 시 존재
    val lecture: LectureRef?, // 선택적
    ...
)

data class TaskRef(
    val id: Long,
    val title: String?,
    val subtaskCount: Int?
)
```

### 변환 상태 판별 (UI)
```kotlin
val hasTask = recording.task != null

when {
    hasTask -> // "단계 보기" 버튼
    status == "ANALYZED" -> // "과제로 변환" 버튼
    status == "COMPLETED" -> // "분석 시작" 버튼
}
```

## 주의사항

### 용어 사용
- 코드에서는 영문 용어 사용 (Recording, Subtask 등)
- UI에서는 한글 용어 사용 (녹화, 단계 등)
- **Step은 사용하지 않음** → Subtask로 통일

### API 경로
- 녹화 관련: `/api/recordings/`
- 세션 관련: `/api/sessions/` (실시간 수업용)
- **URL 액션 하이픈**: Django REST Framework DefaultRouter는 언더스코어를 하이픈으로 변환
  - 예: `save_events_batch` → `save-events-batch`

### 변환 로직 위치
- 서비스: `backend/apps/sessions/services/lecture_conversion_service.py`
  - 클래스명: `TaskConversionService`
  - 메서드: `convert_to_task()`
- Celery 태스크: `backend/apps/sessions/tasks.py`
  - 함수명: `convert_recording_to_task_task()`

### 파일 명명 규칙
- Screen 파일: `{기능}Screen.kt` (예: RecordingListScreen.kt)
- ViewModel: `{기능}ViewModel.kt` (예: SubtaskViewModel.kt)
- DTO: `{기능}Request.kt`, `{기능}Response.kt`
