import api from './api';
import { ProductivityReport } from '../types';

export const productivityService = {
  async getDailyReport(date?: string): Promise<ProductivityReport> {
    const params = date ? { date } : {};
    const response = await api.get<ProductivityReport>('/productivity/daily', { params });
    return response.data;
  },
};
