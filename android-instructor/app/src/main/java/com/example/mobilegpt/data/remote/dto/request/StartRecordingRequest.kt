package com.example.mobilegpt.data.remote.dto.request

data class StartRecordingRequest(
    val title: String,
    val description: String? = null
)
