package com.mobilegpt.student.detector.models

import com.mobilegpt.student.domain.model.SubtaskDetail

/**
 * UI 매칭에 사용되는 키 뷰 정보
 *
 * 단계 완료 판단 시 화면에서 찾아야 할 UI 요소를 정의합니다.
 * viewId 또는 text 중 하나만 있어도 매칭이 가능합니다.
 *
 * @property viewId UI 요소의 리소스 ID (예: "com.example:id/button_ok" 또는 "button_ok")
 * @property text UI 요소에 표시된 텍스트
 */
data class KeyView(
    val viewId: String? = null,
    val text: String? = null
) {
    /**
     * 이 KeyView가 유효한지 확인
     * (최소한 viewId 또는 text 중 하나는 있어야 함)
     */
    val isValid: Boolean
        get() = !viewId.isNullOrBlank() || !text.isNullOrBlank()

    /**
     * UISnapshot에서 이 KeyView를 찾을 수 있는지 확인
     *
     * 매칭 전략 (유연한 매칭):
     * 1. viewId: 정확한 일치 → 접미사 일치 → 부분 포함
     * 2. text: 대소문자 무시 + 부분 일치 허용
     * 3. OR 조건: viewId 또는 text 중 하나만 매칭되어도 성공
     */
    fun existsIn(snapshot: UISnapshot): Boolean {
        if (!isValid) return false

        // viewId로 매칭 (여러 전략 시도)
        if (!viewId.isNullOrBlank()) {
            val viewIdMatched = snapshot.containsViewId(viewId) ||
                    snapshot.containsViewIdFlexible(viewId)
            if (viewIdMatched) {
                return true
            }
        }

        // text로 매칭 (유연한 텍스트 비교)
        if (!text.isNullOrBlank()) {
            val textMatched = snapshot.containsText(text) ||
                    snapshot.containsTextFlexible(text)
            if (textMatched) {
                return true
            }
        }

        return false
    }

    companion object {
        /**
         * SubtaskDetail에서 KeyView 생성
         */
        fun fromSubtask(subtask: SubtaskDetail): KeyView {
            return KeyView(
                viewId = subtask.viewId,
                text = subtask.text ?: subtask.contentDescription
            )
        }

        /**
         * SubtaskDetail에서 여러 KeyView 생성
         * (viewId, text, contentDescription 각각을 별도의 KeyView로)
         */
        fun listFromSubtask(subtask: SubtaskDetail): List<KeyView> {
            val keyViews = mutableListOf<KeyView>()

            // viewId가 있으면 추가
            if (!subtask.viewId.isNullOrBlank()) {
                keyViews.add(KeyView(viewId = subtask.viewId))
            }

            // text가 있으면 추가
            if (!subtask.text.isNullOrBlank()) {
                keyViews.add(KeyView(text = subtask.text))
            }

            // contentDescription이 있으면 추가
            if (!subtask.contentDescription.isNullOrBlank() &&
                subtask.contentDescription != subtask.text) {
                keyViews.add(KeyView(text = subtask.contentDescription))
            }

            return keyViews
        }
    }
}
