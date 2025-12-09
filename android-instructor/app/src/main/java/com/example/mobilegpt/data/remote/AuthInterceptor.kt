package com.example.mobilegpt.data.remote

import com.example.mobilegpt.data.local.TokenManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 모든 API 요청에 JWT 토큰을 자동으로 추가하는 인터셉터
 * 로그인/회원가입 등 인증이 필요 없는 경로는 제외
 */
class AuthInterceptor(
    private val tokenManager: TokenManager
) : Interceptor {

    companion object {
        // 인증이 필요 없는 경로 목록
        private val EXCLUDED_PATHS = listOf(
            "/api/auth/login/",
            "/api/auth/register/",
            "/api/auth/refresh/",
            "/api/health/"
        )
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val path = originalRequest.url.encodedPath

        // 인증 제외 경로인 경우 토큰 추가 없이 진행
        if (EXCLUDED_PATHS.any { path.contains(it) }) {
            return chain.proceed(originalRequest)
        }

        // 토큰이 없으면 원래 요청 그대로 진행
        val token = tokenManager.accessToken
        if (token.isNullOrEmpty()) {
            return chain.proceed(originalRequest)
        }

        // Authorization 헤더 추가
        val newRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        return chain.proceed(newRequest)
    }
}
