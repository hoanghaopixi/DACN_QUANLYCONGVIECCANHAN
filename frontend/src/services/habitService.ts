import api from './api';

export interface HabitLogData {
  id?: number;
  userId?: number;
  taskId: number;
  taskTitle?: string;
  checkInDate?: string;
  status?: string;
  note?: string;
  createdAt?: string;
}

export const habitService = {
  async getHabitLogs(): Promise<HabitLogData[]> {
    const res = await api.get<HabitLogData[]>('/habits');
    return res.data;
  },

  async getTaskHabitLogs(taskId: number): Promise<HabitLogData[]> {
    const res = await api.get<HabitLogData[]>(`/habits/tasks/${taskId}`);
    return res.data;
  },

  async checkIn(data: HabitLogData): Promise<HabitLogData> {
    const res = await api.post<HabitLogData>('/habits/check-in', data);
    return res.data;
  },
};
