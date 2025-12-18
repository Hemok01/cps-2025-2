// Core types for the MobileGPT Instructor Dashboard

export interface User {
  id: number;
  email: string;
  name: string;
  role: 'INSTRUCTOR' | 'STUDENT';
}

export interface AuthTokens {
  access: string;
  refresh: string;
}

export interface Lecture {
  id: number;
  title: string;
  description: string;
  isActive: boolean;
  studentCount: number;
  sessionCount: number;
}

export type SessionStatus = 'CREATED' | 'ACTIVE' | 'PAUSED' | 'ENDED';

export interface SessionLecture {
  id: number;
  lectureId: number;
  lectureName: string;
  order: number; // Order in the session
  isActive: boolean; // Currently active lecture in the session
  completedAt?: string;
}

export interface Session {
  id: number;
  title: string;
  code: string;
  session_code?: string; // Alias for compatibility
  status: SessionStatus;
  createdAt: string;
  startedAt?: string;
  endedAt?: string;
  currentStep?: string;
  lectures: SessionLecture[]; // Multiple lectures in one session
  activeLectureId?: number; // Currently active lecture
  participantCount?: number; // Number of participants in the session
}

export interface Participant {
  id: number;
  name: string;
  email: string;
  joinedAt: string;
  isActive: boolean;
}

export interface StudentProgress {
  studentId: number;
  studentName: string;
  currentStep: string;
  progress: number; // 0-100
  status: 'completed' | 'in_progress' | 'not_started' | 'help_needed' | 'paused';
  helpCount: number;
  lastActivity: string;
}

export type HelpRequestType = 'MANUAL' | 'AUTO';

export interface HelpRequest {
  id: number;
  studentId: number;
  studentName: string;
  studentEmail: string;
  type: HelpRequestType;
  currentStep: string;
  requestedAt: string;
  isResolved: boolean;
  mgptAnalysis?: {
    difficultyLevel: number;
    stuckDuration: number;
    errorPatterns: string[];
    suggestedHint: string;
    confidence: number;
  };
}

export interface LectureStatistics {
  lectureId: number;
  lectureName: string;
  totalStudents: number;
  totalHelpRequests: number;
  averageProgress: number; // 0-100
  completionRate: number; // 0-100
  difficultSteps: DifficultStep[];
  lastUpdated: string;
}

export interface DifficultStep {
  subtaskName: string;
  helpRequestCount: number;
  avgTimeSpent: number; // seconds
  studentCount: number;
}

// 단계별 분석 타입
export interface StepAnalysisItem {
  subtaskId: number;
  subtaskName: string;
  taskName: string;
  orderIndex: number;
  avgTimeSpent: number; // seconds
  delayRate: number; // 0-1
  helpRequestCount: number;
  studentCount: number;
  completionRate: number; // 0-1
  bottleneckScore: number; // 0-1
}

export interface StepAnalysisData {
  lectureId: number;
  lectureName: string;
  totalSubtasks: number;
  stepAnalysis: StepAnalysisItem[];
  summary: {
    mostDelayedStep: string | null;
    mostHelpRequestedStep: string | null;
    avgOverallDelayRate: number;
  };
  lastUpdated: string;
}

// 세션 비교 타입
export type TrendDirection = 'improving' | 'stable' | 'declining' | 'insufficient_data';

export interface SessionTrendItem {
  sessionId: number;
  sessionTitle: string;
  sessionDate: string | null;
  startedAt: string | null;
  endedAt: string | null;
  participantCount: number;
  completionRate: number; // 0-1
  avgCompletionTime: number; // seconds
  totalHelpRequests: number;
  helpRequestRate: number;
}

export interface SessionComparisonData {
  lectureId: number;
  lectureName: string;
  sessions: SessionTrendItem[];
  trendSummary: {
    completionRateTrend: TrendDirection;
    helpRequestTrend: TrendDirection;
    avgCompletionTimeTrend: TrendDirection;
  };
  lastUpdated: string;
}

// 세션 요약 타입
export interface SessionSummaryParticipant {
  id: number;
  name: string;
  status: string;
  completedCount: number;
  totalSteps: number;
  progressRate: number;
  joinedAt: string | null;
  completedAt: string | null;
}

export interface SessionSummaryDifficultStep {
  subtaskId: number;
  subtaskName: string;
  helpRequestCount: number;
}

export interface SessionSummary {
  sessionId: number;
  sessionTitle: string;
  sessionCode: string;
  lectureId: number | null;
  lectureName: string | null;
  status: string;

  // 시간 정보
  startedAt: string | null;
  endedAt: string | null;
  durationSeconds: number;

  // 참가자 통계
  totalParticipants: number;
  completedParticipants: number;
  completionRate: number;
  avgProgress: number;

  // 단계 정보
  totalSteps: number;
  subtaskCompletionStats: Record<number, number>;

  // 도움 요청 통계
  totalHelpRequests: number;
  resolvedHelpRequests: number;
  helpResolutionRate: number;

  // 어려운 단계
  difficultSteps: SessionSummaryDifficultStep[];

  // 참가자 상세
  participants: SessionSummaryParticipant[];
}