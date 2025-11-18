// Mock API service for the MobileGPT Instructor Dashboard
// In production, replace these with actual API calls

import { 
  Lecture, 
  Session, 
  Participant, 
  StudentProgress, 
  HelpRequest, 
  LectureStatistics,
  SessionStatus 
} from './types';

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
    await delay(300);
    return mockLectures;
  },

  // Sessions
  async createSession(lectureIds: number[], title: string): Promise<Session> {
    await delay(500);
    
    const lectures = lectureIds.map((lectureId, index) => {
      const lecture = mockLectures.find(l => l.id === lectureId);
      if (!lecture) throw new Error(`Lecture ${lectureId} not found`);
      
      return {
        id: index + 1,
        lectureId,
        lectureName: lecture.title,
        order: index + 1,
        isActive: index === 0, // First lecture is active
        completedAt: undefined,
      };
    });
    
    const code = generateSessionCode();
    const newSession: Session = {
      id: sessionIdCounter++,
      title,
      code,
      status: 'CREATED',
      createdAt: new Date().toISOString(),
      lectures,
      activeLectureId: lectures[0]?.lectureId,
    };
    
    mockSessions.push(newSession);
    mockParticipants.set(newSession.id, []);
    
    return newSession;
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
    await delay(200);
    return mockSessions.find(s => s.id === sessionId) || null;
  },

  async startSession(sessionId: number): Promise<Session> {
    await delay(300);
    const session = mockSessions.find(s => s.id === sessionId);
    if (!session) throw new Error('Session not found');
    
    session.status = 'ACTIVE';
    session.startedAt = new Date().toISOString();
    return session;
  },

  async pauseSession(sessionId: number): Promise<Session> {
    await delay(300);
    const session = mockSessions.find(s => s.id === sessionId);
    if (!session) throw new Error('Session not found');
    
    session.status = 'PAUSED';
    return session;
  },

  async resumeSession(sessionId: number): Promise<Session> {
    await delay(300);
    const session = mockSessions.find(s => s.id === sessionId);
    if (!session) throw new Error('Session not found');
    
    session.status = 'ACTIVE';
    return session;
  },

  async nextStep(sessionId: number): Promise<Session> {
    await delay(300);
    const session = mockSessions.find(s => s.id === sessionId);
    if (!session) throw new Error('Session not found');
    
    // Mock: increment step
    const currentStepMatch = session.currentStep?.match(/Subtask (\d+)/);
    const currentNum = currentStepMatch ? parseInt(currentStepMatch[1]) : 0;
    session.currentStep = `Task 1 - Subtask ${currentNum + 1}`;
    
    return session;
  },

  async endSession(sessionId: number): Promise<Session> {
    await delay(300);
    const session = mockSessions.find(s => s.id === sessionId);
    if (!session) throw new Error('Session not found');
    
    session.status = 'ENDED';
    session.endedAt = new Date().toISOString();
    return session;
  },

  async getSessionParticipants(sessionId: number): Promise<Participant[]> {
    await delay(200);
    return mockParticipants.get(sessionId) || [];
  },

  // Monitoring
  async getStudentProgress(lectureId: number): Promise<StudentProgress[]> {
    await delay(400);
    return mockStudentProgress;
  },

  // Help Requests
  async getPendingHelpRequests(): Promise<HelpRequest[]> {
    await delay(300);
    return mockHelpRequests.filter(r => !r.isResolved);
  },

  async getPendingHelpRequestCount(): Promise<number> {
    await delay(200);
    return mockHelpRequests.filter(r => !r.isResolved).length;
  },

  async resolveHelpRequest(requestId: number): Promise<void> {
    await delay(300);
    const request = mockHelpRequests.find(r => r.id === requestId);
    if (request) {
      request.isResolved = true;
    }
  },

  // Statistics
  async getLectureStatistics(lectureId: number): Promise<LectureStatistics> {
    await delay(500);
    return {
      ...mockStatistics,
      lectureId,
      lectureName: mockLectures.find(l => l.id === lectureId)?.title || 'Unknown',
    };
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