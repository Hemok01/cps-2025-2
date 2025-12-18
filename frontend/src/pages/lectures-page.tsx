import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { lectureService } from '../lib/lecture-service';
import { Lecture } from '../lib/lecture-types';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import {
  BookOpen,
  Plus,
  Users,
  Play,
  Edit,
  Trash2,
  ToggleLeft,
  ToggleRight,
  TrendingUp
} from 'lucide-react';
import { toast } from 'sonner';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '../components/ui/alert-dialog';

export function LecturesPage() {
  const navigate = useNavigate();
  const [lectures, setLectures] = useState<Lecture[]>([]);
  const [loading, setLoading] = useState(true);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [lectureToDelete, setLectureToDelete] = useState<Lecture | null>(null);

  useEffect(() => {
    loadLectures();
  }, []);

  const loadLectures = async () => {
    setLoading(true);
    try {
      const data = await lectureService.getAllLectures();
      setLectures(data);
    } catch (error) {
      toast.error('강의 목록을 불러오는데 실패했습니다');
      console.error('Failed to load lectures:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleToggleStatus = async (lecture: Lecture) => {
    try {
      const updated = await lectureService.toggleLectureStatus(lecture.id);
      setLectures(prev => prev.map(l => l.id === updated.id ? updated : l));
      toast.success(updated.isActive ? '강의가 활성화되었습니다' : '강의가 비활성화되었습니다');
    } catch (error) {
      toast.error('상태 변경에 실패했습니다');
    }
  };

  const handleDeleteClick = (lecture: Lecture) => {
    setLectureToDelete(lecture);
    setDeleteDialogOpen(true);
  };

  const handleDeleteConfirm = async () => {
    if (!lectureToDelete) return;

    try {
      await lectureService.deleteLecture(lectureToDelete.id);
      setLectures(prev => prev.filter(l => l.id !== lectureToDelete.id));
      toast.success('강의가 삭제되었습니다');
      setDeleteDialogOpen(false);
      setLectureToDelete(null);
    } catch (error) {
      toast.error('강의 삭제에 실패했습니다');
    }
  };

  const getDifficultyBadge = (difficulty: string) => {
    const styles: Record<string, { bg: string; text: string; label: string }> = {
      beginner: { bg: 'var(--success)', text: 'white', label: '초급' },
      intermediate: { bg: 'var(--warning)', text: 'white', label: '중급' },
      advanced: { bg: 'var(--error)', text: 'white', label: '고급' },
    };

    const style = styles[difficulty] || styles.beginner;

    return (
      <Badge style={{ backgroundColor: style.bg, color: style.text }}>
        {style.label}
      </Badge>
    );
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-96">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 mx-auto mb-4" style={{ borderColor: 'var(--primary)' }}></div>
          <p>강의 목록 로딩 중...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl mb-2">강의 관리</h1>
          <p style={{ color: 'var(--text-secondary)' }}>강의를 추가하고 관리하세요</p>
        </div>
        <Button
          onClick={() => navigate('/lectures/new')}
          className="gap-2"
          style={{ backgroundColor: 'var(--primary)' }}
        >
          <Plus className="w-5 h-5" />
          새 강의 추가
        </Button>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Card style={{ borderRadius: 'var(--radius-lg)' }}>
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="p-2 rounded-lg" style={{ backgroundColor: 'var(--accent)' }}>
                <BookOpen className="w-5 h-5" style={{ color: 'var(--primary)' }} />
              </div>
              <div>
                <p className="text-sm" style={{ color: 'var(--text-secondary)' }}>전체 강의</p>
                <p className="text-2xl">{lectures.length}</p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card style={{ borderRadius: 'var(--radius-lg)' }}>
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="p-2 rounded-lg" style={{ backgroundColor: '#E8F5E9' }}>
                <ToggleRight className="w-5 h-5" style={{ color: 'var(--success)' }} />
              </div>
              <div>
                <p className="text-sm" style={{ color: 'var(--text-secondary)' }}>활성 강의</p>
                <p className="text-2xl">{lectures.filter(l => l.isActive).length}</p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card style={{ borderRadius: 'var(--radius-lg)' }}>
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="p-2 rounded-lg" style={{ backgroundColor: '#FFF3E0' }}>
                <Users className="w-5 h-5" style={{ color: 'var(--warning)' }} />
              </div>
              <div>
                <p className="text-sm" style={{ color: 'var(--text-secondary)' }}>총 수강생</p>
                <p className="text-2xl">{lectures.reduce((sum, l) => sum + l.studentCount, 0)}</p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card style={{ borderRadius: 'var(--radius-lg)' }}>
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="p-2 rounded-lg" style={{ backgroundColor: '#FFEBEE' }}>
                <Play className="w-5 h-5" style={{ color: 'var(--error)' }} />
              </div>
              <div>
                <p className="text-sm" style={{ color: 'var(--text-secondary)' }}>총 세션</p>
                <p className="text-2xl">{lectures.reduce((sum, l) => sum + l.sessionCount, 0)}</p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Lectures Grid */}
      {lectures.length === 0 ? (
        <Card className="p-12 text-center" style={{ borderRadius: 'var(--radius-lg)' }}>
          <BookOpen className="w-16 h-16 mx-auto mb-4 opacity-20" />
          <h3 className="text-xl mb-2">강의가 없습니다</h3>
          <p className="mb-4" style={{ color: 'var(--text-secondary)' }}>
            새로운 강의를 추가해보세요
          </p>
          <Button
            onClick={() => navigate('/lectures/new')}
            style={{ backgroundColor: 'var(--primary)' }}
          >
            강의 추가하기
          </Button>
        </Card>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {lectures.map((lecture) => (
            <Card 
              key={lecture.id}
              className="hover:shadow-lg transition-shadow"
              style={{ borderRadius: 'var(--radius-lg)' }}
            >
              <CardHeader>
                <div className="flex items-start justify-between mb-2">
                  <BookOpen className="w-8 h-8" style={{ color: 'var(--primary)' }} />
                  <div className="flex gap-2">
                    {getDifficultyBadge(lecture.difficulty)}
                    {lecture.isActive ? (
                      <Badge style={{ backgroundColor: '#E8F5E9', color: 'var(--success)' }}>
                        활성
                      </Badge>
                    ) : (
                      <Badge style={{ backgroundColor: 'var(--muted)', color: 'var(--text-secondary)' }}>
                        비활성
                      </Badge>
                    )}
                  </div>
                </div>
                <CardTitle>{lecture.title}</CardTitle>
                <CardDescription>{lecture.description}</CardDescription>
              </CardHeader>
              <CardContent>
                <div className="space-y-3 mb-4">
                  <div className="flex items-center gap-2 text-sm" style={{ color: 'var(--text-secondary)' }}>
                    <Users className="w-4 h-4" />
                    <span>학생 {lecture.studentCount}명</span>
                  </div>
                  <div className="flex items-center gap-2 text-sm" style={{ color: 'var(--text-secondary)' }}>
                    <Play className="w-4 h-4" />
                    <span>세션 {lecture.sessionCount}회</span>
                  </div>
                  <div className="flex items-center gap-2 text-sm" style={{ color: 'var(--text-secondary)' }}>
                    <BookOpen className="w-4 h-4" />
                    <span>과제 {lecture.taskCount}개</span>
                  </div>
                  <div className="flex items-center gap-2 text-sm" style={{ color: 'var(--text-secondary)' }}>
                    <TrendingUp className="w-4 h-4" />
                    <span>단계 {lecture.stepCount}개</span>
                  </div>
                </div>

                <div className="flex gap-2">
                  <Button
                    size="sm"
                    variant="outline"
                    className="flex-1 gap-2"
                    onClick={() => navigate(`/lectures/edit/${lecture.id}`)}
                  >
                    <Edit className="w-4 h-4" />
                    수정
                  </Button>
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => handleToggleStatus(lecture)}
                    className="gap-2"
                    title={lecture.isActive ? '비활성화' : '활성화'}
                  >
                    {lecture.isActive ? (
                      <ToggleRight className="w-4 h-4" style={{ color: 'var(--success)' }} />
                    ) : (
                      <ToggleLeft className="w-4 h-4" style={{ color: 'var(--text-secondary)' }} />
                    )}
                  </Button>
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => handleDeleteClick(lecture)}
                    className="gap-2"
                    title="삭제"
                  >
                    <Trash2 className="w-4 h-4" style={{ color: 'var(--error)' }} />
                  </Button>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* Delete Confirmation Dialog */}
      <AlertDialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>강의를 삭제하시겠습니까?</AlertDialogTitle>
            <AlertDialogDescription>
              {lectureToDelete && (
                <>
                  <span className="block mb-2">"{lectureToDelete.title}"을(를) 삭제하려고 합니다.</span>
                  <span className="block text-sm" style={{ color: 'var(--error)' }}>
                    이 작업은 되돌릴 수 없습니다. 관련된 모�� 세션 데이터도 함께 삭제됩니다.
                  </span>
                </>
              )}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>취소</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDeleteConfirm}
              style={{ backgroundColor: 'var(--error)', color: 'white' }}
            >
              삭제
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}