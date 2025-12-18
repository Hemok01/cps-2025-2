import {
  Lecture,
  CreateLectureRequest,
  UpdateLectureRequest,
  RecordingProcessResponse,
  RecordingMetadata,
  LectureStep,
  AvailableTask,
  BackendSubtask
} from './lecture-types';
import apiClient from './api-client';

function delay(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms));
}

export const lectureService = {
  async getAllLectures(): Promise<Lecture[]> {
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

      // 백엔드 응답을 프론트엔드 형식으로 변환
      return lecturesData.map((lecture: any) => ({
        id: lecture.id,
        title: lecture.title,
        description: lecture.description || '',
        studentCount: lecture.enrolled_count || lecture.student_count || 0,
        sessionCount: lecture.session_count || 0,
        taskCount: lecture.task_count || 0,
        stepCount: lecture.subtask_count || 0,
        isActive: lecture.is_active !== undefined ? lecture.is_active : true,
        createdAt: lecture.created_at,
        updatedAt: lecture.updated_at,
        instructor: lecture.instructor?.name || lecture.instructor || '강사',
        difficulty: lecture.difficulty || 'beginner',
        duration: lecture.duration || 30,
        steps: lecture.steps || [],
        recordingId: lecture.recording_id || null,
      }));
    } catch (error) {
      console.error('Failed to fetch lectures:', error);
      throw error;
    }
  },

  async getLectureById(id: number): Promise<Lecture | null> {
    try {
      const response = await apiClient.get(`/lectures/${id}/`);
      const lecture = response.data;
      return {
        id: lecture.id,
        title: lecture.title,
        description: lecture.description || '',
        studentCount: lecture.enrolled_count || lecture.student_count || 0,
        sessionCount: lecture.session_count || 0,
        taskCount: lecture.task_count || 0,
        stepCount: lecture.subtask_count || 0,
        isActive: lecture.is_active !== undefined ? lecture.is_active : true,
        createdAt: lecture.created_at,
        updatedAt: lecture.updated_at,
        instructor: lecture.instructor?.name || lecture.instructor || '강사',
        difficulty: lecture.difficulty || 'beginner',
        duration: lecture.duration || 30,
        steps: lecture.steps || [],
        recordingId: lecture.recording_id || null,
      };
    } catch (error: any) {
      if (error.response?.status === 404) {
        return null;
      }
      console.error('Failed to fetch lecture:', error);
      throw error;
    }
  },

  async createLecture(data: CreateLectureRequest): Promise<Lecture> {
    try {
      const response = await apiClient.post('/lectures/', {
        title: data.title,
        description: data.description,
        difficulty: data.difficulty,
        duration: data.duration,
        steps: data.steps,
        recording_id: data.recordingId,
      });
      const lecture = response.data;
      return {
        id: lecture.id,
        title: lecture.title,
        description: lecture.description || '',
        studentCount: lecture.enrolled_count || lecture.student_count || 0,
        sessionCount: lecture.session_count || 0,
        taskCount: lecture.task_count || 0,
        stepCount: lecture.subtask_count || 0,
        isActive: lecture.is_active !== undefined ? lecture.is_active : true,
        createdAt: lecture.created_at,
        updatedAt: lecture.updated_at,
        instructor: lecture.instructor?.name || lecture.instructor || '강사',
        difficulty: lecture.difficulty || 'beginner',
        duration: lecture.duration || 30,
        steps: lecture.steps || [],
        recordingId: lecture.recording_id || null,
      };
    } catch (error) {
      console.error('Failed to create lecture:', error);
      throw error;
    }
  },

  async updateLecture(data: UpdateLectureRequest): Promise<Lecture> {
    try {
      const response = await apiClient.patch(`/lectures/${data.id}/`, {
        title: data.title,
        description: data.description,
        difficulty: data.difficulty,
        duration: data.duration,
        steps: data.steps,
        is_active: data.isActive,
        recording_id: data.recordingId,
      });
      const lecture = response.data;
      return {
        id: lecture.id,
        title: lecture.title,
        description: lecture.description || '',
        studentCount: lecture.enrolled_count || lecture.student_count || 0,
        sessionCount: lecture.session_count || 0,
        taskCount: lecture.task_count || 0,
        stepCount: lecture.subtask_count || 0,
        isActive: lecture.is_active !== undefined ? lecture.is_active : true,
        createdAt: lecture.created_at,
        updatedAt: lecture.updated_at,
        instructor: lecture.instructor?.name || lecture.instructor || '강사',
        difficulty: lecture.difficulty || 'beginner',
        duration: lecture.duration || 30,
        steps: lecture.steps || [],
        recordingId: lecture.recording_id || null,
      };
    } catch (error) {
      console.error('Failed to update lecture:', error);
      throw error;
    }
  },

  async deleteLecture(id: number): Promise<void> {
    try {
      await apiClient.delete(`/lectures/${id}/`);
    } catch (error) {
      console.error('Failed to delete lecture:', error);
      throw error;
    }
  },

  async toggleLectureStatus(id: number): Promise<Lecture> {
    try {
      // 먼저 현재 상태를 가져옴
      const currentLecture = await this.getLectureById(id);
      if (!currentLecture) {
        throw new Error('Lecture not found');
      }

      // 상태를 반전시켜 업데이트
      const response = await apiClient.patch(`/lectures/${id}/`, {
        is_active: !currentLecture.isActive
      });
      const lecture = response.data;
      return {
        id: lecture.id,
        title: lecture.title,
        description: lecture.description || '',
        studentCount: lecture.enrolled_count || lecture.student_count || 0,
        sessionCount: lecture.session_count || 0,
        taskCount: lecture.task_count || 0,
        stepCount: lecture.subtask_count || 0,
        isActive: lecture.is_active !== undefined ? lecture.is_active : true,
        createdAt: lecture.created_at,
        updatedAt: lecture.updated_at,
        instructor: lecture.instructor?.name || lecture.instructor || '강사',
        difficulty: lecture.difficulty || 'beginner',
        duration: lecture.duration || 30,
        steps: lecture.steps || [],
        recordingId: lecture.recording_id || null,
      };
    } catch (error) {
      console.error('Failed to toggle lecture status:', error);
      throw error;
    }
  },

  // Get available phone recordings from instructor's device
  async getAvailableRecordings(): Promise<RecordingMetadata[]> {
    try {
      const response = await apiClient.get('/recordings/');
      // DRF 페이지네이션 응답 처리
      const data = response.data.results || response.data;

      // 배열인지 확인
      if (!Array.isArray(data)) {
        console.warn('Unexpected response format for recordings:', response.data);
        return [];
      }

      // 백엔드 응답을 프론트엔드 형식으로 변환
      return data.map((rec: any) => ({
        id: String(rec.id),
        name: rec.title || rec.name || '제목 없음',
        createdAt: rec.created_at,
        actionCount: rec.event_count || rec.action_count || 0,
        duration: rec.duration_seconds || rec.duration || 0,
        apps: rec.apps || [],
        primaryApp: rec.primary_app || rec.apps?.[0] || '',
        deviceInfo: rec.device_info || {
          model: 'Unknown',
          androidVersion: 'Unknown'
        }
      }));
    } catch (error) {
      console.error('Failed to fetch recordings:', error);
      // 에러 시 빈 배열 반환 (UI 크래시 방지)
      return [];
    }
  },

  // Get detailed recording metadata
  async getRecordingDetails(recordingId: string): Promise<RecordingMetadata | null> {
    try {
      const response = await apiClient.get(`/recordings/${recordingId}/`);
      const rec = response.data;
      return {
        id: String(rec.id),
        name: rec.title || rec.name || '제목 없음',
        createdAt: rec.created_at,
        actionCount: rec.event_count || rec.action_count || 0,
        duration: rec.duration_seconds || rec.duration || 0,
        apps: rec.apps || [],
        primaryApp: rec.primary_app || rec.apps?.[0] || '',
        deviceInfo: rec.device_info || {
          model: 'Unknown',
          androidVersion: 'Unknown'
        }
      };
    } catch (error: any) {
      if (error.response?.status === 404) {
        return null;
      }
      console.error('Failed to fetch recording details:', error);
      return null; // 에러 시 null 반환
    }
  },

  // Process phone recording and generate lecture steps
  async processRecording(recordingId: string): Promise<RecordingProcessResponse> {
    await delay(2500); // Simulate AI processing time
    
    // Get recording details
    const recording = await this.getRecordingDetails(recordingId);
    
    // Mock AI-generated steps based on recording
    // In real implementation, this would analyze the PhoneActionLog array
    const mockGeneratedSteps: LectureStep[] = [];
    
    // Generate different steps based on recording ID
    if (recordingId.includes('001')) {
      // Naver Map
      mockGeneratedSteps.push(
        {
          id: `${recordingId}-step-1`,
          order: 1,
          title: '홈 화면에서 네이버 지도 앱 찾기',
          description: '스마트폰 홈 화면에서 네이버 지도 앱 아이콘을 찾습니다',
          action: '초록색 지도 모양의 네이버 지도 아이콘을 터치하세요',
          expectedResult: '네이버 지도 앱이 실행되고 현재 위치가 표시됩니다',
          tips: '앱이 위치 권한을 요청하면 허용해주세요',
          technicalDetails: {
            targetPackage: 'com.nhn.android.nmap',
            targetViewId: 'com.sec.android.app.launcher:id/icon',
            targetText: '네이버 지도',
          }
        },
        {
          id: `${recordingId}-step-2`,
          order: 2,
          title: '목적지 검색하기',
          description: '상단 검색창에 가고 싶은 장소를 입력합니다',
          action: '상단의 검색창을 터치하고 목적지를 입력하세요',
          expectedResult: '검색 결과가 나타나고 관련 장소들이 표시됩니다',
          technicalDetails: {
            targetPackage: 'com.nhn.android.nmap',
            targetViewId: 'com.nhn.android.nmap:id/home_search_box',
          }
        },
        {
          id: `${recordingId}-step-3`,
          order: 3,
          title: '장소 선택하기',
          description: '검색 결과에서 원하는 장소를 선택합니다',
          action: '목록에서 가고 싶은 장소를 터치하세요',
          expectedResult: '선택한 장소의 상세 정보가 표시됩니다',
          technicalDetails: {
            targetPackage: 'com.nhn.android.nmap',
            targetViewId: 'com.nhn.android.nmap:id/search_result_item',
          }
        },
        {
          id: `${recordingId}-step-4`,
          order: 4,
          title: '길찾기 시작하기',
          description: '선택한 장소로 가는 경로를 찾습니다',
          action: '파란색 \'길찾기\' 버튼을 터치하세요',
          expectedResult: '현재 위치에서 목적지까지의 경로가 지도에 표시됩니다',
          tips: '도보, 대중교통, 자동차 중 원하는 이동 수단을 선택할 수 있습니다',
          technicalDetails: {
            targetPackage: 'com.nhn.android.nmap',
            targetViewId: 'com.nhn.android.nmap:id/btn_route',
            targetText: '길찾기',
          }
        }
      );
    } else if (recordingId.includes('002')) {
      // Instagram
      mockGeneratedSteps.push(
        {
          id: `${recordingId}-step-1`,
          order: 1,
          title: '인스타그램 앱 실행하기',
          description: '홈 화면에서 인스타그램 앱을 찾아 실행합니다',
          action: '그라데이션 카메라 모양의 인스타그램 아이콘을 터치하세요',
          expectedResult: '인스타그램이 실행되고 피드 화면이 나타납니다',
          technicalDetails: {
            targetPackage: 'com.instagram.android',
            targetText: 'Instagram',
          }
        },
        {
          id: `${recordingId}-step-2`,
          order: 2,
          title: '새 게시물 만들기',
          description: '하단의 + 버튼을 눌러 새 게시물을 만듭니다',
          action: '화면 하단 중앙의 + 버튼을 터치하세요',
          expectedResult: '게시물 종류 선택 메뉴가 나타납니다',
          technicalDetails: {
            targetPackage: 'com.instagram.android',
            targetViewId: 'com.instagram.android:id/feed_new_post_button',
            contentDescription: '새 게시물',
          }
        },
        {
          id: `${recordingId}-step-3`,
          order: 3,
          title: '사진 선택하기',
          description: '갤러리에서 업로드할 사진을 선택합니다',
          action: '갤러리에서 원하는 사진을 터치하세요',
          expectedResult: '선택한 사진이 미리보기로 표시됩니다',
          tips: '여러 장을 선택하려면 우측 상단의 다중 선택 버튼을 먼저 누르세요',
          technicalDetails: {
            targetPackage: 'com.instagram.android',
            targetViewId: 'com.instagram.android:id/gallery_image',
          }
        },
        {
          id: `${recordingId}-step-4`,
          order: 4,
          title: '필터 적용하기',
          description: '사진에 원하는 필터를 적용합니다',
          action: '하단의 필터 목록에서 마음에 드는 필터를 터치하세요',
          expectedResult: '선택한 필터가 사진에 적용됩니다',
          technicalDetails: {
            targetPackage: 'com.instagram.android',
            targetViewId: 'com.instagram.android:id/filter_item',
          }
        },
        {
          id: `${recordingId}-step-5`,
          order: 5,
          title: '문구 작성 및 게시',
          description: '게시물에 문구를 작성하고 업로드합니다',
          action: '\'문구 작성...\'을 터치하여 내용을 입력하고 \'공유\' 버튼을 누르세요',
          expectedResult: '게시물이 업로드되고 피드에 표시됩니다',
          technicalDetails: {
            targetPackage: 'com.instagram.android',
            targetViewId: 'com.instagram.android:id/share_button',
            targetText: '공유',
          }
        }
      );
    } else {
      // Delivery app
      mockGeneratedSteps.push(
        {
          id: `${recordingId}-step-1`,
          order: 1,
          title: '배달의민족 앱 실행',
          description: '홈 화면에서 배달의민족 앱을 찾아 실행합니다',
          action: '분홍색 배달의민족 아이콘을 터치하세요',
          expectedResult: '배달의민족이 실행되고 메인 화면이 나타납니다',
          technicalDetails: {
            targetPackage: 'com.sampleapp',
          }
        },
        {
          id: `${recordingId}-step-2`,
          order: 2,
          title: '배달 주소 설정',
          description: '배달받을 주소를 입력하거나 선택합니다',
          action: '상단의 주소를 터치하여 배달 주소를 설정하세요',
          expectedResult: '현재 위치 근처의 음식점들이 표시됩니다',
          technicalDetails: {
            targetPackage: 'com.sampleapp',
          }
        },
        {
          id: `${recordingId}-step-3`,
          order: 3,
          title: '음식점 선택',
          description: '주문하고 싶은 음식점을 선택합니다',
          action: '원하는 음식점을 터치하세요',
          expectedResult: '음식점의 메뉴가 표시됩니다',
          technicalDetails: {
            targetPackage: 'com.sampleapp',
          }
        },
        {
          id: `${recordingId}-step-4`,
          order: 4,
          title: '메뉴 선택 및 장바구니 담기',
          description: '원하는 메뉴를 선택하고 장바구니에 담습니다',
          action: '메뉴를 선택하고 옵션을 고른 후 \'담기\' 버튼을 누르세요',
          expectedResult: '선택한 메뉴가 장바구니에 추가됩니다',
          tips: '최소 주문 금액을 확인하세요',
          technicalDetails: {
            targetPackage: 'com.sampleapp',
          }
        },
        {
          id: `${recordingId}-step-5`,
          order: 5,
          title: '주문하기',
          description: '장바구니를 확인하고 결제합니다',
          action: '\'주문하기\' 버튼을 누르고 결제 수단을 선택한 후 결제하세요',
          expectedResult: '주문이 완료되고 배달 상태를 확인할 수 있습니다',
          technicalDetails: {
            targetPackage: 'com.sampleapp',
          }
        }
      );
    }
    
    // Calculate metadata
    const appsUsed = recording ? [
      { 
        package: recording.primaryApp, 
        name: this.getAppName(recording.primaryApp),
        count: recording.actionCount - 5 
      },
      { 
        package: 'com.android.systemui', 
        name: 'Android System UI',
        count: 5 
      },
    ] : [];
    
    return {
      recordingId,
      generatedSteps: mockGeneratedSteps,
      detectedActions: recording?.actionCount || mockGeneratedSteps.length * 3,
      processingTime: 2150,
      metadata: {
        appsUsed,
        totalDuration: recording?.duration || 180,
        eventTypes: [
          { type: 'TYPE_VIEW_CLICKED', count: mockGeneratedSteps.length },
          { type: 'TYPE_VIEW_TEXT_CHANGED', count: 2 },
          { type: 'TYPE_WINDOW_STATE_CHANGED', count: mockGeneratedSteps.length - 1 },
        ]
      }
    };
  },

  // Helper function to get app name from package
  getAppName(packageName: string): string {
    const appNames: Record<string, string> = {
      'com.google.android.youtube': 'YouTube',
      'com.kakao.talk': '카카오톡',
      'com.nhn.android.nmap': '네이버 지도',
      'com.instagram.android': 'Instagram',
      'com.sampleapp': '배달의민족',
      'com.android.systemui': 'Android System UI',
    };
    return appNames[packageName] || packageName;
  },

  // ============================================
  // Task-based Lecture Creation APIs
  // ============================================

  /**
   * Get available tasks (not linked to any Lecture yet)
   * These are tasks created from Android app recordings
   */
  async getAvailableTasks(): Promise<AvailableTask[]> {
    try {
      const response = await apiClient.get('/tasks/available/');
      // DRF 페이지네이션 응답 처리 (results 필드에 실제 데이터가 있을 수 있음)
      const data = response.data.results || response.data;
      // 배열인지 확인
      if (!Array.isArray(data)) {
        console.warn('Unexpected response format for available tasks:', response.data);
        return [];
      }
      return data;
    } catch (error) {
      console.error('Failed to fetch available tasks:', error);
      // 에러 시 빈 배열 반환 (UI 크래시 방지)
      return [];
    }
  },

  /**
   * Convert Backend Subtasks to Frontend LectureSteps
   * Maps the backend data structure to the frontend format
   */
  convertSubtasksToSteps(subtasks: BackendSubtask[]): LectureStep[] {
    return subtasks.map((subtask, idx) => ({
      id: `subtask-${subtask.id}`,
      order: idx + 1,
      title: subtask.title,
      description: subtask.description,
      action: subtask.guide_text || subtask.text || '',
      expectedResult: subtask.voice_guide_text || '',
      technicalDetails: {
        targetPackage: subtask.target_package,
        targetViewId: subtask.view_id,
        targetText: subtask.text,
        contentDescription: subtask.content_description,
        bounds: subtask.bounds,
      }
    }));
  },

  /**
   * Attach existing independent Tasks to a Lecture
   * Used when creating a lecture from existing tasks
   */
  async attachTasksToLecture(lectureId: number, taskIds: number[]): Promise<{ message: string; attached_task_ids: number[] }> {
    try {
      const response = await apiClient.post(`/lectures/${lectureId}/tasks/attach/`, {
        task_ids: taskIds
      });
      return response.data;
    } catch (error) {
      console.error('Failed to attach tasks to lecture:', error);
      throw error;
    }
  },

  /**
   * Create a lecture and optionally attach tasks
   * Enhanced version that supports task-based lecture creation
   */
  async createLectureWithTasks(
    data: CreateLectureRequest,
    taskIds?: number[]
  ): Promise<Lecture> {
    // First create the lecture
    const lecture = await this.createLecture(data);

    // Then attach tasks if provided
    if (taskIds && taskIds.length > 0) {
      await this.attachTasksToLecture(lecture.id, taskIds);
    }

    return lecture;
  },
};