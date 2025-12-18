package com.example.mobilegpt_instructor.data.api

import com.example.mobilegpt_instructor.data.model.LoginRequest
import com.example.mobilegpt_instructor.data.model.LoginResponse
import com.example.mobilegpt_instructor.data.model.UserResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {

    @POST("auth/login/")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/logout/")
    suspend fun logout(): Response<Unit>

    @GET("auth/me/")
    suspend fun getCurrentUser(): Response<UserResponse>
}
