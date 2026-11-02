import api from './api';
import { TimeBlock, UserStats, EisenhowerQuadrant } from '../types';

export interface BackendTaskDto {
  id?: number;
  userId?: number;
  workspaceId?: number;
  categoryId?: number;
  categoryType?: string;
  title: string;
  description?: string;
  note?: string;
  priority?: string;
  status?: string;
  eisenhowerQuadrant?: EisenhowerQuadrant;
  startTime: string;
  endTime: string;
  estimatedDurationMinutes?: number;
  actualDurationMinutes?: number;
  isRecurring?: boolean;
  recurrenceRule?: string;
  streakCount?: number;
  colorCode?: string;
  isAutoScheduled?: boolean;
  subtext?: string;
  subtasksCount?: number;
  completedSubtasksCount?: number;
  isLocked?: boolean;
}

function formatOffsetDateTime(timeStr: string): string {
  if (!timeStr) {
    return new Date().toISOString();
  }
  if (timeStr.includes('+') || timeStr.endsWith('Z')) {
    return timeStr;
  }
  return `${timeStr}+07:00`;
}

function mapDtoToTimeBlock(dto: BackendTaskDto): TimeBlock & { isRecurring?: boolean; subtext?: string; colorOverride?: string; priority?: string; workspaceId?: number } {
  return {
    id: dto.id || Date.now(),
    taskId: dto.id,
    title: dto.title,
    description: dto.description || dto.note,
    note: dto.note || dto.description,
    startTime: dto.startTime,
    endTime: dto.endTime,
    categoryType: (dto.categoryType as any) || 'WORK',
    status: (dto.status as any) || 'TODO',
    priority: (dto.priority as any),
    eisenhowerQuadrant: dto.eisenhowerQuadrant || 'SCHEDULE',
    estimatedDurationMinutes: dto.estimatedDurationMinutes,
    actualDurationMinutes: dto.actualDurationMinutes || 0,
    isAutoScheduled: !!dto.isAutoScheduled,
    isRecurring: !!dto.isRecurring,
    recurrenceRule: dto.recurrenceRule || 'DAILY',
    streakCount: dto.streakCount || 0,
    subtasksCount: dto.subtasksCount || 0,
    completedSubtasksCount: dto.completedSubtasksCount || 0,
    colorOverride: dto.colorCode,
    workspaceId: dto.workspaceId,
    isLocked: !!dto.isLocked,
    subtext: dto.subtext || (dto.status === 'COMPLETED' ? 'Đã đánh dấu hoàn thành' : dto.status === 'OVERDUE' ? 'Đã qua' : dto.status === 'IN_PROGRESS' ? 'Đang làm' : 'Cần làm'),
  };
}

export const taskService = {
  async getTasks(workspaceId?: number): Promise<(TimeBlock & { isRecurring?: boolean; subtext?: string; colorOverride?: string; priority?: string; workspaceId?: number })[]> {
    const params = workspaceId ? { workspaceId } : {};
    const response = await api.get<BackendTaskDto[]>('/tasks', { params });
    return response.data.map(mapDtoToTimeBlock);
  },

  async createTask(data: Partial<BackendTaskDto>): Promise<TimeBlock & { isRecurring?: boolean; subtext?: string; colorOverride?: string; priority?: string; workspaceId?: number }> {
    const payload: BackendTaskDto = {
      title: data.title || '',
      categoryType: data.categoryType || 'WORK',
      categoryId: data.categoryId,
      description: data.note || data.description,
      note: data.note || data.description,
      priority: data.priority || 'MEDIUM',
      status: data.status || 'TODO',
      eisenhowerQuadrant: data.eisenhowerQuadrant,
      startTime: formatOffsetDateTime(data.startTime || ''),
      endTime: formatOffsetDateTime(data.endTime || ''),
      estimatedDurationMinutes: data.estimatedDurationMinutes,
      actualDurationMinutes: data.actualDurationMinutes || 0,
      isRecurring: !!data.isRecurring,
      recurrenceRule: data.recurrenceRule,
      colorCode: data.colorCode,
      isAutoScheduled: !!data.isAutoScheduled,
      workspaceId: data.workspaceId,
      isLocked: !!data.isLocked,
    };
    const response = await api.post<BackendTaskDto>('/tasks', payload);
    return mapDtoToTimeBlock(response.data);
  },

  async updateTask(id: number, data: Partial<BackendTaskDto>): Promise<TimeBlock & { isRecurring?: boolean; subtext?: string; colorOverride?: string; priority?: string; workspaceId?: number }> {
    const payload: Partial<BackendTaskDto> = {
      ...data,
      note: data.note !== undefined ? data.note : data.description,
      description: data.note !== undefined ? data.note : data.description,
      startTime: data.startTime ? formatOffsetDateTime(data.startTime) : undefined,
      endTime: data.endTime ? formatOffsetDateTime(data.endTime) : undefined,
    };
    const response = await api.put<BackendTaskDto>(`/tasks/${id}`, payload);
    return mapDtoToTimeBlock(response.data);
  },

  async deleteTask(id: number): Promise<void> {
    await api.delete(`/tasks/${id}`);
  },

  async getStats(workspaceId?: number): Promise<UserStats> {
    const params = workspaceId ? { workspaceId } : {};
    const response = await api.get<UserStats>('/tasks/stats', { params });
    return response.data;
  },

  async getEisenhowerMatrix(workspaceId?: number): Promise<{
    doFirst: TimeBlock[];
    schedule: TimeBlock[];
    delegate: TimeBlock[];
    dontDo: TimeBlock[];
  }> {
    const params = workspaceId ? { workspaceId } : {};
    const response = await api.get<{
      doFirst: BackendTaskDto[];
      schedule: BackendTaskDto[];
      delegate: BackendTaskDto[];
      dontDo: BackendTaskDto[];
    }>('/tasks/eisenhower', { params });

    return {
      doFirst: response.data.doFirst.map(mapDtoToTimeBlock),
      schedule: response.data.schedule.map(mapDtoToTimeBlock),
      delegate: response.data.delegate.map(mapDtoToTimeBlock),
      dontDo: response.data.dontDo.map(mapDtoToTimeBlock),
    };
  },

  async updateQuadrant(id: number, quadrant: EisenhowerQuadrant): Promise<void> {
    await api.patch(`/tasks/${id}/quadrant`, { quadrant });
  },
};
