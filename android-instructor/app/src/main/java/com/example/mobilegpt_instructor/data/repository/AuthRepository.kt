package com.example.mobilegpt_instructor.data.repository

import com.example.mobilegpt_instructor.data.api.AuthApi
import com.example.mobilegpt_instructor.data.local.TokenManager
import com.example.mobilegpt_instructor.data.model.LoginRequest
import com.example.mobilegpt_instructor.data.model.UserResponse
import com.example.mobilegpt_instructor.data.network.NetworkModule
import kotlinx.coroutines.flow.Flow

/**
 * 인증 관련 데이터 처리를 담당하는 Repository
 * API 호출과 로컬 토큰 저장을 모두 처리
 */
class AuthRepository {

    private val api: AuthApi = NetworkModule.getAuthApi()
    private val tokenManager: TokenManager = NetworkModule.getTokenManager()

    // 로그인 상태 Flow
    val isLoggedIn: Flow<Boolean> = tokenManager.isLoggedIn

    // 로그인
    suspend fun login(email: String, password: String): Result<UserResponse> {
        return try {
            val response = api.login(LoginRequest(email, password))

            if (response.isSuccessful) {
                val body = response.body()!!
                // 토큰 저장
                tokenManager.saveTokens(body.access, body.refresh)
                Result.success(body.user)
            } else {
                val errorMessage = when (response.code()) {
                    401 -> "이메일 또는 비밀번호가 올바르지 않습니다"
                    else -> "로그인에 실패했습니다 (${response.code()})"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("네트워크 오류: ${e.message}"))
        }
    }

    // 로그아웃
    suspend fun logout(): Result<Unit> {
        return try {
            api.logout()
            tokenManager.clearTokens()
            Result.success(Unit)
        } catch (e: Exception) {
            // 서버 로그아웃 실패해도 로컬 토큰은 삭제
            tokenManager.clearTokens()
            Result.success(Unit)
        }
    }

    // 현재 사용자 정보 조회
    suspend fun getCurrentUser(): Result<UserResponse> {
        return try {
            val response = api.getCurrentUser()

            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("사용자 정보를 가져올 수 없습니다"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("네트워크 오류: ${e.message}"))
        }
    }
}
