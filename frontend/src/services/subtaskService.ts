import api from './api';
import { Subtask } from '../types';

export const subtaskService = {
  async getSubtasks(taskId: number): Promise<Subtask[]> {
    const response = await api.get<Subtask[]>(`/tasks/${taskId}/subtasks`);
    return response.data;
  },

  async createSubtask(
    taskId: number,
    data: { title: string; durationMinutes?: number }
  ): Promise<Subtask> {
    const response = await api.post<Subtask>(`/tasks/${taskId}/subtasks`, data);
    return response.data;
  },

  async updateSubtask(
    subtaskId: number,
    data: Partial<Subtask>
  ): Promise<Subtask> {
    const response = await api.put<Subtask>(`/tasks/subtasks/${subtaskId}`, data);
    return response.data;
  },

  async toggleSubtask(subtaskId: number): Promise<Subtask> {
    const response = await api.patch<Subtask>(`/tasks/subtasks/${subtaskId}/toggle`);
    return response.data;
  },

  async deleteSubtask(subtaskId: number): Promise<void> {
    await api.delete(`/tasks/subtasks/${subtaskId}`);
  },

  async breakdownWithAi(
    taskId?: number,
    payload?: { title: string; description?: string; durationMinutes?: number }
  ): Promise<Subtask[]> {
    if (taskId && taskId > 0) {
      const response = await api.post<Subtask[]>(`/ai/tasks/${taskId}/breakdown`);
      return response.data;
    }
    const response = await api.post<Subtask[]>(`/ai/breakdown`, {
      taskId: null,
      title: payload?.title || '',
      description: payload?.description || '',
      durationMinutes: payload?.durationMinutes || 60,
    });
    return response.data;
  },
};
