# AccessibilityService 상세 구현 가이드

## 개요

이 문서는 Android AccessibilityService를 활용한 시니어 디지털 교육 시스템의 상세 구현 가이드입니다.
Android 공식 문서([AccessibilityService Guide](https://developer.android.com/guide/topics/ui/accessibility/service))를 기반으로 작성되었습니다.

---

## 1. AccessibilityService란?

AccessibilityService는 시각/청각/신체 장애가 있는 사용자를 위해 Android가 제공하는 접근성 기능입니다.
우리 서비스는 이를 활용하여:
- 시니어 학습자의 UI 상호작용을 감지
- 실시간 가이드 제공
- 목표 달성 여부 자동 확인

**중요**: 이 기능은 **반드시 사용자 동의 하에 활용**되어야 하며, 수집된 데이터는 교육 목적으로만 사용됩니다.

---

## 2. 권한 및 설정

### 2.1 AndroidManifest.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.senior.edu.mobilegpt">

    <!-- 접근성 서비스 바인딩 권한 (필수) -->
    <uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE" />

    <!-- 오버레이 표시 권한 -->
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />

    <!-- 네트워크 권한 (로그 전송용) -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application
        android:name=".app.SeniorEduApplication"
        android:allowBackup="false"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.SeniorEdu">

        <!-- AccessibilityService 선언 -->
        <service
            android:name=".service.accessibility.SeniorEduAccessibilityService"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
            android:exported="true"
            android:enabled="true">

            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService" />
            </intent-filter>

            <meta-data
                android:name="android.accessibilityservice"
                android:resource="@xml/accessibility_service_config" />
        </service>

        <!-- 기타 컴포넌트들... -->

    </application>

</manifest>
```

**주요 속성 설명**:
- `android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"`: 시스템만 이 서비스에 바인딩 가능 (보안)
- `android:exported="true"`: 시스템이 접근할 수 있도록 설정
- `android:enabled="true"`: 서비스 활성화

---

### 2.2 XML 설정 파일

**res/xml/accessibility_service_config.xml**:

```xml
<?xml version="1.0" encoding="utf-8"?>
<accessibility-service
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:description="@string/accessibility_service_description"
    android:packageNames="@null"
    android:accessibilityEventTypes="typeViewClicked|typeViewFocused|typeViewScrolled|typeViewTextChanged|typeWindowStateChanged|typeViewLongClicked|typeViewSelected"
    android:accessibilityFlags="flagIncludeNotImportantViews|flagReportViewIds|flagRetrieveInteractiveWindows"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:notificationTimeout="100"
    android:canRetrieveWindowContent="true"
    android:settingsActivity="com.senior.edu.mobilegpt.presentation.ui.settings.SettingsActivity" />
```

**속성 상세 설명**:

| 속성 | 값 | 설명 |
|------|-----|------|
| `description` | @string 리소스 | 설정 화면에 표시될 서비스 설명 |
| `packageNames` | @null | 모든 앱의 이벤트 감지 (특정 앱만 감지하려면 "com.app1,com.app2") |
| `accessibilityEventTypes` | 타입들의 OR 조합 | 감지할 이벤트 타입 (아래 표 참조) |
| `accessibilityFlags` | 플래그들의 OR 조합 | 추가 기능 플래그 (아래 표 참조) |
| `accessibilityFeedbackType` | feedbackGeneric | 피드백 타입 (진동, 소리 등) |
| `notificationTimeout` | 100ms | 이벤트 알림 최소 간격 |
| `canRetrieveWindowContent` | true | UI 계층 구조 접근 가능 여부 (필수) |
| `settingsActivity` | Activity 클래스명 | 설정 화면 Activity |

**이벤트 타입**:
- `typeViewClicked`: 클릭
- `typeViewLongClicked`: 길게 누르기
- `typeViewFocused`: 포커스 변경
- `typeViewScrolled`: 스크롤
- `typeViewTextChanged`: 텍스트 변경
- `typeWindowStateChanged`: 화면 전환
- `typeViewSelected`: 항목 선택

**플래그**:
- `flagIncludeNotImportantViews`: 접근성 중요도가 낮은 뷰도 포함 (권장)
- `flagReportViewIds`: 뷰 ID 리소스 이름 보고 (필수)
- `flagRetrieveInteractiveWindows`: 상호작용 가능한 윈도우 정보 조회

---

### 2.3 strings.xml

```xml
<resources>
    <string name="accessibility_service_description">
        이 서비스는 시니어 학습자의 앱 사용을 돕기 위해 화면 상호작용을 감지합니다.
        수집된 정보는 학습 가이드 제공 및 진행 상황 추적에만 사용되며,
        민감한 정보(비밀번호 등)는 수집하지 않습니다.
    </string>
</resources>
```

---

## 3. 서비스 활성화 체크 및 유도

사용자가 AccessibilityService를 활성화하지 않으면 앱이 작동하지 않으므로, 활성화 여부를 체크하고 설정으로 유도해야 합니다.

### 3.1 활성화 체크 유틸리티

```kotlin
object AccessibilityUtil {

    /**
     * AccessibilityService가 활성화되어 있는지 확인
     */
    fun isAccessibilityServiceEnabled(
        context: Context,
        serviceClass: Class<out AccessibilityService>
    ): Boolean {
        val expectedComponentName = ComponentName(context, serviceClass)
        val enabledServicesSetting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabledServicesSetting
            .split(":")
            .any { ComponentName.unflattenFromString(it) == expectedComponentName }
    }

    /**
     * 접근성 설정 화면으로 이동
     */
    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
}
```

### 3.2 활성화 유도 UI

**AccessibilityPermissionActivity.kt**:
```kotlin
class AccessibilityPermissionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accessibility_permission)

        checkAccessibilityService()

        findViewById<Button>(R.id.btnEnableService).setOnClickListener {
            AccessibilityUtil.openAccessibilitySettings(this)
        }
    }

    override fun onResume() {
        super.onResume()
        checkAccessibilityService()
    }

    private fun checkAccessibilityService() {
        if (AccessibilityUtil.isAccessibilityServiceEnabled(
                this,
                SeniorEduAccessibilityService::class.java
            )) {
            // 활성화됨 → 메인 화면으로 이동
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            // 아직 비활성화 → 안내 표시
            showEnableGuide()
        }
    }

    private fun showEnableGuide() {
        findViewById<TextView>(R.id.tvGuide).text = """
            학습 가이드 기능을 사용하려면 접근성 서비스를 활성화해주세요.

            1. "설정으로 이동" 버튼을 눌러주세요
            2. "시니어 교육 앱"을 찾아주세요
            3. 스위치를 켜주세요
            4. 다시 앱으로 돌아오세요
        """.trimIndent()
    }
}
```

---

## 4. 목표 요소 찾기 및 자동 완료

Subtask의 목표 요소를 찾아서 자동으로 완료 처리하는 기능입니다.

### 4.1 목표 검증 로직

```kotlin
class TargetValidator @Inject constructor(
    private val nodeInfoHelper: AccessibilityNodeInfoHelper
) {

    /**
     * Subtask의 목표가 달성되었는지 확인
     */
    fun isTargetAchieved(
        subtask: Subtask,
        event: AccessibilityEvent,
        source: AccessibilityNodeInfo?,
        rootNode: AccessibilityNodeInfo?
    ): Boolean {
        source ?: return false
        rootNode ?: return false

        return when (subtask.validationType) {
            ValidationType.AUTO -> checkAutoValidation(subtask, event, source)
            ValidationType.MANUAL -> false // 사용자가 직접 "완료" 버튼 클릭
            ValidationType.PATTERN_MATCH -> checkPatternMatch(subtask, source, rootNode)
            else -> false
        }
    }

    private fun checkAutoValidation(
        subtask: Subtask,
        event: AccessibilityEvent,
        source: AccessibilityNodeInfo
    ): Boolean {
        // 목표 액션 타입 확인
        if (event.eventType != getEventTypeFromSubtask(subtask.targetAction)) {
            return false
        }

        // 목표 요소 매칭
        return matchesTargetElement(subtask, source)
    }

    private fun checkPatternMatch(
        subtask: Subtask,
        source: AccessibilityNodeInfo,
        rootNode: AccessibilityNodeInfo
    ): Boolean {
        val criteria = subtask.validationCriteria ?: return false

        // JSON 형태의 검증 조건 파싱
        // 예: {"view_id": "btn_send", "action": "CLICK", "text": "전송"}
        val targetViewId = criteria.optString("view_id")
        val targetText = criteria.optString("text")
        val targetPackage = criteria.optString("package")

        // viewId 매칭
        if (targetViewId.isNotEmpty() && source.viewIdResourceName?.contains(targetViewId) != true) {
            return false
        }

        // 텍스트 매칭
        if (targetText.isNotEmpty() && source.text?.toString() != targetText) {
            return false
        }

        // 패키지 매칭
        if (targetPackage.isNotEmpty() && rootNode.packageName != targetPackage) {
            return false
        }

        return true
    }

    private fun matchesTargetElement(
        subtask: Subtask,
        source: AccessibilityNodeInfo
    ): Boolean {
        val hint = subtask.targetElementHint ?: return false

        // ViewId 힌트 매칭
        if (source.viewIdResourceName?.contains(hint, ignoreCase = true) == true) {
            return true
        }

        // 텍스트 힌트 매칭
        if (source.text?.toString()?.contains(hint, ignoreCase = true) == true) {
            return true
        }

        // ContentDescription 힌트 매칭
        if (source.contentDescription?.toString()?.contains(hint, ignoreCase = true) == true) {
            return true
        }

        return false
    }

    private fun getEventTypeFromSubtask(targetAction: String?): Int {
        return when (targetAction) {
            "CLICK" -> AccessibilityEvent.TYPE_VIEW_CLICKED
            "LONG_CLICK" -> AccessibilityEvent.TYPE_VIEW_LONG_CLICKED
            "SCROLL" -> AccessibilityEvent.TYPE_VIEW_SCROLLED
            "INPUT" -> AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
            "NAVIGATE" -> AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            else -> -1
        }
    }
}
```

### 4.2 자동 완료 처리

**SeniorEduAccessibilityService.kt (추가 부분)**:
```kotlin
class SeniorEduAccessibilityService : AccessibilityService() {

    @Inject lateinit var targetValidator: TargetValidator
    @Inject lateinit var progressRepository: ProgressRepository

    private var currentSubtask: Subtask? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        // ... (기존 로직)

        // 목표 달성 확인
        checkTargetAchievement(event)
    }

    private fun checkTargetAchievement(event: AccessibilityEvent) {
        val subtask = currentSubtask ?: return
        val source = event.source ?: return

        try {
            if (targetValidator.isTargetAchieved(subtask, event, source, rootInActiveWindow)) {
                Log.i(TAG, "Target achieved for subtask: ${subtask.id}")

                // 자동 완료 처리
                scope.launch {
                    progressRepository.updateProgress(
                        subtaskId = subtask.id,
                        status = ProgressStatus.COMPLETED
                    )
                }

                // 다음 Subtask 로드
                loadNextSubtask()
            }
        } finally {
            source.recycle()
        }
    }

    private fun loadNextSubtask() {
        // ViewModel 또는 Repository를 통해 다음 Subtask 로드
        // Overlay UI 업데이트
    }
}
```

---

## 5. 보안 및 개인정보 보호

### 5.1 민감 정보 필터링

**SensitiveDataFilter.kt** (확장):
```kotlin
class SensitiveDataFilter @Inject constructor(
    private val context: Context
) {

    private val sensitivePackages = setOf(
        // 은행 앱
        "com.kbstar.kbbank",
        "com.shinhan.sbanking",
        "com.wooribank.smart",
        "com.ibk.neobanking",
        "com.nhcard",

        // 금융 앱
        "com.kftc.bankpay",
        "com.samsung.android.spay",

        // 인증 앱
        "com.samsung.android.samsungpass",
        "com.google.android.gms",

        // 추가 가능...
    )

    private val sensitiveViewIds = setOf(
        "password", "pwd", "pin", "security",
        "account", "card_number", "cvv", "otp",
        "ssn", "social_security"
    )

    fun isSensitivePackage(packageName: String?): Boolean {
        packageName ?: return false
        return sensitivePackages.any { packageName.contains(it) }
    }

    fun isSensitiveNode(node: AccessibilityNodeInfo): Boolean {
        // 1. 비밀번호 필드
        if (node.isPassword) return true

        // 2. ViewId 체크
        node.viewIdResourceName?.let { viewId ->
            if (sensitiveViewIds.any { viewId.contains(it, ignoreCase = true) }) {
                return true
            }
        }

        // 3. ContentDescription 체크
        node.contentDescription?.toString()?.let { desc ->
            if (sensitiveViewIds.any { desc.contains(it, ignoreCase = true) }) {
                return true
            }
        }

        // 4. InputType 체크 (비밀번호 입력 필드)
        if (node.inputType and InputType.TYPE_TEXT_VARIATION_PASSWORD != 0) {
            return true
        }

        return false
    }

    /**
     * 텍스트 마스킹 (민감 정보는 일부만 표시)
     */
    fun maskSensitiveText(text: String): String {
        return when {
            text.length <= 2 -> "[REDACTED]"
            text.length <= 4 -> text[0] + "**"
            else -> text[0] + "*".repeat(text.length - 2) + text[text.length - 1]
        }
    }
}
```

### 5.2 데이터 보안 정책

1. **수집 제외 대상**:
   - 은행/금융 앱의 모든 이벤트
   - 비밀번호 필드 입력
   - 인증 앱 (OTP 등)

2. **마스킹 처리**:
   - 민감 필드의 텍스트는 `[REDACTED]` 또는 일부 마스킹
   - 저장 전 마스킹 처리

3. **암호화**:
   - `is_sensitive_data: true`인 로그는 DB 저장 시 추가 암호화

4. **자동 삭제**:
   - 민감 데이터는 분석 후 즉시 삭제 (7일 이내)

---

## 6. 디버깅 및 테스트

### 6.1 로그 확인

```kotlin
// AccessibilityService에서 로깅
override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    Log.d(TAG, """
        Event: ${event?.eventType}
        Package: ${event?.packageName}
        Source: ${event?.source?.viewIdResourceName}
        Text: ${event?.text}
    """.trimIndent())
}
```

**Logcat 필터**:
```
adb logcat | grep "SeniorEduA11yService"
```

### 6.2 테스트 시나리오

1. **클릭 이벤트 감지**:
   - 카카오톡 앱에서 "전송" 버튼 클릭
   - 로그에 CLICK 이벤트 출력 확인

2. **민감 정보 필터링**:
   - 은행 앱 실행
   - 이벤트가 수집되지 않는지 확인

3. **목표 달성 자동 확인**:
   - Subtask 목표 요소 클릭
   - 자동으로 완료 처리되는지 확인

4. **AccessibilityNodeInfo 재활용**:
   - 메모리 프로파일러로 메모리 누수 확인

---

## 7. 성능 최적화

### 7.1 이벤트 쓰로틀링

```kotlin
class EventThrottler {
    private val lastEventTime = mutableMapOf<String, Long>()
    private val throttleInterval = 100L // ms

    fun shouldProcess(eventKey: String): Boolean {
        val now = System.currentTimeMillis()
        val lastTime = lastEventTime[eventKey] ?: 0

        return if (now - lastTime > throttleInterval) {
            lastEventTime[eventKey] = now
            true
        } else {
            false
        }
    }
}
```

### 7.2 배치 전송

```kotlin
class LogBatchSender @Inject constructor(
    private val logRepository: LogRepository
) {
    private val logQueue = mutableListOf<ActivityLog>()
    private val batchSize = 50

    suspend fun addLog(log: ActivityLog) {
        logQueue.add(log)

        if (logQueue.size >= batchSize) {
            sendBatch()
        }
    }

    private suspend fun sendBatch() {
        if (logQueue.isEmpty()) return

        logRepository.sendBatchLogs(logQueue.toList())
        logQueue.clear()
    }
}
```

---

## 8. 사용자 가이드

### 8.1 설정 화면 안내

앱 내에 명확한 안내 페이지를 제공해야 합니다:

```
이 앱은 어떻게 도와드리나요?

✅ 실시간 학습 가이드
  - 현재 단계를 항상 표시합니다
  - 막힐 때 도움을 요청할 수 있습니다

✅ 자동 진행 상황 추적
  - 어디까지 완료했는지 자동 기록
  - 강사님께 진행 상황 공유

🔒 개인정보 보호
  - 비밀번호는 절대 수집하지 않습니다
  - 은행 앱 등은 감지하지 않습니다
  - 수집된 정보는 학습 목적으로만 사용됩니다
```

---

## 9. 주의사항

1. **시스템 부하**: 너무 많은 이벤트를 수집하면 시스템이 느려질 수 있으므로 쓰로틀링 필요

2. **배터리 소모**: 백그라운드에서 계속 동작하므로 최적화 필요

3. **호환성**: Android 버전별로 AccessibilityService 동작이 다를 수 있음

4. **사용자 신뢰**: 투명한 정보 제공 및 동의 절차 필수

---

이 가이드는 Android 공식 문서를 기반으로 작성되었으며, 실제 구현 시 최신 Android 버전과의 호환성을 확인해야 합니다.
