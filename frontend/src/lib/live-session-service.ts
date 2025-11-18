// Mock service for Live Session data
import { 
  LiveSessionData, 
  StudentListItem, 
  ProgressData, 
  GroupProgress, 
  LiveNotification,
  StudentScreen 
} from './live-session-types';

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
    await delay(300);
    return { ...mockLiveSession, sessionId };
  },

  async getStudentList(sessionId: number): Promise<StudentListItem[]> {
    await delay(200);
    return mockStudents;
  },

  async getProgressData(sessionId: number): Promise<ProgressData[]> {
    await delay(200);
    return mockProgressData;
  },

  async getGroupProgress(sessionId: number): Promise<GroupProgress[]> {
    await delay(200);
    return mockGroupProgress;
  },

  async getNotifications(sessionId: number): Promise<LiveNotification[]> {
    await delay(200);
    return mockNotifications.filter(n => !n.isResolved);
  },

  async getStudentScreen(studentId: number): Promise<StudentScreen> {
    await delay(500);
    
    // Mock: return a placeholder screen
    return {
      studentId,
      studentName: `학생${studentId}`,
      imageUrl: undefined, // In production, this would be the actual screen capture
      lastUpdated: new Date().toISOString(),
      isLoading: false,
    };
  },

  async startSession(sessionId: number): Promise<LiveSessionData> {
    await delay(300);
    mockLiveSession.status = 'ACTIVE';
    mockLiveSession.startedAt = new Date().toISOString();
    return mockLiveSession;
  },

  async pauseSession(sessionId: number): Promise<LiveSessionData> {
    await delay(300);
    mockLiveSession.status = 'PAUSED';
    return mockLiveSession;
  },

  async resumeSession(sessionId: number): Promise<LiveSessionData> {
    await delay(300);
    mockLiveSession.status = 'ACTIVE';
    return mockLiveSession;
  },

  async nextStep(sessionId: number): Promise<void> {
    await delay(300);
    // Mock: just delay
  },

  async endSession(sessionId: number): Promise<LiveSessionData> {
    await delay(300);
    mockLiveSession.status = 'ENDED';
    return mockLiveSession;
  },

  async switchLecture(sessionId: number, lectureId: number): Promise<LiveSessionData> {
    await delay(300);
    // In real implementation, this would update the active lecture
    // For now, just return the mock session data
    return mockLiveSession;
  },

  async resolveNotification(notificationId: number): Promise<void> {
    await delay(200);
    const notification = mockNotifications.find(n => n.id === notificationId);
    if (notification) {
      notification.isResolved = true;
    }
  },
};