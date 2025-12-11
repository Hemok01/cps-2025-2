import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { lectureService } from '../lib/lecture-service';
import { LectureStep, AvailableTask } from '../lib/lecture-types';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import { Textarea } from '../components/ui/textarea';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../components/ui/select';
import {
  ArrowLeft,
  Save,
  Smartphone,
  Loader2,
  Play,
  Plus,
  Trash2,
  ChevronUp,
  ChevronDown,
  CheckCircle2,
  AlertCircle,
  RefreshCw,
  BookOpen,
  Clock,
  Activity,
  ListChecks,
  FileVideo
} from 'lucide-react';
import { toast } from 'sonner';
import { Badge } from '../components/ui/badge';

export function LectureFormPage() {
  const navigate = useNavigate();
  const { lectureId } = useParams<{ lectureId: string }>();
  const isEditMode = !!lectureId;

  // Steps
  const [currentStep, setCurrentStep] = useState(1); // 1: Basic Info, 2: Recording, 3: Review Steps, 4: Confirm
  
  // Form data
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [difficulty, setDifficulty] = useState<'beginner' | 'intermediate' | 'advanced'>('beginner');
  const [duration, setDuration] = useState(60);
  
  // Content source tab (Task or Recording)
  const [contentSourceTab, setContentSourceTab] = useState<'task' | 'recording'>('task');

  // Task data (for Task-based lecture creation)
  const [availableTasks, setAvailableTasks] = useState<AvailableTask[]>([]);
  const [selectedTaskIds, setSelectedTaskIds] = useState<number[]>([]);
  const [loadingTasks, setLoadingTasks] = useState(false);

  // Recording data (for Recording-based lecture creation)
  const [recordings, setRecordings] = useState<{ id: string; name: string; createdAt: string; actionCount: number; duration: number; primaryApp: string; apps: string[]; deviceInfo?: { model?: string; androidVersion?: string } }[]>([]);
  const [selectedRecordingId, setSelectedRecordingId] = useState<string>('');
  const [loadingRecordings, setLoadingRecordings] = useState(false);
  const [processingRecording, setProcessingRecording] = useState(false);
  
  // Lecture steps
  const [lectureSteps, setLectureSteps] = useState<LectureStep[]>([]);
  const [editingStepId, setEditingStepId] = useState<string | null>(null);
  
  // General loading
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (isEditMode && lectureId) {
      loadLecture();
    }
  }, [lectureId]);

  const loadLecture = async () => {
    if (!lectureId) return;

    setLoading(true);
    try {
      const lecture = await lectureService.getLectureById(parseInt(lectureId));
      if (lecture) {
        setTitle(lecture.title);
        setDescription(lecture.description);
        setDifficulty(lecture.difficulty);
        setDuration(lecture.duration);
        setLectureSteps(lecture.steps || []);
        setSelectedRecordingId(lecture.recordingId || '');
        
        // Skip to review step for editing
        if (lecture.steps && lecture.steps.length > 0) {
          setCurrentStep(3);
        }
      } else {
        toast.error('강의를 찾을 수 없습니다');
        navigate('/lectures');
      }
    } catch (error) {
      toast.error('강의 정보를 불러오는데 실패했습니다');
      navigate('/lectures');
    } finally {
      setLoading(false);
    }
  };

  const loadRecordings = async () => {
    setLoadingRecordings(true);
    try {
      const data = await lectureService.getAvailableRecordings();
      setRecordings(data);
    } catch (error) {
      toast.error('녹화 목록을 불러오는데 실패했습니다');
    } finally {
      setLoadingRecordings(false);
    }
  };

  // Load available tasks (not linked to any lecture)
  const loadAvailableTasks = async () => {
    setLoadingTasks(true);
    try {
      const data = await lectureService.getAvailableTasks();
      setAvailableTasks(data);
    } catch (error) {
      toast.error('사용 가능한 과제 목록을 불러오는데 실패했습니다');
    } finally {
      setLoadingTasks(false);
    }
  };

  // Toggle task selection
  const handleTaskToggle = (taskId: number) => {
    setSelectedTaskIds(prev =>
      prev.includes(taskId)
        ? prev.filter(id => id !== taskId)
        : [...prev, taskId]
    );
  };

  // Load steps from selected tasks
  const handleLoadSelectedTaskSteps = () => {
    if (selectedTaskIds.length === 0) {
      toast.error('최소 1개 이상의 과제를 선택해주세요');
      return;
    }

    const allSteps: LectureStep[] = [];
    let order = 1;

    for (const taskId of selectedTaskIds) {
      const task = availableTasks.find(t => t.id === taskId);
      if (task && task.subtasks) {
        const steps = lectureService.convertSubtasksToSteps(task.subtasks);
        steps.forEach(step => {
          allSteps.push({ ...step, order: order++ });
        });
      }
    }

    if (allSteps.length === 0) {
      toast.error('선택된 과제에 단계가 없습니다');
      return;
    }

    setLectureSteps(allSteps);
    toast.success(`${allSteps.length}개의 단계가 로드되었습니다`);
    setCurrentStep(3);
  };

  const handleProcessRecording = async () => {
    if (!selectedRecordingId) {
      toast.error('녹화를 선택해주세요');
      return;
    }

    setProcessingRecording(true);
    try {
      const result = await lectureService.processRecording(selectedRecordingId);
      setLectureSteps(result.generatedSteps);
      toast.success(`${result.generatedSteps.length}개의 단계가 자동으로 생성되었습니다`);
      setCurrentStep(3); // Move to review steps
    } catch (error) {
      toast.error('녹화 처리에 실패했습니다');
    } finally {
      setProcessingRecording(false);
    }
  };

  const handleStepUpdate = (stepId: string, field: keyof LectureStep, value: string) => {
    setLectureSteps(prev => prev.map(step =>
      step.id === stepId ? { ...step, [field]: value } : step
    ));
  };

  const handleAddStep = () => {
    const newStep: LectureStep = {
      id: `step-${Date.now()}`,
      order: lectureSteps.length + 1,
      title: '',
      description: '',
      action: '',
      expectedResult: '',
    };
    setLectureSteps(prev => [...prev, newStep]);
    setEditingStepId(newStep.id);
  };

  const handleDeleteStep = (stepId: string) => {
    setLectureSteps(prev => {
      const filtered = prev.filter(s => s.id !== stepId);
      // Reorder
      return filtered.map((step, index) => ({ ...step, order: index + 1 }));
    });
  };

  const handleMoveStep = (stepId: string, direction: 'up' | 'down') => {
    const index = lectureSteps.findIndex(s => s.id === stepId);
    if (
      (direction === 'up' && index === 0) ||
      (direction === 'down' && index === lectureSteps.length - 1)
    ) {
      return;
    }

    const newSteps = [...lectureSteps];
    const targetIndex = direction === 'up' ? index - 1 : index + 1;
    
    // Swap
    [newSteps[index], newSteps[targetIndex]] = [newSteps[targetIndex], newSteps[index]];
    
    // Reorder
    const reordered = newSteps.map((step, idx) => ({ ...step, order: idx + 1 }));
    setLectureSteps(reordered);
  };

  const handleSubmit = async () => {
    // Validation
    if (!title.trim()) {
      toast.error('강의명을 입력해주세요');
      setCurrentStep(1);
      return;
    }

    if (!description.trim()) {
      toast.error('강의 설명을 입력해주세요');
      setCurrentStep(1);
      return;
    }

    if (lectureSteps.length === 0) {
      toast.error('최소 1개 이상의 단계가 필요합니다');
      setCurrentStep(2);
      return;
    }

    // Check if all steps have required fields
    const invalidStep = lectureSteps.find(
      step => !step.title.trim() || !step.action.trim() || !step.expectedResult.trim()
    );
    if (invalidStep) {
      toast.error('모든 단계의 필수 정보를 입력해주세요');
      setCurrentStep(3);
      return;
    }

    setLoading(true);
    try {
      const lectureData = {
        title,
        description,
        difficulty,
        duration,
        steps: lectureSteps,
        recordingId: selectedRecordingId || undefined,
      };

      if (isEditMode && lectureId) {
        await lectureService.updateLecture({
          id: parseInt(lectureId),
          ...lectureData,
        });
        toast.success('강의가 수정되었습니다');
      } else {
        // Task가 선택된 경우 함께 연결
        await lectureService.createLectureWithTasks(lectureData, selectedTaskIds);
        toast.success('강의가 추가되었습니다');
      }
      navigate('/lectures');
    } catch (error) {
      toast.error(isEditMode ? '강의 수정에 실패했습니다' : '강의 추가에 실패했습니다');
    } finally {
      setLoading(false);
    }
  };

  const renderStepIndicator = () => {
    const steps = [
      { num: 1, label: '기본 정보' },
      { num: 2, label: '내용 선택' },
      { num: 3, label: '단계 수정' },
      { num: 4, label: '최종 확인' },
    ];

    return (
      <div className="flex items-center justify-between mb-8">
        {steps.map((step, index) => (
          <div key={step.num} className="flex items-center flex-1">
            <div className="flex flex-col items-center flex-1">
              <div
                className={`w-10 h-10 rounded-full flex items-center justify-center mb-2 transition-colors ${
                  currentStep >= step.num
                    ? 'text-white'
                    : 'bg-gray-200'
                }`}
                style={{
                  backgroundColor: currentStep >= step.num ? 'var(--primary)' : undefined,
                  color: currentStep >= step.num ? 'white' : 'var(--text-secondary)',
                }}
              >
                {currentStep > step.num ? (
                  <CheckCircle2 className="w-5 h-5" />
                ) : (
                  step.num
                )}
              </div>
              <span
                className="text-sm text-center"
                style={{
                  color: currentStep >= step.num ? 'var(--primary)' : 'var(--text-secondary)',
                  fontWeight: currentStep === step.num ? 'bold' : 'normal',
                }}
              >
                {step.label}
              </span>
            </div>
            {index < steps.length - 1 && (
              <div
                className="h-0.5 flex-1 mx-2"
                style={{
                  backgroundColor: currentStep > step.num ? 'var(--primary)' : '#e0e0e0',
                }}
              />
            )}
          </div>
        ))}
      </div>
    );
  };

  const renderBasicInfo = () => (
    <Card style={{ borderRadius: 'var(--radius-lg)' }}>
      <CardHeader>
        <CardTitle>강의 기본 정보</CardTitle>
        <CardDescription>강의의 기본 정보를 입력합니다</CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        <div className="space-y-2">
          <Label htmlFor="title">
            강의명 <span style={{ color: 'var(--error)' }}>*</span>
          </Label>
          <Input
            id="title"
            placeholder="예: 유튜브 영상 검색하고 좋아요 누르기"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            required
          />
        </div>

        <div className="space-y-2">
          <Label htmlFor="description">
            강의 설명 <span style={{ color: 'var(--error)' }}>*</span>
          </Label>
          <Textarea
            id="description"
            placeholder="강의에 대한 자세한 설명을 입력하세요"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            rows={4}
            required
          />
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div className="space-y-2">
            <Label htmlFor="difficulty">
              난이도 <span style={{ color: 'var(--error)' }}>*</span>
            </Label>
            <Select
              value={difficulty}
              onValueChange={(value: 'beginner' | 'intermediate' | 'advanced') =>
                setDifficulty(value)
              }
            >
              <SelectTrigger id="difficulty">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="beginner">초급</SelectItem>
                <SelectItem value="intermediate">중급</SelectItem>
                <SelectItem value="advanced">고급</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <Label htmlFor="duration">
              강의 시간 (분) <span style={{ color: 'var(--error)' }}>*</span>
            </Label>
            <Input
              id="duration"
              type="number"
              min="15"
              max="240"
              step="15"
              placeholder="60"
              value={duration}
              onChange={(e) => setDuration(parseInt(e.target.value) || 60)}
              required
            />
          </div>
        </div>

        <div className="flex gap-3 pt-4">
          <Button
            type="button"
            variant="outline"
            onClick={() => navigate('/lectures')}
            className="flex-1"
          >
            취소
          </Button>
          <Button
            type="button"
            className="flex-1"
            style={{ backgroundColor: 'var(--primary)' }}
            onClick={() => {
              if (!title.trim() || !description.trim()) {
                toast.error('모든 필수 항목을 입력해주세요');
                return;
              }
              setCurrentStep(2);
              // Load tasks by default (Task tab is default)
              loadAvailableTasks();
            }}
          >
            다음
          </Button>
        </div>
      </CardContent>
    </Card>
  );

  // Task Selection Panel (Tab 1)
  const renderTaskSelection = () => (
    <>
      {loadingTasks ? (
        <div className="flex items-center justify-center py-12">
          <Loader2 className="w-8 h-8 animate-spin" style={{ color: 'var(--primary)' }} />
        </div>
      ) : availableTasks.length === 0 ? (
        <div className="text-center py-12">
          <ListChecks className="w-16 h-16 mx-auto mb-4 opacity-20" />
          <h3 className="text-xl mb-2">사용 가능한 과제가 없습니다</h3>
          <p style={{ color: 'var(--text-secondary)' }}>
            강의자 앱에서 녹화를 분석하여 과제를 먼저 생성해주세요
          </p>
          <Button
            variant="outline"
            onClick={loadAvailableTasks}
            className="mt-4 gap-2"
          >
            <RefreshCw className="w-4 h-4" />
            새로고침
          </Button>
        </div>
      ) : (
        <>
          <div className="space-y-3">
            {availableTasks.map((task) => (
              <div
                key={task.id}
                className={`p-4 border rounded-lg cursor-pointer transition-all ${
                  selectedTaskIds.includes(task.id)
                    ? 'border-2'
                    : 'hover:border-gray-400'
                }`}
                style={{
                  borderColor: selectedTaskIds.includes(task.id) ? 'var(--primary)' : undefined,
                }}
                onClick={() => handleTaskToggle(task.id)}
              >
                <div className="flex items-center justify-between mb-2">
                  <h4 className="text-lg font-medium">{task.title}</h4>
                  {selectedTaskIds.includes(task.id) && (
                    <CheckCircle2 className="w-6 h-6" style={{ color: 'var(--primary)' }} />
                  )}
                </div>

                {task.description && (
                  <p className="text-sm mb-2" style={{ color: 'var(--text-secondary)' }}>
                    {task.description}
                  </p>
                )}

                <div className="flex items-center gap-4 text-sm" style={{ color: 'var(--text-secondary)' }}>
                  <div className="flex items-center gap-1">
                    <ListChecks className="w-4 h-4" />
                    <span>{task.subtask_count}개 단계</span>
                  </div>
                  <div className="flex items-center gap-1">
                    <Clock className="w-4 h-4" />
                    <span>{new Date(task.created_at).toLocaleDateString('ko-KR')}</span>
                  </div>
                </div>
              </div>
            ))}
          </div>

          {selectedTaskIds.length > 0 && (
            <div
              className="p-4 rounded-lg flex items-start gap-3 mt-4"
              style={{ backgroundColor: '#E8F5E9' }}
            >
              <CheckCircle2 className="w-5 h-5 flex-shrink-0 mt-0.5" style={{ color: '#4CAF50' }} />
              <div className="text-sm">
                <p className="font-medium mb-1">
                  {selectedTaskIds.length}개의 과제가 선택되었습니다
                </p>
                <p style={{ color: 'var(--text-secondary)' }}>
                  선택한 과제들의 단계가 순서대로 병합되어 강의 단계로 추가됩니다.
                </p>
              </div>
            </div>
          )}
        </>
      )}
    </>
  );

  // Recording Selection Panel (Tab 2)
  const renderRecordingSelectionPanel = () => (
    <>
      {loadingRecordings ? (
        <div className="flex items-center justify-center py-12">
          <Loader2 className="w-8 h-8 animate-spin" style={{ color: 'var(--primary)' }} />
        </div>
      ) : recordings.length === 0 ? (
        <div className="text-center py-12">
          <Smartphone className="w-16 h-16 mx-auto mb-4 opacity-20" />
          <h3 className="text-xl mb-2">사용 가능한 녹화가 없습니다</h3>
          <p style={{ color: 'var(--text-secondary)' }}>
            핸드폰에서 동작을 녹화하고 서버로 전송해주세요
          </p>
          <Button
            variant="outline"
            onClick={loadRecordings}
            className="mt-4 gap-2"
          >
            <RefreshCw className="w-4 h-4" />
            새로고침
          </Button>
        </div>
      ) : (
        <>
          <div className="space-y-3">
            {recordings.map((recording) => {
              const formatDuration = (seconds: number) => {
                const mins = Math.floor(seconds / 60);
                const secs = seconds % 60;
                return `${mins}:${secs.toString().padStart(2, '0')}`;
              };

              return (
                <div
                  key={recording.id}
                  className={`p-4 border rounded-lg cursor-pointer transition-all ${
                    selectedRecordingId === recording.id
                      ? 'border-2'
                      : 'hover:border-gray-400'
                  }`}
                  style={{
                    borderColor: selectedRecordingId === recording.id ? 'var(--primary)' : undefined,
                  }}
                  onClick={() => setSelectedRecordingId(recording.id)}
                >
                  <div className="flex items-center justify-between mb-3">
                    <h4 className="text-lg">{recording.name}</h4>
                    {selectedRecordingId === recording.id && (
                      <CheckCircle2 className="w-6 h-6" style={{ color: 'var(--primary)' }} />
                    )}
                  </div>

                  <div className="grid grid-cols-3 gap-3 mb-2">
                    <div className="flex items-center gap-2 text-sm" style={{ color: 'var(--text-secondary)' }}>
                      <Clock className="w-4 h-4" />
                      <span>{formatDuration(recording.duration)}</span>
                    </div>
                    <div className="flex items-center gap-2 text-sm" style={{ color: 'var(--text-secondary)' }}>
                      <Activity className="w-4 h-4" />
                      <span>{recording.actionCount}개 동작</span>
                    </div>
                    <div className="flex items-center gap-2 text-sm" style={{ color: 'var(--text-secondary)' }}>
                      <Smartphone className="w-4 h-4" />
                      <span>{lectureService.getAppName(recording.primaryApp)}</span>
                    </div>
                  </div>

                  <p className="text-xs" style={{ color: 'var(--text-secondary)' }}>
                    {new Date(recording.createdAt).toLocaleString('ko-KR')}
                    {recording.deviceInfo && ` · ${recording.deviceInfo.model}`}
                  </p>
                </div>
              );
            })}
          </div>

          {selectedRecordingId && (
            <div
              className="p-4 rounded-lg flex items-start gap-3 mt-4"
              style={{ backgroundColor: '#E3F2FD' }}
            >
              <AlertCircle className="w-5 h-5 flex-shrink-0 mt-0.5" style={{ color: 'var(--primary)' }} />
              <div className="text-sm">
                <p className="mb-1">
                  선택한 녹화를 분석하여 자동으로 강의 단계를 생성합니다.
                </p>
                <p style={{ color: 'var(--text-secondary)' }}>
                  생성 후 각 단계를 검토하고 수정할 수 있습니다.
                </p>
              </div>
            </div>
          )}
        </>
      )}
    </>
  );

  // Main Content Source Selection (Step 2 with Tabs)
  const renderContentSourceSelection = () => (
    <Card style={{ borderRadius: 'var(--radius-lg)' }}>
      <CardHeader>
        <CardTitle>강의 내용 선택</CardTitle>
        <CardDescription>
          기존 과제를 선택하거나 새로운 녹화에서 강의 단계를 생성합니다
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        {/* Tab Buttons */}
        <div className="flex gap-2 border-b pb-4">
          <Button
            variant={contentSourceTab === 'task' ? 'default' : 'outline'}
            onClick={() => {
              setContentSourceTab('task');
              if (availableTasks.length === 0) {
                loadAvailableTasks();
              }
            }}
            className="gap-2"
            style={contentSourceTab === 'task' ? { backgroundColor: 'var(--primary)' } : {}}
          >
            <ListChecks className="w-4 h-4" />
            기존 과제 선택
          </Button>
          <Button
            variant={contentSourceTab === 'recording' ? 'default' : 'outline'}
            onClick={() => {
              setContentSourceTab('recording');
              if (recordings.length === 0) {
                loadRecordings();
              }
            }}
            className="gap-2"
            style={contentSourceTab === 'recording' ? { backgroundColor: 'var(--primary)' } : {}}
          >
            <FileVideo className="w-4 h-4" />
            녹화에서 생성
          </Button>
        </div>

        {/* Tab Content */}
        {contentSourceTab === 'task' && renderTaskSelection()}
        {contentSourceTab === 'recording' && renderRecordingSelectionPanel()}

        {/* Action Buttons */}
        <div className="flex gap-3 pt-4 border-t">
          <Button
            type="button"
            variant="outline"
            onClick={() => setCurrentStep(1)}
            className="flex-1"
          >
            이전
          </Button>

          {contentSourceTab === 'task' ? (
            <Button
              type="button"
              className="flex-1 gap-2"
              style={{ backgroundColor: 'var(--primary)' }}
              onClick={handleLoadSelectedTaskSteps}
              disabled={selectedTaskIds.length === 0}
            >
              <ListChecks className="w-4 h-4" />
              단계 로드
            </Button>
          ) : (
            <Button
              type="button"
              className="flex-1 gap-2"
              style={{ backgroundColor: 'var(--primary)' }}
              onClick={handleProcessRecording}
              disabled={!selectedRecordingId || processingRecording}
            >
              {processingRecording ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  분석 중...
                </>
              ) : (
                <>
                  <Play className="w-4 h-4" />
                  단계 자동 생성
                </>
              )}
            </Button>
          )}

          <Button
            type="button"
            variant="outline"
            className="gap-2"
            onClick={() => {
              setCurrentStep(3);
              if (lectureSteps.length === 0) {
                toast.info('직접 단계를 추가할 수 있습니다');
              }
            }}
          >
            건너뛰기
          </Button>
        </div>
      </CardContent>
    </Card>
  );

  const renderStepReview = () => (
    <Card style={{ borderRadius: 'var(--radius-lg)' }}>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div>
            <CardTitle>강의 단계 검토 및 수정</CardTitle>
            <CardDescription>
              자동 생성된 단계를 검토하고 수정합니다
            </CardDescription>
          </div>
          <Button
            variant="outline"
            onClick={handleAddStep}
            className="gap-2"
          >
            <Plus className="w-4 h-4" />
            단계 추가
          </Button>
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        {lectureSteps.length === 0 ? (
          <div className="text-center py-12">
            <BookOpen className="w-16 h-16 mx-auto mb-4 opacity-20" />
            <h3 className="text-xl mb-2">강의 단계가 없습니다</h3>
            <p className="mb-4" style={{ color: 'var(--text-secondary)' }}>
              새로운 단계를 추가해보세요
            </p>
            <Button
              onClick={handleAddStep}
              className="gap-2"
              style={{ backgroundColor: 'var(--primary)' }}
            >
              <Plus className="w-4 h-4" />
              첫 번째 단계 추가
            </Button>
          </div>
        ) : (
          <>
            {lectureSteps.map((step, index) => (
              <div
                key={step.id}
                className="border rounded-lg p-4"
                style={{ borderColor: editingStepId === step.id ? 'var(--primary)' : undefined }}
              >
                <div className="flex items-start gap-3">
                  <div className="flex flex-col gap-1">
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => handleMoveStep(step.id, 'up')}
                      disabled={index === 0}
                      className="h-6 w-6 p-0"
                    >
                      <ChevronUp className="w-4 h-4" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => handleMoveStep(step.id, 'down')}
                      disabled={index === lectureSteps.length - 1}
                      className="h-6 w-6 p-0"
                    >
                      <ChevronDown className="w-4 h-4" />
                    </Button>
                  </div>

                  <div className="flex-1 space-y-3">
                    <div className="flex items-center gap-2 mb-2">
                      <Badge style={{ backgroundColor: 'var(--primary)', color: 'white' }}>
                        단계 {step.order}
                      </Badge>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => handleDeleteStep(step.id)}
                        className="h-6 gap-1 text-red-600"
                      >
                        <Trash2 className="w-3 h-3" />
                        삭제
                      </Button>
                    </div>

                    <div className="space-y-2">
                      <Label>
                        단계 제목 <span style={{ color: 'var(--error)' }}>*</span>
                      </Label>
                      <Input
                        placeholder="예: 홈 화면에서 유튜브 앱 찾기"
                        value={step.title}
                        onChange={(e) => handleStepUpdate(step.id, 'title', e.target.value)}
                        onFocus={() => setEditingStepId(step.id)}
                      />
                    </div>

                    <div className="space-y-2">
                      <Label>설명</Label>
                      <Textarea
                        placeholder="이 단계에 대한 자세한 설명"
                        value={step.description}
                        onChange={(e) => handleStepUpdate(step.id, 'description', e.target.value)}
                        onFocus={() => setEditingStepId(step.id)}
                        rows={2}
                      />
                    </div>

                    <div className="space-y-2">
                      <Label>
                        수행할 액션 <span style={{ color: 'var(--error)' }}>*</span>
                      </Label>
                      <Input
                        placeholder="예: 빨간색 재생 버튼이 있는 유튜브 아이콘을 터치하세요"
                        value={step.action}
                        onChange={(e) => handleStepUpdate(step.id, 'action', e.target.value)}
                        onFocus={() => setEditingStepId(step.id)}
                      />
                    </div>

                    <div className="space-y-2">
                      <Label>
                        예상 결과 <span style={{ color: 'var(--error)' }}>*</span>
                      </Label>
                      <Input
                        placeholder="예: 유튜브 앱이 실행되고 메인 화면이 나타납니다"
                        value={step.expectedResult}
                        onChange={(e) => handleStepUpdate(step.id, 'expectedResult', e.target.value)}
                        onFocus={() => setEditingStepId(step.id)}
                      />
                    </div>

                    <div className="space-y-2">
                      <Label>추가 팁 (선택사항)</Label>
                      <Input
                        placeholder="학생들을 위한 추가 팁"
                        value={step.tips || ''}
                        onChange={(e) => handleStepUpdate(step.id, 'tips', e.target.value)}
                        onFocus={() => setEditingStepId(step.id)}
                      />
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </>
        )}

        <div className="flex gap-3 pt-4">
          <Button
            type="button"
            variant="outline"
            onClick={() => setCurrentStep(2)}
            className="flex-1"
          >
            이전
          </Button>
          <Button
            type="button"
            className="flex-1"
            style={{ backgroundColor: 'var(--primary)' }}
            onClick={() => {
              if (lectureSteps.length === 0) {
                toast.error('최소 1개 이상의 단계를 추가해주세요');
                return;
              }
              const invalidStep = lectureSteps.find(
                step => !step.title.trim() || !step.action.trim() || !step.expectedResult.trim()
              );
              if (invalidStep) {
                toast.error('모든 단계의 필수 정보를 입력해주세요');
                return;
              }
              setCurrentStep(4);
            }}
          >
            다음
          </Button>
        </div>
      </CardContent>
    </Card>
  );

  const renderConfirmation = () => (
    <Card style={{ borderRadius: 'var(--radius-lg)' }}>
      <CardHeader>
        <CardTitle>최종 확인</CardTitle>
        <CardDescription>강의 정보를 최종 확인하고 저장합니다</CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        <div className="space-y-4">
          <div>
            <Label className="text-sm" style={{ color: 'var(--text-secondary)' }}>
              강의명
            </Label>
            <p className="text-lg mt-1">{title}</p>
          </div>

          <div>
            <Label className="text-sm" style={{ color: 'var(--text-secondary)' }}>
              설명
            </Label>
            <p className="mt-1">{description}</p>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label className="text-sm" style={{ color: 'var(--text-secondary)' }}>
                난이도
              </Label>
              <p className="mt-1">
                {difficulty === 'beginner' ? '초급' : difficulty === 'intermediate' ? '중급' : '고급'}
              </p>
            </div>
            <div>
              <Label className="text-sm" style={{ color: 'var(--text-secondary)' }}>
                강의 시간
              </Label>
              <p className="mt-1">{duration}분</p>
            </div>
          </div>

          <div>
            <Label className="text-sm" style={{ color: 'var(--text-secondary)' }}>
              강의 단계
            </Label>
            <p className="mt-1">총 {lectureSteps.length}개 단계</p>
            <div className="mt-2 space-y-2">
              {lectureSteps.map((step) => (
                <div
                  key={step.id}
                  className="p-3 rounded-lg"
                  style={{ backgroundColor: 'var(--muted)' }}
                >
                  <div className="flex items-center gap-2 mb-1">
                    <Badge variant="outline">단계 {step.order}</Badge>
                    <span>{step.title}</span>
                  </div>
                  <p className="text-sm" style={{ color: 'var(--text-secondary)' }}>
                    {step.action}
                  </p>
                </div>
              ))}
            </div>
          </div>
        </div>

        <div className="flex gap-3 pt-4">
          <Button
            type="button"
            variant="outline"
            onClick={() => setCurrentStep(3)}
            className="flex-1"
            disabled={loading}
          >
            이전
          </Button>
          <Button
            type="button"
            className="flex-1 gap-2"
            style={{ backgroundColor: 'var(--primary)' }}
            onClick={handleSubmit}
            disabled={loading}
          >
            {loading ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                저장 중...
              </>
            ) : (
              <>
                <Save className="w-4 h-4" />
                {isEditMode ? '수정 완료' : '강의 추가'}
              </>
            )}
          </Button>
        </div>
      </CardContent>
    </Card>
  );

  if (loading && isEditMode) {
    return (
      <div className="flex items-center justify-center min-h-96">
        <div className="text-center">
          <Loader2 className="w-12 h-12 animate-spin mx-auto mb-4" style={{ color: 'var(--primary)' }} />
          <p>강의 정보 로딩 중...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      {/* Header */}
      <div>
        <Button
          variant="ghost"
          onClick={() => navigate('/lectures')}
          className="gap-2 mb-4"
        >
          <ArrowLeft className="w-4 h-4" />
          강의 목록으로
        </Button>
        <h1 className="text-3xl mb-2">
          {isEditMode ? '강의 수정' : '새 강의 추가'}
        </h1>
        <p style={{ color: 'var(--text-secondary)' }}>
          {isEditMode ? '강의 정보를 수정하세요' : '핸드폰 동작 녹화를 바탕으로 강의를 생성하세요'}
        </p>
      </div>

      {/* Step Indicator */}
      {renderStepIndicator()}

      {/* Step Content */}
      {currentStep === 1 && renderBasicInfo()}
      {currentStep === 2 && renderContentSourceSelection()}
      {currentStep === 3 && renderStepReview()}
      {currentStep === 4 && renderConfirmation()}
    </div>
  );
}