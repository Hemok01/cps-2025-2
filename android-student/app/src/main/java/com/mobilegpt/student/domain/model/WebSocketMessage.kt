package com.mobilegpt.student.domain.model

import com.google.gson.annotations.SerializedName

/**
 * WebSocket Message Models
 */

/**
 * 서버로부터 받는 메시지
 *
 * ★ 주의: Gson 역직렬화 시 JSON에 type 필드가 없으면 null이 될 수 있음
 * Kotlin null safety를 우회하므로 nullable로 선언
 */
data class SessionMessage(
    @SerializedName("type")
    val type: String?,  // Gson에서 null이 들어올 수 있음

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
 * 백엔드 WebSocket Consumer와 동기화된 메시지 타입
 */
object MessageType {
    // Client -> Server
    const val JOIN = "join"
    const val HEARTBEAT = "heartbeat"
    const val STEP_COMPLETE = "step_complete"
    const val REQUEST_HELP = "request_help"

    // Server -> Client (확인 메시지)
    const val JOIN_CONFIRMED = "join_confirmed"
    const val HEARTBEAT_ACK = "heartbeat_ack"
    const val STEP_COMPLETE_CONFIRMED = "step_complete_confirmed"

    // Server -> Client (세션 상태)
    const val SESSION_STATUS_CHANGED = "session_status_changed"
    const val STEP_CHANGED = "step_changed"

    // Server -> Client (참가자 관련)
    const val PARTICIPANT_JOINED = "participant_joined"
    const val PARTICIPANT_LEFT = "participant_left"

    // Server -> Client (진행도/도움 관련)
    const val PROGRESS_UPDATED = "progress_updated"
    const val HELP_REQUESTED = "help_requested"

    // Legacy (호환성 유지)
    const val STEP_UPDATE = "step_update"
    const val SESSION_START = "session_start"
    const val SESSION_END = "session_end"
    const val HELP_RESPONSE = "help_response"
    const val INSTRUCTOR_MESSAGE = "instructor_message"
}

/**
 * 세션 상태
 */
object SessionStatus {
    const val SCHEDULED = "SCHEDULED"
    const val WAITING = "WAITING"
    const val IN_PROGRESS = "IN_PROGRESS"
    const val PAUSED = "PAUSED"
    const val REVIEW_MODE = "REVIEW_MODE"
    const val ENDED = "ENDED"
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
