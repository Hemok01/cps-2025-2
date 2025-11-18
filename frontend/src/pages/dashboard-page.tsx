import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiService } from '../lib/api-service';
import { Lecture } from '../lib/types';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { Play, Users, BookOpen, BarChart3 } from 'lucide-react';

export function DashboardPage() {
  const [lectures, setLectures] = useState<Lecture[]>([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setLoading(true);
    try {
      const lecturesData = await apiService.getLectures();
      setLectures(lecturesData);
    } catch (error) {
      console.error('Failed to load dashboard data:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-4">데이터 로딩 중...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl mb-2">환영합니다! 👋</h1>
        <p className="text-gray-600">오늘도 즐거운 강의 되세요</p>
      </div>

      {/* Quick Actions */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Card 
          className="cursor-pointer hover:shadow-lg transition-shadow"
          onClick={() => navigate('/sessions')}
          style={{ borderRadius: 'var(--radius-lg)' }}
        >
          <CardContent className="p-6 flex items-center gap-4">
            <div 
              className="p-3 rounded-full" 
              style={{ backgroundColor: '#E8F5E9' }}
            >
              <Play className="w-6 h-6" style={{ color: 'var(--success)' }} />
            </div>
            <div>
              <h3 className="mb-1">세션 시작</h3>
              <p className="text-sm" style={{ color: 'var(--text-secondary)' }}>새로운 학습 세션을 시작하세요</p>
            </div>
          </CardContent>
        </Card>

        <Card 
          className="cursor-pointer hover:shadow-lg transition-shadow"
          onClick={() => navigate('/lectures')}
          style={{ borderRadius: 'var(--radius-lg)' }}
        >
          <CardContent className="p-6 flex items-center gap-4">
            <div 
              className="p-3 rounded-full" 
              style={{ backgroundColor: 'var(--accent)' }}
            >
              <BookOpen className="w-6 h-6" style={{ color: 'var(--primary)' }} />
            </div>
            <div>
              <h3 className="mb-1">강의 관리</h3>
              <p className="text-sm" style={{ color: 'var(--text-secondary)' }}>강의를 추가하고 관리하세요</p>
            </div>
          </CardContent>
        </Card>

        <Card 
          className="cursor-pointer hover:shadow-lg transition-shadow"
          onClick={() => navigate('/statistics')}
          style={{ borderRadius: 'var(--radius-lg)' }}
        >
          <CardContent className="p-6 flex items-center gap-4">
            <div 
              className="p-3 rounded-full" 
              style={{ backgroundColor: '#FFF3E0' }}
            >
              <BarChart3 className="w-6 h-6" style={{ color: 'var(--warning)' }} />
            </div>
            <div>
              <h3 className="mb-1">통계</h3>
              <p className="text-sm" style={{ color: 'var(--text-secondary)' }}>학습 데이터와 인사이트 확인</p>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Lectures Grid */}
      <div>
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-2xl">내 강의</h2>
          <Button
            variant="outline"
            onClick={() => navigate('/lectures')}
            className="gap-2"
          >
            <BookOpen className="w-4 h-4" />
            전체 관리
          </Button>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {lectures.map((lecture) => (
            <Card 
              key={lecture.id} 
              className="hover:shadow-lg transition-shadow"
              style={{ borderRadius: 'var(--radius-lg)' }}
            >
              <CardHeader>
                <div className="flex items-start justify-between">
                  <BookOpen className="w-8 h-8" style={{ color: 'var(--primary)' }} />
                  {lecture.isActive && (
                    <Badge 
                      variant="secondary" 
                      style={{ 
                        backgroundColor: '#E8F5E9',
                        color: 'var(--success)'
                      }}
                    >
                      활성
                    </Badge>
                  )}
                </div>
                <CardTitle className="mt-2">{lecture.title}</CardTitle>
                <CardDescription>{lecture.description}</CardDescription>
              </CardHeader>
              <CardContent>
                <div className="space-y-2 mb-4">
                  <div className="flex items-center gap-2 text-sm" style={{ color: 'var(--text-secondary)' }}>
                    <Users className="w-4 h-4" />
                    <span>학생 {lecture.studentCount}명</span>
                  </div>
                  <div className="flex items-center gap-2 text-sm" style={{ color: 'var(--text-secondary)' }}>
                    <Play className="w-4 h-4" />
                    <span>세션 {lecture.sessionCount}개</span>
                  </div>
                </div>
                <div className="flex gap-2">
                  <Button
                    size="sm"
                    variant="outline"
                    className="flex-1"
                    onClick={() => navigate(`/statistics?lectureId=${lecture.id}`)}
                  >
                    통계 보기
                  </Button>
                  <Button
                    size="sm"
                    className="flex-1"
                    onClick={() => navigate(`/sessions?lectureId=${lecture.id}`)}
                    style={{ backgroundColor: 'var(--primary)' }}
                  >
                    세션 시작
                  </Button>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      </div>
    </div>
  );
}