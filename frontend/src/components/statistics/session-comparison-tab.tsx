import { useEffect, useState } from 'react';
import { apiService } from '../../lib/api-service';
import { SessionComparisonData, SessionTrendItem, TrendDirection } from '../../lib/types';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from 'recharts';
import {
  TrendingUp,
  TrendingDown,
  Minus,
  AlertCircle,
  Users,
  Clock,
  HelpCircle,
  CheckCircle,
  Calendar,
} from 'lucide-react';
import { toast } from 'sonner';

interface SessionComparisonTabProps {
  lectureId: number | null;
  loading: boolean;
}

export function SessionComparisonTab({ lectureId }: SessionComparisonTabProps) {
  const [data, setData] = useState<SessionComparisonData | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (lectureId) {
      loadData();
    }
  }, [lectureId]);

  const loadData = async () => {
    if (!lectureId) return;

    setLoading(true);
    try {
      const result = await apiService.getSessionTrends(lectureId);
      setData(result);
    } catch (error) {
      toast.error('세션 비교 데이터를 불러오는데 실패했습니다');
    } finally {
      setLoading(false);
    }
  };

  // 추세 방향에 따른 아이콘과 스타일
  const getTrendInfo = (trend: TrendDirection) => {
    switch (trend) {
      case 'improving':
        return {
          icon: TrendingUp,
          color: 'text-green-600',
          bgColor: 'bg-green-100',
          label: '개선 중',
          description: '이전 세션보다 좋아지고 있습니다',
        };
      case 'declining':
        return {
          icon: TrendingDown,
          color: 'text-red-600',
          bgColor: 'bg-red-100',
          label: '악화 중',
          description: '이전 세션보다 나빠지고 있습니다',
        };
      case 'stable':
        return {
          icon: Minus,
          color: 'text-blue-600',
          bgColor: 'bg-blue-100',
          label: '유지 중',
          description: '안정적인 수준을 유지하고 있습니다',
        };
      case 'insufficient_data':
      default:
        return {
          icon: AlertCircle,
          color: 'text-gray-500',
          bgColor: 'bg-gray-100',
          label: '데이터 부족',
          description: '비교할 세션 데이터가 부족합니다',
        };
    }
  };

  // 날짜 포맷
  const formatDate = (dateString: string | null) => {
    if (!dateString) return '-';
    return new Date(dateString).toLocaleDateString('ko-KR', {
      month: 'short',
      day: 'numeric',
    });
  };

  // 시간 포맷
  const formatTime = (seconds: number) => {
    if (seconds < 60) return `${seconds}초`;
    if (seconds < 3600) return `${Math.round(seconds / 60)}분`;
    return `${Math.round(seconds / 3600)}시간 ${Math.round((seconds % 3600) / 60)}분`;
  };

  // 차트용 데이터 변환
  const chartData = data?.sessions.map((session, index) => ({
    name: session.sessionTitle || `세션 ${index + 1}`,
    date: formatDate(session.sessionDate),
    completionRate: Math.round(session.completionRate * 100),
    helpRequestRate: Math.round(session.helpRequestRate * 100),
    participantCount: session.participantCount,
    avgCompletionTime: session.avgCompletionTime,
  })) || [];

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-4">세션 비교 데이터 로딩 중...</p>
        </div>
      </div>
    );
  }

  if (!lectureId) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="text-center text-gray-500">
          <p>강의를 선택해주세요</p>
        </div>
      </div>
    );
  }

  if (!data || data.sessions.length === 0) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="text-center text-gray-500">
          <Calendar className="w-16 h-16 mx-auto mb-4 text-gray-300" />
          <p className="text-lg font-medium">비교할 세션이 없습니다</p>
          <p className="text-sm mt-2">세션을 진행하면 비교 데이터가 생성됩니다.</p>
        </div>
      </div>
    );
  }

  // 추세 정보
  const completionTrend = getTrendInfo(data.trendSummary.completionRateTrend);
  const helpRequestTrend = getTrendInfo(data.trendSummary.helpRequestTrend);
  const timeTrend = getTrendInfo(data.trendSummary.avgCompletionTimeTrend);

  return (
    <div className="space-y-6">
      {/* 추세 요약 카드 */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Card>
          <CardContent className="pt-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-600 mb-1">완료율 추이</p>
                <p className={`text-lg font-medium ${completionTrend.color}`}>
                  {completionTrend.label}
                </p>
                <p className="text-xs text-gray-500 mt-1">
                  {completionTrend.description}
                </p>
              </div>
              <div className={`p-3 rounded-full ${completionTrend.bgColor}`}>
                <completionTrend.icon className={`w-6 h-6 ${completionTrend.color}`} />
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="pt-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-600 mb-1">도움 요청 추이</p>
                <p className={`text-lg font-medium ${helpRequestTrend.color}`}>
                  {helpRequestTrend.label}
                </p>
                <p className="text-xs text-gray-500 mt-1">
                  {helpRequestTrend.description}
                </p>
              </div>
              <div className={`p-3 rounded-full ${helpRequestTrend.bgColor}`}>
                <helpRequestTrend.icon className={`w-6 h-6 ${helpRequestTrend.color}`} />
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="pt-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-600 mb-1">소요 시간 추이</p>
                <p className={`text-lg font-medium ${timeTrend.color}`}>
                  {timeTrend.label}
                </p>
                <p className="text-xs text-gray-500 mt-1">
                  {timeTrend.description}
                </p>
              </div>
              <div className={`p-3 rounded-full ${timeTrend.bgColor}`}>
                <timeTrend.icon className={`w-6 h-6 ${timeTrend.color}`} />
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* 추이 라인 차트 */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <TrendingUp className="w-5 h-5 text-green-500" />
            세션별 성과 추이
          </CardTitle>
          <p className="text-sm text-gray-600">
            세션별 완료율과 도움 요청률의 변화를 확인할 수 있습니다
          </p>
        </CardHeader>
        <CardContent>
          {chartData.length >= 2 ? (
            <ResponsiveContainer width="100%" height={350}>
              <LineChart
                data={chartData}
                margin={{ top: 5, right: 30, left: 20, bottom: 5 }}
              >
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis
                  dataKey="name"
                  tick={{ fontSize: 12 }}
                  interval={0}
                  angle={-45}
                  textAnchor="end"
                  height={80}
                />
                <YAxis
                  domain={[0, 100]}
                  tickFormatter={(v) => `${v}%`}
                />
                <Tooltip
                  content={({ active, payload, label }) => {
                    if (active && payload && payload.length) {
                      const item = payload[0].payload;
                      return (
                        <div className="bg-white p-4 border rounded-lg shadow-lg">
                          <p className="font-medium mb-2">{label}</p>
                          <p className="text-xs text-gray-500 mb-3">{item.date}</p>
                          <div className="space-y-1 text-sm">
                            <div className="flex justify-between gap-4">
                              <span className="text-gray-600">완료율:</span>
                              <span className="font-medium text-green-600">{item.completionRate}%</span>
                            </div>
                            <div className="flex justify-between gap-4">
                              <span className="text-gray-600">도움 요청률:</span>
                              <span className="font-medium text-orange-600">{item.helpRequestRate}%</span>
                            </div>
                            <div className="flex justify-between gap-4">
                              <span className="text-gray-600">참가자:</span>
                              <span>{item.participantCount}명</span>
                            </div>
                            <div className="flex justify-between gap-4">
                              <span className="text-gray-600">평균 소요:</span>
                              <span>{formatTime(item.avgCompletionTime)}</span>
                            </div>
                          </div>
                        </div>
                      );
                    }
                    return null;
                  }}
                />
                <Legend />
                <Line
                  type="monotone"
                  dataKey="completionRate"
                  name="완료율"
                  stroke="#10B981"
                  strokeWidth={2}
                  dot={{ fill: '#10B981', strokeWidth: 2, r: 4 }}
                  activeDot={{ r: 6 }}
                />
                <Line
                  type="monotone"
                  dataKey="helpRequestRate"
                  name="도움 요청률"
                  stroke="#F59E0B"
                  strokeWidth={2}
                  dot={{ fill: '#F59E0B', strokeWidth: 2, r: 4 }}
                  activeDot={{ r: 6 }}
                />
              </LineChart>
            </ResponsiveContainer>
          ) : (
            <div className="text-center py-12 text-gray-500">
              <AlertCircle className="w-12 h-12 mx-auto mb-3 text-gray-300" />
              <p>차트를 표시하려면 최소 2개 이상의 세션이 필요합니다</p>
            </div>
          )}
        </CardContent>
      </Card>

      {/* 세션 상세 테이블 */}
      <Card>
        <CardHeader>
          <CardTitle>세션별 상세 데이터</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b bg-gray-50">
                  <th className="text-left p-3 font-medium">세션명</th>
                  <th className="text-center p-3 font-medium">
                    <div className="flex items-center justify-center gap-1">
                      <Calendar className="w-4 h-4" />
                      날짜
                    </div>
                  </th>
                  <th className="text-center p-3 font-medium">
                    <div className="flex items-center justify-center gap-1">
                      <Users className="w-4 h-4" />
                      참가자
                    </div>
                  </th>
                  <th className="text-center p-3 font-medium">
                    <div className="flex items-center justify-center gap-1">
                      <CheckCircle className="w-4 h-4" />
                      완료율
                    </div>
                  </th>
                  <th className="text-center p-3 font-medium">
                    <div className="flex items-center justify-center gap-1">
                      <Clock className="w-4 h-4" />
                      평균 소요
                    </div>
                  </th>
                  <th className="text-center p-3 font-medium">
                    <div className="flex items-center justify-center gap-1">
                      <HelpCircle className="w-4 h-4" />
                      도움 요청
                    </div>
                  </th>
                </tr>
              </thead>
              <tbody>
                {data.sessions.map((session, index) => (
                  <tr key={session.sessionId} className="border-b hover:bg-gray-50">
                    <td className="p-3">
                      <div className="font-medium">{session.sessionTitle || `세션 ${index + 1}`}</div>
                    </td>
                    <td className="text-center p-3 text-gray-600">
                      {session.sessionDate
                        ? new Date(session.sessionDate).toLocaleDateString('ko-KR')
                        : '-'}
                    </td>
                    <td className="text-center p-3">
                      <span className="inline-flex items-center gap-1">
                        <Users className="w-4 h-4 text-gray-400" />
                        {session.participantCount}명
                      </span>
                    </td>
                    <td className="text-center p-3">
                      <span
                        className={`px-2 py-1 rounded text-sm ${
                          session.completionRate >= 0.8
                            ? 'bg-green-100 text-green-700'
                            : session.completionRate >= 0.5
                            ? 'bg-yellow-100 text-yellow-700'
                            : 'bg-red-100 text-red-700'
                        }`}
                      >
                        {Math.round(session.completionRate * 100)}%
                      </span>
                    </td>
                    <td className="text-center p-3 text-gray-600">
                      {formatTime(session.avgCompletionTime)}
                    </td>
                    <td className="text-center p-3">
                      <span className="inline-flex items-center gap-1">
                        {session.totalHelpRequests}회
                        <span className="text-gray-400 text-xs">
                          ({Math.round(session.helpRequestRate * 100)}%)
                        </span>
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="mt-4 text-sm text-gray-500">
            마지막 업데이트: {new Date(data.lastUpdated).toLocaleString('ko-KR')}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
