import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './lib/auth-context';
import { ProtectedRoute } from './components/protected-route';
import { Layout } from './components/layout';
import { LoginPage } from './pages/login-page';
import { DashboardPage } from './pages/dashboard-page';
import { LecturesPage } from './pages/lectures-page';
import { LectureFormPage } from './pages/lecture-form-page';
import { SessionControlPage } from './pages/session-control-page';
import { StatisticsPage } from './pages/statistics-page';
import { LiveSessionPage } from './pages/live-session-page';
import { JoinSessionPage } from './pages/join-session-page';
import { Toaster } from './components/ui/sonner';

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/join/:sessionCode" element={<JoinSessionPage />} />

          <Route
            path="/"
            element={
              <ProtectedRoute>
                <Layout>
                  <DashboardPage />
                </Layout>
              </ProtectedRoute>
            }
          />
          
          <Route
            path="/lectures"
            element={
              <ProtectedRoute>
                <Layout>
                  <LecturesPage />
                </Layout>
              </ProtectedRoute>
            }
          />
          
          <Route
            path="/lectures/new"
            element={
              <ProtectedRoute>
                <Layout>
                  <LectureFormPage />
                </Layout>
              </ProtectedRoute>
            }
          />
          
          <Route
            path="/lectures/edit/:lectureId"
            element={
              <ProtectedRoute>
                <Layout>
                  <LectureFormPage />
                </Layout>
              </ProtectedRoute>
            }
          />
          
          <Route
            path="/sessions"
            element={
              <ProtectedRoute>
                <Layout>
                  <SessionControlPage />
                </Layout>
              </ProtectedRoute>
            }
          />
          
          <Route
            path="/statistics"
            element={
              <ProtectedRoute>
                <Layout>
                  <StatisticsPage />
                </Layout>
              </ProtectedRoute>
            }
          />
          
          <Route
            path="/live-session"
            element={
              <ProtectedRoute>
                <LiveSessionPage />
              </ProtectedRoute>
            }
          />

          <Route
            path="/live-session/:sessionId"
            element={
              <ProtectedRoute>
                <LiveSessionPage />
              </ProtectedRoute>
            }
          />

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
      
      <Toaster position="top-right" />
    </AuthProvider>
  );
}