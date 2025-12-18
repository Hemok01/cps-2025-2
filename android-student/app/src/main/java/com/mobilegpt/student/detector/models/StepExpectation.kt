package com.mobilegpt.student.detector.models

import com.mobilegpt.student.domain.model.SubtaskDetail

/**
 * 단계 완료 조건을 정의하는 기대 모델
 *
 * 이 모델은 특정 단계가 완료되었는지 판단하기 위한 조건들을 포함합니다.
 * SubtaskDetail에서 변환되어 생성되며, AdvancedStepMatcher에서 사용됩니다.
 *
 * @property expectedPackage 예상되는 앱 패키지명
 * @property expectedActivity 예상되는 Activity 이름 (선택적)
 * @property expectedKeyViews 화면에 존재해야 할 KeyView 목록
 * @property targetAction 예상되는 액션 타입 (CLICK, LONG_CLICK, SCROLL 등)
 * @property subtaskId 원본 Subtask의 ID
 * @property subtaskTitle 원본 Subtask의 제목
 */
data class StepExpectation(
    val expectedPackage: String,
    val expectedActivity: String? = null,
    val expectedKeyViews: List<KeyView> = emptyList(),
    val targetAction: String? = null,
    val subtaskId: Int,
    val subtaskTitle: String
) {
    /**
     * 이 기대 조건이 유효한지 확인
     * (최소한 패키지명이 있어야 함)
     */
    val isValid: Boolean
        get() = expectedPackage.isNotBlank()

    /**
     * 매칭에 사용할 수 있는 KeyView가 있는지 확인
     */
    val hasKeyViews: Boolean
        get() = expectedKeyViews.any { it.isValid }

    companion object {
        /**
         * SubtaskDetail에서 StepExpectation 생성
         */
        fun fromSubtask(subtask: SubtaskDetail): StepExpectation {
            return StepExpectation(
                expectedPackage = subtask.effectivePackage ?: "",
                expectedActivity = null,  // Activity 정보는 보통 없음
                expectedKeyViews = KeyView.listFromSubtask(subtask),
                targetAction = subtask.targetAction,
                subtaskId = subtask.id,
                subtaskTitle = subtask.title
            )
        }

        /**
         * 여러 SubtaskDetail에서 StepExpectation 목록 생성
         */
        fun listFromSubtasks(subtasks: List<SubtaskDetail>): List<StepExpectation> {
            return subtasks.map { fromSubtask(it) }
        }
    }
}

/**
 * 매칭 결과를 나타내는 데이터 클래스
 *
 * @property isMatched 완전 매칭 여부 (UI 상태 + 액션 타입 모두 매칭)
 * @property matchRatio 매칭 비율 (0.0 ~ 1.0)
 * @property matchedKeyViews 매칭된 KeyView 목록
 * @property unmatchedKeyViews 매칭되지 않은 KeyView 목록
 * @property packageMatched 패키지명 매칭 여부
 * @property actionMismatch 행동 타입 불일치 여부 (CLICK vs SCROLL 등)
 * @property uiStateMatched UI 상태 매칭 여부 (패키지 + KeyView, 액션 타입 무관)
 * @property subtaskId 원본 Subtask ID
 * @property subtaskTitle 원본 Subtask 제목
 */
data class AdvancedMatchResult(
    val isMatched: Boolean,
    val matchRatio: Float,
    val matchedKeyViews: List<KeyView>,
    val unmatchedKeyViews: List<KeyView>,
    val packageMatched: Boolean,
    val subtaskId: Int,
    val subtaskTitle: String,
    val actionMismatch: Boolean = false,  // 행동 타입 불일치 플래그
    val uiStateMatched: Boolean = false   // UI 상태 매칭 여부 (액션 타입 무관)
) {
    /**
     * 부분 매칭 여부 (일부 KeyView만 매칭됨)
     */
    val isPartialMatch: Boolean
        get() = !isMatched && matchedKeyViews.isNotEmpty()

    /**
     * 매칭 실패 원인 설명
     */
    val failureReason: String?
        get() = when {
            isMatched -> null
            !packageMatched -> "다른 앱에 있음"
            unmatchedKeyViews.isNotEmpty() -> "UI 요소를 찾을 수 없음"
            uiStateMatched && actionMismatch -> "올바른 화면, 클릭 대기 중"
            actionMismatch -> "행동 타입 불일치"
            else -> "알 수 없음"
        }

    /**
     * UI 상태는 매칭되었지만 액션 타입만 다른 경우
     * (사용자가 올바른 화면에 있지만 아직 클릭하지 않음)
     */
    val isWaitingForAction: Boolean
        get() = uiStateMatched && actionMismatch

    companion object {
        /**
         * 매칭 실패 결과 생성
         */
        fun noMatch(expectation: StepExpectation): AdvancedMatchResult {
            return AdvancedMatchResult(
                isMatched = false,
                matchRatio = 0f,
                matchedKeyViews = emptyList(),
                unmatchedKeyViews = expectation.expectedKeyViews,
                packageMatched = false,
                subtaskId = expectation.subtaskId,
                subtaskTitle = expectation.subtaskTitle,
                actionMismatch = false
            )
        }

        /**
         * 패키지만 매칭된 결과 생성
         */
        fun packageOnlyMatch(expectation: StepExpectation): AdvancedMatchResult {
            return AdvancedMatchResult(
                isMatched = false,
                matchRatio = 0.3f,  // 패키지만 매칭되면 30%
                matchedKeyViews = emptyList(),
                unmatchedKeyViews = expectation.expectedKeyViews,
                packageMatched = true,
                subtaskId = expectation.subtaskId,
                subtaskTitle = expectation.subtaskTitle,
                actionMismatch = false
            )
        }

        /**
         * 완전 매칭 결과 생성
         */
        fun fullMatch(expectation: StepExpectation, matchedKeyViews: List<KeyView>): AdvancedMatchResult {
            return AdvancedMatchResult(
                isMatched = true,
                matchRatio = 1f,
                matchedKeyViews = matchedKeyViews,
                unmatchedKeyViews = emptyList(),
                packageMatched = true,
                subtaskId = expectation.subtaskId,
                subtaskTitle = expectation.subtaskTitle,
                actionMismatch = false
            )
        }
    }
}
