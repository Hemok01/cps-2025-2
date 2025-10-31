import React, { useState, useEffect } from 'react';
import {
  Box,
  Grid,
  Card,
  CardContent,
  Typography,
  Alert,
  CircularProgress,
  Chip,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Divider,
} from '@mui/material';
import {
  CheckCircle,
  AccessTime,
  Psychology,
} from '@mui/icons-material';
import { Layout } from '../components/common/Layout';
import { helpService } from '../services/helpService';
import type { HelpRequest, MGptAnalysis } from '../types';

export const HelpRequestsPage: React.FC = () => {
  const [helpRequests, setHelpRequests] = useState<HelpRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [selectedRequest, setSelectedRequest] = useState<HelpRequest | null>(null);
  const [analysis, setAnalysis] = useState<MGptAnalysis | null>(null);
  const [dialogOpen, setDialogOpen] = useState(false);

  useEffect(() => {
    loadHelpRequests();
    const interval = setInterval(loadHelpRequests, 10000); // Refresh every 10 seconds
    return () => clearInterval(interval);
  }, []);

  const loadHelpRequests = async () => {
    try {
      const data = await helpService.getPendingHelpRequests();
      setHelpRequests(data);
    } catch (err) {
      console.error('Failed to load help requests:', err);
      setError('도움 요청을 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleViewRequest = async (request: HelpRequest) => {
    setSelectedRequest(request);
    setDialogOpen(true);

    // Load M-GPT analysis if exists
    try {
      const analysisData = await helpService.getAnalysis(request.id);
      setAnalysis(analysisData);
    } catch (err) {
      console.error('Failed to load analysis:', err);
      setAnalysis(null);
    }
  };

  const handleResolveRequest = async () => {
    if (!selectedRequest) return;

    try {
      await helpService.resolveHelpRequest(selectedRequest.id);
      setHelpRequests(helpRequests.filter((r) => r.id !== selectedRequest.id));
      setDialogOpen(false);
      setSelectedRequest(null);
      setAnalysis(null);
    } catch (err) {
      console.error('Failed to resolve help request:', err);
      setError('도움 요청 해결에 실패했습니다.');
    }
  };

  const getStatusChip = (status: string) => {
    const statusMap: Record<string, { label: string; color: any; icon: any }> = {
      PENDING: { label: '대기 중', color: 'warning', icon: <AccessTime /> },
      ANALYZING: { label: '분석 중', color: 'info', icon: <Psychology /> },
      RESOLVED: { label: '해결됨', color: 'success', icon: <CheckCircle /> },
    };
    const statusInfo = statusMap[status] || { label: status, color: 'default', icon: null };
    return <Chip label={statusInfo.label} color={statusInfo.color} icon={statusInfo.icon} />;
  };

  const getRequestTypeChip = (type: string) => {
    return type === 'MANUAL' ? (
      <Chip label="수동" color="primary" size="small" />
    ) : (
      <Chip label="자동" color="secondary" size="small" />
    );
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
          도움 요청 관리
        </Typography>

        {error && (
          <Alert severity="error" onClose={() => setError('')} sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        {helpRequests.length === 0 ? (
          <Alert severity="info">현재 대기 중인 도움 요청이 없습니다.</Alert>
        ) : (
          <Grid container spacing={2}>
            {helpRequests.map((request) => (
              <Grid item xs={12} md={6} lg={4} key={request.id}>
                <Card>
                  <CardContent>
                    <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
                      {getStatusChip(request.status)}
                      {getRequestTypeChip(request.request_type)}
                    </Box>

                    <Typography variant="h6" gutterBottom>
                      {request.user_name || '익명 학생'}
                    </Typography>

                    {request.subtask_details && (
                      <Typography variant="body2" color="text.secondary" paragraph>
                        단계: {request.subtask_details.guide_text}
                      </Typography>
                    )}

                    {request.description && (
                      <Typography variant="body2" paragraph>
                        {request.description}
                      </Typography>
                    )}

                    <Typography variant="caption" color="text.secondary" display="block" mb={2}>
                      요청 시간: {new Date(request.created_at).toLocaleString('ko-KR')}
                    </Typography>

                    <Button
                      fullWidth
                      variant="outlined"
                      onClick={() => handleViewRequest(request)}
                    >
                      자세히 보기
                    </Button>
                  </CardContent>
                </Card>
              </Grid>
            ))}
          </Grid>
        )}

        {/* Request Detail Dialog */}
        <Dialog
          open={dialogOpen}
          onClose={() => setDialogOpen(false)}
          maxWidth="md"
          fullWidth
        >
          {selectedRequest && (
            <>
              <DialogTitle>
                도움 요청 상세
              </DialogTitle>
              <DialogContent>
                <Box mb={2}>
                  <Typography variant="subtitle2" color="text.secondary">
                    학생
                  </Typography>
                  <Typography variant="body1" gutterBottom>
                    {selectedRequest.user_name}
                  </Typography>
                </Box>

                <Box mb={2}>
                  <Typography variant="subtitle2" color="text.secondary">
                    현재 단계
                  </Typography>
                  <Typography variant="body1" gutterBottom>
                    {selectedRequest.subtask_details?.guide_text}
                  </Typography>
                </Box>

                {selectedRequest.description && (
                  <Box mb={2}>
                    <Typography variant="subtitle2" color="text.secondary">
                      설명
                    </Typography>
                    <Typography variant="body1" gutterBottom>
                      {selectedRequest.description}
                    </Typography>
                  </Box>
                )}

                <Box mb={2}>
                  <Typography variant="subtitle2" color="text.secondary">
                    요청 유형
                  </Typography>
                  <Box mt={1}>
                    {getRequestTypeChip(selectedRequest.request_type)}
                  </Box>
                </Box>

                <Box mb={2}>
                  <Typography variant="subtitle2" color="text.secondary">
                    상태
                  </Typography>
                  <Box mt={1}>
                    {getStatusChip(selectedRequest.status)}
                  </Box>
                </Box>

                {analysis && (
                  <>
                    <Divider sx={{ my: 2 }} />
                    <Typography variant="h6" gutterBottom>
                      M-GPT 분석 결과
                    </Typography>

                    <Box mb={2}>
                      <Typography variant="subtitle2" color="text.secondary">
                        문제 진단
                      </Typography>
                      <Typography variant="body1" gutterBottom>
                        {analysis.problem_diagnosis}
                      </Typography>
                    </Box>

                    <Box mb={2}>
                      <Typography variant="subtitle2" color="text.secondary">
                        권장 도움
                      </Typography>
                      <Typography variant="body1" gutterBottom>
                        {analysis.suggested_help}
                      </Typography>
                    </Box>

                    <Box mb={2}>
                      <Typography variant="subtitle2" color="text.secondary">
                        신뢰도
                      </Typography>
                      <Typography variant="body1" gutterBottom>
                        {(analysis.confidence_score * 100).toFixed(1)}%
                      </Typography>
                    </Box>
                  </>
                )}
              </DialogContent>
              <DialogActions>
                <Button onClick={() => setDialogOpen(false)}>
                  닫기
                </Button>
                <Button
                  variant="contained"
                  color="success"
                  onClick={handleResolveRequest}
                >
                  해결 완료
                </Button>
              </DialogActions>
            </>
          )}
        </Dialog>
      </Box>
    </Layout>
  );
};
