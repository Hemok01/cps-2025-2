package com.mobilegpt.student.detector

import android.util.Log
import com.mobilegpt.student.data.local.SessionPreferences
import com.mobilegpt.student.data.repository.SessionRepository
import com.mobilegpt.student.domain.model.SubtaskDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 단계 완료 여부를 판단하고 서버에 보고하는 클래스
 *
 * 핵심 기능:
 * 1. 현재 단계(SubtaskDetail)와 UI 시그니처를 비교하여 완료 여부 판단
 * 2. 완료 시 서버에 보고 (강사 대시보드에 표시)
 * 3. 중복 보고 방지
 */
@Singleton
class StepCompletionChecker @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val sessionPreferences: SessionPreferences
) {

    companion object {
        private const val TAG = "StepCompletionChecker"

        // 매칭 임계값 (이 비율 이상이면 완료로 판단)
        private const val MATCH_THRESHOLD = 0.7f
    }

    private val matcher = StepMatcher()

    // 완료 상태 캐시 (중복 보고 방지)
    private val completedSubtasks = mutableSetOf<Int>()

    // 현재 완료 상태를 외부에 노출
    private val _completionState = MutableStateFlow<CompletionState>(CompletionState.NotStarted)
    val completionState: StateFlow<CompletionState> = _completionState.asStateFlow()

    /**
     * 현재 단계가 완료되었는지 확인
     *
     * @param currentSubtask 현재 진행 중인 단계
     * @param signature 현재 UI 시그니처
     * @param eventType AccessibilityEvent 타입
     * @return 완료 결과
     */
    fun checkCompletion(
        currentSubtask: SubtaskDetail,
        signature: Map<String, String?>,
        eventType: Int
    ): CompletionResult {
        // 이미 완료된 단계는 스킵
        if (completedSubtasks.contains(currentSubtask.id)) {
            Log.d(TAG, "Subtask ${currentSubtask.id} already completed, skipping")
            return CompletionResult(
                isCompleted = true,
                isNewCompletion = false,
                matchResult = null,
                subtaskId = currentSubtask.id
            )
        }

        // 매칭 수행
        val matchResult = matcher.matchSingleStep(currentSubtask, signature, eventType)

        Log.d(TAG, "Match result for ${currentSubtask.title}: " +
                "matched=${matchResult.isMatched}, ratio=${matchResult.matchRatio}")

        // 완료 여부 판단
        val isCompleted = matchResult.isMatched || matchResult.matchRatio >= MATCH_THRESHOLD

        if (isCompleted) {
            completedSubtasks.add(currentSubtask.id)
            _completionState.value = CompletionState.Completed(currentSubtask.id)

            Log.i(TAG, "Step completed! Subtask: ${currentSubtask.id} (${currentSubtask.title})")
        }

        return CompletionResult(
            isCompleted = isCompleted,
            isNewCompletion = isCompleted && !completedSubtasks.contains(currentSubtask.id),
            matchResult = matchResult,
            subtaskId = currentSubtask.id
        )
    }

    /**
     * 완료 상태를 서버에 보고
     *
     * @param subtaskId 완료한 단계 ID
     * @param sessionId 세션 ID
     * @return 보고 성공 여부
     */
    suspend fun reportCompletion(subtaskId: Int, sessionId: Int): Result<Boolean> {
        return try {
            Log.d(TAG, "Reporting completion: subtaskId=$subtaskId, sessionId=$sessionId")

            val result = sessionRepository.reportStepCompletion(
                sessionId = sessionId,
                subtaskId = subtaskId
            )

            result.onSuccess {
                Log.i(TAG, "Completion reported successfully for subtask $subtaskId")
                // 로컬 완료 상태 저장
                sessionPreferences.saveSubtaskCompletionStatus(subtaskId, true)
            }.onFailure { error ->
                Log.e(TAG, "Failed to report completion: ${error.message}")
            }

            result
        } catch (e: Exception) {
            Log.e(TAG, "Error reporting completion", e)
            Result.failure(e)
        }
    }

    /**
     * 특정 단계가 완료되었는지 확인 (로컬 캐시)
     */
    fun isCompleted(subtaskId: Int): Boolean {
        return completedSubtasks.contains(subtaskId) ||
                sessionPreferences.getSubtaskCompletionStatus(subtaskId)
    }

    /**
     * 완료 상태 초기화 (새 세션 시작 시)
     */
    fun resetCompletionStatus() {
        completedSubtasks.clear()
        _completionState.value = CompletionState.NotStarted
        Log.d(TAG, "Completion status reset")
    }

    /**
     * 서버에서 기존 완료 상태 동기화
     */
    fun syncCompletionStatus(completedIds: List<Int>) {
        completedSubtasks.clear()
        completedSubtasks.addAll(completedIds)
        Log.d(TAG, "Synced ${completedIds.size} completed subtasks")
    }
}

/**
 * 완료 결과 데이터 클래스
 */
data class CompletionResult(
    val isCompleted: Boolean,
    val isNewCompletion: Boolean,  // 새로 완료된 것인지 (중복 체크용)
    val matchResult: MatchResult?,
    val subtaskId: Int
)

/**
 * 완료 상태를 나타내는 sealed class
 */
sealed class CompletionState {
    object NotStarted : CompletionState()
    object InProgress : CompletionState()
    data class Completed(val subtaskId: Int) : CompletionState()
    data class Error(val message: String) : CompletionState()
}
