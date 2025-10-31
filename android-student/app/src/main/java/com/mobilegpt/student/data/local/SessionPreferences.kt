package com.mobilegpt.student.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Session Preferences Helper
 * 세션 정보를 SharedPreferences에 저장/조회
 */
@Singleton
class SessionPreferences @Inject constructor(
    @ApplicationContext context: Context
) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "mobilegpt_session"
        private const val KEY_SESSION_ID = "session_id"
        private const val KEY_SUBTASK_ID = "subtask_id"
    }

    /**
     * 세션 ID 저장
     */
    fun setSessionId(sessionId: Int) {
        prefs.edit().putInt(KEY_SESSION_ID, sessionId).apply()
    }

    /**
     * 세션 ID 가져오기
     */
    fun getSessionId(): Int? {
        val id = prefs.getInt(KEY_SESSION_ID, -1)
        return if (id == -1) null else id
    }

    /**
     * 서브태스크 ID 저장
     */
    fun setSubtaskId(subtaskId: Int) {
        prefs.edit().putInt(KEY_SUBTASK_ID, subtaskId).apply()
    }

    /**
     * 서브태스크 ID 가져오기
     */
    fun getSubtaskId(): Int? {
        val id = prefs.getInt(KEY_SUBTASK_ID, -1)
        return if (id == -1) null else id
    }

    /**
     * 세션 정보 초기화
     */
    fun clear() {
        prefs.edit().clear().apply()
    }
}
