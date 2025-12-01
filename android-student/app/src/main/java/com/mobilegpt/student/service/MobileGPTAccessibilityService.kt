package com.mobilegpt.student.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.mobilegpt.student.data.local.SessionPreferences
import com.mobilegpt.student.data.repository.SessionRepository
import com.mobilegpt.student.detector.CurrentSignatureHolder
import com.mobilegpt.student.detector.SignatureExtractor
import com.mobilegpt.student.detector.StepCompletionChecker
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
 */
class MobileGPTAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())

    private lateinit var sessionRepository: SessionRepository
    private lateinit var sessionPreferences: SessionPreferences
    private lateinit var stepCompletionChecker: StepCompletionChecker

    companion object {
        private const val TAG = "MobileGPT_A11y"
        var isRunning = false
            private set

        // FloatingOverlayService로 완료 피드백 전송을 위한 액션
        const val ACTION_STEP_COMPLETED = "com.mobilegpt.student.STEP_COMPLETED"
        const val EXTRA_SUBTASK_ID = "subtask_id"
        const val EXTRA_SUBTASK_TITLE = "subtask_title"
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

        Log.d(TAG, "Accessibility Service Connected with Step Completion Checker")
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

        // 로그 생성
        val log = createActivityLog(event, eventType)

        // 로그 전송 (비동기)
        serviceScope.launch {
            sendLogToServer(log)
        }

        // [NEW] UI 시그니처 추출 및 단계 완료 체크
        checkStepCompletion(event, eventType)
    }

    /**
     * 단계 완료 여부 확인 및 서버 보고
     */
    private fun checkStepCompletion(event: AccessibilityEvent, eventType: String) {
        // 초기화 확인
        if (!::stepCompletionChecker.isInitialized || !::sessionPreferences.isInitialized) {
            return
        }

        // 세션이 활성화되어 있는지 확인
        val sessionId = sessionPreferences.getSessionId() ?: return
        val currentSubtask = sessionPreferences.getCurrentSubtaskDetail() ?: return

        // UI 시그니처 추출
        val signature = SignatureExtractor.fromEvent(event)

        // 현재 시그니처 상태 업데이트 (버퍼)
        CurrentSignatureHolder.update(signature, event.eventType)

        Log.d(TAG, "Checking step completion for: ${currentSubtask.title}")
        Log.d(TAG, "Signature: package=${signature["package"]}, viewId=${signature["viewId"]}")

        // 단계 완료 체크 (비동기)
        serviceScope.launch {
            try {
                val result = stepCompletionChecker.checkCompletion(
                    currentSubtask = currentSubtask,
                    signature = signature,
                    eventType = event.eventType
                )

                if (result.isCompleted && result.isNewCompletion) {
                    Log.i(TAG, "Step completed! Reporting to server...")

                    // 서버에 완료 상태 보고
                    val reportResult = stepCompletionChecker.reportCompletion(
                        subtaskId = currentSubtask.id,
                        sessionId = sessionId
                    )

                    reportResult.onSuccess {
                        Log.i(TAG, "Completion reported successfully")
                        // FloatingOverlayService에 완료 알림
                        notifyStepCompleted(currentSubtask.id, currentSubtask.title)
                    }.onFailure { error ->
                        Log.e(TAG, "Failed to report completion: ${error.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking step completion", e)
            }
        }
    }

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

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility Service Interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
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
