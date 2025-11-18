import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { liveSessionService } from '../lib/live-session-service';
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
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (sessionId) {
      loadInitialData();
    }
  }, [sessionId]);

  useEffect(() => {
    if (selectedStudentId) {
      loadStudentScreen(selectedStudentId);
      
      // Auto-refresh screen every 5 seconds
      const interval = setInterval(() => {
        loadStudentScreen(selectedStudentId);
      }, 5000);
      
      return () => clearInterval(interval);
    }
  }, [selectedStudentId]);

  useEffect(() => {
    // Auto-refresh notifications every 10 seconds
    const interval = setInterval(() => {
      if (sessionId) {
        loadNotifications();
      }
    }, 10000);
    
    return () => clearInterval(interval);
  }, [sessionId]);

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
    try {
      setStudentScreen(prev => prev ? { ...prev, isLoading: true } : null);
      const screen = await liveSessionService.getStudentScreen(studentId);
      setStudentScreen(screen);
    } catch (error) {
      setStudentScreen(prev => prev ? {
        ...prev,
        isLoading: false,
        error: '화면을 불러올 수 없습니다',
      } : null);
    }
  };

  const loadNotifications = async () => {
    if (!sessionId) return;
    
    try {
      const notifs = await liveSessionService.getNotifications(parseInt(sessionId));
      setNotifications(notifs);
    } catch (error) {
      console.error('Failed to load notifications:', error);
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
    
    try {
      const updated = await liveSessionService.startSession(parseInt(sessionId));
      setSessionData(updated);
      toast.success('수업이 시작되었습니다');
    } catch (error) {
      toast.error('수업 시작에 실패했습니다');
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
      const updated = await liveSessionService.endSession(parseInt(sessionId));
      setSessionData(updated);
      toast.success('수업이 종료되었습니다');
      
      // Redirect after 2 seconds
      setTimeout(() => {
        navigate('/sessions');
      }, 2000);
    } catch (error) {
      toast.error('수업 종료에 실패했습니다');
    }
  };

  const handleResolveNotification = async (notificationId: number) => {
    try {
      await liveSessionService.resolveNotification(notificationId);
      setNotifications(prev => prev.filter(n => n.id !== notificationId));
      toast.success('알림이 해결되었습니다');
    } catch (error) {
      toast.error('알림 해결에 실패했습니다');
    }
  };

  const handleLogout = () => {
    navigate('/');
  };

  const handleBroadcastMessage = () => {
    // TODO: Implement broadcast message modal
    toast.success('전체 학생에게 알림을 전송했습니다');
  };

  const handleTakeSnapshot = () => {
    // TODO: Implement snapshot functionality
    toast.success('전체 학생 화면 스냅샷을 저장했습니다');
  };

  if (loading || !sessionData) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 mx-auto mb-4" style={{ borderColor: 'var(--primary)' }}></div>
          <p>세션 데이터 로딩 중...</p>
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
          paddingTop: sessionData.status === 'ACTIVE' || sessionData.status === 'PAUSED' ? '112px' : '64px' 
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
        />
      </div>
    </div>
  );
}