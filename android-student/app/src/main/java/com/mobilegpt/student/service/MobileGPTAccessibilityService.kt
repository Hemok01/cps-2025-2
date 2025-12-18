package com.mobilegpt.student.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.mobilegpt.student.data.local.SessionPreferences
import com.mobilegpt.student.data.repository.SessionRepository
import com.mobilegpt.student.detector.AdvancedStepMatcher
import com.mobilegpt.student.detector.CurrentSignatureHolder
import com.mobilegpt.student.detector.ErrorDetector
import com.mobilegpt.student.detector.SignatureExtractor
import com.mobilegpt.student.detector.StepCompletionChecker
import com.mobilegpt.student.detector.UISnapshotBuilder
import com.mobilegpt.student.detector.models.StepExpectation
import com.mobilegpt.student.detector.models.TrackingState
import com.mobilegpt.student.domain.model.ActivityLog
import com.mobilegpt.student.domain.model.EventType
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Hilt EntryPoint for AccessibilityService
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AccessibilityServiceEntryPoint {
    fun sessionRepository(): SessionRepository
    fun stepCompletionChecker(): StepCompletionChecker
    fun sessionPreferencesProvider(): SessionPreferences
}

/**
 * MobileGPT Accessibility Service
 * UI 이벤트 감지, 로그 수집, 단계 완료 판단
 *
 * UIComparison2 프로젝트의 StepMonitoringService 참고하여 구현:
 * - 디바운싱 (150ms): 연속된 이벤트를 묶어서 처리
 * - UISnapshot 기반 매칭: 전체 화면 상태를 캡처하여 비교
 * - 오류 감지: WRONG_APP, FROZEN_SCREEN, WRONG_CLICK
 */
class MobileGPTAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())

    private lateinit var sessionRepository: SessionRepository
    private lateinit var sessionPreferences: SessionPreferences
    private lateinit var stepCompletionChecker: StepCompletionChecker

    // ===== 새로 추가: UISnapshot 기반 매칭 =====
    private val advancedMatcher = AdvancedStepMatcher()
    private val errorDetector = ErrorDetector()

    // 디바운싱을 위한 Handler
    private val mainHandler = Handler(Looper.getMainLooper())
    private var pendingSnapshotCheck: Runnable? = null

    // 현재 추적 상태
    private var currentTrackingState: TrackingState = TrackingState.WAITING

    companion object {
        private const val TAG = "MobileGPT_A11y"
        var isRunning = false
            private set

        /**
         * 디바운싱 지연 시간 (밀리초)
         * UIComparison2에서는 150ms 사용
         */
        const val DEBOUNCE_DELAY_MS = 150L

        // FloatingOverlayService로 완료 피드백 전송을 위한 액션
        const val ACTION_STEP_COMPLETED = "com.mobilegpt.student.STEP_COMPLETED"
        const val ACTION_TRACKING_STATE_CHANGED = "com.mobilegpt.student.TRACKING_STATE_CHANGED"
        const val ACTION_ERROR_DETECTED = "com.mobilegpt.student.ERROR_DETECTED"
        const val EXTRA_SUBTASK_ID = "subtask_id"
        const val EXTRA_SUBTASK_TITLE = "subtask_title"
        const val EXTRA_TRACKING_STATE = "tracking_state"
        const val EXTRA_ERROR_TYPE = "error_type"
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true

        // Hilt EntryPoint를 통해 의존성 가져오기
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            AccessibilityServiceEntryPoint::class.java
        )
        sessionRepository = entryPoint.sessionRepository()
        stepCompletionChecker = entryPoint.stepCompletionChecker()
        sessionPreferences = entryPoint.sessionPreferencesProvider()

        // 오류 감지기 초기화
        errorDetector.reset()

        Log.d(TAG, "Accessibility Service Connected with UISnapshot-based Step Completion")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // 이벤트 타입 필터링
        val eventType = when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED -> EventType.CLICK
            AccessibilityEvent.TYPE_VIEW_LONG_CLICKED -> EventType.LONG_CLICK
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> EventType.SCROLL
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> EventType.TEXT_INPUT
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> EventType.FOCUS
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> EventType.WINDOW_CHANGE
            else -> return // 관심 없는 이벤트는 무시
        }

        // 이벤트 패키지 저장 (디바운싱된 스냅샷 체크에서 사용)
        val packageName = event.packageName?.toString() ?: "unknown"

        // 로그 생성
        val log = createActivityLog(event, eventType)

        // 로그 전송 (비동기)
        serviceScope.launch {
            sendLogToServer(log)
        }

        // [통합됨] UISnapshot 기반 단계 완료 체크 (디바운싱)
        // 기존 시그니처 기반 체크는 제거하고 AdvancedStepMatcher로 통합
        // 연속된 이벤트를 묶어서 150ms 후 스냅샷 생성 및 매칭
        scheduleSnapshotCheck(packageName, event.eventType)
    }

    /**
     * UISnapshot 체크를 디바운싱하여 스케줄
     *
     * UIComparison2의 디바운싱 전략:
     * - 연속된 이벤트를 하나로 묶어 처리 (150ms 대기)
     * - 마지막 이벤트 후 150ms가 지나면 스냅샷 생성
     * - 불필요한 중복 체크 방지 및 성능 최적화
     */
    private fun scheduleSnapshotCheck(packageName: String, rawEventType: Int) {
        // 기존 대기 중인 체크 취소
        pendingSnapshotCheck?.let { mainHandler.removeCallbacks(it) }

        // 새로운 체크 스케줄
        pendingSnapshotCheck = Runnable {
            createAndCheckSnapshot(packageName, rawEventType)
        }
        mainHandler.postDelayed(pendingSnapshotCheck!!, DEBOUNCE_DELAY_MS)
    }

    /**
     * UISnapshot을 생성하고 AdvancedStepMatcher로 단계 완료 체크
     *
     * 이 메서드는 디바운싱 후 호출되며:
     * 1. 전체 화면 스냅샷 생성 (UISnapshotBuilder)
     * 2. 현재 단계의 기대 조건과 비교 (AdvancedStepMatcher)
     * 3. 오류 감지 (ErrorDetector)
     * 4. 결과에 따른 상태 업데이트 및 브로드캐스트
     */
    private fun createAndCheckSnapshot(packageName: String, rawEventType: Int) {
        // 초기화 확인
        if (!::sessionPreferences.isInitialized || !::stepCompletionChecker.isInitialized) {
            return
        }

        // 세션이 활성화되어 있는지 확인
        val sessionId = sessionPreferences.getSessionId()
        if (sessionId == null) {
            Log.d(TAG, "createAndCheckSnapshot: No active session (sessionId is null)")
            return
        }

        val currentSubtask = sessionPreferences.getCurrentSubtaskDetail()
        if (currentSubtask == null) {
            Log.d(TAG, "createAndCheckSnapshot: No current subtask detail (subtask is null) - sessionId=$sessionId")
            return
        }

        Log.d(TAG, "createAndCheckSnapshot: subtask=${currentSubtask.id}, title=${currentSubtask.title}, " +
                "viewId=${currentSubtask.viewId}, text=${currentSubtask.text}, package=${currentSubtask.effectivePackage}")

        // 현재 화면 스냅샷 생성
        val rootNode = rootInActiveWindow
        if (rootNode == null) {
            Log.w(TAG, "rootInActiveWindow is null, cannot create snapshot")
            return
        }

        try {
            // UISnapshot 생성
            val snapshot = UISnapshotBuilder.build(rootNode, packageName)
            Log.d(TAG, "Created UISnapshot: package=${snapshot.packageName}, " +
                    "views=${snapshot.visibleViews.size}, texts=${snapshot.textNodes.size}")

            // StepExpectation 생성
            val expectation = StepExpectation.fromSubtask(currentSubtask)

            // 1. 오류 감지
            val detectedError = errorDetector.detectError(snapshot, expectation)
            if (detectedError != null && errorDetector.shouldReport(detectedError.type)) {
                Log.w(TAG, "Error detected: ${detectedError.type}")
                updateTrackingState(TrackingState.ERROR)
                notifyErrorDetected(detectedError)

                // 서버에 오류 보고 (비동기)
                serviceScope.launch {
                    reportErrorToServer(detectedError, sessionId, currentSubtask.id)
                }
                return
            }

            // 2. UISnapshot 기반 매칭 (행동 타입도 함께 검증)
            val matchResult = advancedMatcher.match(snapshot, expectation, rawEventType)
            Log.d(TAG, "=== Match Result ===")
            Log.d(TAG, "  isMatched: ${matchResult.isMatched}")
            Log.d(TAG, "  matchRatio: ${matchResult.matchRatio}")
            Log.d(TAG, "  packageMatched: ${matchResult.packageMatched}")
            Log.d(TAG, "  actionMismatch: ${matchResult.actionMismatch}")
            Log.d(TAG, "  matchedKeyViews: ${matchResult.matchedKeyViews.size}")
            Log.d(TAG, "  unmatchedKeyViews: ${matchResult.unmatchedKeyViews.size}")
            if (!matchResult.isMatched) {
                Log.d(TAG, "  failureReason: ${matchResult.failureReason}")
            }

            // 3. 상태 업데이트
            val newState = TrackingState.fromMatchResult(matchResult)
            if (newState != currentTrackingState) {
                updateTrackingState(newState)
            }

            // 4. 매칭 성공 시 완료 처리
            if (matchResult.isMatched) {
                serviceScope.launch {
                    handleStepCompletion(currentSubtask.id, currentSubtask.title, sessionId)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in createAndCheckSnapshot", e)
        } finally {
            rootNode.recycle()
        }
    }

    // [제거됨] checkStepCompletion - AdvancedStepMatcher 기반 createAndCheckSnapshot으로 통합됨

    /**
     * FloatingOverlayService에 단계 완료 알림
     */
    private fun notifyStepCompleted(subtaskId: Int, subtaskTitle: String) {
        val intent = Intent(ACTION_STEP_COMPLETED).apply {
            putExtra(EXTRA_SUBTASK_ID, subtaskId)
            putExtra(EXTRA_SUBTASK_TITLE, subtaskTitle)
            setPackage(packageName)
        }
        sendBroadcast(intent)
        Log.d(TAG, "Sent step completion broadcast: $subtaskTitle")
    }

    /**
     * 추적 상태 업데이트 및 브로드캐스트
     */
    private fun updateTrackingState(newState: TrackingState) {
        if (currentTrackingState == newState) return

        val oldState = currentTrackingState
        currentTrackingState = newState

        Log.d(TAG, "Tracking state changed: $oldState -> $newState")

        // FloatingOverlayService에 상태 변경 알림
        val intent = Intent(ACTION_TRACKING_STATE_CHANGED).apply {
            putExtra(EXTRA_TRACKING_STATE, newState.name)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    /**
     * 오류 감지 알림 (FloatingOverlayService로 전송)
     */
    private fun notifyErrorDetected(error: com.mobilegpt.student.detector.models.DetectedError) {
        val intent = Intent(ACTION_ERROR_DETECTED).apply {
            putExtra(EXTRA_ERROR_TYPE, error.type.name)
            putExtra(EXTRA_SUBTASK_ID, error.subtaskId)
            setPackage(packageName)
        }
        sendBroadcast(intent)
        Log.d(TAG, "Error broadcast sent: ${error.type}")
    }

    /**
     * 서버에 오류 보고
     */
    private suspend fun reportErrorToServer(
        error: com.mobilegpt.student.detector.models.DetectedError,
        sessionId: Int,
        subtaskId: Int
    ) {
        try {
            if (!::sessionRepository.isInitialized) return

            // 오류를 ActivityLog로 변환하여 전송
            val errorLog = ActivityLog(
                sessionId = sessionId,
                subtaskId = subtaskId,
                eventType = "ERROR_${error.type.name}",
                packageName = error.actualPackage ?: "unknown",
                activityName = null,
                elementId = null,
                elementText = error.additionalInfo,
                elementType = null,
                screenTitle = null,
                timestamp = dateFormat.format(Date()),
                metadata = mapOf(
                    "error_type" to error.type.name,
                    "expected_package" to (error.expectedPackage ?: ""),
                    "actual_package" to (error.actualPackage ?: ""),
                    "additional_info" to (error.additionalInfo ?: "")
                )
            )

            val result = sessionRepository.sendActivityLog(errorLog)
            result.onSuccess {
                Log.d(TAG, "Error reported to server: ${error.type}")
                errorDetector.markAsReported(error)
            }.onFailure { e ->
                Log.e(TAG, "Failed to report error: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reporting to server failed", e)
        }
    }

    /**
     * 단계 완료 처리 (UISnapshot 기반)
     *
     * StepCompletionChecker를 통해 중복 완료를 방지하고 서버에 보고
     */
    private suspend fun handleStepCompletion(subtaskId: Int, subtaskTitle: String, sessionId: Int) {
        try {
            // 기존 StepCompletionChecker를 통한 완료 보고 (중복 방지 포함)
            val reportResult = stepCompletionChecker.reportCompletion(
                subtaskId = subtaskId,
                sessionId = sessionId
            )

            reportResult.onSuccess {
                Log.i(TAG, "Step completed via UISnapshot matching: $subtaskTitle")
                updateTrackingState(TrackingState.MATCHED)
                notifyStepCompleted(subtaskId, subtaskTitle)
            }.onFailure { error ->
                // 이미 완료된 경우도 있으므로 warn 레벨로 로깅
                Log.w(TAG, "Step completion report result: ${error.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling step completion", e)
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility Service Interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false

        // 대기 중인 스냅샷 체크 취소
        pendingSnapshotCheck?.let { mainHandler.removeCallbacks(it) }
        pendingSnapshotCheck = null

        // 오류 감지기 리셋
        errorDetector.reset()

        Log.d(TAG, "Accessibility Service Destroyed")
    }

    /**
     * AccessibilityEvent로부터 ActivityLog 생성
     */
    private fun createActivityLog(event: AccessibilityEvent, eventType: String): ActivityLog {
        val packageName = event.packageName?.toString() ?: "unknown"
        val className = event.className?.toString()

        // Source 노드에서 추가 정보 추출
        val sourceNode = event.source
        val elementId = sourceNode?.viewIdResourceName
        val elementText = getNodeText(sourceNode)
        val elementType = className

        // 메타데이터 수집
        val metadata = mutableMapOf<String, Any>()
        metadata["event_time"] = event.eventTime
        metadata["item_count"] = event.itemCount

        if (eventType == EventType.SCROLL) {
            metadata["scroll_x"] = event.scrollX
            metadata["scroll_y"] = event.scrollY
        }

        sourceNode?.recycle()

        // SharedPreferences에서 세션 ID와 서브태스크 ID 가져오기
        val sessionId = if (::sessionPreferences.isInitialized) {
            sessionPreferences.getSessionId()
        } else null

        val subtaskId = if (::sessionPreferences.isInitialized) {
            sessionPreferences.getSubtaskId()
        } else null

        return ActivityLog(
            sessionId = sessionId,
            subtaskId = subtaskId,
            eventType = eventType,
            packageName = packageName,
            activityName = className,
            elementId = elementId,
            elementText = elementText,
            elementType = elementType,
            screenTitle = getScreenTitle(event),
            timestamp = dateFormat.format(Date()),
            metadata = metadata
        )
    }

    /**
     * 노드에서 텍스트 추출
     */
    private fun getNodeText(node: AccessibilityNodeInfo?): String? {
        if (node == null) return null

        return when {
            !node.text.isNullOrEmpty() -> node.text.toString()
            !node.contentDescription.isNullOrEmpty() -> node.contentDescription.toString()
            else -> null
        }
    }

    /**
     * 화면 제목 추출
     */
    private fun getScreenTitle(event: AccessibilityEvent): String? {
        return rootInActiveWindow?.let { root ->
            findTitle(root)?.also { root.recycle() }
        }
    }

    /**
     * 타이틀 노드 찾기 (재귀)
     */
    private fun findTitle(node: AccessibilityNodeInfo): String? {
        // 일반적인 타이틀 ID 패턴
        val titlePatterns = listOf(
            "title", "toolbar_title", "action_bar_title",
            "tv_title", "text_title"
        )

        val viewId = node.viewIdResourceName
        if (viewId != null && titlePatterns.any { viewId.contains(it, ignoreCase = true) }) {
            return getNodeText(node)
        }

        // 자식 노드 탐색
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val title = findTitle(child)
            child.recycle()
            if (title != null) return title
        }

        return null
    }

    /**
     * 서버로 로그 전송
     */
    private suspend fun sendLogToServer(log: ActivityLog) {
        try {
            // Repository를 통한 API 호출
            if (!::sessionRepository.isInitialized) {
                Log.w(TAG, "Repository not initialized yet")
                return
            }

            val result = sessionRepository.sendActivityLog(log)

            result.onSuccess { response ->
                Log.d(TAG, "Log sent successfully: ${response.log_id}")
                Log.d(TAG, "Event: ${log.eventType}, Package: ${log.packageName}, Element: ${log.elementText}")
            }.onFailure { error ->
                Log.e(TAG, "Failed to send log: ${error.message}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to send log", e)
        }
    }
}
