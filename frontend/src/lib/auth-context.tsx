import React, { createContext, useContext, useState, useEffect } from 'react';
import { User, AuthTokens } from './types';

interface AuthContextType {
  user: User | null;
  tokens: AuthTokens | null;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
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
    // Mock API call - in production, this would call the actual backend
    // POST /api/auth/login/
    
    if (email === 'instructor@test.com' && password === 'TestInstructor123!@#') {
      const mockTokens: AuthTokens = {
        access: 'mock_access_token_' + Date.now(),
        refresh: 'mock_refresh_token_' + Date.now(),
      };
      
      const mockUser: User = {
        id: 1,
        email: 'instructor@test.com',
        name: '김강사',
        role: 'INSTRUCTOR',
      };
      
      setTokens(mockTokens);
      setUser(mockUser);
      
      localStorage.setItem('auth_tokens', JSON.stringify(mockTokens));
      localStorage.setItem('user', JSON.stringify(mockUser));
    } else {
      throw new Error('이메일 또는 비밀번호가 올바르지 않습니다');
    }
  };

  const logout = () => {
    setUser(null);
    setTokens(null);
    localStorage.removeItem('auth_tokens');
    localStorage.removeItem('user');
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
