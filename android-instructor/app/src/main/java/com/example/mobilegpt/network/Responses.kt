package com.example.mobilegpt.network

data class SessionListResponse(val sessions: List<String>)
data class StepListResponse(val steps: List<Map<String, Any?>>)
data class BasicResponse(val status: String?, val error: String?)
