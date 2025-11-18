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

// 응답 인터셉터 - 토큰 갱신
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        const tokens = localStorage.getItem('auth_tokens');
        if (tokens) {
          const { refresh } = JSON.parse(tokens);
          const response = await axios.post(`${API_BASE_URL}/token/refresh/`, {
            refresh
          });

          const newTokens = {
            access: response.data.access,
            refresh: response.data.refresh || refresh
          };

          localStorage.setItem('auth_tokens', JSON.stringify(newTokens));
          originalRequest.headers.Authorization = `Bearer ${response.data.access}`;

          return apiClient(originalRequest);
        }
      } catch (refreshError) {
        // 토큰 갱신 실패 시 로그아웃 처리
        localStorage.removeItem('auth_tokens');
        localStorage.removeItem('user');
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export default apiClient;
