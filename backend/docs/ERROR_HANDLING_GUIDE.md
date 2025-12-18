# MobileGPT 에러 핸들링 가이드

> **Version**: 1.0.0
> **Last Updated**: 2024-12-07

---

## 목차

1. [에러 응답 형식](#1-에러-응답-형식)
2. [에러 코드 목록](#2-에러-코드-목록)
3. [잠재적 에러 분석](#3-잠재적-에러-분석)
4. [해결 방안](#4-해결-방안)
5. [커스텀 예외 클래스](#5-커스텀-예외-클래스)

---

## 1. 에러 응답 형식

모든 API 에러는 일관된 형식으로 반환됩니다:

```json
{
  "error": {
    "code": "ERROR_CODE",
    "message": "한국어 에러 메시지",
    "details": { ... },
    "field_errors": { ... }
  },
  "status": 400
}
```

### 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| `code` | string | 에러 식별 코드 (예: `AUTH_FAILED`) |
| `message` | string | 사용자 친화적 한국어 메시지 |
| `details` | object | 추가 에러 정보 |
| `field_errors` | object | 필드별 유효성 검증 에러 (ValidationError 시) |
| `status` | number | HTTP 상태 코드 |

### 유효성 검증 에러 예시

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "입력 데이터가 유효하지 않습니다.",
    "field_errors": {
      "email": ["이미 사용 중인 이메일입니다."],
      "password": ["비밀번호는 8자 이상이어야 합니다."]
    }
  },
  "status": 400
}
```

---

## 2. 에러 코드 목록

### 인증 관련

| 코드 | HTTP 상태 | 설명 |
|------|----------|------|
| `AUTH_FAILED` | 401 | 인증 실패 (잘못된 자격 증명) |
| `AUTH_REQUIRED` | 401 | 인증 필요 (토큰 미제공) |
| `PERMISSION_DENIED` | 403 | 권한 없음 |
| `TOKEN_EXPIRED` | 401 | JWT 토큰 만료 |
| `TOKEN_BLACKLISTED` | 401 | 블랙리스트 처리된 토큰 |

### 리소스 관련

| 코드 | HTTP 상태 | 설명 |
|------|----------|------|
| `NOT_FOUND` | 404 | 리소스 없음 |
| `SESSION_NOT_FOUND` | 404 | 세션 없음 |
| `LECTURE_NOT_FOUND` | 404 | 강의 없음 |

### 세션 관련

| 코드 | HTTP 상태 | 설명 |
|------|----------|------|
| `SESSION_NOT_STARTED` | 400 | 세션 미시작 |
| `SESSION_ENDED` | 400 | 세션 이미 종료 |
| `INSTRUCTOR_ONLY` | 403 | 강사 전용 기능 |
| `DEVICE_ID_REQUIRED` | 400 | device_id 필요 |

### 수강 관련

| 코드 | HTTP 상태 | 설명 |
|------|----------|------|
| `ALREADY_ENROLLED` | 409 | 이미 수강 중 |
| `NOT_ENROLLED` | 400 | 수강 중 아님 |

### 기타

| 코드 | HTTP 상태 | 설명 |
|------|----------|------|
| `VALIDATION_ERROR` | 400 | 유효성 검증 실패 |
| `RATE_LIMIT_EXCEEDED` | 429 | 요청 한도 초과 |
| `METHOD_NOT_ALLOWED` | 405 | 허용되지 않은 HTTP 메서드 |
| `INTEGRITY_ERROR` | 400 | 데이터 무결성 오류 |
| `SERVER_ERROR` | 500 | 서버 내부 오류 |

---

## 3. 잠재적 에러 분석

### 3.1 인증 관련 에러

#### JWT 토큰 만료
```
에러: 401 Unauthorized
코드: AUTH_FAILED
메시지: "Token is invalid or expired"
```

**원인**:
- ACCESS_TOKEN_LIFETIME: 1시간으로 설정
- 장시간 사용 시 토큰 만료

**해결방안**:
```kotlin
// Android 앱에서 자동 갱신 로직 구현
class TokenInterceptor : Interceptor {
    override fun intercept(chain: Chain): Response {
        val response = chain.proceed(request)
        if (response.code == 401) {
            // refresh 토큰으로 새 access 토큰 발급
            val newToken = refreshAccessToken()
            // 원래 요청 재시도
        }
        return response
    }
}
```

#### Refresh 토큰 블랙리스트
```
에러: 401 Unauthorized
코드: AUTH_FAILED
메시지: "Token is blacklisted"
```

**원인**:
- 로그아웃 후 동일 refresh 토큰 사용 시도
- `BLACKLIST_AFTER_ROTATION: True` 설정

**해결방안**:
- 토큰 갱신 시 새 refresh 토큰 저장
- 로그아웃 시 로컬 토큰 완전 삭제

---

### 3.2 WebSocket 연결 에러

#### WebSocket 인증 실패
```
에러: WebSocket connection closed (4001 - Unauthorized)
```

**원인**:
- 쿼리 파라미터에 token 미포함
- JWT 토큰 만료/무효

**해결방안**:
```kotlin
// Android에서 WebSocket 연결 시 토큰 포함
val wsUrl = "ws://host/ws/sessions/$code/?token=$accessToken"
```

#### Channel Layer 연결 실패
```
에러: "Could not connect to Redis"
```

**해결방안**:
```bash
# Redis 실행 확인
docker-compose up -d redis

# 또는 개발 환경에서 InMemory 사용
USE_REDIS_CHANNELS=False
```

---

### 3.3 세션 관련 에러

#### 세션 상태 불일치
```
에러: 400 Bad Request
코드: SESSION_NOT_STARTED
메시지: "세션이 시작되지 않았습니다"
```

**원인**: 세션 상태가 WAITING인데 next-step 호출

**해결방안**:
```python
# 상태 확인 후 적절한 에러 반환
if session.status != 'IN_PROGRESS':
    raise SessionNotStartedError()
```

#### Race Condition (동시성 문제)
```
에러: 중복 참가자 생성, 잘못된 완료 카운트
```

**해결방안**:
```python
from django.db import transaction

@transaction.atomic
def report_completion(request, session_id):
    participant = SessionParticipant.objects.select_for_update().get(
        session_id=session_id,
        device_id=request.data.get('device_id')
    )
    # 업데이트 로직
```

---

### 3.4 파일 업로드 에러

#### Base64 디코딩 실패
```
에러: 400 Bad Request
코드: VALIDATION_ERROR
메시지: "Invalid base64 image data"
```

**해결방안**:
```python
import base64
from PIL import Image
import io

def validate_base64_image(data):
    try:
        if 'data:' in data and ';base64,' in data:
            header, data = data.split(';base64,')
        decoded = base64.b64decode(data)
        Image.open(io.BytesIO(decoded))
        return decoded
    except Exception as e:
        raise ValidationError(f"이미지 처리 실패: {str(e)}")
```

#### 파일 크기 초과
```
에러: 413 Request Entity Too Large
```

**해결방안**:
```python
# settings.py
DATA_UPLOAD_MAX_MEMORY_SIZE = 10 * 1024 * 1024  # 10MB

# serializers.py
def validate_image(self, value):
    if value.size > 5 * 1024 * 1024:  # 5MB
        raise ValidationError("이미지 크기는 5MB 이하여야 합니다")
    return value
```

---

### 3.5 Kafka 관련 에러

#### Kafka 연결 실패
```
에러: KafkaException - "Unable to connect to broker"
```

**현재 구현 (Fallback)**:
```python
# logs/views.py
try:
    kafka_producer.send('activity-logs', log_data)
except Exception as e:
    # Kafka 실패 시 DB에 직접 저장
    ActivityLog.objects.create(**log_data)
    logger.warning(f"Kafka 전송 실패, DB 저장: {e}")
```

---

### 3.6 Rate Limiting 에러

#### 요청 한도 초과
```
에러: 429 Too Many Requests
코드: RATE_LIMIT_EXCEEDED
메시지: "요청 한도를 초과했습니다. 60초 후에 다시 시도해주세요."
```

**현재 설정**:
- 익명 사용자: 100 요청/시간
- 인증 사용자: 1000 요청/시간
- 버스트: 60 요청/분

**클라이언트 대응**:
```kotlin
// Retry-After 헤더 확인
val retryAfter = response.headers["Retry-After"]?.toIntOrNull() ?: 60
delay(retryAfter * 1000L)
// 재시도
```

---

### 3.7 N+1 쿼리 문제

**증상**: API 응답 지연, 데이터베이스 과부하

**해결방안**:
```python
# views.py에서 select_related/prefetch_related 사용
class LectureListCreateView(generics.ListCreateAPIView):
    def get_queryset(self):
        return Lecture.objects.select_related('instructor').prefetch_related(
            'tasks__subtasks',
            'sessions__participants'
        )
```

---

## 4. 해결 방안 요약

### 클라이언트 (Android) 권장 사항

1. **토큰 갱신 자동화**
   - 401 에러 시 자동으로 refresh 토큰으로 재발급
   - 재발급 실패 시 로그인 화면으로 이동

2. **Rate Limiting 대응**
   - 429 에러 시 `Retry-After` 헤더 확인
   - 지수 백오프 재시도 구현

3. **WebSocket 재연결**
   - 연결 끊김 시 자동 재연결
   - 토큰 만료 시 갱신 후 재연결

4. **오프라인 대응**
   - 로컬 캐싱
   - 동기화 큐 구현

### 서버 권장 사항

1. **트랜잭션 관리**
   - 동시성 문제 발생 가능 API에 `select_for_update()` 사용
   - `@transaction.atomic` 데코레이터 활용

2. **쿼리 최적화**
   - `select_related`, `prefetch_related` 적극 활용
   - 필요한 필드만 조회 (`only()`, `defer()`)

3. **로깅 강화**
   - 모든 에러 로깅
   - Sentry 연동으로 실시간 모니터링

---

## 5. 커스텀 예외 클래스

`core/utils/exception_handler.py`에 정의된 커스텀 예외:

```python
from core.utils.exception_handler import (
    CustomAPIException,
    SessionNotFoundError,
    SessionNotStartedError,
    SessionAlreadyEndedError,
    InstructorOnlyError,
    DeviceIdRequiredError,
    AlreadyEnrolledError,
    NotEnrolledError,
)

# 사용 예시
def start_session(request, session_id):
    session = get_object_or_404(LectureSession, id=session_id)

    if session.lecture.instructor != request.user:
        raise InstructorOnlyError()

    if session.status == 'ENDED':
        raise SessionAlreadyEndedError()

    # ...
```

### 새 커스텀 예외 정의하기

```python
from core.utils.exception_handler import CustomAPIException
from rest_framework import status

class MyCustomError(CustomAPIException):
    status_code = status.HTTP_400_BAD_REQUEST
    default_code = 'MY_ERROR_CODE'
    default_detail = '에러 메시지'
```

---

## 부록: HTTP 상태 코드 참조

| 코드 | 의미 | 사용 상황 |
|------|------|----------|
| 200 | OK | 조회/수정 성공 |
| 201 | Created | 생성 성공 |
| 202 | Accepted | 비동기 처리 수락 |
| 204 | No Content | 삭제 성공 |
| 400 | Bad Request | 잘못된 요청 |
| 401 | Unauthorized | 인증 실패 |
| 403 | Forbidden | 권한 없음 |
| 404 | Not Found | 리소스 없음 |
| 405 | Method Not Allowed | 허용되지 않은 메서드 |
| 409 | Conflict | 충돌 (중복 등) |
| 422 | Unprocessable Entity | 유효성 검증 실패 |
| 429 | Too Many Requests | Rate Limit 초과 |
| 500 | Internal Server Error | 서버 오류 |
