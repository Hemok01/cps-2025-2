import React, { useState, useEffect } from 'react';
import {
  Box,
  Grid,
  Card,
  CardContent,
  Typography,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Alert,
  CircularProgress,
  Chip,
  LinearProgress,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
} from '@mui/material';
import {
  CheckCircle,
  Error,
  HelpOutline,
  HourglassEmpty,
} from '@mui/icons-material';
import { Layout } from '../components/common/Layout';
import { sessionService } from '../services/sessionService';
import { dashboardService } from '../services/dashboardService';
import { DashboardWebSocketService } from '../services/websocketService';
import type { Lecture, StudentProgress, ProgressStatus } from '../types';

export const MonitoringPage: React.FC = () => {
  const [lectures, setLectures] = useState<Lecture[]>([]);
  const [selectedLectureId, setSelectedLectureId] = useState<number | ''>('');
  const [students, setStudents] = useState<StudentProgress[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [wsService, setWsService] = useState<DashboardWebSocketService | null>(null);

  useEffect(() => {
    loadLectures();
    return () => {
      if (wsService) {
        wsService.disconnect();
      }
    };
  }, []);

  useEffect(() => {
    if (selectedLectureId) {
      loadStudents();
      connectWebSocket();
    } else {
      if (wsService) {
        wsService.disconnect();
        setWsService(null);
      }
    }
  }, [selectedLectureId]);

  const loadLectures = async () => {
    try {
      const data = await sessionService.getLectures();
      setLectures(data.filter((l) => l.is_active));
    } catch (err) {
      console.error('Failed to load lectures:', err);
      setError('강의 목록을 불러오는데 실패했습니다.');
    }
  };

  const loadStudents = async () => {
    if (!selectedLectureId) return;

    try {
      setLoading(true);
      const data = await dashboardService.getLectureStudents(selectedLectureId as number);
      setStudents(data);
    } catch (err) {
      console.error('Failed to load students:', err);
      setError('학생 데이터를 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const connectWebSocket = () => {
    if (!selectedLectureId) return;

    // Disconnect previous WebSocket if exists
    if (wsService) {
      wsService.disconnect();
    }

    const ws = new DashboardWebSocketService(selectedLectureId as number);
    ws.connect()
      .then(() => {
        console.log('Dashboard WebSocket connected');
        ws.onMessage((message) => {
          console.log('Received message:', message);
          if (message.type === 'progress_update') {
            // Update student progress in real-time
            loadStudents();
          } else if (message.type === 'help_request') {
            // Reload to show new help request
            loadStudents();
          }
        });
      })
      .catch((err) => {
        console.error('Failed to connect WebSocket:', err);
      });

    setWsService(ws);
  };

  const getStatusIcon = (status: ProgressStatus) => {
    switch (status) {
      case 'COMPLETED':
        return <CheckCircle color="success" />;
      case 'IN_PROGRESS':
        return <HourglassEmpty color="info" />;
      case 'HELP_NEEDED':
        return <HelpOutline color="warning" />;
      default:
        return <Error color="disabled" />;
    }
  };

  const getStatusChip = (status: ProgressStatus) => {
    const statusMap: Record<ProgressStatus, { label: string; color: any }> = {
      NOT_STARTED: { label: '미시작', color: 'default' },
      IN_PROGRESS: { label: '진행 중', color: 'info' },
      COMPLETED: { label: '완료', color: 'success' },
      HELP_NEEDED: { label: '도움 필요', color: 'warning' },
    };
    const statusInfo = statusMap[status];
    return <Chip label={statusInfo.label} color={statusInfo.color} size="small" />;
  };

  return (
    <Layout>
      <Box>
        <Typography variant="h4" gutterBottom>
          학생 모니터링
        </Typography>

        {error && (
          <Alert severity="error" onClose={() => setError('')} sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        <FormControl fullWidth sx={{ mb: 3 }}>
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

        {loading && (
          <Box display="flex" justifyContent="center" my={4}>
            <CircularProgress />
          </Box>
        )}

        {!loading && selectedLectureId && students.length === 0 && (
          <Alert severity="info">이 강의에 등록된 학생이 없습니다.</Alert>
        )}

        {!loading && students.length > 0 && (
          <>
            {/* Summary Cards */}
            <Grid container spacing={2} sx={{ mb: 3 }}>
              <Grid item xs={12} sm={6} md={3}>
                <Card>
                  <CardContent>
                    <Typography color="textSecondary" gutterBottom variant="caption">
                      전체 학생
                    </Typography>
                    <Typography variant="h4">{students.length}</Typography>
                  </CardContent>
                </Card>
              </Grid>
              <Grid item xs={12} sm={6} md={3}>
                <Card>
                  <CardContent>
                    <Typography color="textSecondary" gutterBottom variant="caption">
                      완료
                    </Typography>
                    <Typography variant="h4" color="success.main">
                      {students.filter((s) => s.status === 'COMPLETED').length}
                    </Typography>
                  </CardContent>
                </Card>
              </Grid>
              <Grid item xs={12} sm={6} md={3}>
                <Card>
                  <CardContent>
                    <Typography color="textSecondary" gutterBottom variant="caption">
                      진행 중
                    </Typography>
                    <Typography variant="h4" color="info.main">
                      {students.filter((s) => s.status === 'IN_PROGRESS').length}
                    </Typography>
                  </CardContent>
                </Card>
              </Grid>
              <Grid item xs={12} sm={6} md={3}>
                <Card>
                  <CardContent>
                    <Typography color="textSecondary" gutterBottom variant="caption">
                      도움 필요
                    </Typography>
                    <Typography variant="h4" color="warning.main">
                      {students.filter((s) => s.status === 'HELP_NEEDED').length}
                    </Typography>
                  </CardContent>
                </Card>
              </Grid>
            </Grid>

            {/* Students Table */}
            <TableContainer component={Paper}>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>상태</TableCell>
                    <TableCell>학생 이름</TableCell>
                    <TableCell>이메일</TableCell>
                    <TableCell>진행률</TableCell>
                    <TableCell>현재 단계</TableCell>
                    <TableCell>도움 요청</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {students.map((student) => (
                    <TableRow key={student.user_id}>
                      <TableCell>{getStatusIcon(student.status)}</TableCell>
                      <TableCell>{student.user_name}</TableCell>
                      <TableCell>{student.user_email}</TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <LinearProgress
                            variant="determinate"
                            value={student.progress_rate}
                            sx={{ width: 100 }}
                          />
                          <Typography variant="caption">
                            {Math.round(student.progress_rate)}%
                          </Typography>
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Box>
                          {getStatusChip(student.status)}
                          {student.current_subtask && (
                            <Typography variant="caption" display="block" sx={{ mt: 0.5 }}>
                              {student.current_subtask.guide_text}
                            </Typography>
                          )}
                        </Box>
                      </TableCell>
                      <TableCell>
                        {student.help_request_count > 0 && (
                          <Chip
                            label={`${student.help_request_count}회`}
                            color="warning"
                            size="small"
                          />
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </>
        )}
      </Box>
    </Layout>
  );
};
