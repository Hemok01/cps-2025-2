package com.example.mobilegpt.data.remote.dto.response

import com.google.gson.annotations.SerializedName

/**
 * 녹화 정보 응답
 */
data class RecordingResponse(
    val id: Long,
    val title: String,
    val description: String? = null,
    val status: String,  // "RECORDING", "COMPLETED"
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("event_count")
    val eventCount: Int? = null
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
