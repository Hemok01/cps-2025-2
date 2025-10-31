import apiClient from './api';
import type { LoginRequest, LoginResponse, User } from '../types';

export const authService = {
  // Login
  login: async (credentials: LoginRequest): Promise<LoginResponse> => {
    const response = await apiClient.post<LoginResponse>('/api/auth/login/', credentials);
    return response.data;
  },

  // Get current user
  getCurrentUser: async (): Promise<User> => {
    const response = await apiClient.get<User>('/api/auth/me/');
    return response.data;
  },

  // Logout
  logout: () => {
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    localStorage.removeItem('user');
  },

  // Check if user is authenticated
  isAuthenticated: (): boolean => {
    return !!localStorage.getItem('access_token');
  },

  // Get stored user
  getStoredUser: (): User | null => {
    const userStr = localStorage.getItem('user');
    return userStr ? JSON.parse(userStr) : null;
  },

  // Store auth data
  storeAuthData: (tokens: { access: string; refresh: string }, user: User) => {
    localStorage.setItem('access_token', tokens.access);
    localStorage.setItem('refresh_token', tokens.refresh);
    localStorage.setItem('user', JSON.stringify(user));
  },
};
