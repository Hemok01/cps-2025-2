import { useState } from 'react';
import { LiveNotification, SubtaskInfo, StudentListItem } from '../../lib/live-session-types';
import { Button } from '../ui/button';
import { Badge } from '../ui/badge';
import { ScrollArea } from '../ui/scroll-area';
import { Input } from '../ui/input';
import { Progress } from '../ui/progress';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../ui/tabs';
import { CheckCircle, HelpCircle, AlertTriangle, Info, Send, Monitor } from 'lucide-react';
import { SubtaskProgress } from './subtask-progress';

// Constants
const NOTIFICATION_STYLES = {
  help_request: {
    bgColor: '#FFEBEE',
    borderColor: 'var(--error)',
    textColor: 'var(--error-dark)',
    icon: HelpCircle,
  },
  progress_alert: {
    bgColor: '#FFF3E0',
    borderColor: 'var(--warning)',
    textColor: 'var(--warning-dark)',
    icon: AlertTriangle,
  },
  system: {
    bgColor: 'var(--accent)',
    borderColor: 'var(--info)',
    textColor: 'var(--info-dark)',
    icon: Info,
  },
  default: {
    bgColor: 'var(--muted)',
    borderColor: 'var(--border)',
    textColor: 'var(--text-primary)',
    icon: Info,
  },
} as const;

const MAX_VISIBLE_AVATARS = 4;

interface RightPanelProps {
  participants: { id: number; name: string; avatarUrl?: string; isOnline: boolean }[];
  notifications: LiveNotification[];
  onResolveNotification: (notificationId: number) => void;
  onViewScreen?: (notification: LiveNotification) => void;
  // Progress tracking props
  subtasks: SubtaskInfo[];
  currentSubtaskIndex: number;
  students: StudentListItem[];
  selectedStudentId: number | null;
  totalSubtasks: number;
}

interface ChatMessage {
  id: number;
  sender: string;
  message: string;
  timestamp: string;
}

export function RightPanel({
  participants,
  notifications,
  onResolveNotification,
  onViewScreen,
  subtasks,
  currentSubtaskIndex,
  students,
  selectedStudentId,
  totalSubtasks,
}: RightPanelProps) {
  const [chatMessage, setChatMessage] = useState('');
  const [chatMessages, setChatMessages] = useState<ChatMessage[]>([
    {
      id: 1,
      sender: 'system',
      message: '실시간 채팅이 시작되었습니다.',
      timestamp: new Date().toISOString(),
    },
  ]);

  const handleSendMessage = () => {
    if (!chatMessage.trim()) return;

    const newMessage: ChatMessage = {
      id: chatMessages.length + 1,
      sender: 'instructor',
      message: chatMessage,
      timestamp: new Date().toISOString(),
    };

    setChatMessages([...chatMessages, newMessage]);
    setChatMessage('');
  };

  return (
    <div className="h-full flex flex-col" style={{ width: '340px', backgroundColor: 'white', borderLeft: '1px solid var(--border)' }}>
      <Tabs defaultValue="notifications" className="h-full flex flex-col">
        {/* Tabs Header - Fixed */}
        <div className="p-4 pb-3 border-b" style={{ borderColor: 'var(--border)' }}>
          <TabsList className="grid w-full grid-cols-3">
            <TabsTrigger value="notifications" className="text-xs">
              알림
              {notifications.length > 0 && (
                <Badge className="ml-1 h-4 px-1 text-xs" style={{ backgroundColor: 'var(--error)', color: 'white' }}>
                  {notifications.length}
                </Badge>
              )}
            </TabsTrigger>
            <TabsTrigger value="progress" className="text-xs">
              진행현황
            </TabsTrigger>
            <TabsTrigger value="chat" className="text-xs">
              채팅
            </TabsTrigger>
          </TabsList>
        </div>

        {/* Participants Summary - Fixed */}
        <ParticipantsSummary participants={participants} />

        {/* Tab Contents - Scrollable */}
        <div className="flex-1 flex flex-col overflow-hidden">
          <NotificationsTab
            notifications={notifications}
            onResolve={onResolveNotification}
            onViewScreen={onViewScreen}
          />
          <ProgressTab
            subtasks={subtasks}
            currentSubtaskIndex={currentSubtaskIndex}
            students={students}
            selectedStudentId={selectedStudentId}
            totalSubtasks={totalSubtasks}
          />
          <ChatTab
            messages={chatMessages}
            currentMessage={chatMessage}
            onMessageChange={setChatMessage}
            onSendMessage={handleSendMessage}
          />
        </div>
      </Tabs>
    </div>
  );
}

// Sub-components
interface ParticipantsSummaryProps {
  participants: { id: number; name: string; isOnline: boolean }[];
}

function ParticipantsSummary({ participants }: ParticipantsSummaryProps) {
  const visibleParticipants = participants.slice(0, MAX_VISIBLE_AVATARS);
  const remainingCount = Math.max(0, participants.length - MAX_VISIBLE_AVATARS);

  return (
    <div className="p-4 border-b" style={{ borderColor: 'var(--border)' }}>
      <h3 className="text-sm mb-3" style={{ color: 'var(--text-secondary)' }}>참가자 현황</h3>
      <div className="flex items-center">
        {visibleParticipants.map((participant, index) => (
          <AvatarWithStatus
            key={participant.id}
            isOnline={participant.isOnline}
            marginLeft={index > 0 ? '-12px' : '0'}
            zIndex={MAX_VISIBLE_AVATARS - index}
          />
        ))}
        {remainingCount > 0 && (
          <div
            className="flex items-center justify-center rounded-full border-2 border-white text-sm"
            style={{
              width: '48px',
              height: '48px',
              marginLeft: '-12px',
              backgroundColor: 'var(--muted)',
              color: 'var(--text-secondary)',
              fontWeight: 'var(--font-weight-semibold)',
            }}
          >
            +{remainingCount}
          </div>
        )}
      </div>
    </div>
  );
}

interface AvatarWithStatusProps {
  isOnline: boolean;
  marginLeft: string;
  zIndex: number;
}

function AvatarWithStatus({ isOnline, marginLeft, zIndex }: AvatarWithStatusProps) {
  return (
    <div
      className="relative rounded-full border-2 border-white"
      style={{
        width: '48px',
        height: '48px',
        marginLeft,
        backgroundColor: 'var(--muted)',
        zIndex,
      }}
    >
      <div className="w-full h-full flex items-center justify-center text-xl">👤</div>
      {isOnline && (
        <div
          className="absolute bottom-0 right-0 w-3 h-3 rounded-full border-2 border-white"
          style={{ backgroundColor: 'var(--success)' }}
        />
      )}
    </div>
  );
}

interface NotificationsTabProps {
  notifications: LiveNotification[];
  onResolve: (id: number) => void;
  onViewScreen?: (notification: LiveNotification) => void;
}

function NotificationsTab({ notifications, onResolve, onViewScreen }: NotificationsTabProps) {
  return (
    <TabsContent value="notifications" className="flex-1 m-0 overflow-hidden">
      <ScrollArea className="h-full">
        <div className="space-y-3 p-4">
          {notifications.length === 0 ? (
            <EmptyNotifications />
          ) : (
            notifications.map((notification) => (
              <NotificationCard
                key={notification.id}
                notification={notification}
                onResolve={onResolve}
                onViewScreen={onViewScreen}
              />
            ))
          )}
        </div>
      </ScrollArea>
    </TabsContent>
  );
}

function EmptyNotifications() {
  return (
    <div className="text-center py-8" style={{ color: 'var(--text-secondary)' }}>
      <CheckCircle className="w-12 h-12 mx-auto mb-2 opacity-50" />
      <p className="text-sm">알림이 없습니다</p>
    </div>
  );
}

interface NotificationCardProps {
  notification: LiveNotification;
  onResolve: (id: number) => void;
  onViewScreen?: (notification: LiveNotification) => void;
}

function NotificationCard({ notification, onResolve, onViewScreen }: NotificationCardProps) {
  const style = NOTIFICATION_STYLES[notification.type as keyof typeof NOTIFICATION_STYLES] || NOTIFICATION_STYLES.default;
  const Icon = style.icon;
  const isHelpRequest = notification.type === 'help_request';

  return (
    <div
      className="rounded-lg p-3"
      style={{
        backgroundColor: style.bgColor,
        borderLeft: `4px solid ${style.borderColor}`,
        boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
      }}
    >
      <div className="flex items-start gap-2 mb-2">
        <Icon className="w-4 h-4 mt-0.5" style={{ color: style.textColor }} />
        <div className="flex-1 min-w-0">
          <h4 className="text-sm" style={{ fontWeight: 'var(--font-weight-semibold)', color: style.textColor }}>
            {notification.title}
          </h4>
          {notification.studentName && (
            <p className="text-xs mt-0.5" style={{ color: 'var(--text-secondary)' }}>
              {notification.studentName}
            </p>
          )}
        </div>
      </div>
      <p className="text-xs mb-2" style={{ color: 'var(--text-secondary)' }}>
        {notification.message || '도움을 요청했습니다.'}
      </p>
      <div className="flex items-center justify-between">
        <span className="text-xs" style={{ color: 'var(--text-secondary)' }}>
          {getRelativeTime(notification.timestamp)}
        </span>
        <div className="flex items-center gap-2">
          {isHelpRequest && onViewScreen && (
            <Button
              size="sm"
              variant="default"
              onClick={() => onViewScreen(notification)}
              className="h-7 text-xs gap-1"
              style={{ backgroundColor: 'var(--primary)' }}
            >
              <Monitor className="w-3 h-3" />
              화면 보기
            </Button>
          )}
          <Button
            size="sm"
            variant="outline"
            onClick={() => onResolve(notification.id)}
            className="h-7 text-xs"
          >
            확인
          </Button>
        </div>
      </div>
    </div>
  );
}

// Progress Tab - 단계 진행 현황
interface ProgressTabProps {
  subtasks: SubtaskInfo[];
  currentSubtaskIndex: number;
  students: StudentListItem[];
  selectedStudentId: number | null;
  totalSubtasks: number;
}

function ProgressTab({
  subtasks,
  currentSubtaskIndex,
  students,
  selectedStudentId,
  totalSubtasks,
}: ProgressTabProps) {
  return (
    <TabsContent value="progress" className="flex-1 m-0 overflow-hidden flex flex-col">
      <SubtaskProgress
        subtasks={subtasks}
        currentSubtaskIndex={currentSubtaskIndex}
        students={students}
        selectedStudentId={selectedStudentId}
        totalSubtasks={totalSubtasks}
      />
    </TabsContent>
  );
}

interface ChatTabProps {
  messages: ChatMessage[];
  currentMessage: string;
  onMessageChange: (message: string) => void;
  onSendMessage: () => void;
}

function ChatTab({ messages, currentMessage, onMessageChange, onSendMessage }: ChatTabProps) {
  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      onSendMessage();
    }
  };

  return (
    <TabsContent value="chat" className="flex-1 m-0 flex flex-col overflow-hidden">
      <ScrollArea className="flex-1">
        <div className="space-y-3 p-4">
          {messages.map((msg) => (
            <ChatMessageBubble key={msg.id} message={msg} />
          ))}
        </div>
      </ScrollArea>

      {/* Chat Input - Fixed at bottom */}
      <div className="p-4 border-t" style={{ borderColor: 'var(--border)' }}>
        <div className="flex gap-2">
          <Input
            placeholder="메시지를 입력하세요..."
            value={currentMessage}
            onChange={(e) => onMessageChange(e.target.value)}
            onKeyPress={handleKeyPress}
          />
          <Button
            size="sm"
            onClick={onSendMessage}
            disabled={!currentMessage.trim()}
            className="gap-2"
          >
            <Send className="w-4 h-4" />
          </Button>
        </div>
        <p className="text-xs mt-2" style={{ color: 'var(--text-secondary)' }}>
          전체 학생에게 메시지를 전송합니다
        </p>
      </div>
    </TabsContent>
  );
}

interface ChatMessageBubbleProps {
  message: ChatMessage;
}

function ChatMessageBubble({ message }: ChatMessageBubbleProps) {
  const isInstructor = message.sender === 'instructor';
  const isSystem = message.sender === 'system';

  return (
    <div className={`flex ${isInstructor ? 'justify-end' : 'justify-start'}`}>
      <div
        className="rounded-lg p-3 max-w-[80%]"
        style={{
          backgroundColor: isInstructor ? 'var(--primary)' : 'var(--muted)',
          color: isInstructor ? 'white' : 'var(--text-primary)',
        }}
      >
        {!isInstructor && !isSystem && (
          <p className="text-xs mb-1" style={{ opacity: 0.8 }}>{message.sender}</p>
        )}
        <p className="text-sm">{message.message}</p>
        <p className="text-xs mt-1" style={{ opacity: 0.7 }}>
          {new Date(message.timestamp).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })}
        </p>
      </div>
    </div>
  );
}

// Utility functions
function getRelativeTime(timestamp: string): string {
  const seconds = Math.floor((Date.now() - new Date(timestamp).getTime()) / 1000);

  if (seconds < 60) return `${seconds}초 전`;
  if (seconds < 3600) return `${Math.floor(seconds / 60)}분 전`;
  if (seconds < 86400) return `${Math.floor(seconds / 3600)}시간 전`;
  return `${Math.floor(seconds / 86400)}일 전`;
}
