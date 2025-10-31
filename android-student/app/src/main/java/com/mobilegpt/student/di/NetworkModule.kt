package com.mobilegpt.student.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.mobilegpt.student.BuildConfig
import com.mobilegpt.student.data.api.AuthApi
import com.mobilegpt.student.data.api.StudentApi
import com.mobilegpt.student.data.api.WebSocketApi
import com.mobilegpt.student.data.local.TokenPreferences
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.messageadapter.gson.GsonMessageAdapter
import com.tinder.scarlet.retry.LinearBackoffStrategy
import com.tinder.scarlet.websocket.okhttp.newWebSocketFactory
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
     * 인증 인터셉터
     */
    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenPreferences: TokenPreferences): Interceptor {
        return Interceptor { chain ->
            val request = chain.request()
            val token = tokenPreferences.getAccessToken()

            val newRequest = if (token != null) {
                request.newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            } else {
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
    fun provideWebSocketApi(
        okHttpClient: OkHttpClient,
        gson: Gson
    ): WebSocketApi {
        // WebSocket URL은 동적으로 설정될 수 있도록 수정 필요
        // 현재는 임시 URL 사용
        val baseWsUrl = BuildConfig.WS_BASE_URL

        val scarlet = Scarlet.Builder()
            .webSocketFactory(okHttpClient.newWebSocketFactory("$baseWsUrl/session/test/"))
            .addMessageAdapterFactory(GsonMessageAdapter.Factory(gson))
            //.addStreamAdapterFactory(CoroutinesStreamAdapterFactory())  // 이 줄
            .backoffStrategy(LinearBackoffStrategy(5000))
            .build()
        return scarlet.create(WebSocketApi::class.java)
    }
}

/**
 * Qualifiers for different instances
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthInterceptorOkHttpClient
