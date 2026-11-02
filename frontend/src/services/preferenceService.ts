import api from './api';

export interface UserPreferenceData {
  id?: number;
  userId?: number;
  workStartTime?: string;
  workEndTime?: string;
  workDays?: string;
  bufferMinutes?: number;
  peakProductivityTime?: string;
  autoReschedule?: boolean;
  timeZone?: string;
  theme?: string;
  backgroundUrl?: string;
  backgroundType?: 'NONE' | 'IMAGE' | 'VIDEO';
  overlayDarkness?: number; // 0 to 100 percentage
}

export const preferenceService = {
  async getPreferences(): Promise<UserPreferenceData> {
    const res = await api.get<UserPreferenceData>('/preferences');
    return res.data;
  },

  async updatePreferences(data: UserPreferenceData): Promise<UserPreferenceData> {
    const res = await api.put<UserPreferenceData>('/preferences', data);
    return res.data;
  },

  async uploadBackground(file: File): Promise<{
    success: boolean;
    message: string;
    backgroundUrl: string;
    backgroundType: 'IMAGE' | 'VIDEO';
    overlayDarkness: number;
  }> {
    const formData = new FormData();
    formData.append('file', file);
    const res = await api.post('/preferences/upload-background', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return res.data;
  },
};
