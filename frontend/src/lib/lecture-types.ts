// Types for Lecture Management

// Phone action log recorded from instructor's device (Android AccessibilityEvent)
export interface PhoneActionLog {
  time: number; // Event time (device basis, ms unit UNIX timestamp)
  package: string; // App package name (e.g., com.google.android.youtube)
  className: string; // UI component class name (e.g., android.widget.Button)
  text: string; // UI displayed text (button label, input value, etc)
  contentDescription: string; // Accessibility description
  viewId: string; // View resource ID (e.g., com.app:id/search_button)
  isClickable: boolean; // Whether the view is clickable
  isEditable: boolean; // Whether the view is editable (EditText, etc)
  isEnabled: boolean; // Whether the view is enabled
  isFocused: boolean; // Whether the view is focused
  bounds: string; // Screen coordinates ([left,top][right,bottom] format)
  server_time: number; // Server received time (server basis, ms unit)
}

// Individual step in a lecture
export interface LectureStep {
  id: string;
  order: number;
  title: string; // e.g., "홈 화면에서 유튜브 앱 찾기"
  description: string; // detailed description
  action: string; // e.g., "유튜브 아이콘을 터치하세요"
  expectedResult: string; // e.g., "유튜브 앱이 실행됩니다"
  imageUrl?: string; // optional screenshot
  tips?: string; // optional tips for students
  // Technical details from log (optional, for debugging)
  technicalDetails?: {
    targetPackage?: string; // app package
    targetViewId?: string; // view ID
    targetText?: string; // button text
    contentDescription?: string; // accessibility description
    bounds?: string; // coordinates
  };
}

export interface Lecture {
  id: number;
  title: string;
  description: string;
  studentCount: number;
  sessionCount: number;
  taskCount: number;        // 과제 수 (백엔드에서 계산)
  stepCount: number;        // 총 단계 수 = 모든 Task의 Subtask 합계
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  instructor: string;
  difficulty: 'beginner' | 'intermediate' | 'advanced';
  duration: number; // in minutes
  steps: LectureStep[]; // lecture steps (상세 조회 시에만 로드)
  recordingId?: string; // ID of the phone recording used to generate this lecture
}

export interface CreateLectureRequest {
  title: string;
  description: string;
  difficulty: 'beginner' | 'intermediate' | 'advanced';
  duration: number;
  steps: LectureStep[];
  recordingId?: string;
}

export interface UpdateLectureRequest extends Partial<CreateLectureRequest> {
  id: number;
  isActive?: boolean;
}

// Recording metadata
export interface RecordingMetadata {
  id: string;
  name: string; // User-provided name or auto-generated
  createdAt: string;
  actionCount: number; // Total number of events
  duration: number; // Recording duration in seconds
  apps: string[]; // List of app package names used
  primaryApp: string; // Main app used (most frequent)
  deviceInfo?: {
    model?: string;
    androidVersion?: string;
  };
}

// Response from server after uploading/processing phone recording
export interface RecordingProcessResponse {
  recordingId: string;
  generatedSteps: LectureStep[];
  detectedActions: number;
  processingTime: number;
  metadata: {
    appsUsed: { package: string; name: string; count: number }[];
    totalDuration: number; // in seconds
    eventTypes: { type: string; count: number }[];
  };
}

// Backend Subtask structure (from Task conversion)
export interface BackendSubtask {
  id: number;
  task: number;
  title: string;
  description: string;
  order_index: number;
  target_action: string;
  target_element_hint: string;
  guide_text: string;
  voice_guide_text: string;
  time: number | null;
  text: string;
  content_description: string;
  view_id: string;
  bounds: string;
  target_package: string;
  target_class: string;
  created_at: string;
  updated_at: string;
}

// Available Task (not linked to any Lecture yet)
export interface AvailableTask {
  id: number;
  lecture: number | null;
  title: string;
  description: string;
  order_index: number;
  subtasks: BackendSubtask[];
  subtask_count: number;
  created_at: string;
  updated_at: string;
}