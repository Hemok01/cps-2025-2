package com.mobilegpt.student

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * MobileGPT Student Application
 * Hilt dependency injection setup
 */
@HiltAndroidApp
class MobileGPTApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
