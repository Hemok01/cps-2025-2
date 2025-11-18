// WebSocket Client for real-time updates

const WS_BASE_URL = import.meta.env.VITE_WS_BASE_URL || 'ws://localhost:8000/ws';

export type WebSocketMessage = {
  type: 'session_update' | 'student_progress' | 'help_request' | 'notification';
  data: any;
};

export type WebSocketCallback = (message: WebSocketMessage) => void;

class WebSocketClient {
  private ws: WebSocket | null = null;
  private reconnectTimeout: NodeJS.Timeout | null = null;
  private reconnectDelay = 3000; // 3 seconds
  private callbacks: Set<WebSocketCallback> = new Set();
  private sessionId: number | null = null;
  private isIntentionallyClosed = false;

  connect(sessionId: number) {
    this.sessionId = sessionId;
    this.isIntentionallyClosed = false;

    // Get auth token
    const tokens = localStorage.getItem('auth_tokens');
    let accessToken = '';
    if (tokens) {
      try {
        const { access } = JSON.parse(tokens);
        accessToken = access;
      } catch (error) {
        console.error('Failed to parse auth tokens:', error);
      }
    }

    // Connect to WebSocket with session ID and token
    const wsUrl = `${WS_BASE_URL}/session/${sessionId}/?token=${accessToken}`;
    this.ws = new WebSocket(wsUrl);

    this.ws.onopen = () => {
      console.log(`WebSocket connected to session ${sessionId}`);
    };

    this.ws.onmessage = (event) => {
      try {
        const message: WebSocketMessage = JSON.parse(event.data);
        this.callbacks.forEach(callback => callback(message));
      } catch (error) {
        console.error('Failed to parse WebSocket message:', error);
      }
    };

    this.ws.onerror = (error) => {
      console.error('WebSocket error:', error);
    };

    this.ws.onclose = () => {
      console.log('WebSocket disconnected');
      this.ws = null;

      // Auto-reconnect if not intentionally closed
      if (!this.isIntentionallyClosed && this.sessionId) {
        console.log(`Reconnecting in ${this.reconnectDelay}ms...`);
        this.reconnectTimeout = setTimeout(() => {
          if (this.sessionId) {
            this.connect(this.sessionId);
          }
        }, this.reconnectDelay);
      }
    };
  }

  disconnect() {
    this.isIntentionallyClosed = true;

    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
      this.reconnectTimeout = null;
    }

    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }

    this.sessionId = null;
  }

  subscribe(callback: WebSocketCallback) {
    this.callbacks.add(callback);

    // Return unsubscribe function
    return () => {
      this.callbacks.delete(callback);
    };
  }

  send(message: any) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message));
    } else {
      console.error('WebSocket is not connected');
    }
  }

  isConnected(): boolean {
    return this.ws !== null && this.ws.readyState === WebSocket.OPEN;
  }
}

// Export singleton instance
export const wsClient = new WebSocketClient();
