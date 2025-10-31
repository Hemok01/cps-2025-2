import apiClient from './api';
import type { HelpRequest, MGptAnalysis } from '../types';

export const helpService = {
  // Get pending help requests
  getPendingHelpRequests: async (): Promise<HelpRequest[]> => {
    const response = await apiClient.get<{ pending_requests: HelpRequest[] }>(
      '/api/dashboard/help-requests/pending/'
    );
    return response.data.pending_requests;
  },

  // Get help request by ID
  getHelpRequest: async (requestId: number): Promise<HelpRequest> => {
    const response = await apiClient.get<HelpRequest>(`/api/help/request/${requestId}/`);
    return response.data;
  },

  // Resolve help request
  resolveHelpRequest: async (requestId: number): Promise<HelpRequest> => {
    const response = await apiClient.post<HelpRequest>(
      `/api/help/request/${requestId}/resolve/`
    );
    return response.data;
  },

  // Get M-GPT analysis for help request
  getAnalysis: async (requestId: number): Promise<MGptAnalysis | null> => {
    try {
      const response = await apiClient.get<MGptAnalysis>(
        `/api/help/request/${requestId}/analysis/`
      );
      return response.data;
    } catch (error) {
      return null;
    }
  },
};
