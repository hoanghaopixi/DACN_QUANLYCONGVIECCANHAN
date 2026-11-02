import api from './api';

export interface AuthResponse {
  token: string;
  email: string;
  fullName: string;
  role: string;
  avatarUrl?: string;
  phone?: string;
}

export interface LoginPayload {
  email: string;
  password: string;
}

export interface RegisterPayload {
  fullName: string;
  email: string;
  password: string;
}

export interface ChangePasswordPayload {
  currentPassword: string;
  newPassword: string;
}

export interface UpdateProfilePayload {
  fullName?: string;
  phone?: string;
  avatarUrl?: string;
}

export const authService = {
  async login(payload: LoginPayload): Promise<AuthResponse> {
    const response = await api.post<AuthResponse>('/auth/login', payload);
    const data = response.data;
    localStorage.setItem('smarttask_current_user', JSON.stringify(data));
    return data;
  },

  async register(payload: RegisterPayload): Promise<AuthResponse> {
    const response = await api.post<AuthResponse>('/auth/register', payload);
    const data = response.data;
    localStorage.setItem('smarttask_current_user', JSON.stringify(data));
    return data;
  },

  async changePassword(payload: ChangePasswordPayload): Promise<{ success: boolean; message: string }> {
    const response = await api.post<{ success: boolean; message: string }>('/auth/change-password', payload);
    return response.data;
  },

  async verifyPassword(password: string): Promise<{ success: boolean; message: string }> {
    const response = await api.post<{ success: boolean; message: string }>('/auth/verify-password', { password });
    return response.data;
  },

  async updateProfile(payload: UpdateProfilePayload): Promise<AuthResponse> {
    const response = await api.put<AuthResponse>('/auth/profile', payload);
    const updated = response.data;
    const current = authService.getCurrentUser();
    const merged = { ...current, ...updated };
    localStorage.setItem('smarttask_current_user', JSON.stringify(merged));
    return merged;
  },

  async uploadAvatar(file: File): Promise<{ success: boolean; avatarUrl: string; message: string; fileName: string }> {
    const formData = new FormData();
    formData.append('file', file);
    const response = await api.post<{ success: boolean; avatarUrl: string; message: string; fileName: string }>(
      '/auth/upload-avatar',
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      }
    );
    const data = response.data;
    if (data.avatarUrl) {
      const current = authService.getCurrentUser();
      if (current) {
        const merged = { ...current, avatarUrl: data.avatarUrl };
        localStorage.setItem('smarttask_current_user', JSON.stringify(merged));
      }
    }
    return data;
  },

  async getProfile(): Promise<AuthResponse> {
    const response = await api.get<AuthResponse>('/auth/me');
    const data = response.data;
    const current = authService.getCurrentUser();
    const merged = { ...current, ...data };
    localStorage.setItem('smarttask_current_user', JSON.stringify(merged));
    return merged;
  },

  logout(): void {
    localStorage.removeItem('smarttask_current_user');
  },

  getCurrentUser(): AuthResponse | null {
    const saved = localStorage.getItem('smarttask_current_user');
    if (!saved) return null;
    try {
      return JSON.parse(saved);
    } catch {
      return null;
    }
  },
};

