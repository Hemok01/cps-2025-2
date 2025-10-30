package com.mobilegpt.student.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.mobilegpt.student.domain.model.ActivityLog
import com.mobilegpt.student.domain.model.EventType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * MobileGPT Accessibility Service
 * UI 이벤트 감지 및 로그 수집
 */
class MobileGPTAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())

    companion object {
        private const val TAG = "MobileGPT_A11y"
        var isRunning = false
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        Log.d(TAG, "Accessibility Service Connected")
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

        return ActivityLog(
            sessionId = null, // TODO: 현재 세션 ID 가져오기
            subtaskId = null, // TODO: 현재 단계 ID 가져오기
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
            // TODO: Repository를 통한 API 호출
            Log.d(TAG, "Log collected: ${log.eventType} - ${log.packageName}")

            // 임시: 로그 출력
            Log.d(TAG, "Event: ${log.eventType}")
            Log.d(TAG, "Package: ${log.packageName}")
            Log.d(TAG, "Element: ${log.elementText}")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to send log", e)
        }
    }
}
