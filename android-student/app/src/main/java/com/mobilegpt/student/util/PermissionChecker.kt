package com.mobilegpt.student.util

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.accessibility.AccessibilityManager

/**
 * Permission Checker
 * 앱에서 필요한 권한들의 상태를 확인하고 설정 화면으로 이동하는 유틸리티
 */
object PermissionChecker {

    /**
     * 오버레이 권한 확인 (SYSTEM_ALERT_WINDOW)
     * 다른 앱 위에 플로팅 UI를 표시하기 위해 필요
     */
    fun canDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * Accessibility Service 활성화 여부 확인
     * 사용자 활동 로그 수집을 위해 필요
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )

        val packageName = context.packageName
        val serviceName = "$packageName/.service.MobileGPTAccessibilityService"

        return enabledServices.any { serviceInfo ->
            serviceInfo.resolveInfo.serviceInfo.let {
                "${it.packageName}/.service.${it.name.substringAfterLast('.')}" == serviceName ||
                "${it.packageName}/${it.name}" == serviceName ||
                it.name.contains("MobileGPTAccessibilityService")
            }
        }
    }

    /**
     * 대체 방법: Settings.Secure를 통한 확인
     */
    fun isAccessibilityServiceEnabledAlt(context: Context): Boolean {
        val enabledServicesString = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val packageName = context.packageName
        val expectedServiceName = "$packageName/com.mobilegpt.student.service.MobileGPTAccessibilityService"
        val shortServiceName = "$packageName/.service.MobileGPTAccessibilityService"

        return enabledServicesString.contains(expectedServiceName) ||
               enabledServicesString.contains(shortServiceName)
    }

    /**
     * 모든 필수 권한이 허용되었는지 확인
     */
    fun areAllPermissionsGranted(context: Context): Boolean {
        return canDrawOverlays(context) &&
               (isAccessibilityServiceEnabled(context) || isAccessibilityServiceEnabledAlt(context))
    }

    /**
     * 오버레이 권한 설정 화면 열기
     */
    fun openOverlaySettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Accessibility 설정 화면 열기
     */
    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * 앱 정보 설정 화면 열기
     */
    fun openAppSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
