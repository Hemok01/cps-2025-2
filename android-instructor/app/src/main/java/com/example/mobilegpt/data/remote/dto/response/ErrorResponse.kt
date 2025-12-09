package com.example.mobilegpt.data.remote.dto.response

import com.google.gson.annotations.SerializedName

/**
 * Django 서버의 에러 응답 형식
 */
data class ErrorResponse(
    val error: ErrorDetail
)

data class ErrorDetail(
    val code: String,
    val message: String,
    @SerializedName("field_errors")
    val fieldErrors: Map<String, List<String>>? = null
)
