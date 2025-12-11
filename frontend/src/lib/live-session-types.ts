// Types for Live Session Page

import { SessionLecture } from './types';

/**
 * 단계(Subtask) 정보
 * 세션에서 진행되는 각 단계의 상세 정보
 */
export interface SubtaskInfo {
  id: number;
  title: string;
  description?: string;
  orderIndex: number;
  targetAction?: string;
  guideText?: string;
  taskId?: number;
  taskTitle?: string;
}

export interface LiveSessionData {
  sessionId: number;
  sessionCode: string;
  lectureName: string;
  lectureDate: string;
  instructor: string;
  totalStudents: number;
  status: 'CREATED' | 'ACTIVE' | 'IN_PROGRESS' | 'PAUSED' | 'ENDED' | 'REVIEW_MODE';
  startedAt?: string;
  lectures?: SessionLecture[]; // Multiple lectures in session
  activeLectureId?: number;
  // Subtask tracking
  subtasks: SubtaskInfo[];           // 전체 단계 목록
  currentSubtask: SubtaskInfo | null; // 현재 진행 중인 단계
  currentSubtaskIndex: number;       // 현재 단계 인덱스 (0-based)
  totalSubtasks: number;             // 전체 단계 수
}

export interface StudentListItem {
  id: number;
  name: string;
  avatarUrl?: string;
  isSelected: boolean;
  status: 'active' | 'inactive' | 'help_needed';
  deviceId?: string;
  hasRecentScreenshot?: boolean;
  lastScreenshotAt?: string;
  // Step completion tracking
  completedSubtasks?: number[];        // 완료된 Subtask ID 배열
  currentStepCompleted?: boolean;      // 현재 단계 완료 여부
  lastCompletedAt?: string;            // 마지막 완료 시간
  currentSubtaskId?: number;           // 현재 진행 중인 단계 ID
  progressPercentage?: number;         // 진행률 (0-100)
}

export interface ProgressData {
  label: string;
  current: number;
  total: number;
  percentage: number;
  color: string;
}

export interface GroupProgress {
  groupId: number;
  groupName: string;
  currentTask: string;
  participants: {
    id: number;
    name: string;
    avatarUrl?: string;
  }[];
}

export interface LiveNotification {
  id: number;
  type: 'help_request' | 'progress_alert' | 'system';
  title: string;
  message: string;
  timestamp: string;
  studentId?: number;
  studentName?: string;
  isResolved: boolean;
  screenshotUrl?: string; // 도움 요청 시 캡처한 스크린샷 URL
}

export interface StudentScreen {
  studentId: number;
  studentName: string;
  imageUrl?: string;
  lastUpdated: string;
  isLoading: boolean;
  error?: string;
  deviceId?: string;
}