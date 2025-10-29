# Android 앱 아키텍처 설계 (학생용)

## 개요

**중요**: 이 Android 앱은 **학생(시니어 학습자) 전용**입니다. 강사용 Dashboard는 별도의 웹 서비스로 제공됩니다 ([06_web_dashboard_architecture.md](./06_web_dashboard_architecture.md) 참조).

**실시간 강의방 시스템** ⭐ NEW: 학생들은 QR 코드로 강의방에 입장하여 강사와 동기식으로 학습합니다.

Android 앱은 다음 기술 스택과 패턴을 사용합니다:

- **언어**: Kotlin
- **아키텍처**: Clean Architecture + MVVM
- **DI**: Hilt (Dagger)
- **네트워크**: Retrofit + OkHttp
- **비동기**: Coroutines + Flow
- **로컬 DB**: Room
- **메시징**: Kafka Producer (librdkafka)
- **실시간 통신**: WebSocket (OkHttp WebSocket) ⭐ NEW
- **QR 스캔**: ZXing (Zebra Crossing) ⭐ NEW
- **핵심 기능**: AccessibilityService, Overlay (WindowManager)

---

## 전체 레이어 구조

```
┌─────────────────────────────────────┐
│  Presentation Layer (UI)            │
│  - Activities, Fragments            │
│  - ViewModels                       │
│  - Overlay UI                       │
└─────────────────────────────────────┘
           ↓ ↑
┌─────────────────────────────────────┐
│  Domain Layer (Business Logic)      │
│  - Use Cases                        │
│  - Domain Models                    │
│  - Repository Interfaces            │
└─────────────────────────────────────┘
           ↓ ↑
┌─────────────────────────────────────┐
│  Data Layer                         │
│  - Repository Implementations       │
│  - Data Sources (Remote, Local)     │
│  - DTOs / Entities                  │
└─────────────────────────────────────┘
           ↓ ↑
┌─────────────────────────────────────┐
│  Services Layer                     │
│  - AccessibilityService             │
│  - Overlay Service                  │
│  - Kafka Producer Service           │
└─────────────────────────────────────┘
```

---

## 패키지 구조

```
com.senior.edu.mobilegpt/
├── app/
│   └── SeniorEduApplication.kt        # Application 클래스
│
├── di/                                 # Dependency Injection
│   ├── AppModule.kt
│   ├── NetworkModule.kt
│   ├── RepositoryModule.kt
│   └── ServiceModule.kt
│
├── data/                               # Data Layer
│   ├── remote/                         # API 통신
│   │   ├── api/
│   │   │   ├── AuthApi.kt
│   │   │   ├── LectureApi.kt
│   │   │   ├── ProgressApi.kt
│   │   │   ├── LogApi.kt
│   │   │   └── HelpApi.kt
│   │   └── dto/
│   │       ├── LoginRequest.kt
│   │       ├── UserResponse.kt
│   │       ├── LectureResponse.kt
│   │       └── ...
│   │
│   ├── local/                          # 로컬 DB (Room)
│   │   ├── database/
│   │   │   └── AppDatabase.kt
│   │   ├── dao/
│   │   │   ├── UserDao.kt
│   │   │   ├── LectureDao.kt
│   │   │   └── LogDao.kt
│   │   └── entity/
│   │       ├── UserEntity.kt
│   │       ├── LectureEntity.kt
│   │       └── LogEntity.kt
│   │
│   ├── repository/                     # Repository 구현
│   │   ├── AuthRepositoryImpl.kt
│   │   ├── LectureRepositoryImpl.kt
│   │   ├── ProgressRepositoryImpl.kt
│   │   ├── LogRepositoryImpl.kt
│   │   └── HelpRepositoryImpl.kt
│   │
│   └── kafka/                          # Kafka Producer
│       ├── KafkaProducer.kt
│       └── KafkaMessage.kt
│
├── domain/                             # Domain Layer
│   ├── model/                          # Domain Models
│   │   ├── User.kt
│   │   ├── Lecture.kt
│   │   ├── Task.kt
│   │   ├── Subtask.kt
│   │   ├── ActivityLog.kt
│   │   ├── HelpRequest.kt
│   │   └── UserProgress.kt
│   │
│   ├── repository/                     # Repository Interfaces
│   │   ├── AuthRepository.kt
│   │   ├── LectureRepository.kt
│   │   ├── ProgressRepository.kt
│   │   ├── LogRepository.kt
│   │   └── HelpRepository.kt
│   │
│   └── usecase/                        # Use Cases
│       ├── auth/
│       │   ├── LoginUseCase.kt
│       │   ├── RegisterUseCase.kt
│       │   └── GetCurrentUserUseCase.kt
│       ├── lecture/
│       │   ├── GetLecturesUseCase.kt
│       │   ├── EnrollLectureUseCase.kt
│       │   └── GetLectureDetailsUseCase.kt
│       ├── progress/
│       │   ├── GetCurrentSubtaskUseCase.kt
│       │   ├── UpdateProgressUseCase.kt
│       │   └── CompleteSubtaskUseCase.kt
│       ├── log/
│       │   ├── SendActivityLogUseCase.kt
│       │   └── SendBatchLogsUseCase.kt
│       └── help/
│           ├── RequestHelpUseCase.kt
│           ├── GetHelpResponseUseCase.kt
│           └── SubmitFeedbackUseCase.kt
│
├── presentation/                       # Presentation Layer
│   ├── ui/
│   │   ├── auth/                       # 인증 화면
│   │   │   ├── LoginActivity.kt
│   │   │   ├── LoginViewModel.kt
│   │   │   └── RegisterActivity.kt
│   │   │
│   │   ├── main/                       # 메인 화면
│   │   │   ├── MainActivity.kt
│   │   │   └── MainViewModel.kt
│   │   │
│   │   ├── lecture/                    # 강의 목록/상세
│   │   │   ├── LectureListFragment.kt
│   │   │   ├── LectureListViewModel.kt
│   │   │   ├── LectureDetailActivity.kt
│   │   │   └── LectureDetailViewModel.kt
│   │   │
│   │   ├── learning/                   # 학습 진행 화면
│   │   │   ├── LearningActivity.kt
│   │   │   ├── LearningViewModel.kt
│   │   │   └── SubtaskGuideFragment.kt
│   │   │
│   │   └── settings/
│   │       ├── SettingsActivity.kt
│   │       └── SettingsViewModel.kt
│   │
│   │   # NOTE: 강사용 Dashboard는 별도 웹 서비스로 제공 (06_web_dashboard_architecture.md 참조)
│   │
│   ├── overlay/                        # Overlay UI
│   │   ├── OverlayView.kt
│   │   ├── OverlayManager.kt
│   │   └── OverlayViewModel.kt
│   │
│   └── common/
│       ├── BaseActivity.kt
│       ├── BaseFragment.kt
│       └── BaseViewModel.kt
│
├── service/                            # Android Services
│   ├── accessibility/
│   │   ├── SeniorEduAccessibilityService.kt
│   │   ├── EventCollector.kt
│   │   ├── AccessibilityNodeInfoHelper.kt  # NEW: UI 요소 탐색 헬퍼
│   │   └── SensitiveDataFilter.kt          # NEW: 민감 정보 필터링
│   │
│   ├── overlay/
│   │   ├── OverlayService.kt
│   │   └── OverlayWindowManager.kt
│   │
│   └── sync/
│       ├── LogSyncService.kt           # 로그 동기화
│       └── ProgressSyncService.kt      # 진행 상태 동기화
│
├── util/                               # Utility Classes
│   ├── NetworkUtil.kt
│   ├── PreferenceUtil.kt
│   ├── PermissionUtil.kt
│   ├── TTSHelper.kt                    # Text-to-Speech
│   └── Logger.kt
│
└── common/
    ├── Constants.kt
    ├── Result.kt                       # Sealed class for API results
    └── Event.kt                        # Single Event wrapper
```

---

## 주요 컴포넌트 상세 설계

### 1. AccessibilityService

**역할**: UI 이벤트를 감지하고 로그로 수집

#### 1.1 XML 설정 파일

**res/xml/accessibility_service_config.xml**:
```xml
<?xml version="1.0" encoding="utf-8"?>
<accessibility-service
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeViewClicked|typeViewFocused|typeViewScrolled|typeViewTextChanged|typeWindowStateChanged|typeViewLongClicked|typeViewSelected"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagIncludeNotImportantViews|flagReportViewIds|flagRetrieveInteractiveWindows"
    android:canRetrieveWindowContent="true"
    android:description="@string/accessibility_service_description"
    android:notificationTimeout="100"
    android:settingsActivity="com.senior.edu.mobilegpt.presentation.ui.settings.SettingsActivity" />
```

**설명**:
- `accessibilityEventTypes`: 감지할 이벤트 타입 (클릭, 포커스, 스크롤, 텍스트 변경 등)
- `accessibilityFlags`:
  - `flagIncludeNotImportantViews`: 접근성 중요하지 않은 뷰도 포함
  - `flagReportViewIds`: 뷰 ID 리소스 이름 보고
  - `flagRetrieveInteractiveWindows`: 상호작용 가능한 윈도우 정보 조회
- `canRetrieveWindowContent`: UI 계층 구조 접근 허용
- `notificationTimeout`: 이벤트 알림 간격 (ms)

#### 1.2 AndroidManifest.xml

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- 접근성 서비스 권한 -->
    <uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE" />

    <application>
        <!-- AccessibilityService 선언 -->
        <service
            android:name=".service.accessibility.SeniorEduAccessibilityService"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
            android:exported="true">
            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService" />
            </intent-filter>
            <meta-data
                android:name="android.accessibilityservice"
                android:resource="@xml/accessibility_service_config" />
        </service>
    </application>
</manifest>
```

#### 1.3 서비스 구현

**SeniorEduAccessibilityService.kt**:
```kotlin
class SeniorEduAccessibilityService : AccessibilityService() {

    @Inject lateinit var eventCollector: EventCollector
    @Inject lateinit var logRepository: LogRepository
    @Inject lateinit var nodeInfoHelper: AccessibilityNodeInfoHelper
    @Inject lateinit var sensitiveDataFilter: SensitiveDataFilter

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onServiceConnected() {
        super.onServiceConnected()

        // 런타임 설정 (XML과 함께 사용)
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED or
                        AccessibilityEvent.TYPE_VIEW_FOCUSED or
                        AccessibilityEvent.TYPE_VIEW_SCROLLED or
                        AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                        AccessibilityEvent.TYPE_VIEW_LONG_CLICKED or
                        AccessibilityEvent.TYPE_VIEW_SELECTED

            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC

            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS

            notificationTimeout = 100
        }

        serviceInfo = info
        Log.i(TAG, "AccessibilityService connected and configured")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        // 민감한 앱 필터링
        if (sensitiveDataFilter.isSensitivePackage(event.packageName?.toString())) {
            Log.d(TAG, "Skipping sensitive package: ${event.packageName}")
            return
        }

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED -> handleClick(event)
            AccessibilityEvent.TYPE_VIEW_LONG_CLICKED -> handleLongClick(event)
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> handleScroll(event)
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> handleTextInput(event)
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> handleScreenChange(event)
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> handleFocus(event)
            AccessibilityEvent.TYPE_VIEW_SELECTED -> handleSelection(event)
        }
    }

    private fun handleClick(event: AccessibilityEvent) {
        val source = event.source
        if (source == null) {
            Log.w(TAG, "Event source is null")
            return
        }

        try {
            val log = eventCollector.collectClickEvent(event, source, rootInActiveWindow)
            sendLog(log)
        } finally {
            source.recycle() // 메모리 누수 방지
        }
    }

    private fun handleLongClick(event: AccessibilityEvent) {
        val source = event.source ?: return
        try {
            val log = eventCollector.collectLongClickEvent(event, source, rootInActiveWindow)
            sendLog(log)
        } finally {
            source.recycle()
        }
    }

    private fun handleScroll(event: AccessibilityEvent) {
        val source = event.source ?: return
        try {
            val log = eventCollector.collectScrollEvent(event, source)
            sendLog(log)
        } finally {
            source.recycle()
        }
    }

    private fun handleTextInput(event: AccessibilityEvent) {
        val source = event.source ?: return

        // 비밀번호 필드 체크
        if (source.isPassword) {
            Log.d(TAG, "Skipping password field")
            source.recycle()
            return
        }

        try {
            val log = eventCollector.collectTextInputEvent(event, source)
            sendLog(log)
        } finally {
            source.recycle()
        }
    }

    private fun handleScreenChange(event: AccessibilityEvent) {
        val log = eventCollector.collectScreenChangeEvent(event, rootInActiveWindow)
        sendLog(log)
    }

    private fun handleFocus(event: AccessibilityEvent) {
        val source = event.source ?: return
        try {
            val log = eventCollector.collectFocusEvent(event, source)
            sendLog(log)
        } finally {
            source.recycle()
        }
    }

    private fun handleSelection(event: AccessibilityEvent) {
        val source = event.source ?: return
        try {
            val log = eventCollector.collectSelectionEvent(event, source)
            sendLog(log)
        } finally {
            source.recycle()
        }
    }

    private fun sendLog(log: ActivityLog) {
        scope.launch {
            try {
                logRepository.sendActivityLog(log)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send log", e)
            }
        }
    }

    override fun onInterrupt() {
        Log.i(TAG, "AccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        Log.i(TAG, "AccessibilityService destroyed")
    }

    companion object {
        private const val TAG = "SeniorEduA11yService"
    }
}
```

#### 1.4 EventCollector (개선)

**EventCollector.kt**:
```kotlin
class EventCollector @Inject constructor(
    private val nodeInfoHelper: AccessibilityNodeInfoHelper,
    private val sensitiveDataFilter: SensitiveDataFilter,
    private val context: Context
) {

    fun collectClickEvent(
        event: AccessibilityEvent,
        source: AccessibilityNodeInfo,
        rootNode: AccessibilityNodeInfo?
    ): ActivityLog {
        val nodeInfo = nodeInfoHelper.extractNodeInfo(source)
        val parentInfo = nodeInfoHelper.getParentNodeInfo(source)

        return ActivityLog(
            userId = getCurrentUserId(),
            subtaskId = getCurrentSubtaskId(),
            eventType = EventType.CLICK,
            eventData = EventData(
                elementText = source.text?.toString(),
                elementClass = source.className?.toString(),
                viewIdResourceName = source.viewIdResourceName,
                contentDescription = source.contentDescription?.toString(),
                isClickable = source.isClickable,
                isEnabled = source.isEnabled,
                bounds = nodeInfoHelper.getBounds(source)
            ),
            nodeInfo = nodeInfo,
            parentNodeInfo = parentInfo,
            screenInfo = ScreenInfo(
                packageName = event.packageName?.toString(),
                className = event.className?.toString(),
                windowTitle = getWindowTitle(rootNode)
            ),
            isSensitiveData = sensitiveDataFilter.isSensitiveNode(source),
            timestamp = System.currentTimeMillis()
        )
    }

    fun collectLongClickEvent(
        event: AccessibilityEvent,
        source: AccessibilityNodeInfo,
        rootNode: AccessibilityNodeInfo?
    ): ActivityLog {
        // Similar to clickEvent
        return collectClickEvent(event, source, rootNode).copy(
            eventType = EventType.LONG_CLICK
        )
    }

    fun collectScrollEvent(
        event: AccessibilityEvent,
        source: AccessibilityNodeInfo
    ): ActivityLog {
        return ActivityLog(
            userId = getCurrentUserId(),
            subtaskId = getCurrentSubtaskId(),
            eventType = EventType.SCROLL,
            eventData = EventData(
                scrollX = event.scrollX,
                scrollY = event.scrollY,
                maxScrollX = event.maxScrollX,
                maxScrollY = event.maxScrollY,
                elementClass = source.className?.toString()
            ),
            nodeInfo = nodeInfoHelper.extractNodeInfo(source),
            screenInfo = ScreenInfo(
                packageName = event.packageName?.toString()
            ),
            timestamp = System.currentTimeMillis()
        )
    }

    fun collectTextInputEvent(
        event: AccessibilityEvent,
        source: AccessibilityNodeInfo
    ): ActivityLog {
        val text = if (sensitiveDataFilter.isSensitiveNode(source)) {
            "[REDACTED]"
        } else {
            event.text?.toString() ?: ""
        }

        return ActivityLog(
            userId = getCurrentUserId(),
            subtaskId = getCurrentSubtaskId(),
            eventType = EventType.TEXT_INPUT,
            eventData = EventData(
                inputText = text,
                elementClass = source.className?.toString(),
                viewIdResourceName = source.viewIdResourceName,
                isPassword = source.isPassword
            ),
            nodeInfo = nodeInfoHelper.extractNodeInfo(source),
            isSensitiveData = source.isPassword,
            screenInfo = ScreenInfo(
                packageName = event.packageName?.toString()
            ),
            timestamp = System.currentTimeMillis()
        )
    }

    fun collectScreenChangeEvent(
        event: AccessibilityEvent,
        rootNode: AccessibilityNodeInfo?
    ): ActivityLog {
        return ActivityLog(
            userId = getCurrentUserId(),
            subtaskId = getCurrentSubtaskId(),
            eventType = EventType.SCREEN_CHANGE,
            eventData = EventData(
                elementClass = event.className?.toString()
            ),
            screenInfo = ScreenInfo(
                packageName = event.packageName?.toString(),
                className = event.className?.toString(),
                windowTitle = getWindowTitle(rootNode)
            ),
            timestamp = System.currentTimeMillis()
        )
    }

    fun collectFocusEvent(
        event: AccessibilityEvent,
        source: AccessibilityNodeInfo
    ): ActivityLog {
        return ActivityLog(
            userId = getCurrentUserId(),
            subtaskId = getCurrentSubtaskId(),
            eventType = EventType.FOCUS,
            eventData = EventData(
                elementText = source.text?.toString(),
                elementClass = source.className?.toString(),
                viewIdResourceName = source.viewIdResourceName,
                isFocused = source.isFocused
            ),
            nodeInfo = nodeInfoHelper.extractNodeInfo(source),
            screenInfo = ScreenInfo(
                packageName = event.packageName?.toString()
            ),
            timestamp = System.currentTimeMillis()
        )
    }

    fun collectSelectionEvent(
        event: AccessibilityEvent,
        source: AccessibilityNodeInfo
    ): ActivityLog {
        return ActivityLog(
            userId = getCurrentUserId(),
            subtaskId = getCurrentSubtaskId(),
            eventType = EventType.SELECTION,
            eventData = EventData(
                elementText = source.text?.toString(),
                elementClass = source.className?.toString(),
                viewIdResourceName = source.viewIdResourceName,
                selectedIndex = event.currentItemIndex
            ),
            nodeInfo = nodeInfoHelper.extractNodeInfo(source),
            screenInfo = ScreenInfo(
                packageName = event.packageName?.toString()
            ),
            timestamp = System.currentTimeMillis()
        )
    }

    private fun getCurrentUserId(): Long {
        // SharedPreferences에서 가져오기
        return context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            .getLong("user_id", -1)
    }

    private fun getCurrentSubtaskId(): Long? {
        return context.getSharedPreferences("learning_prefs", Context.MODE_PRIVATE)
            .getLong("current_subtask_id", -1)
            .takeIf { it != -1L }
    }

    private fun getWindowTitle(rootNode: AccessibilityNodeInfo?): String? {
        return rootNode?.let {
            try {
                it.findAccessibilityNodeInfosByText("title")?.firstOrNull()?.text?.toString()
            } catch (e: Exception) {
                null
            } finally {
                // rootNode는 외부에서 관리되므로 recycle하지 않음
            }
        }
    }
}
```

#### 1.5 AccessibilityNodeInfoHelper

**AccessibilityNodeInfoHelper.kt** (새 파일):
```kotlin
class AccessibilityNodeInfoHelper @Inject constructor() {

    fun extractNodeInfo(node: AccessibilityNodeInfo): NodeInfo {
        return NodeInfo(
            viewIdResourceName = node.viewIdResourceName,
            className = node.className?.toString(),
            text = node.text?.toString(),
            contentDescription = node.contentDescription?.toString(),
            isClickable = node.isClickable,
            isEnabled = node.isEnabled,
            isFocused = node.isFocused,
            isSelected = node.isSelected,
            isScrollable = node.isScrollable,
            isPassword = node.isPassword,
            bounds = getBounds(node),
            childCount = node.childCount
        )
    }

    fun getParentNodeInfo(node: AccessibilityNodeInfo): NodeInfo? {
        val parent = node.parent ?: return null
        return try {
            extractNodeInfo(parent)
        } finally {
            parent.recycle()
        }
    }

    fun getBounds(node: AccessibilityNodeInfo): Bounds {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        return Bounds(
            left = rect.left,
            top = rect.top,
            right = rect.right,
            bottom = rect.bottom
        )
    }

    /**
     * 특정 텍스트를 가진 노드 찾기
     */
    fun findNodesByText(
        rootNode: AccessibilityNodeInfo?,
        text: String
    ): List<AccessibilityNodeInfo> {
        return rootNode?.findAccessibilityNodeInfosByText(text) ?: emptyList()
    }

    /**
     * 특정 viewId를 가진 노드 찾기
     */
    fun findNodesByViewId(
        rootNode: AccessibilityNodeInfo?,
        viewId: String
    ): List<AccessibilityNodeInfo> {
        return rootNode?.findAccessibilityNodeInfosByViewId(viewId) ?: emptyList()
    }

    /**
     * 현재 포커스된 노드 찾기
     */
    fun findFocusedNode(rootNode: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        return rootNode?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
    }

    /**
     * UI 계층 구조 탐색
     */
    fun traverseNodeTree(
        node: AccessibilityNodeInfo?,
        callback: (AccessibilityNodeInfo) -> Unit
    ) {
        node ?: return

        callback(node)

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                traverseNodeTree(child, callback)
                child.recycle()
            }
        }
    }
}

data class NodeInfo(
    val viewIdResourceName: String?,
    val className: String?,
    val text: String?,
    val contentDescription: String?,
    val isClickable: Boolean,
    val isEnabled: Boolean,
    val isFocused: Boolean,
    val isSelected: Boolean,
    val isScrollable: Boolean,
    val isPassword: Boolean,
    val bounds: Bounds,
    val childCount: Int
)

data class Bounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)
```

#### 1.6 SensitiveDataFilter

**SensitiveDataFilter.kt** (새 파일):
```kotlin
class SensitiveDataFilter @Inject constructor() {

    private val sensitivePackages = setOf(
        "com.kbstar.kbbank",      // KB국민은행
        "com.shinhan.sbanking",   // 신한은행
        "com.wooribank.smart",    // 우리은행
        "com.kftc.bankpay",       // 뱅크페이
        "com.samsung.android.samsungpass",  // 삼성패스
        // 추가 민감 앱들...
    )

    private val sensitiveViewIds = setOf(
        "password",
        "pwd",
        "pin",
        "security",
        "account",
        "card_number",
        "cvv"
    )

    fun isSensitivePackage(packageName: String?): Boolean {
        packageName ?: return false
        return sensitivePackages.contains(packageName)
    }

    fun isSensitiveNode(node: AccessibilityNodeInfo): Boolean {
        // 비밀번호 필드
        if (node.isPassword) return true

        // ViewId 체크
        node.viewIdResourceName?.let { viewId ->
            if (sensitiveViewIds.any { viewId.contains(it, ignoreCase = true) }) {
                return true
            }
        }

        // ContentDescription 체크
        node.contentDescription?.toString()?.let { desc ->
            if (sensitiveViewIds.any { desc.contains(it, ignoreCase = true) }) {
                return true
            }
        }

        return false
    }
}
```

---

### 2. Overlay UI

**역할**: 현재 학습 단계 안내 및 도움 표시

**OverlayManager.kt**:
```kotlin
class OverlayManager @Inject constructor(
    private val context: Context,
    private val windowManager: WindowManager
) {
    private var overlayView: View? = null

    fun showGuide(subtask: Subtask) {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        overlayView = LayoutInflater.from(context)
            .inflate(R.layout.overlay_guide, null).apply {
                findViewById<TextView>(R.id.guideText).text = subtask.guideText
                findViewById<Button>(R.id.helpButton).setOnClickListener {
                    requestHelp()
                }
            }

        windowManager.addView(overlayView, params)
    }

    fun hideGuide() {
        overlayView?.let {
            windowManager.removeView(it)
            overlayView = null
        }
    }

    fun showHelpResponse(helpContent: String) {
        overlayView?.findViewById<TextView>(R.id.helpText)?.apply {
            text = helpContent
            visibility = View.VISIBLE
        }

        // TTS로 음성 안내
        TTSHelper.speak(helpContent)
    }
}
```

**OverlayView.kt** (커스텀 View):
```kotlin
class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private lateinit var viewModel: OverlayViewModel

    init {
        inflate(context, R.layout.view_overlay, this)
        setupUI()
    }

    private fun setupUI() {
        findViewById<Button>(R.id.btnHelp).setOnClickListener {
            viewModel.requestHelp()
        }

        findViewById<Button>(R.id.btnNext).setOnClickListener {
            viewModel.moveToNextSubtask()
        }
    }

    fun updateGuide(guideText: String) {
        findViewById<TextView>(R.id.tvGuide).text = guideText
    }
}
```

---

### 3. Kafka Producer

**역할**: 로그를 Kafka Topic으로 전송

**KafkaProducer.kt**:
```kotlin
class KafkaProducer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val producer: Producer<String, String> by lazy {
        val props = Properties().apply {
            put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BOOTSTRAP_SERVERS)
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
        }
        KafkaProducer(props)
    }

    suspend fun sendLog(topic: String, log: ActivityLog) = withContext(Dispatchers.IO) {
        try {
            val record = ProducerRecord(
                topic,
                log.userId.toString(),
                Json.encodeToString(log)
            )
            producer.send(record).get()
        } catch (e: Exception) {
            Log.e("KafkaProducer", "Failed to send log", e)
            // 로컬 DB에 저장 (나중에 재전송)
            saveToLocalDB(log)
        }
    }

    private suspend fun saveToLocalDB(log: ActivityLog) {
        // Room DB에 저장
    }
}
```

---

### 4. ViewModel 예시

**LearningViewModel.kt**:
```kotlin
@HiltViewModel
class LearningViewModel @Inject constructor(
    private val getCurrentSubtaskUseCase: GetCurrentSubtaskUseCase,
    private val updateProgressUseCase: UpdateProgressUseCase,
    private val requestHelpUseCase: RequestHelpUseCase,
    private val getHelpResponseUseCase: GetHelpResponseUseCase
) : ViewModel() {

    private val _currentSubtask = MutableStateFlow<Subtask?>(null)
    val currentSubtask: StateFlow<Subtask?> = _currentSubtask.asStateFlow()

    private val _helpResponse = MutableSharedFlow<HelpResponse>()
    val helpResponse: SharedFlow<HelpResponse> = _helpResponse.asSharedFlow()

    fun loadCurrentSubtask(lectureId: Long) {
        viewModelScope.launch {
            getCurrentSubtaskUseCase(lectureId).collect { result ->
                when (result) {
                    is Result.Success -> _currentSubtask.value = result.data
                    is Result.Error -> handleError(result.error)
                    is Result.Loading -> showLoading()
                }
            }
        }
    }

    fun completeCurrentSubtask() {
        viewModelScope.launch {
            _currentSubtask.value?.let { subtask ->
                updateProgressUseCase(
                    subtaskId = subtask.id,
                    status = ProgressStatus.COMPLETED
                )
                loadCurrentSubtask(subtask.lectureId)
            }
        }
    }

    fun requestHelp() {
        viewModelScope.launch {
            _currentSubtask.value?.let { subtask ->
                val helpRequestId = requestHelpUseCase(
                    subtaskId = subtask.id,
                    requestType = HelpRequestType.MANUAL
                )

                // 폴링 또는 WebSocket으로 응답 대기
                pollHelpResponse(helpRequestId)
            }
        }
    }

    private suspend fun pollHelpResponse(helpRequestId: Long) {
        while (true) {
            delay(2000) // 2초마다 확인
            val response = getHelpResponseUseCase(helpRequestId)
            if (response.status == HelpStatus.RESOLVED) {
                _helpResponse.emit(response)
                break
            }
        }
    }
}
```

---

### 5. Repository 예시

**ProgressRepositoryImpl.kt**:
```kotlin
class ProgressRepositoryImpl @Inject constructor(
    private val progressApi: ProgressApi,
    private val progressDao: ProgressDao
) : ProgressRepository {

    override suspend fun getCurrentSubtask(lectureId: Long): Flow<Result<Subtask>> = flow {
        emit(Result.Loading)

        try {
            // 먼저 로컬 DB에서 조회
            val localProgress = progressDao.getCurrentProgress(lectureId)
            if (localProgress != null) {
                emit(Result.Success(localProgress.toSubtask()))
            }

            // 서버에서 최신 정보 가져오기
            val response = progressApi.getMyProgress(lectureId)
            if (response.isSuccessful) {
                val subtask = response.body()?.currentSubtask?.toDomain()
                subtask?.let {
                    progressDao.saveProgress(it.toEntity())
                    emit(Result.Success(it))
                }
            } else {
                emit(Result.Error("Failed to fetch progress"))
            }
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Unknown error"))
        }
    }

    override suspend fun updateProgress(subtaskId: Long, status: ProgressStatus) {
        try {
            progressApi.updateProgress(
                UpdateProgressRequest(subtaskId, status.name)
            )
            progressDao.updateStatus(subtaskId, status)
        } catch (e: Exception) {
            Log.e("ProgressRepository", "Failed to update progress", e)
        }
    }
}
```

---

### 6. Use Case 예시

**GetCurrentSubtaskUseCase.kt**:
```kotlin
class GetCurrentSubtaskUseCase @Inject constructor(
    private val progressRepository: ProgressRepository
) {
    suspend operator fun invoke(lectureId: Long): Flow<Result<Subtask>> {
        return progressRepository.getCurrentSubtask(lectureId)
    }
}
```

---

## 주요 플로우

### 1. 학습 시작 플로우

```
1. 사용자가 강의 선택 → LectureDetailActivity
2. "학습 시작" 버튼 클릭 → LearningActivity 시작
3. LearningViewModel.loadCurrentSubtask() 호출
4. Repository에서 현재 Subtask 가져오기
5. Overlay 표시 (OverlayManager.showGuide())
6. AccessibilityService 활성화 확인
7. 사용자 행동 감지 시작
```

### 2. 로그 수집 플로우

```
1. AccessibilityService가 이벤트 감지 (예: 클릭)
2. EventCollector가 로그 데이터 생성
3. LogRepository.sendActivityLog() 호출
4. Kafka Producer가 로그를 Kafka Topic으로 전송
5. (실패 시) 로컬 DB에 저장 후 나중에 재전송
```

### 3. 도움 요청 플로우

```
1. 사용자가 "?" 버튼 클릭 (Overlay)
2. LearningViewModel.requestHelp() 호출
3. HelpRepository.requestHelp() → API 호출
4. 서버가 Kafka로 M-GPT에 분석 요청
5. (앱) 2초마다 폴링으로 응답 확인
6. 응답 받으면 OverlayManager.showHelpResponse() 호출
7. TTS로 음성 안내 재생
```

---

## 의존성 주입 (Hilt)

**AppModule.kt**:
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideWindowManager(
        @ApplicationContext context: Context
    ): WindowManager {
        return context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(
        @ApplicationContext context: Context
    ): SharedPreferences {
        return context.getSharedPreferences("senior_edu_prefs", Context.MODE_PRIVATE)
    }
}
```

**NetworkModule.kt**:
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideLectureApi(retrofit: Retrofit): LectureApi {
        return retrofit.create(LectureApi::class.java)
    }
}
```

---

## 권한 관리

앱 실행에 필요한 권한:

1. **Accessibility Service**: 설정에서 수동으로 활성화
2. **Overlay Permission**: `SYSTEM_ALERT_WINDOW`
3. **인터넷**: `INTERNET`
4. **네트워크 상태**: `ACCESS_NETWORK_STATE`

**권한 요청 플로우**:
```kotlin
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissions()
    }

    private fun checkPermissions() {
        if (!isAccessibilityServiceEnabled()) {
            showAccessibilitySettingDialog()
        }

        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission()
        }
    }
}
```

---

## 테스트 전략

1. **Unit Tests**: Use Cases, ViewModels, Repositories
2. **Integration Tests**: Room DB, Retrofit API
3. **UI Tests**: Espresso (주요 화면)
4. **Accessibility Tests**: AccessibilityService 이벤트 수집

---

## 실시간 강의방 시스템 지원 ⭐ NEW

### 추가된 컴포넌트

#### 1. QR 스캔 기능
**QRScanActivity.kt**:
```kotlin
class QRScanActivity : AppCompatActivity() {
    private val codeScanner: CodeScanner by lazy {
        CodeScanner(this, binding.scannerView)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupScanner()
    }

    private fun setupScanner() {
        codeScanner.decodeCallback = DecodeCallback { result ->
            runOnUiThread {
                val sessionCode = result.text
                joinSession(sessionCode)
            }
        }
        codeScanner.startPreview()
    }

    private fun joinSession(sessionCode: String) {
        viewModel.getSessionByCode(sessionCode)
    }
}
```

#### 2. 대기실 화면
**WaitingRoomActivity.kt**:
```kotlin
@AndroidEntryPoint
class WaitingRoomActivity : AppCompatActivity() {
    private val viewModel: SessionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sessionId = intent.getLongExtra("session_id", -1)

        viewModel.joinSession(sessionId)
        observeSessionState()
        connectWebSocket(sessionId)
    }

    private fun observeSessionState() {
        lifecycleScope.launch {
            viewModel.sessionState.collect { state ->
                when (state.status) {
                    SessionStatus.WAITING -> showWaitingUI()
                    SessionStatus.IN_PROGRESS -> navigateToLearning()
                    else -> {}
                }
            }
        }
    }

    private fun connectWebSocket(sessionId: Long) {
        viewModel.connectToSessionWebSocket(sessionId)
    }

    private fun navigateToLearning() {
        startActivity(Intent(this, LearningActivity::class.java).apply {
            putExtra("session_id", viewModel.sessionId.value)
        })
        finish()
    }
}
```

#### 3. WebSocket 연결 관리
**SessionWebSocketManager.kt**:
```kotlin
@Singleton
class SessionWebSocketManager @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private var webSocket: WebSocket? = null
    private val _messages = MutableSharedFlow<WebSocketMessage>()
    val messages: SharedFlow<WebSocketMessage> = _messages.asSharedFlow()

    fun connect(sessionId: Long, token: String) {
        val request = Request.Builder()
            .url("$WS_BASE_URL/session/$sessionId/student/")
            .addHeader("Authorization", "Bearer $token")
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebSocket", "Connected to session $sessionId")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val message = Json.decodeFromString<WebSocketMessage>(text)
                GlobalScope.launch {
                    _messages.emit(message)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocket", "Connection failed", t)
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "Normal closure")
        webSocket = null
    }
}
```

#### 4. SessionViewModel (실시간 강의방 전용)
**SessionViewModel.kt**:
```kotlin
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val webSocketManager: SessionWebSocketManager,
    private val sharedPreferences: SharedPreferences
) : ViewModel() {

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Idle)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    val sessionId = MutableStateFlow<Long?>(null)

    fun getSessionByCode(code: String) {
        viewModelScope.launch {
            sessionRepository.getSessionByCode(code).collect { result ->
                when (result) {
                    is Result.Success -> {
                        _sessionState.value = SessionState.SessionFound(result.data)
                        sessionId.value = result.data.id
                    }
                    is Result.Error -> {
                        _sessionState.value = SessionState.Error(result.error)
                    }
                    is Result.Loading -> {
                        _sessionState.value = SessionState.Loading
                    }
                }
            }
        }
    }

    fun joinSession(sessionId: Long) {
        viewModelScope.launch {
            sessionRepository.joinSession(sessionId).collect { result ->
                when (result) {
                    is Result.Success -> {
                        _sessionState.value = SessionState.Joined(result.data)
                        this@SessionViewModel.sessionId.value = sessionId
                    }
                    is Result.Error -> {
                        _sessionState.value = SessionState.Error(result.error)
                    }
                    is Result.Loading -> {
                        _sessionState.value = SessionState.Loading
                    }
                }
            }
        }
    }

    fun connectToSessionWebSocket(sessionId: Long) {
        val token = sharedPreferences.getString("access_token", "") ?: return
        webSocketManager.connect(sessionId, token)

        viewModelScope.launch {
            webSocketManager.messages.collect { message ->
                handleWebSocketMessage(message)
            }
        }
    }

    private fun handleWebSocketMessage(message: WebSocketMessage) {
        when (message.type) {
            "session_started" -> {
                _sessionState.value = SessionState.Started(message.data)
            }
            "step_changed" -> {
                _sessionState.value = SessionState.StepChanged(message.data)
            }
            "session_paused" -> {
                _sessionState.value = SessionState.Paused
            }
            "session_resumed" -> {
                _sessionState.value = SessionState.Resumed
            }
            "session_ended" -> {
                _sessionState.value = SessionState.Ended
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        webSocketManager.disconnect()
    }
}

sealed class SessionState {
    object Idle : SessionState()
    object Loading : SessionState()
    data class SessionFound(val session: Session) : SessionState()
    data class Joined(val participant: Participant) : SessionState()
    data class Started(val data: JsonObject) : SessionState()
    data class StepChanged(val data: JsonObject) : SessionState()
    object Paused : SessionState()
    object Resumed : SessionState()
    object Ended : SessionState()
    data class Error(val message: String) : SessionState()
}
```

#### 5. SessionApi (새 API 엔드포인트)
**SessionApi.kt**:
```kotlin
interface SessionApi {
    @GET("/api/sessions/{code}")
    suspend fun getSessionByCode(@Path("code") code: String): Response<SessionResponse>

    @POST("/api/sessions/{id}/join")
    suspend fun joinSession(
        @Path("id") sessionId: Long,
        @Body request: JoinSessionRequest
    ): Response<ParticipantResponse>

    @GET("/api/sessions/{id}/current")
    suspend fun getCurrentSessionState(@Path("id") sessionId: Long): Response<SessionStateResponse>

    @GET("/api/sessions/my-active")
    suspend fun getMyActiveSession(): Response<ActiveSessionResponse>
}
```

#### 6. SessionRepository
**SessionRepositoryImpl.kt**:
```kotlin
class SessionRepositoryImpl @Inject constructor(
    private val sessionApi: SessionApi,
    private val sessionDao: SessionDao
) : SessionRepository {

    override suspend fun getSessionByCode(code: String): Flow<Result<Session>> = flow {
        emit(Result.Loading)
        try {
            val response = sessionApi.getSessionByCode(code)
            if (response.isSuccessful) {
                val session = response.body()?.toDomain()
                session?.let {
                    sessionDao.saveSession(it.toEntity())
                    emit(Result.Success(it))
                }
            } else {
                emit(Result.Error("세션을 찾을 수 없습니다"))
            }
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Unknown error"))
        }
    }

    override suspend fun joinSession(sessionId: Long): Flow<Result<Participant>> = flow {
        emit(Result.Loading)
        try {
            val response = sessionApi.joinSession(
                sessionId,
                JoinSessionRequest(getUserId())
            )
            if (response.isSuccessful) {
                val participant = response.body()?.toDomain()
                participant?.let {
                    emit(Result.Success(it))
                }
            } else {
                emit(Result.Error("세션 입장에 실패했습니다"))
            }
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Unknown error"))
        }
    }

    private fun getUserId(): Long {
        // SharedPreferences에서 user_id 가져오기
        return 1L
    }
}
```

### 수정된 학습 플로우 (실시간 강의방)

```
1. 학생이 QR 코드 스캔 → QRScanActivity
2. 세션 코드로 세션 조회 → SessionViewModel.getSessionByCode()
3. 세션 입장 → SessionViewModel.joinSession()
4. 대기실 화면 표시 → WaitingRoomActivity
5. WebSocket 연결 → SessionWebSocketManager.connect()
6. 강사가 수업 시작 → WebSocket 메시지 수신 ("session_started")
7. LearningActivity로 전환 → 첫 번째 단계 표시
8. 강사가 "다음 단계" 누르면 → WebSocket 메시지 수신 ("step_changed")
9. 모든 학생의 Overlay가 동시에 다음 단계로 업데이트
10. 강의 종료 → WebSocket 메시지 수신 ("session_ended")
11. 복습 모드 전환 (계속 학습 가능)
```

### 추가된 의존성

**build.gradle.kts**:
```kotlin
dependencies {
    // 기존 의존성...

    // WebSocket (OkHttp)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // QR 코드 스캔 (ZXing)
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.google.zxing:core:3.5.2")
}
```

### 권한 추가

**AndroidManifest.xml**:
```xml
<uses-permission android:name="android.permission.CAMERA" />
```

---

이 구조는 **실시간 동기식 강의방 시스템**을 지원하며, 확장 가능하고 유지보수가 용이한 Clean Architecture 원칙을 따릅니다.
