# 테스트 보고서 디렉토리

이 디렉토리에는 프론트엔드-백엔드 통합 테스트 결과와 관련 스크립트가 포함되어 있습니다.

## 📁 파일 목록

### 테스트 보고서

| 파일 | 설명 |
|------|------|
| `INTEGRATION_TEST_REPORT.md` | 프론트엔드-백엔드 API 통합 테스트 보고서 |
| `SESSION_CONTROL_TEST_REPORT.md` | 세션 제어 기능 테스트 보고서 |
| `RECORDING_TO_TASK_INTEGRATION_ANALYSIS.md` | 녹화→과제 변환 통합 분석 |
| `DOCKER_COMPOSE_TEST_REPORT.md` | Docker Compose 환경 테스트 보고서 |

### 테스트 스크립트

| 파일 | 설명 |
|------|------|
| `test-api.sh` | 백엔드 API 자동 테스트 |
| `test-session-control.sh` | 세션 제어 API 테스트 |
| `create-sample-lectures.sh` | 샘플 강의 데이터 생성 |
| `create_sample_lectures.py` | 샘플 데이터 생성 (Python) |

### WebSocket 테스트

| 파일 | 설명 |
|------|------|
| `test_websocket_full.py` | WebSocket 전체 테스트 (Python) |
| `test_ws_final.py` | WebSocket 최종 테스트 |
| `test_broadcast.py` | 브로드캐스트 테스트 |
| `test_browser_websocket.html` | 브라우저 WebSocket 테스트 |
| `test_ws_simple.html` | 간단한 WebSocket 테스트 |

## 🚀 빠른 시작

### 1. 백엔드 서버 실행
```bash
cd ../backend
docker-compose up -d
```

### 2. 프론트엔드 서버 실행
```bash
cd ../frontend
npm run dev
```

### 3. API 테스트 실행
```bash
cd test-reports
./test-api.sh
```

### 4. 브라우저에서 프론트엔드 접속
```
URL: http://localhost:3001/
로그인: instructor@test.com / test1234
```

## 📊 테스트 결과 요약

### ✅ 성공한 테스트
- JWT 인증 및 토큰 발급
- 사용자 정보 조회
- 강의 목록 조회
- 강의 생성/수정/삭제
- 세션 생성
- 세션 코드 생성

### ⚠️ 주의사항
- 일부 기능은 백엔드 API가 미구현되어 목 데이터 사용
- WebSocket 실시간 업데이트는 백엔드 Consumer 구현 필요
- Task/Subtask 관련 기능은 추가 개발 필요

## 🔧 문제 해결

### 백엔드가 실행되지 않을 때
```bash
cd ../backend
docker-compose down
docker-compose up -d
docker-compose logs -f backend
```

### 프론트엔드가 실행되지 않을 때
```bash
cd ../frontend
npm install
npm run dev
```

### 데이터베이스 초기화가 필요할 때
```bash
cd ../backend
docker-compose down -v  # 볼륨까지 삭제
docker-compose up -d
docker-compose exec backend python manage.py migrate
```

## 📝 테스트 계정

**강사 계정**:
- 이메일: `instructor@test.com`
- 비밀번호: `test1234`

**학생 계정**:
- 이메일: `student@test.com`
- 비밀번호: `test1234`

## 🔗 관련 문서

- [통합 테스트 보고서](./INTEGRATION_TEST_REPORT.md)
- [백엔드 README](../backend/README.md)
- [프론트엔드 README](../frontend/README.md)

## 📅 마지막 업데이트

2025-12-18
