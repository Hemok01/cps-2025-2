package com.example.mobilegpt.network

import retrofit2.Call
import retrofit2.http.*
import okhttp3.RequestBody

interface ApiService {

    @GET("/api/list_sessions")
    fun listSessions(): Call<SessionListResponse>

    @GET("/api/get_steps/{id}")
    fun getSteps(@Path("id") sessionId: String): Call<StepListResponse>

    @POST("/api/update_steps")
    fun updateSteps(@Body body: RequestBody): Call<BasicResponse>

    @POST("/api/update_step")
    fun updateStep(@Body body: RequestBody): Call<BasicResponse>

    @POST("/api/delete_step")
    fun deleteStep(@Body body: RequestBody): Call<BasicResponse>
}
