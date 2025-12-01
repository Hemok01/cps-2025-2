/**
 * WebSocket Message Types
 * These types match the backend SessionConsumer message types
 */

// ============================================================================
// Incoming Messages (from server)
// ============================================================================

export type IncomingMessageType =
  | 'step_changed'
  | 'session_status_changed'
  | 'participant_joined'
  | 'participant_left'
  | 'progress_updated'
  | 'student_completion'
  | 'help_requested'
  | 'screenshot_updated'
  | 'error';

export interface BaseIncomingMessage {
  type: IncomingMessageType;
  timestamp?: string;
}

export interface StepChangedMessage extends BaseIncomingMessage {
  type: 'step_changed';
  data: {
    session_code: string;
    current_step: number;
    total_steps: number;
    step_title?: string;
    step_description?: string;
  };
}

export interface SessionStatusChangedMessage extends BaseIncomingMessage {
  type: 'session_status_changed';
  data: {
    session_code: string;
    status: 'active' | 'paused' | 'completed';
    message?: string;
  };
}

export interface ParticipantJoinedMessage extends BaseIncomingMessage {
  type: 'participant_joined';
  data: {
    user_id: number;
    username: string;
    role: 'instructor' | 'student';
  };
}

export interface ParticipantLeftMessage extends BaseIncomingMessage {
  type: 'participant_left';
  data: {
    user_id: number;
    username: string;
  };
}

export interface ProgressUpdatedMessage extends BaseIncomingMessage {
  type: 'progress_updated';
  data: {
    user_id: number;
    username: string;
    current_subtask: number | null;
    progress_percentage: number;
    completed_subtasks: number[];
  };
}

export interface StudentCompletionMessage extends BaseIncomingMessage {
  type: 'student_completion';
  data: {
    device_id: string;
    participant_id: number | null;
    student_name: string;
    subtask_id: number;
    completed_subtasks: number[];
    total_completed: number;
    timestamp: string;
  };
}

export interface HelpRequestedMessage extends BaseIncomingMessage {
  type: 'help_requested';
  data: {
    user_id: number;
    username: string;
    subtask_id: number | null;
    message: string;
    timestamp: string;
  };
}

export interface ErrorMessage extends BaseIncomingMessage {
  type: 'error';
  data: {
    message: string;
    code?: string;
  };
}

export interface ScreenshotUpdatedMessage extends BaseIncomingMessage {
  type: 'screenshot_updated';
  data: {
    participant_id: number | null;
    device_id: string;
    participant_name: string;
    image_url: string;
    captured_at: string;
  };
}

export type IncomingMessage =
  | StepChangedMessage
  | SessionStatusChangedMessage
  | ParticipantJoinedMessage
  | ParticipantLeftMessage
  | ProgressUpdatedMessage
  | StudentCompletionMessage
  | HelpRequestedMessage
  | ScreenshotUpdatedMessage
  | ErrorMessage;

// ============================================================================
// Outgoing Messages (to server)
// ============================================================================

export type OutgoingMessageType =
  // Instructor commands
  | 'next_step'
  | 'pause_session'
  | 'resume_session'
  | 'end_session'
  // Student messages
  | 'join'
  | 'heartbeat'
  | 'step_complete'
  | 'request_help'
  // Legacy (for backward compatibility)
  | 'progress_update'
  | 'help_request';

export interface BaseOutgoingMessage {
  type: OutgoingMessageType;
}

// Instructor Commands
export interface NextStepMessage extends BaseOutgoingMessage {
  type: 'next_step';
  data?: Record<string, never>;
}

export interface PauseSessionMessage extends BaseOutgoingMessage {
  type: 'pause_session';
  data?: Record<string, never>;
}

export interface ResumeSessionMessage extends BaseOutgoingMessage {
  type: 'resume_session';
  data?: Record<string, never>;
}

export interface EndSessionMessage extends BaseOutgoingMessage {
  type: 'end_session';
  data?: Record<string, never>;
}

// Student Messages
export interface JoinMessage extends BaseOutgoingMessage {
  type: 'join';
  data?: Record<string, never>;
}

export interface HeartbeatMessage extends BaseOutgoingMessage {
  type: 'heartbeat';
  data?: Record<string, never>;
}

export interface StepCompleteMessage extends BaseOutgoingMessage {
  type: 'step_complete';
  data: {
    subtask_id: number;
  };
}

export interface RequestHelpMessage extends BaseOutgoingMessage {
  type: 'request_help';
  data: {
    subtask_id?: number;
    message: string;
  };
}

// Legacy Messages
export interface ProgressUpdateMessage extends BaseOutgoingMessage {
  type: 'progress_update';
  data: {
    subtask_id: number;
    status: 'completed' | 'in_progress';
  };
}

export interface HelpRequestMessage extends BaseOutgoingMessage {
  type: 'help_request';
  data: {
    subtask_id?: number;
    message: string;
  };
}

export type OutgoingMessage =
  | NextStepMessage
  | PauseSessionMessage
  | ResumeSessionMessage
  | EndSessionMessage
  | JoinMessage
  | HeartbeatMessage
  | StepCompleteMessage
  | RequestHelpMessage
  | ProgressUpdateMessage
  | HelpRequestMessage;

// ============================================================================
// WebSocket Client Types
// ============================================================================

export type WebSocketCallback = (message: IncomingMessage) => void;

export type WebSocketConnectionStatus =
  | 'disconnected'
  | 'connecting'
  | 'connected'
  | 'reconnecting'
  | 'error';

export interface WebSocketConnectionInfo {
  status: WebSocketConnectionStatus;
  sessionCode: string | null;
  reconnectAttempts: number;
  lastError?: string;
}
