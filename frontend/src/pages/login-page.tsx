import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../lib/auth-context';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import { Alert, AlertDescription } from '../components/ui/alert';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/card';

export function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    // Basic validation
    if (!email || !password) {
      setError('이메일과 비밀번호를 입력해주세요');
      setLoading(false);
      return;
    }

    if (!email.includes('@')) {
      setError('올바른 이메일 형식이 아닙니다');
      setLoading(false);
      return;
    }

    if (password.length < 8) {
      setError('비밀번호는 최소 8자 이상이어야 합니다');
      setLoading(false);
      return;
    }

    try {
      await login(email, password);
      navigate('/');
    } catch (err) {
      setError(err instanceof Error ? err.message : '로그인에 실패했습니다');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div 
      className="min-h-screen flex items-center justify-center p-4" 
      style={{ 
        background: 'linear-gradient(135deg, #E3F2FD 0%, #BBDEFB 100%)'
      }}
    >
      <Card className="w-full max-w-md" style={{ borderRadius: 'var(--radius-lg)' }}>
        <CardHeader className="text-center">
          <div className="mb-4">
            <div 
              className="inline-flex items-center justify-center w-16 h-16 rounded-full mb-2"
              style={{ backgroundColor: 'var(--primary)' }}
            >
              <span className="text-3xl">📱</span>
            </div>
          </div>
          <CardTitle className="text-3xl" style={{ color: 'var(--primary)' }}>MobileGPT</CardTitle>
          <CardDescription className="text-lg">강사 대시보드</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            {error && (
              <Alert variant="destructive" style={{ backgroundColor: '#FFEBEE', borderLeft: '4px solid var(--error)' }}>
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            )}

            <div className="space-y-2">
              <Label htmlFor="email" style={{ color: 'var(--text-secondary)' }}>이메일</Label>
              <Input
                id="email"
                type="email"
                placeholder="instructor@test.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                disabled={loading}
                required
                className="min-touch-target"
                style={{ fontSize: '1rem' }}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="password" style={{ color: 'var(--text-secondary)' }}>비밀번호</Label>
              <Input
                id="password"
                type="password"
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                disabled={loading}
                required
                className="min-touch-target"
                style={{ fontSize: '1rem' }}
              />
            </div>

            <Button
              type="submit"
              className="w-full min-touch-target"
              disabled={loading}
              style={{ 
                backgroundColor: 'var(--primary)',
                fontSize: '1.125rem',
                fontWeight: 'var(--font-weight-semibold)'
              }}
            >
              {loading ? '로그인 중...' : '로그인'}
            </Button>

            <div 
              className="mt-4 p-3 rounded-lg text-sm" 
              style={{ 
                backgroundColor: 'var(--accent)',
                borderLeft: '4px solid var(--info)'
              }}
            >
              <p className="mb-1" style={{ color: 'var(--info-dark)', fontWeight: 'var(--font-weight-semibold)' }}>
                💡 테스트 계정
              </p>
              <p className="text-xs" style={{ color: 'var(--text-secondary)' }}>이메일: instructor@test.com</p>
              <p className="text-xs" style={{ color: 'var(--text-secondary)' }}>비밀번호: test1234</p>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}