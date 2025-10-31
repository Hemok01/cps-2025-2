import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Grid,
  Card,
  CardContent,
  Typography,
  Button,
  CircularProgress,
  Alert,
  Chip,
} from '@mui/material';
import {
  PlayArrow as PlayArrowIcon,
  People as PeopleIcon,
  HelpOutline as HelpIcon,
  School as SchoolIcon,
} from '@mui/icons-material';
import { Layout } from '../components/common/Layout';
import { sessionService } from '../services/sessionService';
import { helpService } from '../services/helpService';
import type { Lecture, HelpRequest } from '../types';

export const DashboardPage: React.FC = () => {
  const [lectures, setLectures] = useState<Lecture[]>([]);
  const [pendingHelp, setPendingHelp] = useState<HelpRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      const [lecturesData, helpData] = await Promise.all([
        sessionService.getLectures(),
        helpService.getPendingHelpRequests(),
      ]);
      setLectures(lecturesData);
      setPendingHelp(helpData);
    } catch (err: any) {
      console.error('Failed to load dashboard data:', err);
      setError('데이터를 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <Layout>
        <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
          <CircularProgress />
        </Box>
      </Layout>
    );
  }

  return (
    <Layout>
      <Box>
        <Typography variant="h4" gutterBottom>
          대시보드
        </Typography>

        {error && (
          <Alert severity="error" sx={{ mb: 3 }}>
            {error}
          </Alert>
        )}

        {/* Summary Cards */}
        <Grid container spacing={3} sx={{ mb: 4 }}>
          <Grid item xs={12} sm={6} md={3}>
            <Card>
              <CardContent>
                <Box display="flex" alignItems="center" justifyContent="space-between">
                  <Box>
                    <Typography color="textSecondary" gutterBottom>
                      내 강의
                    </Typography>
                    <Typography variant="h4">{lectures.length}</Typography>
                  </Box>
                  <SchoolIcon sx={{ fontSize: 48, color: 'primary.main' }} />
                </Box>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} sm={6} md={3}>
            <Card>
              <CardContent>
                <Box display="flex" alignItems="center" justifyContent="space-between">
                  <Box>
                    <Typography color="textSecondary" gutterBottom>
                      대기 중 도움 요청
                    </Typography>
                    <Typography variant="h4">{pendingHelp.length}</Typography>
                  </Box>
                  <HelpIcon sx={{ fontSize: 48, color: 'warning.main' }} />
                </Box>
              </CardContent>
            </Card>
          </Grid>
        </Grid>

        {/* Quick Actions */}
        <Box sx={{ mb: 4 }}>
          <Typography variant="h5" gutterBottom>
            빠른 작업
          </Typography>
          <Grid container spacing={2}>
            <Grid item xs={12} sm={6} md={3}>
              <Button
                fullWidth
                variant="contained"
                startIcon={<PlayArrowIcon />}
                onClick={() => navigate('/session-control')}
                size="large"
              >
                세션 시작하기
              </Button>
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <Button
                fullWidth
                variant="outlined"
                startIcon={<PeopleIcon />}
                onClick={() => navigate('/monitoring')}
                size="large"
              >
                학생 모니터링
              </Button>
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <Button
                fullWidth
                variant="outlined"
                startIcon={<HelpIcon />}
                onClick={() => navigate('/help-requests')}
                size="large"
              >
                도움 요청 확인
              </Button>
            </Grid>
          </Grid>
        </Box>

        {/* Lectures List */}
        <Box>
          <Typography variant="h5" gutterBottom>
            내 강의
          </Typography>
          {lectures.length === 0 ? (
            <Alert severity="info">등록된 강의가 없습니다.</Alert>
          ) : (
            <Grid container spacing={2}>
              {lectures.map((lecture) => (
                <Grid item xs={12} sm={6} md={4} key={lecture.id}>
                  <Card>
                    <CardContent>
                      <Typography variant="h6" gutterBottom>
                        {lecture.title}
                      </Typography>
                      <Typography variant="body2" color="text.secondary" paragraph>
                        {lecture.description}
                      </Typography>
                      {lecture.is_active ? (
                        <Chip label="활성" color="success" size="small" />
                      ) : (
                        <Chip label="비활성" size="small" />
                      )}
                    </CardContent>
                  </Card>
                </Grid>
              ))}
            </Grid>
          )}
        </Box>
      </Box>
    </Layout>
  );
};
