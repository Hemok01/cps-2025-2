package com.mobilegpt.student.domain.model

import com.google.gson.annotations.SerializedName

/**
 * 세션 데이터 모델 (API 응답용)
 */
data class SessionData(
    @SerializedName("id")
    val id: Int,

    @SerializedName("lecture")
    val lecture: Int?,

    @SerializedName("lecture_title")
    val lectureTitle: String?,

    @SerializedName("instructor_name")
    val instructorName: String?,

    @SerializedName("title")
    val title: String,

    @SerializedName("session_code")
    val sessionCode: String,

    @SerializedName("status")
    val status: String,

    @SerializedName("current_subtask")
    val currentSubtask: Int?,

    @SerializedName("current_subtask_detail")
    val currentSubtaskDetail: SubtaskDetail?,

    @SerializedName("my_status")
    val myStatus: String?,

    @SerializedName("scheduled_at")
    val scheduledAt: String?,

    @SerializedName("started_at")
    val startedAt: String?,

    @SerializedName("ended_at")
    val endedAt: String?
)

/**
 * 단계 상세 정보
 */
data class SubtaskDetail(
    @SerializedName("id")
    val id: Int,

    @SerializedName("task")
    val task: Int,

    @SerializedName("task_title")
    val taskTitle: String?,

    @SerializedName("title")
    val title: String,

    @SerializedName("description")
    val description: String,

    @SerializedName("order")
    val order: Int,

    @SerializedName("target_app")
    val targetApp: String?,

    @SerializedName("target_action")
    val targetAction: String?
)
