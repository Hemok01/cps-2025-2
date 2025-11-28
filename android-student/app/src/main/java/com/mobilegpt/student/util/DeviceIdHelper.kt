package com.mobilegpt.student.util

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import java.util.UUID

/**
 * Device ID Helper
 * 기기 고유값을 가져오는 유틸리티
 *
 * Android ID를 기본으로 사용하며,
 * 불가능한 경우 UUID를 생성하여 SharedPreferences에 저장합니다.
 */
object DeviceIdHelper {

    private const val PREFS_NAME = "device_prefs"
    private const val KEY_DEVICE_ID = "device_id"

    /**
     * 기기 고유값 가져오기
     * 1. Android ID 시도
     * 2. 실패 시 저장된 UUID 사용
     * 3. 없으면 새 UUID 생성 후 저장
     */
    @SuppressLint("HardwareIds")
    fun getDeviceId(context: Context): String {
        // 먼저 Android ID 시도
        val androidId = try {
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )
        } catch (e: Exception) {
            null
        }

        // Android ID가 유효하면 사용
        if (!androidId.isNullOrEmpty() && androidId != "9774d56d682e549c") {
            return androidId
        }

        // 저장된 UUID 확인 또는 새로 생성
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var deviceId = prefs.getString(KEY_DEVICE_ID, null)

        if (deviceId.isNullOrEmpty()) {
            deviceId = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        }

        return deviceId
    }

    /**
     * 기기 고유값 초기화 (테스트용)
     */
    fun clearDeviceId(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_DEVICE_ID).apply()
    }
}
