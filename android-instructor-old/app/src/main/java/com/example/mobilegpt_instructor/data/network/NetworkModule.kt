package com.example.mobilegpt_instructor.data.network

import android.content.Context
import com.mobilegpt.instructor.BuildConfig
import com.example.mobilegpt_instructor.data.api.AuthApi
import com.example.mobilegpt_instructor.data.api.RecordingApi
import com.example.mobilegpt_instructor.data.local.TokenManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 네트워크 모듈 - Retrofit 및 API 인스턴스 제공
 * Singleton 패턴으로 구현하여 앱 전체에서 동일한 인스턴스 사용
 */
object NetworkModule {

    private var retrofit: Retrofit? = null
    private var tokenManager: TokenManager? = null

    fun initialize(context: Context) {
        tokenManager = TokenManager(context)
        retrofit = createRetrofit()
    }

    private fun createRetrofit(): Retrofit {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val authInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val token = tokenManager?.getAccessToken()

            val newRequest = if (token != null) {
                originalRequest.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                originalRequest
            }

            chain.proceed(newRequest)
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun getAuthApi(): AuthApi {
        return retrofit?.create(AuthApi::class.java)
            ?: throw IllegalStateException("NetworkModule not initialized")
    }

    fun getRecordingApi(): RecordingApi {
        return retrofit?.create(RecordingApi::class.java)
            ?: throw IllegalStateException("NetworkModule not initialized")
    }

    fun getTokenManager(): TokenManager {
        return tokenManager
            ?: throw IllegalStateException("NetworkModule not initialized")
    }
}
