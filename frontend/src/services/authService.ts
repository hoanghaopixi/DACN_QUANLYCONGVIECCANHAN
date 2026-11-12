import api from './api';

export interface AuthResponse {
  token: string;
  email: string;
  fullName: string;
  role: string;
  avatarUrl?: string;
  phone?: string;
  authProvider?: string;
  hasPassword?: boolean;
}

export interface LoginPayload {
  email: string;
  password: string;
}

export interface RegisterPayload {
  fullName: string;
  email: string;
  password: string;
  otp?: string;
}

export interface SendRegisterOtpPayload {
  fullName: string;
  email: string;
}

export interface SetPasswordPayload {
  username: string;
  newPassword: string;
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

export interface ForgotPasswordPayload {
  email: string;
}

export interface VerifyOtpPayload {
  email: string;
  otp: string;
}

export interface ResetPasswordPayload {
  email: string;
  otp: string;
  newPassword: string;
}

export interface GoogleLoginPayload {
  idToken?: string;
  email: string;
  fullName?: string;
  avatarUrl?: string;
}

export interface OtpResponse {
  success: boolean;
  message: string;
  email?: string;
  expiresInSeconds?: number;
}

export const authService = {
  async login(payload: LoginPayload): Promise<AuthResponse> {
    const response = await api.post<AuthResponse>('/auth/login', payload);
    const data = response.data;
    localStorage.setItem('smarttask_current_user', JSON.stringify(data));
    return data;
  },

  async sendRegisterOtp(payload: SendRegisterOtpPayload): Promise<OtpResponse> {
    const response = await api.post<OtpResponse>('/auth/register/send-otp', payload);
    return response.data;
  },

  async register(payload: RegisterPayload): Promise<AuthResponse> {
    const response = await api.post<AuthResponse>('/auth/register', payload);
    const data = response.data;
    localStorage.setItem('smarttask_current_user', JSON.stringify(data));
    return data;
  },

  async loginWithGoogle(payload: GoogleLoginPayload): Promise<AuthResponse> {
    const response = await api.post<AuthResponse>('/auth/google', payload);
    const data = response.data;
    localStorage.setItem('smarttask_current_user', JSON.stringify(data));
    return data;
  },

  async forgotPassword(email: string): Promise<OtpResponse> {
    const response = await api.post<OtpResponse>('/auth/forgot-password', { email });
    return response.data;
  },

  async verifyOtp(payload: VerifyOtpPayload): Promise<OtpResponse> {
    const response = await api.post<OtpResponse>('/auth/verify-otp', payload);
    return response.data;
  },

  async resetPassword(payload: ResetPasswordPayload): Promise<OtpResponse> {
    const response = await api.post<OtpResponse>('/auth/reset-password', payload);
    return response.data;
  },

  async changePassword(payload: ChangePasswordPayload): Promise<{ success: boolean; message: string }> {
    const response = await api.post<{ success: boolean; message: string }>('/auth/change-password', payload);
    return response.data;
  },

  async setPassword(payload: SetPasswordPayload): Promise<AuthResponse> {
    const response = await api.post<AuthResponse>('/auth/set-password', payload);
    const data = response.data;
    const current = authService.getCurrentUser();
    const merged = { ...current, ...data };
    localStorage.setItem('smarttask_current_user', JSON.stringify(merged));
    return merged;
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

