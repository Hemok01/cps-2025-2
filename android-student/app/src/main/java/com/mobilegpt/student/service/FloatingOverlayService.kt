package com.mobilegpt.student.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.mobilegpt.student.R
import com.mobilegpt.student.data.local.TokenPreferences
import com.mobilegpt.student.data.websocket.WebSocketManager
import com.mobilegpt.student.detector.models.ErrorType
import com.mobilegpt.student.detector.models.TrackingState
import com.mobilegpt.student.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import javax.inject.Inject

/**
 * Floating Overlay Service
 * 다른 앱 위에 플로팅 버튼을 표시하는 Foreground Service
 *
 * 주요 기능:
 * - 현재 단계 및 진행도 표시
 * - 완료 버튼
 * - 도움 요청 버튼
 * - 드래그로 위치 이동
 * - 탭으로 확장/축소
 */
@AndroidEntryPoint
class FloatingOverlayService : Service() {

    companion object {
        private const val TAG = "FloatingOverlayService"
        private const val NOTIFICATION_CHANNEL_ID = "floating_overlay_channel"
        private const val NOTIFICATION_ID = 1001

        // Intent Actions
        const val ACTION_START = "com.mobilegpt.student.ACTION_START_OVERLAY"
        const val ACTION_STOP = "com.mobilegpt.student.ACTION_STOP_OVERLAY"
        const val ACTION_UPDATE_PROGRESS = "com.mobilegpt.student.ACTION_UPDATE_PROGRESS"
        const val ACTION_STEP_COMPLETE = "com.mobilegpt.student.ACTION_STEP_COMPLETE"
        const val ACTION_HELP_REQUEST = "com.mobilegpt.student.ACTION_HELP_REQUEST"
        const val ACTION_SESSION_ENDED = "com.mobilegpt.student.ACTION_SESSION_ENDED"

        // Intent Extras
        const val EXTRA_CURRENT_STEP = "extra_current_step"
        const val EXTRA_TOTAL_STEPS = "extra_total_steps"
        const val EXTRA_STEP_TITLE = "extra_step_title"
        const val EXTRA_SESSION_CODE = "extra_session_code"
        const val EXTRA_SUBTASK_ID = "extra_subtask_id"

        /**
         * 서비스 시작
         * @param subtaskId 현재 서브태스크 ID (서버 전송용)
         * @return true if service started, false if permission not granted
         */
        fun start(
            context: Context,
            sessionCode: String,
            currentStep: Int,
            totalSteps: Int,
            stepTitle: String,
            subtaskId: Int? = null
        ): Boolean {
            // 권한 확인
            if (!android.provider.Settings.canDrawOverlays(context)) {
                Log.w(TAG, "Cannot start overlay service: SYSTEM_ALERT_WINDOW permission not granted")
                return false
            }

            val intent = Intent(context, FloatingOverlayService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SESSION_CODE, sessionCode)
                putExtra(EXTRA_CURRENT_STEP, currentStep)
                putExtra(EXTRA_TOTAL_STEPS, totalSteps)
                putExtra(EXTRA_STEP_TITLE, stepTitle)
                subtaskId?.let { putExtra(EXTRA_SUBTASK_ID, it) }
            }
            context.startForegroundService(intent)
            return true
        }

        /**
         * 서비스 중지
         */
        fun stop(context: Context) {
            val intent = Intent(context, FloatingOverlayService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        /**
         * 세션 종료 시 서비스 중지
         * WebSocket 메시지와 별개로 AccessibilityService에서도 호출 가능
         */
        fun onSessionEnded(context: Context) {
            Log.d(TAG, "onSessionEnded called")
            val intent = Intent(context, FloatingOverlayService::class.java).apply {
                action = ACTION_SESSION_ENDED
            }
            context.startService(intent)
        }

        /**
         * 진행도 업데이트
         */
        fun updateProgress(
            context: Context,
            currentStep: Int,
            totalSteps: Int,
            stepTitle: String,
            subtaskId: Int? = null
        ) {
            val intent = Intent(context, FloatingOverlayService::class.java).apply {
                action = ACTION_UPDATE_PROGRESS
                putExtra(EXTRA_CURRENT_STEP, currentStep)
                putExtra(EXTRA_TOTAL_STEPS, totalSteps)
                putExtra(EXTRA_STEP_TITLE, stepTitle)
                subtaskId?.let { putExtra(EXTRA_SUBTASK_ID, it) }
            }
            context.startService(intent)
        }

        /**
         * 연결 상태 업데이트
         */
        fun updateConnectionStatus(context: Context, isConnected: Boolean) {
            val intent = Intent(context, FloatingOverlayService::class.java).apply {
                action = "com.mobilegpt.student.ACTION_UPDATE_CONNECTION"
                putExtra("extra_is_connected", isConnected)
            }
            context.startService(intent)
        }
    }

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var isExpanded = false

    // 현재 상태
    private var currentStep = 1
    private var totalSteps = 1
    private var stepTitle = ""
    private var sessionCode = ""
    private var subtaskId: Int? = null
    private var isConnected = true

    // 연결 끊김 시 자동 종료 타이머
    private var disconnectTimeoutRunnable: Runnable? = null
    private val DISCONNECT_TIMEOUT_MS = 30_000L  // 30초

    // UI 비교 기반 추적 상태
    private var currentTrackingState: TrackingState = TrackingState.WAITING

    // 콜백
    private var onStepComplete: (() -> Unit)? = null
    private var onHelpRequest: (() -> Unit)? = null

    // Handler for UI updates
    private val mainHandler = Handler(Looper.getMainLooper())

    // 완료 피드백 오버레이
    private var completionFeedbackView: View? = null

    // 자동 완료 및 추적 상태 이벤트 수신용 BroadcastReceiver
    private val accessibilityEventReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                MobileGPTAccessibilityService.ACTION_STEP_COMPLETED -> {
                    val subtaskId = intent.getIntExtra(MobileGPTAccessibilityService.EXTRA_SUBTASK_ID, -1)
                    val subtaskTitle = intent.getStringExtra(MobileGPTAccessibilityService.EXTRA_SUBTASK_TITLE) ?: ""
                    Log.d(TAG, "Received step completion broadcast: id=$subtaskId, title=$subtaskTitle")
                    showCompletionFeedback(subtaskTitle)

                    // ★ 단계 완료 후 다음 단계 정보로 오버레이 UI 업데이트
                    refreshProgressFromPreferences()
                }

                MobileGPTAccessibilityService.ACTION_TRACKING_STATE_CHANGED -> {
                    val stateName = intent.getStringExtra(MobileGPTAccessibilityService.EXTRA_TRACKING_STATE) ?: "WAITING"
                    val newState = try {
                        TrackingState.valueOf(stateName)
                    } catch (e: Exception) {
                        TrackingState.WAITING
                    }
                    Log.d(TAG, "Received tracking state change: $stateName")
                    updateTrackingStateUI(newState)
                }

                MobileGPTAccessibilityService.ACTION_ERROR_DETECTED -> {
                    val errorTypeName = intent.getStringExtra(MobileGPTAccessibilityService.EXTRA_ERROR_TYPE) ?: ""
                    val errorSubtaskId = intent.getIntExtra(MobileGPTAccessibilityService.EXTRA_SUBTASK_ID, -1)
                    Log.d(TAG, "Received error broadcast: type=$errorTypeName, subtaskId=$errorSubtaskId")
                    showErrorFeedback(errorTypeName)
                }
            }
        }
    }

    // Hilt EntryPoint for accessing dependencies
    @dagger.hilt.EntryPoint
    @dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
    interface FloatingOverlayEntryPoint {
        fun webSocketManager(): WebSocketManager
        fun tokenPreferences(): TokenPreferences
        fun sessionPreferences(): com.mobilegpt.student.data.local.SessionPreferences
    }

    private val entryPoint by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            FloatingOverlayEntryPoint::class.java
        )
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()

        // AccessibilityService 이벤트 수신용 BroadcastReceiver 등록
        // (단계 완료, 추적 상태 변경, 오류 감지)
        val filter = IntentFilter().apply {
            addAction(MobileGPTAccessibilityService.ACTION_STEP_COMPLETED)
            addAction(MobileGPTAccessibilityService.ACTION_TRACKING_STATE_CHANGED)
            addAction(MobileGPTAccessibilityService.ACTION_ERROR_DETECTED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(accessibilityEventReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(accessibilityEventReceiver, filter)
        }
        Log.d(TAG, "Accessibility event receiver registered (completion, tracking, error)")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                sessionCode = intent.getStringExtra(EXTRA_SESSION_CODE) ?: ""
                currentStep = intent.getIntExtra(EXTRA_CURRENT_STEP, 1)
                totalSteps = intent.getIntExtra(EXTRA_TOTAL_STEPS, 1)
                stepTitle = intent.getStringExtra(EXTRA_STEP_TITLE) ?: ""
                subtaskId = if (intent.hasExtra(EXTRA_SUBTASK_ID)) {
                    intent.getIntExtra(EXTRA_SUBTASK_ID, -1).takeIf { it >= 0 }
                } else null

                Log.d(TAG, "Starting overlay: sessionCode=$sessionCode, step=$currentStep/$totalSteps, subtaskId=$subtaskId")
                startForeground(NOTIFICATION_ID, createNotification())
                showOverlay()
            }
            ACTION_STOP -> {
                Log.d(TAG, "Stopping overlay")
                hideOverlay()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_UPDATE_PROGRESS -> {
                currentStep = intent.getIntExtra(EXTRA_CURRENT_STEP, currentStep)
                totalSteps = intent.getIntExtra(EXTRA_TOTAL_STEPS, totalSteps)
                stepTitle = intent.getStringExtra(EXTRA_STEP_TITLE) ?: stepTitle
                if (intent.hasExtra(EXTRA_SUBTASK_ID)) {
                    subtaskId = intent.getIntExtra(EXTRA_SUBTASK_ID, -1).takeIf { it >= 0 }
                }
                Log.d(TAG, "Updating progress: step=$currentStep/$totalSteps, subtaskId=$subtaskId")
                updateOverlayUI()
            }
            ACTION_STEP_COMPLETE -> {
                performStepComplete()
            }
            ACTION_HELP_REQUEST -> {
                performHelpRequest()
            }
            "com.mobilegpt.student.ACTION_UPDATE_CONNECTION" -> {
                isConnected = intent.getBooleanExtra("extra_is_connected", true)
                updateConnectionStatus()
            }
            ACTION_SESSION_ENDED -> {
                Log.d(TAG, "Session ended - stopping overlay and screen capture")
                hideOverlay()
                // 스크린캡처도 함께 종료
                ScreenCaptureService.stop(this)
                ScreenCaptureService.clearMediaProjectionResult()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        // START_NOT_STICKY: 앱이 종료되면 서비스도 재시작하지 않음
        return START_NOT_STICKY
    }

    /**
     * 앱이 최근 앱에서 스와이프로 제거될 때 호출됨
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d(TAG, "App removed from recent apps - stopping overlay and screen capture")
        hideOverlay()
        hideCompletionFeedback()
        // 스크린캡처도 함께 종료
        ScreenCaptureService.stop(this)
        ScreenCaptureService.clearMediaProjectionResult()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        hideOverlay()
        hideCompletionFeedback()

        // 연결 끊김 타이머 취소
        cancelDisconnectTimeout()

        // BroadcastReceiver 해제
        try {
            unregisterReceiver(accessibilityEventReceiver)
            Log.d(TAG, "Accessibility event receiver unregistered")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unregister receiver", e)
        }

        super.onDestroy()
    }

    /**
     * 알림 채널 생성
     */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "강의 진행",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "강의 진행 상태를 표시합니다"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    /**
     * Foreground Notification 생성
     */
    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("강의 진행 중")
            .setContentText("$currentStep/$totalSteps - $stepTitle")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    /**
     * 오버레이 표시
     */
    private fun showOverlay() {
        if (overlayView != null) return

        // 권한 확인
        if (!android.provider.Settings.canDrawOverlays(this)) {
            Log.e(TAG, "SYSTEM_ALERT_WINDOW permission not granted! Cannot show overlay.")
            // 권한이 없으면 서비스 중지
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        // 오버레이 뷰 생성
        overlayView = createOverlayView()

        // WindowManager 파라미터 설정
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 200
        }

        try {
            windowManager.addView(overlayView, params)
            Log.d(TAG, "Overlay shown successfully")
        } catch (e: WindowManager.BadTokenException) {
            Log.e(TAG, "Failed to add overlay view: BadTokenException", e)
            overlayView = null
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add overlay view", e)
            overlayView = null
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    /**
     * 오버레이 뷰 생성
     */
    private fun createOverlayView(): View {
        // 프로그래매틱하게 뷰 생성 (XML 리소스 없이)
        val context = this

        // 메인 컨테이너
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xE6FFFFFF.toInt()) // 약간 투명한 흰색
            setPadding(24, 16, 24, 16)
            elevation = 8f
        }

        // 축소 상태 뷰 (항상 표시)
        val collapsedView = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            // 진행률 텍스트
            val progressText = TextView(context).apply {
                id = R.id.overlay_progress_text
                text = "$currentStep/$totalSteps"
                textSize = 16f
                setTextColor(0xFF1976D2.toInt())
                setPadding(0, 0, 16, 0)
            }
            addView(progressText)

            // 단계 제목 (짧게)
            val titleText = TextView(context).apply {
                id = R.id.overlay_title_text
                text = stepTitle.take(15) + if (stepTitle.length > 15) "..." else ""
                textSize = 14f
                setTextColor(0xFF333333.toInt())
                maxLines = 1
            }
            addView(titleText)

            // 추적 상태 인디케이터 (TrackingState 이모지 + 색상)
            val statusIndicator = TextView(context).apply {
                id = R.id.overlay_status_dot
                text = currentTrackingState.emoji
                textSize = 16f
                setPadding(16, 0, 0, 0)
            }
            addView(statusIndicator)
        }
        container.addView(collapsedView)

        // 확장 상태 뷰 (탭 시 표시)
        val expandedView = LinearLayout(context).apply {
            id = R.id.overlay_expanded_view
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(0, 16, 0, 0)

            // 상세 제목
            val fullTitle = TextView(context).apply {
                id = R.id.overlay_full_title
                text = stepTitle
                textSize = 14f
                setTextColor(0xFF666666.toInt())
            }
            addView(fullTitle)

            // 진행률 바
            val progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
                id = R.id.overlay_progress_bar
                max = totalSteps
                progress = currentStep
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 8
                }
            }
            addView(progressBar)

            // 버튼 컨테이너
            val buttonContainer = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 12
                }

                // 완료 버튼
                val completeBtn = android.widget.Button(context).apply {
                    text = "✓ 완료"
                    textSize = 12f
                    setOnClickListener { performStepComplete() }
                }
                addView(completeBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

                // 도움요청 버튼
                val helpBtn = android.widget.Button(context).apply {
                    text = "🆘 도움"
                    textSize = 12f
                    setOnClickListener { performHelpRequest() }
                }
                addView(helpBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

                // 닫기 버튼
                val closeBtn = android.widget.Button(context).apply {
                    text = "✕"
                    textSize = 12f
                    setOnClickListener { toggleExpanded() }
                }
                addView(closeBtn, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ))
            }
            addView(buttonContainer)
        }
        container.addView(expandedView)

        // 터치 이벤트 처리 (드래그 + 탭)
        setupTouchListener(container)

        return container
    }

    /**
     * 터치 리스너 설정 (드래그 + 탭)
     */
    private fun setupTouchListener(view: View) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isMoving = false

        view.setOnTouchListener { v, event ->
            val params = v.layoutParams as WindowManager.LayoutParams

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isMoving = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY

                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        isMoving = true
                        params.x = initialX + dx.toInt()
                        params.y = initialY + dy.toInt()
                        windowManager.updateViewLayout(v, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isMoving) {
                        // 탭 - 확장/축소 토글
                        toggleExpanded()
                    }
                    true
                }
                else -> false
            }
        }
    }

    /**
     * 확장/축소 토글
     */
    private fun toggleExpanded() {
        isExpanded = !isExpanded
        overlayView?.findViewById<View>(R.id.overlay_expanded_view)?.visibility =
            if (isExpanded) View.VISIBLE else View.GONE
    }

    /**
     * 오버레이 UI 업데이트
     */
    private fun updateOverlayUI() {
        overlayView?.let { view ->
            view.findViewById<TextView>(R.id.overlay_progress_text)?.text = "$currentStep/$totalSteps"
            view.findViewById<TextView>(R.id.overlay_title_text)?.text =
                stepTitle.take(15) + if (stepTitle.length > 15) "..." else ""
            view.findViewById<TextView>(R.id.overlay_full_title)?.text = stepTitle
            view.findViewById<ProgressBar>(R.id.overlay_progress_bar)?.apply {
                max = totalSteps
                progress = currentStep
            }
        }

        // 알림도 업데이트
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, createNotification())
    }

    /**
     * SharedPreferences에서 진행도 새로고침
     *
     * AccessibilityService가 단계 완료를 보고하면 next_subtask가 SharedPreferences에 저장됨.
     * 이 메서드는 저장된 다음 단계 정보를 읽어 오버레이 UI를 업데이트함.
     */
    private fun refreshProgressFromPreferences() {
        mainHandler.post {
            try {
                val sessionPrefs = entryPoint.sessionPreferences()
                val nextSubtask = sessionPrefs.getCurrentSubtaskDetail()

                if (nextSubtask != null) {
                    // 다음 단계 정보가 있으면 업데이트
                    val newStep = (nextSubtask.orderIndex ?: (currentStep)) + 1  // 0-based -> 1-based
                    if (newStep != currentStep || stepTitle != nextSubtask.title) {
                        currentStep = newStep
                        stepTitle = nextSubtask.title
                        subtaskId = nextSubtask.id

                        Log.d(TAG, "refreshProgressFromPreferences: Updated to step=$currentStep, title=$stepTitle, id=$subtaskId")
                        updateOverlayUI()
                    }
                } else {
                    // 다음 단계가 없으면 모든 단계 완료 (마지막 단계였음)
                    Log.d(TAG, "refreshProgressFromPreferences: No next subtask - all steps completed!")
                    // currentStep을 totalSteps로 설정하여 완료 상태 표시
                    if (currentStep < totalSteps) {
                        currentStep = totalSteps
                        stepTitle = "✅ 모든 단계 완료!"
                        updateOverlayUI()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh progress from preferences", e)
            }
        }
    }

    /**
     * 오버레이 숨기기
     */
    private fun hideOverlay() {
        overlayView?.let {
            windowManager.removeView(it)
            overlayView = null
        }
        Log.d(TAG, "Overlay hidden")
    }

    /**
     * 단계 완료 처리
     */
    private fun performStepComplete() {
        val id = subtaskId
        Log.d(TAG, "Step complete: step=$currentStep, subtaskId=$id")

        if (id == null) {
            Log.w(TAG, "subtaskId is null, cannot send step complete")
            // 로컬에서만 단계 증가
            if (currentStep < totalSteps) {
                currentStep++
                updateOverlayUI()
            }
            return
        }

        // WebSocket으로 완료 메시지 전송 (subtask_id 사용)
        try {
            entryPoint.webSocketManager().sendStepComplete(id)
            Log.d(TAG, "Step complete sent for subtaskId=$id")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send step complete", e)
        }

        // 로컬 단계 증가 (실제 진행도 업데이트는 서버에서 WebSocket으로 받음)
        if (currentStep < totalSteps) {
            currentStep++
            updateOverlayUI()
        }
    }

    /**
     * 도움 요청 처리 (스크린샷 포함)
     */
    private fun performHelpRequest() {
        val id = subtaskId
        val deviceId = entryPoint.tokenPreferences().getDeviceId()
        Log.d(TAG, "Help requested: step=$currentStep, subtaskId=$id")

        // 스크린캡처 서비스가 활성화되어 있으면 스크린샷 캡처 후 전송
        if (ScreenCaptureService.hasMediaProjectionPermission()) {
            Log.d(TAG, "Capturing screenshot before help request")
            ScreenCaptureService.captureOnce(this) { base64Screenshot ->
                try {
                    entryPoint.webSocketManager().sendHelpRequest(id, deviceId, base64Screenshot)
                    Log.d(TAG, "Help request sent with screenshot=${base64Screenshot != null}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send help request", e)
                }
            }
        } else {
            // 스크린캡처 권한이 없으면 스크린샷 없이 전송
            try {
                entryPoint.webSocketManager().sendHelpRequest(id, deviceId, null)
                Log.d(TAG, "Help request sent without screenshot (no permission)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send help request", e)
            }
        }
    }

    /**
     * 연결 상태 UI 업데이트 및 자동 종료 타이머 관리
     */
    private fun updateConnectionStatus() {
        if (!isConnected) {
            // 연결 끊김 시 상태를 ERROR로 표시
            updateTrackingStateUI(TrackingState.ERROR)

            // 자동 종료 타이머 시작 (이미 있으면 재설정)
            startDisconnectTimeout()
            Log.d(TAG, "Connection lost - starting auto-stop timer (${DISCONNECT_TIMEOUT_MS / 1000}s)")
        } else {
            // 연결 복구 시 타이머 취소
            cancelDisconnectTimeout()
            Log.d(TAG, "Connection restored - cancelled auto-stop timer")
        }
    }

    /**
     * 연결 끊김 타임아웃 시작
     * 지정된 시간 동안 연결이 복구되지 않으면 서비스 자동 종료
     */
    private fun startDisconnectTimeout() {
        // 기존 타이머 취소
        cancelDisconnectTimeout()

        disconnectTimeoutRunnable = Runnable {
            Log.d(TAG, "Disconnect timeout reached - stopping overlay and screen capture automatically")
            hideOverlay()
            hideCompletionFeedback()
            // 스크린캡처도 함께 종료
            ScreenCaptureService.stop(this)
            ScreenCaptureService.clearMediaProjectionResult()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        mainHandler.postDelayed(disconnectTimeoutRunnable!!, DISCONNECT_TIMEOUT_MS)
    }

    /**
     * 연결 끊김 타임아웃 취소
     */
    private fun cancelDisconnectTimeout() {
        disconnectTimeoutRunnable?.let {
            mainHandler.removeCallbacks(it)
            disconnectTimeoutRunnable = null
        }
    }

    // ==================== TrackingState UI ====================

    /**
     * 추적 상태 UI 업데이트
     *
     * TrackingState에 따라 상태 인디케이터의 이모지를 변경합니다.
     * - WAITING: 👀 (대기 중)
     * - CHECKING: 🔍 (확인 중)
     * - MATCHED: ✅ (완료!)
     * - ERROR: ❌ (오류)
     * - COMPLETED: 🎉 (완료!)
     * - IN_PROGRESS: 📱 (진행 중)
     */
    private fun updateTrackingStateUI(newState: TrackingState) {
        mainHandler.post {
            currentTrackingState = newState

            overlayView?.let { view ->
                val statusIndicator = view.findViewById<TextView>(R.id.overlay_status_dot)
                statusIndicator?.text = newState.emoji
            }

            Log.d(TAG, "Tracking state UI updated: ${newState.displayLabel}")
        }
    }

    /**
     * 오류 피드백 표시
     *
     * 오류 타입에 따라 잠깐 오류 상태를 표시합니다.
     */
    private fun showErrorFeedback(errorTypeName: String) {
        mainHandler.post {
            try {
                // 상태를 ERROR로 변경
                updateTrackingStateUI(TrackingState.ERROR)

                // 진동 피드백 (짧게)
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        android.os.VibrationEffect.createOneShot(100, android.os.VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(100)
                }

                // 오류 메시지 표시 (토스트 대신 오버레이 제목 변경)
                val errorMessage = when (errorTypeName) {
                    "WRONG_APP" -> "⚠️ 다른 앱입니다"
                    "FROZEN_SCREEN" -> "⚠️ 화면이 멈췄습니다"
                    "WRONG_CLICK" -> "⚠️ 잘못된 클릭"
                    else -> "⚠️ 오류 발생"
                }

                overlayView?.let { view ->
                    val titleText = view.findViewById<TextView>(R.id.overlay_title_text)
                    val originalTitle = stepTitle.take(15) + if (stepTitle.length > 15) "..." else ""
                    titleText?.text = errorMessage

                    // 2초 후 원래 제목으로 복원
                    mainHandler.postDelayed({
                        titleText?.text = originalTitle
                        updateTrackingStateUI(TrackingState.WAITING)
                    }, 2000)
                }

                Log.d(TAG, "Error feedback shown: $errorTypeName")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show error feedback", e)
            }
        }
    }

    // ==================== Completion Feedback ====================

    /**
     * 단계 완료 피드백 표시 (체크마크 애니메이션)
     */
    private fun showCompletionFeedback(stepTitle: String = "") {
        mainHandler.post {
            try {
                hideCompletionFeedback()  // 기존 피드백 제거

                // 완료 피드백 뷰 생성
                completionFeedbackView = createCompletionFeedbackView(stepTitle)

                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.CENTER
                }

                windowManager.addView(completionFeedbackView, params)

                // 애니메이션 실행
                completionFeedbackView?.startAnimation(createCompletionAnimation())

                // 2초 후 자동 제거
                mainHandler.postDelayed({
                    hideCompletionFeedback()
                }, 2000)

                Log.d(TAG, "Completion feedback shown for: $stepTitle")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show completion feedback", e)
            }
        }
    }

    /**
     * 완료 피드백 뷰 생성
     */
    private fun createCompletionFeedbackView(stepTitle: String): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(0xCC000000.toInt())  // 반투명 검정
            setPadding(48, 32, 48, 32)
        }

        // 체크마크 이모지
        val checkmark = TextView(this).apply {
            text = "✅"
            textSize = 64f
            gravity = Gravity.CENTER
        }
        container.addView(checkmark)

        // "완료!" 텍스트
        val completedText = TextView(this).apply {
            text = "완료!"
            textSize = 24f
            setTextColor(0xFF4CAF50.toInt())  // 녹색
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 0)
        }
        container.addView(completedText)

        // 단계 제목 (있는 경우)
        if (stepTitle.isNotEmpty()) {
            val titleText = TextView(this).apply {
                text = stepTitle
                textSize = 14f
                setTextColor(0xFFFFFFFF.toInt())  // 흰색
                gravity = Gravity.CENTER
                maxLines = 2
                setPadding(0, 8, 0, 0)
            }
            container.addView(titleText)
        }

        return container
    }

    /**
     * 완료 애니메이션 생성 (확대 + 페이드인)
     */
    private fun createCompletionAnimation(): AnimationSet {
        val animSet = AnimationSet(true)

        // 확대 애니메이션 (0.5 -> 1.0)
        val scaleAnim = ScaleAnimation(
            0.5f, 1.0f,  // X축
            0.5f, 1.0f,  // Y축
            android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
            android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 300
        }
        animSet.addAnimation(scaleAnim)

        // 페이드인 애니메이션
        val alphaAnim = AlphaAnimation(0.0f, 1.0f).apply {
            duration = 300
        }
        animSet.addAnimation(alphaAnim)

        return animSet
    }

    /**
     * 완료 피드백 숨기기
     */
    private fun hideCompletionFeedback() {
        completionFeedbackView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to remove completion feedback view", e)
            }
            completionFeedbackView = null
        }
    }
}
