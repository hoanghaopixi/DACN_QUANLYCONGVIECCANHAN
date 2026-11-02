import api from './api';

export interface MediaHistoryItem {
  id: number;
  userId: number;
  mediaType: 'AVATAR' | 'BACKGROUND_IMAGE' | 'BACKGROUND_VIDEO';
  url: string;
  fileName?: string;
  fileSize?: number;
  createdAt: string;
}

export const mediaService = {
  async getHistory(type?: 'AVATAR' | 'BACKGROUND'): Promise<MediaHistoryItem[]> {
    const params = type ? { type } : {};
    const res = await api.get<MediaHistoryItem[]>('/media/history', { params });
    return res.data;
  },

  async deleteHistory(id: number): Promise<void> {
    await api.delete(`/media/history/${id}`);
  },
};
