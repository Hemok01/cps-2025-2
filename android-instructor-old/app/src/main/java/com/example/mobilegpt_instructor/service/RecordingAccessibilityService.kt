package com.example.mobilegpt_instructor.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityEvent
import com.example.mobilegpt_instructor.data.model.AccessibilityEventData

/**
 * 접근성 서비스 - 화면 이벤트를 캡처하여 녹화
 *
 * mobilegpt2의 MyAccessibilityService를 참조하여 구현
 * 필수 필드: time, eventType, package, className, text, contentDescription, viewId, bounds
 */
class RecordingAccessibilityService : AccessibilityService() {

    companion object {
        // 녹화 상태
        private var isRecording = false
        private var eventCallback: ((AccessibilityEventData) -> Unit)? = null

        // 서비스 인스턴스 (콜백 등록용)
        private var instance: RecordingAccessibilityService? = null

        /**
         * 녹화 시작 - 콜백 등록
         */
        fun startRecording(callback: (AccessibilityEventData) -> Unit) {
            eventCallback = callback
            isRecording = true
        }

        /**
         * 녹화 중지
         */
        fun stopRecording() {
            isRecording = false
            eventCallback = null
        }

        /**
         * 접근성 서비스 활성화 여부 확인
         */
        fun isEnabled(context: Context): Boolean {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServices)

            val myServiceName = "${context.packageName}/${RecordingAccessibilityService::class.java.canonicalName}"

            while (colonSplitter.hasNext()) {
                val componentName = colonSplitter.next()
                if (componentName.equals(myServiceName, ignoreCase = true)) {
                    return true
                }
            }
            return false
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this

        // 서비스 설정
        serviceInfo = serviceInfo?.apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = flags or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE
            notificationTimeout = 100
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isRecording || event == null) return

        // 이벤트 필터링 (중요한 이벤트만)
        val eventType = getEventTypeName(event.eventType)
        if (!isImportantEvent(event.eventType)) return

        // 이벤트 데이터 생성
        val eventData = AccessibilityEventData(
            time = event.eventTime,
            eventType = eventType,
            packageName = event.packageName?.toString(),
            className = event.className?.toString(),
            text = event.text?.joinToString(" ") { it.toString() },
            contentDescription = event.contentDescription?.toString(),
            viewId = event.source?.viewIdResourceName,
            bounds = getBoundsString(event)
        )

        // 콜백으로 이벤트 전달
        eventCallback?.invoke(eventData)
    }

    override fun onInterrupt() {
        // 서비스 인터럽트 처리
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        isRecording = false
        eventCallback = null
    }

    /**
     * 이벤트 타입을 문자열로 변환
     */
    private fun getEventTypeName(eventType: Int): String {
        return when (eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED -> "TYPE_VIEW_CLICKED"
            AccessibilityEvent.TYPE_VIEW_LONG_CLICKED -> "TYPE_VIEW_LONG_CLICKED"
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> "TYPE_VIEW_FOCUSED"
            AccessibilityEvent.TYPE_VIEW_SELECTED -> "TYPE_VIEW_SELECTED"
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> "TYPE_VIEW_TEXT_CHANGED"
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> "TYPE_WINDOW_STATE_CHANGED"
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> "TYPE_VIEW_SCROLLED"
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> "TYPE_VIEW_TEXT_SELECTION_CHANGED"
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> "TYPE_WINDOW_CONTENT_CHANGED"
            AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_START -> "TYPE_TOUCH_EXPLORATION_GESTURE_START"
            AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_END -> "TYPE_TOUCH_EXPLORATION_GESTURE_END"
            else -> "TYPE_UNKNOWN($eventType)"
        }
    }

    /**
     * 중요한 이벤트인지 확인 (노이즈 필터링)
     */
    private fun isImportantEvent(eventType: Int): Boolean {
        return eventType in listOf(
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            AccessibilityEvent.TYPE_VIEW_LONG_CLICKED,
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_VIEW_SCROLLED
        )
    }

    /**
     * Bounds를 문자열로 변환
     */
    private fun getBoundsString(event: AccessibilityEvent): String? {
        return try {
            val source = event.source ?: return null
            val rect = android.graphics.Rect()
            source.getBoundsInScreen(rect)
            "[${rect.left},${rect.top}][${rect.right},${rect.bottom}]"
        } catch (e: Exception) {
            null
        }
    }
}
