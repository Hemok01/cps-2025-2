package com.mobilegpt.student.detector

import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.mobilegpt.student.domain.model.SubtaskDetail

/**
 * 단계 매칭 알고리즘
 * GitHub UI_comparison 프로젝트의 StepMatcher 기반
 *
 * 현재 UI 시그니처가 목표 단계(SubtaskDetail)와 일치하는지 판단
 */
class StepMatcher {

    companion object {
        private const val TAG = "StepMatcher"

        // 이벤트 타입 상수 (AccessibilityEvent)
        const val EVENT_TYPE_CLICK = 1                      // TYPE_VIEW_CLICKED
        const val EVENT_TYPE_LONG_CLICK = 2                 // TYPE_VIEW_LONG_CLICKED
        const val EVENT_TYPE_TEXT_CHANGED = 16              // TYPE_VIEW_TEXT_CHANGED
        const val EVENT_TYPE_WINDOW_CHANGED = 32            // TYPE_WINDOW_STATE_CHANGED
        const val EVENT_TYPE_WINDOW_CONTENT_CHANGED = 4096  // TYPE_WINDOW_CONTENT_CHANGED
    }

    /**
     * 이벤트 타입별 매칭에 사용할 필드 규칙
     * Key: 이벤트 타입
     * Value: 비교할 필드 리스트
     */
    private val matchingRules: Map<Int, List<String>> = mapOf(
        EVENT_TYPE_CLICK to listOf("package", "viewId", "className"),
        EVENT_TYPE_LONG_CLICK to listOf("package", "viewId", "className"),
        EVENT_TYPE_TEXT_CHANGED to listOf("package", "viewId", "className"),
        EVENT_TYPE_WINDOW_CHANGED to listOf("package", "className"),
        EVENT_TYPE_WINDOW_CONTENT_CHANGED to listOf("package")
    )

    /**
     * 기본 매칭 필드 (규칙이 없는 이벤트 타입용)
     */
    private val defaultMatchingFields = listOf("package", "className")

    /**
     * 단일 단계 매칭
     *
     * @param subtask 목표 단계 정보
     * @param signature 현재 UI 시그니처
     * @param eventType 이벤트 타입 (AccessibilityEvent.eventType)
     * @return 매칭 결과
     */
    fun matchSingleStep(
        subtask: SubtaskDetail,
        signature: Map<String, String?>,
        eventType: Int
    ): MatchResult {
        // 매칭할 필드 결정
        val fieldsToMatch = matchingRules[eventType] ?: defaultMatchingFields

        Log.d(TAG, "Matching step: ${subtask.title}, eventType: $eventType")
        Log.d(TAG, "Fields to match: $fieldsToMatch")

        // 각 필드 매칭
        val matchedFields = mutableListOf<String>()
        val unmatchedFields = mutableListOf<String>()

        for (field in fieldsToMatch) {
            val expected = getExpectedValue(subtask, field)
            val actual = signature[field]

            val isMatched = compareField(expected, actual, field)

            if (isMatched) {
                matchedFields.add(field)
            } else {
                unmatchedFields.add(field)
            }

            Log.d(TAG, "Field '$field': expected='$expected', actual='$actual', matched=$isMatched")
        }

        // 결과 계산
        val matchRatio = if (fieldsToMatch.isNotEmpty()) {
            matchedFields.size.toFloat() / fieldsToMatch.size
        } else 0f

        val isFullMatch = unmatchedFields.isEmpty() && matchedFields.isNotEmpty()

        return MatchResult(
            isMatched = isFullMatch,
            matchRatio = matchRatio,
            matchedFields = matchedFields,
            unmatchedFields = unmatchedFields,
            subtaskId = subtask.id
        )
    }

    /**
     * SubtaskDetail에서 기대값 추출
     */
    private fun getExpectedValue(subtask: SubtaskDetail, field: String): String? {
        return when (field) {
            "package" -> subtask.targetApp
            "viewId" -> subtask.targetAction  // targetAction을 viewId로 매핑
            "className" -> null  // SubtaskDetail에 className 필드 없음 - 추후 확장 가능
            "text" -> subtask.description  // description을 텍스트로 사용
            else -> null
        }
    }

    /**
     * 필드 값 비교
     *
     * @param expected 기대값
     * @param actual 실제값
     * @param field 필드명
     * @return 매칭 여부
     */
    private fun compareField(expected: String?, actual: String?, field: String): Boolean {
        // 기대값이 없으면 해당 필드는 무시 (매칭 성공으로 처리)
        if (expected.isNullOrBlank()) {
            return true
        }

        // 실제값이 없으면 매칭 실패
        if (actual.isNullOrBlank()) {
            return false
        }

        return when (field) {
            "package" -> {
                // 패키지명: 정확히 일치 또는 포함 관계
                actual == expected || actual.contains(expected) || expected.contains(actual)
            }
            "viewId" -> {
                // viewId: 정확히 일치 또는 ID 부분만 비교
                // 예: "com.example:id/button_ok" vs "button_ok"
                if (actual == expected) {
                    true
                } else {
                    val actualId = actual.substringAfterLast("/")
                    val expectedId = expected.substringAfterLast("/")
                    actualId == expectedId || actual.contains(expected) || expected.contains(actual)
                }
            }
            "className" -> {
                // 클래스명: 정확히 일치 또는 단순 클래스명 비교
                if (actual == expected) {
                    true
                } else {
                    val actualSimple = actual.substringAfterLast(".")
                    val expectedSimple = expected.substringAfterLast(".")
                    actualSimple == expectedSimple
                }
            }
            "text", "contentDescription" -> {
                // 텍스트: 포함 관계 비교 (대소문자 무시)
                actual.contains(expected, ignoreCase = true) ||
                        expected.contains(actual, ignoreCase = true)
            }
            else -> actual == expected
        }
    }

    /**
     * 여러 단계에 대해 매칭 시도
     *
     * @param subtasks 단계 목록
     * @param signature 현재 UI 시그니처
     * @param eventType 이벤트 타입
     * @return 가장 높은 매칭 결과
     */
    fun findBestMatch(
        subtasks: List<SubtaskDetail>,
        signature: Map<String, String?>,
        eventType: Int
    ): MatchResult? {
        return subtasks
            .map { matchSingleStep(it, signature, eventType) }
            .maxByOrNull { it.matchRatio }
            ?.takeIf { it.matchRatio > 0f }
    }
}

/**
 * 매칭 결과 데이터 클래스
 */
data class MatchResult(
    val isMatched: Boolean,
    val matchRatio: Float,
    val matchedFields: List<String>,
    val unmatchedFields: List<String>,
    val subtaskId: Int
)
