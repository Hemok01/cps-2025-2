package com.example.mobilegpt.data.remote.dto.request

import com.google.gson.annotations.SerializedName

/**
 * 녹화를 과제(Task)로 변환 요청
 */
data class ConvertToTaskRequest(
    @SerializedName("title")
    val title: String,

    @SerializedName("description")
    val description: String = ""
)
