import { useState } from 'react';
import { StudentScreen } from '../../lib/live-session-types';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Button } from '../ui/button';
import { Badge } from '../ui/badge';
import { ChevronLeft, ChevronRight, RefreshCw, Grid2X2, ZoomIn, ZoomOut, Download } from 'lucide-react';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '../ui/dropdown-menu';

// Constants
const VIEW_MODES = {
  SINGLE: 'single',
  GRID_2: 'grid-2',
  GRID_4: 'grid-4',
} as const;

const VIEW_MODE_LABELS = {
  [VIEW_MODES.SINGLE]: '단일 화면',
  [VIEW_MODES.GRID_2]: '2분할',
  [VIEW_MODES.GRID_4]: '4분할',
};

const ZOOM_CONSTRAINTS = { MIN: 50, MAX: 150, STEP: 10 };
const MAX_VISIBLE_PAGES = 5;

interface CenterAreaProps {
  studentScreen: StudentScreen | null;
  totalStudents: number;
  currentStudentIndex: number;
  onPreviousStudent: () => void;
  onNextStudent: () => void;
  onRefresh: () => void;
}

type ViewMode = typeof VIEW_MODES[keyof typeof VIEW_MODES];

export function CenterArea({
  studentScreen,
  totalStudents,
  currentStudentIndex,
  onPreviousStudent,
  onNextStudent,
  onRefresh,
}: CenterAreaProps) {
  const [viewMode, setViewMode] = useState<ViewMode>(VIEW_MODES.SINGLE);
  const [zoomLevel, setZoomLevel] = useState(100);

  const handleZoom = (direction: 'in' | 'out') => {
    setZoomLevel(prev => 
      direction === 'in'
        ? Math.min(prev + ZOOM_CONSTRAINTS.STEP, ZOOM_CONSTRAINTS.MAX)
        : Math.max(prev - ZOOM_CONSTRAINTS.STEP, ZOOM_CONSTRAINTS.MIN)
    );
  };

  const handleDownload = () => {
    if (!studentScreen?.imageUrl) return;
    
    const link = document.createElement('a');
    link.href = studentScreen.imageUrl;
    link.download = `${studentScreen.studentName}_screen_${new Date().getTime()}.png`;
    link.click();
  };

  return (
    <div className="flex-1 flex flex-col h-full p-4" style={{ backgroundColor: 'var(--surface)' }}>
      <Card className="flex-1 flex flex-col" style={{ borderRadius: 'var(--radius-lg)' }}>
        <CardHeader className="pb-3 border-b" style={{ borderColor: 'var(--border)' }}>
          <ScreenHeader
            studentName={studentScreen?.studentName}
            viewMode={viewMode}
            onViewModeChange={setViewMode}
            zoomLevel={zoomLevel}
            onZoomIn={() => handleZoom('in')}
            onZoomOut={() => handleZoom('out')}
            canDownload={!!studentScreen?.imageUrl}
            onDownload={handleDownload}
            canRefresh={!!studentScreen}
            onRefresh={onRefresh}
          />
        </CardHeader>

        <CardContent className="flex-1 flex items-center justify-center p-8">
          <ScreenDisplay
            studentScreen={studentScreen}
            viewMode={viewMode}
            zoomLevel={zoomLevel}
            onRefresh={onRefresh}
          />
        </CardContent>

        {studentScreen && (
          <ScreenPagination
            currentIndex={currentStudentIndex}
            total={totalStudents}
            onPrevious={onPreviousStudent}
            onNext={onNextStudent}
          />
        )}
      </Card>
    </div>
  );
}

// Sub-components
interface ScreenHeaderProps {
  studentName?: string;
  viewMode: ViewMode;
  onViewModeChange: (mode: ViewMode) => void;
  zoomLevel: number;
  onZoomIn: () => void;
  onZoomOut: () => void;
  canDownload: boolean;
  onDownload: () => void;
  canRefresh: boolean;
  onRefresh: () => void;
}

function ScreenHeader({
  studentName,
  viewMode,
  onViewModeChange,
  zoomLevel,
  onZoomIn,
  onZoomOut,
  canDownload,
  onDownload,
  canRefresh,
  onRefresh,
}: ScreenHeaderProps) {
  return (
    <div className="flex items-center justify-between">
      <CardTitle className="text-xl">
        {studentName ? `${studentName} 님의 화면` : '학생 화면'}
      </CardTitle>
      <div className="flex items-center gap-2">
        {/* View Mode Selector */}
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <button className="flex items-center gap-2 h-9 px-3 rounded-md border border-gray-300 hover:bg-gray-50 transition-colors">
              <Grid2X2 className="w-4 h-4" />
              <span className="text-sm">{VIEW_MODE_LABELS[viewMode]}</span>
            </button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuItem onClick={() => onViewModeChange(VIEW_MODES.SINGLE)}>
              단일 화면 (1x1)
            </DropdownMenuItem>
            <DropdownMenuItem onClick={() => onViewModeChange(VIEW_MODES.GRID_2)}>
              2분할 (1x2)
            </DropdownMenuItem>
            <DropdownMenuItem onClick={() => onViewModeChange(VIEW_MODES.GRID_4)}>
              4분할 (2x2)
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>

        {/* Zoom Controls */}
        {studentName && viewMode === VIEW_MODES.SINGLE && (
          <div className="flex items-center gap-1 border rounded-lg p-1">
            <Button
              variant="ghost"
              size="sm"
              onClick={onZoomOut}
              disabled={zoomLevel <= ZOOM_CONSTRAINTS.MIN}
              className="h-7 w-7 p-0"
            >
              <ZoomOut className="w-4 h-4" />
            </Button>
            <span className="text-xs px-2" style={{ color: 'var(--text-secondary)' }}>
              {zoomLevel}%
            </span>
            <Button
              variant="ghost"
              size="sm"
              onClick={onZoomIn}
              disabled={zoomLevel >= ZOOM_CONSTRAINTS.MAX}
              className="h-7 w-7 p-0"
            >
              <ZoomIn className="w-4 h-4" />
            </Button>
          </div>
        )}

        {/* Download */}
        {canDownload && (
          <Button variant="outline" size="sm" onClick={onDownload} className="gap-2">
            <Download className="w-4 h-4" />
            저장
          </Button>
        )}

        {/* Refresh */}
        {canRefresh && (
          <Button variant="outline" size="sm" onClick={onRefresh} className="gap-2">
            <RefreshCw className="w-4 h-4" />
            새로고침
          </Button>
        )}
      </div>
    </div>
  );
}

interface ScreenDisplayProps {
  studentScreen: StudentScreen | null;
  viewMode: ViewMode;
  zoomLevel: number;
  onRefresh: () => void;
}

function ScreenDisplay({ studentScreen, viewMode, zoomLevel, onRefresh }: ScreenDisplayProps) {
  if (viewMode === VIEW_MODES.SINGLE) {
    return <StudentScreenRenderer screen={studentScreen} zoomLevel={zoomLevel} onRefresh={onRefresh} />;
  }

  const gridCols = viewMode === VIEW_MODES.GRID_2 ? 2 : 2;
  const placeholderCount = viewMode === VIEW_MODES.GRID_2 ? 1 : 3;

  return (
    <div className={`grid grid-cols-${gridCols} gap-6 w-full max-w-4xl`}>
      <StudentScreenRenderer screen={studentScreen} zoomLevel={100} onRefresh={onRefresh} viewMode={viewMode} />
      {Array.from({ length: placeholderCount }, (_, i) => (
        <PlaceholderScreen key={i} index={i + 1} isGrid2={viewMode === VIEW_MODES.GRID_2} />
      ))}
    </div>
  );
}

interface StudentScreenRendererProps {
  screen: StudentScreen | null;
  zoomLevel: number;
  onRefresh: () => void;
  viewMode?: ViewMode;
}

function StudentScreenRenderer({ screen, zoomLevel, onRefresh, viewMode = VIEW_MODES.SINGLE }: StudentScreenRendererProps) {
  const isSingleView = viewMode === VIEW_MODES.SINGLE;
  const maxWidth = isSingleView ? '480px' : '280px';

  if (!screen) {
    return <EmptyScreenPlaceholder />;
  }

  if (screen.isLoading) {
    return <LoadingScreen />;
  }

  if (screen.error) {
    return <ErrorScreen onRefresh={onRefresh} />;
  }

  if (screen.imageUrl) {
    return (
      <div className="relative flex flex-col items-center">
        <div className="mb-2">
          <Badge variant="outline" className="mb-1">{screen.studentName}</Badge>
        </div>
        <div
          className="rounded-2xl overflow-hidden"
          style={{
            border: '2px solid var(--border)',
            boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
            maxWidth,
            aspectRatio: '9/16',
            transform: `scale(${zoomLevel / 100})`,
            transition: 'transform 0.2s',
          }}
        >
          <img
            src={screen.imageUrl}
            alt={`${screen.studentName}의 화면`}
            className="w-full h-full object-cover"
          />
        </div>
        {isSingleView && (
          <div className="mt-4 text-center text-sm" style={{ color: 'var(--text-secondary)' }}>
            마지막 업데이트: {new Date(screen.lastUpdated).toLocaleTimeString('ko-KR')}
          </div>
        )}
      </div>
    );
  }

  return <NoDataScreen maxWidth={maxWidth} />;
}

function EmptyScreenPlaceholder() {
  return (
    <div className="text-center" style={{ color: 'var(--text-secondary)' }}>
      <div className="text-6xl mb-4">👈</div>
      <h3 className="text-xl mb-2">왼쪽에서 학생을 선택하세요</h3>
      <p>학생 이름을 클릭하면 화면이 표시됩니다</p>
    </div>
  );
}

function LoadingScreen() {
  return (
    <div className="text-center">
      <div className="animate-spin rounded-full h-16 w-16 border-b-4 mx-auto mb-4" style={{ borderColor: 'var(--primary)' }}></div>
      <p style={{ color: 'var(--text-secondary)' }}>화면을 불러오는 중...</p>
    </div>
  );
}

function ErrorScreen({ onRefresh }: { onRefresh: () => void }) {
  return (
    <div className="text-center p-8 rounded-lg" style={{ backgroundColor: '#FFEBEE', maxWidth: '500px' }}>
      <div className="text-6xl mb-4">📱</div>
      <h3 className="text-xl mb-2" style={{ color: 'var(--error)' }}>
        화면을 불러올 수 없습니다
      </h3>
      <ul className="text-left space-y-2 mt-4 mb-4" style={{ color: 'var(--text-secondary)' }}>
        <li>• 학생이 비활성 상태일 수 있습니다</li>
        <li>• AccessibilityService가 꺼져있을 수 있습니다</li>
      </ul>
      <Button onClick={onRefresh} variant="outline">
        다시 시도
      </Button>
    </div>
  );
}

function NoDataScreen({ maxWidth }: { maxWidth: string }) {
  return (
    <div
      className="flex items-center justify-center rounded-2xl"
      style={{
        border: '2px solid var(--border)',
        backgroundColor: 'var(--muted)',
        maxWidth,
        aspectRatio: '9/16',
        width: '100%',
      }}
    >
      <div className="text-center" style={{ color: 'var(--text-secondary)' }}>
        <div className="text-6xl mb-4">📱</div>
        <p className="text-xl">화면 데이터 없음</p>
        <p className="text-sm mt-2">학생이 아직 화면을 전송하지 않았습니다</p>
      </div>
    </div>
  );
}

function PlaceholderScreen({ index, isGrid2 }: { index: number; isGrid2?: boolean }) {
  return (
    <div className="text-center" style={{ color: 'var(--text-secondary)' }}>
      <div className="text-4xl mb-2">👈</div>
      <p className={isGrid2 ? '' : 'text-sm'}>
        {isGrid2 ? '추가 학생 선택' : `추가 학생 ${index}`}
      </p>
    </div>
  );
}

interface ScreenPaginationProps {
  currentIndex: number;
  total: number;
  onPrevious: () => void;
  onNext: () => void;
}

function ScreenPagination({ currentIndex, total, onPrevious, onNext }: ScreenPaginationProps) {
  const getPageNumbers = () => {
    if (total <= MAX_VISIBLE_PAGES) {
      return Array.from({ length: total }, (_, i) => i + 1);
    }

    if (currentIndex < 2) {
      return Array.from({ length: MAX_VISIBLE_PAGES }, (_, i) => i + 1);
    }

    if (currentIndex > total - 3) {
      return Array.from({ length: MAX_VISIBLE_PAGES }, (_, i) => total - 4 + i);
    }

    return Array.from({ length: MAX_VISIBLE_PAGES }, (_, i) => currentIndex - 1 + i);
  };

  const pageNumbers = getPageNumbers();
  const showEllipsis = total > MAX_VISIBLE_PAGES && currentIndex < total - 3;

  return (
    <div className="border-t p-4" style={{ borderColor: 'var(--border)' }}>
      <div className="flex items-center justify-center gap-2">
        <Button
          variant="outline"
          size="sm"
          onClick={onPrevious}
          disabled={currentIndex === 0}
          className="gap-2"
        >
          <ChevronLeft className="w-4 h-4" />
          이전
        </Button>

        <div className="flex items-center gap-2 px-4">
          {pageNumbers.map((pageNum, i) => {
            const isActive = pageNum === currentIndex + 1;
            return (
              <button
                key={i}
                className="w-8 h-8 rounded flex items-center justify-center text-sm transition-colors"
                style={{
                  backgroundColor: isActive ? 'var(--primary)' : 'transparent',
                  color: isActive ? 'white' : 'var(--text-primary)',
                  fontWeight: isActive ? 'var(--font-weight-semibold)' : 'var(--font-weight-normal)',
                }}
                onMouseEnter={(e) => {
                  if (!isActive) e.currentTarget.style.backgroundColor = 'var(--muted)';
                }}
                onMouseLeave={(e) => {
                  if (!isActive) e.currentTarget.style.backgroundColor = 'transparent';
                }}
              >
                {pageNum}
              </button>
            );
          })}
          {showEllipsis && (
            <>
              <span>...</span>
              <span className="text-sm" style={{ color: 'var(--text-secondary)' }}>{total}</span>
            </>
          )}
        </div>

        <Button
          variant="outline"
          size="sm"
          onClick={onNext}
          disabled={currentIndex === total - 1}
          className="gap-2"
        >
          다음
          <ChevronRight className="w-4 h-4" />
        </Button>
      </div>
    </div>
  );
}
