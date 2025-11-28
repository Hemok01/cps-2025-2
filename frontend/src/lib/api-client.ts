import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8000/api';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 요청 인터셉터 - 토큰 추가
apiClient.interceptors.request.use(
  (config) => {
    const tokens = localStorage.getItem('auth_tokens');
    if (tokens) {
      try {
        const { access } = JSON.parse(tokens);
        if (access) {
          config.headers.Authorization = `Bearer ${access}`;
        }
      } catch (error) {
        console.error('Failed to parse auth tokens:', error);
      }
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// 토큰 갱신 진행 중인지 추적
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (value?: unknown) => void;
  reject: (reason?: unknown) => void;
}> = [];

const processQueue = (error: unknown, token: string | null = null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

const redirectToLogin = () => {
  console.warn('[API Client] Redirecting to login due to authentication failure');
  localStorage.removeItem('auth_tokens');
  localStorage.removeItem('user');
  // 현재 페이지가 이미 로그인 페이지가 아닌 경우에만 리다이렉트
  if (!window.location.pathname.includes('/login')) {
    window.location.href = '/login';
  }
};

// 응답 인터셉터 - 토큰 갱신
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // 401 에러가 아니면 바로 reject
    if (error.response?.status !== 401) {
      return Promise.reject(error);
    }

    // 이미 재시도한 요청이면 reject
    if (originalRequest._retry) {
      return Promise.reject(error);
    }

    // 토큰 정보 확인
    const tokens = localStorage.getItem('auth_tokens');
    if (!tokens) {
      console.warn('[API Client] No auth tokens found, redirecting to login');
      redirectToLogin();
      return Promise.reject(error);
    }

    let parsedTokens;
    try {
      parsedTokens = JSON.parse(tokens);
    } catch {
      console.error('[API Client] Failed to parse auth tokens');
      redirectToLogin();
      return Promise.reject(error);
    }

    const { refresh } = parsedTokens;
    if (!refresh) {
      console.warn('[API Client] No refresh token available');
      redirectToLogin();
      return Promise.reject(error);
    }

    // 이미 토큰 갱신 중이면 큐에 추가
    if (isRefreshing) {
      return new Promise((resolve, reject) => {
        failedQueue.push({ resolve, reject });
      }).then(token => {
        originalRequest.headers.Authorization = `Bearer ${token}`;
        return apiClient(originalRequest);
      }).catch(err => Promise.reject(err));
    }

    originalRequest._retry = true;
    isRefreshing = true;

    try {
      console.log('[API Client] Attempting to refresh token...');
      const response = await axios.post(`${API_BASE_URL}/token/refresh/`, { refresh });

      const newTokens = {
        access: response.data.access,
        refresh: response.data.refresh || refresh
      };

      localStorage.setItem('auth_tokens', JSON.stringify(newTokens));
      console.log('[API Client] Token refreshed successfully');

      // 대기 중인 요청들 처리
      processQueue(null, newTokens.access);

      originalRequest.headers.Authorization = `Bearer ${newTokens.access}`;
      return apiClient(originalRequest);
    } catch (refreshError) {
      console.error('[API Client] Token refresh failed:', refreshError);
      processQueue(refreshError, null);
      redirectToLogin();
      return Promise.reject(refreshError);
    } finally {
      isRefreshing = false;
    }
  }
);

export default apiClient;
