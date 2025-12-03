package com.mobilegpt.student.service

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.util.Base64
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.mobilegpt.student.R
import com.mobilegpt.student.data.api.ScreenshotApi
import com.mobilegpt.student.data.local.TokenPreferences
import com.mobilegpt.student.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * Screen Capture Service
 * MediaProjection API를 사용하여 화면을 캡처하는 Foreground Service
 *
 * 주요 기능:
 * - MediaProjection을 사용한 전체 화면 캡처
 * - 도움 요청 시 1회성 캡처 (Base64 반환)
 * - JPEG 압축 (30% 품질, ~50KB)
 *
 * 변경: 30초 주기 자동 캡처 제거 → 도움 요청 시에만 캡처
 */
@AndroidEntryPoint
class ScreenCaptureService : Service() {

    companion object {
        private const val TAG = "ScreenCaptureService"
        private const val NOTIFICATION_CHANNEL_ID = "screen_capture_channel"
        private const val NOTIFICATION_ID = 1002

        // 캡처 설정
        const val JPEG_QUALITY = 30              // 30% 품질

        // Intent Actions
        const val ACTION_START = "com.mobilegpt.student.ACTION_START_CAPTURE"
        const val ACTION_STOP = "com.mobilegpt.student.ACTION_STOP_CAPTURE"
        const val ACTION_CAPTURE_ONCE = "com.mobilegpt.student.ACTION_CAPTURE_ONCE"

        // Intent Extras
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        const val EXTRA_SESSION_ID = "extra_session_id"
        const val EXTRA_DEVICE_ID = "extra_device_id"

        // MediaProjection Intent를 저장 (Activity에서 설정)
        private var mediaProjectionResultCode: Int = Activity.RESULT_CANCELED
        private var mediaProjectionResultData: Intent? = null

        /**
         * MediaProjection 권한 결과 저장
         * MainActivity의 onActivityResult에서 호출
         */
        fun setMediaProjectionResult(resultCode: Int, data: Intent?) {
            mediaProjectionResultCode = resultCode
            mediaProjectionResultData = data
            Log.d(TAG, "MediaProjection result saved: resultCode=$resultCode")
        }

        /**
         * MediaProjection 권한이 있는지 확인
         */
        fun hasMediaProjectionPermission(): Boolean {
            return mediaProjectionResultCode == Activity.RESULT_OK && mediaProjectionResultData != null
        }

        /**
         * 서비스 시작
         */
        fun start(context: Context, sessionId: Int, deviceId: String): Boolean {
            if (!hasMediaProjectionPermission()) {
                Log.w(TAG, "Cannot start: MediaProjection permission not granted")
                return false
            }

            val intent = Intent(context, ScreenCaptureService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_RESULT_CODE, mediaProjectionResultCode)
                putExtra(EXTRA_RESULT_DATA, mediaProjectionResultData)
                putExtra(EXTRA_SESSION_ID, sessionId)
                putExtra(EXTRA_DEVICE_ID, deviceId)
            }
            context.startForegroundService(intent)
            return true
        }

        /**
         * 서비스 중지
         */
        fun stop(context: Context) {
            val intent = Intent(context, ScreenCaptureService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        /**
         * 권한 초기화 (세션 종료 시)
         */
        fun clearMediaProjectionResult() {
            mediaProjectionResultCode = Activity.RESULT_CANCELED
            mediaProjectionResultData = null
        }

        // 1회성 캡처 결과 콜백
        private var captureCallback: ((String?) -> Unit)? = null

        /**
         * 1회성 화면 캡처 요청 (도움 요청 시 사용)
         * @param callback Base64 인코딩된 이미지 또는 null (실패 시)
         */
        fun captureOnce(context: Context, callback: (String?) -> Unit) {
            if (!hasMediaProjectionPermission()) {
                Log.w(TAG, "captureOnce: MediaProjection permission not granted")
                callback(null)
                return
            }
            captureCallback = callback
            val intent = Intent(context, ScreenCaptureService::class.java).apply {
                action = ACTION_CAPTURE_ONCE
            }
            context.startService(intent)
        }

        /**
         * 캡처 결과 전달 (서비스 내부에서 호출)
         */
        internal fun deliverCaptureResult(base64Image: String?) {
            captureCallback?.invoke(base64Image)
            captureCallback = null
        }
    }

    @Inject
    lateinit var screenshotApi: ScreenshotApi

    @Inject
    lateinit var tokenPreferences: TokenPreferences

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private var sessionId: Int = 0
    private var deviceId: String = ""

    private var captureJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null

    private var screenWidth = 0
    private var screenHeight = 0
    private var screenDensity = 0

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        createNotificationChannel()
        initScreenMetrics()
        initHandlerThread()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                val resultData = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
                sessionId = intent.getIntExtra(EXTRA_SESSION_ID, 0)
                deviceId = intent.getStringExtra(EXTRA_DEVICE_ID) ?: ""

                Log.d(TAG, "Starting capture service: sessionId=$sessionId, deviceId=$deviceId")
                startForeground(NOTIFICATION_ID, createNotification())
                startCapture(resultCode, resultData)
            }
            ACTION_STOP -> {
                Log.d(TAG, "Stopping capture")
                stopCapture()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_CAPTURE_ONCE -> {
                // 1회성 캡처 (도움 요청 시)
                Log.d(TAG, "Capture once requested")
                serviceScope.launch {
                    val base64Image = captureAndEncode()
                    deliverCaptureResult(base64Image)
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopCapture()
        serviceScope.cancel()
        handlerThread?.quitSafely()
        Log.d(TAG, "Service destroyed")
    }

    private fun initScreenMetrics() {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getMetrics(metrics)

        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
        screenDensity = metrics.densityDpi

        Log.d(TAG, "Screen metrics: ${screenWidth}x${screenHeight}, density=$screenDensity")
    }

    private fun initHandlerThread() {
        handlerThread = HandlerThread("ScreenCaptureThread").apply { start() }
        handler = Handler(handlerThread!!.looper)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "화면 캡처",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "학생 화면 캡처 서비스"
            setShowBadge(false)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("수업 모니터링 중")
            .setContentText("도움 요청 시 화면이 캡처됩니다")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startCapture(resultCode: Int, resultData: Intent?) {
        if (resultData == null) {
            Log.e(TAG, "MediaProjection data is null")
            stopSelf()
            return
        }

        try {
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, resultData)

            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    Log.d(TAG, "MediaProjection stopped")
                    stopCapture()
                }
            }, Handler(Looper.getMainLooper()))

            setupImageReader()
            // 주기적 캡처 제거 - 도움 요청 시에만 캡처
            // startPeriodicCapture()

            Log.d(TAG, "Screen capture service ready (on-demand capture mode)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start screen capture", e)
            stopSelf()
        }
    }

    private fun setupImageReader() {
        // 이미지 크기를 줄여서 메모리와 네트워크 사용량 감소
        val captureWidth = screenWidth / 2
        val captureHeight = screenHeight / 2

        imageReader = ImageReader.newInstance(
            captureWidth,
            captureHeight,
            PixelFormat.RGBA_8888,
            2
        )

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            captureWidth,
            captureHeight,
            screenDensity / 2,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            handler
        )

        Log.d(TAG, "ImageReader setup: ${captureWidth}x${captureHeight}")
    }

    // 주기적 캡처 제거됨 - 아래 메서드는 더 이상 사용되지 않음
    // private fun startPeriodicCapture() { ... }

    /**
     * 1회성 캡처 후 Base64 반환 (도움 요청 시 사용)
     */
    private suspend fun captureAndEncode(): String? {
        return try {
            val bitmap = captureScreen()
            if (bitmap == null) {
                Log.w(TAG, "captureAndEncode: bitmap is null")
                return null
            }
            val base64Image = compressAndEncode(bitmap)
            bitmap.recycle()
            Log.d(TAG, "captureAndEncode: success, size=${base64Image.length}")
            base64Image
        } catch (e: Exception) {
            Log.e(TAG, "captureAndEncode failed", e)
            null
        }
    }

    private fun captureScreen(): Bitmap? {
        val image: Image? = try {
            imageReader?.acquireLatestImage()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire image", e)
            null
        }

        if (image == null) {
            Log.w(TAG, "No image available")
            return null
        }

        return try {
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * image.width

            val bitmap = Bitmap.createBitmap(
                image.width + rowPadding / pixelStride,
                image.height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)

            // Crop to actual size (remove padding)
            if (rowPadding > 0) {
                Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height).also {
                    bitmap.recycle()
                }
            } else {
                bitmap
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create bitmap", e)
            null
        } finally {
            image.close()
        }
    }

    private fun compressAndEncode(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
        val byteArray = outputStream.toByteArray()
        val base64 = Base64.encodeToString(byteArray, Base64.NO_WRAP)

        Log.d(TAG, "Compressed image: ${byteArray.size / 1024}KB")
        return base64
    }

    // uploadScreenshot 제거됨 - 도움 요청 시 WebSocket으로 스크린샷 전송
    // private suspend fun uploadScreenshot(base64Image: String) { ... }

    private fun stopCapture() {
        captureJob?.cancel()
        captureJob = null

        virtualDisplay?.release()
        virtualDisplay = null

        imageReader?.close()
        imageReader = null

        mediaProjection?.stop()
        mediaProjection = null

        Log.d(TAG, "Capture stopped")
    }
}

/**
 * 스크린샷 업로드 요청 데이터 클래스
 */
data class ScreenshotUploadRequest(
    val device_id: String,
    val image_data: String,
    val captured_at: Long
)
