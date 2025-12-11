package com.mobilegpt.student.detector

import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.mobilegpt.student.detector.models.*
import com.mobilegpt.student.domain.model.SubtaskDetail

/**
 * UISnapshot 기반 고급 단계 매칭 알고리즘
 *
 * UIComparison2 프로젝트의 매칭 로직을 참고하여 구현.
 * 기존 StepMatcher를 완전 대체합니다.
 *
 * 매칭 전략:
 * 1. 행동 타입 확인 (CLICK, SCROLL 등 - 올바른 행동인지)
 * 2. 패키지명 확인 (대소문자 무시, 부분 일치)
 * 3. KeyView 매칭 (viewId 또는 text)
 *    - 정확한 일치
 *    - 접미사 일치 (/button_ok)
 *    - 부분 포함
 * 4. 하나의 KeyView만 매칭되어도 성공으로 판정
 */
class AdvancedStepMatcher {

    companion object {
        private const val TAG = "AdvancedStepMatcher"

        /**
         * 매칭 성공 임계값
         * KeyView가 없는 경우 패키지만 매칭되면 성공으로 판정
         */
        private const val PACKAGE_ONLY_THRESHOLD = 0.3f

        /**
         * KeyView가 있는 경우 최소 하나는 매칭되어야 함
         */
        private const val MIN_KEYVIEW_MATCH_FOR_SUCCESS = 1

        /**
         * targetAction과 AccessibilityEvent 타입 매핑
         */
        private val ACTION_EVENT_MAP = mapOf(
            "CLICK" to listOf(
                AccessibilityEvent.TYPE_VIEW_CLICKED,
                AccessibilityEvent.TYPE_VIEW_SELECTED
            ),
            "LONG_CLICK" to listOf(
                AccessibilityEvent.TYPE_VIEW_LONG_CLICKED
            ),
            "SCROLL" to listOf(
                AccessibilityEvent.TYPE_VIEW_SCROLLED,
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            ),
            "INPUT" to listOf(
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
                AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED
            ),
            "NAVIGATE" to listOf(
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                AccessibilityEvent.TYPE_WINDOWS_CHANGED
            )
        )

        /**
         * 이벤트 타입이 기대하는 행동과 일치하는지 확인
         */
        fun isActionMatched(expectedAction: String?, actualEventType: Int): Boolean {
            // 기대 행동이 없으면 모든 이벤트 허용
            if (expectedAction.isNullOrBlank()) return true

            val allowedEvents = ACTION_EVENT_MAP[expectedAction.uppercase()]
            // 매핑이 없으면 모든 이벤트 허용 (알 수 없는 행동 타입)
            if (allowedEvents == null) return true

            return actualEventType in allowedEvents
        }

        /**
         * 이벤트 타입 코드를 사람이 읽을 수 있는 이름으로 변환
         */
        fun getEventTypeName(eventType: Int?): String {
            return when (eventType) {
                AccessibilityEvent.TYPE_VIEW_CLICKED -> "CLICKED"
                AccessibilityEvent.TYPE_VIEW_LONG_CLICKED -> "LONG_CLICKED"
                AccessibilityEvent.TYPE_VIEW_SELECTED -> "SELECTED"
                AccessibilityEvent.TYPE_VIEW_FOCUSED -> "FOCUSED"
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> "TEXT_CHANGED"
                AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> "TEXT_SELECTION_CHANGED"
                AccessibilityEvent.TYPE_VIEW_SCROLLED -> "SCROLLED"
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> "WINDOW_STATE_CHANGED"
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> "WINDOW_CONTENT_CHANGED"
                AccessibilityEvent.TYPE_WINDOWS_CHANGED -> "WINDOWS_CHANGED"
                null -> "NULL"
                else -> "UNKNOWN($eventType)"
            }
        }
    }

    /**
     * UISnapshot과 StepExpectation을 비교하여 매칭 결과 반환
     *
     * @param snapshot 현재 화면 스냅샷
     * @param expectation 단계 기대 조건
     * @param eventType 발생한 이벤트 타입 (선택적, 행동 검증용)
     * @return 매칭 결과
     */
    fun match(
        snapshot: UISnapshot,
        expectation: StepExpectation,
        eventType: Int? = null
    ): AdvancedMatchResult {
        Log.d(TAG, "=== Matching step: ${expectation.subtaskTitle} ===")
        Log.d(TAG, "  Expected package: ${expectation.expectedPackage}")
        Log.d(TAG, "  Actual package: ${snapshot.packageName}")
        Log.d(TAG, "  Expected action: ${expectation.targetAction}")
        Log.d(TAG, "  Actual eventType: $eventType (${getEventTypeName(eventType)})")
        Log.d(TAG, "  KeyViews count: ${expectation.expectedKeyViews.size}")

        // 0. 행동 타입 확인 (이벤트 타입이 제공된 경우)
        // ★ 변경: 액션 불일치 시에도 UI 상태 체크는 진행하되, 최종 결과에 반영
        val actionMatched = if (eventType != null && expectation.targetAction != null) {
            isActionMatched(expectation.targetAction, eventType)
        } else {
            true  // 이벤트 타입이 없으면 매칭으로 간주
        }
        Log.d(TAG, "  Action match: $actionMatched")

        // 1. 패키지명 확인
        val packageMatched = snapshot.matchesPackage(expectation.expectedPackage)

        if (!packageMatched && expectation.expectedPackage.isNotBlank()) {
            Log.d(TAG, "  Result: Package mismatch")
            return AdvancedMatchResult.noMatch(expectation)
        }

        // 2. KeyView 매칭
        if (expectation.expectedKeyViews.isEmpty()) {
            // KeyView가 없으면 패키지 + 액션 매칭으로 판단
            // ★ 중요: 클릭 액션이 필요한 경우 클릭 이벤트일 때만 완료로 인정
            val uiMatched = packageMatched
            val finalMatched = uiMatched && actionMatched

            Log.d(TAG, "  Result: No KeyViews - uiMatched=$uiMatched, actionMatched=$actionMatched, final=$finalMatched")
            return AdvancedMatchResult(
                isMatched = finalMatched,
                matchRatio = if (uiMatched) 1f else 0f,
                matchedKeyViews = emptyList(),
                unmatchedKeyViews = emptyList(),
                packageMatched = packageMatched,
                subtaskId = expectation.subtaskId,
                subtaskTitle = expectation.subtaskTitle,
                actionMismatch = !actionMatched,
                uiStateMatched = uiMatched
            )
        }

        // KeyView 매칭 수행
        val matchedKeyViews = mutableListOf<KeyView>()
        val unmatchedKeyViews = mutableListOf<KeyView>()

        for (keyView in expectation.expectedKeyViews) {
            if (keyView.existsIn(snapshot)) {
                matchedKeyViews.add(keyView)
                Log.d(TAG, "  KeyView matched: viewId=${keyView.viewId}, text=${keyView.text}")
            } else {
                unmatchedKeyViews.add(keyView)
                Log.d(TAG, "  KeyView NOT matched: viewId=${keyView.viewId}, text=${keyView.text}")
            }
        }

        // 3. 결과 계산
        val matchRatio = if (expectation.expectedKeyViews.isNotEmpty()) {
            matchedKeyViews.size.toFloat() / expectation.expectedKeyViews.size
        } else {
            if (packageMatched) 1f else 0f
        }

        // UI 상태 매칭: 패키지 + 최소 하나의 KeyView
        val uiStateMatched = packageMatched && matchedKeyViews.size >= MIN_KEYVIEW_MATCH_FOR_SUCCESS

        // ★ 최종 매칭: UI 상태 매칭 + 액션 타입 매칭
        // 클릭 액션이 기대되면 클릭 이벤트가 발생해야 완료로 인정
        val isMatched = uiStateMatched && actionMatched

        Log.d(TAG, "  Result: uiStateMatched=$uiStateMatched, actionMatched=$actionMatched, final=$isMatched")
        Log.d(TAG, "  KeyViews: matched=${matchedKeyViews.size}/${expectation.expectedKeyViews.size}")

        return AdvancedMatchResult(
            isMatched = isMatched,
            matchRatio = matchRatio,
            matchedKeyViews = matchedKeyViews,
            unmatchedKeyViews = unmatchedKeyViews,
            packageMatched = packageMatched,
            subtaskId = expectation.subtaskId,
            subtaskTitle = expectation.subtaskTitle,
            actionMismatch = !actionMatched,
            uiStateMatched = uiStateMatched
        )
    }

    /**
     * SubtaskDetail과 UISnapshot을 직접 비교 (편의 메서드)
     */
    fun matchSubtask(snapshot: UISnapshot, subtask: SubtaskDetail): AdvancedMatchResult {
        val expectation = StepExpectation.fromSubtask(subtask)
        return match(snapshot, expectation)
    }

    /**
     * 여러 StepExpectation 중 가장 잘 매칭되는 것 찾기
     *
     * @param snapshot 현재 화면 스냅샷
     * @param expectations 기대 조건 목록
     * @return 가장 높은 매칭률을 가진 결과 (또는 null)
     */
    fun findBestMatch(
        snapshot: UISnapshot,
        expectations: List<StepExpectation>
    ): AdvancedMatchResult? {
        return expectations
            .map { match(snapshot, it) }
            .maxByOrNull { it.matchRatio }
            ?.takeIf { it.matchRatio > 0f }
    }

    /**
     * 현재 단계가 완료되었는지 확인
     *
     * @param snapshot 현재 화면 스냅샷
     * @param currentSubtask 현재 진행 중인 단계
     * @return 완료 여부
     */
    fun isStepCompleted(snapshot: UISnapshot, currentSubtask: SubtaskDetail): Boolean {
        val result = matchSubtask(snapshot, currentSubtask)
        return result.isMatched
    }
}
