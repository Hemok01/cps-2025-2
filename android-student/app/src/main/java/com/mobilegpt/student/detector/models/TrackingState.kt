package com.mobilegpt.student.detector.models

/**
 * UI 추적 상태
 *
 * FloatingOverlayService에서 현재 진행 상태를 표시하는 데 사용됩니다.
 */
enum class TrackingState(
    val emoji: String,
    val koreanLabel: String,
    val colorHex: Long  // ARGB 형태
) {
    /**
     * 대기 중 - 세션 시작 전 또는 다음 단계 대기
     */
    WAITING(
        emoji = "👀",
        koreanLabel = "대기 중",
        colorHex = 0xFF9E9E9E  // 회색
    ),

    /**
     * 확인 중 - UI 매칭 진행 중
     */
    CHECKING(
        emoji = "🔍",
        koreanLabel = "확인 중",
        colorHex = 0xFF2196F3  // 파란색
    ),

    /**
     * 매칭됨 - 단계 완료 조건 충족
     */
    MATCHED(
        emoji = "✅",
        koreanLabel = "완료!",
        colorHex = 0xFF4CAF50  // 초록색
    ),

    /**
     * 오류 발생 - 잘못된 앱 또는 화면 정지
     */
    ERROR(
        emoji = "❌",
        koreanLabel = "오류",
        colorHex = 0xFFF44336  // 빨간색
    ),

    /**
     * 모든 단계 완료
     */
    COMPLETED(
        emoji = "🎉",
        koreanLabel = "완료!",
        colorHex = 0xFF9C27B0  // 보라색
    ),

    /**
     * 진행 중 - 현재 단계 수행 중
     */
    IN_PROGRESS(
        emoji = "📱",
        koreanLabel = "진행 중",
        colorHex = 0xFFFF9800  // 주황색
    );

    /**
     * 상태에 해당하는 색상을 Int로 반환
     */
    val color: Int
        get() = colorHex.toInt()

    /**
     * 표시용 라벨 (이모지 + 텍스트)
     */
    val displayLabel: String
        get() = "$emoji $koreanLabel"

    companion object {
        /**
         * 매칭 결과에 따른 TrackingState 반환
         *
         * 우선순위:
         * 1. 완전 매칭 → MATCHED
         * 2. 행동 타입 불일치 → WAITING (잘못된 행동이지만 오류는 아님)
         * 3. 패키지 매칭 + 부분 KeyView 매칭 → IN_PROGRESS
         * 4. 패키지만 매칭 → CHECKING (올바른 앱에 있음)
         * 5. 그 외 → WAITING
         */
        fun fromMatchResult(result: AdvancedMatchResult): TrackingState {
            return when {
                result.isMatched -> MATCHED
                result.actionMismatch -> WAITING  // 행동 타입 불일치는 다시 시도하면 됨
                result.isPartialMatch -> IN_PROGRESS  // 일부 매칭 - 진행 중
                result.packageMatched -> CHECKING  // 올바른 앱에 있음
                else -> WAITING
            }
        }

        /**
         * 오류 타입에 따른 TrackingState 반환
         */
        fun fromError(error: ErrorType?): TrackingState {
            return if (error != null) ERROR else WAITING
        }
    }
}
