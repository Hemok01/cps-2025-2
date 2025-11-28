// WebSocket Client for real-time updates

import type {
  IncomingMessage,
  OutgoingMessage,
  WebSocketCallback,
  WebSocketConnectionStatus,
  WebSocketConnectionInfo,
} from '../types/websocket';

const WS_BASE_URL = import.meta.env.VITE_WS_BASE_URL || 'ws://localhost:8000/ws';

class WebSocketClient {
  private ws: WebSocket | null = null;
  private reconnectTimeout: NodeJS.Timeout | null = null;
  private heartbeatInterval: NodeJS.Timeout | null = null;
  private reconnectDelay = 3000; // 3 seconds
  private heartbeatDelay = 30000; // 30 seconds
  private maxReconnectAttempts = 5;
  private reconnectAttempts = 0;
  private callbacks: Set<WebSocketCallback> = new Set();
  private sessionCode: string | null = null;
  private isIntentionallyClosed = false;
  private connectionStatus: WebSocketConnectionStatus = 'disconnected';
  private statusCallbacks: Set<(info: WebSocketConnectionInfo) => void> = new Set();

  connect(sessionCode: string) {
    this.sessionCode = sessionCode;
    this.isIntentionallyClosed = false;

    // Update connection status
    this.updateStatus('connecting');

    // Get JWT token from localStorage for authentication
    const token = localStorage.getItem('accessToken');

    // Connect to WebSocket with session code and JWT token
    // Note: Backend expects /ws/sessions/{session_code}/?token={jwt_token}
    let wsUrl = `${WS_BASE_URL}/sessions/${sessionCode}/`;
    if (token) {
      wsUrl += `?token=${encodeURIComponent(token)}`;
      console.log(`[WebSocket] Connecting with authentication to: ${WS_BASE_URL}/sessions/${sessionCode}/`);
    } else {
      console.log(`[WebSocket] Connecting without authentication to: ${wsUrl}`);
    }

    this.ws = new WebSocket(wsUrl);

    this.ws.onopen = () => {
      console.log(`[WebSocket] Connected to session ${sessionCode}`);
      this.reconnectAttempts = 0;
      this.updateStatus('connected');

      // Send join message to authenticate and register
      this.send({ type: 'join', data: {} });

      // Start heartbeat to keep connection alive
      this.startHeartbeat();
    };

    this.ws.onmessage = (event) => {
      try {
        const message: IncomingMessage = JSON.parse(event.data);
        console.log('[WebSocket] Received:', message.type, message);
        this.callbacks.forEach(callback => callback(message));
      } catch (error) {
        console.error('[WebSocket] Failed to parse message:', error);
      }
    };

    this.ws.onerror = (error) => {
      console.error('[WebSocket] Error:', error);
      this.updateStatus('error', 'Connection error occurred');
    };

    this.ws.onclose = (event) => {
      console.log(`[WebSocket] Disconnected (code: ${event.code}, reason: ${event.reason})`);
      this.ws = null;
      this.stopHeartbeat();

      // Auto-reconnect if not intentionally closed and within retry limit
      if (!this.isIntentionallyClosed && this.sessionCode) {
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
          this.reconnectAttempts++;
          this.updateStatus('reconnecting');
          console.log(`[WebSocket] Reconnecting (attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts})...`);

          this.reconnectTimeout = setTimeout(() => {
            if (this.sessionCode) {
              this.connect(this.sessionCode);
            }
          }, this.reconnectDelay);
        } else {
          console.error('[WebSocket] Max reconnection attempts reached');
          this.updateStatus('error', 'Max reconnection attempts reached');
        }
      } else {
        this.updateStatus('disconnected');
      }
    };
  }

  private startHeartbeat() {
    this.stopHeartbeat();
    this.heartbeatInterval = setInterval(() => {
      if (this.isConnected()) {
        this.send({ type: 'heartbeat', data: {} });
      }
    }, this.heartbeatDelay);
  }

  private stopHeartbeat() {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
      this.heartbeatInterval = null;
    }
  }

  private updateStatus(status: WebSocketConnectionStatus, error?: string) {
    this.connectionStatus = status;
    const info: WebSocketConnectionInfo = {
      status,
      sessionCode: this.sessionCode,
      reconnectAttempts: this.reconnectAttempts,
      lastError: error,
    };
    this.statusCallbacks.forEach(callback => callback(info));
  }

  disconnect() {
    this.isIntentionallyClosed = true;
    this.reconnectAttempts = 0;

    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
      this.reconnectTimeout = null;
    }

    this.stopHeartbeat();

    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }

    this.sessionCode = null;
    this.updateStatus('disconnected');
  }

  subscribe(callback: WebSocketCallback) {
    this.callbacks.add(callback);

    // Return unsubscribe function
    return () => {
      this.callbacks.delete(callback);
    };
  }

  subscribeToStatus(callback: (info: WebSocketConnectionInfo) => void) {
    this.statusCallbacks.add(callback);

    // Send current status immediately
    callback({
      status: this.connectionStatus,
      sessionCode: this.sessionCode,
      reconnectAttempts: this.reconnectAttempts,
    });

    // Return unsubscribe function
    return () => {
      this.statusCallbacks.delete(callback);
    };
  }

  send(message: OutgoingMessage) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      console.log('[WebSocket] Sending:', message.type, message);
      this.ws.send(JSON.stringify(message));
    } else {
      console.error('[WebSocket] Cannot send message - not connected');
    }
  }

  isConnected(): boolean {
    return this.ws !== null && this.ws.readyState === WebSocket.OPEN;
  }

  getStatus(): WebSocketConnectionStatus {
    return this.connectionStatus;
  }

  getSessionCode(): string | null {
    return this.sessionCode;
  }

  // Convenience methods for common messages

  sendNextStep() {
    this.send({ type: 'next_step', data: {} });
  }

  sendPauseSession() {
    this.send({ type: 'pause_session', data: {} });
  }

  sendResumeSession() {
    this.send({ type: 'resume_session', data: {} });
  }

  sendEndSession() {
    this.send({ type: 'end_session', data: {} });
  }

  sendStepComplete(subtaskId: number) {
    this.send({
      type: 'step_complete',
      data: { subtask_id: subtaskId },
    });
  }

  sendRequestHelp(message: string, subtaskId?: number) {
    this.send({
      type: 'request_help',
      data: {
        message,
        subtask_id: subtaskId,
      },
    });
  }
}

// Export singleton instance
export const wsClient = new WebSocketClient();
