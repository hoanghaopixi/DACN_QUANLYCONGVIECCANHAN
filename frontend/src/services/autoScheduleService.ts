import api from './api';
import { AutoScheduleResponse } from '../types';

export const autoScheduleService = {
  async autoSchedule(options?: {
    targetDate?: string;
    workspaceId?: number;
    taskIds?: number[];
    applyImmediately?: boolean;
  }): Promise<AutoScheduleResponse> {
    const response = await api.post<AutoScheduleResponse>(
      '/ai/auto-schedule',
      options || {}
    );
    return response.data;
  },
};
