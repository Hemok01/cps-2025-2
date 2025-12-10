package com.example.mobilegpt.data.remote.dto.response

import com.google.gson.annotations.SerializedName

/**
 * 세부단계 응답
 * 백엔드 SubtaskSerializer와 일치
 */
data class SubtaskResponse(
    val id: Long,
    val title: String,
    val description: String? = null,
    @SerializedName("order_index")
    val orderIndex: Int = 0,
    @SerializedName("target_action")
    val targetAction: String? = null,  // "CLICK", "LONG_CLICK", "SCROLL", "INPUT", "NAVIGATE"
    @SerializedName("target_package")
    val targetPackage: String? = null,
    @SerializedName("target_class")
    val targetClass: String? = null,
    @SerializedName("target_element_hint")  // 백엔드 필드명과 일치
    val uiHint: String? = null,
    @SerializedName("guide_text")
    val guideText: String? = null,
    @SerializedName("voice_guide_text")
    val voiceGuideText: String? = null,

    // ===== 추가 필드 (Flask 원본 동기화) =====
    val time: Long? = null,
    val text: String? = null,
    @SerializedName("content_description")
    val contentDescription: String? = null,
    @SerializedName("view_id")
    val viewId: String? = null,
    val bounds: String? = null
) {
    /** step은 order_index + 1 (1부터 시작하는 순서 번호) */
    val step: Int get() = orderIndex + 1
}

/**
 * 세부단계 목록 응답 (Recording 기반)
 * GET /api/recordings/{id}/subtasks/
 */
data class RecordingSubtasksResponse(
    @SerializedName("recording_id")
    val recordingId: Long,
    @SerializedName("task_id")
    val taskId: Long? = null,
    @SerializedName("task_title")
    val taskTitle: String? = null,
    @SerializedName("subtask_count")
    val subtaskCount: Int? = null,
    val subtasks: List<SubtaskResponse>,
    // 변환되지 않은 경우 오류 정보
    val error: String? = null,
    val message: String? = null,
    @SerializedName("recording_status")
    val recordingStatus: String? = null
)
