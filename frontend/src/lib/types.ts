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