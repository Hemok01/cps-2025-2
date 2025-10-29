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

### 2. Docker Compose로 실행

```bash
# 모든 서비스 시작
docker-compose up -d

# 로그 확인
docker-compose logs -f backend

# 서비스 중지
docker-compose down
```

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
- `POST /api/lectures/{id}/sessions/` - 강의방 생성
- `GET /api/sessions/{code}/` - 세션 조회 (QR 코드)
- `POST /api/sessions/{id}/join/` - 세션 입장
- `POST /api/sessions/{id}/start/` - 강의 시작
- `POST /api/sessions/{id}/next-step/` - 다음 단계 진행

### WebSocket
- `ws://localhost:8000/ws/session/{session_id}/student/` - 학생용 실시간 연결
- `ws://localhost:8000/ws/session/{session_id}/instructor/` - 강사용 실시간 연결

## 기술 스택

- **Django 5.0** - 웹 프레임워크
- **Django REST Framework** - REST API
- **Django Channels** - WebSocket 지원
- **PostgreSQL** - 메인 데이터베이스
- **Redis** - 캐싱 및 메시지 브로커
- **Kafka** - 이벤트 스트리밍
- **Celery** - 비동기 작업
- **OpenAI API** - M-GPT 분석

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

## 라이선스

Private Project
