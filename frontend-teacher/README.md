# MobEdu 선생님 대시보드

MobEdu 시니어 교육 서비스의 선생님용 웹 대시보드입니다.

## 기술 스택

- **Frontend**: React 18 + TypeScript + Vite
- **UI Library**: Material-UI (MUI)
- **HTTP Client**: Axios
- **Routing**: React Router v6
- **Charts**: Recharts
- **WebSocket**: Native WebSocket API

## 주요 기능

### 1. 대시보드 (홈)
- 내 강의 목록 조회
- 대기 중인 도움 요청 수 확인
- 빠른 작업 바로가기

### 2. 세션 제어
- 강의 선택 및 새 세션 생성
- 6자리 세션 코드 자동 생성
- 세션 시작/일시정지/재개/종료
- 다음 단계(Subtask)로 이동
- 참가 학생 실시간 확인

### 3. 학생 모니터링
- 강의별 학생 진행 상황 실시간 모니터링
- WebSocket을 통한 실시간 업데이트
- 학생별 진행률, 현재 단계, 상태 확인
- 도움 요청 횟수 확인

### 4. 도움 요청 관리
- 대기 중인 도움 요청 목록
- 수동/자동 요청 구분
- M-GPT 분석 결과 확인
- 도움 요청 해결 처리

### 5. 통계 및 분석
- 강의별 통계 조회
- 총 학생 수, 도움 요청 수
- 어려운 단계 분석 (차트)
- 상위 10개 난이도 높은 Subtask

## 설치 및 실행

### 사전 요구사항
- Node.js 18 이상
- npm 또는 yarn
- Django 백엔드 서버 실행 중

### 1. 의존성 설치
```bash
npm install
```

### 2. 환경 변수 설정
`.env` 파일을 생성하고 다음 내용을 입력하세요:

```env
VITE_API_BASE_URL=http://localhost:8000
VITE_WS_BASE_URL=ws://localhost:8000
```

### 3. 개발 서버 실행
```bash
npm run dev
```

브라우저에서 `http://localhost:5173`으로 접속하세요.

### 4. 프로덕션 빌드
```bash
npm run build
npm run preview
```

## 로그인

- **이메일**: 선생님 계정 이메일
- **비밀번호**: 선생님 계정 비밀번호
- **권한**: INSTRUCTOR 권한이 있는 계정만 로그인 가능

## 프로젝트 구조

```
frontend-teacher/
├── src/
│   ├── components/       # 재사용 가능한 컴포넌트
│   │   └── common/       # 공통 컴포넌트 (Layout, ProtectedRoute)
│   ├── contexts/         # React Context (Auth)
│   ├── hooks/            # Custom hooks
│   ├── pages/            # 페이지 컴포넌트
│   │   ├── LoginPage.tsx
│   │   ├── DashboardPage.tsx
│   │   ├── SessionControlPage.tsx
│   │   ├── MonitoringPage.tsx
│   │   ├── HelpRequestsPage.tsx
│   │   └── StatisticsPage.tsx
│   ├── services/         # API 및 WebSocket 서비스
│   │   ├── api.ts
│   │   ├── authService.ts
│   │   ├── sessionService.ts
│   │   ├── dashboardService.ts
│   │   ├── helpService.ts
│   │   └── websocketService.ts
│   ├── types/            # TypeScript 타입 정의
│   │   └── index.ts
│   ├── utils/            # 유틸리티 함수
│   ├── App.tsx           # 메인 App 컴포넌트
│   └── main.tsx          # 진입점
├── .env                  # 환경 변수
├── package.json
├── tsconfig.json
└── vite.config.ts
```

## API 엔드포인트

### 인증
- `POST /api/auth/login/` - 로그인
- `GET /api/auth/me/` - 현재 사용자 정보

### 강의
- `GET /api/lectures/` - 강의 목록

### 세션
- `POST /api/sessions/lectures/{lecture_id}/create/` - 세션 생성
- `POST /api/sessions/{id}/start/` - 세션 시작
- `POST /api/sessions/{id}/next-step/` - 다음 단계
- `POST /api/sessions/{id}/pause/` - 세션 일시정지
- `POST /api/sessions/{id}/resume/` - 세션 재개
- `POST /api/sessions/{id}/end/` - 세션 종료
- `GET /api/sessions/{id}/participants/` - 참가자 목록

### 대시보드
- `GET /api/dashboard/lectures/{lecture_id}/students/` - 학생 진행 상황
- `GET /api/dashboard/statistics/lecture/{lecture_id}/` - 강의 통계

### 도움 요청
- `GET /api/dashboard/help-requests/pending/` - 대기 중인 도움 요청
- `POST /api/help/requests/{id}/resolve/` - 도움 요청 해결

## WebSocket

### Session WebSocket
- URL: `ws://localhost:8000/ws/sessions/{session_code}/`
- 사용: 세션 제어 명령 전송

### Dashboard WebSocket
- URL: `ws://localhost:8000/ws/dashboard/lectures/{lecture_id}/`
- 사용: 학생 진행 상황 실시간 수신

## 개발 팁

### 디버깅
브라우저 개발자 도구의 Console과 Network 탭을 활용하여 API 호출과 WebSocket 메시지를 확인하세요.

### Hot Reload
Vite는 파일 변경 시 자동으로 페이지를 새로고침합니다.

### CORS 문제
백엔드 서버의 CORS 설정에 `http://localhost:5173`이 포함되어 있는지 확인하세요.

## 라이선스

MIT License
