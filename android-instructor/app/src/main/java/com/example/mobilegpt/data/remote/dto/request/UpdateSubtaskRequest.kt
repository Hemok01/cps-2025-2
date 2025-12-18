package com.example.mobilegpt.data.remote.dto.request

import com.google.gson.annotations.SerializedName

data class UpdateSubtaskRequest(
    val title: String,
    val description: String,
    @SerializedName("ui_hint")
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
    val bounds: String? = null,
    @SerializedName("target_package")
    val targetPackage: String? = null,
    @SerializedName("target_class")
    val targetClass: String? = null,
    @SerializedName("target_action")
    val targetAction: String? = null
)
