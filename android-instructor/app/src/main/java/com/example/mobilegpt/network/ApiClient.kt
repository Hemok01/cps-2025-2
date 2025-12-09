package com.example.mobilegpt.network

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import com.example.mobilegpt.BuildConfig
import com.example.mobilegpt.Constants
import com.example.mobilegpt.data.local.TokenManager
import com.example.mobilegpt.data.remote.AuthInterceptor
import com.example.mobilegpt.data.remote.TokenAuthenticator
import com.example.mobilegpt.data.remote.api.AuthApiService
import com.example.mobilegpt.data.remote.api.RecordingApiService

/**
 * Retrofit API 클라이언트
 * JWT 인증 및 토큰 자동 갱신 지원
 */
object ApiClient {

    private var tokenManager: TokenManager? = null
    private var retrofit: Retrofit? = null

    /**
     * 앱 시작 시 Context로 초기화 필수
     */
    fun initialize(context: Context) {
        tokenManager = TokenManager.getInstance(context)
        retrofit = null // 재초기화
    }

    private fun getRetrofit(): Retrofit {
        return retrofit ?: synchronized(this) {
            retrofit ?: buildRetrofit().also { retrofit = it }
        }
    }

    private fun buildRetrofit(): Retrofit {
        val tm = tokenManager ?: throw IllegalStateException(
            "ApiClient not initialized. Call ApiClient.initialize(context) first."
        )

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(tm))
            .addInterceptor(loggingInterceptor)
            .authenticator(TokenAuthenticator(tm))
            .retryOnConnectionFailure(true)
            .build()

        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // ===== API Services =====

    val authApi: AuthApiService by lazy {
        getRetrofit().create(AuthApiService::class.java)
    }

    val recordingApi: RecordingApiService by lazy {
        getRetrofit().create(RecordingApiService::class.java)
    }

    // TokenManager 접근 (로그인 상태 확인 등)
    fun getTokenManager(): TokenManager {
        return tokenManager ?: throw IllegalStateException(
            "ApiClient not initialized. Call ApiClient.initialize(context) first."
        )
    }

    // ===== Legacy API (기존 Flask 호환용 - 추후 삭제 예정) =====

    @Deprecated("Use authApi, recordingApi instead")
    val api: ApiService by lazy {
        getRetrofit().create(ApiService::class.java)
    }
}
