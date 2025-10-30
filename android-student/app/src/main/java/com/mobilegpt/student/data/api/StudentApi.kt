package com.mobilegpt.student.data.api

import com.mobilegpt.student.domain.model.SessionData
import retrofit2.Response
import retrofit2.http.*

/**
 * Student REST API
 */
interface StudentApi {

    /**
     * 세션 참가
     */
    @POST("students/sessions/join/")
    suspend fun joinSession(
        @Body request: JoinSessionRequest
    ): Response<JoinSessionResponse>

    /**
     * 내 세션 목록
     */
    @GET("students/sessions/my_sessions/")
    suspend fun getMySessions(): Response<List<SessionData>>

    /**
     * 진행 중인 세션 목록
     */
    @GET("students/sessions/active_sessions/")
    suspend fun getActiveSessions(): Response<List<SessionData>>

    /**
     * 세션 나가기
     */
    @POST("students/sessions/{id}/leave/")
    suspend fun leaveSession(
        @Path("id") sessionId: Int
    ): Response<Unit>

    /**
     * Activity Log 전송
     */
    @POST("logs/activity/")
    suspend fun sendActivityLog(
        @Body request: ActivityLogRequest
    ): Response<ActivityLogResponse>
}

/**
 * Request/Response Models
 */
data class JoinSessionRequest(
    val session_code: String
)

data class JoinSessionResponse(
    val message: String,
    val session: SessionData,
    val participant: ParticipantData
)

data class ParticipantData(
    val id: Int,
    val status: String,
    val joined_at: String
)

/**
 * Activity Log Request
 * 백엔드 API 형식에 맞춘 요청 모델
 */
data class ActivityLogRequest(
    val session: Int?,
    val subtask: Int?,
    val event_type: String,
    val event_data: Map<String, Any>? = null,
    val screen_info: Map<String, Any>? = null,
    val node_info: Map<String, Any>? = null,
    val parent_node_info: Map<String, Any>? = null,
    val view_id_resource_name: String? = null,
    val content_description: String? = null,
    val is_sensitive_data: Boolean = false
)

/**
 * Activity Log Response
 */
data class ActivityLogResponse(
    val log_id: Int,
    val message: String
)
