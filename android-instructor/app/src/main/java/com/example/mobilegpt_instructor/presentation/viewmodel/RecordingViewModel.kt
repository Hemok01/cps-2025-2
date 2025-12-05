package com.example.mobilegpt_instructor.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobilegpt_instructor.data.model.AccessibilityEventData
import com.example.mobilegpt_instructor.data.model.RecordingResponse
import com.example.mobilegpt_instructor.data.repository.RecordingRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 녹화 세션을 관리하는 ViewModel
 * 녹화 생성, 시작, 중지, 이벤트 버퍼링 및 전송 관리
 */
class RecordingViewModel : ViewModel() {

    private val repository = RecordingRepository()

    // UI 상태
    private val _uiState = MutableStateFlow<RecordingUiState>(RecordingUiState.Idle)
    val uiState: StateFlow<RecordingUiState> = _uiState.asStateFlow()

    // 현재 녹화 세션
    private val _currentRecording = MutableStateFlow<RecordingResponse?>(null)
    val currentRecording: StateFlow<RecordingResponse?> = _currentRecording.asStateFlow()

    // 녹화 목록
    private val _recordings = MutableStateFlow<List<RecordingResponse>>(emptyList())
    val recordings: StateFlow<List<RecordingResponse>> = _recordings.asStateFlow()

    // 이벤트 버퍼 (서버로 전송 전 임시 저장)
    private val eventBuffer = mutableListOf<AccessibilityEventData>()
    private var flushJob: Job? = null

    companion object {
        private const val BUFFER_SIZE = 50 // 버퍼 크기
        private const val FLUSH_INTERVAL = 5000L // 5초마다 자동 전송
    }

    // 녹화 목록 로드
    fun loadRecordings() {
        viewModelScope.launch {
            _uiState.value = RecordingUiState.Loading

            repository.getRecordings()
                .onSuccess { list ->
                    _recordings.value = list
                    _uiState.value = RecordingUiState.Idle
                }
                .onFailure { e ->
                    _uiState.value = RecordingUiState.Error(e.message ?: "목록 로드 실패")
                }
        }
    }

    // 새 녹화 생성
    fun createRecording(title: String, description: String = "") {
        viewModelScope.launch {
            _uiState.value = RecordingUiState.Loading

            repository.createRecording(title, description)
                .onSuccess { recording ->
                    _currentRecording.value = recording
                    _uiState.value = RecordingUiState.Created(recording)
                }
                .onFailure { e ->
                    _uiState.value = RecordingUiState.Error(e.message ?: "생성 실패")
                }
        }
    }

    // 녹화 시작
    fun startRecording() {
        val recordingId = _currentRecording.value?.id ?: return

        viewModelScope.launch {
            repository.startRecording(recordingId)
                .onSuccess { recording ->
                    _currentRecording.value = recording
                    _uiState.value = RecordingUiState.Recording(recording)
                    startFlushTimer()
                }
                .onFailure { e ->
                    _uiState.value = RecordingUiState.Error(e.message ?: "시작 실패")
                }
        }
    }

    // 녹화 중지
    fun stopRecording() {
        val recordingId = _currentRecording.value?.id ?: return

        viewModelScope.launch {
            // 버퍼에 남은 이벤트 전송
            flushEvents()
            flushJob?.cancel()

            repository.stopRecording(recordingId)
                .onSuccess { recording ->
                    _currentRecording.value = recording
                    _uiState.value = RecordingUiState.Completed(recording)
                }
                .onFailure { e ->
                    _uiState.value = RecordingUiState.Error(e.message ?: "중지 실패")
                }
        }
    }

    // 이벤트 추가 (AccessibilityService에서 호출)
    fun addEvent(event: AccessibilityEventData) {
        synchronized(eventBuffer) {
            eventBuffer.add(event)

            // 버퍼가 가득 차면 전송
            if (eventBuffer.size >= BUFFER_SIZE) {
                viewModelScope.launch { flushEvents() }
            }
        }
    }

    // 버퍼의 이벤트들을 서버로 전송
    private suspend fun flushEvents() {
        val recordingId = _currentRecording.value?.id ?: return

        val eventsToSend = synchronized(eventBuffer) {
            if (eventBuffer.isEmpty()) return
            val copy = eventBuffer.toList()
            eventBuffer.clear()
            copy
        }

        repository.saveEvents(recordingId, eventsToSend)
            .onFailure {
                // 실패 시 버퍼에 다시 추가 (재시도용)
                synchronized(eventBuffer) {
                    eventBuffer.addAll(0, eventsToSend)
                }
            }
    }

    // 주기적 전송 타이머 시작
    private fun startFlushTimer() {
        flushJob = viewModelScope.launch {
            while (isActive) {
                delay(FLUSH_INTERVAL)
                flushEvents()
            }
        }
    }

    // 녹화 선택 (목록에서)
    fun selectRecording(recording: RecordingResponse) {
        _currentRecording.value = recording
    }

    // 상태 초기화
    fun resetState() {
        _uiState.value = RecordingUiState.Idle
        _currentRecording.value = null
        eventBuffer.clear()
        flushJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        flushJob?.cancel()
    }
}

sealed class RecordingUiState {
    object Idle : RecordingUiState()
    object Loading : RecordingUiState()
    data class Created(val recording: RecordingResponse) : RecordingUiState()
    data class Recording(val recording: RecordingResponse) : RecordingUiState()
    data class Completed(val recording: RecordingResponse) : RecordingUiState()
    data class Error(val message: String) : RecordingUiState()
}
