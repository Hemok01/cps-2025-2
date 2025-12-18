// API service for the MobileGPT Instructor Dashboard

import {
  Lecture,
  Session,
  Participant,
  StudentProgress,
  HelpRequest,
  LectureStatistics,
  SessionStatus,
  SessionSummary
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

      // 에러 응답 체크
      if (response.data.error) {
        throw new Error(response.data.error.message || 'Failed to fetch lectures');
      }

      // DRF 페이지네이션 응답 처리 (results 필드에 실제 데이터가 있음)
      const lecturesData = response.data.results || response.data;

      // 배열이 아니면 빈 배열 반환
      if (!Array.isArray(lecturesData)) {
        console.warn('Unexpected response format:', response.data);
        return [];
      }

      return lecturesData.map((lecture: any) => ({
        id: lecture.id,
        title: lecture.title,
        description: lecture.description || '',
        isActive: lecture.is_active !== undefined ? lecture.is_active : true,
        studentCount: lecture.student_count || lecture.enrolled_count || 0,
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
      'CREATED': 'CREATED',
      'ACTIVE': 'ACTIVE',
      'IN_PROGRESS': 'ACTIVE',
      'PAUSED': 'PAUSED',
      'ENDED': 'ENDED',
      'REVIEW_MODE': 'ENDED',
      'COMPLETED': 'ENDED',
    };
    return statusMap[backendStatus] || 'CREATED';
  },

  async switchLecture(sessionId: number, lectureId: number): Promise<Session> {
    try {
      const response = await apiClient.post(`/sessions/${sessionId}/switch-lecture/`, {
        lecture_id: lectureId
      });

      const result = response.data;

      // 세션 정보 다시 조회하여 최신 상태 반환
      const updatedSession = await this.getSessionById(sessionId);
      if (!updatedSession) {
        throw new Error('Session not found after lecture switch');
      }

      return updatedSession;
    } catch (error) {
      console.error('Failed to switch lecture:', error);
      throw error;
    }
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

  async getInstructorActiveSessions(): Promise<Session[]> {
    try {
      const response = await apiClient.get('/sessions/instructor-active/');
      const sessions = response.data.active_sessions || [];

      return sessions.map((session: any) => ({
        id: session.id,
        title: session.title,
        code: session.session_code,
        status: this.mapSessionStatus(session.status),
        createdAt: session.created_at,
        startedAt: session.started_at,
        currentStep: session.current_subtask?.title,
        lectures: session.lecture ? [{
          id: 1,
          lectureId: session.lecture.id,
          lectureName: session.lecture.title,
          order: 1,
          isActive: true,
          completedAt: undefined,
        }] : [],
        activeLectureId: session.lecture?.id,
        participantCount: session.participant_count || 0,
      }));
    } catch (error) {
      console.error('Failed to fetch instructor active sessions:', error);
      return [];
    }
  },

  async startSession(sessionId: number, firstSubtaskId?: number): Promise<Session> {
    try {
      // first_subtask_id가 없으면 백엔드에서 자동으로 첫 번째 subtask를 찾아 사용
      const requestData: Record<string, any> = {
        message: '수업을 시작합니다'
      };
      if (firstSubtaskId) {
        requestData.first_subtask_id = firstSubtaskId;
      }

      const response = await apiClient.post(`/sessions/${sessionId}/start/`, requestData);
      const session = response.data;
      return {
        id: session.session_id || session.id,
        title: session.title || '',
        code: session.session_code || session.code || '',
        status: this.mapSessionStatus(session.status),
        createdAt: session.created_at || '',
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

      // 다양한 응답 형식 처리:
      // 1. { participants: [...] } - 커스텀 API
      // 2. { results: [...] } - DRF 페이지네이션
      // 3. [...] - 직접 배열
      const participantsData = response.data.participants || response.data.results || response.data;

      if (!Array.isArray(participantsData)) {
        console.warn('Unexpected response format for participants:', response.data);
        return [];
      }

      return participantsData.map((participant: any) => ({
        id: participant.user?.id || participant.id,
        name: participant.user?.name || participant.name || participant.display_name,
        email: participant.user?.email || participant.email || '',
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
      const students = response.data.results || response.data.students || response.data;

      if (!Array.isArray(students)) {
        console.warn('Unexpected response format for student progress:', response.data);
        return [];
      }

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
      const requests = response.data.results || response.data.pending_requests || response.data;

      if (!Array.isArray(requests)) {
        console.warn('Unexpected response format for help requests:', response.data);
        return [];
      }

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

  // Broadcast message to all participants
  async broadcastMessage(sessionId: number, message: string): Promise<{ success: boolean; broadcastTo: number }> {
    try {
      const response = await apiClient.post(`/sessions/${sessionId}/broadcast/`, {
        message
      });
      return {
        success: response.data.success,
        broadcastTo: response.data.broadcast_to || 0,
      };
    } catch (error) {
      console.error('Failed to broadcast message:', error);
      throw error;
    }
  },

  // Statistics
  async getLectureStatistics(lectureId: number): Promise<LectureStatistics> {
    try {
      const response = await apiClient.get(`/dashboard/statistics/lecture/${lectureId}/`);
      const stats = response.data;

      // difficult_steps 변환
      const difficultSteps = (stats.difficult_steps || []).map((step: any) => ({
        subtaskName: step.subtask_name,
        helpRequestCount: step.help_request_count,
        avgTimeSpent: step.avg_time_spent,
        studentCount: step.student_count,
      }));

      return {
        lectureId: stats.lecture_id || lectureId,
        lectureName: stats.lecture_name || 'Unknown',
        totalStudents: stats.total_students || 0,
        totalHelpRequests: stats.total_help_requests || 0,
        averageProgress: stats.average_progress || 0,
        completionRate: stats.completion_rate || 0,
        difficultSteps,
        lastUpdated: stats.last_updated || new Date().toISOString(),
      };
    } catch (error) {
      console.error('Failed to fetch lecture statistics:', error);
      throw error;
    }
  },

  // 단계별 분석 API
  async getStepAnalysis(lectureId: number): Promise<import('./types').StepAnalysisData> {
    try {
      const response = await apiClient.get(`/dashboard/statistics/lecture/${lectureId}/step-analysis/`);
      const data = response.data;

      return {
        lectureId: data.lecture_id,
        lectureName: data.lecture_name,
        totalSubtasks: data.total_subtasks,
        stepAnalysis: data.step_analysis.map((item: any) => ({
          subtaskId: item.subtask_id,
          subtaskName: item.subtask_name,
          taskName: item.task_name,
          orderIndex: item.order_index,
          avgTimeSpent: item.avg_time_spent,
          delayRate: item.delay_rate,
          helpRequestCount: item.help_request_count,
          studentCount: item.student_count,
          completionRate: item.completion_rate,
          bottleneckScore: item.bottleneck_score,
        })),
        summary: {
          mostDelayedStep: data.summary.most_delayed_step,
          mostHelpRequestedStep: data.summary.most_help_requested_step,
          avgOverallDelayRate: data.summary.avg_overall_delay_rate,
        },
        lastUpdated: data.last_updated,
      };
    } catch (error) {
      console.error('Failed to fetch step analysis:', error);
      throw error;
    }
  },

  // 세션 간 추이 비교 API
  async getSessionTrends(lectureId: number): Promise<import('./types').SessionComparisonData> {
    try {
      const response = await apiClient.get(`/dashboard/statistics/lecture/${lectureId}/session-trends/`);
      const data = response.data;

      return {
        lectureId: data.lecture_id,
        lectureName: data.lecture_name,
        sessions: data.sessions.map((s: any) => ({
          sessionId: s.session_id,
          sessionTitle: s.session_title,
          sessionDate: s.session_date,
          startedAt: s.started_at,
          endedAt: s.ended_at,
          participantCount: s.participant_count,
          completionRate: s.completion_rate,
          avgCompletionTime: s.avg_completion_time,
          totalHelpRequests: s.total_help_requests,
          helpRequestRate: s.help_request_rate,
        })),
        trendSummary: {
          completionRateTrend: data.trend_summary.completion_rate_trend,
          helpRequestTrend: data.trend_summary.help_request_trend,
          avgCompletionTimeTrend: data.trend_summary.avg_completion_time_trend,
        },
        lastUpdated: data.last_updated,
      };
    } catch (error) {
      console.error('Failed to fetch session trends:', error);
      throw error;
    }
  },

  // Session Summary (세션 요약)
  async getSessionSummary(sessionId: number): Promise<SessionSummary> {
    try {
      const response = await apiClient.get(`/sessions/${sessionId}/summary/`);
      return response.data;
    } catch (error) {
      console.error('Failed to fetch session summary:', error);
      throw error;
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