package com.mobilegpt.student.domain.model

/**
 * Session UI State
 * 세션 참여 전체 플로우의 상태를 나타내는 Sealed Class
 */
sealed class SessionUiState {

    /**
     * 초기 로딩 상태
     */
    object Loading : SessionUiState()

    /**
     * 권한 필요 상태
     */
    data class PermissionRequired(
        val overlayGranted: Boolean = false,
        val accessibilityGranted: Boolean = false
    ) : SessionUiState()

    /**
     * 세션 대기 상태 (강사가 시작하기 전)
     */
    data class Waiting(
        val sessionData: SessionData,
        val connectionStatus: ConnectionStatus = ConnectionStatus.CONNECTING
    ) : SessionUiState()

    /**
     * 세션 활성 상태 (강의 진행 중)
     */
    data class Active(
        val sessionData: SessionData,
        val currentStep: SubtaskDetail? = null,
        val currentStepIndex: Int = 1,
        val totalSteps: Int = 1,
        val isOverlayShowing: Boolean = false,
        val connectionStatus: ConnectionStatus = ConnectionStatus.CONNECTED
    ) : SessionUiState()

    /**
     * 세션 일시정지 상태
     */
    data class Paused(
        val sessionData: SessionData,
        val message: String = ""
    ) : SessionUiState()

    /**
     * 세션 종료 상태
     */
    data class Ended(
        val sessionData: SessionData,
        val summary: SessionSummary
    ) : SessionUiState()

    /**
     * 에러 상태
     */
    data class Error(
        val message: String,
        val canRetry: Boolean = true
    ) : SessionUiState()
}

/**
 * WebSocket 연결 상태
 */
enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

/**
 * 세션 결과 요약
 */
data class SessionSummary(
    val durationMinutes: Long = 0,
    val completedSteps: Int = 0,
    val totalSteps: Int = 0,
    val helpRequestCount: Int = 0,
    val eventsLogged: Int = 0
)

/**
 * Map에서 SubtaskDetail 파싱
 * 기존 SubtaskDetail 클래스는 SessionData.kt에 정의되어 있음
 */
@Suppress("UNCHECKED_CAST")
fun Map<String, Any>.toSubtaskDetail(): SubtaskDetail? {
    return try {
        SubtaskDetail(
            id = (this["id"] as? Number)?.toInt() ?: return null,
            task = (this["task"] as? Number)?.toInt() ?: 0,
            taskTitle = this["task_title"] as? String,
            title = this["title"] as? String ?: "",
            description = this["description"] as? String ?: "",
            order = (this["order"] as? Number)?.toInt() ?: 0,
            targetApp = this["target_app"] as? String,
            targetAction = this["target_action"] as? String
        )
    } catch (e: Exception) {
        null
    }
}
