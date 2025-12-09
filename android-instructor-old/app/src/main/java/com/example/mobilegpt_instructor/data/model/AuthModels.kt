package com.example.mobilegpt_instructor.data.model

import com.google.gson.annotations.SerializedName

// Request Models
data class LoginRequest(
    val email: String,
    val password: String
)

// Response Models
data class LoginResponse(
    val access: String,
    val refresh: String,
    val user: UserResponse
)

data class UserResponse(
    val id: Int,
    val email: String,
    val username: String,
    @SerializedName("first_name")
    val firstName: String,
    @SerializedName("last_name")
    val lastName: String,
    val role: String  // "instructor" 또는 "student"
)

// Token Storage
data class AuthTokens(
    val accessToken: String,
    val refreshToken: String
)
