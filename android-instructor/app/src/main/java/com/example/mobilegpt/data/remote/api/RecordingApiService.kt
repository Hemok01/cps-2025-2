package com.example.mobilegpt.data.remote.api

import com.example.mobilegpt.data.remote.dto.request.BulkUpdateSubtasksRequest
import com.example.mobilegpt.data.remote.dto.request.ConvertToTaskRequest
import com.example.mobilegpt.data.remote.dto.request.SaveEventsBatchRequest
import com.example.mobilegpt.data.remote.dto.request.StartRecordingRequest
import com.example.mobilegpt.data.remote.dto.request.UpdateSubtaskRequest
import com.example.mobilegpt.data.remote.dto.response.AnalyzeResponse
import com.example.mobilegpt.data.remote.dto.response.BulkUpdateResponse
import com.example.mobilegpt.data.remote.dto.response.ConvertToTaskResponse
import com.example.mobilegpt.data.remote.dto.response.RecordingEventResponse
import com.example.mobilegpt.data.remote.dto.response.RecordingListResponse
import com.example.mobilegpt.data.remote.dto.response.RecordingResponse
import com.example.mobilegpt.data.remote.dto.response.SubtaskResponse
import com.example.mobilegpt.data.remote.dto.response.RecordingSubtasksResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface RecordingApiService {

    // ===== 녹화(Recording) 관련 =====

    /**
     * 녹화 목록 조회
     * GET /api/recordings/
     */
    @GET("/api/recordings/")
    suspend fun getRecordings(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): Response<RecordingListResponse>

    /**
     * 녹화 시작
     * POST /api/recordings/
     */
    @POST("/api/recordings/")
    suspend fun startRecording(
        @Body request: StartRecordingRequest
    ): Response<RecordingResponse>

    /**
     * 녹화 상세 조회
     * GET /api/recordings/{id}/
     */
    @GET("/api/recordings/{id}/")
    suspend fun getRecording(
        @Path("id") recordingId: Long
    ): Response<RecordingResponse>

    /**
     * 이벤트 배치 저장
     * POST /api/recordings/{id}/save-events-batch/
     */
    @POST("/api/recordings/{id}/save-events-batch/")
    suspend fun saveEventsBatch(
        @Path("id") recordingId: Long,
        @Body request: SaveEventsBatchRequest
    ): Response<Unit>

    /**
     * 녹화 종료
     * POST /api/recordings/{id}/stop/
     */
    @POST("/api/recordings/{id}/stop/")
    suspend fun stopRecording(
        @Path("id") recordingId: Long
    ): Response<RecordingResponse>

    /**
     * 녹화 이벤트 목록 조회
     * GET /api/recordings/{id}/events/
     */
    @GET("/api/recordings/{id}/events/")
    suspend fun getRecordingEvents(
        @Path("id") recordingId: Long
    ): Response<List<RecordingEventResponse>>

    /**
     * 녹화 삭제
     * DELETE /api/recordings/{id}/
     */
    @DELETE("/api/recordings/{id}/")
    suspend fun deleteRecording(
        @Path("id") recordingId: Long
    ): Response<Unit>

    // ===== 녹화 분석 및 변환 =====

    /**
     * 녹화 분석 (GPT로 단계 추출)
     * POST /api/recordings/{id}/analyze/
     */
    @POST("/api/recordings/{id}/analyze/")
    suspend fun analyzeRecording(
        @Path("id") recordingId: Long
    ): Response<AnalyzeResponse>

    /**
     * 분석 상태 조회
     * GET /api/recordings/{id}/analysis-status/
     */
    @GET("/api/recordings/{id}/analysis-status/")
    suspend fun getAnalysisStatus(
        @Path("id") recordingId: Long
    ): Response<RecordingResponse>

    /**
     * 녹화를 과제(Task)로 변환
     * POST /api/recordings/{id}/convert-to-task/
     */
    @POST("/api/recordings/{id}/convert-to-task/")
    suspend fun convertToTask(
        @Path("id") recordingId: Long,
        @Body request: ConvertToTaskRequest
    ): Response<ConvertToTaskResponse>

    // ===== 단계(Subtask) 관련 =====

    /**
     * 단계 목록 조회 (녹화 기반)
     * 녹화가 과제로 변환된 경우 해당 과제의 Subtask 반환
     * 변환되지 않은 경우 안내 메시지 + 빈 배열 반환
     */
    @GET("/api/recordings/{id}/subtasks/")
    suspend fun getSubtasksByRecording(
        @Path("id") recordingId: Long
    ): Response<RecordingSubtasksResponse>

    /**
     * 단계 수정
     * PUT /api/tasks/subtasks/{id}/
     */
    @PUT("/api/tasks/subtasks/{id}/")
    suspend fun updateSubtask(
        @Path("id") subtaskId: Long,
        @Body request: UpdateSubtaskRequest
    ): Response<SubtaskResponse>

    /**
     * 단계 삭제
     * DELETE /api/tasks/subtasks/{id}/
     */
    @DELETE("/api/tasks/subtasks/{id}/")
    suspend fun deleteSubtask(
        @Path("id") subtaskId: Long
    ): Response<Unit>

    /**
     * 단계 일괄 업데이트
     * PUT /api/tasks/{task_id}/subtasks/bulk/
     */
    @PUT("/api/tasks/{task_id}/subtasks/bulk/")
    suspend fun bulkUpdateSubtasks(
        @Path("task_id") taskId: Long,
        @Body request: BulkUpdateSubtasksRequest
    ): Response<BulkUpdateResponse>
}
