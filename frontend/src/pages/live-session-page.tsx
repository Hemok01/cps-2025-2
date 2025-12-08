import { useEffect, useState, useCallback, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { liveSessionService } from '../lib/live-session-service';
import { apiService } from '../lib/api-service';
import { Session } from '../lib/types';
import {
  LiveSessionData,
  StudentListItem,
  ProgressData,
  GroupProgress,
  LiveNotification,
  StudentScreen
} from '../lib/live-session-types';
import { TopControlBar } from '../components/live-session/top-control-bar';
import { LeftPanel } from '../components/live-session/left-panel';
import { CenterArea } from '../components/live-session/center-area';
import { RightPanel } from '../components/live-session/right-panel';
import { toast } from 'sonner';
import { wsClient } from '../lib/websocket-client';
import type { IncomingMessage, WebSocketConnectionInfo } from '../types/websocket';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../components/ui/dialog';
import { Textarea } from '../components/ui/textarea';
import { Monitor, Users, Play, RefreshCw, ArrowLeft, Send } from 'lucide-react';

// Status config for badges
const STATUS_CONFIG = {
  CREATED: { label: '생성됨', bgColor: '#F5F5F5', textColor: 'var(--text-secondary)' },
  WAITING: { label: '대기 중', bgColor: '#F5F5F5', textColor: 'var(--text-secondary)' },
  ACTIVE: { label: '진행 중', bgColor: 'var(--success)', textColor: 'white' },
  IN_PROGRESS: { label: '진행 중', bgColor: 'var(--success)', textColor: 'white' },
  PAUSED: { label: '일시정지', bgColor: 'var(--warning)', textColor: 'white' },
  ENDED: { label: '종료됨', bgColor: 'var(--status-inactive)', textColor: 'white' },
  REVIEW_MODE: { label: '복습 모드', bgColor: 'var(--info)', textColor: 'white' },
} as const;

export function LiveSessionPage() {
  const { sessionId } = useParams<{ sessionId: string }>();
  const navigate = useNavigate();

  const [sessionData, setSessionData] = useState<LiveSessionData | null>(null);
  const [students, setStudents] = useState<StudentListItem[]>([]);
  const [progressData, setProgressData] = useState<ProgressData[]>([]);
  const [groupProgress, setGroupProgress] = useState<GroupProgress[]>([]);
  const [notifications, setNotifications] = useState<LiveNotification[]>([]);
  const [studentScreen, setStudentScreen] = useState<StudentScreen | null>(null);
  const [selectedStudentId, setSelectedStudentId] = useState<number | null>(null);
  // 도움 요청으로 스크린샷이 설정된 경우 API 호출을 스킵하기 위한 ref
  const skipScreenLoadRef = useRef<number | null>(null);
  // 도움 요청 중복 방지를 위한 ref (user_id -> 마지막 요청 시간)
  const helpRequestTimestampsRef = useRef<Map<number, number>>(new Map());
  const [loading, setLoading] = useState(true);
  const [activeSessions, setActiveSessions] = useState<Session[]>([]);
  const [wsConnectionInfo, setWsConnectionInfo] = useState<WebSocketConnectionInfo>({
    status: 'disconnected',
    sessionCode: null,
    reconnectAttempts: 0,
  });

  // Broadcast message modal state
  const [showBroadcastModal, setShowBroadcastModal] = useState(false);
  const [broadcastMessage, setBroadcastMessage] = useState('');
  const [isBroadcasting, setIsBroadcasting] = useState(false);

  // Load initial data and setup WebSocket
  useEffect(() => {
    if (sessionId) {
      loadInitialData();
    } else {
      // No sessionId - load active sessions for selection
      loadActiveSessions();
    }

    // Cleanup: disconnect WebSocket when component unmounts
    return () => {
      console.log('[LiveSession] Disconnecting WebSocket on unmount');
      wsClient.disconnect();
    };
  }, [sessionId]);

  const loadActiveSessions = async () => {
    setLoading(true);
    try {
      const sessions = await apiService.getInstructorActiveSessions();
      setActiveSessions(sessions);

      // 활성 세션이 하나만 있으면 자동으로 이동
      if (sessions.length === 1) {
        navigate(`/live-session/${sessions[0].id}`, { replace: true });
      }
    } catch (error) {
      console.error('Failed to load active sessions:', error);
      toast.error('활성 세션을 불러오는데 실패했습니다');
    } finally {
      setLoading(false);
    }
  };

  const getStatusBadge = (status: string) => {
    const config = STATUS_CONFIG[status as keyof typeof STATUS_CONFIG] || STATUS_CONFIG.CREATED;
    return (
      <Badge style={{ backgroundColor: config.bgColor, color: config.textColor }}>
        {config.label}
      </Badge>
    );
  };

  // 학생 목록 로드 함수 (useCallback으로 분리)
  const loadStudents = useCallback(async () => {
    if (!sessionId) return;
    try {
      const studentList = await liveSessionService.getStudentList(parseInt(sessionId));
      setStudents(studentList);
    } catch (error) {
      console.error('Failed to load students:', error);
    }
  }, [sessionId]);

  // Handle incoming WebSocket messages
  const handleWebSocketMessage = useCallback((message: IncomingMessage) => {
    console.log('[LiveSession] Received WebSocket message:', message);

    switch (message.type) {
      case 'step_changed':
        // Update session data with new step information
        setSessionData(prev => prev ? {
          ...prev,
          currentStep: message.data.current_step,
          totalSteps: message.data.total_steps,
        } : null);
        toast.info(`단계 변경: ${message.data.current_step}/${message.data.total_steps}`);
        break;

      case 'session_status_changed':
        // Update session status
        const newStatus = message.status?.toUpperCase() || message.data?.status?.toUpperCase() || 'ACTIVE';
        setSessionData(prev => prev ? {
          ...prev,
          status: newStatus as 'CREATED' | 'ACTIVE' | 'IN_PROGRESS' | 'PAUSED' | 'ENDED' | 'REVIEW_MODE',
        } : null);

        // Status changed to REVIEW_MODE or ENDED means session ended
        if (newStatus === 'REVIEW_MODE' || newStatus === 'ENDED') {
          toast.success('수업이 종료되었습니다. 요약 페이지로 이동합니다.');
          setTimeout(() => {
            navigate(`/sessions/${sessionId}/summary`);
          }, 1500);
        } else {
          toast.info(`세션 상태: ${newStatus === 'ACTIVE' || newStatus === 'IN_PROGRESS' ? '진행 중' : newStatus === 'PAUSED' ? '일시정지' : newStatus}`);
        }
        break;

      case 'participant_joined':
        // Add new participant to students list
        toast.success(`${message.data.username}님이 입장했습니다`);
        // Reload student list
        loadStudents();
        break;

      case 'participant_left':
        // Remove participant from students list
        toast.info(`${message.data.username}님이 퇴장했습니다`);
        // Reload student list
        loadStudents();
        break;

      case 'progress_updated':
        // Update progress data for specific student
        setProgressData(prev => {
          const existing = prev.find(p => p.userId === message.data.user_id);
          if (existing) {
            return prev.map(p =>
              p.userId === message.data.user_id
                ? {
                    ...p,
                    currentSubtask: message.data.current_subtask,
                    progressPercentage: message.data.progress_percentage,
                    completedSubtasks: message.data.completed_subtasks,
                  }
                : p
            );
          } else {
            // Add new progress entry
            return [
              ...prev,
              {
                userId: message.data.user_id,
                username: message.data.username,
                currentSubtask: message.data.current_subtask,
                progressPercentage: message.data.progress_percentage,
                completedSubtasks: message.data.completed_subtasks,
              },
            ];
          }
        });
        break;

      case 'help_requested':
        // Update student status to help_needed
        const helpData = message.data;

        // 중복 방지: 동일한 user_id에서 5초 이내 요청이 있었으면 무시
        const now = Date.now();
        const lastHelpTime = helpRequestTimestampsRef.current.get(helpData.user_id);
        if (lastHelpTime && now - lastHelpTime < 5000) {
          console.log('[LiveSession] Skipping duplicate help request (within 5s):', helpData.user_id);
          break;
        }
        // 마지막 요청 시간 기록
        helpRequestTimestampsRef.current.set(helpData.user_id, now);

        setStudents(prev => prev.map(student => {
          if (student.id === helpData.user_id) {
            return {
              ...student,
              status: 'help_needed' as const,
            };
          }
          return student;
        }));

        // Add new help request notification (스크린샷 URL 포함)
        // 유니크한 ID 생성: timestamp + random + user_id
        const notificationId = Date.now() * 1000 + Math.floor(Math.random() * 1000) + (helpData.user_id || 0);
        const newNotification: LiveNotification = {
          id: notificationId,
          type: 'help_request',
          title: '도움 요청',
          studentId: helpData.user_id,
          studentName: helpData.username,
          message: helpData.message || '',
          timestamp: helpData.timestamp || new Date().toISOString(),
          isResolved: false,
          screenshotUrl: helpData.screenshot_url, // 스크린샷 URL 추가
        };

        setNotifications(prev => [newNotification, ...prev]);
        toast.warning(`🆘 도움 요청: ${helpData.username}${helpData.screenshot_url ? ' (스크린샷 포함)' : ''}`);
        break;

      case 'screenshot_updated':
        // Update student screen if viewing this student
        const screenshotData = message.data;
        console.log('[LiveSession] Screenshot updated:', screenshotData);

        // Update if viewing the same participant
        if (selectedStudentId && screenshotData.participant_id === selectedStudentId) {
          setStudentScreen({
            studentId: screenshotData.participant_id || 0,
            studentName: screenshotData.participant_name,
            imageUrl: screenshotData.image_url,
            lastUpdated: screenshotData.captured_at,
            isLoading: false,
            deviceId: screenshotData.device_id,
          });
        }

        // Update student list to show latest screenshot indicator
        setStudents(prev => prev.map(student => {
          if (student.id === screenshotData.participant_id) {
            return {
              ...student,
              hasRecentScreenshot: true,
              lastScreenshotAt: screenshotData.captured_at,
            };
          }
          return student;
        }));
        break;

      case 'student_completion':
        // Handle student step completion notification
        const completionData = message.data;
        console.log('[LiveSession] Student completion:', completionData);

        // Update student list with completion status
        setStudents(prev => prev.map(student => {
          // Match by participant_id or device_id
          if (student.id === completionData.participant_id ||
              student.deviceId === completionData.device_id) {
            return {
              ...student,
              completedSubtasks: completionData.completed_subtasks,
              currentStepCompleted: true,
              lastCompletedAt: completionData.timestamp,
            };
          }
          return student;
        }));

        // Show toast notification
        toast.success(`${completionData.student_name}님이 단계를 완료했습니다`, {
          description: `완료된 단계: ${completionData.total_completed}개`,
          duration: 3000,
        });
        break;

      case 'error':
        // Handle error messages
        toast.error(`오류: ${message.data.message}`);
        break;

      default:
        console.warn('[LiveSession] Unknown message type:', message.type);
    }
  }, [loadStudents, selectedStudentId, navigate, sessionId]);

  // Setup WebSocket connection after initial data is loaded
  useEffect(() => {
    if (sessionData?.sessionCode) {
      console.log('[LiveSession] Setting up WebSocket for session:', sessionData.sessionCode);

      // Connect to WebSocket
      wsClient.connect(sessionData.sessionCode);

      // Subscribe to connection status updates
      const unsubscribeStatus = wsClient.subscribeToStatus((info) => {
        setWsConnectionInfo(info);

        if (info.status === 'connected') {
          toast.success('실시간 연결이 설정되었습니다');
        } else if (info.status === 'error') {
          toast.error(`연결 오류: ${info.lastError || '알 수 없는 오류'}`);
        } else if (info.status === 'reconnecting') {
          toast.info(`재연결 중... (${info.reconnectAttempts}/5)`);
        }
      });

      // Subscribe to WebSocket messages
      const unsubscribeMessages = wsClient.subscribe(handleWebSocketMessage);

      return () => {
        unsubscribeStatus();
        unsubscribeMessages();
      };
    }
  }, [sessionData?.sessionCode, handleWebSocketMessage]);

  // Load student screen when selected
  // 도움 요청 등으로 이미 스크린샷이 설정된 경우 API 호출 스킵
  useEffect(() => {
    console.log('[LiveSession] useEffect triggered - selectedStudentId:', selectedStudentId, 'sessionId:', sessionId, 'skipRef:', skipScreenLoadRef.current, 'currentScreen:', studentScreen?.studentId, studentScreen?.imageUrl ? 'hasImage' : 'noImage');

    if (selectedStudentId && sessionId) {
      // skipScreenLoadRef를 통해 도움 요청으로 설정된 스크린샷이 있는지 확인
      // (closure 문제를 피하기 위해 ref 사용)
      if (skipScreenLoadRef.current === selectedStudentId) {
        console.log('[LiveSession] Skipping API call - screenshot set via help request for student:', selectedStudentId);
        skipScreenLoadRef.current = null; // 다음 선택을 위해 리셋
        return;
      }
      console.log('[LiveSession] Calling loadStudentScreen for:', selectedStudentId);
      loadStudentScreen(selectedStudentId);
      // Note: Real-time updates will come via WebSocket, no need for polling
    }
  }, [selectedStudentId, sessionId]);

  const loadInitialData = async () => {
    if (!sessionId) return;
    
    setLoading(true);
    try {
      const [session, studentList, progress, groups, notifs] = await Promise.all([
        liveSessionService.getSessionData(parseInt(sessionId)),
        liveSessionService.getStudentList(parseInt(sessionId)),
        liveSessionService.getProgressData(parseInt(sessionId)),
        liveSessionService.getGroupProgress(parseInt(sessionId)),
        liveSessionService.getNotifications(parseInt(sessionId)),
      ]);

      setSessionData(session);
      setStudents(studentList);
      setProgressData(progress);
      setGroupProgress(groups);
      setNotifications(notifs);

      // Auto-select first student
      if (studentList.length > 0) {
        setSelectedStudentId(studentList[0].id);
      }
    } catch (error) {
      toast.error('데이터를 불러오는데 실패했습니다');
      console.error('Failed to load session data:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadStudentScreen = async (studentId: number) => {
    if (!sessionId) return;

    // 현재 화면 상태 저장 (API 호출 전)
    const currentScreen = studentScreen;

    try {
      setStudentScreen(prev => prev ? { ...prev, isLoading: true } : {
        studentId,
        studentName: '',
        imageUrl: undefined,
        lastUpdated: new Date().toISOString(),
        isLoading: true,
      });
      const screen = await liveSessionService.getStudentScreen(studentId, parseInt(sessionId));

      // API 결과에 이미지가 없고, 현재 같은 학생의 이미지가 있으면 기존 이미지 유지
      // (도움 요청으로 받은 스크린샷이 DB에 없을 수 있음)
      if (!screen.imageUrl && currentScreen?.studentId === studentId && currentScreen?.imageUrl) {
        console.log('[LiveSession] API returned no image, keeping existing screenshot');
        setStudentScreen({
          ...currentScreen,
          isLoading: false,
          error: undefined,
        });
        return;
      }

      setStudentScreen(screen);
    } catch (error) {
      // 에러 시에도 기존 이미지 유지 시도
      if (currentScreen?.studentId === studentId && currentScreen?.imageUrl) {
        console.log('[LiveSession] API error, keeping existing screenshot');
        setStudentScreen({
          ...currentScreen,
          isLoading: false,
        });
        return;
      }

      setStudentScreen(prev => prev ? {
        ...prev,
        isLoading: false,
        error: '화면을 불러올 수 없습니다',
      } : {
        studentId,
        studentName: '',
        imageUrl: undefined,
        lastUpdated: new Date().toISOString(),
        isLoading: false,
        error: '화면을 불러올 수 없습니다',
      });
    }
  };

  const handleStudentSelect = (studentId: number) => {
    setSelectedStudentId(studentId);
    setStudents(prev => prev.map(s => ({
      ...s,
      isSelected: s.id === studentId,
    })));
  };

  const handlePreviousStudent = () => {
    const currentIndex = students.findIndex(s => s.id === selectedStudentId);
    if (currentIndex > 0) {
      handleStudentSelect(students[currentIndex - 1].id);
    }
  };

  const handleNextStudent = () => {
    const currentIndex = students.findIndex(s => s.id === selectedStudentId);
    if (currentIndex < students.length - 1) {
      handleStudentSelect(students[currentIndex + 1].id);
    }
  };

  const handleRefreshScreen = () => {
    if (selectedStudentId) {
      loadStudentScreen(selectedStudentId);
      toast.success('화면을 새로고침했습니다');
    }
  };

  const handleStart = async () => {
    if (!sessionId) return;

    // 이미 시작된 세션인지 확인
    if (sessionData?.status === 'ACTIVE' || sessionData?.status === 'IN_PROGRESS') {
      toast.info('이미 진행 중인 수업입니다');
      return;
    }

    try {
      // Use REST API for session start (initial setup)
      const updated = await liveSessionService.startSession(parseInt(sessionId));
      setSessionData(updated);
      toast.success('수업이 시작되었습니다');
    } catch (error: any) {
      // 이미 시작된 세션 에러 처리
      if (error.response?.status === 400) {
        toast.info('이미 진행 중인 수업입니다');
        // 세션 데이터 새로고침
        const refreshed = await liveSessionService.getSessionData(parseInt(sessionId));
        setSessionData(refreshed);
      } else {
        toast.error('수업 시작에 실패했습니다');
      }
    }
  };

  const handlePause = async () => {
    if (!sessionId) return;

    try {
      const updated = await liveSessionService.pauseSession(parseInt(sessionId));
      setSessionData(updated);
      toast.success('수업이 일시정지되었습니다');
    } catch (error) {
      toast.error('일시정지에 실패했습니다');
    }
  };

  const handleResume = async () => {
    if (!sessionId) return;

    try {
      const updated = await liveSessionService.resumeSession(parseInt(sessionId));
      setSessionData(updated);
      toast.success('수업이 재개되었습니다');
    } catch (error) {
      toast.error('재개에 실패했습니다');
    }
  };

  const handleNextStep = async () => {
    if (!sessionId) return;

    try {
      await liveSessionService.nextStep(parseInt(sessionId));
      // 세션 데이터 새로고침
      const updated = await liveSessionService.getSessionData(parseInt(sessionId));
      setSessionData(updated);
      toast.success('다음 단계로 진행되었습니다');
    } catch (error) {
      toast.error('다음 단계 진행에 실패했습니다');
    }
  };

  const handleSwitchLecture = async (lectureId: number) => {
    if (!sessionId) return;
    
    try {
      const updated = await liveSessionService.switchLecture(parseInt(sessionId), lectureId);
      setSessionData(updated);
      toast.success('강의가 전환되었습니다');
      
      // Reload data for new lecture
      loadInitialData();
    } catch (error) {
      toast.error('강의 전환에 실패했습니다');
    }
  };

  const handleEnd = async () => {
    if (!sessionId) return;

    if (!confirm('정말로 수업을 종료하시겠습니까?')) return;

    try {
      await liveSessionService.endSession(parseInt(sessionId));
      toast.success('수업이 종료되었습니다. 요약 페이지로 이동합니다.');

      // 세션 요약 페이지로 이동
      setTimeout(() => {
        navigate(`/sessions/${sessionId}/summary`);
      }, 1000);
    } catch (error) {
      toast.error('수업 종료에 실패했습니다');
    }
  };

  const handleResolveNotification = async (notificationId: number) => {
    try {
      // Find the notification to get studentId before removing
      const notification = notifications.find(n => n.id === notificationId);

      await liveSessionService.resolveNotification(notificationId);

      // Remove the notification
      setNotifications(prev => prev.filter(n => n.id !== notificationId));

      // Reset student status to active if studentId exists
      if (notification?.studentId) {
        setStudents(prev => prev.map(student => {
          if (student.id === notification.studentId) {
            return {
              ...student,
              status: 'active' as const,
            };
          }
          return student;
        }));
      }

      toast.success('알림이 해결되었습니다');
    } catch (error) {
      toast.error('알림 해결에 실패했습니다');
    }
  };

  const handleLogout = () => {
    navigate('/');
  };

  const handleBroadcastMessage = () => {
    setShowBroadcastModal(true);
  };

  const handleSendBroadcast = async () => {
    if (!sessionId || !broadcastMessage.trim()) {
      toast.error('메시지를 입력해주세요');
      return;
    }

    setIsBroadcasting(true);
    try {
      const result = await apiService.broadcastMessage(parseInt(sessionId), broadcastMessage.trim());
      if (result.success) {
        toast.success(`${result.broadcastTo}명의 학생에게 메시지를 전송했습니다`);
        setShowBroadcastModal(false);
        setBroadcastMessage('');
      }
    } catch (error) {
      toast.error('메시지 전송에 실패했습니다');
      console.error('Broadcast failed:', error);
    } finally {
      setIsBroadcasting(false);
    }
  };

  const handleTakeSnapshot = () => {
    // TODO: Implement snapshot functionality
    toast.success('전체 학생 화면 스냅샷을 저장했습니다');
  };

  const handleViewScreen = (notification: LiveNotification) => {
    if (!notification.studentId) {
      toast.error('학생 정보를 찾을 수 없습니다');
      return;
    }

    let fullImageUrl: string | undefined;

    // 스크린샷이 있으면 URL 변환
    if (notification.screenshotUrl) {
      const backendUrl = import.meta.env.VITE_API_BASE_URL?.replace('/api', '') || 'http://localhost:8000';
      fullImageUrl = notification.screenshotUrl.startsWith('http')
        ? notification.screenshotUrl
        : `${backendUrl}${notification.screenshotUrl}`;
    }

    // API 호출 스킵 플래그 설정 (useEffect보다 먼저 실행)
    skipScreenLoadRef.current = notification.studentId;

    setStudentScreen({
      studentId: notification.studentId,
      studentName: notification.studentName || '',
      imageUrl: fullImageUrl,
      lastUpdated: notification.timestamp || new Date().toISOString(),
      isLoading: false,
      error: fullImageUrl ? undefined : '스크린샷이 전송되지 않았습니다',
    });
    setSelectedStudentId(notification.studentId);

    // 학생 선택 상태 업데이트
    setStudents(prev => prev.map(s => ({
      ...s,
      isSelected: s.id === notification.studentId,
    })));

    toast.info(`${notification.studentName}님의 화면을 확인합니다`);
  };

  // Loading state
  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-center">
          <RefreshCw className="w-12 h-12 animate-spin mx-auto mb-4 text-gray-400" />
          <p>세션 데이터 로딩 중...</p>
        </div>
      </div>
    );
  }

  // No sessionId - show session selection UI
  if (!sessionId) {
    return (
      <div className="min-h-screen bg-gray-50 p-8">
        <div className="max-w-4xl mx-auto space-y-6">
          <div className="flex items-center gap-4">
            <Button variant="ghost" onClick={() => navigate('/sessions')} className="gap-2">
              <ArrowLeft className="w-4 h-4" />
              수업 시작으로 돌아가기
            </Button>
          </div>

          <div>
            <h1 className="text-3xl font-bold mb-2">실시간 모니터링</h1>
            <p className="text-gray-600">진행 중인 수업을 선택하여 학생들을 실시간으로 모니터링하세요</p>
          </div>

          {activeSessions.length === 0 ? (
            <Card>
              <CardContent className="py-12">
                <div className="text-center">
                  <Monitor className="w-16 h-16 mx-auto mb-4 text-gray-300" />
                  <h3 className="text-lg font-medium text-gray-700 mb-2">진행 중인 수업이 없습니다</h3>
                  <p className="text-gray-500 mb-6">새 수업을 시작하여 학생들을 모니터링하세요</p>
                  <Button onClick={() => navigate('/sessions')} className="gap-2">
                    <Play className="w-4 h-4" />
                    수업 시작하기
                  </Button>
                </div>
              </CardContent>
            </Card>
          ) : (
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Monitor className="w-5 h-5" />
                  진행 중인 수업 ({activeSessions.length}개)
                </CardTitle>
                <CardDescription>
                  모니터링할 수업을 선택하세요
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-3">
                {activeSessions.map((session) => (
                  <div
                    key={session.id}
                    className="flex items-center justify-between p-4 bg-white rounded-lg border hover:border-blue-300 hover:bg-blue-50 transition-colors cursor-pointer"
                    onClick={() => navigate(`/live-session/${session.id}`)}
                  >
                    <div className="flex items-center gap-4">
                      <div className="w-12 h-12 rounded-full bg-blue-100 flex items-center justify-center">
                        <Monitor className="w-6 h-6 text-blue-600" />
                      </div>
                      <div>
                        <p className="font-medium">{session.title}</p>
                        <p className="text-sm text-gray-500">
                          코드: <span className="font-mono font-bold">{session.code}</span>
                          {session.participantCount !== undefined && (
                            <span className="ml-2">
                              <Users className="w-3 h-3 inline mr-1" />
                              {session.participantCount}명 참가 중
                            </span>
                          )}
                        </p>
                      </div>
                    </div>
                    <div className="flex items-center gap-3">
                      {getStatusBadge(session.status)}
                      <Button variant="outline" size="sm" className="gap-2">
                        <Monitor className="w-4 h-4" />
                        모니터링
                      </Button>
                    </div>
                  </div>
                ))}
              </CardContent>
            </Card>
          )}
        </div>
      </div>
    );
  }

  // Has sessionId but no session data
  if (!sessionData) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-center">
          <Monitor className="w-16 h-16 mx-auto mb-4 text-gray-300" />
          <h3 className="text-lg font-medium text-gray-700 mb-2">세션을 찾을 수 없습니다</h3>
          <p className="text-gray-500 mb-6">세션이 종료되었거나 존재하지 않습니다</p>
          <Button onClick={() => navigate('/live-session')} className="gap-2">
            <ArrowLeft className="w-4 h-4" />
            세션 선택으로 돌아가기
          </Button>
        </div>
      </div>
    );
  }

  const currentStudentIndex = students.findIndex(s => s.id === selectedStudentId);

  // Mock participants for right panel
  const participants = students.slice(0, 10).map(s => ({
    id: s.id,
    name: s.name,
    avatarUrl: s.avatarUrl,
    isOnline: s.status === 'active',
  }));

  // Calculate stats for top bar
  const activeStudents = students.filter(s => s.status === 'active').length;
  const helpRequestCount = students.filter(s => s.status === 'help_needed').length;

  return (
    <div className="h-screen flex flex-col overflow-hidden">
      {/* Top Control Bar */}
      <TopControlBar
        sessionTitle={sessionData.lectureName}
        sessionCode={sessionData.sessionCode}
        sessionStatus={sessionData.status}
        startedAt={sessionData.startedAt}
        lectures={sessionData.lectures}
        activeLectureId={sessionData.activeLectureId}
        currentStep={3}
        totalSteps={10}
        activeStudents={activeStudents}
        totalStudents={students.length}
        helpRequestCount={helpRequestCount}
        onStart={handleStart}
        onPause={handlePause}
        onResume={handleResume}
        onNextStep={handleNextStep}
        onEnd={handleEnd}
        onSwitchLecture={handleSwitchLecture}
        onLogout={handleLogout}
        onBroadcastMessage={handleBroadcastMessage}
        onTakeSnapshot={handleTakeSnapshot}
      />

      {/* Main Content - 3 Column Layout */}
      <div
        className="flex-1 flex overflow-hidden"
        style={{
          paddingTop: ['ACTIVE', 'IN_PROGRESS', 'PAUSED', 'REVIEW_MODE'].includes(sessionData.status) ? '112px' : '64px'
        }}
      >
        {/* Left Panel */}
        <LeftPanel
          lectureName={sessionData.lectureName}
          lectureDate={sessionData.lectureDate}
          instructor={sessionData.instructor}
          totalStudents={sessionData.totalStudents}
          students={students}
          progressData={progressData}
          selectedStudentId={selectedStudentId}
          onStudentSelect={handleStudentSelect}
        />

        {/* Center Area */}
        <CenterArea
          studentScreen={studentScreen}
          totalStudents={students.length}
          currentStudentIndex={currentStudentIndex}
          onPreviousStudent={handlePreviousStudent}
          onNextStudent={handleNextStudent}
          onRefresh={handleRefreshScreen}
        />

        {/* Right Panel */}
        <RightPanel
          participants={participants}
          groupProgress={groupProgress}
          notifications={notifications}
          onResolveNotification={handleResolveNotification}
          onViewScreen={handleViewScreen}
        />
      </div>

      {/* Broadcast Message Modal */}
      <Dialog open={showBroadcastModal} onOpenChange={setShowBroadcastModal}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Send className="w-5 h-5" />
              전체 알림 전송
            </DialogTitle>
            <DialogDescription>
              모든 학생에게 메시지를 전송합니다. 학생 화면에 알림이 표시됩니다.
            </DialogDescription>
          </DialogHeader>
          <div className="py-4">
            <Textarea
              placeholder="학생들에게 전달할 메시지를 입력하세요..."
              value={broadcastMessage}
              onChange={(e) => setBroadcastMessage(e.target.value)}
              className="min-h-[120px] resize-none"
              maxLength={500}
            />
            <p className="text-xs text-gray-500 mt-2 text-right">
              {broadcastMessage.length}/500
            </p>
          </div>
          <DialogFooter className="gap-2 sm:gap-0">
            <Button
              variant="outline"
              onClick={() => {
                setShowBroadcastModal(false);
                setBroadcastMessage('');
              }}
              disabled={isBroadcasting}
            >
              취소
            </Button>
            <Button
              onClick={handleSendBroadcast}
              disabled={!broadcastMessage.trim() || isBroadcasting}
              className="gap-2"
            >
              {isBroadcasting ? (
                <>
                  <RefreshCw className="w-4 h-4 animate-spin" />
                  전송 중...
                </>
              ) : (
                <>
                  <Send className="w-4 h-4" />
                  전송하기
                </>
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}