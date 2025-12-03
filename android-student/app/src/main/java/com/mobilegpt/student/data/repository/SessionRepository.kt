package com.mobilegpt.student.data.repository

import com.mobilegpt.student.data.api.ActivityLogRequest
import com.mobilegpt.student.data.api.ActivityLogResponse
import com.mobilegpt.student.data.api.JoinSessionRequest
import com.mobilegpt.student.data.api.JoinSessionResponse
import com.mobilegpt.student.data.api.ReportCompletionRequest
import com.mobilegpt.student.data.api.StudentApi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.mobilegpt.student.data.local.TokenPreferences
import com.mobilegpt.student.data.websocket.WebSocketConnectionState
import com.mobilegpt.student.data.websocket.WebSocketManager
import com.mobilegpt.student.domain.model.ActivityLog
import com.mobilegpt.student.domain.model.SessionData
import com.mobilegpt.student.domain.model.SessionMessage
import com.tinder.scarlet.WebSocket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Session Repository
 * 세션 관련 데이터 관리 및 WebSocket 연결 관리
 */
@Singleton
class SessionRepository @Inject constructor(
    private val studentApi: StudentApi,
    private val webSocketManager: WebSocketManager,
    private val tokenPreferences: TokenPreferences
) {

    /**
     * 세션 참가 (익명)
     * @param sessionCode 세션 코드
     * @param deviceId 기기 고유 ID
     * @param name 표시 이름
     */
    suspend fun joinSession(
        sessionCode: String,
        deviceId: String,
        name: String
    ): Result<JoinSessionResponse> {
        return try {
            val response = studentApi.joinSession(
                JoinSessionRequest(
                    session_code = sessionCode,
                    device_id = deviceId,
                    name = name
                )
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Result.failure(Exception("Failed to join session: ${response.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 내 세션 목록 조회
     */
    suspend fun getMySessions(): Result<List<SessionData>> {
        return try {
            val response = studentApi.getMySessions()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get sessions: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 진행 중인 세션 목록
     */
    suspend fun getActiveSessions(): Result<List<SessionData>> {
        return try {
            val response = studentApi.getActiveSessions()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get active sessions: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 세션 나가기
     */
    suspend fun leaveSession(sessionId: Int): Result<Unit> {
        return try {
            val response = studentApi.leaveSession(sessionId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to leave session: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== WebSocket Methods ====================

    /**
     * WebSocket 연결 상태 관찰
     */
    val connectionState: StateFlow<WebSocketConnectionState>
        get() = webSocketManager.connectionState

    /**
     * 현재 연결된 세션 코드
     */
    val connectedSessionCode: StateFlow<String?>
        get() = webSocketManager.connectedSessionCode

    /**
     * WebSocket 연결
     * @param sessionCode 세션 코드
     */
    fun connectWebSocket(sessionCode: String) {
        webSocketManager.connect(sessionCode)
    }

    /**
     * WebSocket 연결 해제
     */
    fun disconnectWebSocket() {
        webSocketManager.disconnect()
    }

    /**
     * WebSocket 연결 여부
     */
    fun isWebSocketConnected(): Boolean {
        return webSocketManager.isConnected()
    }

    /**
     * WebSocket 연결 상태 이벤트 관찰
     */
    fun observeWebSocketEvents(): Flow<WebSocket.Event> {
        return webSocketManager.observeWebSocketEvents() ?: emptyFlow()
    }

    /**
     * 서버 메시지 수신
     * SharedFlow를 반환하므로 WebSocket 연결 전에도 collect 가능
     */
    fun observeSessionMessages(): Flow<SessionMessage> {
        return webSocketManager.observeMessages()
    }

    /**
     * Join 메시지 전송
     * @param deviceId 기기 고유값
     * @param name 사용자 이름
     */
    fun sendJoinMessage(deviceId: String, name: String) {
        webSocketManager.sendJoinMessage(deviceId, name)
    }

    /**
     * 하트비트 전송
     */
    fun sendHeartbeat() {
        webSocketManager.sendHeartbeat()
    }

    /**
     * 단계 완료 알림
     * @param subtaskId 완료한 단계 ID
     */
    fun notifyStepComplete(subtaskId: Int) {
        val deviceId = tokenPreferences.getDeviceId()
        webSocketManager.sendStepComplete(subtaskId, deviceId)
    }

    /**
     * 도움 요청 (메시지 없이 즉시)
     * @param subtaskId 현재 단계 ID (optional)
     * @param screenshot Base64 인코딩된 스크린샷 (optional)
     */
    fun requestHelp(subtaskId: Int? = null, screenshot: String? = null) {
        val deviceId = tokenPreferences.getDeviceId()
        webSocketManager.sendHelpRequest(subtaskId, deviceId, screenshot)
    }

    /**
     * Activity Log 전송 (익명 사용자용)
     * device_id로 사용자를 식별합니다.
     */
    suspend fun sendActivityLog(log: ActivityLog): Result<ActivityLogResponse> {
        return try {
            // device_id 가져오기
            val deviceId = tokenPreferences.getDeviceId()
                ?: return Result.failure(Exception("Device ID not found"))

            // ActivityLog를 백엔드 API 형식으로 변환
            val request = ActivityLogRequest(
                device_id = deviceId,
                session = log.sessionId,
                subtask = log.subtaskId,
                event_type = log.eventType,
                event_data = buildMap {
                    put("package_name", log.packageName)
                    log.activityName?.let { put("activity_name", it) }
                    log.elementText?.let { put("element_text", it) }
                    log.elementType?.let { put("element_type", it) }
                    log.metadata?.let { putAll(it) }
                },
                screen_info = buildMap {
                    log.screenTitle?.let { put("title", it) }
                    log.activityName?.let { put("activity_name", it) }
                },
                node_info = buildMap {
                    log.elementId?.let { put("element_id", it) }
                    log.elementText?.let { put("text", it) }
                    log.elementType?.let { put("type", it) }
                },
                view_id_resource_name = log.elementId,
                content_description = log.elementText,
                is_sensitive_data = false
            )

            val response = studentApi.sendActivityLog(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to send activity log: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Step Completion Methods ====================

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())

    /**
     * 단계 완료 상태 서버에 보고
     *
     * @param sessionId 세션 ID
     * @param subtaskId 완료한 단계 ID
     * @return 성공 여부
     */
    suspend fun reportStepCompletion(sessionId: Int, subtaskId: Int): Result<Boolean> {
        return try {
            val deviceId = tokenPreferences.getDeviceId()
                ?: return Result.failure(Exception("Device ID not found"))

            val request = ReportCompletionRequest(
                device_id = deviceId,
                subtask_id = subtaskId,
                is_completed = true,
                completed_at = dateFormat.format(Date())
            )

            val response = studentApi.reportCompletion(sessionId, request)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success) {
                    Result.success(true)
                } else {
                    Result.failure(Exception(body.message))
                }
            } else {
                Result.failure(Exception("Failed to report completion: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
