package com.example.mobilegpt_instructor.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.core.app.NotificationCompat
import com.example.mobilegpt_instructor.InstructorApplication
import com.mobilegpt.instructor.R

/**
 * 플로팅 오버레이 서비스 - 다른 앱 위에 녹화 제어 버튼 표시
 *
 * 녹화 중에 다른 앱으로 이동해도 녹화 제어가 가능하도록 함
 */
class FloatingOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_TOGGLE_RECORDING = "ACTION_TOGGLE_RECORDING"

        var onStopRecordingClick: (() -> Unit)? = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForegroundService()
                showFloatingButton()
            }
            ACTION_STOP -> {
                hideFloatingButton()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, InstructorApplication.CHANNEL_ID)
            .setContentTitle("녹화 중")
            .setContentText("화면 이벤트를 녹화하고 있습니다")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    private fun showFloatingButton() {
        if (floatingView != null) return

        // 플로팅 버튼 레이아웃 생성
        floatingView = createFloatingButton()

        // 윈도우 파라미터 설정
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 20
            y = 200
        }

        // 드래그 가능하게 설정
        setupDrag(floatingView!!, params)

        windowManager?.addView(floatingView, params)
    }

    private fun createFloatingButton(): View {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
            setPadding(16, 16, 16, 16)
        }

        // 녹화 중지 버튼
        val stopButton = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_media_pause)
            setBackgroundResource(android.R.drawable.btn_default)
            setOnClickListener {
                onStopRecordingClick?.invoke()
            }
        }

        layout.addView(stopButton, LinearLayout.LayoutParams(120, 120))

        return layout
    }

    private fun setupDrag(view: View, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX - (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(view, params)
                    true
                }
                else -> false
            }
        }
    }

    private fun hideFloatingButton() {
        floatingView?.let {
            windowManager?.removeView(it)
            floatingView = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        hideFloatingButton()
    }
}
