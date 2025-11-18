// Types for Live Session Page

import { SessionLecture } from './types';

export interface LiveSessionData {
  sessionId: number;
  sessionCode: string;
  lectureName: string;
  lectureDate: string;
  instructor: string;
  totalStudents: number;
  status: 'CREATED' | 'ACTIVE' | 'PAUSED' | 'ENDED';
  startedAt?: string;
  lectures?: SessionLecture[]; // Multiple lectures in session
  activeLectureId?: number;
}

export interface StudentListItem {
  id: number;
  name: string;
  avatarUrl?: string;
  isSelected: boolean;
  status: 'active' | 'inactive' | 'help_needed';
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
}

export interface StudentScreen {
  studentId: number;
  studentName: string;
  imageUrl?: string;
  lastUpdated: string;
  isLoading: boolean;
  error?: string;
}