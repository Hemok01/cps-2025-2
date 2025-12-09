package com.example.mobilegpt.data.remote.dto.response

import com.google.gson.annotations.SerializedName

/**
 * 세부단계 응답
 */
data class SubtaskResponse(
    val id: Long,
    val step: Int,  // 순서 번호 (1, 2, 3...)
    val title: String,
    val description: String,
    @SerializedName("order_index")
    val orderIndex: Int,
    @SerializedName("target_action")
    val targetAction: String?,  // "CLICK", "LONG_CLICK", "SCROLL", "INPUT", "NAVIGATE"
    @SerializedName("target_package")
    val targetPackage: String?,
    @SerializedName("target_class")
    val targetClass: String?,
    @SerializedName("ui_hint")
    val uiHint: String?,
    @SerializedName("guide_text")
    val guideText: String?,
    @SerializedName("voice_guide_text")
    val voiceGuideText: String?,

    // ===== 추가 필드 (Flask 원본 동기화) =====
    val time: Long? = null,
    val text: String? = null,
    @SerializedName("content_description")
    val contentDescription: String? = null,
    @SerializedName("view_id")
    val viewId: String? = null,
    val bounds: String? = null
)

/**
 * 세부단계 목록 응답 (Recording 기반)
 * GET /api/sessions/recordings/{id}/subtasks/
 */
data class RecordingSubtasksResponse(
    @SerializedName("recording_id")
    val recordingId: Long,
    @SerializedName("lecture_id")
    val lectureId: Long? = null,
    @SerializedName("lecture_title")
    val lectureTitle: String? = null,
    @SerializedName("subtask_count")
    val subtaskCount: Int? = null,
    val subtasks: List<SubtaskResponse>,
    // 변환되지 않은 경우 오류 정보
    val error: String? = null,
    val message: String? = null,
    @SerializedName("recording_status")
    val recordingStatus: String? = null
)
