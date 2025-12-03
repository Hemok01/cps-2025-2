package com.mobilegpt.student.presentation.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mobilegpt.student.data.api.JoinSessionResponse
import com.mobilegpt.student.data.local.SessionPreferences
import com.mobilegpt.student.data.local.TokenPreferences
import com.mobilegpt.student.data.repository.AuthRepository
import com.mobilegpt.student.data.repository.SessionRepository
import com.mobilegpt.student.data.websocket.WebSocketConnectionState
import com.mobilegpt.student.domain.model.MessageType
import com.mobilegpt.student.domain.model.SessionData
import com.mobilegpt.student.domain.model.SessionSummary
import com.mobilegpt.student.presentation.navigation.JoinSessionUiState
import com.mobilegpt.student.service.FloatingOverlayService
import com.mobilegpt.student.service.ScreenCaptureService
import com.tinder.scarlet.WebSocket
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Session ViewModel
 * 세션 전체 플로우의 상태를 관리합니다.
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    application: Application,
    private val sessionRepository: SessionRepository,
    private val authRepository: AuthRepository,
    private val sessionPreferences: SessionPreferences,
    private val tokenPreferences: TokenPreferences
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "SessionViewModel"
    }

    // ==================== UI States ====================

    private val _joinSessionState = MutableStateFlow<JoinSessionUiState>(JoinSessionUiState.Idle)
    val joinSessionState: StateFlow<JoinSessionUiState> = _joinSessionState.asStateFlow()

    private val _connectionState = MutableStateFlow(WebSocketConnectionState.DISCONNECTED)
    val connectionState: StateFlow<WebSocketConnectionState> = _connectionState.asStateFlow()

    private val _messages = MutableStateFlow<List<String>>(emptyList())
    val messages: StateFlow<List<String>> = _messages.asStateFlow()

    // ==================== Session Status (실시간 WebSocket 업데이트) ====================

    private val _sessionStatus = MutableStateFlow<String?>(null)
    val sessionStatus: StateFlow<String?> = _sessionStatus.asStateFlow()

    // ==================== Progress States (더미 로직) ====================

    private val _currentStep = MutableStateFlow(1)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _totalSteps = MutableStateFlow(5)  // 기본값 5단계 (테스트용)
    val totalSteps: StateFlow<Int> = _totalSteps.asStateFlow()

    private val _currentSubtaskId = MutableStateFlow<Int?>(null)
    val currentSubtaskId: StateFlow<Int?> = _currentSubtaskId.asStateFlow()

    private val _currentStepTitle = MutableStateFlow("")
    val currentStepTitle: StateFlow<String> = _currentStepTitle.asStateFlow()

    private val _helpRequestCount = MutableStateFlow(0)
    val helpRequestCount: StateFlow<Int> = _helpRequestCount.asStateFlow()

    private val _isOverlayShowing = MutableStateFlow(false)
    val isOverlayShowing: StateFlow<Boolean> = _isOverlayShowing.asStateFlow()

    private val _isScreenCaptureActive = MutableStateFlow(false)
    val isScreenCaptureActive: StateFlow<Boolean> = _isScreenCaptureActive.asStateFlow()

    private val _isMediaProjectionPermissionGranted = MutableStateFlow(false)
    val isMediaProjectionPermissionGranted: StateFlow<Boolean> = _isMediaProjectionPermissionGranted.asStateFlow()

    private val _sessionSummary = MutableStateFlow(SessionSummary())
    val sessionSummary: StateFlow<SessionSummary> = _sessionSummary.asStateFlow()

    // ==================== Session Data ====================

    private var currentSessionCode: String? = null
    private var currentSessionId: Int? = null
    private var sessionStartTime: Long = 0

    init {
        // WebSocket 연결 상태 관찰
        observeConnectionState()
        // WebSocket 메시지 관찰
        observeSessionMessages()
    }

    // ==================== Session Actions ====================

    /**
     * 세션 참가
     */
    fun joinSession(sessionCode: String) {
        viewModelScope.launch {
            _joinSessionState.value = JoinSessionUiState.Loading

            // device_id와 name 가져오기
            val deviceId = tokenPreferences.getDeviceId() ?: ""
            val name = tokenPreferences.getDisplayName() ?: "사용자"

            val result = sessionRepository.joinSession(
                sessionCode = sessionCode,
                deviceId = deviceId,
                name = name
            )

            if (result.isSuccess) {
                val response = result.getOrNull()!!

                // 세션 ID 저장
                sessionPreferences.setSessionId(response.session.id)
                currentSessionCode = sessionCode
                currentSessionId = response.session.id

                // 초기 세션 상태 설정 (API 응답에서)
                _sessionStatus.value = response.session.status
                Log.d(TAG, "joinSession: Initial session status = ${response.session.status}")

                // totalSteps 업데이트
                response.session.totalSteps?.let {
                    _totalSteps.value = it
                }

                // 초기 subtask 정보 설정
                val initialSubtask = response.session.currentSubtaskDetail
                    ?: response.session.currentSubtask
                initialSubtask?.let {
                    _currentSubtaskId.value = it.id
                    _currentStepTitle.value = it.title
                    it.order?.let { order -> _currentStep.value = order }
                    it.orderIndex?.let { orderIndex -> _currentStep.value = orderIndex }
                    Log.d(TAG, "joinSession: Initial subtask - id=${it.id}, title=${it.title}")
                }

                // WebSocket 연결
                connectWebSocket(sessionCode)

                _joinSessionState.value = JoinSessionUiState.Success(response)
                sessionStartTime = System.currentTimeMillis()
            } else {
                _joinSessionState.value = JoinSessionUiState.Error(
                    result.exceptionOrNull()?.message ?: "세션 참가 실패"
                )
            }
        }
    }

    /**
     * WebSocket 연결
     */
    fun connectWebSocket(sessionCode: String) {
        sessionRepository.connectWebSocket(sessionCode)
        currentSessionCode = sessionCode

        // Join 메시지 전송
        val deviceId = tokenPreferences.getDeviceId() ?: ""
        val name = tokenPreferences.getDisplayName() ?: "사용자"
        sessionRepository.sendJoinMessage(deviceId, name)
    }

    /**
     * WebSocket 연결 해제
     */
    fun disconnectWebSocket() {
        sessionRepository.disconnectWebSocket()
        currentSessionCode = null
        _connectionState.value = WebSocketConnectionState.DISCONNECTED
    }

    // ==================== Progress Actions (더미 로직) ====================

    /**
     * 현재 단계 완료
     * TODO: 동료 개발 - 실제 앱 사용 감지로 자동 진행
     */
    fun completeCurrentStep() {
        val current = _currentStep.value
        val total = _totalSteps.value

        if (current < total) {
            _currentStep.value = current + 1
            sessionRepository.notifyStepComplete(current)
            addMessage("✅ 단계 $current 완료")

            // 오버레이 업데이트
            updateOverlayProgress()
        }
    }

    /**
     * 도움 요청 (스크린샷 포함)
     * 화면을 캡처한 후 WebSocket으로 전송
     */
    fun requestHelp() {
        val context = getApplication<Application>()
        val subtaskId = sessionPreferences.getSubtaskId()

        // 스크린캡처 서비스가 활성화되어 있으면 스크린샷 캡처 후 전송
        if (ScreenCaptureService.hasMediaProjectionPermission()) {
            Log.d(TAG, "requestHelp: Capturing screenshot before sending help request")
            ScreenCaptureService.captureOnce(context) { base64Screenshot ->
                // 스크린샷 캡처 결과 (성공 또는 null)
                Log.d(TAG, "requestHelp: Screenshot captured=${base64Screenshot != null}")
                sessionRepository.requestHelp(subtaskId, base64Screenshot)
                _helpRequestCount.value += 1
                addMessage("🆘 도움 요청 (스크린샷 ${if (base64Screenshot != null) "포함" else "없음"})")
            }
        } else {
            // 스크린캡처 권한이 없으면 스크린샷 없이 전송
            Log.d(TAG, "requestHelp: No screenshot permission, sending without screenshot")
            sessionRepository.requestHelp(subtaskId, null)
            _helpRequestCount.value += 1
            addMessage("🆘 도움 요청 (스크린샷 없음)")
        }
    }

    /**
     * 하트비트 전송
     */
    fun sendHeartbeat() {
        sessionRepository.sendHeartbeat()
        addMessage("💓 하트비트 전송")
    }

    // ==================== Overlay Actions ====================

    /**
     * 오버레이 시작 + 앱 최소화
     */
    fun startOverlay() {
        Log.d(TAG, "startOverlay() called")
        val context = getApplication<Application>()
        val sessionCode = currentSessionCode
        if (sessionCode == null) {
            Log.e(TAG, "startOverlay() failed: sessionCode is null")
            return
        }
        val step = _currentStep.value
        val total = _totalSteps.value
        val title = getStepTitle()
        val subtaskId = getCurrentSubtaskId()

        Log.d(TAG, "Starting overlay: code=$sessionCode, step=$step/$total, title=$title, subtaskId=$subtaskId")

        val started = FloatingOverlayService.start(
            context = context,
            sessionCode = sessionCode,
            currentStep = step,
            totalSteps = total,
            stepTitle = title,
            subtaskId = subtaskId
        )

        if (started) {
            _isOverlayShowing.value = true
            Log.d(TAG, "Overlay started successfully")

            // 앱 최소화 (Home 화면으로 이동)
            minimizeApp(context)
        } else {
            Log.e(TAG, "Overlay failed to start - permission not granted?")
        }
    }

    /**
     * 앱 최소화 (Home 화면으로 이동)
     */
    private fun minimizeApp(context: Context) {
        val homeIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_HOME)
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(homeIntent)
        Log.d(TAG, "App minimized to home screen")
    }

    /**
     * 오버레이 중지
     */
    fun stopOverlay() {
        val context = getApplication<Application>()
        FloatingOverlayService.stop(context)
        _isOverlayShowing.value = false
        Log.d(TAG, "Overlay stopped")
    }

    /**
     * 오버레이 진행도 업데이트
     */
    private fun updateOverlayProgress() {
        if (_isOverlayShowing.value) {
            val context = getApplication<Application>()
            FloatingOverlayService.updateProgress(
                context = context,
                currentStep = _currentStep.value,
                totalSteps = _totalSteps.value,
                stepTitle = getStepTitle(),
                subtaskId = getCurrentSubtaskId()
            )
        }
    }

    /**
     * 오버레이 연결 상태 업데이트
     */
    private fun updateOverlayConnectionStatus(isConnected: Boolean) {
        if (_isOverlayShowing.value) {
            val context = getApplication<Application>()
            FloatingOverlayService.updateConnectionStatus(context, isConnected)
        }
    }

    // ==================== Screen Capture Actions ====================

    /**
     * MediaProjection 권한 결과 처리
     * MainActivity에서 권한 요청 결과를 받아 처리
     */
    fun onMediaProjectionPermissionResult(granted: Boolean) {
        _isMediaProjectionPermissionGranted.value = granted
        Log.d(TAG, "MediaProjection permission: $granted")

        if (granted) {
            // 권한이 승인되면 스크린캡처 시작
            startScreenCapture()
        }
    }

    /**
     * 스크린캡처 시작
     * 세션이 IN_PROGRESS 상태일 때 호출
     */
    fun startScreenCapture() {
        val context = getApplication<Application>()
        val sessionId = currentSessionId
        val deviceId = tokenPreferences.getDeviceId() ?: ""

        if (sessionId == null) {
            Log.e(TAG, "startScreenCapture failed: sessionId is null")
            return
        }

        if (!ScreenCaptureService.hasMediaProjectionPermission()) {
            Log.w(TAG, "startScreenCapture: MediaProjection permission not granted yet")
            return
        }

        Log.d(TAG, "Starting screen capture: sessionId=$sessionId, deviceId=$deviceId")

        val started = ScreenCaptureService.start(context, sessionId, deviceId)
        if (started) {
            _isScreenCaptureActive.value = true
            Log.d(TAG, "Screen capture started successfully")
        } else {
            Log.e(TAG, "Screen capture failed to start")
        }
    }

    /**
     * 스크린캡처 중지
     * 세션이 ENDED 상태가 되거나 세션에서 나갈 때 호출
     */
    fun stopScreenCapture() {
        val context = getApplication<Application>()
        ScreenCaptureService.stop(context)
        _isScreenCaptureActive.value = false
        Log.d(TAG, "Screen capture stopped")
    }

    /**
     * 현재 단계 제목 가져오기
     * StateFlow 값 -> JoinState 값 -> 기본값 순으로 참조
     */
    private fun getStepTitle(): String {
        // 먼저 StateFlow 값 확인
        val stateFlowTitle = _currentStepTitle.value
        if (stateFlowTitle.isNotEmpty()) {
            return stateFlowTitle
        }

        // JoinState에서 가져오기
        val joinState = _joinSessionState.value
        return if (joinState is JoinSessionUiState.Success) {
            joinState.response.session.currentSubtaskDetail?.title
                ?: joinState.response.session.currentSubtask?.title
                ?: "단계 ${_currentStep.value}"
        } else {
            "단계 ${_currentStep.value}"
        }
    }

    /**
     * 현재 서브태스크 ID 가져오기
     * StateFlow 값 -> JoinState 값 순으로 참조
     */
    private fun getCurrentSubtaskId(): Int? {
        // 먼저 StateFlow 값 확인
        val stateFlowId = _currentSubtaskId.value
        if (stateFlowId != null) {
            return stateFlowId
        }

        // JoinState에서 가져오기
        val joinState = _joinSessionState.value
        return if (joinState is JoinSessionUiState.Success) {
            joinState.response.session.currentSubtaskDetail?.id
                ?: joinState.response.session.currentSubtask?.id
        } else {
            null
        }
    }

    // ==================== WebSocket Observation ====================

    /**
     * 연결 상태 관찰
     */
    private fun observeConnectionState() {
        viewModelScope.launch {
            sessionRepository.connectionState.collect { state ->
                _connectionState.value = state
                // 오버레이에 연결 상태 전달
                val isConnected = state == WebSocketConnectionState.CONNECTED
                updateOverlayConnectionStatus(isConnected)
            }
        }
    }

    /**
     * 서버 메시지 수신
     */
    private fun observeSessionMessages() {
        Log.d(TAG, "observeSessionMessages: Starting collection from SharedFlow")
        viewModelScope.launch {
            Log.d(TAG, "observeSessionMessages: Coroutine started, collecting...")
            try {
                sessionRepository.observeSessionMessages().collect { message ->
                    Log.d(TAG, "observeSessionMessages: Received message from Flow: ${message.type}")
                    handleWebSocketMessage(message)
                }
            } catch (e: Exception) {
                Log.e(TAG, "observeSessionMessages: Error collecting messages", e)
            }
        }
    }

    /**
     * WebSocket 메시지 처리
     */
    private fun handleWebSocketMessage(message: com.mobilegpt.student.domain.model.SessionMessage) {
        Log.d(TAG, "handleWebSocketMessage: type=${message.type}, data=${message.data}")
        when (message.type) {
            MessageType.JOIN_CONFIRMED -> {
                addMessage("✅ 세션 참가 확인")
            }
            MessageType.HEARTBEAT_ACK -> {
                // 하트비트 응답 - 로그 생략
            }
            MessageType.STEP_CHANGED -> {
                val stepData = message.data
                val stepTitle = stepData?.get("title") as? String
                val subtaskId = (stepData?.get("id") as? Number)?.toInt()
                    ?: (stepData?.get("subtask_id") as? Number)?.toInt()

                addMessage("📍 단계 변경: $stepTitle")

                // 단계 정보 업데이트
                (stepData?.get("order") as? Number)?.toInt()?.let {
                    _currentStep.value = it
                }
                // subtask id 업데이트
                subtaskId?.let { _currentSubtaskId.value = it }
                // 단계 제목 업데이트
                stepTitle?.let { _currentStepTitle.value = it }

                updateOverlayProgress()
            }
            MessageType.SESSION_STATUS_CHANGED -> {
                val status = message.data?.get("status") as? String
                Log.d(TAG, "SESSION_STATUS_CHANGED: status=$status")

                // 실시간 세션 상태 업데이트 (화면 전환 트리거)
                status?.let { _sessionStatus.value = it }

                when (status) {
                    "IN_PROGRESS" -> {
                        addMessage("▶️ 세션 시작!")
                        // 세션 시작 시 스크린캡처 시작 (권한이 있는 경우)
                        if (ScreenCaptureService.hasMediaProjectionPermission()) {
                            startScreenCapture()
                        }
                    }
                    "PAUSED" -> addMessage("⏸️ 세션 일시정지")
                    "REVIEW_MODE", "ENDED" -> {
                        addMessage("⏹️ 세션 종료")
                        // 오버레이 및 스크린캡처 자동 종료
                        stopOverlay()
                        stopScreenCapture()
                        generateSessionSummary()
                    }
                    else -> addMessage("상태 변경: $status")
                }
            }
            MessageType.STEP_COMPLETE_CONFIRMED -> {
                addMessage("✅ 단계 완료 확인")
            }
            else -> {
                addMessage("메시지: ${message.type}")
            }
        }
    }

    // ==================== Session Summary ====================

    /**
     * 세션 요약 생성
     */
    private fun generateSessionSummary() {
        val durationMs = System.currentTimeMillis() - sessionStartTime
        val durationMinutes = durationMs / 60000

        _sessionSummary.value = SessionSummary(
            durationMinutes = durationMinutes,
            completedSteps = _currentStep.value,
            totalSteps = _totalSteps.value,
            helpRequestCount = _helpRequestCount.value,
            eventsLogged = 0  // TODO: AccessibilityService에서 카운트
        )
    }

    /**
     * 세션 초기화
     */
    fun resetSession() {
        disconnectWebSocket()
        stopOverlay()
        stopScreenCapture()

        // MediaProjection 권한 초기화
        ScreenCaptureService.clearMediaProjectionResult()
        _isMediaProjectionPermissionGranted.value = false
        _isScreenCaptureActive.value = false

        _joinSessionState.value = JoinSessionUiState.Idle
        _sessionStatus.value = null  // 세션 상태 초기화
        _currentStep.value = 1
        _currentSubtaskId.value = null
        _currentStepTitle.value = ""
        _helpRequestCount.value = 0
        _messages.value = emptyList()
        _sessionSummary.value = SessionSummary()

        currentSessionId = null
        sessionPreferences.clear()
    }

    // ==================== Helpers ====================

    /**
     * 메시지 추가
     */
    private fun addMessage(message: String) {
        _messages.value = _messages.value + message
    }

    /**
     * 에러 상태 초기화
     */
    fun clearJoinError() {
        _joinSessionState.value = JoinSessionUiState.Idle
    }
}
