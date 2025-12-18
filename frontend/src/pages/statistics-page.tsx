import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { apiService } from '../lib/api-service';
import { Lecture, LectureStatistics } from '../lib/types';
import { Card, CardContent } from '../components/ui/card';
import { Label } from '../components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue
} from '../components/ui/select';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../components/ui/tabs';
import { OverviewTab, StepAnalysisTab, SessionComparisonTab } from '../components/statistics';
import { BarChart3, AlertTriangle, TrendingUp } from 'lucide-react';
import { toast } from 'sonner';

type TabValue = 'overview' | 'step-analysis' | 'session-comparison';

export function StatisticsPage() {
  const [searchParams] = useSearchParams();
  const preselectedLectureId = searchParams.get('lectureId');

  // 공통 상태
  const [lectures, setLectures] = useState<Lecture[]>([]);
  const [selectedLectureId, setSelectedLectureId] = useState<string>(preselectedLectureId || '');
  const [activeTab, setActiveTab] = useState<TabValue>('overview');

  // 탭별 데이터 (Lazy Loading)
  const [overviewData, setOverviewData] = useState<LectureStatistics | null>(null);
  // TODO: Phase 3, 5에서 추가될 타입
  // const [stepAnalysisData, setStepAnalysisData] = useState<StepAnalysisData | null>(null);
  // const [sessionComparisonData, setSessionComparisonData] = useState<SessionComparisonData | null>(null);

  // 탭별 로딩 상태
  const [loadingStates, setLoadingStates] = useState({
    overview: false,
    stepAnalysis: false,
    sessionComparison: false,
  });

  // 강의 목록 로드
  useEffect(() => {
    loadLectures();
  }, []);

  // 강의 선택 시 현재 탭 데이터 로드
  useEffect(() => {
    if (selectedLectureId) {
      loadCurrentTabData();
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

  const loadCurrentTabData = async () => {
    if (!selectedLectureId) return;

    switch (activeTab) {
      case 'overview':
        await loadOverviewData();
        break;
      case 'step-analysis':
        // TODO: Phase 3에서 구현
        break;
      case 'session-comparison':
        // TODO: Phase 5에서 구현
        break;
    }
  };

  const loadOverviewData = async () => {
    if (!selectedLectureId) return;

    setLoadingStates(prev => ({ ...prev, overview: true }));
    try {
      const data = await apiService.getLectureStatistics(parseInt(selectedLectureId));
      setOverviewData(data);
    } catch (error: any) {
      if (error?.response?.status === 403) {
        toast.error('이 강의의 통계를 조회할 권한이 없습니다');
      } else {
        toast.error('통계를 불러오는데 실패했습니다');
      }
      setOverviewData(null);
    } finally {
      setLoadingStates(prev => ({ ...prev, overview: false }));
    }
  };

  // 탭 변경 핸들러
  const handleTabChange = async (value: string) => {
    const tabValue = value as TabValue;
    setActiveTab(tabValue);

    if (!selectedLectureId) return;

    // Lazy Loading: 해당 탭 데이터가 없으면 로드
    switch (tabValue) {
      case 'overview':
        if (!overviewData) {
          await loadOverviewData();
        }
        break;
      case 'step-analysis':
        // TODO: Phase 3에서 구현
        break;
      case 'session-comparison':
        // TODO: Phase 5에서 구현
        break;
    }
  };

  // 강의 변경 핸들러
  const handleLectureChange = (lectureId: string) => {
    setSelectedLectureId(lectureId);
    // 모든 탭 데이터 초기화
    setOverviewData(null);
    // TODO: 다른 탭 데이터도 초기화
    // setStepAnalysisData(null);
    // setSessionComparisonData(null);
  };

  return (
    <div className="space-y-6 max-w-7xl">
      {/* 페이지 헤더 */}
      <div>
        <h1 className="text-3xl mb-2">통계 및 분석</h1>
        <p className="text-gray-600">강의별 통계 데이터와 개선 인사이트를 확인하세요</p>
      </div>

      {/* 강의 선택 */}
      <Card>
        <CardContent className="pt-6">
          <div className="space-y-2">
            <Label htmlFor="lecture">강의 선택</Label>
            <Select value={selectedLectureId} onValueChange={handleLectureChange}>
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

      {/* 탭 구조 */}
      <Tabs value={activeTab} onValueChange={handleTabChange} className="w-full">
        <TabsList className="grid w-full grid-cols-3">
          <TabsTrigger value="overview" className="flex items-center gap-2">
            <BarChart3 className="w-4 h-4" />
            <span>개요</span>
          </TabsTrigger>
          <TabsTrigger value="step-analysis" className="flex items-center gap-2">
            <AlertTriangle className="w-4 h-4" />
            <span>단계별 분석</span>
          </TabsTrigger>
          <TabsTrigger value="session-comparison" className="flex items-center gap-2">
            <TrendingUp className="w-4 h-4" />
            <span>세션 비교</span>
          </TabsTrigger>
        </TabsList>

        <TabsContent value="overview" className="mt-6">
          <OverviewTab
            statistics={overviewData}
            loading={loadingStates.overview}
            lectureId={selectedLectureId ? parseInt(selectedLectureId) : null}
          />
        </TabsContent>

        <TabsContent value="step-analysis" className="mt-6">
          <StepAnalysisTab
            lectureId={selectedLectureId ? parseInt(selectedLectureId) : null}
            loading={loadingStates.stepAnalysis}
          />
        </TabsContent>

        <TabsContent value="session-comparison" className="mt-6">
          <SessionComparisonTab
            lectureId={selectedLectureId ? parseInt(selectedLectureId) : null}
            loading={loadingStates.sessionComparison}
          />
        </TabsContent>
      </Tabs>
    </div>
  );
}
