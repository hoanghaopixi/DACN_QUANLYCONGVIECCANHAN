import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Attach only the signed bearer token; the API derives the user identity from it.
api.interceptors.request.use((config) => {
  const savedUser = localStorage.getItem('smarttask_current_user');
  if (savedUser) {
    try {
      const user = JSON.parse(savedUser);
      if (user.token) {
        config.headers.Authorization = `Bearer ${user.token}`;
      }
    } catch (e) {
      // ignore
    }
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const url = String(error?.config?.url || '');
    const isCredentialForm = ['/auth/login', '/auth/register', '/auth/verify-password', '/auth/change-password']
      .some((path) => url.includes(path));
    if (error?.response?.status === 401 && !isCredentialForm) {
      window.dispatchEvent(new Event('smarttask:session-expired'));
    }
    return Promise.reject(error);
  }
);

export default api;
