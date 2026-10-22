import api from './api';

export interface AiParseTaskRequest {
  prompt: string;
  currentDateTime?: string;
  workspaceId?: number;
}

export interface AiParseTaskResponse {
  title: string;
  description?: string;
  categoryType: 'WORK' | 'STUDY' | 'HEALTH' | 'ENTERTAINMENT';
  startTime: string;
  endTime: string;
  estimatedDurationMinutes: number;
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
  isFixedTime: boolean;
  formattedDateLabel: string;
  note?: string;
  hasConflict?: boolean;
  conflictMessage?: string;
  conflictingTaskTitle?: string;
  suggestedAlternativeStartTime?: string;
  suggestedAlternativeEndTime?: string;
  isValidTask?: boolean;
  validationMessage?: string;
  validationType?: 'GREETING' | 'INAPPROPRIATE' | 'NONSENSE' | 'VALID';
  tasks?: AiParseTaskResponse[];
}

export const aiService = {
  /**
   * Bóc tách câu lệnh giọng nói / văn bản thành công việc có cấu trúc
   */
  async parseTask(prompt: string, workspaceId?: number, currentDateTime?: string): Promise<AiParseTaskResponse> {
    const payload: AiParseTaskRequest = {
      prompt,
      currentDateTime: currentDateTime || new Date().toISOString(),
      workspaceId,
    };
    const response = await api.post<AiParseTaskResponse>('/ai/parse-task', payload);
    return response.data;
  },
};
