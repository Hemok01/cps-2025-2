import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { apiService } from '../lib/api-service';
import { SessionSummary } from '../lib/types';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Progress } from '../components/ui/progress';
import {
  Users,
  Clock,
  CheckCircle,
  HelpCircle,
  AlertTriangle,
  ArrowLeft,
  BarChart3,
  Trophy,
  TrendingUp,
} from 'lucide-react';
import { toast } from 'sonner';

export function SessionSummaryPage() {
  const { sessionId } = useParams<{ sessionId: string }>();
  const navigate = useNavigate();
  const [summary, setSummary] = useState<SessionSummary | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (sessionId) {
      loadSummary();
    }
  }, [sessionId]);

  const loadSummary = async () => {
    if (!sessionId) return;

    setLoading(true);
    try {
      const data = await apiService.getSessionSummary(parseInt(sessionId));
      setSummary(data);
    } catch (error: any) {
      if (error?.response?.status === 403) {
        toast.error('이 세션의 요약을 조회할 권한이 없습니다');
      } else {
        toast.error('세션 요약을 불러오는데 실패했습니다');
      }
    } finally {
      setLoading(false);
    }
  };

  // 시간 포맷
  const formatDuration = (seconds: number) => {
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);

    if (hours > 0) {
      return `${hours}시간 ${minutes}분`;
    }
    return `${minutes}분`;
  };

  // 상태별 색상
  const getStatusColor = (status: string) => {
    switch (status) {
      case 'COMPLETED':
        return 'text-green-600 bg-green-100';
      case 'ACTIVE':
        return 'text-blue-600 bg-blue-100';
      case 'WAITING':
        return 'text-gray-600 bg-gray-100';
      default:
        return 'text-gray-600 bg-gray-100';
    }
  };

  const getStatusLabel = (status: string) => {
    switch (status) {
      case 'COMPLETED':
        return '완료';
      case 'ACTIVE':
        return '진행중';
      case 'WAITING':
        return '대기';
      case 'DISCONNECTED':
        return '연결끊김';
      default:
        return status;
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-4 text-gray-600">요약 로딩 중...</p>
        </div>
      </div>
    );
  }

  if (!summary) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <AlertTriangle className="w-16 h-16 mx-auto mb-4 text-gray-300" />
          <h3 className="text-lg font-medium text-gray-700 mb-2">요약을 불러올 수 없습니다</h3>
          <Button onClick={() => navigate('/sessions')} className="gap-2 mt-4">
            <ArrowLeft className="w-4 h-4" />
            세션 목록으로
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="max-w-5xl mx-auto px-4">
        {/* 헤더 */}
        <div className="mb-8">
          <Button
            variant="ghost"
            onClick={() => navigate('/sessions')}
            className="gap-2 mb-4"
          >
            <ArrowLeft className="w-4 h-4" />
            세션 목록으로
          </Button>

          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-3xl font-bold text-gray-900 flex items-center gap-3">
                <Trophy className="w-8 h-8 text-yellow-500" />
                수업 요약
              </h1>
              <p className="text-gray-600 mt-1">
                {summary.sessionTitle} • {summary.lectureName}
              </p>
            </div>
            <div className="text-right">
              <p className="text-sm text-gray-500">세션 코드</p>
              <p className="text-xl font-mono font-bold">{summary.sessionCode}</p>
            </div>
          </div>
        </div>

        {/* 주요 지표 카드 */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
          <Card>
            <CardContent className="pt-6">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-gray-600 mb-1">참가자</p>
                  <p className="text-3xl font-bold">{summary.totalParticipants}명</p>
                  <p className="text-xs text-gray-500 mt-1">
                    완료: {summary.completedParticipants}명
                  </p>
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
                  <p className="text-sm text-gray-600 mb-1">완료율</p>
                  <p className="text-3xl font-bold">{summary.completionRate}%</p>
                  <p className="text-xs text-gray-500 mt-1">
                    평균 진행률: {summary.avgProgress}%
                  </p>
                </div>
                <div className="p-3 bg-green-100 rounded-full">
                  <CheckCircle className="w-6 h-6 text-green-600" />
                </div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="pt-6">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-gray-600 mb-1">수업 시간</p>
                  <p className="text-3xl font-bold">{formatDuration(summary.durationSeconds)}</p>
                  <p className="text-xs text-gray-500 mt-1">
                    {summary.totalSteps}개 단계
                  </p>
                </div>
                <div className="p-3 bg-purple-100 rounded-full">
                  <Clock className="w-6 h-6 text-purple-600" />
                </div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="pt-6">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-gray-600 mb-1">도움 요청</p>
                  <p className="text-3xl font-bold">{summary.totalHelpRequests}회</p>
                  <p className="text-xs text-gray-500 mt-1">
                    해결률: {summary.helpResolutionRate}%
                  </p>
                </div>
                <div className="p-3 bg-orange-100 rounded-full">
                  <HelpCircle className="w-6 h-6 text-orange-600" />
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
          {/* 어려운 단계 */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <AlertTriangle className="w-5 h-5 text-yellow-500" />
                어려운 단계 TOP 5
              </CardTitle>
            </CardHeader>
            <CardContent>
              {summary.difficultSteps.length === 0 ? (
                <p className="text-gray-500 text-center py-4">
                  도움 요청이 없었습니다
                </p>
              ) : (
                <div className="space-y-3">
                  {summary.difficultSteps.map((step, index) => (
                    <div
                      key={step.subtaskId}
                      className="flex items-center justify-between p-3 bg-gray-50 rounded-lg"
                    >
                      <div className="flex items-center gap-3">
                        <span className="w-6 h-6 rounded-full bg-yellow-100 text-yellow-600 flex items-center justify-center text-sm font-medium">
                          {index + 1}
                        </span>
                        <span className="text-sm font-medium">{step.subtaskName}</span>
                      </div>
                      <span className="text-sm text-orange-600 font-medium">
                        {step.helpRequestCount}회
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>

          {/* 통계 링크 */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <TrendingUp className="w-5 h-5 text-blue-500" />
                다음 단계
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <p className="text-gray-600">
                이 세션의 결과를 바탕으로 강의를 개선해보세요.
              </p>

              <div className="space-y-3">
                <Button
                  variant="outline"
                  className="w-full justify-start gap-2"
                  onClick={() => navigate(`/statistics?lectureId=${summary.lectureId}`)}
                >
                  <BarChart3 className="w-4 h-4" />
                  강의 통계 상세 보기
                </Button>

                <Button
                  variant="outline"
                  className="w-full justify-start gap-2"
                  onClick={() => navigate('/sessions')}
                >
                  <Users className="w-4 h-4" />
                  새 세션 시작하기
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* 참가자 목록 */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Users className="w-5 h-5 text-blue-500" />
              참가자 상세
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b bg-gray-50">
                    <th className="text-left p-3 font-medium">이름</th>
                    <th className="text-center p-3 font-medium">상태</th>
                    <th className="text-center p-3 font-medium">진행률</th>
                    <th className="text-center p-3 font-medium">완료 단계</th>
                  </tr>
                </thead>
                <tbody>
                  {summary.participants.map((participant) => (
                    <tr key={participant.id} className="border-b hover:bg-gray-50">
                      <td className="p-3 font-medium">{participant.name}</td>
                      <td className="text-center p-3">
                        <span
                          className={`px-2 py-1 rounded text-xs font-medium ${getStatusColor(
                            participant.status
                          )}`}
                        >
                          {getStatusLabel(participant.status)}
                        </span>
                      </td>
                      <td className="p-3">
                        <div className="flex items-center gap-2">
                          <Progress
                            value={participant.progressRate}
                            className="h-2 flex-1"
                          />
                          <span className="text-sm text-gray-600 w-12 text-right">
                            {participant.progressRate}%
                          </span>
                        </div>
                      </td>
                      <td className="text-center p-3 text-gray-600">
                        {participant.completedCount} / {participant.totalSteps}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </CardContent>
        </Card>

        {/* 시간 정보 */}
        <div className="mt-6 text-center text-sm text-gray-500">
          {summary.startedAt && (
            <p>
              시작: {new Date(summary.startedAt).toLocaleString('ko-KR')}
              {summary.endedAt && (
                <> • 종료: {new Date(summary.endedAt).toLocaleString('ko-KR')}</>
              )}
            </p>
          )}
        </div>
      </div>
    </div>
  );
}
