package com.example.mobilegpt.data.remote

import android.util.Log
import com.example.mobilegpt.Constants
import com.example.mobilegpt.data.local.TokenManager
import com.google.gson.Gson
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route

/**
 * 401 응답 시 자동으로 토큰을 갱신하는 Authenticator
 * refresh 토큰을 사용하여 새로운 access 토큰을 발급받음
 */
class TokenAuthenticator(
    private val tokenManager: TokenManager
) : Authenticator {

    companion object {
        private const val TAG = "TokenAuthenticator"
    }

    // 토큰 갱신 요청용 클래스
    private data class RefreshRequest(val refresh: String)
    private data class RefreshResponse(val access: String)

    override fun authenticate(route: Route?, response: Response): Request? {
        // refresh 토큰이 없으면 재인증 불가
        val refreshToken = tokenManager.refreshToken
        if (refreshToken.isNullOrEmpty()) {
            Log.w(TAG, "No refresh token available")
            tokenManager.clearTokens()
            return null
        }

        // 이미 토큰 갱신을 시도한 요청이면 무한 루프 방지를 위해 null 반환
        if (response.request.header("X-Retry-Auth") != null) {
            Log.w(TAG, "Token refresh already attempted, giving up")
            tokenManager.clearTokens()
            return null
        }

        // 토큰 갱신 시도
        synchronized(this) {
            // 다른 스레드에서 이미 갱신했는지 확인
            val currentToken = tokenManager.accessToken
            val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")

            if (currentToken != null && currentToken != requestToken) {
                // 이미 새 토큰이 있으므로 재시도
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .header("X-Retry-Auth", "true")
                    .build()
            }

            // 토큰 갱신 요청
            val newAccessToken = refreshAccessToken(refreshToken)
            if (newAccessToken != null) {
                tokenManager.accessToken = newAccessToken
                Log.d(TAG, "Token refreshed successfully")

                return response.request.newBuilder()
                    .header("Authorization", "Bearer $newAccessToken")
                    .header("X-Retry-Auth", "true")
                    .build()
            }

            // 갱신 실패
            Log.w(TAG, "Token refresh failed")
            tokenManager.clearTokens()
            return null
        }
    }

    private fun refreshAccessToken(refreshToken: String): String? {
        return try {
            val client = OkHttpClient.Builder().build()
            val gson = Gson()

            val requestBody = gson.toJson(RefreshRequest(refreshToken))
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("${Constants.BASE_URL}${Constants.Endpoints.REFRESH}")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val body = response.body?.string()
                val refreshResponse = gson.fromJson(body, RefreshResponse::class.java)
                refreshResponse?.access
            } else {
                Log.e(TAG, "Refresh request failed: ${response.code}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing token", e)
            null
        }
    }
}
