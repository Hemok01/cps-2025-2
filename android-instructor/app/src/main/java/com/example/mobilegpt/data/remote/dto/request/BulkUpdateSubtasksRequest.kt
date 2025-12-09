package com.example.mobilegpt.data.remote.dto.request

import com.google.gson.annotations.SerializedName

/**
 * Subtask 일괄 업데이트 요청
 * PUT /api/tasks/{task_id}/subtasks/bulk/
 */
data class BulkUpdateSubtasksRequest(
    val subtasks: List<SubtaskUpdateItem>
)

/**
 * Subtask 업데이트 항목
 */
data class SubtaskUpdateItem(
    val title: String,
    val description: String,
    val time: Long? = null,
    val text: String? = null,
    @SerializedName("content_description")
    val contentDescription: String? = null,
    @SerializedName("view_id")
    val viewId: String? = null,
    val bounds: String? = null,
    @SerializedName("target_package")
    val targetPackage: String? = null,
    @SerializedName("target_class")
    val targetClass: String? = null,
    @SerializedName("target_action")
    val targetAction: String? = null,
    @SerializedName("ui_hint")
    val uiHint: String? = null,
    @SerializedName("guide_text")
    val guideText: String? = null,
    @SerializedName("voice_guide_text")
    val voiceGuideText: String? = null
)
