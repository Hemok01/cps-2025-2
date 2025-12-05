package com.example.mobilegpt_instructor.data.model

import com.google.gson.annotations.SerializedName

// ============ Request Models ============

data class CreateRecordingRequest(
    val title: String,
    val description: String = ""
)

data class SaveEventsRequest(
    val events: List<AccessibilityEventData>
)

data class UpdateStepsRequest(
    val steps: List<AnalyzedStep>
)

data class ConvertToLectureRequest(
    val title: String,
    val description: String = ""
)

// ============ Response Models ============

data class RecordingResponse(
    val id: Int,
    val title: String,
    val description: String,
    val status: String,
    @SerializedName("started_at")
    val startedAt: String?,
    @SerializedName("ended_at")
    val endedAt: String?,
    @SerializedName("event_count")
    val eventCount: Int,
    @SerializedName("step_count")
    val stepCount: Int,
    @SerializedName("analysis_result")
    val analysisResult: AnalysisResult?,
    @SerializedName("analyzed_at")
    val analyzedAt: String?,
    @SerializedName("analysis_error")
    val analysisError: String?,
    @SerializedName("created_at")
    val createdAt: String
)

data class SaveEventsResponse(
    @SerializedName("saved_count")
    val savedCount: Int,
    @SerializedName("total_events")
    val totalEvents: Int
)

data class AnalyzeResponse(
    val status: String,
    val message: String,
    @SerializedName("recording_id")
    val recordingId: Int
)

data class AnalysisStatusResponse(
    @SerializedName("recording_id")
    val recordingId: Int,
    val status: String,
    @SerializedName("analysis_result")
    val analysisResult: AnalysisResult?,
    @SerializedName("analyzed_at")
    val analyzedAt: String?,
    @SerializedName("analysis_error")
    val analysisError: String?
)

data class ConvertToLectureResponse(
    val message: String,
    @SerializedName("lecture_id")
    val lectureId: Int,
    @SerializedName("task_id")
    val taskId: Int,
    @SerializedName("subtask_count")
    val subtaskCount: Int,
    @SerializedName("lecture_title")
    val lectureTitle: String
)

// ============ Data Models ============

data class AnalysisResult(
    val steps: List<AnalyzedStep>,
    @SerializedName("total_steps")
    val totalSteps: Int,
    @SerializedName("analyzed_events_count")
    val analyzedEventsCount: Int
)

data class AnalyzedStep(
    val step: Int,
    val title: String,
    val description: String,
    val time: String?,
    val eventType: String?,
    @SerializedName("package")
    val packageName: String?,
    val className: String?,
    val text: String?,
    val contentDescription: String?,
    val viewId: String?,
    val bounds: String?
)

data class AccessibilityEventData(
    val time: Long,
    val eventType: String,
    @SerializedName("package")
    val packageName: String?,
    val className: String?,
    val text: String?,
    val contentDescription: String?,
    val viewId: String?,
    val bounds: String?
)
