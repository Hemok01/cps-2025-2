package com.mobilegpt.student.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilegpt.student.data.local.SessionPreferences
import com.mobilegpt.student.data.repository.AuthRepository
import com.mobilegpt.student.data.repository.SessionRepository
import com.mobilegpt.student.domain.model.SessionData
import com.mobilegpt.student.domain.model.SessionMessage
import com.tinder.scarlet.WebSocket
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Session ViewModel
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val authRepository: AuthRepository,
    private val sessionPreferences: SessionPreferences
) : ViewModel() {

    private val _joinUiState = MutableStateFlow<JoinSessionUiState>(JoinSessionUiState.Idle)
    val joinUiState: StateFlow<JoinSessionUiState> = _joinUiState.asStateFlow()

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Idle)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private val _messages = MutableStateFlow<List<String>>(emptyList())
    val messages: StateFlow<List<String>> = _messages.asStateFlow()

    init {
        // TODO: WebSocket 연결은 세션 참가 후에 시작
        // WebSocket 연결 상태 관찰
        // observeWebSocketEvents()
        // 서버 메시지 수신
        // observeSessionMessages()
    }

    /**
     * 세션 참가
     */
    fun joinSession(sessionCode: String) {
        viewModelScope.launch {
            _joinUiState.value = JoinSessionUiState.Loading

            val result = sessionRepository.joinSession(sessionCode)
            if (result.isSuccess) {
                val response = result.getOrNull()!!

                // 세션 ID 저장
                sessionPreferences.setSessionId(response.session.id)

                _joinUiState.value = JoinSessionUiState.Success(response.session)
            } else {
                _joinUiState.value = JoinSessionUiState.Error(
                    result.exceptionOrNull()?.message ?: "세션 참가 실패"
                )
            }
        }
    }

    /**
     * WebSocket 연결 상태 관찰
     */
    private fun observeWebSocketEvents() {
        viewModelScope.launch {
            sessionRepository.observeWebSocketEvents().collect { event ->
                when (event) {
                    is WebSocket.Event.OnConnectionOpened<*> -> {
                        _sessionState.value = SessionState.Connected
                        addMessage("WebSocket 연결됨")
                    }
                    is WebSocket.Event.OnConnectionClosed -> {
                        _sessionState.value = SessionState.Disconnected
                        addMessage("WebSocket 연결 종료")
                    }
                    is WebSocket.Event.OnConnectionFailed -> {
                        _sessionState.value = SessionState.Error("연결 실패")
                        addMessage("WebSocket 연결 실패: ${event.throwable.message}")
                    }
                    else -> {}
                }
            }
        }
    }

    /**
     * 서버 메시지 수신
     */
    private fun observeSessionMessages() {
        viewModelScope.launch {
            sessionRepository.observeSessionMessages().collect { message ->
                when (message.type) {
                    "step_change" -> {
                        val stepInfo = message.data
                        addMessage("📍 단계 변경: ${stepInfo?.get("title")}")

                        // subtask_id 저장
                        (stepInfo?.get("subtask_id") as? Double)?.toInt()?.let {
                            sessionPreferences.setSubtaskId(it)
                        }
                    }
                    "session_start" -> {
                        addMessage("▶️ 세션 시작!")
                    }
                    "session_pause" -> {
                        addMessage("⏸️ 세션 일시정지")
                    }
                    "session_resume" -> {
                        addMessage("▶️ 세션 재개")
                    }
                    "session_end" -> {
                        addMessage("⏹️ 세션 종료")
                    }
                    "help_response" -> {
                        val helpText = message.data?.get("help_text") as? String
                        addMessage("💡 도움말: $helpText")
                    }
                    else -> {
                        addMessage("메시지: ${message.type}")
                    }
                }
            }
        }
    }

    /**
     * 하트비트 전송
     */
    fun sendHeartbeat() {
        sessionRepository.sendHeartbeat()
        addMessage("💓 하트비트 전송")
    }

    /**
     * 단계 완료 알림
     */
    fun notifyStepComplete(subtaskId: Int) {
        sessionRepository.notifyStepComplete(subtaskId)
        addMessage("✅ 단계 완료 알림: $subtaskId")
    }

    /**
     * 도움 요청
     */
    fun requestHelp() {
        sessionRepository.requestHelp("도움이 필요합니다")
        addMessage("🆘 도움 요청")
    }

    /**
     * 메시지 추가
     */
    private fun addMessage(message: String) {
        _messages.value = _messages.value + message
    }

    /**
     * 사용자 이름 가져오기
     */
    fun getUserName(): String {
        return authRepository.getUserName()
    }

    /**
     * 에러 상태 초기화
     */
    fun clearJoinError() {
        _joinUiState.value = JoinSessionUiState.Idle
    }
}

/**
 * Join Session UI State
 */
sealed class JoinSessionUiState {
    object Idle : JoinSessionUiState()
    object Loading : JoinSessionUiState()
    data class Success(val session: SessionData) : JoinSessionUiState()
    data class Error(val message: String) : JoinSessionUiState()
}

/**
 * Session State
 */
sealed class SessionState {
    object Idle : SessionState()
    object Connected : SessionState()
    object Disconnected : SessionState()
    data class Error(val message: String) : SessionState()
}
