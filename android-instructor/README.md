# MobEdu - 강의자용 Android 앱

[![Android](https://img.shields.io/badge/Android-26+-green.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-purple.svg)](https://kotlinlang.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**MobEdu 강의자 앱**은 강의자가 스마트폰에서 앱 사용법을 녹화하고, AI가 분석하여 학습 과제(Task/Subtask)로 변환하는 시스템입니다.

## 📱 주요 기능

- **AccessibilityService 기반 녹화**: 화면 녹화 권한 없이 UI 이벤트 캡처
- **실시간 이벤트 전송**: Django 백엔드로 UI 이벤트 배치 전송
- **AI 분석 (GPT-4o-mini)**: 녹화된 세션을 분석하여 단계별 가이드 자동 생성
- **플로팅 오버레이 UI**: 녹화 시작/종료 컨트롤
- **녹화 관리**: 녹화 목록 조회, 분석 상태 확인
- **단계 편집**: 생성된 Subtask의 제목, 설명, 가이드 텍스트 수정

## 🏗️ 프로젝트 구조

```
android-instructor/
├── app/src/main/java/com/example/mobilegpt/
│   ├── MainActivity.kt              # 앱 진입점
│   ├── MyAccessibilityService.kt    # UI 이벤트 캡처 서비스
│   ├── overlay/                     # 플로팅 오버레이 UI
│   ├── recording/                   # 녹화 화면
│   │   ├── RecordingScreen.kt       # 녹화 시작 화면
│   │   └── RecordingListScreen.kt   # 녹화 목록 화면
│   ├── subtask/                     # 단계 관련 화면
│   │   ├── SubtaskListScreen.kt     # 단계 목록
│   │   └── SubtaskDetailScreen.kt   # 단계 수정
│   ├── ui/auth/                     # 로그인 화면
│   ├── viewmodel/                   # ViewModels
│   └── data/
│       ├── remote/api/              # API 서비스
│       └── remote/dto/              # Request/Response DTO
├── build.gradle.kts
└── settings.gradle.kts
```

## 🚀 Getting Started

### Prerequisites

- **Android Studio**: Arctic Fox or later
- **Android Device/Emulator**: API Level 26 (Android 8.0) or higher
- **Python**: 3.8 or higher
- **OpenAI API Key**: Get one from [OpenAI Platform](https://platform.openai.com/api-keys)

### Installation

#### 1. Clone the Repository

```bash
git clone https://github.com/YOUR_USERNAME/mobilegpt.git
cd mobilegpt
```

#### 2. Set Up Android App

1. **Copy and configure `local.properties`:**
   ```bash
   cp local.properties.example local.properties
   ```

2. **Edit `local.properties`:**
   ```properties
   # Update with your Android SDK path
   sdk.dir=/Users/YOUR_USERNAME/Library/Android/sdk

   # Server configuration
   server.url=http://YOUR_IP:5001
   server.host=YOUR_IP
   ```

   > **Note**: For testing on real devices, use your computer's local IP address instead of `localhost`.

3. **Open the project in Android Studio**

4. **Sync Gradle** and resolve dependencies

#### 3. Set Up Flask Server

1. **Navigate to the server directory:**
   ```bash
   cd app/mobilegpt-server
   ```

2. **Create a virtual environment:**
   ```bash
   python3 -m venv venv
   source venv/bin/activate  # On Windows: venv\Scripts\activate
   ```

3. **Install dependencies:**
   ```bash
   pip install -r requirements.txt
   ```

4. **Configure environment variables:**
   ```bash
   cp .env.example .env
   ```

   Edit `.env` and add your OpenAI API key:
   ```
   OPENAI_API_KEY=your_actual_api_key_here
   ```

5. **Start the server:**
   ```bash
   python server.py
   ```

   You should see:
   ```
   🚀 MobileGPT Server Starting...
   📍 Server running at: http://0.0.0.0:5001
   ```

## 📖 Usage

### Setting Up Accessibility Service

1. Launch the MobileGPT app
2. Tap "Enable Accessibility Service"
3. Navigate to Settings → Accessibility → MobileGPT
4. Enable the service

### Recording a Session

1. Tap "Start Recording" on the main screen
2. A floating button will appear on your screen
3. Navigate through your app and perform actions
4. Tap the floating button and select "Stop Recording"
5. The session will be saved automatically

### Viewing Sessions

1. Tap "View Sessions" on the main screen
2. Select a session from the list
3. View the generated steps or edit them

### Editing Steps

1. Tap on any step in the step list
2. Modify the title, description, or text
3. Tap "Save" to update the step

## 🔑 API Endpoints

The Flask server provides the following endpoints:

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/record_event` | POST | Receive accessibility events |
| `/api/save_session` | POST | Save current recording session |
| `/api/analyze_session` | POST | Analyze session with GPT and generate steps |
| `/api/list_sessions` | GET | Get list of all sessions |
| `/api/get_steps/<session_id>` | GET | Get steps for a specific session |
| `/api/update_step` | POST | Update a specific step |
| `/api/delete_step` | POST | Delete a step and reorder |

## ⚙️ Configuration

### Android App

Configuration is managed through `local.properties`:

- `server.url`: Full URL of the Flask server
- `server.host`: Hostname or IP address (for network security config)

### Flask Server

Configuration is managed through `.env`:

- `OPENAI_API_KEY`: Your OpenAI API key for GPT analysis

## 🛡️ Security

- **Never commit** `.env` or `local.properties` files
- API keys are stored locally and never pushed to version control
- Use HTTPS in production (currently HTTP for development)
- Session data is stored locally on the server

## 🧪 Testing

### Running Android Tests

```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest
```

### Testing the Server

```bash
cd app/mobilegpt-server
python -m pytest  # If you add tests
```

## 🤝 Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🐛 Known Issues

- Accessibility service may need to be re-enabled after app updates
- Some UI elements may not be captured correctly on certain devices
- Server must be running before starting a recording session

## 🔮 Roadmap

- [ ] HTTPS support for production
- [ ] Cloud deployment guide
- [ ] Multi-language support
- [ ] Export curricula to PDF/HTML
- [ ] Video recording alongside accessibility events
- [ ] Custom GPT prompts for different learning styles

## 📧 Support

If you encounter any issues or have questions:

1. Check the [Issues](https://github.com/YOUR_USERNAME/mobilegpt/issues) page
2. Create a new issue with detailed information
3. Join our discussions

## 🙏 Acknowledgments

- Built with [Jetpack Compose](https://developer.android.com/jetpack/compose)
- Powered by [OpenAI GPT-4](https://openai.com/)
- Backend with [Flask](https://flask.palletsprojects.com/)

---

**최종 업데이트**: 2025-12-18

**프로젝트**: [MobEdu](../README.md) | CPS 2025-2
