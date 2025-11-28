package com.mobilegpt.student.data.api

import com.mobilegpt.student.service.ScreenshotUploadRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Screenshot API Interface
 * 학생 화면 스크린샷 업로드용 REST API
 */
interface ScreenshotApi {

    /**
     * 스크린샷 업로드
     * POST /api/sessions/{sessionId}/screenshots/upload/
     *
     * @param sessionId 세션 ID
     * @param request 스크린샷 업로드 요청 (device_id, image_data, captured_at)
     * @return 업로드된 스크린샷 정보
     */
    @POST("sessions/{sessionId}/screenshots/upload/")
    suspend fun uploadScreenshot(
        @Path("sessionId") sessionId: Int,
        @Body request: ScreenshotUploadRequest
    ): Response<ScreenshotUploadResponse>
}

/**
 * 스크린샷 업로드 응답 데이터 클래스
 */
data class ScreenshotUploadResponse(
    val id: Int,
    val session: Int,
    val participant: Int?,
    val device_id: String,
    val participant_name: String,
    val image_url: String,
    val captured_at: String,
    val created_at: String
)
