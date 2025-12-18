import { useMemo } from 'react';
import { SubtaskInfo, StudentListItem } from '../../lib/live-session-types';
import { ScrollArea } from '../ui/scroll-area';
import { Progress } from '../ui/progress';
import { CheckCircle2, Circle, PlayCircle, Users } from 'lucide-react';

interface SubtaskProgressProps {
  subtasks: SubtaskInfo[];
  currentSubtaskIndex: number;
  students: StudentListItem[];
  selectedStudentId: number | null;
  totalSubtasks: number;
}

/**
 * SubtaskProgress Component
 *
 * 세션의 모든 단계(Subtask)를 시각화하고,
 * 각 단계별 학생 완료 현황을 표시합니다.
 */
export function SubtaskProgress({
  subtasks,
  currentSubtaskIndex,
  students,
  selectedStudentId,
  totalSubtasks,
}: SubtaskProgressProps) {
  // 선택된 학생의 완료된 단계 ID 목록
  const selectedStudentCompletedIds = useMemo(() => {
    const selectedStudent = students.find(s => s.id === selectedStudentId);
    return new Set(selectedStudent?.completedSubtasks || []);
  }, [students, selectedStudentId]);

  // 각 단계별 완료 학생 수 계산
  const completionStats = useMemo(() => {
    const stats = new Map<number, number>();

    for (const subtask of subtasks) {
      const completedCount = students.filter(
        student => student.completedSubtasks?.includes(subtask.id)
      ).length;
      stats.set(subtask.id, completedCount);
    }

    return stats;
  }, [subtasks, students]);

  // 전체 진행률 계산
  const overallProgress = useMemo(() => {
    if (students.length === 0 || totalSubtasks === 0) return 0;

    let totalCompleted = 0;
    for (const student of students) {
      totalCompleted += (student.completedSubtasks?.length || 0);
    }

    const maxPossible = students.length * totalSubtasks;
    return Math.round((totalCompleted / maxPossible) * 100);
  }, [students, totalSubtasks]);

  if (subtasks.length === 0) {
    return (
      <div className="px-4 py-6 text-center" style={{ color: 'var(--text-secondary)' }}>
        <p className="text-sm">등록된 단계가 없습니다</p>
      </div>
    );
  }

  return (
    <div className="flex flex-col h-full">
      {/* 전체 진행률 요약 */}
      <div className="px-4 py-3 border-b" style={{ borderColor: 'var(--border)' }}>
        <div className="flex items-center justify-between mb-2">
          <span className="text-sm" style={{ fontWeight: 'var(--font-weight-semibold)' }}>
            전체 완료율
          </span>
          <span className="text-sm" style={{ color: 'var(--text-secondary)' }}>
            {currentSubtaskIndex + 1}/{totalSubtasks} 단계
          </span>
        </div>
        <div className="flex items-center gap-3">
          <Progress value={overallProgress} className="flex-1 h-2" />
          <span
            className="text-sm min-w-[40px] text-right"
            style={{ fontWeight: 'var(--font-weight-semibold)', color: 'var(--primary)' }}
          >
            {overallProgress}%
          </span>
        </div>
      </div>

      {/* 단계별 목록 */}
      <ScrollArea className="flex-1">
        <div className="py-1">
          {subtasks.map((subtask, index) => {
            const isCurrentStep = index === currentSubtaskIndex;
            const isPastStep = index < currentSubtaskIndex;
            const completedCount = completionStats.get(subtask.id) || 0;

            // 선택된 학생이 이 단계를 완료했는지
            const isCompletedBySelected = selectedStudentCompletedIds.has(subtask.id);

            return (
              <SubtaskRow
                key={subtask.id}
                subtask={subtask}
                index={index}
                isCurrentStep={isCurrentStep}
                isPastStep={isPastStep}
                completedCount={completedCount}
                totalStudents={students.length}
                isCompletedBySelected={isCompletedBySelected}
                hasSelectedStudent={selectedStudentId !== null}
              />
            );
          })}
        </div>
      </ScrollArea>
    </div>
  );
}

interface SubtaskRowProps {
  subtask: SubtaskInfo;
  index: number;
  isCurrentStep: boolean;
  isPastStep: boolean;
  completedCount: number;
  totalStudents: number;
  isCompletedBySelected: boolean;
  hasSelectedStudent: boolean;
}

function SubtaskRow({
  subtask,
  index,
  isCurrentStep,
  isPastStep,
  completedCount,
  totalStudents,
  isCompletedBySelected,
  hasSelectedStudent,
}: SubtaskRowProps) {
  // 상태에 따른 아이콘 및 색상
  const getStatusIcon = () => {
    if (isCurrentStep) {
      return <PlayCircle className="w-4 h-4" style={{ color: 'var(--primary)' }} />;
    }
    if (isPastStep) {
      return <CheckCircle2 className="w-4 h-4" style={{ color: 'var(--success)' }} />;
    }
    return <Circle className="w-4 h-4" style={{ color: 'var(--text-secondary)' }} />;
  };

  const getBackgroundColor = () => {
    if (isCurrentStep) return 'rgba(37, 99, 235, 0.08)';
    if (hasSelectedStudent && isCompletedBySelected) return 'rgba(34, 197, 94, 0.08)';
    return 'transparent';
  };

  return (
    <div
      className="flex items-center gap-2 px-4 py-2 transition-colors"
      style={{
        backgroundColor: getBackgroundColor(),
        borderLeft: isCurrentStep ? '3px solid var(--primary)' : '3px solid transparent',
      }}
    >
      {/* 순번 + 아이콘 */}
      <div className="flex items-center gap-2 min-w-[44px]">
        <span
          className="text-xs w-5 h-5 flex items-center justify-center rounded"
          style={{
            backgroundColor: isCurrentStep ? 'var(--primary)' : 'var(--muted)',
            color: isCurrentStep ? 'white' : 'var(--text-secondary)',
            fontWeight: 'var(--font-weight-medium)',
          }}
        >
          {index + 1}
        </span>
        {getStatusIcon()}
      </div>

      {/* 단계 제목 */}
      <div className="flex-1 min-w-0">
        <p
          className="text-sm truncate"
          style={{
            fontWeight: isCurrentStep ? 'var(--font-weight-semibold)' : 'normal',
            color: isCurrentStep ? 'var(--primary)' : 'inherit',
          }}
          title={subtask.title}
        >
          {subtask.title}
        </p>
      </div>

      {/* 완료 통계 */}
      <div className="flex items-center gap-1.5 text-xs" style={{ color: 'var(--text-secondary)' }}>
        <Users className="w-3 h-3" />
        <span>{completedCount}/{totalStudents}</span>
        {/* 선택된 학생의 완료 상태 */}
        {hasSelectedStudent && isCompletedBySelected && (
          <CheckCircle2 className="w-3.5 h-3.5" style={{ color: 'var(--success)' }} />
        )}
      </div>
    </div>
  );
}
