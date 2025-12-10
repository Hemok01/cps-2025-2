package com.example.mobilegpt.data.remote.dto.response

import com.google.gson.annotations.SerializedName

/**
 * 녹화 분석 응답
 */
data class AnalyzeResponse(
    @SerializedName("message")
    val message: String,

    @SerializedName("recording_id")
    val recordingId: Long,

    @SerializedName("status")
    val status: String  // PROCESSING, ANALYZED, FAILED
)
