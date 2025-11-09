# MobEdu 배포 준비 진행 상황

**마지막 업데이트**: 2025-11-10
**진행률**: 40% (4/10 단계)

---

## ✅ 완료된 작업

### 1. Docker Compose 통합
**파일**: `/Users/heemok/cps 2025-2/backend/docker-compose.yml`

**전체 서비스 구성 (9개)**:
1. PostgreSQL (5432)
2. Redis (6379)
3. Zookeeper (2181)
4. Kafka (9092)
5. Django Backend/Gunicorn (8000)
6. Daphne ASGI (8001)
7. Celery Worker
8. Celery Beat
9. Kafka Consumer

---

### 2. Kafka Consumer 구현
**파일**: `/Users/heemok/cps 2025-2/backend/apps/logs/management/commands/run_kafka_consumer.py`

기능:
- ✅ Activity Log 실시간 수신 및 저장
- ✅ ActivityLog 모델에 저장
- ✅ 로그 카운팅 및 모니터링

**실행 방법**:
```bash
python manage.py run_kafka_consumer
```

**참고**: AI 분석 기능은 현재 비활성화됨. ActivityLog만 저장합니다.

---

### 3. PostgreSQL 설정
**파일**: `/Users/heemok/cps 2025-2/backend/config/settings.py`

변경사항:
- ✅ SQLite → PostgreSQL 전환 (line 77-96)
- ✅ 환경변수 기반 DB 설정
- ✅ Docker 환경에 맞게 HOST='db' 설정

---

### 4. 환경변수 설정
**파일**: `/Users/heemok/cps 2025-2/backend/.env`

업데이트:
- ✅ DB_HOST=db (Docker용)
- ✅ REDIS_HOST=redis
- ✅ REDIS_URL=redis://redis:6379/0
- ✅ KAFKA_BOOTSTRAP_SERVERS=kafka:9092

---

### 5. 패키지 추가
**파일**: `/Users/heemok/cps 2025-2/backend/requirements.txt`

추가:
- ✅ kafka-python==2.0.2

---

## ⏳ 남은 작업 (6단계)

### 6. AI 분석 서비스 재설계 (선택사항, 1-2시간)
- [ ] MobileGPT 원본 구조 분석 완료
- [ ] AI 분석 기능 재설계 필요 여부 결정
- [ ] 또는 HelpRequest 수동 생성 방식 유지

### 7. Nginx 설정 및 프론트엔드 빌드 (30분)
- [ ] 프론트엔드 프로덕션 빌드
- [ ] Nginx Dockerfile 작성
- [ ] nginx.conf 설정 (정적 파일 + API 프록시 + WebSocket)
- [ ] docker-compose.yml에 Nginx 추가

### 8. 백엔드 테스트 작성 (1시간)
- [ ] `tests/test_auth.py` - 인증 API
- [ ] `tests/test_sessions.py` - 세션 관리
- [ ] `tests/test_help.py` - 도움 요청
- [ ] `tests/test_kafka_consumer.py` - Kafka Consumer
- [ ] pytest 실행 및 커버리지 확인

### 9. Docker Compose 테스트 (30분)
- [ ] 로컬에서 `docker-compose up --build` 실행
- [ ] 모든 서비스 헬스체크 확인
- [ ] DB 마이그레이션 실행
- [ ] 슈퍼유저 생성
- [ ] API 엔드포인트 테스트

### 10. AWS EC2 배포 (1.5시간)
- [ ] EC2 인스턴스 생성 (t3.small/medium)
- [ ] 보안 그룹 설정 (SSH, HTTP, HTTPS)
- [ ] Docker 설치
- [ ] 코드 배포
- [ ] .env.production 설정
- [ ] docker-compose up -d
- [ ] 도메인 연결 (선택)
- [ ] HTTPS 설정 (선택)

### 11. 배포 문서 작성 (30분)
- [ ] DEPLOYMENT.md - 배포 가이드
- [ ] USER_GUIDE.md - 사용자 매뉴얼
- [ ] API 문서 (Swagger/Postman)
- [ ] 발표 준비 체크리스트

---

## 🚀 빠른 재개 가이드

다음에 작업을 재개할 때:

### 1. 현재 상태 확인
```bash
cd /Users/heemok/cps\ 2025-2/backend
git status
```

### 2. 로컬 테스트 (선택)
```bash
# Docker 없이 로컬 테스트
python manage.py runserver

# 또는 Docker Compose로 전체 시스템 테스트
docker-compose up --build
```

### 3. 다음 작업 시작
- 6단계: Nginx 설정부터 시작
- 또는 8단계: 먼저 로컬에서 테스트

---

## 📝 주요 파일 및 위치

```
/Users/heemok/cps 2025-2/
├── backend/
│   ├── docker-compose.yml          ✅ 수정됨 (9개 서비스)
│   ├── config/
│   │   ├── settings.py             ✅ PostgreSQL 설정, OPENAI 제거
│   │   └── asgi.py                 (기존)
│   ├── apps/logs/management/commands/
│   │   └── run_kafka_consumer.py   ✅ 단순화 (ActivityLog만 저장)
│   ├── requirements.txt            ✅ kafka-python 추가
│   └── .env                        ✅ Docker 환경 설정
├── frontend-teacher/               (수정 안함)
├── android-student/                (수정 안함)
└── MobileGPT-main/                 ✅ 원본 코드 분석 완료
```

---

## 🎯 핵심 아키텍처

```
[Android 학생 앱]
     ↓ Activity Logs via API
[Django Backend (Gunicorn)]
     ↓ Kafka Producer
[Kafka Topic: activity-logs]
     ↓
[Kafka Consumer]
     ↓ Save to DB
[ActivityLog 모델]
     ↓
[강사 대시보드 (React)]
     ↑↓ WebSocket (Daphne)
[실시간 업데이트]

참고: AI 분석 기능은 현재 비활성화
HelpRequest는 수동 생성 방식
```

---

## 💡 다음 작업 시 참고사항

1. **테스트 우선 추천**: Docker Compose로 로컬 테스트 먼저 수행
2. **AI 분석 기능**: 현재 비활성화됨. 필요 시 재설계 필요
3. **포트 충돌**: 로컬에서 PostgreSQL(5432), Redis(6379) 등이 이미 실행 중이면 충돌 가능
4. **Kafka 초기화**: Kafka는 첫 실행 시 토픽 자동 생성되므로 시간 소요
5. **MobileGPT 원본**: `/MobileGPT-main/` 폴더에 원본 코드 있음 (참고용)

---

## 🔗 유용한 명령어

### Docker Compose 관리
```bash
# 전체 빌드 및 실행
docker-compose up --build -d

# 로그 확인
docker-compose logs -f [service-name]

# 특정 서비스만 재시작
docker-compose restart [service-name]

# 모두 중지 및 삭제
docker-compose down -v

# 서비스 상태 확인
docker-compose ps
```

### Django 관리
```bash
# 마이그레이션
docker-compose exec backend python manage.py migrate

# 슈퍼유저 생성
docker-compose exec backend python manage.py createsuperuser

# Kafka Consumer 실행
docker-compose exec kafka_consumer python manage.py run_kafka_consumer
```

### 테스트
```bash
# 백엔드 테스트
docker-compose exec backend pytest --cov=apps

# Kafka Consumer 로그 확인
docker-compose logs -f kafka_consumer
```

---

## 예상 일정

- **이미 완료**: 1.5시간 (40%)
- **남은 작업**: 4-5시간 (60%)
- **총 예상**: 5.5-6.5시간
- **버퍼**: +1-2시간 (문제 해결)

**발표 전까지 여유 있게 진행 권장!**

---

## 연락처 / 도움말

- Django Channels: https://channels.readthedocs.io/
- Docker Compose: https://docs.docker.com/compose/
- Apache Kafka: https://kafka.apache.org/documentation/
- MobileGPT 원본 (참고용): https://github.com/mobilegptsys/MobileGPT

---

**작성자**: Claude Code
**최종 업데이트**: 2025-11-10
**프로젝트**: MobEdu (학교 과제용)
**변경사항**: MobileGPT AI 분석 기능 제거, 기본 인프라만 유지
