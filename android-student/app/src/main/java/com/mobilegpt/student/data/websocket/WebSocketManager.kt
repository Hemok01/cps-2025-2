package com.mobilegpt.student.data.websocket

import android.util.Log
import com.google.gson.Gson
import com.mobilegpt.student.BuildConfig
import com.mobilegpt.student.data.api.WebSocketApi
import com.mobilegpt.student.domain.model.ClientMessage
import com.mobilegpt.student.domain.model.MessageType
import com.mobilegpt.student.domain.model.SessionMessage
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.messageadapter.gson.GsonMessageAdapter
import com.tinder.scarlet.retry.LinearBackoffStrategy
import com.tinder.streamadapter.coroutines.CoroutinesStreamAdapterFactory
import com.tinder.scarlet.websocket.okhttp.newWebSocketFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WebSocket 연결 상태
 */
enum class WebSocketConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

/**
 * WebSocket Manager
 * 동적으로 세션 코드에 맞는 WebSocket 연결을 관리합니다.
 *
 * Scarlet은 빌드 후 URL 변경이 불가하므로,
 * 세션 코드가 바뀔 때마다 새 Scarlet 인스턴스를 생성합니다.
 */
@Singleton
class WebSocketManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "WebSocketManager"
        private const val HEARTBEAT_INTERVAL_MS = 30_000L
        private const val RECONNECT_DELAY_MS = 5_000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var scarlet: Scarlet? = null
    private var webSocketApi: WebSocketApi? = null
    private var currentSessionCode: String? = null

    // 연결 상태
    private val _connectionState = MutableStateFlow(WebSocketConnectionState.DISCONNECTED)
    val connectionState: StateFlow<WebSocketConnectionState> = _connectionState.asStateFlow()

    // 현재 연결된 세션 코드
    private val _connectedSessionCode = MutableStateFlow<String?>(null)
    val connectedSessionCode: StateFlow<String?> = _connectedSessionCode.asStateFlow()

    // 메시지 수신용 SharedFlow (연결 전에도 collect 가능)
    private val _messages = MutableSharedFlow<SessionMessage>(
        replay = 0,
        extraBufferCapacity = 64
    )
    val messages: SharedFlow<SessionMessage> = _messages.asSharedFlow()

    // 하트비트 Job
    private var heartbeatJob: kotlinx.coroutines.Job? = null

    // 메시지 관찰 Job
    private var messageObserverJob: kotlinx.coroutines.Job? = null

    /**
     * 세션에 WebSocket 연결
     * @param sessionCode 세션 코드
     * @return WebSocketApi 인스턴스
     */
    fun connect(sessionCode: String): WebSocketApi {
        Log.d(TAG, "Connecting to session: $sessionCode")

        // 이미 같은 세션에 연결되어 있으면 기존 API 반환
        if (currentSessionCode == sessionCode && webSocketApi != null) {
            Log.d(TAG, "Already connected to session: $sessionCode")
            return webSocketApi!!
        }

        // 기존 연결 종료
        disconnect()

        _connectionState.value = WebSocketConnectionState.CONNECTING
        currentSessionCode = sessionCode

        // WebSocket URL 구성: ws://10.0.2.2:8000/ws/sessions/{sessionCode}/
        val wsUrl = "${BuildConfig.WS_BASE_URL}sessions/$sessionCode/"
        Log.d(TAG, "WebSocket URL: $wsUrl")

        // 새 Scarlet 인스턴스 생성
        scarlet = Scarlet.Builder()
            .webSocketFactory(okHttpClient.newWebSocketFactory(wsUrl))
            .addMessageAdapterFactory(GsonMessageAdapter.Factory(gson))
            .addStreamAdapterFactory(CoroutinesStreamAdapterFactory())  // ReceiveChannel 지원
            .backoffStrategy(LinearBackoffStrategy(RECONNECT_DELAY_MS))
            .build()

        webSocketApi = scarlet!!.create(WebSocketApi::class.java)

        // 연결 상태 모니터링
        observeConnectionState()

        // 메시지 관찰 시작 (새 연결에 대해)
        startMessageObserver()

        // 하트비트 시작
        startHeartbeat()

        _connectedSessionCode.value = sessionCode
        return webSocketApi!!
    }

    /**
     * WebSocket 연결 종료
     */
    fun disconnect() {
        Log.d(TAG, "Disconnecting from session: $currentSessionCode")

        heartbeatJob?.cancel()
        heartbeatJob = null

        messageObserverJob?.cancel()
        messageObserverJob = null

        // Scarlet은 명시적 disconnect가 없으므로 null 처리
        scarlet = null
        webSocketApi = null
        currentSessionCode = null

        _connectionState.value = WebSocketConnectionState.DISCONNECTED
        _connectedSessionCode.value = null
    }

    /**
     * 현재 WebSocketApi 가져오기
     */
    fun getApi(): WebSocketApi? = webSocketApi

    /**
     * 연결되어 있는지 확인
     */
    fun isConnected(): Boolean =
        _connectionState.value == WebSocketConnectionState.CONNECTED && webSocketApi != null

    /**
     * WebSocket 이벤트 관찰
     */
    fun observeWebSocketEvents(): Flow<WebSocket.Event>? {
        return webSocketApi?.observeWebSocketEvent()?.receiveAsFlow()
    }

    /**
     * 메시지 수신 관찰
     * SharedFlow를 반환하므로 WebSocket 연결 전에도 collect 가능
     */
    fun observeMessages(): Flow<SessionMessage> {
        return messages
    }

    /**
     * 메시지 전송
     */
    fun sendMessage(message: ClientMessage) {
        webSocketApi?.sendMessage(message)
        Log.d(TAG, "Sent message: ${message.type}")
    }

    /**
     * Join 메시지 전송
     * @param deviceId 기기 고유값
     * @param name 사용자 이름
     */
    fun sendJoinMessage(deviceId: String, name: String) {
        sendMessage(
            ClientMessage(
                type = MessageType.JOIN,
                data = mapOf(
                    "device_id" to deviceId,
                    "name" to name
                )
            )
        )
    }

    /**
     * 하트비트 전송
     */
    fun sendHeartbeat() {
        sendMessage(
            ClientMessage(
                type = MessageType.HEARTBEAT,
                data = mapOf("timestamp" to System.currentTimeMillis())
            )
        )
    }

    /**
     * 단계 완료 전송
     */
    fun sendStepComplete(subtaskId: Int) {
        sendMessage(
            ClientMessage(
                type = MessageType.STEP_COMPLETE,
                data = mapOf("subtask_id" to subtaskId)
            )
        )
    }

    /**
     * 도움 요청 전송 (메시지 없이 즉시)
     */
    fun sendHelpRequest(subtaskId: Int? = null) {
        sendMessage(
            ClientMessage(
                type = MessageType.REQUEST_HELP,
                data = subtaskId?.let { mapOf("subtask_id" to it) }
            )
        )
    }

    /**
     * 연결 상태 모니터링
     */
    private fun observeConnectionState() {
        scope.launch {
            try {
                webSocketApi?.observeWebSocketEvent()?.receiveAsFlow()?.collect { event ->
                    when (event) {
                        is WebSocket.Event.OnConnectionOpened<*> -> {
                            Log.d(TAG, "WebSocket connected")
                            _connectionState.value = WebSocketConnectionState.CONNECTED
                        }
                        is WebSocket.Event.OnConnectionClosing -> {
                            Log.d(TAG, "WebSocket closing")
                            // 연결 종료 중 - 상태 유지
                        }
                        is WebSocket.Event.OnConnectionClosed -> {
                            Log.d(TAG, "WebSocket closed")
                            _connectionState.value = WebSocketConnectionState.DISCONNECTED
                        }
                        is WebSocket.Event.OnConnectionFailed -> {
                            Log.e(TAG, "WebSocket failed: ${event.throwable.message}")
                            _connectionState.value = WebSocketConnectionState.ERROR
                        }
                        is WebSocket.Event.OnMessageReceived -> {
                            // 메시지는 별도 Flow에서 처리
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error observing connection state", e)
                _connectionState.value = WebSocketConnectionState.ERROR
            }
        }
    }

    /**
     * 주기적 하트비트 전송
     */
    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (true) {
                delay(HEARTBEAT_INTERVAL_MS)
                if (isConnected()) {
                    sendHeartbeat()
                }
            }
        }
    }

    /**
     * 메시지 관찰 시작
     * 새 WebSocket 연결의 메시지를 SharedFlow로 전달
     */
    private fun startMessageObserver() {
        messageObserverJob?.cancel()
        messageObserverJob = scope.launch {
            Log.d(TAG, "startMessageObserver: Starting message collection, subscribers=${_messages.subscriptionCount.value}")
            try {
                webSocketApi?.observeMessages()?.receiveAsFlow()?.collect { message ->
                    Log.d(TAG, "Received message: ${message.type}, data=${message.data}")
                    try {
                        _messages.emit(message)
                        Log.d(TAG, "Message emitted successfully to SharedFlow, subscribers=${_messages.subscriptionCount.value}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to emit message", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error observing messages", e)
            }
        }
    }
}
