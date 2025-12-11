package com.mobilegpt.student.domain.model

import com.google.gson.annotations.SerializedName

/**
 * 세션 데이터 모델 (API 응답용)
 */
data class SessionData(
    @SerializedName("id")
    val id: Int,

    @SerializedName("lecture")
    val lecture: LectureInfo?,

    @SerializedName("instructor")
    val instructor: InstructorInfo?,

    @SerializedName("title")
    val title: String,

    @SerializedName("session_code")
    val sessionCode: String,

    @SerializedName("status")
    val status: String,

    @SerializedName("current_subtask")
    val currentSubtask: SubtaskDetail?,

    @SerializedName("currentSubtaskDetail")
    val currentSubtaskDetail: SubtaskDetail?,

    @SerializedName("subtasks")
    val subtasks: List<SubtaskInfo>? = null,

    @SerializedName("total_steps")
    val totalSteps: Int? = null,

    @SerializedName("my_status")
    val myStatus: String? = null,

    @SerializedName("scheduled_at")
    val scheduledAt: String? = null,

    @SerializedName("started_at")
    val startedAt: String? = null,

    @SerializedName("ended_at")
    val endedAt: String? = null
) {
    // 편의 속성: 강의 제목
    val lectureTitle: String?
        get() = lecture?.title

    // 편의 속성: 강사 이름
    val instructorName: String?
        get() = instructor?.name
}

/**
 * 강의 정보
 */
data class LectureInfo(
    @SerializedName("id")
    val id: Int,

    @SerializedName("title")
    val title: String
)

/**
 * 강사 정보
 */
data class InstructorInfo(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String
)

/**
 * 서브태스크 간단 정보 (목록용)
 */
data class SubtaskInfo(
    @SerializedName("id")
    val id: Int,

    @SerializedName("title")
    val title: String,

    @SerializedName("order_index")
    val orderIndex: Int
)

/**
 * 단계 상세 정보
 *
 * UI 비교 기반 진행도 추적을 위한 필드 포함:
 * - viewId: 클릭할 UI 요소의 viewId
 * - text: UI 요소의 텍스트
 * - contentDescription: 접근성 설명
 * - targetPackage: 앱 패키지명
 * - targetClass: UI 요소 클래스명
 */
data class SubtaskDetail(
    @SerializedName("id")
    val id: Int,

    @SerializedName("task")
    val task: Int? = null,

    @SerializedName("task_title")
    val taskTitle: String? = null,

    @SerializedName("title")
    val title: String,

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("order")
    val order: Int? = null,

    @SerializedName("order_index")
    val orderIndex: Int? = null,

    @SerializedName("target_app")
    val targetApp: String? = null,

    @SerializedName("target_action")
    val targetAction: String? = null,

    @SerializedName("guide_text")
    val guideText: String? = null,

    // ===== UI 비교용 필드 (진행도 추적에 사용) =====

    @SerializedName("view_id")
    val viewId: String? = null,

    @SerializedName("text")
    val text: String? = null,

    @SerializedName("content_description")
    val contentDescription: String? = null,

    @SerializedName("target_package")
    val targetPackage: String? = null,

    @SerializedName("target_class")
    val targetClass: String? = null
) {
    /**
     * UI 비교에 사용할 패키지명 반환
     * target_package가 없으면 target_app을 fallback으로 사용
     */
    val effectivePackage: String?
        get() = targetPackage?.takeIf { it.isNotBlank() } ?: targetApp

    /**
     * 이 단계가 UI 비교 가능한지 확인
     * (최소한 패키지명 또는 viewId가 있어야 함)
     */
    val isComparable: Boolean
        get() = !effectivePackage.isNullOrBlank() ||
                !viewId.isNullOrBlank() ||
                !text.isNullOrBlank()
}
