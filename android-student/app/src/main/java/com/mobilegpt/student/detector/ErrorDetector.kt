package com.mobilegpt.student.detector

import android.util.Log
import com.mobilegpt.student.detector.models.*
import com.mobilegpt.student.domain.model.SubtaskDetail

/**
 * UI 비교 중 발생하는 오류를 감지하는 클래스
 *
 * 감지하는 오류 타입:
 * - WRONG_APP: 예상되는 앱이 아닌 다른 앱으로 이동
 * - FROZEN_SCREEN: 일정 시간(3초) 동안 UI 변화 없음
 * - WRONG_CLICK: 예상되지 않은 UI 요소 클릭 (이벤트 기반으로 감지)
 *
 * 오류 발생 시 서버에 보고됩니다 (강사 대시보드에서 확인).
 */
class ErrorDetector {

    companion object {
        private const val TAG = "ErrorDetector"

        /**
         * FROZEN_SCREEN 판정 임계값 (밀리초)
         * 이 시간 동안 UI 변화가 없으면 FROZEN_SCREEN으로 판정
         */
        const val FROZEN_THRESHOLD_MS = 3000L

        /**
         * 오류 보고 임계값
         * 연속으로 이 횟수만큼 동일 오류 발생 시 서버에 보고
         */
        const val ERROR_REPORT_THRESHOLD = 5

        /**
         * WRONG_CLICK은 즉시 보고 (횟수 제한 없음)
         */
        const val WRONG_CLICK_IMMEDIATE_REPORT = true
    }

    // 마지막 스냅샷 (FROZEN_SCREEN 감지용)
    private var lastSnapshot: UISnapshot? = null

    // 마지막 UI 변화 시간
    private var lastChangeTime: Long = System.currentTimeMillis()

    // 오류 카운터 (연속 발생 횟수 추적)
    private val errorCounts = mutableMapOf<ErrorType, Int>()

    // 이미 보고된 오류 (중복 보고 방지)
    private val reportedErrors = mutableSetOf<String>()

    /**
     * 오류 감지 수행
     *
     * @param snapshot 현재 화면 스냅샷
     * @param expectation 현재 단계의 기대 조건
     * @return 감지된 오류 (없으면 null)
     */
    fun detectError(
        snapshot: UISnapshot,
        expectation: StepExpectation
    ): DetectedError? {
        val now = System.currentTimeMillis()

        // 1. WRONG_APP 감지: 패키지가 다른 경우
        if (expectation.expectedPackage.isNotBlank() &&
            !snapshot.matchesPackage(expectation.expectedPackage)) {

            Log.d(TAG, "WRONG_APP detected: expected=${expectation.expectedPackage}, " +
                    "actual=${snapshot.packageName}")

            incrementErrorCount(ErrorType.WRONG_APP)

            return DetectedError(
                type = ErrorType.WRONG_APP,
                expectedPackage = expectation.expectedPackage,
                actualPackage = snapshot.packageName,
                subtaskId = expectation.subtaskId
            )
        }

        // 2. FROZEN_SCREEN 감지: 일정 시간 동안 UI 변화 없음
        val isSameAsLast = snapshot.isSameAs(lastSnapshot)

        if (isSameAsLast) {
            val timeSinceLastChange = now - lastChangeTime
            if (timeSinceLastChange > FROZEN_THRESHOLD_MS) {
                Log.d(TAG, "FROZEN_SCREEN detected: no change for ${timeSinceLastChange}ms")

                incrementErrorCount(ErrorType.FROZEN_SCREEN)

                return DetectedError(
                    type = ErrorType.FROZEN_SCREEN,
                    subtaskId = expectation.subtaskId,
                    additionalInfo = "No UI change for ${timeSinceLastChange}ms"
                )
            }
        } else {
            // UI가 변경되었으면 시간 리셋
            lastChangeTime = now
            clearErrorCount(ErrorType.FROZEN_SCREEN)
        }

        // 현재 스냅샷 저장
        lastSnapshot = snapshot

        // WRONG_APP 오류가 해결되었으면 카운터 리셋
        if (expectation.expectedPackage.isBlank() ||
            snapshot.matchesPackage(expectation.expectedPackage)) {
            clearErrorCount(ErrorType.WRONG_APP)
        }

        return null
    }

    /**
     * SubtaskDetail을 사용하여 오류 감지 (편의 메서드)
     */
    fun detectError(snapshot: UISnapshot, subtask: SubtaskDetail): DetectedError? {
        val expectation = StepExpectation.fromSubtask(subtask)
        return detectError(snapshot, expectation)
    }

    /**
     * WRONG_CLICK 오류 감지
     * (이벤트 기반 - AccessibilityEvent에서 호출)
     *
     * @param clickedViewId 클릭된 viewId
     * @param clickedText 클릭된 요소의 텍스트
     * @param expectation 현재 단계의 기대 조건
     * @return 감지된 오류 (없으면 null)
     */
    fun detectWrongClick(
        clickedViewId: String?,
        clickedText: String?,
        expectation: StepExpectation
    ): DetectedError? {
        // 기대되는 KeyView가 없으면 WRONG_CLICK 감지 불가
        if (!expectation.hasKeyViews) return null

        // 클릭된 요소가 기대하는 KeyView 중 하나인지 확인
        val matchesAnyKeyView = expectation.expectedKeyViews.any { keyView ->
            (keyView.viewId != null && clickedViewId?.contains(keyView.viewId) == true) ||
            (keyView.text != null && clickedText?.contains(keyView.text, ignoreCase = true) == true)
        }

        if (!matchesAnyKeyView && (clickedViewId != null || clickedText != null)) {
            Log.d(TAG, "WRONG_CLICK detected: clicked viewId=$clickedViewId, text=$clickedText")

            return DetectedError(
                type = ErrorType.WRONG_CLICK,
                subtaskId = expectation.subtaskId,
                additionalInfo = "Clicked: viewId=$clickedViewId, text=$clickedText"
            )
        }

        return null
    }

    /**
     * 오류가 보고 임계값에 도달했는지 확인
     *
     * @param errorType 오류 타입
     * @return 보고해야 하면 true
     */
    fun shouldReport(errorType: ErrorType): Boolean {
        // WRONG_CLICK은 즉시 보고
        if (errorType == ErrorType.WRONG_CLICK && WRONG_CLICK_IMMEDIATE_REPORT) {
            return true
        }

        val count = errorCounts[errorType] ?: 0
        return count >= ERROR_REPORT_THRESHOLD
    }

    /**
     * 오류 보고 완료 처리 (중복 방지)
     */
    fun markAsReported(error: DetectedError) {
        val key = "${error.type}_${error.subtaskId}_${error.timestamp / 10000}"
        reportedErrors.add(key)
        clearErrorCount(error.type)
    }

    /**
     * 이미 보고된 오류인지 확인
     */
    fun isAlreadyReported(error: DetectedError): Boolean {
        val key = "${error.type}_${error.subtaskId}_${error.timestamp / 10000}"
        return reportedErrors.contains(key)
    }

    /**
     * 오류 카운터 증가
     */
    private fun incrementErrorCount(errorType: ErrorType) {
        val current = errorCounts[errorType] ?: 0
        errorCounts[errorType] = current + 1
    }

    /**
     * 특정 오류 카운터 초기화
     */
    private fun clearErrorCount(errorType: ErrorType) {
        errorCounts.remove(errorType)
    }

    /**
     * 모든 상태 초기화 (새 세션 시작 시)
     */
    fun reset() {
        lastSnapshot = null
        lastChangeTime = System.currentTimeMillis()
        errorCounts.clear()
        reportedErrors.clear()
        Log.d(TAG, "ErrorDetector reset")
    }

    /**
     * 현재 마지막 변화로부터 경과된 시간 반환
     */
    fun getTimeSinceLastChange(): Long {
        return System.currentTimeMillis() - lastChangeTime
    }
}
