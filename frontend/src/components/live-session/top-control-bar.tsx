import { useEffect, useState } from 'react';
import { Button } from '../ui/button';
import { Badge } from '../ui/badge';
import { Play, Pause, SkipForward, Square, ChevronDown, BookOpen, CheckCircle2, Users, AlertCircle, Bell, Camera } from 'lucide-react';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
  DropdownMenuSeparator,
} from '../ui/dropdown-menu';
import { SessionLecture } from '../../lib/types';
import { Progress } from '../ui/progress';

// Constants
const STATUS_STYLES = {
  CREATED: { bg: 'var(--muted)', text: 'var(--text-secondary)', label: '생성됨' },
  ACTIVE: { bg: 'var(--success)', text: 'white', label: '진행 중' },
  IN_PROGRESS: { bg: 'var(--success)', text: 'white', label: '진행 중' },
  PAUSED: { bg: 'var(--warning)', text: 'white', label: '일시정지' },
  ENDED: { bg: 'var(--status-inactive)', text: 'white', label: '종료됨' },
  REVIEW_MODE: { bg: 'var(--info)', text: 'white', label: '복습 모드' },
} as const;

const CONTROL_BAR_HEIGHT = '64px';
const STATUS_BAR_HEIGHT = '56px';

type SessionStatus = 'CREATED' | 'ACTIVE' | 'IN_PROGRESS' | 'PAUSED' | 'ENDED' | 'REVIEW_MODE';

interface TopControlBarProps {
  sessionTitle: string;
  sessionCode: string;
  sessionStatus: SessionStatus;
  startedAt?: string;
  lectures?: SessionLecture[];
  activeLectureId?: number;
  currentStep?: number;
  totalSteps?: number;
  activeStudents?: number;
  totalStudents?: number;
  helpRequestCount?: number;
  onStart: () => void;
  onPause: () => void;
  onResume: () => void;
  onNextStep: () => void;
  onEnd: () => void;
  onSwitchLecture?: (lectureId: number) => void;
  onBroadcastMessage?: () => void;
  onTakeSnapshot?: () => void;
  onLogout: () => void;
}

export function TopControlBar(props: TopControlBarProps) {
  const {
    sessionStatus,
    startedAt,
    lectures = [],
    currentStep = 0,
    totalSteps = 10,
    activeStudents = 0,
    totalStudents = 0,
    helpRequestCount = 0,
  } = props;

  const [elapsedTime, setElapsedTime] = useState(0);

  useEffect(() => {
    if ((sessionStatus === 'ACTIVE' || sessionStatus === 'IN_PROGRESS') && startedAt) {
      const interval = setInterval(() => {
        const seconds = Math.floor((Date.now() - new Date(startedAt).getTime()) / 1000);
        setElapsedTime(seconds);
      }, 1000);

      return () => clearInterval(interval);
    }
  }, [sessionStatus, startedAt]);

  const showStatusBar = sessionStatus === 'ACTIVE' || sessionStatus === 'IN_PROGRESS' || sessionStatus === 'PAUSED' || sessionStatus === 'REVIEW_MODE';

  return (
    <>
      <MainControlBar {...props} elapsedTime={elapsedTime} lectures={lectures} />
      {showStatusBar && (
        <StatusBar
          currentStep={currentStep}
          totalSteps={totalSteps}
          activeStudents={activeStudents}
          totalStudents={totalStudents}
          helpRequestCount={helpRequestCount}
          onBroadcastMessage={props.onBroadcastMessage}
          onTakeSnapshot={props.onTakeSnapshot}
        />
      )}
    </>
  );
}

// Sub-components
interface MainControlBarProps extends TopControlBarProps {
  elapsedTime: number;
  lectures: SessionLecture[];
}

function MainControlBar({
  sessionTitle,
  sessionCode,
  sessionStatus,
  lectures,
  onStart,
  onPause,
  onResume,
  onNextStep,
  onEnd,
  onSwitchLecture,
  onLogout,
  elapsedTime,
}: MainControlBarProps) {
  return (
    <div 
      className="fixed top-0 left-0 right-0 z-30 flex items-center justify-between px-4 py-3"
      style={{ 
        backgroundColor: 'var(--primary)',
        color: 'white',
        height: CONTROL_BAR_HEIGHT,
      }}
    >
      <LeftSection
        sessionTitle={sessionTitle}
        sessionCode={sessionCode}
        sessionStatus={sessionStatus}
        lectures={lectures}
        onSwitchLecture={onSwitchLecture}
      />
      
      <CenterSection
        sessionStatus={sessionStatus}
        onStart={onStart}
        onPause={onPause}
        onResume={onResume}
        onNextStep={onNextStep}
        onEnd={onEnd}
      />
      
      <RightSection
        sessionStatus={sessionStatus}
        elapsedTime={elapsedTime}
        onLogout={onLogout}
      />
    </div>
  );
}

interface LeftSectionProps {
  sessionTitle: string;
  sessionCode: string;
  sessionStatus: SessionStatus;
  lectures: SessionLecture[];
  onSwitchLecture?: (lectureId: number) => void;
}

function LeftSection({ sessionTitle, sessionCode, sessionStatus, lectures, onSwitchLecture }: LeftSectionProps) {
  return (
    <div className="flex items-center gap-4">
      <Logo />
      <Divider />
      <SessionInfo sessionTitle={sessionTitle} sessionCode={sessionCode} sessionStatus={sessionStatus} />
      {lectures.length > 0 && (
        <>
          <Divider />
          <LectureSelector
            lectures={lectures}
            sessionStatus={sessionStatus}
            onSwitchLecture={onSwitchLecture}
          />
        </>
      )}
    </div>
  );
}

function Logo() {
  return (
    <div className="flex items-center gap-2">
      <div className="w-8 h-8 rounded-full flex items-center justify-center" style={{ backgroundColor: 'white' }}>
        <span>📱</span>
      </div>
      <span style={{ fontWeight: 'var(--font-weight-semibold)' }}>MobileGPT</span>
    </div>
  );
}

function Divider() {
  return <div className="h-6 w-px" style={{ backgroundColor: 'rgba(255,255,255,0.3)' }} />;
}

interface SessionInfoProps {
  sessionTitle: string;
  sessionCode: string;
  sessionStatus: SessionStatus;
}

function SessionInfo({ sessionTitle, sessionCode, sessionStatus }: SessionInfoProps) {
  const style = STATUS_STYLES[sessionStatus];
  
  return (
    <div className="flex items-center gap-2">
      <span>수업: {sessionTitle}</span>
      <span style={{ opacity: 0.8 }}>
        (코드: <span style={{ fontFamily: 'monospace' }}>{sessionCode}</span>)
      </span>
      <Badge style={{ backgroundColor: style.bg, color: style.text }}>
        {style.label}
      </Badge>
    </div>
  );
}

interface LectureSelectorProps {
  lectures: SessionLecture[];
  sessionStatus: SessionStatus;
  onSwitchLecture?: (lectureId: number) => void;
}

function LectureSelector({ lectures, sessionStatus, onSwitchLecture }: LectureSelectorProps) {
  const activeLecture = lectures.find(l => l.isActive);
  const isDisabled = sessionStatus === 'ENDED' || sessionStatus === 'REVIEW_MODE';
  const hasMultipleLectures = lectures.length > 1;

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <button
          className="flex items-center gap-2 h-10 px-3 rounded-md transition-colors text-white hover:bg-white/10 disabled:opacity-50 disabled:cursor-not-allowed"
          disabled={isDisabled}
        >
          <BookOpen className="w-4 h-4" />
          <span>{activeLecture?.lectureName || '강의 선택'}</span>
          {hasMultipleLectures && <ChevronDown className="w-4 h-4" />}
        </button>
      </DropdownMenuTrigger>
      {hasMultipleLectures && (
        <DropdownMenuContent align="start" className="w-64">
          <div className="px-2 py-1.5 text-sm font-semibold" style={{ color: 'var(--text-secondary)' }}>
            수업 강의 목록 ({lectures.length}개)
          </div>
          <DropdownMenuSeparator />
          {lectures.map((lecture, index) => (
            <LectureMenuItem
              key={lecture.id}
              lecture={lecture}
              index={index}
              sessionStatus={sessionStatus}
              onSwitchLecture={onSwitchLecture}
            />
          ))}
        </DropdownMenuContent>
      )}
    </DropdownMenu>
  );
}

interface LectureMenuItemProps {
  lecture: SessionLecture;
  index: number;
  sessionStatus: SessionStatus;
  onSwitchLecture?: (lectureId: number) => void;
}

function LectureMenuItem({ lecture, index, sessionStatus, onSwitchLecture }: LectureMenuItemProps) {
  const isActive = sessionStatus === 'ACTIVE' || sessionStatus === 'IN_PROGRESS';

  const handleClick = () => {
    if (!lecture.isActive && isActive && onSwitchLecture) {
      onSwitchLecture(lecture.lectureId);
    }
  };

  return (
    <DropdownMenuItem
      onClick={handleClick}
      disabled={lecture.isActive || !isActive}
      className={`flex items-center gap-2 ${lecture.isActive ? 'bg-blue-50' : ''}`}
    >
      <Badge variant="outline" className="min-w-6 justify-center">
        {index + 1}
      </Badge>
      <span className="flex-1">{lecture.lectureName}</span>
      {lecture.isActive && (
        <Badge style={{ backgroundColor: 'var(--primary)', color: 'white', fontSize: '0.7rem' }}>
          진행 중
        </Badge>
      )}
      {lecture.completedAt && (
        <CheckCircle2 className="w-4 h-4" style={{ color: 'var(--success)' }} />
      )}
    </DropdownMenuItem>
  );
}

interface CenterSectionProps {
  sessionStatus: SessionStatus;
  onStart: () => void;
  onPause: () => void;
  onResume: () => void;
  onNextStep: () => void;
  onEnd: () => void;
}

function CenterSection({ sessionStatus, onStart, onPause, onResume, onNextStep, onEnd }: CenterSectionProps) {
  const isActive = sessionStatus === 'ACTIVE' || sessionStatus === 'IN_PROGRESS';

  return (
    <div className="flex items-center gap-2">
      {sessionStatus === 'CREATED' && (
        <Button
          onClick={onStart}
          size="sm"
          className="gap-2 h-10"
          style={{ backgroundColor: 'white', color: 'var(--primary)' }}
        >
          <Play className="w-4 h-4" />
          시작
        </Button>
      )}

      {isActive && (
        <>
          <OutlineButton onClick={onPause} icon={Pause} label="일시정지" hoverBg="rgba(255,255,255,0.1)" />
          <OutlineButton onClick={onNextStep} icon={SkipForward} label="다음 단계" hoverBg="rgba(255,255,255,0.1)" />
        </>
      )}

      {sessionStatus === 'PAUSED' && (
        <Button
          onClick={onResume}
          size="sm"
          className="gap-2 h-10"
          style={{ backgroundColor: 'white', color: 'var(--primary)' }}
        >
          <Play className="w-4 h-4" />
          재개
        </Button>
      )}

      {(isActive || sessionStatus === 'PAUSED') && (
        <OutlineButton onClick={onEnd} icon={Square} label="종료" hoverBg="#dc2626" />
      )}

      {sessionStatus === 'REVIEW_MODE' && (
        <Badge
          className="h-10 px-4 flex items-center"
          style={{ backgroundColor: 'rgba(255,255,255,0.2)', color: 'white' }}
        >
          수업 종료됨
        </Badge>
      )}
    </div>
  );
}

interface OutlineButtonProps {
  onClick: () => void;
  icon: React.ElementType;
  label: string;
  hoverBg: string;
}

function OutlineButton({ onClick, icon: Icon, label, hoverBg }: OutlineButtonProps) {
  return (
    <Button
      onClick={onClick}
      size="sm"
      variant="outline"
      className="gap-2 h-10 text-white border-white"
      style={{ backgroundColor: 'transparent' }}
      onMouseEnter={(e) => e.currentTarget.style.backgroundColor = hoverBg}
      onMouseLeave={(e) => e.currentTarget.style.backgroundColor = 'transparent'}
    >
      <Icon className="w-4 h-4" />
      {label}
    </Button>
  );
}

interface RightSectionProps {
  sessionStatus: SessionStatus;
  elapsedTime: number;
  onLogout: () => void;
}

function RightSection({ sessionStatus, elapsedTime, onLogout }: RightSectionProps) {
  const showTimer = sessionStatus === 'ACTIVE' || sessionStatus === 'IN_PROGRESS' || sessionStatus === 'PAUSED' || sessionStatus === 'REVIEW_MODE';

  return (
    <div className="flex items-center gap-4">
      {showTimer && <Timer elapsedTime={elapsedTime} />}
      <UserMenu onLogout={onLogout} />
    </div>
  );
}

function Timer({ elapsedTime }: { elapsedTime: number }) {
  const formatTime = (seconds: number): string => {
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;
    return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
  };

  return (
    <div className="flex items-center gap-2 text-lg" style={{ fontFamily: 'monospace' }}>
      <span>⏱</span>
      <span>{formatTime(elapsedTime)}</span>
    </div>
  );
}

function UserMenu({ onLogout }: { onLogout: () => void }) {
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <button className="flex items-center gap-2 h-10 px-2 rounded-md transition-colors text-white hover:bg-white/10">
          <div className="w-8 h-8 rounded-full flex items-center justify-center" style={{ backgroundColor: 'rgba(255,255,255,0.2)' }}>
            👤
          </div>
          <ChevronDown className="w-4 h-4" />
        </button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end">
        <DropdownMenuItem>프로필</DropdownMenuItem>
        <DropdownMenuItem>설정</DropdownMenuItem>
        <DropdownMenuItem onClick={onLogout} className="text-red-600">
          로그아웃
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

interface StatusBarProps {
  currentStep: number;
  totalSteps: number;
  activeStudents: number;
  totalStudents: number;
  helpRequestCount: number;
  onBroadcastMessage?: () => void;
  onTakeSnapshot?: () => void;
}

function StatusBar({
  currentStep,
  totalSteps,
  activeStudents,
  totalStudents,
  helpRequestCount,
  onBroadcastMessage,
  onTakeSnapshot,
}: StatusBarProps) {
  return (
    <div
      className="fixed top-16 left-0 right-0 z-20 px-4 py-2"
      style={{
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        borderBottom: '1px solid var(--border)',
        backdropFilter: 'blur(8px)',
      }}
    >
      <div className="flex items-center justify-between max-w-full">
        <ProgressSection
          currentStep={currentStep}
          totalSteps={totalSteps}
          activeStudents={activeStudents}
          totalStudents={totalStudents}
          helpRequestCount={helpRequestCount}
        />
        <QuickActions
          onBroadcastMessage={onBroadcastMessage}
          onTakeSnapshot={onTakeSnapshot}
        />
      </div>
    </div>
  );
}

interface ProgressSectionProps {
  currentStep: number;
  totalSteps: number;
  activeStudents: number;
  totalStudents: number;
  helpRequestCount: number;
}

function ProgressSection({ currentStep, totalSteps, activeStudents, totalStudents, helpRequestCount }: ProgressSectionProps) {
  return (
    <div className="flex items-center gap-6 flex-1">
      <StepProgress currentStep={currentStep} totalSteps={totalSteps} />
      <StudentStatus activeStudents={activeStudents} totalStudents={totalStudents} />
      {helpRequestCount > 0 && <HelpRequestAlert count={helpRequestCount} />}
    </div>
  );
}

function StepProgress({ currentStep, totalSteps }: { currentStep: number; totalSteps: number }) {
  return (
    <div className="flex items-center gap-3" style={{ minWidth: '200px' }}>
      <div className="text-sm">
        <span style={{ color: 'var(--text-secondary)' }}>진행:</span>
        <span className="ml-2" style={{ fontWeight: 'var(--font-weight-semibold)' }}>
          {currentStep}/{totalSteps} 단계
        </span>
      </div>
      <div className="flex-1">
        <Progress value={(currentStep / totalSteps) * 100} className="h-2" />
      </div>
    </div>
  );
}

function StudentStatus({ activeStudents, totalStudents }: { activeStudents: number; totalStudents: number }) {
  return (
    <div className="flex items-center gap-2">
      <Users className="w-4 h-4" style={{ color: 'var(--text-secondary)' }} />
      <span className="text-sm">
        <span style={{ color: 'var(--success)', fontWeight: 'var(--font-weight-semibold)' }}>
          {activeStudents}
        </span>
        <span style={{ color: 'var(--text-secondary)' }}>/{totalStudents}명 활성</span>
      </span>
    </div>
  );
}

function HelpRequestAlert({ count }: { count: number }) {
  return (
    <div className="flex items-center gap-2 px-3 py-1 rounded-full" style={{ backgroundColor: '#FFEBEE' }}>
      <AlertCircle className="w-4 h-4" style={{ color: 'var(--error)' }} />
      <span className="text-sm" style={{ color: 'var(--error)', fontWeight: 'var(--font-weight-semibold)' }}>
        도움 요청 {count}건
      </span>
    </div>
  );
}

interface QuickActionsProps {
  onBroadcastMessage?: () => void;
  onTakeSnapshot?: () => void;
}

function QuickActions({ onBroadcastMessage, onTakeSnapshot }: QuickActionsProps) {
  return (
    <div className="flex items-center gap-2">
      {onBroadcastMessage && (
        <Button variant="outline" size="sm" onClick={onBroadcastMessage} className="gap-2">
          <Bell className="w-4 h-4" />
          전체 알림
        </Button>
      )}
      {onTakeSnapshot && (
        <Button variant="outline" size="sm" onClick={onTakeSnapshot} className="gap-2">
          <Camera className="w-4 h-4" />
          화면 캡처
        </Button>
      )}
    </div>
  );
}
