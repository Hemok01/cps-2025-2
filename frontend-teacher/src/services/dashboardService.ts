import apiClient from './api';
import type { StudentProgress, LectureStatistics } from '../types';

export const dashboardService = {
  // Get students for a lecture
  getLectureStudents: async (lectureId: number): Promise<StudentProgress[]> => {
    const response = await apiClient.get<{ lecture_id: number; students: StudentProgress[] }>(
      `/api/dashboard/lectures/${lectureId}/students/`
    );
    return response.data.students;
  },

  // Get lecture statistics
  getLectureStatistics: async (lectureId: number): Promise<LectureStatistics> => {
    const response = await apiClient.get<LectureStatistics>(
      `/api/dashboard/statistics/lecture/${lectureId}/`
    );
    return response.data;
  },
};
