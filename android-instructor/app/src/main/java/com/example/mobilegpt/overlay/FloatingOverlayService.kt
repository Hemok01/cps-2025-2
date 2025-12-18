package com.example.mobilegpt.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.ImageButton
import androidx.core.content.ContextCompat
import android.util.Log
import android.widget.Toast
import com.example.mobilegpt.MyAccessibilityService
import com.example.mobilegpt.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FloatingOverlayService : Service() {

    companion object {
        private const val TAG = "FloatingOverlay"
    }

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private var isRecording = false
    private var currentRecordingId: Long? = null

    override fun onCreate() {
        super.onCreate()

        // 🔥 mediaProjection 타입 없이 일반 FGS로 실행
        startForegroundNotification()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val inflater = LayoutInflater.from(this)
        overlayView = inflater.inflate(R.layout.overlay_fab, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.END
        params.y = 300

        val btnRecord = overlayView.findViewById<ImageButton>(R.id.btnRecord)

        btnRecord.setOnClickListener {
            if (!isRecording) {
                // 녹화 시작 - Django 서버에 녹화 생성 요청
                val title = "녹화_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}"

                MyAccessibilityService.startRecordingWithServer(
                    title = title,
                    onSuccess = { recordingId ->
                        currentRecordingId = recordingId
                        isRecording = true
                        btnRecord.background =
                            ContextCompat.getDrawable(this, R.drawable.overlay_fab_background_red)
                        Log.i(TAG, "Recording started: ID=$recordingId")
                    },
                    onError = { errorMsg ->
                        Toast.makeText(this, "녹화 시작 실패: $errorMsg", Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "Recording start failed: $errorMsg")
                    }
                )
            } else {
                // 녹화 종료 - Django 서버에 녹화 종료 요청
                MyAccessibilityService.stopRecordingWithServer { recordingId ->
                    Log.i(TAG, "Recording stopped: ID=$recordingId")
                    isRecording = false
                    currentRecordingId = null
                    stopSelf()
                }
            }
        }

        windowManager.addView(overlayView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        windowManager.removeView(overlayView)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundNotification() {
        val channelId = "overlay_service"
        val channelName = "Overlay Service"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )

            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
                .setContentTitle("녹화 플로팅 버튼 실행 중")
                .setSmallIcon(R.drawable.ic_mic_white)
                .build()
        } else {
            Notification.Builder(this)
                .setContentTitle("녹화 플로팅 버튼 실행 중")
                .setSmallIcon(R.drawable.ic_mic_white)
                .build()
        }

        // 🔥 일반 Foreground Service로 실행
        startForeground(1, notification)
    }
}
