import {
  Lecture,
  CreateLectureRequest,
  UpdateLectureRequest,
  RecordingProcessResponse,
  RecordingMetadata,
  LectureStep
} from './lecture-types';
import apiClient from './api-client';

// Mock data for development
const mockLectures: Lecture[] = [
  {
    id: 1,
    title: '유튜브 영상 검색하고 좋아요 누르기',
    description: '유튜브에서 원하는 영상을 검색하고 좋아요를 누르는 방법을 배웁니다',
    studentCount: 12,
    sessionCount: 3,
    isActive: true,
    createdAt: '2024-01-15T09:00:00Z',
    updatedAt: '2024-01-20T14:30:00Z',
    instructor: '김강사',
    difficulty: 'beginner',
    duration: 30,
    steps: [
      {
        id: 'step-1',
        order: 1,
        title: '홈 화면에서 유튜브 앱 찾기',
        description: '스마트폰 홈 화면에서 유튜브 앱 아이콘을 찾습니다',
        action: '빨간색 재생 버튼이 있는 유튜브 아이콘을 터치하세요',
        expectedResult: '유튜브 앱이 실행되고 메인 화면이 나타납니다',
        tips: '유튜브 아이콘은 빨간색 배경에 흰색 재생 버튼입니다',
        technicalDetails: {
          targetPackage: 'com.google.android.youtube',
          targetViewId: 'com.google.android.apps.nexuslauncher:id/icon',
          targetText: 'YouTube',
        }
      },
      {
        id: 'step-2',
        order: 2,
        title: '검색 버튼 누르기',
        description: '유튜브 상단의 돋보기 모양 검색 아이콘을 찾습니다',
        action: '화면 상단의 돋보기 아이콘을 터치하세요',
        expectedResult: '검색창이 활성화되고 키보드가 나타납니다',
        technicalDetails: {
          targetPackage: 'com.google.android.youtube',
          targetViewId: 'com.google.android.youtube:id/menu_item_1',
          contentDescription: '검색',
        }
      },
      {
        id: 'step-3',
        order: 3,
        title: '검색어 입력하기',
        description: '원하는 영상의 제목이나 키워드를 입력합니다',
        action: '키보드로 검색어를 입력하고 검색 버튼을 누르세요',
        expectedResult: '검색 결과 목록이 화면에 표시됩니다',
        tips: '천천히 한 글자씩 입력하세요',
        technicalDetails: {
          targetPackage: 'com.google.android.youtube',
          targetViewId: 'com.google.android.youtube:id/search_edit_text',
          targetText: '검색어',
        }
      },
      {
        id: 'step-4',
        order: 4,
        title: '원하는 영상 선택하기',
        description: '검색 결과에서 보고 싶은 영상을 선택합니다',
        action: '목록에서 원하는 영상의 썸네일을 터치하세요',
        expectedResult: '선택한 영상이 재생되기 시작합니다',
        technicalDetails: {
          targetPackage: 'com.google.android.youtube',
          targetViewId: 'com.google.android.youtube:id/thumbnail',
        }
      },
      {
        id: 'step-5',
        order: 5,
        title: '좋아요 버튼 누르기',
        description: '영상 하단의 좋아요 버튼을 찾아 누릅니다',
        action: '엄지손가락 모양의 좋아요 버튼을 터치하세요',
        expectedResult: '좋아요 버튼이 파란색으로 변하고 숫자가 증가합니다',
        tips: '싫어요 버튼과 혼동하지 않도록 주의하세요',
        technicalDetails: {
          targetPackage: 'com.google.android.youtube',
          targetViewId: 'com.google.android.youtube:id/like_button',
          contentDescription: '좋아요',
        }
      }
    ],
    recordingId: 'rec-001',
  },
  {
    id: 2,
    title: '카카오톡으로 사진 전송하기',
    description: '갤러리에서 사진을 선택하여 카카오톡으로 전송하는 방법을 배웁니다',
    studentCount: 8,
    sessionCount: 2,
    isActive: true,
    createdAt: '2024-01-18T10:00:00Z',
    updatedAt: '2024-01-19T16:45:00Z',
    instructor: '김강사',
    difficulty: 'intermediate',
    duration: 45,
    steps: [
      {
        id: 'step-1',
        order: 1,
        title: '카카오톡 앱 실행하기',
        description: '홈 화면에서 카카오톡 앱을 찾아 실행합니다',
        action: '노란색 말풍선 아이콘의 카카오톡을 터치하세요',
        expectedResult: '카카오톡이 실행되고 채팅 목록이 나타납니다',
        technicalDetails: {
          targetPackage: 'com.kakao.talk',
          targetText: '카카오톡',
        }
      },
      {
        id: 'step-2',
        order: 2,
        title: '대화방 선택하기',
        description: '사진을 보낼 대화 상대를 선택합니다',
        action: '채팅 목록에서 대화방을 터치하세요',
        expectedResult: '선택한 대화방이 열립니다',
        technicalDetails: {
          targetPackage: 'com.kakao.talk',
          targetViewId: 'com.kakao.talk:id/chat_item',
        }
      },
      {
        id: 'step-3',
        order: 3,
        title: '사진 첨부 버튼 누르기',
        description: '메시지 입력창 옆의 + 버튼을 누릅니다',
        action: '메시지 입력창 왼쪽의 + 버튼을 터치하세요',
        expectedResult: '여러 옵션 메뉴가 나타납니다',
        technicalDetails: {
          targetPackage: 'com.kakao.talk',
          targetViewId: 'com.kakao.talk:id/media_send_button',
          contentDescription: '미디어 전송',
        }
      },
      {
        id: 'step-4',
        order: 4,
        title: '앨범 선택하기',
        description: '옵션 메뉴에서 앨범을 선택합니다',
        action: '앨범 아이콘을 터치하세요',
        expectedResult: '갤러리가 열리고 사진 목록이 표시됩니다',
        technicalDetails: {
          targetPackage: 'com.kakao.talk',
          targetViewId: 'com.kakao.talk:id/album',
          targetText: '앨범',
        }
      },
      {
        id: 'step-5',
        order: 5,
        title: '사진 선택 및 전송',
        description: '보낼 사진을 선택하고 전송합니다',
        action: '원하는 사진을 선택하고 전송 버튼을 누르세요',
        expectedResult: '선택한 사진이 대화방에 전송됩니다',
        tips: '여러 장을 선택하려면 사진을 길게 누른 후 추가로 선택하세요',
        technicalDetails: {
          targetPackage: 'com.kakao.talk',
          targetViewId: 'com.kakao.talk:id/send',
          targetText: '전송',
        }
      }
    ],
    recordingId: 'rec-002',
  },
];

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
        studentCount: lecture.student_count || lecture.enrolled_count || 0,
        sessionCount: lecture.session_count || 0,
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
        studentCount: lecture.student_count || 0,
        sessionCount: lecture.session_count || 0,
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
        studentCount: lecture.student_count || 0,
        sessionCount: lecture.session_count || 0,
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
        studentCount: lecture.student_count || 0,
        sessionCount: lecture.session_count || 0,
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
        studentCount: lecture.student_count || 0,
        sessionCount: lecture.session_count || 0,
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
      const response = await apiClient.get('/sessions/recordings/');
      // 백엔드 응답을 프론트엔드 형식으로 변환
      return response.data.map((rec: any) => ({
        id: rec.id || rec.recording_id,
        name: rec.title || rec.name,
        createdAt: rec.created_at,
        actionCount: rec.action_count || rec.event_count || 0,
        duration: rec.duration || 0,
        apps: rec.apps || [],
        primaryApp: rec.primary_app || rec.apps?.[0] || '',
        deviceInfo: rec.device_info || {
          model: 'Unknown',
          androidVersion: 'Unknown'
        }
      }));
    } catch (error) {
      console.error('Failed to fetch recordings:', error);
      // 에러 시 목 데이터 반환 (fallback)
      return [
      {
        id: 'rec-new-001',
        name: '네이버 지도로 길찾기',
        createdAt: '2024-11-16T10:30:00Z',
        actionCount: 28,
        duration: 145, // seconds
        apps: ['com.nhn.android.nmap', 'com.android.systemui'],
        primaryApp: 'com.nhn.android.nmap',
        deviceInfo: {
          model: 'Samsung Galaxy A53',
          androidVersion: '13'
        }
      },
      {
        id: 'rec-new-002',
        name: '인스타그램 게시물 작성하기',
        createdAt: '2024-11-16T09:15:00Z',
        actionCount: 35,
        duration: 210,
        apps: ['com.instagram.android', 'com.android.gallery3d', 'com.android.systemui'],
        primaryApp: 'com.instagram.android',
        deviceInfo: {
          model: 'Samsung Galaxy A53',
          androidVersion: '13'
        }
      },
      {
        id: 'rec-new-003',
        name: '배달의민족 주문하기',
        createdAt: '2024-11-15T18:45:00Z',
        actionCount: 42,
        duration: 320,
        apps: ['com.sampleapp', 'com.android.systemui'],
        primaryApp: 'com.sampleapp',
        deviceInfo: {
          model: 'Samsung Galaxy A53',
          androidVersion: '13'
        }
      },
    ];
    }
  },

  // Get detailed recording metadata
  async getRecordingDetails(recordingId: string): Promise<RecordingMetadata | null> {
    try {
      const response = await apiClient.get(`/sessions/recordings/${recordingId}/`);
      const rec = response.data;
      return {
        id: rec.id || rec.recording_id,
        name: rec.title || rec.name,
        createdAt: rec.created_at,
        actionCount: rec.action_count || rec.event_count || 0,
        duration: rec.duration || 0,
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
      // Fallback to mock data
      const recordings = await this.getAvailableRecordings();
      return recordings.find(r => r.id === recordingId) || null;
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
};