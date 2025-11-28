import { useEffect, useState, useCallback } from "react";
import { useSearchParams, useNavigate } from "react-router-dom";
import { QRCodeSVG } from "qrcode.react";
import { apiService } from "../lib/api-service";
import { Lecture, Session, Participant } from "../lib/types";
import { wsClient } from "../lib/websocket-client";
import type { IncomingMessage } from "../types/websocket";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  CardDescription,
} from "../components/ui/card";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { Label } from "../components/ui/label";
import { Badge } from "../components/ui/badge";
import {
  Alert,
  AlertDescription,
} from "../components/ui/alert";
import { Checkbox } from "../components/ui/checkbox";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "../components/ui/table";
import {
  Play,
  Pause,
  Square,
  SkipForward,
  Copy,
  CheckCircle2,
  Users,
  RefreshCw,
  Monitor,
  BookOpen,
  Plus,
  X,
  ChevronRight,
  QrCode,
  Download,
} from "lucide-react";
import { toast } from "sonner";

// Constants
const STATUS_CONFIG = {
  CREATED: { label: "생성됨", bgColor: "#F5F5F5", textColor: "var(--text-secondary)" },
  ACTIVE: { label: "진행 중", bgColor: "var(--success)", textColor: "white" },
  PAUSED: { label: "일시정지", bgColor: "var(--warning)", textColor: "white" },
  ENDED: { label: "종료됨", bgColor: "var(--status-inactive)", textColor: "white" },
} as const;

const POLL_INTERVAL = 5000;

// Types
type SessionAction = 'start' | 'pause' | 'resume' | 'end' | 'nextStep' | 'switchLecture';

interface SessionActionConfig {
  apiCall: (sessionId: number, ...args: any[]) => Promise<Session>;
  successMessage: string | ((data?: any) => string);
  confirmMessage?: string;
}

export function SessionControlPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const preselectedLectureId = searchParams.get("lectureId");

  // State
  const [lectures, setLectures] = useState<Lecture[]>([]);
  const [selectedLectureIds, setSelectedLectureIds] = useState<number[]>(
    preselectedLectureId ? [parseInt(preselectedLectureId)] : []
  );
  const [sessionTitle, setSessionTitle] = useState("");
  const [currentSession, setCurrentSession] = useState<Session | null>(null);
  const [activeSessions, setActiveSessions] = useState<Session[]>([]);
  const [participants, setParticipants] = useState<Participant[]>([]);
  const [loading, setLoading] = useState(false);
  const [initialLoading, setInitialLoading] = useState(true);
  const [copied, setCopied] = useState(false);

  // Load data
  useEffect(() => {
    loadInitialData();
  }, []);

  const loadInitialData = async () => {
    setInitialLoading(true);
    try {
      await Promise.all([loadLectures(), loadActiveSessions()]);
    } finally {
      setInitialLoading(false);
    }
  };

  const loadActiveSessions = async () => {
    try {
      const sessions = await apiService.getInstructorActiveSessions();
      setActiveSessions(sessions);
      // 활성 세션이 하나만 있으면 자동으로 선택
      if (sessions.length === 1 && !currentSession) {
        setCurrentSession(sessions[0]);
      }
    } catch (error) {
      console.error("Failed to load active sessions:", error);
    }
  };

  // Setup WebSocket for real-time participant updates
  useEffect(() => {
    const sessionCode = currentSession?.code || currentSession?.session_code;
    if (currentSession && sessionCode && currentSession.status !== "ENDED") {
      console.log('[SessionControl] Setting up WebSocket for session:', sessionCode);

      // Connect to WebSocket
      wsClient.connect(sessionCode);

      // Subscribe to WebSocket messages
      const unsubscribe = wsClient.subscribe(handleWebSocketMessage);

      // Load initial participants
      loadParticipants();

      return () => {
        unsubscribe();
        wsClient.disconnect();
      };
    }
  }, [currentSession]);

  // Handle incoming WebSocket messages
  const handleWebSocketMessage = useCallback((message: IncomingMessage) => {
    console.log('[SessionControl] Received WebSocket message:', message);

    switch (message.type) {
      case 'participant_joined':
        // Reload participants list when someone joins
        toast.success(`${message.data.username}님이 입장했습니다`);
        loadParticipants();
        break;

      case 'participant_left':
        // Reload participants list when someone leaves
        toast.info(`${message.data.username}님이 퇴장했습니다`);
        loadParticipants();
        break;

      case 'session_status_changed':
        // Update session status
        if (currentSession) {
          setCurrentSession({
            ...currentSession,
            status: message.data.status.toUpperCase() as 'CREATED' | 'ACTIVE' | 'PAUSED' | 'ENDED',
          });
        }
        break;

      default:
        // Ignore other message types for Session Control Page
        break;
    }
  }, [currentSession]);

  const loadLectures = async () => {
    try {
      const data = await apiService.getLectures();
      setLectures(data);
    } catch (error) {
      toast.error("강의 목록을 불러오는데 실패했습니다");
    }
  };

  const loadParticipants = useCallback(async () => {
    if (!currentSession) return;

    try {
      const data = await apiService.getSessionParticipants(currentSession.id);
      setParticipants(data);
    } catch (error) {
      console.error("Failed to load participants:", error);
    }
  }, [currentSession]);

  // Generic session action handler
  const executeSessionAction = useCallback(async (
    action: SessionAction,
    config: SessionActionConfig,
    ...args: any[]
  ) => {
    if (!currentSession) return;

    if (config.confirmMessage && !confirm(config.confirmMessage)) {
      return;
    }

    setLoading(true);
    try {
      const updated = await config.apiCall(currentSession.id, ...args);
      setCurrentSession(updated);
      
      const message = typeof config.successMessage === 'function' 
        ? config.successMessage(...args)
        : config.successMessage;
      toast.success(message);

      // Special handling for end action
      if (action === 'end') {
        setTimeout(() => {
          setCurrentSession(null);
          setParticipants([]);
          setSelectedLectureIds([]);
        }, 2000);
      }
    } catch (error) {
      const actionLabels: Record<SessionAction, string> = {
        start: '시작',
        pause: '일시정지',
        resume: '재개',
        end: '종료',
        nextStep: '다음 단계 진행',
        switchLecture: '강의 전환',
      };
      toast.error(`수업 ${actionLabels[action]}에 실패했습니다`);
    } finally {
      setLoading(false);
    }
  }, [currentSession]);

  // Session actions
  const handleStartSession = async () => {
    if (!currentSession) return;

    setLoading(true);
    try {
      const updated = await apiService.startSession(currentSession.id);
      setCurrentSession(updated);
      toast.success("수업이 시작되었습니다. 실시간 화면으로 이동합니다...");

      // 실시간 화면으로 자동 이동
      setTimeout(() => {
        navigate(`/live-session/${currentSession.id}`);
      }, 1000);
    } catch (error: any) {
      // 이미 시작된 세션인 경우 처리
      if (error.response?.status === 400) {
        toast.info("이미 진행 중인 수업입니다. 실시간 화면으로 이동합니다...");
        setTimeout(() => {
          navigate(`/live-session/${currentSession.id}`);
        }, 1000);
      } else {
        toast.error("수업 시작에 실패했습니다");
      }
    } finally {
      setLoading(false);
    }
  };

  const handlePauseSession = () => executeSessionAction('pause', {
    apiCall: apiService.pauseSession,
    successMessage: "수업이 일시정지되었습니다",
  });

  const handleResumeSession = () => executeSessionAction('resume', {
    apiCall: apiService.resumeSession,
    successMessage: "수업이 재개되었습니다",
  });

  const handleEndSession = () => executeSessionAction('end', {
    apiCall: apiService.endSession,
    successMessage: "수업이 종료되었습니다",
    confirmMessage: "정말로 수업을 종료하시겠습니까?",
  });

  const handleNextStep = () => executeSessionAction('nextStep', {
    apiCall: apiService.nextStep,
    successMessage: "다음 단계로 진행되었습니다",
  });

  const handleSwitchLecture = (lectureId: number) => executeSessionAction('switchLecture', {
    apiCall: apiService.switchLecture,
    successMessage: (lectureId: number) => {
      const lectureName = lectures.find((l) => l.id === lectureId)?.title || "강의";
      return `${lectureName}(으)로 전환되었습니다`;
    },
  }, lectureId);

  // Lecture selection handlers
  const handleLectureToggle = (lectureId: number) => {
    setSelectedLectureIds((prev) =>
      prev.includes(lectureId)
        ? prev.filter((id) => id !== lectureId)
        : [...prev, lectureId]
    );
  };

  const handleRemoveLecture = (lectureId: number) => {
    setSelectedLectureIds((prev) => prev.filter((id) => id !== lectureId));
  };

  const handleCreateSession = async () => {
    if (selectedLectureIds.length === 0) {
      toast.error("최소 1개 이상의 강의를 선택해주세요");
      return;
    }

    if (!sessionTitle.trim()) {
      toast.error("수업 제목을 입력해주세요");
      return;
    }

    setLoading(true);
    try {
      const session = await apiService.createSession(selectedLectureIds, sessionTitle);
      setCurrentSession(session);
      setSessionTitle("");
      toast.success("수업이 생성되었습니다");
      loadParticipants();
    } catch (error) {
      toast.error("수업 생성에 실패했습니다");
    } finally {
      setLoading(false);
    }
  };

  const handleCopyCode = () => {
    if (currentSession) {
      navigator.clipboard.writeText(currentSession.code);
      setCopied(true);
      toast.success("수업 코드가 복사되었습니다");
      setTimeout(() => setCopied(false), 2000);
    }
  };

  // Helper functions
  const getStatusBadge = (status: string) => {
    const config = STATUS_CONFIG[status as keyof typeof STATUS_CONFIG] || STATUS_CONFIG.CREATED;
    return (
      <Badge style={{ backgroundColor: config.bgColor, color: config.textColor }}>
        {config.label}
      </Badge>
    );
  };

  const getActiveLecture = () => {
    return currentSession?.lectures.find((l) => l.isActive) || null;
  };

  const getSelectedLectures = () => {
    return selectedLectureIds
      .map((id) => lectures.find((l) => l.id === id))
      .filter((l): l is Lecture => l !== undefined);
  };

  // 세션 선택 핸들러
  const handleSelectActiveSession = (session: Session) => {
    setCurrentSession(session);
  };

  // 활성 세션 리스트에서 현재 세션 제거 (종료 후)
  const handleSessionEnded = () => {
    setActiveSessions(prev => prev.filter(s => s.id !== currentSession?.id));
  };

  // 종료 핸들러 수정
  const handleEndSessionWithCleanup = async () => {
    if (!currentSession) return;

    if (!confirm("정말로 수업을 종료하시겠습니까?")) {
      return;
    }

    setLoading(true);
    try {
      await apiService.endSession(currentSession.id);
      toast.success("수업이 종료되었습니다");
      handleSessionEnded();
      setTimeout(() => {
        setCurrentSession(null);
        setParticipants([]);
        setSelectedLectureIds([]);
      }, 1000);
    } catch (error) {
      toast.error("수업 종료에 실패했습니다");
    } finally {
      setLoading(false);
    }
  };

  if (initialLoading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <RefreshCw className="w-8 h-8 animate-spin text-gray-400" />
      </div>
    );
  }

  return (
    <div className="space-y-6 max-w-6xl">
      <div>
        <h1 className="text-3xl mb-2">수업 시작</h1>
        <p className="text-gray-600">학습 수업을 생성하고 관리하세요</p>
      </div>

      {/* Active Sessions Alert */}
      {!currentSession && activeSessions.length > 0 && (
        <Card className="border-2 border-yellow-400 bg-yellow-50">
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-yellow-700">
              <Play className="w-5 h-5" />
              진행 중인 수업이 있습니다
            </CardTitle>
            <CardDescription className="text-yellow-600">
              아래 수업을 선택하여 계속 진행하거나, 종료 후 새 수업을 시작할 수 있습니다.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-3">
            {activeSessions.map((session) => (
              <div
                key={session.id}
                className="flex items-center justify-between p-4 bg-white rounded-lg border"
              >
                <div className="flex items-center gap-4">
                  <div>
                    <p className="font-medium">{session.title}</p>
                    <p className="text-sm text-gray-500">
                      코드: <span className="font-mono font-bold">{session.code}</span>
                      {session.participantCount !== undefined && (
                        <span className="ml-2">• 참가자 {session.participantCount}명</span>
                      )}
                    </p>
                  </div>
                  {getStatusBadge(session.status)}
                </div>
                <div className="flex gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handleSelectActiveSession(session)}
                    className="gap-2"
                  >
                    <Monitor className="w-4 h-4" />
                    관리하기
                  </Button>
                </div>
              </div>
            ))}
          </CardContent>
        </Card>
      )}

      {/* Session Creation */}
      {!currentSession && (
        <SessionCreationCard
          sessionTitle={sessionTitle}
          setSessionTitle={setSessionTitle}
          lectures={lectures}
          selectedLectureIds={selectedLectureIds}
          onLectureToggle={handleLectureToggle}
          onRemoveLecture={handleRemoveLecture}
          onCreateSession={handleCreateSession}
          loading={loading}
        />
      )}

      {/* Active Session */}
      {currentSession && (
        <>
          <ActiveSessionCard
            session={currentSession}
            lectures={lectures}
            onCopyCode={handleCopyCode}
            copied={copied}
            onStartSession={handleStartSession}
            onPauseSession={handlePauseSession}
            onResumeSession={handleResumeSession}
            onEndSession={handleEndSessionWithCleanup}
            onNextStep={handleNextStep}
            onSwitchLecture={handleSwitchLecture}
            navigate={navigate}
            loading={loading}
          />

          <ParticipantsCard
            participants={participants}
            onRefresh={loadParticipants}
          />
        </>
      )}
    </div>
  );
}

// Sub-components
interface SessionCreationCardProps {
  sessionTitle: string;
  setSessionTitle: (title: string) => void;
  lectures: Lecture[];
  selectedLectureIds: number[];
  onLectureToggle: (lectureId: number) => void;
  onRemoveLecture: (lectureId: number) => void;
  onCreateSession: () => void;
  loading: boolean;
}

function SessionCreationCard({
  sessionTitle,
  setSessionTitle,
  lectures,
  selectedLectureIds,
  onLectureToggle,
  onRemoveLecture,
  onCreateSession,
  loading,
}: SessionCreationCardProps) {
  const selectedLectures = selectedLectureIds
    .map((id) => lectures.find((l) => l.id === id))
    .filter((l): l is Lecture => l !== undefined);

  return (
    <Card>
      <CardHeader>
        <CardTitle>새 수업 생성</CardTitle>
        <CardDescription>
          여러 개의 강의를 선택하여 하나의 수업으로 진행할 수 있습니다
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        <div className="space-y-2">
          <Label htmlFor="title">
            수업 제목 <span style={{ color: "var(--error)" }}>*</span>
          </Label>
          <Input
            id="title"
            placeholder="예: 2024-11-16 오전반"
            value={sessionTitle}
            onChange={(e) => setSessionTitle(e.target.value)}
          />
        </div>

        <div className="space-y-3">
          <Label>
            강의 선택 <span style={{ color: "var(--error)" }}>*</span>
          </Label>
          <p className="text-sm" style={{ color: "var(--text-secondary)" }}>
            수업에 포함할 강의를 선택하세요 (순서대로 진행됩니다)
          </p>

          {/* Selected Lectures */}
          {selectedLectures.length > 0 && (
            <div className="space-y-2 p-4 rounded-lg" style={{ backgroundColor: "var(--muted)" }}>
              <div className="flex items-center justify-between mb-2">
                <span className="text-sm">선택된 강의 ({selectedLectures.length}개)</span>
              </div>
              {selectedLectures.map((lecture, index) => (
                <div
                  key={lecture.id}
                  className="flex items-center justify-between p-3 bg-white rounded-lg border"
                >
                  <div className="flex items-center gap-3">
                    <Badge variant="outline">{index + 1}</Badge>
                    <div>
                      <p>{lecture.title}</p>
                      <p className="text-sm" style={{ color: "var(--text-secondary)" }}>
                        {lecture.description}
                      </p>
                    </div>
                  </div>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => onRemoveLecture(lecture.id)}
                    className="gap-2 text-red-600"
                  >
                    <X className="w-4 h-4" />
                    제거
                  </Button>
                </div>
              ))}
            </div>
          )}

          {/* Available Lectures */}
          <div className="border rounded-lg p-4">
            <p className="text-sm mb-3">사용 가능한 강의</p>
            <div className="space-y-2 max-h-80 overflow-y-auto">
              {lectures.map((lecture) => {
                const isSelected = selectedLectureIds.includes(lecture.id);
                return (
                  <div
                    key={lecture.id}
                    className={`flex items-center gap-3 p-3 rounded-lg border transition-colors ${
                      isSelected ? "border-2" : "hover:bg-gray-50"
                    }`}
                    style={{ borderColor: isSelected ? "var(--primary)" : undefined }}
                  >
                    <Checkbox
                      id={`lecture-${lecture.id}`}
                      checked={isSelected}
                      onCheckedChange={() => onLectureToggle(lecture.id)}
                    />
                    <label htmlFor={`lecture-${lecture.id}`} className="flex-1 cursor-pointer">
                      <p>{lecture.title}</p>
                      <p className="text-sm" style={{ color: "var(--text-secondary)" }}>
                        {lecture.description}
                      </p>
                    </label>
                  </div>
                );
              })}
            </div>
          </div>
        </div>

        <Button
          onClick={onCreateSession}
          disabled={loading || selectedLectureIds.length === 0}
          className="w-full gap-2"
          style={{ backgroundColor: "var(--primary)" }}
        >
          <Plus className="w-4 h-4" />
          {loading ? "생성 중..." : "수업 생성"}
        </Button>
      </CardContent>
    </Card>
  );
}

interface ActiveSessionCardProps {
  session: Session;
  lectures: Lecture[];
  onCopyCode: () => void;
  copied: boolean;
  onStartSession: () => void;
  onPauseSession: () => void;
  onResumeSession: () => void;
  onEndSession: () => void;
  onNextStep: () => void;
  onSwitchLecture: (lectureId: number) => void;
  navigate: (path: string) => void;
  loading: boolean;
}

function ActiveSessionCard({
  session,
  lectures,
  onCopyCode,
  copied,
  onStartSession,
  onPauseSession,
  onResumeSession,
  onEndSession,
  onNextStep,
  onSwitchLecture,
  navigate,
  loading,
}: ActiveSessionCardProps) {
  const getStatusBadge = (status: string) => {
    const config = STATUS_CONFIG[status as keyof typeof STATUS_CONFIG] || STATUS_CONFIG.CREATED;
    return (
      <Badge style={{ backgroundColor: config.bgColor, color: config.textColor }}>
        {config.label}
      </Badge>
    );
  };

  const activeLecture = session.lectures.find((l) => l.isActive);

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <CardTitle>{session.title}</CardTitle>
          {getStatusBadge(session.status)}
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        {/* Session Code and QR Code */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Session Code */}
          <div className="flex flex-col justify-between p-6 bg-blue-50 rounded-lg">
            <div>
              <p className="text-sm text-gray-600 mb-1">수업 코드</p>
              <p className="text-4xl font-bold tracking-wider mb-4">{session.code}</p>
              <p className="text-sm text-gray-500">
                학생들이 앱에서 이 코드를 입력하면 수업에 참가할 수 있습니다
              </p>
            </div>
            <Button variant="outline" size="lg" onClick={onCopyCode} className="gap-2 mt-4">
              {copied ? (
                <>
                  <CheckCircle2 className="w-5 h-5 text-green-600" />
                  복사됨
                </>
              ) : (
                <>
                  <Copy className="w-5 h-5" />
                  코드 복사
                </>
              )}
            </Button>
          </div>

          {/* QR Code */}
          <div className="flex flex-col items-center justify-center p-6 bg-white rounded-lg border-2 border-dashed border-gray-300">
            <div className="mb-3 session-qr-code">
              <QRCodeSVG
                value={`mobilegpt://join/${session.code}`}
                size={180}
                level="M"
                includeMargin={true}
              />
            </div>
            <p className="text-sm text-gray-600 text-center mb-3">
              <QrCode className="w-4 h-4 inline mr-1" />
              QR 코드를 스캔하여 참가
            </p>
            <Button
              variant="outline"
              size="sm"
              onClick={() => {
                const svg = document.querySelector('.session-qr-code svg');
                if (svg) {
                  const svgData = new XMLSerializer().serializeToString(svg);
                  const canvas = document.createElement('canvas');
                  const ctx = canvas.getContext('2d');
                  const img = new Image();
                  img.onload = () => {
                    canvas.width = img.width;
                    canvas.height = img.height;
                    ctx?.drawImage(img, 0, 0);
                    const pngFile = canvas.toDataURL('image/png');
                    const downloadLink = document.createElement('a');
                    downloadLink.download = `session-${session.code}-qr.png`;
                    downloadLink.href = pngFile;
                    downloadLink.click();
                    toast.success('QR 코드가 다운로드되었습니다');
                  };
                  img.src = 'data:image/svg+xml;base64,' + btoa(svgData);
                }
              }}
              className="gap-2"
            >
              <Download className="w-4 h-4" />
              QR 다운로드
            </Button>
          </div>
        </div>

        <Alert>
          <AlertDescription>
            학생들은 <strong>QR 코드 스캔</strong> 또는 <strong>코드 입력</strong> 두 가지 방법으로 수업에 참가할 수 있습니다.
            앱 미설치 시 자동으로 앱 설치 페이지로 안내됩니다.
          </AlertDescription>
        </Alert>

        {/* Lecture Progression */}
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <Label>수업 진행 강의 ({session.lectures.length}개)</Label>
            {session.status === "ACTIVE" && activeLecture && (
              <Badge style={{ backgroundColor: "var(--success)", color: "white" }}>
                {activeLecture.lectureName}
              </Badge>
            )}
          </div>
          <div className="space-y-2">
            {session.lectures.map((lecture, index) => (
              <LectureProgressItem
                key={lecture.id}
                lecture={lecture}
                index={index}
                sessionStatus={session.status}
                onSwitch={onSwitchLecture}
                loading={loading}
              />
            ))}
          </div>
        </div>

        {/* Live Session Link */}
        {session.status !== "ENDED" && (
          <Alert style={{ backgroundColor: "#E8F5E9", borderLeft: "4px solid var(--success)" }}>
            <AlertDescription className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Monitor className="w-5 h-5" style={{ color: "var(--success)" }} />
                <span>실시간 화면에서 학생들을 모니터링하세요</span>
              </div>
              <Button
                variant="outline"
                size="sm"
                onClick={() => navigate(`/live-session/${session.id}`)}
                className="gap-2"
                style={{ borderColor: "var(--success)", color: "var(--success)" }}
              >
                <Monitor className="w-4 h-4" />
                실시간 화면 진입
              </Button>
            </AlertDescription>
          </Alert>
        )}

        {/* Control Buttons */}
        <SessionControlButtons
          status={session.status}
          onStart={onStartSession}
          onPause={onPauseSession}
          onResume={onResumeSession}
          onEnd={onEndSession}
          onNextStep={onNextStep}
          loading={loading}
        />
      </CardContent>
    </Card>
  );
}

interface LectureProgressItemProps {
  lecture: any;
  index: number;
  sessionStatus: string;
  onSwitch: (lectureId: number) => void;
  loading: boolean;
}

function LectureProgressItem({ lecture, index, sessionStatus, onSwitch, loading }: LectureProgressItemProps) {
  const getBorderColor = () => {
    if (lecture.isActive) return "var(--primary)";
    if (lecture.completedAt) return "var(--success)";
    return "#e0e0e0";
  };

  const getBgColor = () => {
    if (lecture.isActive) return "bg-blue-50";
    if (lecture.completedAt) return "bg-green-50";
    return "";
  };

  return (
    <div
      className={`flex items-center justify-between p-4 rounded-lg border-2 ${getBgColor()}`}
      style={{ borderColor: getBorderColor() }}
    >
      <div className="flex items-center gap-3 flex-1">
        <Badge variant="outline">{index + 1}</Badge>
        <div className="flex items-center gap-2">
          <BookOpen className="w-5 h-5" style={{ color: "var(--text-secondary)" }} />
          <span>{lecture.lectureName}</span>
        </div>
      </div>
      <div className="flex items-center gap-2">
        {lecture.completedAt && (
          <Badge style={{ backgroundColor: "var(--success)", color: "white" }}>완료</Badge>
        )}
        {lecture.isActive && (
          <Badge style={{ backgroundColor: "var(--primary)", color: "white" }}>진행 중</Badge>
        )}
        {!lecture.isActive && !lecture.completedAt && sessionStatus === "ACTIVE" && (
          <Button
            variant="outline"
            size="sm"
            onClick={() => onSwitch(lecture.lectureId)}
            disabled={loading}
            className="gap-2"
          >
            <ChevronRight className="w-4 h-4" />
            전환
          </Button>
        )}
      </div>
    </div>
  );
}

interface SessionControlButtonsProps {
  status: string;
  onStart: () => void;
  onPause: () => void;
  onResume: () => void;
  onEnd: () => void;
  onNextStep: () => void;
  loading: boolean;
}

function SessionControlButtons({
  status,
  onStart,
  onPause,
  onResume,
  onEnd,
  onNextStep,
  loading,
}: SessionControlButtonsProps) {
  return (
    <div className="flex flex-wrap gap-2">
      {status === "CREATED" && (
        <Button onClick={onStart} disabled={loading} className="gap-2">
          <Play className="w-4 h-4" />
          시작
        </Button>
      )}

      {status === "ACTIVE" && (
        <>
          <Button onClick={onPause} disabled={loading} variant="outline" className="gap-2">
            <Pause className="w-4 h-4" />
            일시정지
          </Button>
          <Button onClick={onNextStep} disabled={loading} className="gap-2">
            <SkipForward className="w-4 h-4" />
            다음 단계
          </Button>
        </>
      )}

      {status === "PAUSED" && (
        <Button onClick={onResume} disabled={loading} className="gap-2">
          <Play className="w-4 h-4" />
          재개
        </Button>
      )}

      {status !== "ENDED" && (
        <Button onClick={onEnd} disabled={loading} variant="destructive" className="gap-2 ml-auto">
          <Square className="w-4 h-4" />
          종료
        </Button>
      )}
    </div>
  );
}

interface ParticipantsCardProps {
  participants: Participant[];
  onRefresh: () => void;
}

function ParticipantsCard({ participants, onRefresh }: ParticipantsCardProps) {
  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <CardTitle className="flex items-center gap-2">
            <Users className="w-5 h-5" />
            참가 학생 ({participants.length}명)
          </CardTitle>
          <Button variant="outline" size="sm" onClick={onRefresh} className="gap-2">
            <RefreshCw className="w-4 h-4" />
            새로고침
          </Button>
        </div>
      </CardHeader>
      <CardContent>
        {participants.length === 0 ? (
          <div className="text-center py-8 text-gray-500">
            <Users className="w-12 h-12 mx-auto mb-2 opacity-50" />
            <p>아직 참가한 학생이 없습니다</p>
            <p className="text-sm mt-1">학생들이 수업 코드를 입력하면 여기에 표시됩니다</p>
          </div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>이름</TableHead>
                <TableHead>이메일</TableHead>
                <TableHead>참가 시간</TableHead>
                <TableHead>상태</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {participants.map((participant) => (
                <TableRow key={participant.id}>
                  <TableCell>{participant.name}</TableCell>
                  <TableCell>{participant.email}</TableCell>
                  <TableCell>
                    {new Date(participant.joinedAt).toLocaleString("ko-KR", {
                      hour: "2-digit",
                      minute: "2-digit",
                    })}
                  </TableCell>
                  <TableCell>
                    <Badge className={participant.isActive ? "bg-green-600" : "bg-gray-400"}>
                      {participant.isActive ? "활성" : "비활성"}
                    </Badge>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </CardContent>
    </Card>
  );
}
