import React, { createContext, useContext, useState, useEffect } from 'react';
import { User, AuthTokens } from './types';
import apiClient from './api-client';

interface AuthContextType {
  user: User | null;
  tokens: AuthTokens | null;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  isLoading: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [tokens, setTokens] = useState<AuthTokens | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    // Check for existing tokens in localStorage
    const storedTokens = localStorage.getItem('auth_tokens');
    const storedUser = localStorage.getItem('user');
    
    if (storedTokens && storedUser) {
      setTokens(JSON.parse(storedTokens));
      setUser(JSON.parse(storedUser));
    }
    
    setIsLoading(false);
  }, []);

  const login = async (email: string, password: string) => {
    try {
      // POST /api/auth/login/ (또는 /api/token/)
      const loginResponse = await apiClient.post('/token/', { email, password });

      const authTokens: AuthTokens = {
        access: loginResponse.data.access,
        refresh: loginResponse.data.refresh,
      };

      // 토큰을 먼저 localStorage에 저장 (apiClient가 사용할 수 있도록)
      localStorage.setItem('auth_tokens', JSON.stringify(authTokens));
      setTokens(authTokens);

      // GET /api/auth/me/ - 사용자 정보 가져오기
      const userResponse = await apiClient.get('/auth/me/');
      const userData: User = {
        id: userResponse.data.id,
        email: userResponse.data.email,
        name: userResponse.data.name,
        role: userResponse.data.role,
      };

      setUser(userData);
      localStorage.setItem('user', JSON.stringify(userData));
    } catch (error: any) {
      // 인증 실패 시 localStorage 정리
      localStorage.removeItem('auth_tokens');
      localStorage.removeItem('user');

      if (error.response?.status === 401) {
        throw new Error('이메일 또는 비밀번호가 올바르지 않습니다');
      } else {
        throw new Error(error.response?.data?.detail || '로그인 중 오류가 발생했습니다');
      }
    }
  };

  const logout = async () => {
    try {
      // POST /api/auth/logout/ (optional - 백엔드에 로그아웃 알림)
      await apiClient.post('/auth/logout/');
    } catch (error) {
      // 로그아웃 요청이 실패해도 로컬에서는 정리
      console.error('Logout request failed:', error);
    } finally {
      setUser(null);
      setTokens(null);
      localStorage.removeItem('auth_tokens');
      localStorage.removeItem('user');
    }
  };

  return (
    <AuthContext.Provider value={{ user, tokens, login, logout, isLoading }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
