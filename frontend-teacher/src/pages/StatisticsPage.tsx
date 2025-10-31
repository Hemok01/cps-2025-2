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
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
} from '@mui/material';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { Layout } from '../components/common/Layout';
import { sessionService } from '../services/sessionService';
import { dashboardService } from '../services/dashboardService';
import type { Lecture, LectureStatistics } from '../types';

export const StatisticsPage: React.FC = () => {
  const [lectures, setLectures] = useState<Lecture[]>([]);
  const [selectedLectureId, setSelectedLectureId] = useState<number | ''>('');
  const [statistics, setStatistics] = useState<LectureStatistics | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

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
      const data = await sessionService.getLectures();
      setLectures(data);
    } catch (err) {
      console.error('Failed to load lectures:', err);
      setError('강의 목록을 불러오는데 실패했습니다.');
    }
  };

  const loadStatistics = async () => {
    if (!selectedLectureId) return;

    try {
      setLoading(true);
      const data = await dashboardService.getLectureStatistics(selectedLectureId as number);
      setStatistics(data);
    } catch (err) {
      console.error('Failed to load statistics:', err);
      setError('통계를 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const getDifficultyChartData = () => {
    if (!statistics || !statistics.common_difficulties) return [];

    return statistics.common_difficulties.slice(0, 10).map((item) => ({
      name: item.subtask.guide_text.substring(0, 30) + '...',
      도움요청: item.help_count,
    }));
  };

  return (
    <Layout>
      <Box>
        <Typography variant="h4" gutterBottom>
          통계 및 분석
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

        {!loading && statistics && (
          <>
            {/* Summary Cards */}
            <Grid container spacing={3} sx={{ mb: 4 }}>
              <Grid item xs={12} sm={6} md={4}>
                <Card>
                  <CardContent>
                    <Typography color="textSecondary" gutterBottom>
                      강의명
                    </Typography>
                    <Typography variant="h6">{statistics.lecture_title}</Typography>
                  </CardContent>
                </Card>
              </Grid>
              <Grid item xs={12} sm={6} md={4}>
                <Card>
                  <CardContent>
                    <Typography color="textSecondary" gutterBottom>
                      총 학생 수
                    </Typography>
                    <Typography variant="h4">{statistics.total_students}</Typography>
                  </CardContent>
                </Card>
              </Grid>
              <Grid item xs={12} sm={6} md={4}>
                <Card>
                  <CardContent>
                    <Typography color="textSecondary" gutterBottom>
                      총 도움 요청
                    </Typography>
                    <Typography variant="h4">{statistics.total_help_requests}</Typography>
                  </CardContent>
                </Card>
              </Grid>
            </Grid>

            {/* Difficulty Chart */}
            {statistics.common_difficulties && statistics.common_difficulties.length > 0 && (
              <Paper sx={{ p: 3, mb: 3 }}>
                <Typography variant="h6" gutterBottom>
                  어려운 단계 분석 (상위 10개)
                </Typography>
                <Typography variant="body2" color="text.secondary" paragraph>
                  학생들이 가장 많이 도움을 요청한 단계입니다.
                </Typography>
                <ResponsiveContainer width="100%" height={400}>
                  <BarChart data={getDifficultyChartData()}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="name" angle={-45} textAnchor="end" height={150} />
                    <YAxis />
                    <Tooltip />
                    <Legend />
                    <Bar dataKey="도움요청" fill="#ff9800" />
                  </BarChart>
                </ResponsiveContainer>
              </Paper>
            )}

            {/* Difficulty Table */}
            {statistics.common_difficulties && statistics.common_difficulties.length > 0 && (
              <Paper>
                <Box p={2}>
                  <Typography variant="h6" gutterBottom>
                    상세 난이도 정보
                  </Typography>
                </Box>
                <TableContainer>
                  <Table>
                    <TableHead>
                      <TableRow>
                        <TableCell>순위</TableCell>
                        <TableCell>단계</TableCell>
                        <TableCell>목표 동작</TableCell>
                        <TableCell align="right">도움 요청 횟수</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {statistics.common_difficulties.map((item, index) => (
                        <TableRow key={item.subtask.id}>
                          <TableCell>{index + 1}</TableCell>
                          <TableCell>{item.subtask.guide_text}</TableCell>
                          <TableCell>
                            {item.subtask.target_action}
                          </TableCell>
                          <TableCell align="right">{item.help_count}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              </Paper>
            )}
          </>
        )}

        {!loading && selectedLectureId && !statistics && (
          <Alert severity="info">이 강의에 대한 통계가 없습니다.</Alert>
        )}
      </Box>
    </Layout>
  );
};
