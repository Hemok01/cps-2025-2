package com.example.mobilegpt.data.remote.dto.response

/**
 * 로그인/토큰 갱신 응답
 */
data class AuthResponse(
    val access: String,
    val refresh: String? = null
)

/**
 * 사용자 정보 응답
 */
data class UserResponse(
    val id: Long,
    val email: String,
    val name: String,
    val role: String,
    val phone: String? = null
)
