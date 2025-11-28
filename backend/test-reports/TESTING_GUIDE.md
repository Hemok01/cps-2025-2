# MobileGPT AccessibilityService 로그 전송 테스트 가이드

## 완성된 기능

✅ **백엔드 (Django)**
- ActivityLog API 엔드포인트 (`POST /api/logs/activity/`)
- 테스트 데이터 생성 (사용자, 강의, 세션)
- JWT 인증 설정

✅ **Android (Student App)**
- StudentApi에 ActivityLog 전송 엔드포인트 추가
- SessionRepository에 sendActivityLog 메서드 구현
- MobileGPTAccessibilityService의 sendLogToServer() 구현
- SharedPreferences를 통한 세션 ID 관리
- MainActivity에서 테스트용 세션 ID 설정

## 테스트 환경 설정

### 1. 백엔드 서버 시작

```bash
cd backend

# 의존성이 설치되지 않았다면
source venv/bin/activate
pip install -r requirements.txt

# 테스트 데이터 생성 (이미 생성되어 있음)
python manage.py create_test_data

# 서버 시작
./start_server.sh
# 또는
python manage.py runserver
```

서버가 시작되면 다음 주소에서 접근 가능합니다:
- API: http://localhost:8000/api/
- Admin: http://localhost:8000/admin/

### 2. Android 앱 설정

#### 네트워크 설정 확인
Android 에뮬레이터에서 localhost 접근을 위해 `10.0.2.2`를 사용합니다.

`android-student/app/src/main/java/com/mobilegpt/student/di/NetworkModule.kt` 파일 확인:
```kotlin
private const val BASE_URL = "http://10.0.2.2:8000/api/"
```

#### 권한 설정
`AndroidManifest.xml`에 다음 권한이 있는지 확인:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### 3. JWT 토큰 획득

AccessibilityService가 로그를 전송하려면 인증 토큰이 필요합니다.

```bash
# 학생 계정으로 토큰 획득
curl -X POST http://localhost:8000/api/token/ \
  -H "Content-Type: application/json" \
  -d '{
    "email": "student1@example.com",
    "password": "student123"
  }'
```

응답 예시:
```json
{
  "access": "eyJ0eXAiOiJKV1QiLCJhbGc...",
  "refresh": "eyJ0eXAiOiJKV1QiLCJhbGc..."
}
```

**중요**: Android 앱에서 이 토큰을 저장하고 API 요청 시 헤더에 포함해야 합니다.
현재 MVP 구현에서는 Hilt의 네트워크 모듈에 Interceptor를 추가하여 자동으로 토큰을 포함시켜야 합니다.

## 테스트 시나리오

### 시나리오 1: 기본 로그 전송 테스트

1. **백엔드 서버 시작**
   ```bash
   cd backend
   ./start_server.sh
   ```

2. **Android Studio에서 앱 실행**
   - 에뮬레이터 또는 실제 기기에서 앱 실행

3. **세션 참가**
   - 앱의 "세션 참가 (테스트)" 버튼 클릭
   - Toast 메시지 확인: "테스트 세션에 참가했습니다 (Session ID: 1)"

4. **AccessibilityService 활성화**
   - 설정 > 접근성 > MobileGPT Accessibility Service 활성화

5. **이벤트 발생**
   - 앱 내에서 버튼 클릭, 스크롤 등의 이벤트 발생
   - Logcat에서 로그 확인:
     ```
     MobileGPT_A11y: Log sent successfully: 123
     MobileGPT_A11y: Event: CLICK, Package: com.mobilegpt.student, Element: 세션 참가 (테스트)
     ```

6. **백엔드에서 로그 확인**
   ```bash
   # Django shell에서 확인
   python manage.py shell
   ```

   ```python
   from apps.logs.models import ActivityLog

   # 최근 로그 확인
   logs = ActivityLog.objects.all().order_by('-timestamp')[:10]
   for log in logs:
       print(f"{log.timestamp}: {log.event_type} - {log.user.name}")
   ```

   또는 Admin 패널에서 확인:
   - http://localhost:8000/admin/
   - admin@example.com / admin123로 로그인
   - Activity Logs 메뉴에서 확인

### 시나리오 2: API 직접 테스트

토큰을 획득한 후 직접 API 호출:

```bash
# 토큰 변수 설정
TOKEN="your_access_token_here"

# ActivityLog 전송
curl -X POST http://localhost:8000/api/logs/activity/ \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "session": 1,
    "subtask": null,
    "event_type": "CLICK",
    "event_data": {
      "package_name": "com.mobilegpt.student",
      "element_text": "Test Button"
    },
    "screen_info": {
      "title": "Main Screen"
    },
    "node_info": {
      "element_id": "button_test",
      "text": "Test Button"
    },
    "view_id_resource_name": "button_test",
    "content_description": "Test Button",
    "is_sensitive_data": false
  }'
```

성공 시 응답:
```json
{
  "log_id": 1,
  "message": "Log saved successfully"
}
```

## 문제 해결

### 1. "Failed to send log: 401 Unauthorized"

**문제**: 인증 토큰이 없거나 만료됨

**해결**:
- 토큰을 다시 획득
- NetworkModule에 AuthInterceptor 추가하여 자동으로 토큰 포함

### 2. "Failed to send log: 400 Bad Request"

**문제**: 요청 데이터 형식 오류

**해결**:
- Logcat에서 자세한 오류 메시지 확인
- 백엔드 로그 확인: `python manage.py runserver` 출력

### 3. "Repository not initialized yet"

**문제**: AccessibilityService가 아직 완전히 초기화되지 않음

**해결**:
- 서비스를 비활성화했다가 다시 활성화
- 앱을 재시작

### 4. "Connection refused" 또는 네트워크 오류

**문제**: 백엔드 서버에 연결할 수 없음

**해결**:
- 백엔드 서버가 실행 중인지 확인
- 에뮬레이터에서 `10.0.2.2` 사용 확인
- 실제 기기에서는 컴퓨터의 로컬 IP 사용 (예: `192.168.1.100`)

## 다음 단계 개선 사항

현재 MVP 구현에서 추가해야 할 사항:

1. **JWT 토큰 관리**
   - TokenInterceptor 구현
   - 토큰 저장 및 자동 갱신
   - 로그인 화면 구현

2. **오프라인 지원**
   - 로컬 데이터베이스에 로그 저장
   - 네트워크 연결 시 배치 전송

3. **재시도 로직**
   - 실패한 요청 재시도
   - Exponential backoff 구현

4. **세션 관리**
   - 실제 세션 참가 API 연동
   - 세션 상태 추적

5. **에러 처리**
   - 사용자에게 친화적인 에러 메시지
   - 에러 로깅 및 리포팅

## 테스트 체크리스트

- [ ] 백엔드 서버 정상 실행
- [ ] 테스트 데이터 생성 완료
- [ ] Android 앱 빌드 및 실행 성공
- [ ] AccessibilityService 활성화
- [ ] 세션 ID 저장 확인
- [ ] 로그 전송 성공 (Logcat 확인)
- [ ] 백엔드 데이터베이스에 로그 저장 확인
- [ ] Admin 패널에서 로그 확인

## 성공 기준

✅ AccessibilityService가 UI 이벤트를 감지하고 로그 생성
✅ 생성된 로그가 백엔드 API로 전송
✅ 백엔드에서 로그를 받아 데이터베이스에 저장
✅ Admin 패널 또는 Django shell에서 로그 조회 가능
✅ 세션 ID가 로그에 올바르게 포함됨

모든 항목이 완료되면 AccessibilityService 로그 전송 기능이 정상적으로 동작하는 것입니다!
