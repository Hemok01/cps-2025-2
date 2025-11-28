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

  async getStudentList(sessionId: number): Promise<StudentListItem[]> {
    try {
      const response = await apiClient.get(`/sessions/${sessionId}/participants/`);
      // API returns { session_id, participants, total_count }
      const participants = response.data.participants || response.data;

      return participants.map((participant: any, index: number) => {
        // Determine status: help_needed > active > inactive
        let status: 'active' | 'inactive' | 'help_needed' = 'inactive';

        if (participant.has_pending_help_request) {
          status = 'help_needed';
        } else if (participant.is_active || participant.status === 'ACTIVE' || participant.status === 'WAITING') {
          status = 'active';
        }

        return {
          id: participant.user?.id || participant.id,
          name: participant.name || participant.user?.name || participant.display_name || '익명',
          avatarUrl: participant.user?.avatar_url || participant.avatar_url,
          isSelected: index === 0, // First student is selected by default
          status,
        };
      });
    } catch (error) {
      console.error('Failed to fetch student list:', error);
      return [];
    }
  },

  async getProgressData(sessionId: number): Promise<ProgressData[]> {
    try {
      const response = await apiClient.get(`/dashboard/sessions/${sessionId}/progress-stats/`);
      const data = response.data;

      // progress_data를 ProgressData 형식으로 변환
      if (data.progress_data && Array.isArray(data.progress_data)) {
        return data.progress_data.map((item: any) => ({
          userId: item.user_id || item.device_id,
          username: item.username,
          currentSubtask: item.current_subtask?.title || null,
          progressPercentage: item.progress_percentage,
          completedSubtasks: item.current_subtask?.order_index || 0,
          status: item.status,
        }));
      }

      return [];
    } catch (error) {
      console.error('Failed to fetch progress data:', error);
      // API 실패 시 목 데이터 반환
      return mockProgressData;
    }
  },

  async getGroupProgress(sessionId: number): Promise<GroupProgress[]> {
    try {
      const response = await apiClient.get(`/dashboard/sessions/${sessionId}/progress-stats/`);
      const data = response.data;

      // groups를 GroupProgress 형식으로 변환
      if (data.groups && Array.isArray(data.groups)) {
        return data.groups.map((group: any) => ({
          groupName: group.name,
          studentCount: group.count,
          averageProgress: group.percentage,
        }));
      }

      return [];
    } catch (error) {
      console.error('Failed to fetch group progress:', error);
      // API 실패 시 목 데이터 반환
      return mockGroupProgress;
    }
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

  async getStudentScreen(studentId: number, sessionId: number): Promise<StudentScreen> {
    // Validate parameters
    if (!sessionId || isNaN(sessionId) || !studentId || isNaN(studentId)) {
      console.warn('getStudentScreen called with invalid params:', { studentId, sessionId });
      return {
        studentId: studentId || 0,
        studentName: '',
        imageUrl: undefined,
        lastUpdated: new Date().toISOString(),
        isLoading: false,
        error: '잘못된 요청입니다',
      };
    }

    try {
      const response = await apiClient.get(`/sessions/${sessionId}/screenshots/${studentId}/`);
      const data = response.data;

      return {
        studentId,
        studentName: data.participant_name || `학생${studentId}`,
        imageUrl: data.image_url,
        lastUpdated: data.captured_at || new Date().toISOString(),
        isLoading: false,
      };
    } catch (error: any) {
      // 401은 인터셉터에서 처리하도록 re-throw (토큰 갱신 또는 로그인 리다이렉트)
      if (error.response?.status === 401) {
        throw error;
      }
      // 404는 스크린샷이 아직 없는 경우 (정상)
      if (error.response?.status === 404) {
        return {
          studentId,
          studentName: `학생${studentId}`,
          imageUrl: undefined,
          lastUpdated: new Date().toISOString(),
          isLoading: false,
          error: '스크린샷이 아직 없습니다',
        };
      }
      console.error('Failed to fetch student screen:', error);
      return {
        studentId,
        studentName: `학생${studentId}`,
        imageUrl: undefined,
        lastUpdated: new Date().toISOString(),
        isLoading: false,
        error: '화면을 불러올 수 없습니다',
      };
    }
  },

  async getStudentScreenByDeviceId(sessionId: number, deviceId: string): Promise<StudentScreen> {
    try {
      const response = await apiClient.get(`/sessions/${sessionId}/screenshots/by-device/${deviceId}/`);
      const data = response.data;

      return {
        studentId: data.participant?.id || 0,
        studentName: data.participant_name || `익명-${deviceId.substring(0, 8)}`,
        imageUrl: data.image_url,
        lastUpdated: data.captured_at || new Date().toISOString(),
        isLoading: false,
      };
    } catch (error: any) {
      // 401은 인터셉터에서 처리하도록 re-throw
      if (error.response?.status === 401) {
        throw error;
      }
      console.error('Failed to fetch student screen by device ID:', error);
      return {
        studentId: 0,
        studentName: `익명-${deviceId.substring(0, 8)}`,
        imageUrl: undefined,
        lastUpdated: new Date().toISOString(),
        isLoading: false,
        error: '화면을 불러올 수 없습니다',
      };
    }
  },

  async getAllStudentScreenshots(sessionId: number): Promise<StudentScreen[]> {
    try {
      const response = await apiClient.get(`/sessions/${sessionId}/screenshots/`);
      const screenshots = response.data;

      return screenshots.map((screenshot: any) => ({
        studentId: screenshot.participant_id || 0,
        studentName: screenshot.participant_name || `익명`,
        imageUrl: screenshot.image_url,
        lastUpdated: screenshot.captured_at || new Date().toISOString(),
        isLoading: false,
        deviceId: screenshot.device_id,
      }));
    } catch (error: any) {
      // 401은 인터셉터에서 처리하도록 re-throw
      if (error.response?.status === 401) {
        throw error;
      }
      console.error('Failed to fetch all student screenshots:', error);
      return [];
    }
  },

  async startSession(sessionId: number, firstSubtaskId?: number): Promise<LiveSessionData> {
    try {
      // first_subtask_id가 없으면 백엔드에서 자동으로 첫 번째 subtask를 찾아 사용
      const requestData: Record<string, any> = {
        message: '수업을 시작합니다'
      };
      if (firstSubtaskId) {
        requestData.first_subtask_id = firstSubtaskId;
      }
      await apiClient.post(`/sessions/${sessionId}/start/`, requestData);
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
    try {
      await apiClient.post(`/sessions/${sessionId}/switch-lecture/`, {
        lecture_id: lectureId
      });
      // 강의 전환 후 세션 데이터 다시 조회
      return await this.getSessionData(sessionId);
    } catch (error) {
      console.error('Failed to switch lecture:', error);
      throw error;
    }
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