# MobileGPT 설치 가이드 (한국어)

다른 개발자를 위한 상세한 설치 및 설정 가이드입니다.

## 목차
- [사전 요구사항](#사전-요구사항)
- [프로젝트 클론](#프로젝트-클론)
- [Android 앱 설정](#android-앱-설정)
- [Flask 서버 설정](#flask-서버-설정)
- [실행 및 테스트](#실행-및-테스트)
- [문제 해결](#문제-해결)

## 사전 요구사항

### 필수 소프트웨어

1. **Android Studio** (Arctic Fox 이상)
   - [다운로드](https://developer.android.com/studio)
   - Android SDK가 자동으로 설치됩니다

2. **Python 3.8+**
   ```bash
   # 버전 확인
   python3 --version
   ```

3. **Git**
   ```bash
   # 버전 확인
   git --version
   ```

4. **OpenAI API Key**
   - [OpenAI Platform](https://platform.openai.com/api-keys)에서 발급
   - GPT-4 API 사용을 위한 크레딧 필요

### 하드웨어 요구사항

- **Android 기기 또는 에뮬레이터**: API Level 26 (Android 8.0) 이상
- **개발 컴퓨터**:
  - RAM 8GB 이상 권장
  - 저장공간 10GB 이상

## 프로젝트 클론

```bash
# 프로젝트 클론
git clone https://github.com/YOUR_USERNAME/mobilegpt.git
cd mobilegpt

# 디렉터리 구조 확인
ls -la
```

## Android 앱 설정

### 1. local.properties 파일 생성

```bash
# 예제 파일을 복사
cp local.properties.example local.properties
```

### 2. local.properties 편집

**macOS/Linux의 경우:**
```properties
# Android SDK 경로 (기본값)
sdk.dir=/Users/YOUR_USERNAME/Library/Android/sdk

# 서버 설정 (개발 환경)
server.url=http://localhost:5001
server.host=localhost
```

**Windows의 경우:**
```properties
# Android SDK 경로
sdk.dir=C\:\\Users\\YOUR_USERNAME\\AppData\\Local\\Android\\sdk

# 서버 설정 (개발 환경)
server.url=http://localhost:5001
server.host=localhost
```

### 3. 실제 디바이스 테스트 시 설정

실제 Android 기기에서 테스트하려면 컴퓨터의 로컬 IP 주소를 사용해야 합니다.

**로컬 IP 확인 방법:**

**macOS/Linux:**
```bash
ifconfig | grep "inet " | grep -v 127.0.0.1
```

**Windows:**
```cmd
ipconfig
```

**local.properties 업데이트:**
```properties
# 예: 컴퓨터 IP가 192.168.1.100인 경우
server.url=http://192.168.1.100:5001
server.host=192.168.1.100
```

### 4. Android Studio에서 프로젝트 열기

1. Android Studio 실행
2. `Open an Existing Project` 선택
3. 클론한 `mobilegpt` 폴더 선택
4. Gradle 동기화 대기 (처음에는 시간이 걸릴 수 있음)

### 5. 의존성 확인

Gradle 동기화가 완료되면 모든 의존성이 자동으로 다운로드됩니다:
- Jetpack Compose
- Retrofit (네트워크 통신)
- OkHttp (HTTP 클라이언트)
- Navigation Component

## Flask 서버 설정

### 1. 서버 디렉터리로 이동

```bash
cd app/mobilegpt-server
```

### 2. Python 가상환경 생성

**macOS/Linux:**
```bash
python3 -m venv venv
source venv/bin/activate
```

**Windows:**
```cmd
python -m venv venv
venv\Scripts\activate
```

가상환경이 활성화되면 프롬프트 앞에 `(venv)`가 표시됩니다.

### 3. 의존성 설치

```bash
pip install -r requirements.txt
```

설치되는 패키지:
- `flask` - 웹 서버
- `openai>=1.0.0` - OpenAI API 클라이언트
- `python-dotenv` - 환경변수 관리

### 4. 환경변수 설정

```bash
# 예제 파일 복사
cp .env.example .env
```

`.env` 파일을 편집하고 OpenAI API 키를 입력:
```env
OPENAI_API_KEY=sk-proj-your_actual_api_key_here
```

⚠️ **중요**: `.env` 파일은 절대 Git에 커밋하지 마세요!

### 5. 디렉터리 구조 확인

서버를 처음 실행하면 다음 디렉터리가 자동으로 생성됩니다:
- `sessions/` - 녹화된 세션 데이터
- `curriculum/` - 생성된 학습 커리큘럼

## 실행 및 테스트

### 1. Flask 서버 시작

```bash
# app/mobilegpt-server 디렉터리에서
python server.py
```

다음과 같은 메시지가 표시되어야 합니다:
```
🚀 MobileGPT Server Starting...
📍 Server running at: http://0.0.0.0:5001
💾 Sessions saved to: /path/to/sessions
📚 Curriculum saved to: /path/to/curriculum
```

### 2. 서버 테스트

새 터미널을 열고:
```bash
# 서버가 응답하는지 확인
curl http://localhost:5001/api/list_sessions

# 예상 응답:
# {"sessions":[]}
```

### 3. Android 앱 실행

1. Android Studio에서 실행 버튼 클릭 (▶️)
2. 에뮬레이터 또는 실제 기기 선택
3. 앱이 설치되고 실행됩니다

### 4. 접근성 서비스 활성화

1. 앱에서 "Enable Accessibility Service" 버튼 클릭
2. 설정 화면으로 이동
3. `Settings > Accessibility > MobileGPT` 활성화
4. 권한 허용

### 5. 첫 녹화 테스트

1. 앱에서 "Start Recording" 버튼 클릭
2. 플로팅 버튼이 화면에 나타남
3. 다른 앱을 사용하면서 상호작용
4. 플로팅 버튼 클릭 → "Stop Recording"
5. "View Sessions"로 결과 확인

## 문제 해결

### 1. Gradle 동기화 실패

**문제**: `SDK location not found`

**해결**:
```bash
# local.properties 파일 확인
cat local.properties

# SDK 경로가 정확한지 확인
ls /Users/YOUR_USERNAME/Library/Android/sdk  # macOS
dir C:\Users\YOUR_USERNAME\AppData\Local\Android\sdk  # Windows
```

### 2. 서버 연결 실패

**문제**: 앱이 서버에 연결할 수 없음

**해결**:
1. 서버가 실행 중인지 확인
2. local.properties의 server.url이 올바른지 확인
3. 실제 기기의 경우 같은 Wi-Fi 네트워크에 연결되어 있는지 확인
4. 방화벽 설정 확인

**테스트**:
```bash
# 에뮬레이터에서
adb shell
curl http://10.0.2.2:5001/api/list_sessions

# 실제 기기에서 (컴퓨터 IP: 192.168.1.100)
curl http://192.168.1.100:5001/api/list_sessions
```

### 3. OpenAI API 오류

**문제**: `Authentication failed`

**해결**:
1. `.env` 파일에 API 키가 올바르게 설정되었는지 확인
2. API 키에 크레딧이 있는지 확인
3. 서버를 재시작

### 4. Python 모듈 없음

**문제**: `ModuleNotFoundError: No module named 'flask'`

**해결**:
```bash
# 가상환경이 활성화되었는지 확인
which python  # 가상환경 경로가 나와야 함

# 의존성 재설치
pip install -r requirements.txt
```

### 5. 포트 이미 사용 중

**문제**: `Address already in use: 5001`

**해결**:
```bash
# macOS/Linux
lsof -i :5001
kill -9 <PID>

# Windows
netstat -ano | findstr :5001
taskkill /PID <PID> /F
```

### 6. 접근성 서비스가 작동하지 않음

**해결**:
1. 설정에서 접근성 서비스를 끄고 다시 켜기
2. 앱 재설치
3. 기기 재부팅

## 개발 팁

### 로그 확인

**Android 로그:**
```bash
# Logcat 실시간 확인
adb logcat | grep MobileGPT
```

**서버 로그:**
서버는 debug 모드로 실행되므로 모든 요청이 터미널에 표시됩니다.

### 데이터 초기화

```bash
# 세션 데이터 삭제
rm app/mobilegpt-server/sessions/*.json

# 커리큘럼 데이터 삭제
rm -rf app/mobilegpt-server/curriculum/session_*
```

### 코드 수정 시

**Android 앱**:
- Gradle을 다시 빌드하고 앱 재실행

**Flask 서버**:
- Debug 모드에서는 자동으로 재시작됨 (대부분의 경우)
- 환경변수 변경 시에는 수동으로 재시작 필요

## 다음 단계

설정이 완료되었다면:
1. [README.md](README.md)에서 전체 기능 확인
2. [CONTRIBUTING.md](CONTRIBUTING.md)에서 기여 방법 확인
3. 코드 탐색 및 개선 아이디어 생각하기

## 도움이 필요하신가요?

- GitHub Issues에 질문 올리기
- README.md의 문서 확인
- 서버 로그와 Android Logcat 확인

즐거운 개발 되세요! 🚀
