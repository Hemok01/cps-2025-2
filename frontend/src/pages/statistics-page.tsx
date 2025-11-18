import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { apiService } from '../lib/api-service';
import { Lecture, LectureStatistics } from '../lib/types';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Label } from '../components/ui/label';
import { 
  Select, 
  SelectContent, 
  SelectItem, 
  SelectTrigger, 
  SelectValue 
} from '../components/ui/select';
import { 
  BarChart, 
  Bar, 
  XAxis, 
  YAxis, 
  CartesianGrid, 
  Tooltip, 
  Legend, 
  ResponsiveContainer,
  Cell
} from 'recharts';
import { Users, HelpCircle, TrendingUp, CheckCircle } from 'lucide-react';
import { toast } from 'sonner';

export function StatisticsPage() {
  const [searchParams] = useSearchParams();
  const preselectedLectureId = searchParams.get('lectureId');

  const [lectures, setLectures] = useState<Lecture[]>([]);
  const [selectedLectureId, setSelectedLectureId] = useState<string>(preselectedLectureId || '');
  const [statistics, setStatistics] = useState<LectureStatistics | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    loadLectures();
  }, []);

  useEffect(() => {
    if (selectedLectureId) {
      loadStatistics();
    }
  }, [selectedLectureId]);

  const loadLectures = async () => {
    try {
      const data = await apiService.getLectures();
      setLectures(data);
      if (data.length > 0 && !selectedLectureId) {
        setSelectedLectureId(data[0].id.toString());
      }
    } catch (error) {
      toast.error('강의 목록을 불러오는데 실패했습니다');
    }
  };

  const loadStatistics = async () => {
    if (!selectedLectureId) return;
    
    setLoading(true);
    try {
      const data = await apiService.getLectureStatistics(parseInt(selectedLectureId));
      setStatistics(data);
    } catch (error) {
      toast.error('통계를 불러오는데 실패했습니다');
    } finally {
      setLoading(false);
    }
  };

  const getBarColor = (value: number, maxValue: number) => {
    const percentage = (value / maxValue) * 100;
    if (percentage >= 75) return '#EF4444'; // red
    if (percentage >= 50) return '#F59E0B'; // orange
    if (percentage >= 25) return '#FCD34D'; // yellow
    return '#10B981'; // green
  };

  if (loading && !statistics) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-4">통계 로딩 중...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6 max-w-7xl">
      <div>
        <h1 className="text-3xl mb-2">통계 및 분석</h1>
        <p className="text-gray-600">강의별 통계 데이터와 어려운 단계를 확인하세요</p>
      </div>

      {/* Lecture Selection */}
      <Card>
        <CardContent className="pt-6">
          <div className="space-y-2">
            <Label htmlFor="lecture">강의 선택</Label>
            <Select value={selectedLectureId} onValueChange={setSelectedLectureId}>
              <SelectTrigger id="lecture">
                <SelectValue placeholder="강의를 선택하세요" />
              </SelectTrigger>
              <SelectContent>
                {lectures.map((lecture) => (
                  <SelectItem key={lecture.id} value={lecture.id.toString()}>
                    {lecture.title}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </CardContent>
      </Card>

      {statistics && (
        <>
          {/* Summary Cards */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            <Card>
              <CardContent className="pt-6">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm text-gray-600 mb-1">총 학생 수</p>
                    <p className="text-3xl">{statistics.totalStudents}명</p>
                  </div>
                  <div className="p-3 bg-blue-100 rounded-full">
                    <Users className="w-6 h-6 text-blue-600" />
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardContent className="pt-6">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm text-gray-600 mb-1">총 도움 요청</p>
                    <p className="text-3xl">{statistics.totalHelpRequests}회</p>
                  </div>
                  <div className="p-3 bg-red-100 rounded-full">
                    <HelpCircle className="w-6 h-6 text-red-600" />
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardContent className="pt-6">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm text-gray-600 mb-1">평균 진행률</p>
                    <p className="text-3xl">{statistics.averageProgress}%</p>
                  </div>
                  <div className="p-3 bg-green-100 rounded-full">
                    <TrendingUp className="w-6 h-6 text-green-600" />
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardContent className="pt-6">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm text-gray-600 mb-1">완료율</p>
                    <p className="text-3xl">{statistics.completionRate}%</p>
                    <p className="text-xs text-gray-500 mt-1">
                      {Math.round((statistics.completionRate / 100) * statistics.totalStudents)}/
                      {statistics.totalStudents}
                    </p>
                  </div>
                  <div className="p-3 bg-yellow-100 rounded-full">
                    <CheckCircle className="w-6 h-6 text-yellow-600" />
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Difficult Steps Chart */}
          <Card>
            <CardHeader>
              <CardTitle>어려운 단계 분석</CardTitle>
              <p className="text-sm text-gray-600">
                도움 요청이 많은 단계일수록 빨간색으로 표시됩니다
              </p>
            </CardHeader>
            <CardContent>
              {statistics.difficultSteps.length === 0 ? (
                <div className="text-center py-8 text-gray-500">
                  <p>데이터가 충분하지 않습니다</p>
                </div>
              ) : (
                <ResponsiveContainer width="100%" height={400}>
                  <BarChart data={statistics.difficultSteps}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis 
                      dataKey="subtaskName" 
                      angle={-45}
                      textAnchor="end"
                      height={120}
                      interval={0}
                      tick={{ fontSize: 12 }}
                    />
                    <YAxis 
                      label={{ value: '도움 요청 횟수', angle: -90, position: 'insideLeft' }}
                    />
                    <Tooltip 
                      content={({ active, payload }) => {
                        if (active && payload && payload.length) {
                          const data = payload[0].payload;
                          return (
                            <div className="bg-white p-3 border rounded shadow-lg">
                              <p className="mb-2">{data.subtaskName}</p>
                              <p className="text-sm">
                                <span className="text-gray-600">도움 요청:</span>{' '}
                                <span>{data.helpRequestCount}회</span>
                              </p>
                              <p className="text-sm">
                                <span className="text-gray-600">평균 소요 시간:</span>{' '}
                                <span>{Math.round(data.avgTimeSpent / 60)}분</span>
                              </p>
                              <p className="text-sm">
                                <span className="text-gray-600">학생 수:</span>{' '}
                                <span>{data.studentCount}명</span>
                              </p>
                            </div>
                          );
                        }
                        return null;
                      }}
                    />
                    <Bar dataKey="helpRequestCount" radius={[8, 8, 0, 0]}>
                      {statistics.difficultSteps.map((entry, index) => {
                        const maxCount = Math.max(...statistics.difficultSteps.map(s => s.helpRequestCount));
                        const color = getBarColor(entry.helpRequestCount, maxCount);
                        return <Cell key={`cell-${index}`} fill={color} />;
                      })}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              )}
            </CardContent>
          </Card>

          {/* Detailed Table */}
          <Card>
            <CardHeader>
              <CardTitle>상세 데이터</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead>
                    <tr className="border-b">
                      <th className="text-left p-3">단계명</th>
                      <th className="text-right p-3">도움 요청</th>
                      <th className="text-right p-3">평균 소요 시간</th>
                      <th className="text-right p-3">학생 수</th>
                    </tr>
                  </thead>
                  <tbody>
                    {statistics.difficultSteps.map((step, index) => (
                      <tr key={index} className="border-b hover:bg-gray-50">
                        <td className="p-3">{step.subtaskName}</td>
                        <td className="text-right p-3">{step.helpRequestCount}회</td>
                        <td className="text-right p-3">
                          {Math.round(step.avgTimeSpent / 60)}분
                        </td>
                        <td className="text-right p-3">{step.studentCount}명</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              
              <div className="mt-4 text-sm text-gray-500">
                마지막 업데이트: {new Date(statistics.lastUpdated).toLocaleString('ko-KR')}
              </div>
            </CardContent>
          </Card>
        </>
      )}
    </div>
  );
}
