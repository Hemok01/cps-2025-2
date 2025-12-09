package com.example.mobilegpt.data.remote.dto.response

/**
 * Django 서버의 표준 응답 래퍼
 */
data class ApiResponse<T>(
    val data: T?,
    val message: String?,
    val status: String?
)
