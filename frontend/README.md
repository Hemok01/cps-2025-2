# MobEdu 강의자 대시보드

MobEdu 시니어 교육 서비스의 강의자 대시보드 프론트엔드입니다.

## 기술 스택

- **Vite** - 빠른 개발 서버 및 빌드
- **React 18** - UI 라이브러리
- **TypeScript** - 타입 안정성
- **Radix UI** - 접근성 높은 UI 컴포넌트
- **Tailwind CSS** - 유틸리티 기반 CSS
- **React Router** - 클라이언트 사이드 라우팅

## 시작하기

### 의존성 설치

```bash
npm install
```

### 환경 변수 설정

`.env` 파일이 이미 생성되어 있습니다:

```env
VITE_API_BASE_URL=http://localhost:8000/api
VITE_WS_BASE_URL=ws://localhost:8000/ws
```

배포 환경에서는 실제 API URL로 변경하세요.

### 개발 서버 실행

```bash
npm run dev
```

http://localhost:5173 에서 접속 가능합니다.

### 프로덕션 빌드

```bash
npm run build
```

빌드된 파일은 `dist/` 폴더에 생성됩니다.

## 프로젝트 구조

```
src/
├── components/         # 재사용 가능한 컴포넌트
│   ├── ui/            # Radix UI 기반 기본 컴포넌트
│   ├── layout.tsx     # 레이아웃 컴포넌트
│   ├── protected-route.tsx  # 인증 라우트 보호
│   └── live-session/  # 실시간 세션 관련 컴포넌트
├── lib/               # 유틸리티 및 서비스
│   ├── api-service.ts        # API 호출 (현재 Mock)
│   ├── auth-context.tsx      # 인증 컨텍스트
│   ├── lecture-service.ts    # 강의 관련 API
│   ├── live-session-service.ts  # 세션 관련 API
│   ├── types.ts              # 공통 타입 정의
│   ├── lecture-types.ts      # 강의 타입
│   └── live-session-types.ts # 세션 타입
├── pages/             # 페이지 컴포넌트
│   ├── login-page.tsx           # 로그인
│   ├── dashboard-page.tsx       # 대시보드
│   ├── lectures-page.tsx        # 강의 목록
│   ├── lecture-form-page.tsx    # 강의 생성/수정
│   ├── session-control-page.tsx # 세션 제어
│   └── live-session-page.tsx    # 실시간 세션 모니터링
├── styles/            # 전역 스타일
├── App.tsx            # 앱 진입점
└── main.tsx           # React 진입점
```

## 주요 기능

### 1. 인증 (Authentication)
- JWT 기반 로그인
- 보호된 라우트 (ProtectedRoute)
- 자동 토큰 갱신

### 2. 강의 관리
- 강의 목록 조회
- 강의 생성/수정/삭제
- 강의별 통계

### 3. 세션 관리
- 세션 생성
- 세션 시작/일시정지/종료
- 실시간 참여자 모니터링

### 4. 실시간 모니터링
- 학습자 진행 상황 추적
- 도움 요청 관리
- WebSocket 기반 실시간 업데이트

### 5. 통계 및 분석
- 강의별 통계
- 어려운 단계 분석
- 학습자 진행 현황

## API 연동

현재 `src/lib/api-service.ts`는 Mock 데이터를 사용합니다.
실제 백엔드 API와 연동하려면:

1. `src/lib/api-service.ts` 파일을 수정하여 실제 API 호출로 변경
2. 환경 변수(`VITE_API_BASE_URL`)를 사용하여 API 엔드포인트 설정
3. JWT 토큰을 `Authorization` 헤더에 포함

예시:
```typescript
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

async function getLectures(): Promise<Lecture[]> {
  const token = localStorage.getItem('access_token');
  const response = await fetch(`${API_BASE_URL}/lectures/`, {
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
  });

  if (!response.ok) {
    throw new Error('Failed to fetch lectures');
  }

  return response.json();
}
```

## 스타일 커스터마이징

Tailwind CSS를 사용하므로 `tailwind.config.js`에서 테마를 커스터마이징할 수 있습니다.

## 컴포넌트 라이브러리

Radix UI 컴포넌트들은 `src/components/ui/` 폴더에 있습니다.
새로운 컴포넌트 추가 시 shadcn/ui 스타일을 따릅니다.

## 개발 가이드

### 새로운 페이지 추가

1. `src/pages/` 폴더에 새 페이지 컴포넌트 생성
2. `src/App.tsx`에서 라우트 추가

### 새로운 API 서비스 추가

1. `src/lib/` 폴더에 서비스 파일 생성
2. 타입 정의 추가 (`src/lib/types.ts` 또는 별도 파일)
3. API 호출 함수 작성

### WebSocket 연결

WebSocket은 `src/lib/live-session-service.ts`에서 관리됩니다.

## 빌드 및 배포

### Vite 빌드
```bash
npm run build
```

### Docker로 배포
프로젝트 루트의 `docker-compose.yml` 참조

### Nginx 설정 (프로덕션)

```nginx
server {
    listen 80;
    server_name your-domain.com;

    location / {
        root /usr/share/nginx/html;
        try_files $uri $uri/ /index.html;
    }

    location /api {
        proxy_pass http://backend:8000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location /ws {
        proxy_pass http://backend:8000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

## 트러블슈팅

### 개발 서버가 시작되지 않음
- `node_modules` 삭제 후 `npm install` 재실행
- Node.js 버전 확인 (20+)

### API 연결 오류
- 백엔드 서버가 실행 중인지 확인
- CORS 설정 확인
- `.env` 파일의 API URL 확인

### 빌드 오류
- TypeScript 타입 오류 확인
- `npm run build` 전 `npm run dev`로 개발 모드에서 테스트

## 원본 프로젝트

이 대시보드는 Figma 디자인을 기반으로 제작되었습니다:
https://www.figma.com/design/ud1AXuUAbmbrlbDTLjUXLj/Web-Dashboard-Flowchart

## 라이선스

교육용 프로젝트

---

**최종 업데이트**: 2025-12-18
