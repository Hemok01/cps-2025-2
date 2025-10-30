package com.mobilegpt.student.domain.model

import com.google.gson.annotations.SerializedName

/**
 * Activity Log Model
 * 사용자의 UI 이벤트 로그
 */
data class ActivityLog(
    @SerializedName("session_id")
    val sessionId: Int?,

    @SerializedName("subtask_id")
    val subtaskId: Int?,

    @SerializedName("event_type")
    val eventType: String,

    @SerializedName("package_name")
    val packageName: String,

    @SerializedName("activity_name")
    val activityName: String?,

    @SerializedName("element_id")
    val elementId: String?,

    @SerializedName("element_text")
    val elementText: String?,

    @SerializedName("element_type")
    val elementType: String?,

    @SerializedName("screen_title")
    val screenTitle: String?,

    @SerializedName("timestamp")
    val timestamp: String,

    @SerializedName("metadata")
    val metadata: Map<String, Any>? = null
)

/**
 * Event types
 */
object EventType {
    const val CLICK = "CLICK"
    const val LONG_CLICK = "LONG_CLICK"
    const val SCROLL = "SCROLL"
    const val TEXT_INPUT = "TEXT_INPUT"
    const val FOCUS = "FOCUS"
    const val WINDOW_CHANGE = "WINDOW_CHANGE"
    const val VIEW_CLICKED = "VIEW_CLICKED"
}
