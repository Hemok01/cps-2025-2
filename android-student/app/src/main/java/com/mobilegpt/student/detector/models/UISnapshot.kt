package com.mobilegpt.student.detector.models

/**
 * 전체 화면 상태를 나타내는 스냅샷
 *
 * AccessibilityNodeInfo 트리를 순회하여 생성되며,
 * 현재 화면에 표시된 모든 View ID와 텍스트 노드를 포함합니다.
 *
 * @property packageName 현재 포그라운드 앱의 패키지명
 * @property activityName 현재 Activity 이름 (추출 가능한 경우)
 * @property visibleViews 화면에 보이는 모든 viewId 목록
 * @property textNodes 화면에 있는 모든 텍스트 (text + contentDescription)
 * @property timestamp 스냅샷 생성 시간
 */
data class UISnapshot(
    val packageName: String,
    val activityName: String? = null,
    val visibleViews: List<String>,
    val textNodes: List<String>,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * 특정 viewId가 화면에 존재하는지 확인
     *
     * 매칭 전략:
     * 1. 정확한 일치
     * 2. 접미사 일치 (예: "button_ok" → "com.example:id/button_ok")
     * 3. 부분 포함
     */
    fun containsViewId(viewId: String): Boolean {
        if (viewId.isBlank()) return false

        return visibleViews.any { visible ->
            visible == viewId ||
            visible.endsWith("/$viewId") ||
            visible.contains(viewId, ignoreCase = true)
        }
    }

    /**
     * 유연한 viewId 매칭 (추가 전략)
     *
     * 매칭 전략:
     * 1. ID 부분만 추출하여 비교 (패키지 제외)
     * 2. 숫자 접미사 무시 (button1 ≈ button)
     * 3. 언더스코어/하이픈 무시 (btn_ok ≈ btnok)
     */
    fun containsViewIdFlexible(viewId: String): Boolean {
        if (viewId.isBlank()) return false

        // 기대하는 ID의 마지막 부분만 추출
        val expectedId = viewId.substringAfterLast("/")
            .substringAfterLast(":")
            .lowercase()
            .replace(Regex("[_\\-]"), "")  // 언더스코어/하이픈 제거

        return visibleViews.any { visible ->
            val actualId = visible.substringAfterLast("/")
                .substringAfterLast(":")
                .lowercase()
                .replace(Regex("[_\\-]"), "")

            // 정확한 일치 또는 하나가 다른 하나를 포함
            actualId == expectedId ||
            actualId.contains(expectedId) ||
            expectedId.contains(actualId)
        }
    }

    /**
     * 특정 텍스트가 화면에 존재하는지 확인
     * 대소문자 무시, 부분 일치 지원
     */
    fun containsText(text: String): Boolean {
        if (text.isBlank()) return false

        return textNodes.any { node ->
            node.contains(text, ignoreCase = true) ||
            text.contains(node, ignoreCase = true)
        }
    }

    /**
     * 유연한 텍스트 매칭 (추가 전략)
     *
     * 매칭 전략:
     * 1. 공백 제거 후 비교
     * 2. 특수문자 무시
     * 3. 숫자 무시 (선택적)
     */
    fun containsTextFlexible(text: String): Boolean {
        if (text.isBlank()) return false

        // 정규화: 공백 및 특수문자 제거, 소문자 변환
        val normalizedExpected = text.lowercase()
            .replace(Regex("[\\s\\p{Punct}]"), "")

        return textNodes.any { node ->
            val normalizedActual = node.lowercase()
                .replace(Regex("[\\s\\p{Punct}]"), "")

            normalizedActual == normalizedExpected ||
            normalizedActual.contains(normalizedExpected) ||
            normalizedExpected.contains(normalizedActual)
        }
    }

    /**
     * 패키지명이 일치하는지 확인
     * 대소문자 무시, 부분 일치 지원
     */
    fun matchesPackage(expectedPackage: String): Boolean {
        if (expectedPackage.isBlank()) return true

        return packageName.contains(expectedPackage, ignoreCase = true) ||
               expectedPackage.contains(packageName, ignoreCase = true)
    }

    /**
     * 두 스냅샷이 실질적으로 동일한지 비교
     * (timestamp 제외)
     */
    fun isSameAs(other: UISnapshot?): Boolean {
        if (other == null) return false

        return packageName == other.packageName &&
               visibleViews.toSet() == other.visibleViews.toSet() &&
               textNodes.toSet() == other.textNodes.toSet()
    }

    companion object {
        /**
         * 빈 스냅샷 생성
         */
        fun empty(packageName: String = ""): UISnapshot {
            return UISnapshot(
                packageName = packageName,
                visibleViews = emptyList(),
                textNodes = emptyList()
            )
        }
    }
}
