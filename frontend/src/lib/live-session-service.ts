// Live Session service
import {
  LiveSessionData,
  StudentListItem,
  ProgressData,
  GroupProgress,
  LiveNotification,
  StudentScreen
} from './live-session-types';
import apiClient from './api-client';

// Mock data
const mockLiveSession: LiveSessionData = {
  sessionId: 1,
  sessionCode: 'ABC123',
  lectureName: '김OO',
  lectureDate: '2025-11-14',
  instructor: '강OO',
  totalStudents: 68,
  status: 'CREATED',
};

const mockStudents: StudentListItem[] = Array.from({ length: 68 }, (_, i) => ({
  id: i + 1,
  name: `학생${i + 1}`,
  avatarUrl: undefined,
  isSelected: i === 0,
  status: i % 10 === 0 ? 'help_needed' : i % 3 === 0 ? 'inactive' : 'active',
}));

const mockProgressData: ProgressData[] = [
  {
    label: '전체 진도',
    current: 51,
    total: 68,
    percentage: 75,
    color: '#1976D2',
  },
  {
    label: '50대 진도',
    current: 10,
    total: 20,
    percentage: 50,
    color: '#00BCD4',
  },
  {
    label: '60대 진도',
    current: 28,
    total: 30,
    percentage: 93,
    color: '#E91E63',
  },
  {
    label: '70대 진도',
    current: 13,
    total: 18,
    percentage: 72,
    color: '#4CAF50',
  },
];

const mockGroupProgress: GroupProgress[] = [
  {
    groupId: 1,
    groupName: '그룹 1',
    currentTask: '앱 설치에서 막히는 부분',
    participants: [
      { id: 1, name: '김명희' },
      { id: 2, name: '이철수' },
      { id: 3, name: '박순자' },
    ],
  },
  {
    groupId: 2,
    groupName: '그룹 2',
    currentTask: '회원가입에서 막히는 부분',
    participants: [
      { id: 4, name: '최민수' },
      { id: 5, name: '정미경' },
    ],
  },
];

let mockNotifications: LiveNotification[] = [
  {
    id: 1,
    type: 'system',
    title: '50대 진도가 늦습니다',
    message: '50대 그룹의 평균 진도가 다른 그룹보다 낮습니다',
    timestamp: new Date(Date.now() - 30000).toISOString(),
    isResolved: false,
  },
  {
    id: 2,
    type: 'help_request',
    title: '화면이 안 넘어가요',
    message: '다음 단계로 진행할 수 없습니다',
    timestamp: new Date(Date.now() - 60000).toISOString(),
    studentId: 3,
    studentName: '박순자',
    isResolved: false,
  },
];

function delay(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms));
}

export const liveSessionService = {
  async getSessionData(sessionId: number): Promise<LiveSessionData> {
    try {
      const response = await apiClient.get(`/sessions/${sessionId}/current/`);
      const session = response.data;
      return {
        sessionId: session.id,
        sessionCode: session.session_code || session.code,
        lectureName: session.lecture?.title || '강의명',
        lectureDate: session.created_at?.split('T')[0] || new Date().toISOString().split('T')[0],
        instructor: session.instructor?.name || session.lecture?.instructor?.name || '강사',
        totalStudents: session.total_participants || session.participant_count || 0,
        status: this.mapSessionStatus(session.status),
        startedAt: session.started_at,
        currentStep: session.current_subtask?.title || session.current_step,
      };
    } catch (error) {
      console.error('Failed to fetch session data:', error);
      throw error;
    }
  },

  // Helper: Map backend status
  mapSessionStatus(backendStatus: string): 'CREATED' | 'ACTIVE' | 'PAUSED' | 'ENDED' {
    const statusMap: Record<string, 'CREATED' | 'ACTIVE' | 'PAUSED' | 'ENDED'> = {
      'WAITING': 'CREATED',
      'ACTIVE': 'ACTIVE',
      'PAUSED': 'PAUSED',
      'ENDED': 'ENDED',
      'CREATED': 'CREATED',
    };
    return statusMap[backendStatus] || 'CREATED';
  },

  async getStudentList(sessionId: number): Promise<StudentListItem[]> {
    try {
      const response = await apiClient.get(`/sessions/${sessionId}/participants/`);
      const participants = response.data;
      return participants.map((participant: any, index: number) => ({
        id: participant.user?.id || participant.id,
        name: participant.user?.name || participant.name,
        avatarUrl: participant.user?.avatar_url || participant.avatar_url,
        isSelected: index === 0, // First student is selected by default
        status: participant.is_active ? 'active' : 'inactive',
      }));
    } catch (error) {
      console.error('Failed to fetch student list:', error);
      return [];
    }
  },

  async getProgressData(sessionId: number): Promise<ProgressData[]> {
    // 목 데이터 유지 (백엔드에 그룹별 통계 API가 없을 수 있음)
    await delay(200);
    return mockProgressData;
  },

  async getGroupProgress(sessionId: number): Promise<GroupProgress[]> {
    // 목 데이터 유지 (백엔드에 그룹 기능이 없을 수 있음)
    await delay(200);
    return mockGroupProgress;
  },

  async getNotifications(sessionId: number): Promise<LiveNotification[]> {
    try {
      const response = await apiClient.get('/dashboard/help-requests/pending/');
      const requests = response.data.pending_requests || response.data;
      return requests.map((req: any) => ({
        id: req.id,
        type: 'help_request' as const,
        title: req.message?.substring(0, 30) || '도움 요청',
        message: req.message || '',
        timestamp: req.created_at || req.requested_at,
        studentId: req.user?.id || req.student_id,
        studentName: req.user?.name || req.student_name,
        isResolved: req.is_resolved || req.status === 'RESOLVED',
      }));
    } catch (error) {
      console.error('Failed to fetch notifications:', error);
      return [];
    }
  },

  async getStudentScreen(studentId: number): Promise<StudentScreen> {
    // 목 데이터 유지 (백엔드 API 없음 - 학생 화면 조회 기능)
    await delay(500);

    return {
      studentId,
      studentName: `학생${studentId}`,
      imageUrl: undefined,
      lastUpdated: new Date().toISOString(),
      isLoading: false,
    };
  },

  async startSession(sessionId: number): Promise<LiveSessionData> {
    try {
      await apiClient.post(`/sessions/${sessionId}/start/`, {
        first_subtask_id: 1,
        message: '수업을 시작합니다'
      });
      return await this.getSessionData(sessionId);
    } catch (error) {
      console.error('Failed to start session:', error);
      throw error;
    }
  },

  async pauseSession(sessionId: number): Promise<LiveSessionData> {
    try {
      await apiClient.post(`/sessions/${sessionId}/pause/`, {});
      return await this.getSessionData(sessionId);
    } catch (error) {
      console.error('Failed to pause session:', error);
      throw error;
    }
  },

  async resumeSession(sessionId: number): Promise<LiveSessionData> {
    try {
      await apiClient.post(`/sessions/${sessionId}/resume/`, {});
      return await this.getSessionData(sessionId);
    } catch (error) {
      console.error('Failed to resume session:', error);
      throw error;
    }
  },

  async nextStep(sessionId: number, nextSubtaskId?: number): Promise<void> {
    try {
      await apiClient.post(`/sessions/${sessionId}/next-step/`, {
        next_subtask_id: nextSubtaskId,
        message: '다음 단계로 진행합니다'
      });
    } catch (error) {
      console.error('Failed to move to next step:', error);
      throw error;
    }
  },

  async endSession(sessionId: number): Promise<LiveSessionData> {
    try {
      await apiClient.post(`/sessions/${sessionId}/end/`, {});
      return await this.getSessionData(sessionId);
    } catch (error) {
      console.error('Failed to end session:', error);
      throw error;
    }
  },

  async switchLecture(sessionId: number, lectureId: number): Promise<LiveSessionData> {
    // 목 데이터 유지 (백엔드 API 없음 - 세션 강의 전환)
    await delay(300);
    return await this.getSessionData(sessionId);
  },

  async resolveNotification(notificationId: number): Promise<void> {
    try {
      await apiClient.post(`/help/request/${notificationId}/resolve/`, {});
    } catch (error) {
      console.error('Failed to resolve notification:', error);
      throw error;
    }
  },
};