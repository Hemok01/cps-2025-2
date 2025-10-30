package com.mobilegpt.student.domain.model

import com.google.gson.annotations.SerializedName

/**
 * WebSocket Message Models
 */

/**
 * 서버로부터 받는 메시지
 */
data class SessionMessage(
    @SerializedName("type")
    val type: String,

    @SerializedName("data")
    val data: Map<String, Any>?
)

/**
 * 클라이언트에서 보내는 메시지
 */
data class ClientMessage(
    @SerializedName("type")
    val type: String,

    @SerializedName("data")
    val data: Map<String, Any>? = null
)

/**
 * Message Types
 */
object MessageType {
    // Client -> Server
    const val JOIN = "join"
    const val HEARTBEAT = "heartbeat"
    const val STEP_COMPLETE = "step_complete"
    const val REQUEST_HELP = "request_help"

    // Server -> Client
    const val STEP_UPDATE = "step_update"
    const val SESSION_START = "session_start"
    const val SESSION_END = "session_end"
    const val HELP_RESPONSE = "help_response"
    const val INSTRUCTOR_MESSAGE = "instructor_message"
}

/**
 * WebSocket Session Info (간단한 세션 정보)
 */
data class WsSessionInfo(
    @SerializedName("session_id")
    val sessionId: Int,

    @SerializedName("session_code")
    val sessionCode: String,

    @SerializedName("status")
    val status: String,

    @SerializedName("current_subtask_id")
    val currentSubtaskId: Int?
)
