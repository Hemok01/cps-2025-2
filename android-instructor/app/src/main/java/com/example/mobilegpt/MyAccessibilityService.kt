package com.example.mobilegpt

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.mobilegpt.data.remote.dto.request.AccessibilityEventDto
import com.example.mobilegpt.data.remote.dto.request.EventData
import com.example.mobilegpt.data.remote.dto.request.SaveEventsBatchRequest
import com.example.mobilegpt.data.remote.dto.request.StartRecordingRequest
import com.example.mobilegpt.network.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale

class MyAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "A11Y"
        private const val BATCH_SIZE = 20  // 20개 이벤트마다 전송
        private const val FLUSH_INTERVAL_MS = 1000L  // 1초마다 전송

        private var instance: MyAccessibilityService? = null
        private var isRecording = false
        private var currentRecordingId: Long? = null

        /**
         * 녹화 시작 (Django 서버용)
         * @param title 녹화 제목
         * @param onSuccess 성공 시 recordingId 전달
         * @param onError 실패 시 에러 메시지 전달
         */
        fun startRecordingWithServer(
            title: String,
            onSuccess: (Long) -> Unit,
            onError: (String) -> Unit
        ) {
            instance?.startRecordingInternal(title, onSuccess, onError)
        }

        /**
         * 녹화 종료 (Django 서버용)
         * @param onFinished 완료 시 recordingId 전달
         */
        fun stopRecordingWithServer(onFinished: (Long) -> Unit) {
            isRecording = false
            instance?.stopRecordingInternal(onFinished)
            Log.i(TAG, "Recording Stopped")
        }

        /**
         * 녹화 시작 (Legacy - Flask 서버 호환)
         */
        @Deprecated("Use startRecordingWithServer instead")
        fun startRecording() {
            isRecording = true
            Log.i(TAG, "Recording Started (Legacy)")
        }

        /**
         * 녹화 종료 (Legacy - Flask 서버 호환)
         */
        @Deprecated("Use stopRecordingWithServer instead")
        fun stopRecording(onFinished: (String) -> Unit) {
            isRecording = false
            instance?.finishRecordingLegacy(onFinished)
            Log.i(TAG, "Recording Stopped (Legacy)")
        }

        fun isCurrentlyRecording(): Boolean = isRecording
    }

    // Coroutine scope for background operations
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var flushJob: Job? = null

    // Thread-safe event buffer
    private val eventBuffer = Collections.synchronizedList(mutableListOf<AccessibilityEventDto>())

    // ISO 8601 date formatter
    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "Accessibility Connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isRecording || event == null) return
        bufferEvent(event)
    }

    /**
     * 이벤트를 버퍼에 추가
     */
    private fun bufferEvent(event: AccessibilityEvent) {
        try {
            val dto = convertToDto(event)
            eventBuffer.add(dto)

            // 버퍼가 일정 크기 이상이면 즉시 전송
            if (eventBuffer.size >= BATCH_SIZE) {
                serviceScope.launch { flushEvents() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Event Buffer Error", e)
        }
    }

    /**
     * AccessibilityEvent를 DTO로 변환
     */
    private fun convertToDto(event: AccessibilityEvent): AccessibilityEventDto {
        var bounds: String? = null
        var viewId: String? = null

        val node = event.source
        if (node != null) {
            val rect = Rect()
            node.getBoundsInScreen(rect)
            bounds = rect.flattenToString()
            viewId = node.viewIdResourceName
            node.recycle()
        }

        val eventData = EventData(
            time = event.eventTime,
            packageName = event.packageName?.toString() ?: "",
            className = event.className?.toString() ?: "",
            text = event.text?.map { it.toString() },
            contentDescription = event.contentDescription?.toString(),
            viewId = viewId,
            bounds = bounds
        )

        return AccessibilityEventDto(
            eventType = getEventTypeName(event.eventType),
            timestamp = isoDateFormat.format(Date(event.eventTime)),
            eventData = eventData
        )
    }

    /**
     * 이벤트 타입 이름 반환
     */
    private fun getEventTypeName(eventType: Int): String {
        return when (eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED -> "VIEW_CLICKED"
            AccessibilityEvent.TYPE_VIEW_LONG_CLICKED -> "VIEW_LONG_CLICKED"
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> "FOCUS"
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> "VIEW_TEXT_CHANGED"
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> "WINDOW_CHANGE"
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> "SCROLL"
            AccessibilityEvent.TYPE_VIEW_SELECTED -> "SELECTION"
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> "NOTIFICATION"
            AccessibilityEvent.TYPE_ANNOUNCEMENT -> "ANNOUNCEMENT"
            else -> "TYPE_$eventType"
        }
    }

    /**
     * 버퍼의 이벤트를 서버로 전송
     */
    private suspend fun flushEvents() {
        if (eventBuffer.isEmpty() || currentRecordingId == null) return

        val eventsToSend: List<AccessibilityEventDto>
        synchronized(eventBuffer) {
            eventsToSend = eventBuffer.toList()
            eventBuffer.clear()
        }

        if (eventsToSend.isEmpty()) return

        try {
            val request = SaveEventsBatchRequest(events = eventsToSend)
            val response = ApiClient.recordingApi.saveEventsBatch(currentRecordingId!!, request)

            if (response.isSuccessful) {
                Log.d(TAG, "Flushed ${eventsToSend.size} events")
            } else {
                Log.e(TAG, "Flush failed: ${response.code()}")
                // 실패한 이벤트를 다시 버퍼에 추가 (선택적)
                synchronized(eventBuffer) {
                    eventBuffer.addAll(0, eventsToSend)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Flush error", e)
            // 실패한 이벤트를 다시 버퍼에 추가
            synchronized(eventBuffer) {
                eventBuffer.addAll(0, eventsToSend)
            }
        }
    }

    /**
     * 주기적 이벤트 전송 시작
     */
    private fun startPeriodicFlush() {
        flushJob?.cancel()
        flushJob = serviceScope.launch {
            while (isRecording) {
                delay(FLUSH_INTERVAL_MS)
                if (isRecording) {
                    flushEvents()
                }
            }
        }
    }

    /**
     * 녹화 시작 (내부 구현)
     */
    private fun startRecordingInternal(
        title: String,
        onSuccess: (Long) -> Unit,
        onError: (String) -> Unit
    ) {
        serviceScope.launch {
            try {
                val request = StartRecordingRequest(title = title)
                val response = ApiClient.recordingApi.startRecording(request)

                if (response.isSuccessful && response.body() != null) {
                    val recording = response.body()!!
                    currentRecordingId = recording.id
                    isRecording = true
                    eventBuffer.clear()
                    startPeriodicFlush()

                    Log.i(TAG, "Recording started: ID=${recording.id}")

                    Handler(Looper.getMainLooper()).post {
                        onSuccess(recording.id)
                    }
                } else {
                    val errorMsg = "녹화 시작 실패: ${response.code()}"
                    Log.e(TAG, errorMsg)
                    Handler(Looper.getMainLooper()).post {
                        onError(errorMsg)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Start recording error", e)
                Handler(Looper.getMainLooper()).post {
                    onError(e.message ?: "알 수 없는 오류")
                }
            }
        }
    }

    /**
     * 녹화 종료 (내부 구현)
     */
    private fun stopRecordingInternal(onFinished: (Long) -> Unit) {
        val recordingId = currentRecordingId ?: return

        serviceScope.launch {
            try {
                // 남은 이벤트 모두 전송
                flushEvents()

                // 녹화 종료 API 호출
                val response = ApiClient.recordingApi.stopRecording(recordingId)

                if (response.isSuccessful) {
                    Log.i(TAG, "Recording stopped: ID=$recordingId")
                } else {
                    Log.e(TAG, "Stop recording failed: ${response.code()}")
                }

                // 정리
                flushJob?.cancel()
                eventBuffer.clear()

                Handler(Looper.getMainLooper()).post {
                    onFinished(recordingId)
                }

                currentRecordingId = null
            } catch (e: Exception) {
                Log.e(TAG, "Stop recording error", e)
                Handler(Looper.getMainLooper()).post {
                    onFinished(recordingId)
                }
            }
        }
    }

    // ===== Legacy Methods (Flask 서버 호환용) =====

    @Deprecated("Use stopRecordingInternal instead")
    private fun finishRecordingLegacy(onFinished: (String) -> Unit) {
        serviceScope.launch {
            try {
                val saveRes = postJsonReturn(Constants.getFullUrl(Constants.Endpoints.SAVE_SESSION), "{}")
                val file = org.json.JSONObject(saveRes).getString("file")

                val req = org.json.JSONObject().apply { put("file", file) }
                postJsonReturn(Constants.getFullUrl(Constants.Endpoints.ANALYZE_SESSION), req.toString())

                val sessionId = file.replace(".json", "")

                Handler(Looper.getMainLooper()).post {
                    onFinished(sessionId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Finish Error (Legacy)", e)
            }
        }
    }

    @Deprecated("Use Retrofit API instead")
    private fun postJsonReturn(urlStr: String, json: String): String {
        return try {
            val conn = java.net.URL(urlStr).openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { it.write(json.toByteArray()) }

            val scanner = java.util.Scanner(conn.inputStream).useDelimiter("\\A")
            val result = if (scanner.hasNext()) scanner.next() else ""
            conn.disconnect()
            result
        } catch (e: Exception) {
            ""
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        instance = null
    }
}
