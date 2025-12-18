package com.example.mobilegpt

object Constants {
    // BuildConfig에서 서버 설정을 가져옴
    // local.properties의 server.url, server.host 값을 사용
    val BASE_URL: String = BuildConfig.SERVER_URL
    val SERVER_HOST: String = BuildConfig.SERVER_HOST

    // API 엔드포인트 (Django 서버용)
    object Endpoints {
        // Auth
        const val LOGIN = "/api/auth/login/"
        const val REFRESH = "/api/auth/refresh/"
        const val LOGOUT = "/api/auth/logout/"
        const val ME = "/api/auth/me/"

        // Recordings (녹화)
        const val RECORDINGS = "/api/recordings/"

        // Subtasks (단계)
        const val SUBTASKS = "/api/tasks/subtasks/"

        // Legacy (Flask 서버 호환용 - 추후 삭제 예정)
        const val RECORD_EVENT = "/api/record_event"
        const val SAVE_SESSION = "/api/save_session"
        const val ANALYZE_SESSION = "/api/analyze_session"
    }

    fun getFullUrl(endpoint: String): String {
        return "$BASE_URL$endpoint"
    }
}
