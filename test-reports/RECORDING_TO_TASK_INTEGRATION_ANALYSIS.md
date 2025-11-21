# 녹화에서 단계 추출 및 Task/Subtask 통합 분석

**작성일**: 2025-11-19
**작성자**: Claude Code

---

## 📋 목차

1. [개요](#개요)
2. [현재 구조 분석](#현재-구조-분석)
3. [데이터 플로우](#데이터-플로우)
4. [통합 상태 및 격차](#통합-상태-및-격차)
5. [개선 제안](#개선-제안)

---

## 개요

이 시스템은 **강사가 스마트폰에서 직접 시연한 동작을 녹화**하고, 그 녹화 데이터에서 **자동으로 강의 단계를 추출**하는 방식을 채택하고 있습니다.

### 목표
1. 강사가 스마트폰 앱 조작을 직접 녹화
2. AI가 녹화를 분석하여 단계별 가이드 자동 생성
3. 생성된 단계를 **Task/Subtask 구조로 변환**
4. 실시간 강의 시 해당 Task/Subtask를 순차 진행

---

## 현재 구조 분석

### 1. 데이터 모델 계층

```
RecordingSession (녹화 세션)
    ↓
ActivityLog (녹화된 이벤트)
    ↓
[변환 격차] ← 현재 미구현
    ↓
Lecture (강의)
    ↓
Task (큰 단위 작업)
    ↓
Subtask (세부 단계)
    ↓
LectureSession (실시간 강의)
```

---

### 2. RecordingSession 모델

**위치**: `/backend/apps/sessions/models.py:191-250`

```python
class RecordingSession(models.Model):
    """강의 녹화 세션 모델 (강의자의 시연 녹화)"""

    STATUS_CHOICES = [
        ('RECORDING', '녹화 중'),
        ('COMPLETED', '완료'),
        ('PROCESSING', '처리 중'),  # AI 분석 중
        ('FAILED', '실패'),
    ]

    instructor = ForeignKey(User)           # 강의자
    title = CharField(max_length=200)       # 녹화 제목
    description = TextField(blank=True)     # 설명
    status = CharField(max_length=20)       # 상태
    event_count = IntegerField(default=0)   # 이벤트 수
    duration_seconds = IntegerField()       # 녹화 시간(초)

    started_at = DateTimeField()            # 시작 시각
    ended_at = DateTimeField()              # 종료 시각

    # 📌 강의와의 연결 (녹화로부터 강의 생성 후)
    lecture = ForeignKey(
        Lecture,
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name='recordings'
    )
```

**핵심 포인트**:
- ✅ `lecture` ForeignKey로 강의와 연결 가능
- ✅ `event_count`로 녹화된 이벤트 수 추적
- ⚠️ 녹화 완료 후 `PROCESSING` 상태로 AI 분석 가능 (미구현)

---

### 3. ActivityLog 모델 (녹화 이벤트)

**위치**: `/backend/apps/logs/models.py`

```python
class ActivityLog(models.Model):
    """사용자 활동 로그 (녹화 이벤트)"""

    user = ForeignKey(User)
    recording_session = ForeignKey(RecordingSession)  # 녹화 세션 연결

    event_type = CharField(max_length=50)   # CLICK, SCROLL, INPUT 등
    event_data = JSONField()                # 상세 이벤트 데이터

    # UI 요소 정보
    view_id_resource_name = CharField()     # 터치한 UI 요소 ID
    content_description = TextField()       # 콘텐츠 설명
    bounds = CharField()                    # 화면 좌표

    # UI 상태
    is_clickable = BooleanField()
    is_editable = BooleanField()
    is_enabled = BooleanField()
    is_focused = BooleanField()

    timestamp = DateTimeField()
```

**핵심 포인트**:
- ✅ `recording_session`으로 녹화와 연결
- ✅ UI 요소 정보 상세 기록
- ✅ 이벤트 타입별 분류 가능
- 💡 이 데이터를 분석하여 **Subtask 생성 가능**

---

### 4. Task/Subtask 모델

**위치**: `/backend/apps/tasks/models.py`

```python
class Task(models.Model):
    """과제 모델 (강의 내의 큰 단위 작업)"""

    lecture = ForeignKey(Lecture)           # 강의와 연결
    title = CharField(max_length=255)       # 제목
    description = TextField(blank=True)     # 설명
    order_index = IntegerField()            # 순서
```

```python
class Subtask(models.Model):
    """세부 단계 모델 (Task를 구성하는 작은 단위)"""

    ACTION_CHOICES = [
        ('CLICK', '클릭'),
        ('LONG_CLICK', '길게 누르기'),
        ('SCROLL', '스크롤'),
        ('INPUT', '입력'),
        ('NAVIGATE', '화면 이동'),
    ]

    task = ForeignKey(Task)                 # Task와 연결
    title = CharField(max_length=255)       # 제목
    description = TextField(blank=True)     # 설명
    order_index = IntegerField()            # 순서

    # 📌 녹화 데이터와 매칭 가능한 필드
    target_action = CharField(              # 목표 액션
        choices=ACTION_CHOICES
    )
    target_element_hint = TextField()       # UI 요소 힌트
    guide_text = TextField()                # 안내 문구
    voice_guide_text = TextField()          # 음성 안내 문구
```

**핵심 포인트**:
- ✅ Task는 큰 작업 단위 (예: "네이버 지도 사용하기")
- ✅ Subtask는 세부 단계 (예: "앱 열기", "검색하기")
- ✅ `target_action`이 ActivityLog의 `event_type`과 매칭 가능
- ✅ `target_element_hint`가 ActivityLog의 `view_id_resource_name`과 매칭 가능

---

## 데이터 플로우

### 1. 녹화 단계 (Android 앱)

```
[강사 스마트폰]
    ↓
1. 강사가 "녹화 시작" 버튼 클릭
    ↓
2. POST /api/sessions/recordings/
   {
     "title": "유튜브 검색 시연",
     "description": "유튜브 앱 열기, 검색, 동영상 재생"
   }
   → RecordingSession 생성 (status: RECORDING)
    ↓
3. 강사가 실제 앱 조작 (유튜브 앱 사용)
   - 터치 이벤트가 AccessibilityService에 의해 캡처됨
   - 버퍼에 임시 저장
    ↓
4. 강사가 "녹화 중지" 버튼 클릭
    ↓
5. POST /api/sessions/recordings/{id}/save_events_batch/
   {
     "events": [
       {
         "eventType": "CLICK",
         "package": "com.google.android.youtube",
         "className": "com.google.android.youtube.MainActivity",
         "viewId": "com.android.launcher:id/icon",
         "text": "YouTube",
         "bounds": "[100,200][300,400]",
         "isClickable": true
       },
       {
         "eventType": "CLICK",
         "viewId": "com.google.android.youtube:id/search_button",
         "text": "",
         "contentDescription": "검색"
       },
       {
         "eventType": "INPUT",
         "viewId": "com.google.android.youtube:id/search_edit_text",
         "text": "고양이"
       },
       ...
     ]
   }
   → ActivityLog 레코드 생성 (bulk_create)
    ↓
6. POST /api/sessions/recordings/{id}/stop/
   → RecordingSession 상태 변경: RECORDING → COMPLETED
   → event_count, duration_seconds 계산
```

**구현 상태**: ✅ **완료** (recordings.py:108-171)

---

### 2. 강의 생성 단계 (프론트엔드)

```
[강사 웹 대시보드]
    ↓
1. "새 강의 추가" 버튼 클릭
    ↓
2. 기본 정보 입력 (제목, 설명, 난이도 등)
    ↓
3. "녹화 선택" 단계
   GET /api/sessions/recordings/
   → 사용 가능한 녹화 목록 조회
    ↓
4. 녹화 선택 후 "단계 자동 생성" 버튼 클릭
    ↓
5. processRecording(recordingId) 호출 (프론트엔드)
   ⚠️ 현재는 목 데이터 반환
   💡 실제로는 백엔드 AI 분석 API 호출 필요
    ↓
6. 생성된 LectureStep[] 표시
   - 강사가 각 단계 검토 및 수정 가능
    ↓
7. POST /api/lectures/
   {
     "title": "유튜브 동영상 검색하기",
     "description": "...",
     "steps": [
       {
         "order": 1,
         "title": "유튜브 앱 열기",
         "description": "...",
         "action": "홈 화면에서 유튜브 아이콘을 터치하세요",
         "expectedResult": "유튜브 앱이 실행됩니다"
       },
       ...
     ],
     "recordingId": "5"  // RecordingSession ID
   }
```

**구현 상태**:
- ✅ 프론트엔드 UI 완료 (lecture-form-page.tsx)
- ✅ 녹화 목록 조회 완료 (getAvailableRecordings)
- ⚠️ processRecording은 목 데이터 사용 (lecture-service.ts:454-533)
- ❌ 백엔드 강의 생성 시 `steps`, `recordingId` 저장 안 됨

---

### 3. Task/Subtask 변환 단계 (현재 격차)

```
[백엔드 처리 - 미구현]
    ↓
POST /api/lectures/ 호출 시
    ↓
1. Lecture 객체 생성
    ↓
2. ⚠️ LectureStep[] → Task/Subtask 변환 필요

   [제안] 변환 로직:
   - LectureStep 그룹핑 → Task 생성
   - 각 LectureStep → Subtask 생성
   - ActivityLog 데이터와 매칭하여 target_action, target_element_hint 설정
    ↓
3. RecordingSession.lecture 연결
   UPDATE recording_sessions
   SET lecture_id = {생성된 강의 ID}
   WHERE id = {recordingId}
```

**구현 상태**: ❌ **미구현** (백엔드 로직 필요)

---

### 4. 실시간 강의 진행 (현재 완료)

```
[실시간 강의]
    ↓
1. 강사가 세션 생성
   POST /api/lectures/{lecture_id}/sessions/create/
    ↓
2. 학생들이 세션 입장
   POST /api/sessions/{session_id}/join/
    ↓
3. 강사가 세션 시작 (첫 번째 Subtask 지정)
   POST /api/sessions/{session_id}/start/
   {
     "first_subtask_id": 1
   }
   → 모든 참가자가 Subtask 1 단계로 동기화
    ↓
4. 강사가 다음 단계로 이동
   POST /api/sessions/{session_id}/next-step/
   {
     "next_subtask_id": 2
   }
   → 모든 참가자가 Subtask 2 단계로 동기화
    ↓
5. 반복...
```

**구현 상태**: ✅ **완료** (sessions/views.py, 세션 제어 API 테스트 완료)

---

## 통합 상태 및 격차

### ✅ 구현 완료

| 기능 | 상태 | 파일 위치 |
|------|------|----------|
| 녹화 시작/종료 | ✅ | sessions/recordings.py |
| 녹화 이벤트 저장 | ✅ | sessions/recordings.py:108-171 |
| 녹화 목록 조회 | ✅ | sessions/recordings.py:25-37 |
| RecordingSession ↔ Lecture 연결 | ✅ | sessions/models.py:227-234 |
| Task/Subtask 수동 생성 | ✅ | tasks/views.py |
| 실시간 세션 제어 | ✅ | sessions/views.py:118-350 |

### ⚠️ 부분 구현

| 기능 | 상태 | 비고 |
|------|------|------|
| 프론트엔드 녹화 선택 UI | ⚠️ | 목 데이터 사용 중 |
| 강의 생성 시 녹화 연결 | ⚠️ | recordingId 전달되지만 저장 안 됨 |

### ❌ 미구현 (격차)

| 기능 | 우선순위 | 설명 |
|------|---------|------|
| AI 기반 단계 추출 | 🔴 높음 | ActivityLog 분석 → LectureStep 자동 생성 |
| LectureStep → Task/Subtask 변환 | 🔴 높음 | 강의 생성 시 자동 변환 |
| Lecture 모델에 steps 저장 | 🟡 중간 | JSONField로 저장 또는 별도 모델 |
| RecordingSession 처리 상태 관리 | 🟡 중간 | PROCESSING, FAILED 상태 처리 |
| 단계 자동 그룹핑 (Task 생성) | 🟢 낮음 | 비슷한 단계들을 Task로 묶기 |

---

## 개선 제안

### 제안 1: AI 기반 단계 추출 API 구현 (우선순위: 🔴 높음)

**목표**: ActivityLog 데이터를 분석하여 자동으로 LectureStep 생성

#### 1.1 백엔드 API 추가

**파일**: `/backend/apps/sessions/recordings.py`

```python
@action(detail=True, methods=['post'])
def process(self, request, pk=None):
    """
    POST /api/sessions/recordings/{id}/process/

    녹화를 분석하여 강의 단계를 자동 생성
    """
    recording = self.get_object()

    # 상태 확인
    if recording.status != 'COMPLETED':
        return Response(
            {'error': '완료된 녹화만 처리할 수 있습니다.'},
            status=status.HTTP_400_BAD_REQUEST
        )

    # 상태를 PROCESSING으로 변경
    recording.status = 'PROCESSING'
    recording.save()

    try:
        # ActivityLog 조회
        events = ActivityLog.objects.filter(
            recording_session=recording
        ).order_by('timestamp')

        # AI 분석 (비동기 Celery 태스크로 처리 권장)
        from .tasks import analyze_recording
        result = analyze_recording.delay(recording.id)

        return Response({
            'message': '녹화 분석을 시작했습니다.',
            'task_id': result.id,
            'recording_id': recording.id
        })

    except Exception as e:
        recording.status = 'FAILED'
        recording.save()
        raise
```

#### 1.2 Celery 태스크로 AI 분석

**파일**: `/backend/apps/sessions/tasks.py`

```python
from celery import shared_task
from .models import RecordingSession
from apps.logs.models import ActivityLog
import json

@shared_task
def analyze_recording(recording_id):
    """
    녹화를 분석하여 단계 추출

    1. ActivityLog 이벤트 그룹핑
    2. 반복 패턴 감지
    3. 주요 UI 요소 식별
    4. 단계별 설명 생성
    """
    recording = RecordingSession.objects.get(id=recording_id)
    events = ActivityLog.objects.filter(
        recording_session=recording
    ).order_by('timestamp')

    # 1. 이벤트 그룹핑 (패키지별, 액션별)
    groups = []
    current_group = []
    last_package = None

    for event in events:
        package = event.event_data.get('package', '')

        # 앱이 변경되면 새로운 그룹
        if package != last_package and current_group:
            groups.append(current_group)
            current_group = []

        current_group.append(event)
        last_package = package

    if current_group:
        groups.append(current_group)

    # 2. 각 그룹을 LectureStep으로 변환
    steps = []
    for idx, group in enumerate(groups, 1):
        step = generate_step_from_events(group, idx)
        steps.append(step)

    # 3. 결과를 RecordingSession에 저장 (JSONField 추가 필요)
    recording.extracted_steps = steps
    recording.status = 'COMPLETED'
    recording.save()

    return {
        'recording_id': recording.id,
        'step_count': len(steps)
    }


def generate_step_from_events(events, order):
    """이벤트 그룹에서 단계 생성"""
    first_event = events[0]
    package = first_event.event_data.get('package', '')

    # 앱 이름 추출
    app_name = get_app_name(package)

    # 주요 액션 식별
    main_action = identify_main_action(events)

    # 단계 설명 생성
    return {
        'order': order,
        'title': f'{order}단계: {main_action}',
        'description': f'{app_name}에서 {main_action}',
        'action': generate_action_instruction(events),
        'expectedResult': generate_expected_result(events),
        'technicalDetails': {
            'targetPackage': package,
            'targetViewId': first_event.view_id_resource_name,
            'targetAction': main_action
        }
    }


def identify_main_action(events):
    """이벤트 그룹의 주요 액션 식별"""
    # 클릭이 가장 많으면 "클릭"
    # 입력이 있으면 "입력"
    # 스크롤이 많으면 "탐색"
    event_types = [e.event_type for e in events]

    if 'INPUT' in event_types:
        return '입력하기'
    elif event_types.count('CLICK') > 3:
        return '선택하기'
    elif 'SCROLL' in event_types:
        return '탐색하기'
    else:
        return '터치하기'


def generate_action_instruction(events):
    """사용자에게 보여줄 액션 설명 생성"""
    first_event = events[0]

    if first_event.event_type == 'CLICK':
        target = first_event.content_description or first_event.event_data.get('text', '버튼')
        return f'"{target}"을(를) 터치하세요'

    elif first_event.event_type == 'INPUT':
        return '검색창에 원하는 내용을 입력하세요'

    elif first_event.event_type == 'SCROLL':
        return '화면을 위아래로 스크롤하여 탐색하세요'

    return '화면의 해당 요소를 터치하세요'


def generate_expected_result(events):
    """예상 결과 생성"""
    last_event = events[-1]

    # 다음 화면으로 이동했는지 확인
    if any(e.event_type == 'NAVIGATE' for e in events):
        return '새로운 화면이 나타납니다'

    # 입력 후에는
    if any(e.event_type == 'INPUT' for e in events):
        return '입력한 내용이 표시되고 관련 결과가 나타납니다'

    return '선택한 항목이 활성화됩니다'


def get_app_name(package):
    """패키지명에서 앱 이름 추출"""
    app_names = {
        'com.google.android.youtube': '유튜브',
        'com.nhn.android.nmap': '네이버 지도',
        'com.instagram.android': '인스타그램',
        'com.sampleapp': '배달의민족',
    }
    return app_names.get(package, package.split('.')[-1])
```

---

### 제안 2: LectureStep → Task/Subtask 자동 변환 (우선순위: 🔴 높음)

**목표**: 강의 생성 시 자동으로 Task/Subtask 구조로 변환

#### 2.1 Lecture 모델에 RecordingSession 연결

**파일**: `/backend/apps/lectures/models.py`

```python
class Lecture(models.Model):
    # ... 기존 필드 ...

    # 📌 녹화 세션 연결 추가
    recording_session = models.ForeignKey(
        'sessions.RecordingSession',
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name='created_lectures',
        verbose_name='원본 녹화'
    )
```

#### 2.2 강의 생성 시 자동 변환 로직

**파일**: `/backend/apps/lectures/views.py`

```python
from apps.sessions.models import RecordingSession
from apps.tasks.models import Task, Subtask

class LectureListCreateView(generics.ListCreateAPIView):
    # ...

    def perform_create(self, serializer):
        """강의 생성 시 녹화 데이터를 Task/Subtask로 변환"""
        recording_id = self.request.data.get('recording_id')
        steps = self.request.data.get('steps', [])

        # 1. 강의 생성
        lecture = serializer.save(instructor=self.request.user)

        # 2. RecordingSession 연결
        if recording_id:
            try:
                recording = RecordingSession.objects.get(id=recording_id)
                lecture.recording_session = recording
                lecture.save()

                # RecordingSession의 lecture 필드도 업데이트
                recording.lecture = lecture
                recording.save()
            except RecordingSession.DoesNotExist:
                pass

        # 3. LectureStep → Task/Subtask 변환
        if steps:
            self.create_tasks_from_steps(lecture, steps)

        return lecture

    def create_tasks_from_steps(self, lecture, steps):
        """LectureStep 배열을 Task/Subtask 구조로 변환"""

        # 단계 그룹핑 (앱별로 Task 생성)
        task_groups = self.group_steps_by_app(steps)

        for task_idx, (app_name, task_steps) in enumerate(task_groups.items(), 1):
            # Task 생성
            task = Task.objects.create(
                lecture=lecture,
                title=f'{app_name} 사용하기',
                description=f'{app_name} 앱을 사용하는 방법',
                order_index=task_idx
            )

            # Subtask 생성
            for step in task_steps:
                Subtask.objects.create(
                    task=task,
                    title=step.get('title', ''),
                    description=step.get('description', ''),
                    order_index=step.get('order', 0),
                    target_action=self.map_action_type(step),
                    target_element_hint=self.extract_target_element(step),
                    guide_text=step.get('action', ''),
                    voice_guide_text=step.get('action', '')
                )

    def group_steps_by_app(self, steps):
        """단계를 앱별로 그룹핑"""
        from collections import defaultdict
        groups = defaultdict(list)

        for step in steps:
            # technicalDetails에서 앱 정보 추출
            tech = step.get('technicalDetails', {})
            package = tech.get('targetPackage', 'unknown')
            app_name = self.get_app_name(package)
            groups[app_name].append(step)

        return groups

    def map_action_type(self, step):
        """LectureStep의 액션을 Subtask.ACTION_CHOICES로 매핑"""
        action_text = step.get('action', '').lower()

        if '터치' in action_text or '클릭' in action_text:
            return 'CLICK'
        elif '길게' in action_text:
            return 'LONG_CLICK'
        elif '스크롤' in action_text:
            return 'SCROLL'
        elif '입력' in action_text:
            return 'INPUT'
        elif '이동' in action_text:
            return 'NAVIGATE'

        return 'CLICK'  # 기본값

    def extract_target_element(self, step):
        """단계에서 타겟 UI 요소 추출"""
        tech = step.get('technicalDetails', {})
        view_id = tech.get('targetViewId', '')
        text = tech.get('targetText', '')

        if view_id:
            return f'View ID: {view_id}'
        elif text:
            return f'Text: {text}'

        return ''

    def get_app_name(self, package):
        """패키지명에서 앱 이름 추출"""
        app_names = {
            'com.google.android.youtube': '유튜브',
            'com.nhn.android.nmap': '네이버 지도',
            'com.instagram.android': '인스타그램',
        }
        return app_names.get(package, '앱')
```

#### 2.3 Serializer 수정

**파일**: `/backend/apps/lectures/serializers.py`

```python
class LectureCreateUpdateSerializer(serializers.ModelSerializer):
    """Lecture creation/update serializer"""

    # 추가 필드
    recording_id = serializers.IntegerField(
        required=False,
        write_only=True,
        help_text='녹화 세션 ID'
    )
    steps = serializers.JSONField(
        required=False,
        write_only=True,
        help_text='강의 단계 배열'
    )

    class Meta:
        model = Lecture
        fields = [
            'title', 'description', 'thumbnail_url', 'is_active',
            'recording_id', 'steps'  # 추가
        ]
```

---

### 제안 3: 데이터베이스 마이그레이션

```bash
# 1. Lecture 모델에 recording_session 필드 추가
python manage.py makemigrations lectures

# 2. RecordingSession 모델에 extracted_steps JSONField 추가
python manage.py makemigrations sessions

# 3. 마이그레이션 적용
python manage.py migrate
```

**추가할 필드**:

```python
# sessions/models.py - RecordingSession
extracted_steps = models.JSONField(
    default=list,
    blank=True,
    verbose_name='추출된 단계',
    help_text='AI가 분석하여 추출한 강의 단계'
)
```

---

### 제안 4: 프론트엔드 processRecording API 연결

**파일**: `/frontend/src/lib/lecture-service.ts`

```typescript
async processRecording(recordingId: string): Promise<RecordingProcessResponse> {
  try {
    // ✅ 백엔드 API 호출로 변경
    const response = await apiClient.post(
      `/sessions/recordings/${recordingId}/process/`
    );

    // 태스크 ID 받기
    const taskId = response.data.task_id;

    // 폴링으로 결과 확인
    return await this.pollProcessingStatus(recordingId, taskId);

  } catch (error) {
    console.error('Failed to process recording:', error);
    throw error;
  }
}

async pollProcessingStatus(
  recordingId: string,
  taskId: string
): Promise<RecordingProcessResponse> {
  // 5초마다 상태 확인
  const maxAttempts = 60; // 최대 5분

  for (let i = 0; i < maxAttempts; i++) {
    await delay(5000);

    const response = await apiClient.get(
      `/sessions/recordings/${recordingId}/`
    );

    const recording = response.data;

    if (recording.status === 'COMPLETED' && recording.extracted_steps) {
      // 추출된 단계를 LectureStep 형식으로 변환
      return {
        success: true,
        recordingId,
        generatedSteps: recording.extracted_steps.map((step: any) => ({
          id: `${recordingId}-step-${step.order}`,
          order: step.order,
          title: step.title,
          description: step.description,
          action: step.action,
          expectedResult: step.expectedResult,
          technicalDetails: step.technicalDetails
        }))
      };
    }

    if (recording.status === 'FAILED') {
      throw new Error('녹화 처리에 실패했습니다');
    }

    // 계속 PROCESSING 중
  }

  throw new Error('처리 시간이 초과되었습니다');
}
```

---

## 전체 통합 플로우 (개선 후)

```
┌─────────────────────────────────────────────────────────────┐
│ 1. 녹화 단계 (Android 앱)                                    │
├─────────────────────────────────────────────────────────────┤
│ ① 강사가 스마트폰에서 시연 녹화                              │
│ ② POST /api/sessions/recordings/ → RecordingSession 생성     │
│ ③ AccessibilityService로 이벤트 캡처                         │
│ ④ POST /api/sessions/recordings/{id}/save_events_batch/     │
│    → ActivityLog 생성                                        │
│ ⑤ POST /api/sessions/recordings/{id}/stop/                  │
│    → status: COMPLETED                                       │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 2. AI 분석 단계 (백엔드) - 제안                              │
├─────────────────────────────────────────────────────────────┤
│ ⑥ POST /api/sessions/recordings/{id}/process/               │
│    → Celery 태스크 실행                                      │
│ ⑦ ActivityLog 분석                                          │
│    - 이벤트 그룹핑                                           │
│    - 패턴 인식                                               │
│    - UI 요소 식별                                            │
│ ⑧ extracted_steps 생성 및 저장                              │
│    → status: COMPLETED                                       │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 3. 강의 생성 단계 (프론트엔드 + 백엔드)                      │
├─────────────────────────────────────────────────────────────┤
│ ⑨ 프론트엔드에서 녹화 선택                                   │
│ ⑩ 추출된 단계(extracted_steps) 표시                         │
│ ⑪ 강사가 단계 검토 및 수정                                  │
│ ⑫ POST /api/lectures/                                       │
│    {                                                         │
│      "title": "...",                                         │
│      "recording_id": 5,                                      │
│      "steps": [...]                                          │
│    }                                                         │
│ ⑬ Lecture 생성                                              │
│ ⑭ RecordingSession 연결                                     │
│ ⑮ Task/Subtask 자동 생성                                    │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 4. 실시간 강의 진행 (이미 완료)                              │
├─────────────────────────────────────────────────────────────┤
│ ⑯ LectureSession 생성                                       │
│ ⑰ 학생 입장                                                 │
│ ⑱ 세션 시작 (first_subtask_id 지정)                        │
│ ⑲ 단계별 진행 (next-step, pause, resume)                   │
│ ⑳ 세션 종료                                                 │
└─────────────────────────────────────────────────────────────┘
```

---

## 구현 우선순위 및 작업량 추정

| 순위 | 작업 | 예상 작업량 | 의존성 |
|------|------|-----------|--------|
| 1 | RecordingSession에 extracted_steps 필드 추가 | 0.5시간 | - |
| 2 | Lecture 모델에 recording_session 필드 추가 | 0.5시간 | - |
| 3 | 기본 AI 분석 로직 구현 (이벤트 그룹핑) | 4시간 | 1 |
| 4 | LectureStep → Task/Subtask 변환 로직 | 3시간 | 2 |
| 5 | processRecording API 엔드포인트 | 2시간 | 3 |
| 6 | Celery 태스크로 비동기 처리 | 2시간 | 5 |
| 7 | 프론트엔드 API 연결 (폴링) | 2시간 | 5 |
| 8 | AI 고도화 (NLP, 패턴 인식) | 8시간+ | 3 |

**총 예상**: 22시간 (약 3일)

---

## 결론

### 현재 상태
- ✅ 녹화 기능 완성 (RecordingSession, ActivityLog)
- ✅ Task/Subtask 구조 완성
- ✅ 실시간 세션 제어 완성
- ❌ **녹화 → Task/Subtask 자동 변환 미구현**

### 핵심 격차
**LectureStep (프론트엔드) ↔ Task/Subtask (백엔드)** 간의 자동 변환 로직이 없음

### 해결 방안
1. **단기**: 수동으로 Task/Subtask 생성 (현재 방식)
2. **중기**: 간단한 이벤트 그룹핑으로 자동 변환
3. **장기**: AI/NLP 기반 고도화된 단계 추출

### 다음 단계
1. ✅ RecordingSession.extracted_steps 필드 추가
2. ✅ Lecture.recording_session 필드 추가
3. ✅ 기본 AI 분석 로직 구현
4. ✅ LectureStep → Task/Subtask 자동 변환
5. ✅ 프론트엔드 연결

---

**보고서 작성일**: 2025-11-19
**작성자**: Claude Code
