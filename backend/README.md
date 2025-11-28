# MobileGPT Backend

Django REST Framework 기반 백엔드 서버입니다.

## 프로젝트 구조

```
backend/
├── config/              # Django 설정
│   ├── settings.py      # 메인 설정 파일
│   ├── urls.py          # URL 라우팅
│   ├── wsgi.py          # WSGI 설정
│   └── asgi.py          # ASGI 설정 (WebSocket)
├── apps/                # Django 앱들
│   ├── accounts/        # 사용자 관리
│   ├── lectures/        # 강의 관리
│   ├── sessions/        # 실시간 강의방
│   ├── tasks/           # 과제/단계 관리
│   ├── progress/        # 진행 상태
│   ├── logs/            # 행동 로그
│   ├── help/            # 도움 요청
│   └── dashboard/       # 강사용 모니터링
├── core/                # 공통 유틸리티
│   ├── kafka/           # Kafka 관련
│   ├── redis/           # Redis 관련
│   └── utils/           # 기타 유틸리티
├── manage.py
├── requirements.txt
├── Dockerfile
└── docker-compose.yml
```

## 시작하기

### 1. 환경 변수 설정

```bash
cp .env.example .env
```

`.env` 파일을 편집하여 필요한 환경 변수를 설정하세요.

#### 주요 환경 변수
```bash
# Django 설정
SECRET_KEY=your-secret-key
DEBUG=True
ALLOWED_HOSTS=localhost,127.0.0.1

# 데이터베이스
DATABASE_URL=postgres://user:password@localhost:5432/mobilegpt

# Redis
REDIS_URL=redis://localhost:6379

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# JWT 토큰
JWT_ACCESS_TOKEN_LIFETIME=60  # 분 단위
JWT_REFRESH_TOKEN_LIFETIME=1440  # 분 단위

# OpenAI API
OPENAI_API_KEY=your-openai-api-key

# Channels (WebSocket)
# Docker 환경에서 반드시 True로 설정해야 함 (프로세스 간 통신에 필요)
USE_REDIS_CHANNELS=True
```

### 2. Docker Compose로 실행

```bash
# 모든 서비스 시작
docker-compose up -d

# 서비스 상태 확인
docker-compose ps

# 개별 서비스 로그 확인
docker-compose logs -f backend        # Django 백엔드
docker-compose logs -f kafka          # Kafka 브로커
docker-compose logs -f kafka-consumer # Kafka Consumer
docker-compose logs -f daphne         # WebSocket 서버

# 서비스 중지
docker-compose down

# 데이터 포함 완전 삭제
docker-compose down -v
```

#### Docker 서비스 구성
- **PostgreSQL** (포트 5432) - 메인 데이터베이스
- **Redis** (포트 6379) - 캐시 및 채널 레이어
- **Zookeeper** (포트 2181) - Kafka 코디네이터
- **Kafka** (포트 9092, 9093) - 이벤트 스트리밍
- **Backend** (포트 8000) - Django REST API
- **Daphne** (포트 8001) - WebSocket 서버
- **Celery Worker** - 비동기 작업 처리
- **Celery Beat** - 스케줄 작업
- **Kafka Consumer** - 활동 로그 처리

### 3. 로컬 개발 환경 (Docker 없이)

```bash
# 가상환경 생성 및 활성화
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# 의존성 설치
pip install -r requirements.txt

# 마이그레이션
python manage.py migrate

# 슈퍼유저 생성
python manage.py createsuperuser

# 개발 서버 실행
python manage.py runserver
```

## 주요 기능

### 인증 API
- `POST /api/auth/register/` - 회원가입
- `POST /api/auth/login/` - 로그인
- `POST /api/auth/refresh/` - 토큰 갱신
- `GET /api/auth/me/` - 현재 사용자 정보

### 강의 API
- `GET /api/lectures/` - 강의 목록
- `POST /api/lectures/` - 강의 생성 (강사)
- `GET /api/lectures/{id}/` - 강의 상세
- `POST /api/lectures/{id}/enroll/` - 수강 신청

### 세션 API (실시간 강의방)
- `POST /api/lectures/{id}/sessions/create/` - 강의방 생성
- `GET /api/sessions/{code}/` - 세션 조회 (QR 코드)
- `POST /api/sessions/{id}/join/` - 세션 입장
- `POST /api/sessions/{id}/start/` - 강의 시작
- `POST /api/sessions/{id}/next-step/` - 다음 단계 진행
- `POST /api/sessions/{id}/pause/` - 세션 일시정지
- `POST /api/sessions/{id}/resume/` - 세션 재개
- `POST /api/sessions/{id}/end/` - 세션 종료

### 녹화 API (Recording Sessions)
- `GET /api/sessions/recordings/` - 녹화 목록 조회
- `POST /api/sessions/recordings/` - 새 녹화 시작
- `GET /api/sessions/recordings/{id}/` - 녹화 상세 정보
- `POST /api/sessions/recordings/{id}/stop/` - 녹화 중지
- `GET /api/sessions/recordings/{id}/events/` - 녹화된 이벤트 조회
- `POST /api/sessions/recordings/{id}/save-events-batch/` - 이벤트 배치 저장 (Android 앱용)

### 헬스체크 API
- `GET /api/health/` - 기본 헬스체크
- `GET /api/health/detailed/` - 상세 서비스 상태 (DB, Redis, Kafka)

### Android App Links
- `GET /.well-known/assetlinks.json` - Android 앱 인증 파일

### WebSocket 엔드포인트

> **중요**: WebSocket은 **Daphne 서버 (포트 8001)**에서 처리됩니다.
> - REST API: `http://localhost:8000/api/...`
> - WebSocket: `ws://localhost:8001/ws/...`
> - Android 에뮬레이터: `ws://10.0.2.2:8001/ws/...`

- `/ws/sessions/{session_code}/` - 세션별 실시간 통신
  - 세션 참가 (join)
  - 세션 상태 변경 알림 (session_status_changed)
  - 단계 변경 알림 (step_changed)
  - 하트비트 (heartbeat/heartbeat_ack)
  - 도움 요청 (request_help/help_requested)
  - 진행 상태 업데이트 (progress_updated)

- `/ws/dashboard/` - 대시보드 실시간 업데이트
  - 도움 요청 알림
  - 학생 진행 상태 업데이트
  - 세션 상태 변경 알림

## 기술 스택

- **Django 5.0** - 웹 프레임워크
- **Django REST Framework** - REST API
- **Django Channels** - WebSocket 지원
- **PostgreSQL** - 메인 데이터베이스
- **Redis** - 캐싱 및 메시지 브로커
- **Kafka** - 이벤트 스트리밍
- **Celery** - 비동기 작업
- **OpenAI API** - M-GPT 분석

## Kafka 통합

### 개요
Kafka를 사용하여 활동 로그(Activity Logs)를 비동기적으로 처리합니다. 이를 통해 대량의 로그를 효율적으로 처리하고 시스템 성능을 향상시킵니다.

### 아키텍처
```
Django App → Kafka Producer → Kafka Broker → Kafka Consumer → PostgreSQL
                    ↓                                  ↓
              (Fallback: Direct DB)             (처리 실패시 재시도)
```

### Kafka Consumer 실행
```bash
# Docker 환경
docker-compose up -d kafka-consumer

# 로컬 환경
python manage.py run_kafka_consumer
```

### 모니터링
```bash
# Kafka 토픽 목록 확인
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092

# Consumer 로그 확인
docker-compose logs -f kafka-consumer

# 토픽 메시지 확인
docker exec -it kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic activity_logs --from-beginning
```

### Fallback 메커니즘
Kafka가 사용 불가능한 경우, 시스템은 자동으로 직접 데이터베이스 쓰기로 전환됩니다. 이를 통해 서비스 중단 없이 로그를 계속 저장할 수 있습니다.

## 녹화 기능 (Recording Sessions)

### 개요
Android 앱의 사용자 인터랙션을 녹화하고 재생할 수 있는 기능입니다. 학생들의 학습 과정을 분석하고 피드백을 제공하는데 사용됩니다.

### 녹화 시작
```python
POST /api/sessions/recordings/
{
    "title": "안드로이드 실습 녹화",
    "description": "ListView 구현 과정"
}
```

### 이벤트 배치 저장
```python
POST /api/sessions/recordings/{id}/save-events-batch/
{
    "events": [
        {
            "event_type": "touch",
            "timestamp": "2024-01-15T10:30:00Z",
            "data": {
                "x": 540,
                "y": 960,
                "element": "Button#submit"
            }
        }
    ]
}
```

### 녹화 중지
```python
POST /api/sessions/recordings/{id}/stop/
```

## Management Commands

### Kafka Consumer 실행
```bash
python manage.py run_kafka_consumer
```
활동 로그를 처리하는 Kafka Consumer를 실행합니다.

### 샘플 데이터 생성
```bash
python manage.py create_sample_data
```
테스트용 샘플 강의, 사용자, 세션 데이터를 생성합니다.

## 개발 가이드

### 마이그레이션

```bash
# 마이그레이션 파일 생성
python manage.py makemigrations

# 마이그레이션 실행
python manage.py migrate
```

### 테스트

```bash
# 전체 테스트 실행
pytest

# 커버리지와 함께 실행
pytest --cov=apps
```

### 코드 품질

```bash
# 코드 포맷팅
black .
isort .

# 린팅
flake8
```

## 트러블슈팅

### Kafka 연결 문제
```bash
# Kafka 서비스 재시작
docker-compose restart kafka

# Kafka 토픽 초기화
docker exec -it kafka kafka-topics --delete --topic activity_logs --bootstrap-server localhost:9092
docker exec -it kafka kafka-topics --create --topic activity_logs --bootstrap-server localhost:9092

# Zookeeper 상태 확인
docker-compose logs zookeeper
```

### Redis 연결 문제
```bash
# Redis 서비스 재시작
docker-compose restart redis

# Redis CLI 접속 테스트
docker exec -it redis redis-cli ping
```

### 마이그레이션 오류
```bash
# 데이터베이스 초기화 (주의: 모든 데이터 삭제)
docker-compose down -v
docker-compose up -d postgres
python manage.py migrate
```

### WebSocket 연결 실패
```bash
# Daphne 서버 확인
docker-compose logs daphne

# Channel Layer 테스트
python manage.py shell
>>> from channels.layers import get_channel_layer
>>> channel_layer = get_channel_layer()
>>> async_to_sync(channel_layer.send)('test', {'type': 'test.message'})
```

#### WebSocket 체크리스트
1. **포트 확인**: WebSocket은 8001 포트 (Daphne), REST API는 8000 포트 (Gunicorn)
2. **환경변수 확인**: `.env` 파일에 `USE_REDIS_CHANNELS=True` 설정 필요
3. **Docker 재빌드**: Consumer 코드 변경 후 반드시 `docker-compose down && docker-compose up -d --build`
4. **Android 에뮬레이터**: localhost 대신 `10.0.2.2` 사용 (예: `ws://10.0.2.2:8001/ws/sessions/CODE/`)

#### WebSocket 메시지 테스트 (Python)
```python
import asyncio
import websockets
import json

async def test_websocket():
    uri = "ws://localhost:8001/ws/sessions/SESSION_CODE/"
    async with websockets.connect(uri) as ws:
        # Join 메시지 전송
        await ws.send(json.dumps({
            "type": "join",
            "data": {"device_id": "test", "name": "tester"}
        }))
        # 응답 수신
        response = await ws.recv()
        print(f"Received: {response}")

asyncio.run(test_websocket())
```

### 서비스 포트 충돌
```bash
# 사용 중인 포트 확인
lsof -i :8000  # Django
lsof -i :5432  # PostgreSQL
lsof -i :6379  # Redis
lsof -i :9092  # Kafka

# 프로세스 종료 (Mac/Linux)
kill -9 $(lsof -ti:8000)
```

## 의존성 패키지

### 주요 패키지
```
Django==5.0
djangorestframework==3.14.0
django-cors-headers==4.3.1
djangorestframework-simplejwt==5.3.1
channels==4.0.0
channels-redis==4.2.0
daphne==4.0.0
psycopg2-binary==2.9.9
redis==5.0.1
celery==5.3.4
confluent-kafka==2.3.0
kafka-python==2.0.2
Pillow==10.1.0
python-dotenv==1.0.0
```

전체 의존성은 `requirements.txt` 파일을 참조하세요.

## 라이선스

Private Project
