package com.mobilegpt.student.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.mobilegpt.student.BuildConfig
import com.mobilegpt.student.data.api.AuthApi
import com.mobilegpt.student.data.api.ScreenshotApi
import com.mobilegpt.student.data.api.StudentApi
import com.mobilegpt.student.data.local.TokenPreferences
import com.mobilegpt.student.data.websocket.WebSocketManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton
/**
 * Network Module for Hilt DI
 * REST API 및 WebSocket 제공
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .create()
    }

    /**
     * 인증이 필요 없는 엔드포인트 목록
     * 이 경로들은 Authorization 헤더를 첨부하지 않습니다.
     */
    private val NO_AUTH_ENDPOINTS = listOf(
        "sessions/join/",               // 익명 세션 참가
        "logs/activity/anonymous/",     // 익명 Activity Log
        "screenshots/upload/",          // 익명 스크린샷 업로드
        "auth/register/",               // 회원가입
        "auth/login/",                  // 로그인
        "auth/token/"                   // 토큰 발급
    )

    /**
     * 인증 인터셉터
     * 익명 엔드포인트는 Authorization 헤더를 첨부하지 않습니다.
     */
    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenPreferences: TokenPreferences): Interceptor {
        return Interceptor { chain ->
            val request = chain.request()
            val path = request.url.encodedPath

            // 인증이 필요 없는 엔드포인트인지 확인
            val isNoAuthEndpoint = NO_AUTH_ENDPOINTS.any { endpoint ->
                path.contains(endpoint)
            }

            val newRequest = if (!isNoAuthEndpoint) {
                // 인증이 필요한 엔드포인트: 토큰이 있으면 첨부
                val token = tokenPreferences.getAccessToken()
                if (token != null) {
                    request.newBuilder()
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                } else {
                    request
                }
            } else {
                // 익명 엔드포인트: 토큰을 첨부하지 않음
                request
            }

            chain.proceed(newRequest)
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: Interceptor
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi {
        return retrofit.create(AuthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideStudentApi(retrofit: Retrofit): StudentApi {
        return retrofit.create(StudentApi::class.java)
    }

    @Provides
    @Singleton
    fun provideScreenshotApi(retrofit: Retrofit): ScreenshotApi {
        return retrofit.create(ScreenshotApi::class.java)
    }

    /**
     * WebSocket Manager 제공
     * 동적으로 세션 코드에 맞는 WebSocket 연결을 관리합니다.
     */
    @Provides
    @Singleton
    fun provideWebSocketManager(
        okHttpClient: OkHttpClient,
        gson: Gson
    ): WebSocketManager {
        return WebSocketManager(okHttpClient, gson)
    }
}

/**
 * Qualifiers for different instances
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthInterceptorOkHttpClient
