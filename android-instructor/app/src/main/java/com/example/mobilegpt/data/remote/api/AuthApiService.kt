package com.example.mobilegpt.data.remote.api

import com.example.mobilegpt.data.remote.dto.request.LoginRequest
import com.example.mobilegpt.data.remote.dto.response.AuthResponse
import com.example.mobilegpt.data.remote.dto.response.UserResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApiService {

    /**
     * 로그인
     * POST /api/auth/login/
     */
    @POST("/api/auth/login/")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    /**
     * 토큰 갱신
     * POST /api/auth/refresh/
     */
    @POST("/api/auth/refresh/")
    suspend fun refreshToken(@Body request: Map<String, String>): Response<AuthResponse>

    /**
     * 로그아웃
     * POST /api/auth/logout/
     */
    @POST("/api/auth/logout/")
    suspend fun logout(@Body request: Map<String, String>? = null): Response<Unit>

    /**
     * 내 정보 조회
     * GET /api/auth/me/
     */
    @GET("/api/auth/me/")
    suspend fun getMe(): Response<UserResponse>
}
