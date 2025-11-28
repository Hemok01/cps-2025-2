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
    val targetAction: String? = null
)
