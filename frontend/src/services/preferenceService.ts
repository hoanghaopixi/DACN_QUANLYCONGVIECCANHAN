import api from './api';

export interface UserPreferenceData {
  id?: number;
  userId?: number;
  fullName?: string;
  phone?: string;
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

  // Các thuộc tính mở rộng đồng bộ với CSDL PostgreSQL và Android
  lunchBreakStart?: string;
  lunchBreakEnd?: string;
  allowWeekends?: boolean;
  allowWeekendTasks?: boolean;
  autoFindGaps?: boolean;
  aiAutoFindSlot?: boolean;
  alwaysAskBeforeReschedule?: boolean;
  aiAlwaysAskBeforeChange?: boolean;
  allowTaskBreakdown?: boolean;
  aiAllowSplitLongTasks?: boolean;
  autoRescheduleFixedTasks?: boolean;
  aiAutoMoveFixedTasks?: boolean;
  reminderBeforeMinutes?: number;
  reminderMinutesBefore?: number;
  doNotDisturbStart?: string;
  dndStartTime?: string;
  doNotDisturbEnd?: string;
  dndEndTime?: string;
  deviceNotifications?: boolean;
  enableDeviceNotifications?: boolean;
  morningBriefingEmail?: boolean;
  enableMorningSummaryEmail?: boolean;
  language?: string;
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
