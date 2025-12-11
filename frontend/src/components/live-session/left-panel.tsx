import { useState, useMemo } from 'react';
import { StudentListItem } from '../../lib/live-session-types';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { ScrollArea } from '../ui/scroll-area';
import { Input } from '../ui/input';
import { Button } from '../ui/button';
import { Search, Users, AlertCircle, CheckCircle, CheckCircle2, XCircle, ArrowUpDown, ChevronDown, ChevronUp } from 'lucide-react';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '../ui/dropdown-menu';

// Constants
const STATUS_COLORS = {
  active: 'var(--success)',
  inactive: 'var(--status-inactive)',
  help_needed: 'var(--error)',
} as const;

const STATUS_ORDER = { help_needed: 0, active: 1, inactive: 2 } as const;

const STATUS_FILTERS = [
  { type: 'active', icon: CheckCircle, label: '활성', color: 'var(--success)', borderColor: 'border-green-500', bgColor: 'bg-green-50' },
  { type: 'help_needed', icon: AlertCircle, label: '도움', color: 'var(--error)', borderColor: 'border-red-500', bgColor: 'bg-red-50' },
  { type: 'inactive', icon: XCircle, label: '비활성', color: 'var(--status-inactive)', borderColor: 'border-gray-500', bgColor: 'bg-gray-50' },
] as const;

const SORT_LABELS = {
  name: '이름순',
  status: '상태순',
  progress: '진행률순',
} as const;

interface LeftPanelProps {
  lectureName: string;
  lectureDate: string;
  instructor: string;
  totalStudents: number;
  students: StudentListItem[];
  selectedStudentId: number | null;
  onStudentSelect: (studentId: number) => void;
}

type FilterType = 'all' | 'active' | 'inactive' | 'help_needed';
type SortType = 'name' | 'status' | 'progress';

export function LeftPanel({
  lectureName,
  lectureDate,
  instructor,
  totalStudents,
  students,
  selectedStudentId,
  onStudentSelect,
}: LeftPanelProps) {
  const [searchQuery, setSearchQuery] = useState('');
  const [filterType, setFilterType] = useState<FilterType>('all');
  const [sortType, setSortType] = useState<SortType>('name');

  const filteredStudents = useMemo(() => {
    return students
      .filter(student => {
        if (filterType !== 'all' && student.status !== filterType) return false;
        if (searchQuery && !student.name.toLowerCase().includes(searchQuery.toLowerCase())) return false;
        return true;
      })
      .sort((a, b) => {
        if (sortType === 'name') return a.name.localeCompare(b.name);
        if (sortType === 'status') return STATUS_ORDER[a.status] - STATUS_ORDER[b.status];
        return 0;
      });
  }, [students, filterType, searchQuery, sortType]);

  const getStatusCount = (status: FilterType) => {
    if (status === 'all') return students.length;
    return students.filter(s => s.status === status).length;
  };

  const handleResetFilters = () => {
    setSearchQuery('');
    setFilterType('all');
  };

  return (
    <div className="h-full flex flex-col" style={{ width: '350px', minWidth: '350px', backgroundColor: 'white' }}>
      {/* 고정 영역: 강의 정보 + 상태 필터 */}
      <div className="flex-shrink-0">
        <LectureInfoCard
          lectureName={lectureName}
          lectureDate={lectureDate}
          instructor={instructor}
          totalStudents={totalStudents}
        />

        <StatusFilterButtons
          filterType={filterType}
          onFilterChange={setFilterType}
          getStatusCount={getStatusCount}
        />
      </div>

      {/* 가변 영역: 수강생 목록 */}
      <div className="flex-1 min-h-0 flex flex-col overflow-hidden mx-4 mt-2 mb-4">
        <StudentListCard
          students={filteredStudents}
          totalStudents={students.length}
          selectedStudentId={selectedStudentId}
          searchQuery={searchQuery}
          filterType={filterType}
          sortType={sortType}
          onSearchChange={setSearchQuery}
          onFilterChange={setFilterType}
          onSortChange={setSortType}
          onStudentSelect={onStudentSelect}
          onResetFilters={handleResetFilters}
        />
      </div>
    </div>
  );
}

// Sub-components
interface LectureInfoCardProps {
  lectureName: string;
  lectureDate: string;
  instructor: string;
  totalStudents: number;
}

function LectureInfoCard({ lectureName, lectureDate, instructor, totalStudents }: LectureInfoCardProps) {
  const [isCollapsed, setIsCollapsed] = useState(true);

  const infoItems = [
    { label: '강의명', value: lectureName },
    { label: '강의 날짜', value: lectureDate },
    { label: '강사명', value: instructor },
    { label: '수강 인원', value: `${totalStudents}명` },
  ];

  return (
    <Card className="m-4 mb-2" style={{ borderRadius: 'var(--radius-lg)' }}>
      <div
        className={`flex items-center justify-between cursor-pointer select-none ${isCollapsed ? 'px-4 py-2' : 'px-6 pt-6 pb-0'}`}
        onClick={() => setIsCollapsed(!isCollapsed)}
      >
        <span className={isCollapsed ? 'text-sm font-medium' : 'text-lg font-semibold'}>강의 정보</span>
        <Button variant="ghost" size="sm" className="h-6 w-6 p-0">
          {isCollapsed ? (
            <ChevronDown className="w-4 h-4" style={{ color: 'var(--text-secondary)' }} />
          ) : (
            <ChevronUp className="w-4 h-4" style={{ color: 'var(--text-secondary)' }} />
          )}
        </Button>
      </div>
      {!isCollapsed && (
        <CardContent className="space-y-2 text-sm pt-3">
          {infoItems.map((item, index) => (
            <div key={index} className="flex justify-between">
              <span style={{ color: 'var(--text-secondary)' }}>{item.label}:</span>
              <span>{item.value}</span>
            </div>
          ))}
        </CardContent>
      )}
    </Card>
  );
}

interface StatusFilterButtonsProps {
  filterType: FilterType;
  onFilterChange: (type: FilterType) => void;
  getStatusCount: (status: FilterType) => number;
}

function StatusFilterButtons({ filterType, onFilterChange, getStatusCount }: StatusFilterButtonsProps) {
  return (
    <div className="mx-4 mb-2 grid grid-cols-3 gap-2">
      {STATUS_FILTERS.map(({ type, icon: Icon, label, color, borderColor, bgColor }) => {
        const isActive = filterType === type;
        return (
          <button
            key={type}
            onClick={() => onFilterChange(type as FilterType)}
            className={`p-2 rounded-lg border transition-all ${
              isActive ? `${borderColor} ${bgColor}` : 'border-gray-200 hover:bg-gray-50'
            }`}
          >
            <div className="flex flex-col items-center gap-1">
              <Icon className="w-4 h-4" style={{ color }} />
              <span className="text-xs" style={{ color: 'var(--text-secondary)' }}>{label}</span>
              <span className="text-lg" style={{ fontWeight: 'var(--font-weight-semibold)' }}>
                {getStatusCount(type as FilterType)}
              </span>
            </div>
          </button>
        );
      })}
    </div>
  );
}

interface StudentListCardProps {
  students: StudentListItem[];
  totalStudents: number;
  selectedStudentId: number | null;
  searchQuery: string;
  filterType: FilterType;
  sortType: SortType;
  onSearchChange: (query: string) => void;
  onFilterChange: (type: FilterType) => void;
  onSortChange: (type: SortType) => void;
  onStudentSelect: (studentId: number) => void;
  onResetFilters: () => void;
}

function StudentListCard({
  students,
  totalStudents,
  selectedStudentId,
  searchQuery,
  filterType,
  sortType,
  onSearchChange,
  onFilterChange,
  onSortChange,
  onStudentSelect,
  onResetFilters,
}: StudentListCardProps) {
  return (
    <Card className="flex-1 flex flex-col min-h-0 overflow-hidden" style={{ borderRadius: 'var(--radius-lg)' }}>
      <CardHeader className="pb-1.5 flex-shrink-0">
        <div className="flex items-center justify-between">
          <CardTitle className="text-lg">수강생 목록</CardTitle>
          <Button
            variant="ghost"
            size="sm"
            onClick={() => onFilterChange('all')}
            className={filterType !== 'all' ? '' : 'opacity-50'}
          >
            전체
          </Button>
        </div>
      </CardHeader>
      <CardContent className="flex-1 flex flex-col min-h-0 overflow-hidden pb-4">
        <div className="flex-shrink-0">
          <SearchAndSort
            searchQuery={searchQuery}
            sortType={sortType}
            onSearchChange={onSearchChange}
            onSortChange={onSortChange}
          />

          <FilterSummary
            filteredCount={students.length}
            totalCount={totalStudents}
            hasFilters={searchQuery !== '' || filterType !== 'all'}
            onReset={onResetFilters}
          />
        </div>

        <StudentList
          students={students}
          selectedStudentId={selectedStudentId}
          onStudentSelect={onStudentSelect}
        />
      </CardContent>
    </Card>
  );
}

interface SearchAndSortProps {
  searchQuery: string;
  sortType: SortType;
  onSearchChange: (query: string) => void;
  onSortChange: (type: SortType) => void;
}

function SearchAndSort({ searchQuery, sortType, onSearchChange, onSortChange }: SearchAndSortProps) {
  return (
    <div className="space-y-2 mb-3">
      <div className="relative">
        <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4" style={{ color: 'var(--text-secondary)' }} />
        <Input
          placeholder="학생 이름 검색..."
          value={searchQuery}
          onChange={(e) => onSearchChange(e.target.value)}
          className="pl-9"
        />
      </div>

      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <button className="flex items-center gap-2 h-9 px-3 rounded-md border border-gray-300 hover:bg-gray-50 flex-1 w-full transition-colors">
            <ArrowUpDown className="w-4 h-4" />
            <span className="text-sm">정렬: {SORT_LABELS[sortType]}</span>
          </button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="start">
          <DropdownMenuItem onClick={() => onSortChange('name')}>
            이름순
          </DropdownMenuItem>
          <DropdownMenuItem onClick={() => onSortChange('status')}>
            상태순 (도움 우선)
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    </div>
  );
}

interface FilterSummaryProps {
  filteredCount: number;
  totalCount: number;
  hasFilters: boolean;
  onReset: () => void;
}

function FilterSummary({ filteredCount, totalCount, hasFilters, onReset }: FilterSummaryProps) {
  return (
    <div className="flex items-center justify-between text-xs mb-3" style={{ color: 'var(--text-secondary)' }}>
      <span>
        {filteredCount === totalCount 
          ? `전체 ${totalCount}명`
          : `${filteredCount}명 / 전체 ${totalCount}명`
        }
      </span>
      {hasFilters && (
        <button onClick={onReset} className="text-blue-600 hover:underline">
          초기화
        </button>
      )}
    </div>
  );
}

interface StudentListProps {
  students: StudentListItem[];
  selectedStudentId: number | null;
  onStudentSelect: (studentId: number) => void;
}

function StudentList({ students, selectedStudentId, onStudentSelect }: StudentListProps) {
  if (students.length === 0) {
    return <EmptyStudentList />;
  }

  return (
    <div className="flex-1 min-h-0 relative">
      <div className="absolute inset-0">
        <ScrollArea className="h-full w-full">
          <div className="space-y-1 pb-2">
            {students.map((student) => (
              <StudentListItem
                key={student.id}
                student={student}
                isSelected={selectedStudentId === student.id}
                onSelect={onStudentSelect}
              />
            ))}
          </div>
        </ScrollArea>
      </div>
    </div>
  );
}

function EmptyStudentList() {
  return (
    <div className="flex-1 flex flex-col items-center justify-center" style={{ color: 'var(--text-secondary)' }}>
      <Users className="w-12 h-12 mb-2 opacity-50" />
      <p className="text-sm">검색 결과가 없습니다</p>
    </div>
  );
}

interface StudentListItemProps {
  student: StudentListItem;
  isSelected: boolean;
  onSelect: (studentId: number) => void;
}

function StudentListItem({ student, isSelected, onSelect }: StudentListItemProps) {
  const hasCompletedSteps = student.completedSubtasks && student.completedSubtasks.length > 0;

  return (
    <button
      onClick={() => onSelect(student.id)}
      className="w-full flex items-center gap-3 px-3 py-2.5 rounded-lg transition-colors text-left"
      style={{
        backgroundColor: isSelected ? 'var(--accent)' : 'transparent',
        border: isSelected ? '2px solid var(--primary)' : '1px solid transparent',
      }}
      onMouseEnter={(e) => {
        if (!isSelected) e.currentTarget.style.backgroundColor = 'var(--muted)';
      }}
      onMouseLeave={(e) => {
        if (!isSelected) e.currentTarget.style.backgroundColor = 'transparent';
      }}
    >
      <div className="flex items-center justify-center w-8 h-8 rounded-full" style={{ backgroundColor: 'var(--muted)' }}>
        <span className="text-sm">👤</span>
      </div>
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-1.5">
          <p className="truncate">{student.name}</p>
          {student.currentStepCompleted && (
            <CheckCircle2
              className="w-4 h-4 flex-shrink-0"
              style={{ color: 'var(--success)' }}
              title="현재 단계 완료"
            />
          )}
        </div>
        {student.status === 'help_needed' && (
          <p className="text-xs" style={{ color: 'var(--error)' }}>도움 요청</p>
        )}
        {hasCompletedSteps && !student.status.includes('help') && (
          <p className="text-xs" style={{ color: 'var(--success)' }}>
            {student.completedSubtasks!.length}단계 완료
          </p>
        )}
      </div>
      <div className="flex items-center gap-2">
        <div
          className="w-2 h-2 rounded-full"
          style={{ backgroundColor: STATUS_COLORS[student.status] }}
        />
        {student.status === 'help_needed' && <span className="text-sm">🆘</span>}
      </div>
    </button>
  );
}

