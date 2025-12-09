package com.example.mobilegpt_instructor.data.repository

import com.example.mobilegpt_instructor.data.api.RecordingApi
import com.example.mobilegpt_instructor.data.model.*
import com.example.mobilegpt_instructor.data.network.NetworkModule

/**
 * 녹화 세션 관련 데이터 처리를 담당하는 Repository
 * 녹화 생성, 이벤트 저장, GPT 분석, 강의 변환 등의 기능 제공
 */
class RecordingRepository {

    private val api: RecordingApi = NetworkModule.getRecordingApi()

    // ============ 녹화 세션 관리 ============

    // 녹화 세션 생성
    suspend fun createRecording(title: String, description: String = ""): Result<RecordingResponse> {
        return safeApiCall {
            api.createRecording(CreateRecordingRequest(title, description))
        }
    }

    // 녹화 목록 조회 (페이지네이션에서 results 추출)
    suspend fun getRecordings(): Result<List<RecordingListItem>> {
        return try {
            val response = api.getRecordings()
            if (response.isSuccessful) {
                val paginatedResponse = response.body()!!
                Result.success(paginatedResponse.results)
            } else {
                Result.failure(Exception("녹화 목록 조회 실패 (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("네트워크 오류: ${e.message}"))
        }
    }

    // 녹화 상세 조회
    suspend fun getRecording(id: Int): Result<RecordingResponse> {
        return safeApiCall { api.getRecording(id) }
    }

    // 녹화 중지
    suspend fun stopRecording(id: Int): Result<RecordingResponse> {
        return safeApiCall { api.stopRecording(id) }
    }

    // ============ 이벤트 저장 ============

    // 이벤트 배치 저장
    suspend fun saveEvents(
        recordingId: Int,
        events: List<AccessibilityEventData>
    ): Result<SaveEventsResponse> {
        return safeApiCall {
            api.saveEventsBatch(recordingId, SaveEventsRequest(events))
        }
    }

    // ============ GPT 분석 ============

    // GPT 분석 시작 (비동기)
    suspend fun analyzeRecording(id: Int): Result<AnalyzeResponse> {
        return safeApiCall { api.analyzeRecording(id) }
    }

    // 분석 상태/결과 조회
    suspend fun getAnalysisStatus(id: Int): Result<AnalysisStatusResponse> {
        return safeApiCall { api.getAnalysisStatus(id) }
    }

    // 분석된 단계 수정
    suspend fun updateSteps(id: Int, steps: List<AnalyzedStep>): Result<RecordingResponse> {
        return safeApiCall {
            api.updateSteps(id, UpdateStepsRequest(steps))
        }
    }

    // ============ 강의 변환 ============

    // 강의로 변환
    suspend fun convertToLecture(
        recordingId: Int,
        title: String,
        description: String = ""
    ): Result<ConvertToLectureResponse> {
        return safeApiCall {
            api.convertToLecture(recordingId, ConvertToLectureRequest(title, description))
        }
    }

    // ============ Helper ============

    private suspend fun <T> safeApiCall(
        apiCall: suspend () -> retrofit2.Response<T>
    ): Result<T> {
        return try {
            val response = apiCall()

            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = when (response.code()) {
                    400 -> "잘못된 요청입니다"
                    401 -> "인증이 필요합니다"
                    403 -> "권한이 없습니다"
                    404 -> "리소스를 찾을 수 없습니다"
                    500 -> "서버 오류가 발생했습니다"
                    else -> "요청 실패 (${response.code()})"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("네트워크 오류: ${e.message}"))
        }
    }
}
