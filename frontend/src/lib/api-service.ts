// API service for the MobileGPT Instructor Dashboard

import {
  Lecture,
  Session,
  Participant,
  StudentProgress,
  HelpRequest,
  LectureStatistics,
  SessionStatus
} from './types';
import apiClient from './api-client';

// Mock data
const mockLectures: Lecture[] = [
  {
    id: 1,
    title: '스마트폰 기초 - 첫걸음',
    description: '스마트폰의 기본 기능을 배우는 첫 번째 강의',
    isActive: true,
    studentCount: 15,
    sessionCount: 3,
  },
  {
    id: 2,
    title: '카카오톡 완전정복',
    description: '카카오톡 메시지, 사진, 영상통화 마스터하기',
    isActive: true,
    studentCount: 12,
    sessionCount: 2,
  },
  {
    id: 3,
    title: '인터넷 검색과 정보 찾기',
    description: '네이버, 구글로 필요한 정보 찾는 법',
    isActive: false,
    studentCount: 8,
    sessionCount: 1,
  },
];

let mockSessions: Session[] = [];
let sessionIdCounter = 1;

const mockParticipants: Map<number, Participant[]> = new Map();

const mockStudentProgress: StudentProgress[] = [
  {
    studentId: 1,
    studentName: '김영희',
    currentStep: 'Task 1 - Subtask 2',
    progress: 35,
    status: 'in_progress',
    helpCount: 1,
    lastActivity: new Date(Date.now() - 120000).toISOString(),
  },
  {
    studentId: 2,
    studentName: '이철수',
    currentStep: 'Task 2 - Subtask 1',
    progress: 60,
    status: 'in_progress',
    helpCount: 0,
    lastActivity: new Date(Date.now() - 30000).toISOString(),
  },
  {
    studentId: 3,
    studentName: '박순자',
    currentStep: 'Task 1 - Subtask 1',
    progress: 15,
    status: 'help_needed',
    helpCount: 2,
    lastActivity: new Date(Date.now() - 300000).toISOString(),
  },
  {
    studentId: 4,
    studentName: '최민수',
    currentStep: 'Task 3 - Subtask 3',
    progress: 100,
    status: 'completed',
    helpCount: 0,
    lastActivity: new Date(Date.now() - 600000).toISOString(),
  },
  {
    studentId: 5,
    studentName: '정미경',
    currentStep: 'Task 2 - Subtask 2',
    progress: 45,
    status: 'in_progress',
    helpCount: 1,
    lastActivity: new Date(Date.now() - 60000).toISOString(),
  },
];

let mockHelpRequests: HelpRequest[] = [
  {
    id: 1,
    studentId: 3,
    studentName: '박순자',
    studentEmail: 'park@test.com',
    type: 'MANUAL',
    currentStep: 'Task 1 - Subtask 1',
    requestedAt: new Date(Date.now() - 300000).toISOString(),
    isResolved: false,
    mgptAnalysis: {
      difficultyLevel: 7,
      stuckDuration: 280,
      errorPatterns: ['같은 버튼을 3번 반복 클릭', '뒤로가기를 자주 사용'],
      suggestedHint: '화면 하단의 초록색 버튼을 눌러보세요',
      confidence: 0.85,
    },
  },
  {
    id: 2,
    studentId: 1,
    studentName: '김영희',
    studentEmail: 'kim@test.com',
    type: 'AUTO',
    currentStep: 'Task 1 - Subtask 2',
    requestedAt: new Date(Date.now() - 180000).toISOString(),
    isResolved: false,
  },
];

const mockStatistics: LectureStatistics = {
  lectureId: 1,
  lectureName: '스마트폰 기초 - 첫걸음',
  totalStudents: 15,
  totalHelpRequests: 23,
  averageProgress: 67,
  completionRate: 80,
  difficultSteps: [
    {
      subtaskName: 'Task 1 - Subtask 1: 전원 켜기',
      helpRequestCount: 8,
      avgTimeSpent: 180,
      studentCount: 15,
    },
    {
      subtaskName: 'Task 2 - Subtask 3: 앱 설치하기',
      helpRequestCount: 6,
      avgTimeSpent: 240,
      studentCount: 12,
    },
    {
      subtaskName: 'Task 1 - Subtask 3: 화면 밝기 조절',
      helpRequestCount: 5,
      avgTimeSpent: 150,
      studentCount: 15,
    },
    {
      subtaskName: 'Task 3 - Subtask 1: 카메라 열기',
      helpRequestCount: 4,
      avgTimeSpent: 120,
      studentCount: 10,
    },
  ],
  lastUpdated: new Date().toISOString(),
};

// API functions
export const apiService = {
  // Lectures
  async getLectures(): Promise<Lecture[]> {
    try {
      const response = await apiClient.get('/lectures/');
      return response.data.map((lecture: any) => ({
        id: lecture.id,
        title: lecture.title,
        description: lecture.description || '',
        isActive: lecture.is_active !== undefined ? lecture.is_active : true,
        studentCount: lecture.student_count || 0,
        sessionCount: lecture.session_count || 0,
      }));
    } catch (error) {
      console.error('Failed to fetch lectures:', error);
      throw error;
    }
  },

  // Sessions
  async createSession(lectureIds: number[], title: string): Promise<Session> {
    try {
      // 백엔드는 하나의 강의로 세션을 생성하므로, 첫 번째 강의로 세션 생성
      const lectureId = lectureIds[0];
      if (!lectureId) {
        throw new Error('At least one lecture is required');
      }

      const response = await apiClient.post(`/lectures/${lectureId}/sessions/create/`, {
        title
      });

      const session = response.data;
      return {
        id: session.id,
        title: session.title,
        code: session.session_code,
        status: this.mapSessionStatus(session.status),
        createdAt: session.created_at,
        lectures: lectureIds.map((id, index) => ({
          id: index + 1,
          lectureId: id,
          lectureName: session.lecture?.title || `Lecture ${id}`,
          order: index + 1,
          isActive: index === 0,
          completedAt: undefined,
        })),
        activeLectureId: lectureId,
      };
    } catch (error) {
      console.error('Failed to create session:', error);
      throw error;
    }
  },

  // Helper: Map backend status to frontend status
  mapSessionStatus(backendStatus: string): SessionStatus {
    const statusMap: Record<string, SessionStatus> = {
      'WAITING': 'CREATED',
      'ACTIVE': 'ACTIVE',
      'PAUSED': 'PAUSED',
      'ENDED': 'ENDED',
      'CREATED': 'CREATED',
    };
    return statusMap[backendStatus] || 'CREATED';
  },

  async switchLecture(sessionId: number, lectureId: number): Promise<Session> {
    await delay(300);
    const session = mockSessions.find(s => s.id === sessionId);
    if (!session) throw new Error('Session not found');
    
    // Mark current lecture as completed
    const currentLecture = session.lectures.find(l => l.isActive);
    if (currentLecture) {
      currentLecture.isActive = false;
      currentLecture.completedAt = new Date().toISOString();
    }
    
    // Activate new lecture
    const newLecture = session.lectures.find(l => l.lectureId === lectureId);
    if (!newLecture) throw new Error('Lecture not found in session');
    
    newLecture.isActive = true;
    session.activeLectureId = lectureId;
    session.currentStep = undefined; // Reset step for new lecture
    
    return session;
  },

  async getSessionById(sessionId: number): Promise<Session | null> {
    try {
      const response = await apiClient.get(`/sessions/${sessionId}/current/`);
      const session = response.data;
      return {
        id: session.id,
        title: session.title,
        code: session.session_code || session.code,
        status: this.mapSessionStatus(session.status),
        createdAt: session.created_at,
        startedAt: session.started_at,
        endedAt: session.ended_at,
        currentStep: session.current_subtask?.title || session.current_step,
        lectures: session.lectures || [],
        activeLectureId: session.lecture?.id || session.active_lecture_id,
      };
    } catch (error: any) {
      if (error.response?.status === 404) {
        return null;
      }
      console.error('Failed to fetch session:', error);
      throw error;
    }
  },

  async startSession(sessionId: number, firstSubtaskId?: number): Promise<Session> {
    try {
      const response = await apiClient.post(`/sessions/${sessionId}/start/`, {
        first_subtask_id: firstSubtaskId || 1,
        message: '수업을 시작합니다'
      });
      const session = response.data;
      return {
        id: session.id,
        title: session.title,
        code: session.session_code || session.code,
        status: this.mapSessionStatus(session.status),
        createdAt: session.created_at,
        startedAt: session.started_at,
        currentStep: session.current_subtask?.title || session.current_step,
        lectures: session.lectures || [],
        activeLectureId: session.lecture?.id || session.active_lecture_id,
      };
    } catch (error) {
      console.error('Failed to start session:', error);
      throw error;
    }
  },

  async pauseSession(sessionId: number): Promise<Session> {
    try {
      const response = await apiClient.post(`/sessions/${sessionId}/pause/`, {});
      const session = response.data;
      return {
        id: session.id,
        title: session.title,
        code: session.session_code || session.code,
        status: this.mapSessionStatus(session.status),
        createdAt: session.created_at,
        startedAt: session.started_at,
        currentStep: session.current_subtask?.title || session.current_step,
        lectures: session.lectures || [],
        activeLectureId: session.lecture?.id || session.active_lecture_id,
      };
    } catch (error) {
      console.error('Failed to pause session:', error);
      throw error;
    }
  },

  async resumeSession(sessionId: number): Promise<Session> {
    try {
      const response = await apiClient.post(`/sessions/${sessionId}/resume/`, {});
      const session = response.data;
      return {
        id: session.id,
        title: session.title,
        code: session.session_code || session.code,
        status: this.mapSessionStatus(session.status),
        createdAt: session.created_at,
        startedAt: session.started_at,
        currentStep: session.current_subtask?.title || session.current_step,
        lectures: session.lectures || [],
        activeLectureId: session.lecture?.id || session.active_lecture_id,
      };
    } catch (error) {
      console.error('Failed to resume session:', error);
      throw error;
    }
  },

  async nextStep(sessionId: number, nextSubtaskId?: number): Promise<Session> {
    try {
      const response = await apiClient.post(`/sessions/${sessionId}/next-step/`, {
        next_subtask_id: nextSubtaskId,
        message: '다음 단계로 진행합니다'
      });
      const session = response.data;
      return {
        id: session.id,
        title: session.title,
        code: session.session_code || session.code,
        status: this.mapSessionStatus(session.status),
        createdAt: session.created_at,
        startedAt: session.started_at,
        currentStep: session.current_subtask?.title || session.current_step,
        lectures: session.lectures || [],
        activeLectureId: session.lecture?.id || session.active_lecture_id,
      };
    } catch (error) {
      console.error('Failed to move to next step:', error);
      throw error;
    }
  },

  async endSession(sessionId: number): Promise<Session> {
    try {
      const response = await apiClient.post(`/sessions/${sessionId}/end/`, {});
      const session = response.data;
      return {
        id: session.id,
        title: session.title,
        code: session.session_code || session.code,
        status: this.mapSessionStatus(session.status),
        createdAt: session.created_at,
        startedAt: session.started_at,
        endedAt: session.ended_at,
        currentStep: session.current_subtask?.title || session.current_step,
        lectures: session.lectures || [],
        activeLectureId: session.lecture?.id || session.active_lecture_id,
      };
    } catch (error) {
      console.error('Failed to end session:', error);
      throw error;
    }
  },

  async getSessionParticipants(sessionId: number): Promise<Participant[]> {
    try {
      const response = await apiClient.get(`/sessions/${sessionId}/participants/`);
      return response.data.map((participant: any) => ({
        id: participant.user?.id || participant.id,
        name: participant.user?.name || participant.name,
        email: participant.user?.email || participant.email,
        joinedAt: participant.joined_at,
        isActive: participant.is_active !== undefined ? participant.is_active : true,
      }));
    } catch (error) {
      console.error('Failed to fetch participants:', error);
      return [];
    }
  },

  // Monitoring
  async getStudentProgress(lectureId: number): Promise<StudentProgress[]> {
    try {
      const response = await apiClient.get(`/dashboard/lectures/${lectureId}/students/`);
      const students = response.data.students || response.data;
      return students.map((student: any) => ({
        studentId: student.user_id || student.id,
        studentName: student.name,
        currentStep: student.current_subtask?.title || student.current_step || '',
        progress: student.progress_rate ? student.progress_rate * 100 : 0,
        status: this.mapStudentStatus(student.progress_rate, student.help_count),
        helpCount: student.help_count || 0,
        lastActivity: student.last_activity || new Date().toISOString(),
      }));
    } catch (error) {
      console.error('Failed to fetch student progress:', error);
      return [];
    }
  },

  // Helper: Map student progress to status
  mapStudentStatus(progressRate: number, helpCount: number): 'in_progress' | 'completed' | 'help_needed' {
    if (helpCount > 0) return 'help_needed';
    if (progressRate >= 1) return 'completed';
    return 'in_progress';
  },

  // Help Requests
  async getPendingHelpRequests(): Promise<HelpRequest[]> {
    try {
      const response = await apiClient.get('/dashboard/help-requests/pending/');
      const requests = response.data.pending_requests || response.data;
      return requests.map((req: any) => ({
        id: req.id,
        studentId: req.user?.id || req.student_id,
        studentName: req.user?.name || req.student_name,
        studentEmail: req.user?.email || req.student_email,
        taskName: req.subtask?.title || req.task_name || '',
        issue: req.message || req.issue || '',
        requestedAt: req.created_at || req.requested_at,
        isResolved: req.is_resolved || req.status === 'RESOLVED',
        priority: req.priority || 'medium',
      }));
    } catch (error) {
      console.error('Failed to fetch help requests:', error);
      return [];
    }
  },

  async getPendingHelpRequestCount(): Promise<number> {
    try {
      const requests = await this.getPendingHelpRequests();
      return requests.length;
    } catch (error) {
      console.error('Failed to fetch help request count:', error);
      return 0;
    }
  },

  async resolveHelpRequest(requestId: number): Promise<void> {
    try {
      await apiClient.post(`/help/request/${requestId}/resolve/`, {});
    } catch (error) {
      console.error('Failed to resolve help request:', error);
      throw error;
    }
  },

  // Statistics
  async getLectureStatistics(lectureId: number): Promise<LectureStatistics> {
    try {
      const response = await apiClient.get(`/dashboard/statistics/lecture/${lectureId}/`);
      const stats = response.data;
      return {
        lectureId,
        lectureName: stats.lecture_name || stats.lectureName || 'Unknown',
        totalStudents: stats.total_students || 0,
        activeStudents: stats.active_students || 0,
        completionRate: stats.completion_rate || 0,
        averageProgress: stats.average_progress || 0,
        helpRequestsCount: stats.total_help_requests || stats.help_requests_count || 0,
        sessionCount: stats.session_count || 0,
        commonIssues: stats.common_difficulties || [],
        lastUpdated: stats.last_updated || new Date().toISOString(),
      };
    } catch (error) {
      console.error('Failed to fetch lecture statistics:', error);
      // Return fallback data
      return {
        lectureId,
        lectureName: 'Unknown',
        totalStudents: 0,
        activeStudents: 0,
        completionRate: 0,
        averageProgress: 0,
        helpRequestsCount: 0,
        sessionCount: 0,
        commonIssues: [],
        lastUpdated: new Date().toISOString(),
      };
    }
  },
};

// Helper functions
function delay(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms));
}

function generateSessionCode(): string {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
  let code = '';
  for (let i = 0; i < 6; i++) {
    code += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return code;
}

// Simulate adding participants to sessions
setTimeout(() => {
  mockSessions.forEach(session => {
    if (session.status === 'CREATED' || session.status === 'ACTIVE') {
      const participants = mockParticipants.get(session.id) || [];
      if (participants.length === 0) {
        // Add some mock participants
        mockParticipants.set(session.id, [
          {
            id: 1,
            name: '김영희',
            email: 'kim@test.com',
            joinedAt: new Date().toISOString(),
            isActive: true,
          },
          {
            id: 2,
            name: '이철수',
            email: 'lee@test.com',
            joinedAt: new Date().toISOString(),
            isActive: true,
          },
        ]);
      }
    }
  });
}, 3000);