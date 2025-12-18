package com.mobilegpt.student.detector.models

/**
 * UI 비교 중 발생할 수 있는 오류 타입
 *
 * 오류 발생 시 서버에 보고되어 강사 대시보드에서 확인할 수 있습니다.
 */
enum class ErrorType(
    val code: String,
    val koreanMessage: String,
    val description: String
) {
    /**
     * 잘못된 앱으로 이동
     * 예상되는 앱이 아닌 다른 앱으로 이동한 경우
     */
    WRONG_APP(
        code = "WRONG_APP",
        koreanMessage = "잘못된 앱",
        description = "예상되는 앱이 아닌 다른 앱으로 이동했습니다."
    ),

    /**
     * 화면 정지
     * 일정 시간(기본 3초) 동안 UI 변화가 없는 경우
     */
    FROZEN_SCREEN(
        code = "FROZEN_SCREEN",
        koreanMessage = "화면 정지",
        description = "일정 시간 동안 화면 변화가 없습니다."
    ),

    /**
     * 잘못된 클릭
     * 예상되지 않은 UI 요소를 클릭한 경우
     */
    WRONG_CLICK(
        code = "WRONG_CLICK",
        koreanMessage = "잘못된 클릭",
        description = "예상되지 않은 UI 요소를 클릭했습니다."
    );

    companion object {
        /**
         * 코드로 ErrorType 찾기
         */
        fun fromCode(code: String): ErrorType? {
            return entries.find { it.code == code }
        }
    }
}

/**
 * 감지된 오류 정보
 *
 * @property type 오류 타입
 * @property timestamp 오류 발생 시간
 * @property expectedPackage 예상되던 패키지
 * @property actualPackage 실제 패키지
 * @property subtaskId 해당 단계 ID
 * @property additionalInfo 추가 정보 (디버깅용)
 */
data class DetectedError(
    val type: ErrorType,
    val timestamp: Long = System.currentTimeMillis(),
    val expectedPackage: String? = null,
    val actualPackage: String? = null,
    val subtaskId: Int? = null,
    val additionalInfo: String? = null
) {
    /**
     * 서버 보고용 JSON 형태로 변환
     */
    fun toReportMap(): Map<String, Any?> {
        return mapOf(
            "error_type" to type.code,
            "timestamp" to timestamp,
            "expected_package" to expectedPackage,
            "actual_package" to actualPackage,
            "subtask_id" to subtaskId,
            "additional_info" to additionalInfo
        )
    }
}
