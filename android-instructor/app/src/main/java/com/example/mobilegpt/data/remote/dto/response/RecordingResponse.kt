package com.example.mobilegpt.data.remote.dto.response

import com.google.gson.annotations.SerializedName

/**
 * 녹화 정보 응답
 * 백엔드 RecordingSessionSerializer와 일치
 */
data class RecordingResponse(
    val id: Long,
    val title: String,
    val description: String? = null,
    val status: String,  // "RECORDING", "COMPLETED", "PROCESSING", "ANALYZED", "FAILED"
    @SerializedName("event_count")
    val eventCount: Int? = null,
    @SerializedName("duration_seconds")
    val durationSeconds: Int? = null,
    @SerializedName("started_at")
    val startedAt: String? = null,
    @SerializedName("ended_at")
    val endedAt: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null,
    // 강의자 정보 (선택적)
    val instructor: InstructorRef? = null,
    // 변환된 과제 정보 (변환 완료 시에만 존재)
    val task: TaskRef? = null,
    // 연결된 강의 정보 (선택적)
    val lecture: LectureRef? = null
)

/**
 * 강의자 참조 정보
 */
data class InstructorRef(
    val id: Long,
    val email: String? = null,
    val name: String? = null
)

/**
 * 과제 참조 정보 (녹화에서 변환된 경우)
 */
data class TaskRef(
    val id: Long,
    val title: String? = null,
    @SerializedName("subtask_count")
    val subtaskCount: Int? = null
)

/**
 * 강의 참조 정보 (선택적)
 */
data class LectureRef(
    val id: Long,
    val title: String? = null,
    val description: String? = null
)

/**
 * 녹화 목록 응답
 */
data class RecordingListResponse(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<RecordingResponse>
)

/**
 * 녹화 이벤트 응답
 */
data class RecordingEventResponse(
    val id: Long,
    @SerializedName("event_type")
    val eventType: String,
    val timestamp: String,
    @SerializedName("event_data")
    val eventData: Map<String, Any?>
)
