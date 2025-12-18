package com.example.mobilegpt_instructor.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobilegpt_instructor.data.model.AnalysisStatusResponse
import com.example.mobilegpt_instructor.data.model.AnalyzedStep
import com.example.mobilegpt_instructor.data.model.ConvertToLectureResponse
import com.example.mobilegpt_instructor.data.repository.RecordingRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * GPT 분석 및 강의 변환을 관리하는 ViewModel
 * 분석 요청, 상태 폴링, 단계 수정, 강의 변환 처리
 */
class AnalysisViewModel : ViewModel() {

    private val repository = RecordingRepository()

    // UI 상태
    private val _uiState = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Idle)
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    // 분석 결과 (수정 가능한 단계 목록)
    private val _steps = MutableStateFlow<List<AnalyzedStep>>(emptyList())
    val steps: StateFlow<List<AnalyzedStep>> = _steps.asStateFlow()

    // 폴링 Job
    private var pollingJob: Job? = null
    private var currentRecordingId: Int? = null

    companion object {
        private const val POLLING_INTERVAL = 2000L // 2초마다 상태 확인
    }

    // GPT 분석 시작
    fun startAnalysis(recordingId: Int) {
        currentRecordingId = recordingId

        viewModelScope.launch {
            _uiState.value = AnalysisUiState.Starting

            repository.analyzeRecording(recordingId)
                .onSuccess {
                    _uiState.value = AnalysisUiState.Processing
                    startPolling(recordingId)
                }
                .onFailure { e ->
                    _uiState.value = AnalysisUiState.Error(e.message ?: "분석 시작 실패")
                }
        }
    }

    // 분석 상태 폴링 시작
    private fun startPolling(recordingId: Int) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(POLLING_INTERVAL)
                checkAnalysisStatus(recordingId)

                // 완료 또는 실패 상태면 폴링 중단
                when (_uiState.value) {
                    is AnalysisUiState.Completed,
                    is AnalysisUiState.Error -> break
                    else -> {}
                }
            }
        }
    }

    // 분석 상태 확인
    private suspend fun checkAnalysisStatus(recordingId: Int) {
        repository.getAnalysisStatus(recordingId)
            .onSuccess { status ->
                handleStatusResponse(status)
            }
            .onFailure { e ->
                _uiState.value = AnalysisUiState.Error(e.message ?: "상태 조회 실패")
            }
    }

    // 상태 응답 처리
    private fun handleStatusResponse(status: AnalysisStatusResponse) {
        when (status.status) {
            "PROCESSING" -> {
                _uiState.value = AnalysisUiState.Processing
            }
            "ANALYZED" -> {
                val result = status.analysisResult
                if (result != null) {
                    _steps.value = result.steps
                    _uiState.value = AnalysisUiState.Completed(
                        totalSteps = result.totalSteps,
                        analyzedEvents = result.analyzedEventsCount
                    )
                } else {
                    _uiState.value = AnalysisUiState.Error("분석 결과가 없습니다")
                }
                pollingJob?.cancel()
            }
            "FAILED" -> {
                _uiState.value = AnalysisUiState.Error(
                    status.analysisError ?: "분석 실패"
                )
                pollingJob?.cancel()
            }
            else -> {
                // RECORDING, COMPLETED 등 다른 상태는 무시
            }
        }
    }

    // 단계 수정
    fun updateStep(index: Int, updatedStep: AnalyzedStep) {
        val currentSteps = _steps.value.toMutableList()
        if (index in currentSteps.indices) {
            currentSteps[index] = updatedStep
            _steps.value = currentSteps
        }
    }

    // 단계 삭제
    fun removeStep(index: Int) {
        val currentSteps = _steps.value.toMutableList()
        if (index in currentSteps.indices) {
            currentSteps.removeAt(index)
            // step 번호 재정렬
            _steps.value = currentSteps.mapIndexed { i, step ->
                step.copy(step = i + 1)
            }
        }
    }

    // 수정된 단계 저장
    fun saveSteps() {
        val recordingId = currentRecordingId ?: return

        viewModelScope.launch {
            _uiState.value = AnalysisUiState.Saving

            repository.updateSteps(recordingId, _steps.value)
                .onSuccess {
                    _uiState.value = AnalysisUiState.Saved
                }
                .onFailure { e ->
                    _uiState.value = AnalysisUiState.Error(e.message ?: "저장 실패")
                }
        }
    }

    // 강의로 변환
    fun convertToLecture(title: String, description: String = "") {
        val recordingId = currentRecordingId ?: return

        viewModelScope.launch {
            _uiState.value = AnalysisUiState.Converting

            repository.convertToLecture(recordingId, title, description)
                .onSuccess { response ->
                    _uiState.value = AnalysisUiState.Converted(response)
                }
                .onFailure { e ->
                    _uiState.value = AnalysisUiState.Error(e.message ?: "변환 실패")
                }
        }
    }

    // 상태 초기화
    fun resetState() {
        pollingJob?.cancel()
        _uiState.value = AnalysisUiState.Idle
        _steps.value = emptyList()
        currentRecordingId = null
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}

sealed class AnalysisUiState {
    object Idle : AnalysisUiState()
    object Starting : AnalysisUiState()
    object Processing : AnalysisUiState()
    data class Completed(val totalSteps: Int, val analyzedEvents: Int) : AnalysisUiState()
    object Saving : AnalysisUiState()
    object Saved : AnalysisUiState()
    object Converting : AnalysisUiState()
    data class Converted(val response: ConvertToLectureResponse) : AnalysisUiState()
    data class Error(val message: String) : AnalysisUiState()
}
