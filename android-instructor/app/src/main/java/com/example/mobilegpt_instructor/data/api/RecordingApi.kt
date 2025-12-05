package com.example.mobilegpt_instructor.data.api

import com.example.mobilegpt_instructor.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface RecordingApi {

    // 녹화 세션 생성
    @POST("sessions/recordings/")
    suspend fun createRecording(@Body request: CreateRecordingRequest): Response<RecordingResponse>

    // 녹화 목록 조회
    @GET("sessions/recordings/")
    suspend fun getRecordings(): Response<List<RecordingResponse>>

    // 녹화 상세 조회
    @GET("sessions/recordings/{id}/")
    suspend fun getRecording(@Path("id") id: Int): Response<RecordingResponse>

    // 녹화 시작
    @POST("sessions/recordings/{id}/start/")
    suspend fun startRecording(@Path("id") id: Int): Response<RecordingResponse>

    // 녹화 중지
    @POST("sessions/recordings/{id}/stop/")
    suspend fun stopRecording(@Path("id") id: Int): Response<RecordingResponse>

    // 이벤트 배치 저장
    @POST("sessions/recordings/{id}/save_events_batch/")
    suspend fun saveEventsBatch(
        @Path("id") id: Int,
        @Body request: SaveEventsRequest
    ): Response<SaveEventsResponse>

    // GPT 분석 시작 (비동기)
    @POST("sessions/recordings/{id}/analyze/")
    suspend fun analyzeRecording(@Path("id") id: Int): Response<AnalyzeResponse>

    // 분석 상태/결과 조회
    @GET("sessions/recordings/{id}/analysis_status/")
    suspend fun getAnalysisStatus(@Path("id") id: Int): Response<AnalysisStatusResponse>

    // 분석된 단계 수정
    @PUT("sessions/recordings/{id}/update_steps/")
    suspend fun updateSteps(
        @Path("id") id: Int,
        @Body request: UpdateStepsRequest
    ): Response<RecordingResponse>

    // 강의로 변환
    @POST("sessions/recordings/{id}/convert_to_lecture/")
    suspend fun convertToLecture(
        @Path("id") id: Int,
        @Body request: ConvertToLectureRequest
    ): Response<ConvertToLectureResponse>
}
