package com.example.mobilegpt.data.remote.dto.response

import com.google.gson.annotations.SerializedName

/**
 * 녹화 → 과제 변환 응답
 */
data class ConvertToTaskResponse(
    @SerializedName("message")
    val message: String,

    @SerializedName("recording_id")
    val recordingId: Long,

    @SerializedName("title")
    val title: String
)
