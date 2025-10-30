package com.mobilegpt.student.data.api

import com.mobilegpt.student.domain.model.ClientMessage
import com.mobilegpt.student.domain.model.SessionMessage
import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import kotlinx.coroutines.flow.Flow

/**
 * WebSocket API Interface
 * Scarlet을 사용한 WebSocket 통신
 */
interface WebSocketApi {

    /**
     * WebSocket 연결 상태 관찰
     */
    @Receive
    fun observeWebSocketEvent(): Flow<WebSocket.Event>

    /**
     * 서버로부터 메시지 수신
     */
    @Receive
    fun observeMessages(): Flow<SessionMessage>

    /**
     * 서버로 메시지 전송
     */
    @Send
    fun sendMessage(message: ClientMessage)
}
