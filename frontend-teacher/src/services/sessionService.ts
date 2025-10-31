import apiClient from './api';
import type {
  Lecture,
  LectureSession,
  CreateSessionResponse,
  SessionParticipant,
  PaginatedResponse,
} from '../types';

export const sessionService = {
  // Get lectures
  getLectures: async (): Promise<Lecture[]> => {
    const response = await apiClient.get<PaginatedResponse<Lecture>>('/api/lectures/');
    return response.data.results;
  },

  // Get lecture by ID
  getLecture: async (id: number): Promise<Lecture> => {
    const response = await apiClient.get<Lecture>(`/api/lectures/${id}/`);
    return response.data;
  },

  // Create session
  createSession: async (lectureId: number, title: string): Promise<CreateSessionResponse> => {
    const response = await apiClient.post<CreateSessionResponse>(
      `/api/lectures/${lectureId}/sessions/create/`,
      { title }
    );
    return response.data;
  },

  // Get session by ID
  getSession: async (sessionId: number): Promise<LectureSession> => {
    const response = await apiClient.get<LectureSession>(`/api/sessions/${sessionId}/`);
    return response.data;
  },

  // Get session by code
  getSessionByCode: async (sessionCode: string): Promise<LectureSession> => {
    const response = await apiClient.get<LectureSession>(`/api/sessions/${sessionCode}/`);
    return response.data;
  },

  // Get session participants
  getSessionParticipants: async (sessionId: number): Promise<SessionParticipant[]> => {
    const response = await apiClient.get<SessionParticipant[]>(
      `/api/sessions/${sessionId}/participants/`
    );
    return response.data;
  },

  // Start session
  startSession: async (sessionId: number): Promise<LectureSession> => {
    const response = await apiClient.post<LectureSession>(`/api/sessions/${sessionId}/start/`);
    return response.data;
  },

  // Next step
  nextStep: async (sessionId: number): Promise<LectureSession> => {
    const response = await apiClient.post<LectureSession>(`/api/sessions/${sessionId}/next-step/`);
    return response.data;
  },

  // Pause session
  pauseSession: async (sessionId: number): Promise<LectureSession> => {
    const response = await apiClient.post<LectureSession>(`/api/sessions/${sessionId}/pause/`);
    return response.data;
  },

  // Resume session
  resumeSession: async (sessionId: number): Promise<LectureSession> => {
    const response = await apiClient.post<LectureSession>(`/api/sessions/${sessionId}/resume/`);
    return response.data;
  },

  // End session
  endSession: async (sessionId: number): Promise<LectureSession> => {
    const response = await apiClient.post<LectureSession>(`/api/sessions/${sessionId}/end/`);
    return response.data;
  },

  // Get current session state
  getCurrentSession: async (sessionId: number): Promise<LectureSession> => {
    const response = await apiClient.get<LectureSession>(`/api/sessions/${sessionId}/current/`);
    return response.data;
  },
};
