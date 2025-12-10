package com.example.mobilegpt.data.remote.dto.response

import com.google.gson.annotations.SerializedName

/**
 * Subtask 일괄 업데이트 응답
 */
data class BulkUpdateResponse(
    val status: String,
    @SerializedName("deleted_count")
    val deletedCount: Int = 0,
    @SerializedName("created_count")
    val createdCount: Int = 0,
    val subtasks: List<SubtaskResponse>? = null
)
