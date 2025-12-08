import { useState } from 'react';
import { GroupProgress, LiveNotification } from '../../lib/live-session-types';
import { Card, CardContent } from '../ui/card';
import { Button } from '../ui/button';
import { Badge } from '../ui/badge';
import { ScrollArea } from '../ui/scroll-area';
import { Input } from '../ui/input';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../ui/tabs';
import { CheckCircle, HelpCircle, AlertTriangle, Info, Send, Brain, TrendingUp, TrendingDown, Lightbulb, Monitor } from 'lucide-react';

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

const AI_INSIGHTS = [
  {
    type: 'warning',
    icon: TrendingDown,
    title: '진행 지연 예측',
    message: '50대 그룹이 평균보다 25% 느린 속도로 진행 중입니다.',
    action: '개별 지원이 필요할 수 있습니다.',
  },
  {
    type: 'tip',
    icon: Lightbulb,
    title: '추천 액션',
    message: '\'앱 설치하기\' 단계에서 많은 학생이 어려움을 겪고 있습니다.',
    action: '단계별 시연을 추천합니다.',
  },
  {
    type: 'success',
    icon: TrendingUp,
    title: '긍정적 진행',
    message: '전체 학생의 75%가 예상 진도를 초과했습니다.',
    action: '계속 진행하세요!',
  },
] as const;

const MAX_VISIBLE_AVATARS = 4;

interface RightPanelProps {
  participants: { id: number; name: string; avatarUrl?: string; isOnline: boolean }[];
  groupProgress: GroupProgress[];
  notifications: LiveNotification[];
  onResolveNotification: (notificationId: number) => void;
  onViewScreen?: (notification: LiveNotification) => void;
}

interface ChatMessage {
  id: number;
  sender: string;
  message: string;
  timestamp: string;
}

export function RightPanel({
  participants,
  groupProgress,
  notifications,
  onResolveNotification,
  onViewScreen,
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
      {/* Tabs Header - Fixed */}
      <div className="p-4 pb-3 border-b" style={{ borderColor: 'var(--border)' }}>
        <Tabs defaultValue="notifications" className="w-full">
          <TabsList className="grid w-full grid-cols-3">
            <TabsTrigger value="notifications" className="text-xs">
              알림
              {notifications.length > 0 && (
                <Badge className="ml-1 h-4 px-1 text-xs" style={{ backgroundColor: 'var(--error)', color: 'white' }}>
                  {notifications.length}
                </Badge>
              )}
            </TabsTrigger>
            <TabsTrigger value="ai" className="text-xs">
              AI분석
            </TabsTrigger>
            <TabsTrigger value="chat" className="text-xs">
              채팅
            </TabsTrigger>
          </TabsList>
        </Tabs>
      </div>

      {/* Participants Summary - Fixed */}
      <ParticipantsSummary participants={participants} />

      {/* Tab Contents - Scrollable */}
      <Tabs defaultValue="notifications" className="flex-1 flex flex-col overflow-hidden">
        <NotificationsTab
          notifications={notifications}
          onResolve={onResolveNotification}
          onViewScreen={onViewScreen}
        />
        <AIInsightsTab groupProgress={groupProgress} />
        <ChatTab
          messages={chatMessages}
          currentMessage={chatMessage}
          onMessageChange={setChatMessage}
          onSendMessage={handleSendMessage}
        />
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

interface AIInsightsTabProps {
  groupProgress: GroupProgress[];
}

function AIInsightsTab({ groupProgress }: AIInsightsTabProps) {
  return (
    <TabsContent value="ai" className="flex-1 m-0 overflow-hidden">
      <ScrollArea className="h-full">
        <div className="space-y-4 p-4">
          <AIHeader />
          {AI_INSIGHTS.map((insight, index) => (
            <AIInsightCard key={index} insight={insight} />
          ))}
          <GroupProgressSection groupProgress={groupProgress} />
        </div>
      </ScrollArea>
    </TabsContent>
  );
}

function AIHeader() {
  return (
    <Card style={{ borderRadius: 'var(--radius-md)', backgroundColor: '#F0F7FF' }}>
      <CardContent className="p-3">
        <div className="flex items-center gap-2 mb-2">
          <Brain className="w-5 h-5" style={{ color: 'var(--primary)' }} />
          <h3 className="text-sm" style={{ fontWeight: 'var(--font-weight-semibold)' }}>
            AI 실시간 분석
          </h3>
        </div>
        <p className="text-xs" style={{ color: 'var(--text-secondary)' }}>
          MobileGPT AI가 학습 패턴을 실시간으로 분석하여 인사이트를 제공합니다.
        </p>
      </CardContent>
    </Card>
  );
}

interface AIInsightCardProps {
  insight: typeof AI_INSIGHTS[number];
}

function AIInsightCard({ insight }: AIInsightCardProps) {
  const Icon = insight.icon;
  const bgColor = insight.type === 'warning' ? '#FFF3E0' 
    : insight.type === 'success' ? '#E8F5E9' 
    : '#F5F5F5';
  const iconColor = insight.type === 'warning' ? 'var(--warning)' 
    : insight.type === 'success' ? 'var(--success)' 
    : 'var(--primary)';

  return (
    <Card style={{ borderRadius: 'var(--radius-md)', backgroundColor: bgColor }}>
      <CardContent className="p-3">
        <div className="flex items-start gap-2 mb-2">
          <Icon className="w-4 h-4 mt-0.5" style={{ color: iconColor }} />
          <div className="flex-1">
            <h4 className="text-sm mb-1" style={{ fontWeight: 'var(--font-weight-semibold)' }}>
              {insight.title}
            </h4>
            <p className="text-xs mb-2" style={{ color: 'var(--text-secondary)' }}>
              {insight.message}
            </p>
            <div className="flex items-center gap-1">
              <Lightbulb className="w-3 h-3" style={{ color: iconColor }} />
              <p className="text-xs" style={{ color: iconColor, fontWeight: 'var(--font-weight-semibold)' }}>
                {insight.action}
              </p>
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}

interface GroupProgressSectionProps {
  groupProgress: GroupProgress[];
}

function GroupProgressSection({ groupProgress }: GroupProgressSectionProps) {
  return (
    <div className="space-y-2">
      <h3 className="text-sm" style={{ color: 'var(--text-secondary)' }}>그룹별 학습 내용</h3>
      {groupProgress.map((group) => (
        <GroupProgressCard key={group.groupId} group={group} />
      ))}
    </div>
  );
}

interface GroupProgressCardProps {
  group: GroupProgress;
}

function GroupProgressCard({ group }: GroupProgressCardProps) {
  const visibleParticipants = group.participants.slice(0, 3);
  const remainingCount = Math.max(0, group.participants.length - 3);

  return (
    <Card style={{ borderRadius: 'var(--radius-md)' }}>
      <CardContent className="p-3">
        <div className="mb-2">
          <h4 className="text-sm" style={{ fontWeight: 'var(--font-weight-semibold)' }}>
            {group.groupName}
          </h4>
          <p className="text-xs mt-1" style={{ color: 'var(--text-secondary)' }}>
            {group.currentTask}
          </p>
        </div>
        <div className="flex items-center">
          {visibleParticipants.map((participant, index) => (
            <div
              key={participant.id}
              className="relative rounded-full border-2 border-white"
              style={{
                width: '32px',
                height: '32px',
                marginLeft: index > 0 ? '-8px' : '0',
                backgroundColor: 'var(--muted)',
                zIndex: 3 - index,
              }}
            >
              <div className="w-full h-full flex items-center justify-center text-sm">👤</div>
            </div>
          ))}
          {remainingCount > 0 && (
            <div
              className="flex items-center justify-center rounded-full border-2 border-white text-xs"
              style={{
                width: '32px',
                height: '32px',
                marginLeft: '-8px',
                backgroundColor: 'var(--muted)',
                color: 'var(--text-secondary)',
              }}
            >
              +{remainingCount}
            </div>
          )}
        </div>
      </CardContent>
    </Card>
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
