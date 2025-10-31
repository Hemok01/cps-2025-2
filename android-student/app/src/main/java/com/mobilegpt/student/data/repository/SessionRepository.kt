package com.mobilegpt.student.data.repository

import com.mobilegpt.student.data.api.ActivityLogRequest
import com.mobilegpt.student.data.api.ActivityLogResponse
import com.mobilegpt.student.data.api.JoinSessionRequest
import com.mobilegpt.student.data.api.JoinSessionResponse
import com.mobilegpt.student.data.api.StudentApi
import com.mobilegpt.student.data.api.WebSocketApi
import com.mobilegpt.student.domain.model.ActivityLog
import com.mobilegpt.student.domain.model.ClientMessage
import com.mobilegpt.student.domain.model.MessageType
import com.mobilegpt.student.domain.model.SessionData
import com.mobilegpt.student.domain.model.SessionMessage
import com.tinder.scarlet.WebSocket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Session Repository
 * 세션 관련 데이터 관리
 */
@Singleton
class SessionRepository @Inject constructor(
    private val studentApi: StudentApi,
    private val webSocketApi: WebSocketApi
) {

    /**
     * 세션 참가
     */
    suspend fun joinSession(sessionCode: String): Result<JoinSessionResponse> {
        return try {
            val response = studentApi.joinSession(JoinSessionRequest(sessionCode))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to join session: ${response.code()}"))
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

    // WebSocket Methods

    /**
     * WebSocket 연결 상태 관찰
     */
    fun observeWebSocketEvents(): Flow<WebSocket.Event> {
        return webSocketApi.observeWebSocketEvent().receiveAsFlow()
    }

    /**
     * 서버 메시지 수신
     */
    fun observeSessionMessages(): Flow<SessionMessage> {
        return webSocketApi.observeMessages().receiveAsFlow()
    }

    /**
     * 하트비트 전송
     */
    fun sendHeartbeat() {
        webSocketApi.sendMessage(
            ClientMessage(
                type = MessageType.HEARTBEAT,
                data = mapOf("timestamp" to System.currentTimeMillis())
            )
        )
    }

    /**
     * 단계 완료 알림
     */
    fun notifyStepComplete(subtaskId: Int) {
        webSocketApi.sendMessage(
            ClientMessage(
                type = MessageType.STEP_COMPLETE,
                data = mapOf("subtask_id" to subtaskId)
            )
        )
    }

    /**
     * 도움 요청
     */
    fun requestHelp(message: String? = null) {
        webSocketApi.sendMessage(
            ClientMessage(
                type = MessageType.REQUEST_HELP,
                data = message?.let { mapOf("message" to it) }
            )
        )
    }

    /**
     * Activity Log 전송
     */
    suspend fun sendActivityLog(log: ActivityLog): Result<ActivityLogResponse> {
        return try {
            // ActivityLog를 백엔드 API 형식으로 변환
            val request = ActivityLogRequest(
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
}
