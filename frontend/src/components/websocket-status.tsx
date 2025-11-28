import { useEffect, useState } from 'react';
import { wsClient } from '../lib/websocket-client';
import type { WebSocketConnectionInfo } from '../types/websocket';
import { Wifi, WifiOff, RefreshCw, AlertCircle } from 'lucide-react';

interface WebSocketStatusProps {
  className?: string;
  showText?: boolean;
}

export function WebSocketStatus({ className = '', showText = true }: WebSocketStatusProps) {
  const [connectionInfo, setConnectionInfo] = useState<WebSocketConnectionInfo>({
    status: 'disconnected',
    sessionCode: null,
    reconnectAttempts: 0,
  });

  useEffect(() => {
    const unsubscribe = wsClient.subscribeToStatus(setConnectionInfo);
    return unsubscribe;
  }, []);

  const getStatusConfig = () => {
    switch (connectionInfo.status) {
      case 'connected':
        return {
          icon: <Wifi className="w-4 h-4" />,
          text: '연결됨',
          color: 'text-green-600',
          bgColor: 'bg-green-50',
          borderColor: 'border-green-200',
        };
      case 'connecting':
        return {
          icon: <RefreshCw className="w-4 h-4 animate-spin" />,
          text: '연결 중...',
          color: 'text-blue-600',
          bgColor: 'bg-blue-50',
          borderColor: 'border-blue-200',
        };
      case 'reconnecting':
        return {
          icon: <RefreshCw className="w-4 h-4 animate-spin" />,
          text: `재연결 중 (${connectionInfo.reconnectAttempts}/5)`,
          color: 'text-yellow-600',
          bgColor: 'bg-yellow-50',
          borderColor: 'border-yellow-200',
        };
      case 'error':
        return {
          icon: <AlertCircle className="w-4 h-4" />,
          text: '연결 오류',
          color: 'text-red-600',
          bgColor: 'bg-red-50',
          borderColor: 'border-red-200',
        };
      case 'disconnected':
      default:
        return {
          icon: <WifiOff className="w-4 h-4" />,
          text: '연결 끊김',
          color: 'text-gray-600',
          bgColor: 'bg-gray-50',
          borderColor: 'border-gray-200',
        };
    }
  };

  const config = getStatusConfig();

  return (
    <div
      className={`flex items-center gap-2 px-3 py-1.5 rounded-lg border ${config.bgColor} ${config.borderColor} ${className}`}
    >
      <div className={config.color}>{config.icon}</div>
      {showText && (
        <span className={`text-sm font-medium ${config.color}`}>
          {config.text}
        </span>
      )}
    </div>
  );
}

// Compact version for status bars
export function WebSocketStatusCompact() {
  const [connectionInfo, setConnectionInfo] = useState<WebSocketConnectionInfo>({
    status: 'disconnected',
    sessionCode: null,
    reconnectAttempts: 0,
  });

  useEffect(() => {
    const unsubscribe = wsClient.subscribeToStatus(setConnectionInfo);
    return unsubscribe;
  }, []);

  const getStatusColor = () => {
    switch (connectionInfo.status) {
      case 'connected':
        return 'bg-green-500';
      case 'connecting':
      case 'reconnecting':
        return 'bg-yellow-500 animate-pulse';
      case 'error':
        return 'bg-red-500';
      default:
        return 'bg-gray-400';
    }
  };

  const getStatusText = () => {
    switch (connectionInfo.status) {
      case 'connected':
        return '실시간 연결';
      case 'connecting':
        return '연결 중';
      case 'reconnecting':
        return `재연결 중 (${connectionInfo.reconnectAttempts}/5)`;
      case 'error':
        return '연결 오류';
      default:
        return '오프라인';
    }
  };

  return (
    <div className="flex items-center gap-2">
      <div className={`w-2 h-2 rounded-full ${getStatusColor()}`} />
      <span className="text-xs text-gray-600">{getStatusText()}</span>
    </div>
  );
}
