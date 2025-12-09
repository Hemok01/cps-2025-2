package com.example.mobilegpt.data.remote.dto.request

import com.google.gson.annotations.SerializedName

data class SaveEventsBatchRequest(
    val events: List<AccessibilityEventDto>
)

data class AccessibilityEventDto(
    @SerializedName("event_type")
    val eventType: String,

    val timestamp: String,

    @SerializedName("event_data")
    val eventData: EventData
)

data class EventData(
    val time: Long,
    @SerializedName("package")
    val packageName: String,
    @SerializedName("class_name")
    val className: String,
    val text: List<String>?,
    @SerializedName("content_description")
    val contentDescription: String?,
    @SerializedName("view_id")
    val viewId: String?,
    val bounds: String?,
    val x: Int? = null,
    val y: Int? = null
)
