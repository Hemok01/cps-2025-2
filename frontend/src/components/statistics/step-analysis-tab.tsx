import { useEffect, useState } from 'react';
import { apiService } from '../../lib/api-service';
import { StepAnalysisData, StepAnalysisItem } from '../../lib/types';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Cell,
} from 'recharts';
import { AlertTriangle, Clock, HelpCircle, TrendingDown } from 'lucide-react';
import { toast } from 'sonner';

interface StepAnalysisTabProps {
  lectureId: number | null;
  loading: boolean;
}

export function StepAnalysisTab({ lectureId }: StepAnalysisTabProps) {
  const [data, setData] = useState<StepAnalysisData | null>(null);
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
      const result = await apiService.getStepAnalysis(lectureId);
      setData(result);
    } catch (error) {
      toast.error('단계별 분석 데이터를 불러오는데 실패했습니다');
    } finally {
      setLoading(false);
    }
  };

  // 병목 점수에 따른 색상
  const getHeatmapColor = (score: number) => {
    if (score >= 0.75) return '#EF4444'; // red-500
    if (score >= 0.5) return '#F59E0B'; // amber-500
    if (score >= 0.25) return '#FCD34D'; // yellow-400
    return '#10B981'; // emerald-500
  };

  // 시간 포맷
  const formatTime = (seconds: number) => {
    if (seconds < 60) return `${seconds}초`;
    if (seconds < 3600) return `${Math.round(seconds / 60)}분`;
    return `${Math.round(seconds / 3600)}시간 ${Math.round((seconds % 3600) / 60)}분`;
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-4">단계별 분석 로딩 중...</p>
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

  if (!data || data.stepAnalysis.length === 0) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="text-center text-gray-500">
          <AlertTriangle className="w-16 h-16 mx-auto mb-4 text-gray-300" />
          <p className="text-lg font-medium">분석할 데이터가 없습니다</p>
          <p className="text-sm mt-2">학생들이 강의를 진행하면 분석 데이터가 생성됩니다.</p>
        </div>
      </div>
    );
  }

  // 병목 점수로 정렬된 상위 단계
  const sortedByBottleneck = [...data.stepAnalysis]
    .sort((a, b) => b.bottleneckScore - a.bottleneckScore)
    .slice(0, 10);

  return (
    <div className="space-y-6">
      {/* 요약 카드 */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Card>
          <CardContent className="pt-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-600 mb-1">가장 어려운 단계</p>
                <p className="text-lg font-medium truncate" title={data.summary.mostHelpRequestedStep || '-'}>
                  {data.summary.mostHelpRequestedStep || '-'}
                </p>
              </div>
              <div className="p-3 bg-red-100 rounded-full">
                <AlertTriangle className="w-6 h-6 text-red-600" />
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="pt-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-600 mb-1">가장 지체되는 단계</p>
                <p className="text-lg font-medium truncate" title={data.summary.mostDelayedStep || '-'}>
                  {data.summary.mostDelayedStep || '-'}
                </p>
              </div>
              <div className="p-3 bg-yellow-100 rounded-full">
                <Clock className="w-6 h-6 text-yellow-600" />
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="pt-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-600 mb-1">평균 지체율</p>
                <p className="text-3xl font-bold">
                  {Math.round(data.summary.avgOverallDelayRate * 100)}%
                </p>
              </div>
              <div className="p-3 bg-orange-100 rounded-full">
                <TrendingDown className="w-6 h-6 text-orange-600" />
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* 병목 히트맵 차트 */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <AlertTriangle className="w-5 h-5 text-yellow-500" />
            단계별 병목 분석
          </CardTitle>
          <p className="text-sm text-gray-600">
            병목 점수가 높을수록 개선이 필요한 단계입니다 (빨강 → 노랑 → 초록)
          </p>
        </CardHeader>
        <CardContent>
          <ResponsiveContainer width="100%" height={400}>
            <BarChart
              data={sortedByBottleneck}
              layout="vertical"
              margin={{ top: 5, right: 30, left: 150, bottom: 5 }}
            >
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis
                type="number"
                domain={[0, 1]}
                tickFormatter={(v) => `${Math.round(v * 100)}%`}
              />
              <YAxis
                type="category"
                dataKey="subtaskName"
                width={140}
                tick={{ fontSize: 12 }}
              />
              <Tooltip
                content={({ active, payload }) => {
                  if (active && payload && payload.length) {
                    const item = payload[0].payload as StepAnalysisItem;
                    return (
                      <div className="bg-white p-4 border rounded-lg shadow-lg max-w-xs">
                        <p className="font-medium mb-2">{item.subtaskName}</p>
                        <p className="text-xs text-gray-500 mb-3">{item.taskName}</p>
                        <div className="space-y-1 text-sm">
                          <div className="flex justify-between">
                            <span className="text-gray-600">병목 점수:</span>
                            <span className="font-medium" style={{ color: getHeatmapColor(item.bottleneckScore) }}>
                              {Math.round(item.bottleneckScore * 100)}%
                            </span>
                          </div>
                          <div className="flex justify-between">
                            <span className="text-gray-600">지체율:</span>
                            <span>{Math.round(item.delayRate * 100)}%</span>
                          </div>
                          <div className="flex justify-between">
                            <span className="text-gray-600">도움 요청:</span>
                            <span>{item.helpRequestCount}회</span>
                          </div>
                          <div className="flex justify-between">
                            <span className="text-gray-600">평균 소요:</span>
                            <span>{formatTime(item.avgTimeSpent)}</span>
                          </div>
                          <div className="flex justify-between">
                            <span className="text-gray-600">완료율:</span>
                            <span>{Math.round(item.completionRate * 100)}%</span>
                          </div>
                        </div>
                      </div>
                    );
                  }
                  return null;
                }}
              />
              <Bar dataKey="bottleneckScore" radius={[0, 4, 4, 0]}>
                {sortedByBottleneck.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={getHeatmapColor(entry.bottleneckScore)} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>

          {/* 범례 */}
          <div className="flex justify-center gap-6 mt-4 text-sm">
            <div className="flex items-center gap-2">
              <div className="w-4 h-4 rounded" style={{ backgroundColor: '#EF4444' }}></div>
              <span>심각 (75%+)</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="w-4 h-4 rounded" style={{ backgroundColor: '#F59E0B' }}></div>
              <span>주의 (50-75%)</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="w-4 h-4 rounded" style={{ backgroundColor: '#FCD34D' }}></div>
              <span>보통 (25-50%)</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="w-4 h-4 rounded" style={{ backgroundColor: '#10B981' }}></div>
              <span>양호 (25% 미만)</span>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* 상세 테이블 */}
      <Card>
        <CardHeader>
          <CardTitle>상세 데이터</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b bg-gray-50">
                  <th className="text-left p-3 font-medium">단계명</th>
                  <th className="text-right p-3 font-medium">평균 소요</th>
                  <th className="text-right p-3 font-medium">지체율</th>
                  <th className="text-right p-3 font-medium">도움 요청</th>
                  <th className="text-right p-3 font-medium">완료율</th>
                  <th className="text-right p-3 font-medium">병목 점수</th>
                </tr>
              </thead>
              <tbody>
                {data.stepAnalysis.map((step, index) => (
                  <tr key={index} className="border-b hover:bg-gray-50">
                    <td className="p-3">
                      <div className="font-medium">{step.subtaskName}</div>
                      <div className="text-xs text-gray-500">{step.taskName}</div>
                    </td>
                    <td className="text-right p-3">{formatTime(step.avgTimeSpent)}</td>
                    <td className="text-right p-3">
                      <span
                        className={`px-2 py-1 rounded text-sm ${
                          step.delayRate >= 0.5
                            ? 'bg-red-100 text-red-700'
                            : step.delayRate >= 0.25
                            ? 'bg-yellow-100 text-yellow-700'
                            : 'bg-green-100 text-green-700'
                        }`}
                      >
                        {Math.round(step.delayRate * 100)}%
                      </span>
                    </td>
                    <td className="text-right p-3">
                      <span className="inline-flex items-center gap-1">
                        <HelpCircle className="w-4 h-4 text-gray-400" />
                        {step.helpRequestCount}회
                      </span>
                    </td>
                    <td className="text-right p-3">{Math.round(step.completionRate * 100)}%</td>
                    <td className="text-right p-3">
                      <div
                        className="inline-flex items-center justify-center w-12 h-6 rounded text-white text-sm font-medium"
                        style={{ backgroundColor: getHeatmapColor(step.bottleneckScore) }}
                      >
                        {Math.round(step.bottleneckScore * 100)}
                      </div>
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
