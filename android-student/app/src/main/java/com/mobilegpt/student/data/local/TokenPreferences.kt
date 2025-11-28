package com.mobilegpt.student.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Token Preferences Helper
 * JWT 토큰을 SharedPreferences에 저장/조회
 */
@Singleton
class TokenPreferences @Inject constructor(
    @ApplicationContext context: Context
) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "mobilegpt_token"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        // 간편 등록용
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_IS_REGISTERED = "is_registered"
    }

    /**
     * 액세스 토큰 저장
     */
    fun setAccessToken(token: String) {
        prefs.edit().putString(KEY_ACCESS_TOKEN, token).apply()
    }

    /**
     * 액세스 토큰 가져오기
     */
    fun getAccessToken(): String? {
        return prefs.getString(KEY_ACCESS_TOKEN, null)
    }

    /**
     * 리프레시 토큰 저장
     */
    fun setRefreshToken(token: String) {
        prefs.edit().putString(KEY_REFRESH_TOKEN, token).apply()
    }

    /**
     * 리프레시 토큰 가져오기
     */
    fun getRefreshToken(): String? {
        return prefs.getString(KEY_REFRESH_TOKEN, null)
    }

    /**
     * 사용자 정보 저장
     */
    fun setUserInfo(userId: Int, userName: String, userEmail: String) {
        prefs.edit().apply {
            putInt(KEY_USER_ID, userId)
            putString(KEY_USER_NAME, userName)
            putString(KEY_USER_EMAIL, userEmail)
            apply()
        }
    }

    /**
     * 사용자 ID 가져오기
     */
    fun getUserId(): Int {
        return prefs.getInt(KEY_USER_ID, -1)
    }

    /**
     * 사용자 이름 가져오기
     */
    fun getUserName(): String? {
        return prefs.getString(KEY_USER_NAME, null)
    }

    /**
     * 로그인 여부 확인
     */
    fun isLoggedIn(): Boolean {
        return getAccessToken() != null
    }

    // ==================== 간편 등록 관련 ====================

    /**
     * 간편 등록 정보 저장
     * @param deviceId 기기 고유값
     * @param displayName 표시 이름
     */
    fun setSimpleRegister(deviceId: String, displayName: String) {
        prefs.edit().apply {
            putString(KEY_DEVICE_ID, deviceId)
            putString(KEY_DISPLAY_NAME, displayName)
            putBoolean(KEY_IS_REGISTERED, true)
            apply()
        }
    }

    /**
     * 기기 ID 가져오기
     */
    fun getDeviceId(): String? {
        return prefs.getString(KEY_DEVICE_ID, null)
    }

    /**
     * 표시 이름 가져오기
     */
    fun getDisplayName(): String? {
        return prefs.getString(KEY_DISPLAY_NAME, null)
    }

    /**
     * 간편 등록 여부 확인
     */
    fun isRegistered(): Boolean {
        return prefs.getBoolean(KEY_IS_REGISTERED, false) &&
               !getDisplayName().isNullOrEmpty()
    }

    /**
     * 표시 이름 업데이트
     */
    fun updateDisplayName(displayName: String) {
        prefs.edit().putString(KEY_DISPLAY_NAME, displayName).apply()
    }

    /**
     * 모든 토큰 및 사용자 정보 삭제
     */
    fun clear() {
        prefs.edit().clear().apply()
    }
}
