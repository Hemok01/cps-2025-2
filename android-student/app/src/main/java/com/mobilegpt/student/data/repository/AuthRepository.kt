package com.mobilegpt.student.data.repository

import com.mobilegpt.student.data.api.AuthApi
import com.mobilegpt.student.data.api.LoginRequest
import com.mobilegpt.student.data.api.LoginResponse
import com.mobilegpt.student.data.api.UserResponse
import com.mobilegpt.student.data.local.TokenPreferences
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Authentication Repository
 * 인증 관련 데이터 관리
 */
@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val tokenPreferences: TokenPreferences
) {

    /**
     * 로그인
     */
    suspend fun login(email: String, password: String): Result<LoginResponse> {
        return try {
            val response = authApi.login(LoginRequest(email, password))
            if (response.isSuccessful && response.body() != null) {
                val loginResponse = response.body()!!

                // 토큰 저장
                tokenPreferences.setAccessToken(loginResponse.access)
                tokenPreferences.setRefreshToken(loginResponse.refresh)

                // 사용자 정보는 /api/auth/me/ 에서 가져와서 저장
                val userResult = getCurrentUser()
                if (userResult.isSuccess) {
                    val user = userResult.getOrNull()!!
                    tokenPreferences.setUserInfo(user.id, user.name, user.email)
                }

                Result.success(loginResponse)
            } else {
                Result.failure(Exception("로그인 실패: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 로그아웃
     */
    fun logout() {
        tokenPreferences.clear()
    }

    /**
     * 로그인 여부 확인
     */
    fun isLoggedIn(): Boolean {
        return tokenPreferences.isLoggedIn()
    }

    /**
     * 현재 사용자 정보 조회
     */
    suspend fun getCurrentUser(): Result<UserResponse> {
        return try {
            val response = authApi.getMe()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("사용자 정보 조회 실패: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 저장된 사용자 이름 가져오기
     */
    fun getUserName(): String {
        return tokenPreferences.getUserName() ?: "사용자"
    }
}
