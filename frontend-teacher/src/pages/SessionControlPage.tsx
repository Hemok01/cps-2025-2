import React, { useState, useEffect } from 'react';
import {
  Box,
  Grid,
  Card,
  CardContent,
  Typography,
  Button,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Alert,
  Chip,
  Paper,
  Divider,
  TextField,
} from '@mui/material';
import {
  PlayArrow,
  Pause,
  Stop,
  SkipNext,
  ContentCopy,
} from '@mui/icons-material';
import { Layout } from '../components/common/Layout';
import { sessionService } from '../services/sessionService';
import type { Lecture, LectureSession, SessionParticipant } from '../types';

export const SessionControlPage: React.FC = () => {
  const [lectures, setLectures] = useState<Lecture[]>([]);
  const [selectedLectureId, setSelectedLectureId] = useState<number | ''>('');
  const [sessionTitle, setSessionTitle] = useState('');
  const [currentSession, setCurrentSession] = useState<LectureSession | null>(null);
  const [participants, setParticipants] = useState<SessionParticipant[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    loadLectures();
  }, []);

  useEffect(() => {
    if (currentSession) {
      loadParticipants();
      const interval = setInterval(loadParticipants, 5000); // Refresh every 5 seconds
      return () => clearInterval(interval);
    }
  }, [currentSession]);

  const loadLectures = async () => {
    try {
      const data = await sessionService.getLectures();
      setLectures(data.filter((l) => l.is_active));
    } catch (err) {
      console.error('Failed to load lectures:', err);
      setError('강의 목록을 불러오는데 실패했습니다.');
    }
  };

  const loadParticipants = async () => {
    if (!currentSession) return;
    try {
      const data = await sessionService.getSessionParticipants(currentSession.id);
      setParticipants(data.filter((p) => p.is_active));
    } catch (err) {
      console.error('Failed to load participants:', err);
    }
  };

  const handleCreateSession = async () => {
    if (!selectedLectureId || !sessionTitle.trim()) {
      setError('강의와 세션 제목을 모두 입력해주세요.');
      return;
    }

    try {
      setLoading(true);
      setError('');
      const response = await sessionService.createSession(selectedLectureId as number, sessionTitle);
      setCurrentSession(response);
      setSuccess(`세션이 생성되었습니다! 세션 코드: ${response.session_code}`);
      setSessionTitle(''); // Reset title after successful creation
    } catch (err: any) {
      console.error('Failed to create session:', err);
      setError('세션 생성에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleStartSession = async () => {
    if (!currentSession) return;

    try {
      setLoading(true);
      const updated = await sessionService.startSession(currentSession.id);
      setCurrentSession(updated);
      setSuccess('세션이 시작되었습니다!');
    } catch (err) {
      setError('세션 시작에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handlePauseSession = async () => {
    if (!currentSession) return;

    try {
      setLoading(true);
      const updated = await sessionService.pauseSession(currentSession.id);
      setCurrentSession(updated);
      setSuccess('세션이 일시정지되었습니다.');
    } catch (err) {
      setError('세션 일시정지에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleResumeSession = async () => {
    if (!currentSession) return;

    try {
      setLoading(true);
      const updated = await sessionService.resumeSession(currentSession.id);
      setCurrentSession(updated);
      setSuccess('세션이 재개되었습니다.');
    } catch (err) {
      setError('세션 재개에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleNextStep = async () => {
    if (!currentSession) return;

    try {
      setLoading(true);
      const updated = await sessionService.nextStep(currentSession.id);
      setCurrentSession(updated);
      setSuccess('다음 단계로 이동했습니다.');
    } catch (err: any) {
      if (err.response?.data?.detail) {
        setError(err.response.data.detail);
      } else {
        setError('다음 단계로 이동하는데 실패했습니다.');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleEndSession = async () => {
    if (!currentSession) return;
    if (!confirm('세션을 종료하시겠습니까?')) return;

    try {
      setLoading(true);
      const updated = await sessionService.endSession(currentSession.id);
      setCurrentSession(updated);
      setSuccess('세션이 종료되었습니다.');
    } catch (err) {
      setError('세션 종료에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const copySessionCode = () => {
    if (currentSession) {
      navigator.clipboard.writeText(currentSession.session_code);
      setSuccess('세션 코드가 복사되었습니다!');
    }
  };

  const getStatusChip = (status: string) => {
    const statusMap: Record<string, { label: string; color: any }> = {
      WAITING: { label: '대기 중', color: 'default' },
      IN_PROGRESS: { label: '진행 중', color: 'success' },
      ENDED: { label: '종료됨', color: 'error' },
      REVIEW_MODE: { label: '검토 모드', color: 'info' },
    };
    const statusInfo = statusMap[status] || { label: status, color: 'default' };
    return <Chip label={statusInfo.label} color={statusInfo.color} />;
  };

  return (
    <Layout>
      <Box>
        <Typography variant="h4" gutterBottom>
          세션 제어
        </Typography>

        {error && (
          <Alert severity="error" onClose={() => setError('')} sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        {success && (
          <Alert severity="success" onClose={() => setSuccess('')} sx={{ mb: 2 }}>
            {success}
          </Alert>
        )}

        <Grid container spacing={3}>
          {/* Session Creation */}
          <Grid item xs={12} md={6}>
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom>
                  새 세션 만들기
                </Typography>
                <FormControl fullWidth sx={{ mb: 2 }}>
                  <InputLabel>강의 선택</InputLabel>
                  <Select
                    value={selectedLectureId}
                    onChange={(e) => setSelectedLectureId(e.target.value as number)}
                    label="강의 선택"
                  >
                    {lectures.map((lecture) => (
                      <MenuItem key={lecture.id} value={lecture.id}>
                        {lecture.title}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
                <TextField
                  fullWidth
                  label="세션 제목"
                  value={sessionTitle}
                  onChange={(e) => setSessionTitle(e.target.value)}
                  placeholder="예: 2025년 1학기 1차 수업"
                  sx={{ mb: 2 }}
                  disabled={loading || !!currentSession}
                />
                <Button
                  fullWidth
                  variant="contained"
                  onClick={handleCreateSession}
                  disabled={!selectedLectureId || !sessionTitle.trim() || loading || !!currentSession}
                >
                  세션 생성
                </Button>
              </CardContent>
            </Card>
          </Grid>

          {/* Current Session Info */}
          {currentSession && (
            <Grid item xs={12} md={6}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom>
                    현재 세션
                  </Typography>
                  <Box sx={{ mb: 2 }}>
                    <Box display="flex" alignItems="center" gap={1} mb={1}>
                      <Typography variant="h4">{currentSession.session_code}</Typography>
                      <Button size="small" onClick={copySessionCode} startIcon={<ContentCopy />}>
                        복사
                      </Button>
                    </Box>
                    {getStatusChip(currentSession.status)}
                  </Box>
                  <Divider sx={{ my: 2 }} />
                  <Typography variant="body2" color="text.secondary">
                    참가 학생: {participants.length}명
                  </Typography>
                  {currentSession.current_subtask_details && (
                    <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                      현재 단계: {currentSession.current_subtask_details.guide_text}
                    </Typography>
                  )}
                </CardContent>
              </Card>
            </Grid>
          )}

          {/* Session Controls */}
          {currentSession && (
            <Grid item xs={12}>
              <Paper sx={{ p: 3 }}>
                <Typography variant="h6" gutterBottom>
                  세션 제어
                </Typography>
                <Grid container spacing={2}>
                  <Grid item xs={12} sm={6} md={3}>
                    <Button
                      fullWidth
                      variant="contained"
                      color="success"
                      startIcon={<PlayArrow />}
                      onClick={handleStartSession}
                      disabled={currentSession.status !== 'WAITING' || loading}
                    >
                      시작
                    </Button>
                  </Grid>
                  <Grid item xs={12} sm={6} md={3}>
                    <Button
                      fullWidth
                      variant="contained"
                      color="warning"
                      startIcon={<Pause />}
                      onClick={handlePauseSession}
                      disabled={currentSession.status !== 'IN_PROGRESS' || loading}
                    >
                      일시정지
                    </Button>
                  </Grid>
                  <Grid item xs={12} sm={6} md={3}>
                    <Button
                      fullWidth
                      variant="contained"
                      color="info"
                      startIcon={<PlayArrow />}
                      onClick={handleResumeSession}
                      disabled={currentSession.status === 'WAITING' || currentSession.status === 'IN_PROGRESS' || currentSession.status === 'ENDED' || loading}
                    >
                      재개
                    </Button>
                  </Grid>
                  <Grid item xs={12} sm={6} md={3}>
                    <Button
                      fullWidth
                      variant="contained"
                      color="primary"
                      startIcon={<SkipNext />}
                      onClick={handleNextStep}
                      disabled={currentSession.status !== 'IN_PROGRESS' || loading}
                    >
                      다음 단계
                    </Button>
                  </Grid>
                  <Grid item xs={12}>
                    <Button
                      fullWidth
                      variant="outlined"
                      color="error"
                      startIcon={<Stop />}
                      onClick={handleEndSession}
                      disabled={currentSession.status === 'ENDED' || loading}
                    >
                      세션 종료
                    </Button>
                  </Grid>
                </Grid>
              </Paper>
            </Grid>
          )}

          {/* Participants List */}
          {currentSession && participants.length > 0 && (
            <Grid item xs={12}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom>
                    참가 학생 ({participants.length}명)
                  </Typography>
                  <Grid container spacing={1}>
                    {participants.map((participant) => (
                      <Grid item xs={12} sm={6} md={4} key={participant.id}>
                        <Chip label={participant.user_name} variant="outlined" />
                      </Grid>
                    ))}
                  </Grid>
                </CardContent>
              </Card>
            </Grid>
          )}
        </Grid>
      </Box>
    </Layout>
  );
};
