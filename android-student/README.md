# MobEdu - 학생용 Android 앱

시니어를 위한 디지털 교육 도우미 - 수강생용 Android 앱

## 📱 프로젝트 개요

MobEdu Student App은 시니어 사용자가 실시간 강의 세션에 참가하고, AccessibilityService를 통해 학습 과정을 기록하며, 강사의 실시간 가이드를 받을 수 있는 Android 애플리케이션입니다.

## 🏗️ 기술 스택

- **언어**: Kotlin
- **최소 SDK**: API 30 (Android 11.0)
- **아키텍처**: MVVM + Clean Architecture
- **UI**: Jetpack Compose
- **DI**: Hilt
- **비동기**: Coroutines + Flow
- **네트워킹**:
  - REST API: Retrofit + OkHttp
  - WebSocket: Scarlet
- **로컬 저장소**: DataStore

## 📂 프로젝트 구조

```
app/src/main/java/com/mobilegpt/student/
├── data/
│   ├── api/              # API 인터페이스
│   │   ├── StudentApi.kt      # REST API
│   │   └── WebSocketApi.kt    # WebSocket
│   ├── repository/       # Repository 구현
│   │   └── SessionRepository.kt
│   └── local/           # 로컬 데이터 (향후 구현)
│
├── domain/
│   ├── model/           # 도메인 모델
│   │   ├── ActivityLog.kt
│   │   ├── WebSocketMessage.kt
│   │   └── ...
│   └── usecase/         # Use Cases (향후 구현)
│
├── presentation/
│   ├── ui/              # Compose UI (향후 구현)  cd backend
  source venv/bin/activate
  python manage.py runserver
│   ├── viewmodel/       # ViewModels (향후 구현)
│   └── MainActivity.kt
│
├── service/
│   └── MobileGPTAccessibilityService.kt  # 접근성 서비스
│
├── di/
│   └── NetworkModule.kt  # Hilt DI 모듈
│
└── MobileGPTApplication.kt
```

## 🔑 핵심 기능

### 1. AccessibilityService
- UI 이벤트 감지 (클릭, 스크롤, 텍스트 입력)
- 사용자 행동 로그 수집
- 실시간 로그 전송

### 2. 세션 관리
- 세션 코드로 실시간 강의 참가
- WebSocket을 통한 강사와 실시간 연결
- 단계별 학습 진행 동기화

### 3. 실시간 통신
- REST API: 세션 참가, 목록 조회
- WebSocket: 양방향 실시간 메시징

## 🚀 시작하기

### 사전 요구사항

- Android Studio Hedgehog (2023.1.1) 이상
- JDK 17
- Android SDK 34
- Gradle 8.2

### 백엔드 서버 설정

1. Django 서버 실행:
```bash
cd ../backend
python manage.py runserver
```

2. `app/build.gradle.kts`에서 API URL 확인:
```kotlin
buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8000/api/\"")
buildConfigField("String", "WS_BASE_URL", "\"ws://10.0.2.2:8000/ws/\"")
```

> **참고**: `10.0.2.2`는 Android Emulator에서 호스트 머신의 localhost를 가리킵니다.
> 실제 기기에서 테스트할 경우 서버의 실제 IP 주소로 변경하세요.

### 빌드 및 실행

1. 프로젝트 열기:
```bash
cd android-student
```

2. Android Studio에서 프로젝트 열기

3. Gradle Sync 수행

4. 에뮬레이터 또는 실제 기기에서 실행

## 🔐 권한

앱이 요구하는 권한:

- `INTERNET` - 서버 통신
- `ACCESS_NETWORK_STATE` - 네트워크 상태 확인
- `SYSTEM_ALERT_WINDOW` - 오버레이 표시 (도움말 UI)
- `BIND_ACCESSIBILITY_SERVICE` - 접근성 서비스 (UI 이벤트 감지)

### AccessibilityService 활성화

1. 설정 → 접근성
2. "MobileGPT 학습 도우미" 선택
3. 서비스 활성화

## 📡 API 엔드포인트

### REST API

```kotlin
// 세션 참가
POST /api/students/sessions/join/
Body: { "session_code": "ABC123" }

// 내 세션 목록
GET /api/students/sessions/my_sessions/

// 진행 중인 세션
GET /api/students/sessions/active_sessions/

// 세션 나가기
POST /api/students/sessions/{id}/leave/
```

### WebSocket

```
ws://server/ws/session/{session_code}/
```

**메시지 타입**:
- Client → Server: `join`, `heartbeat`, `step_complete`, `request_help`
- Server → Client: `step_update`, `session_start`, `session_end`, `help_response`

## 🧪 테스트

### 기본 테스트 시나리오

1. **세션 참가**:
   - 앱 실행
   - "세션 참가" 버튼 클릭
   - 강사로부터 받은 6자리 코드 입력

2. **AccessibilityService 테스트**:
   - 접근성 서비스 활성화
   - 다른 앱 사용 (예: 카카오톡)
   - Logcat에서 `MobileGPT_A11y` 태그로 로그 확인

3. **WebSocket 연결 테스트**:
   - 세션 참가 후 연결 상태 확인
   - 강사의 단계 전환 메시지 수신 확인

## 🔧 개발 참고사항

### Hilt Dependency Injection

```kotlin
@HiltAndroidApp
class MobileGPTApplication : Application()

@AndroidEntryPoint
class MainActivity : ComponentActivity()
```

### Repository 사용 예시

```kotlin
@Inject
lateinit var sessionRepository: SessionRepository

// 세션 참가
viewModelScope.launch {
    val result = sessionRepository.joinSession("ABC123")
    result.onSuccess { response ->
        // 성공 처리
    }
}

// WebSocket 메시지 수신
sessionRepository.observeSessionMessages()
    .collect { message ->
        when (message.type) {
            MessageType.STEP_UPDATE -> // 단계 업데이트
            MessageType.HELP_RESPONSE -> // 도움말 수신
        }
    }
```

## 📋 향후 개발 계획

- [ ] UI 화면 구현 (Jetpack Compose)
- [ ] ViewModel 및 State 관리
- [ ] 세션 참가 화면
- [ ] 실시간 단계 표시 화면
- [ ] 오버레이 도움말 UI
- [ ] 로컬 로그 캐싱 (Room DB)
- [ ] JWT 인증 구현
- [ ] 푸시 알림
- [ ] 오프라인 모드

## 🐛 문제 해결

### Gradle Sync 실패
```bash
./gradlew clean build --refresh-dependencies
```

### AccessibilityService가 작동하지 않음
- 설정에서 서비스가 활성화되었는지 확인
- 앱 권한에서 "다른 앱 위에 표시" 권한 확인

### WebSocket 연결 실패
- 백엔드 서버가 실행 중인지 확인
- `build.gradle.kts`의 API URL 확인
- 방화벽 설정 확인

## 📄 라이선스

이 프로젝트는 교육 목적으로 개발되었습니다.

---

**최종 업데이트**: 2025-12-18

**프로젝트**: [MobEdu](../README.md) | CPS 2025-2
