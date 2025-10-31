package com.mobilegpt.student.data.api

import retrofit2.Response
import retrofit2.http.*

/**
 * Authentication API
 */
interface AuthApi {

    /**
     * 로그인
     */
    @POST("auth/login/")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    /**
     * 회원가입
     */
    @POST("auth/register/")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<LoginResponse>

    /**
     * 내 정보 조회
     */
    @GET("auth/me/")
    suspend fun getMe(): Response<UserResponse>
}

/**
 * Request/Response Models
 */
data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val password_confirm: String,
    val name: String,
    val role: String = "STUDENT"
)

data class LoginResponse(
    val access: String,
    val refresh: String
)

data class UserResponse(
    val id: Int,
    val email: String,
    val name: String,
    val role: String
)
