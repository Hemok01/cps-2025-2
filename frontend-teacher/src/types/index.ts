// User Types
export interface User {
  id: number;
  email: string;
  name: string;
  role: 'INSTRUCTOR' | 'STUDENT';
  age: number;
  phone?: string;
  digital_level: 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED';
  created_at: string;
}

export interface AuthTokens {
  access: string;
  refresh: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  access: string;
  refresh: string;
}

// Lecture Types
export interface Lecture {
  id: number;
  title: string;
  description: string;
  thumbnail_url?: string;
  instructor: User;
  is_active: boolean;
  created_at: string;
  updated_at: string;
}

// Task & Subtask Types
export interface Task {
  id: number;
  lecture: number;
  title: string;
  description: string;
  order_index: number;
  created_at: string;
}

export interface Subtask {
  id: number;
  task: number;
  order_index: number;
  target_action: 'CLICK' | 'LONG_CLICK' | 'SCROLL' | 'INPUT' | 'NAVIGATE';
  target_element_hint: string;
  guide_text: string;
  voice_guide_text?: string;
  created_at: string;
}

// Session Types
export type SessionStatus = 'WAITING' | 'IN_PROGRESS' | 'ENDED' | 'REVIEW_MODE';

export interface LectureSession {
  id: number;
  lecture: number;
  lecture_title?: string;
  instructor: number;
  instructor_name?: string;
  session_code: string;
  status: SessionStatus;
  current_subtask?: number;
  current_subtask_details?: Subtask;
  started_at?: string;
  ended_at?: string;
  created_at: string;
}

export interface SessionParticipant {
  id: number;
  session: number;
  user: number;
  user_name: string;
  user_email: string;
  joined_at: string;
  is_active: boolean;
}

export interface CreateSessionRequest {
  lecture_id: number;
  title: string;
}

export interface CreateSessionResponse extends LectureSession {
  // The backend returns a full LectureSession object
}

// Progress Types
export type ProgressStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED' | 'HELP_NEEDED';

export interface UserProgress {
  id: number;
  user: number;
  user_name?: string;
  subtask: number;
  subtask_details?: Subtask;
  lecture: number;
  session?: number;
  status: ProgressStatus;
  attempts: number;
  help_count: number;
  started_at?: string;
  completed_at?: string;
  updated_at: string;
}

// Dashboard Types
export interface StudentProgress {
  user_id: number;
  user_name: string;
  user_email: string;
  progress_rate: number;
  current_subtask?: Subtask;
  status: ProgressStatus;
  help_request_count: number;
  last_activity?: string;
}

export interface PendingHelpRequest {
  id: number;
  user: User;
  session: LectureSession;
  subtask: Subtask;
  request_type: 'MANUAL' | 'AUTO';
  status: 'PENDING' | 'ANALYZING' | 'RESOLVED';
  created_at: string;
}

export interface LectureStatistics {
  lecture_id: number;
  lecture_title: string;
  total_students: number;
  total_help_requests: number;
  common_difficulties: Array<{
    subtask: Subtask;
    help_count: number;
  }>;
}

// Help Request Types
export interface HelpRequest {
  id: number;
  user: number;
  user_name?: string;
  session: number;
  subtask: number;
  subtask_details?: Subtask;
  request_type: 'MANUAL' | 'AUTO';
  status: 'PENDING' | 'ANALYZING' | 'RESOLVED';
  description?: string;
  created_at: string;
  resolved_at?: string;
}

export interface MGptAnalysis {
  id: number;
  help_request: number;
  problem_diagnosis: string;
  suggested_help: string;
  confidence_score: number;
  created_at: string;
}

// WebSocket Message Types
export interface WSMessage {
  type: string;
  data: any;
}

export interface ProgressUpdateMessage extends WSMessage {
  type: 'progress_update';
  data: {
    user_id: number;
    user_name: string;
    subtask_id: number;
    status: ProgressStatus;
    progress_rate: number;
  };
}

export interface HelpRequestMessage extends WSMessage {
  type: 'help_request';
  data: {
    request_id: number;
    user_id: number;
    user_name: string;
    subtask_id: number;
    request_type: 'MANUAL' | 'AUTO';
  };
}

export interface ParticipantStatusMessage extends WSMessage {
  type: 'participant_status';
  data: {
    user_id: number;
    user_name: string;
    status: 'joined' | 'left';
  };
}

export interface StepChangedMessage extends WSMessage {
  type: 'step_changed';
  data: {
    subtask_id: number;
    subtask: Subtask;
  };
}

export interface SessionStatusMessage extends WSMessage {
  type: 'session_status_changed';
  data: {
    status: SessionStatus;
  };
}

// API Response Types
export interface ApiResponse<T> {
  data: T;
  message?: string;
}

export interface ApiError {
  detail: string;
  code?: string;
}

export interface PaginatedResponse<T> {
  count: number;
  next?: string;
  previous?: string;
  results: T[];
}
