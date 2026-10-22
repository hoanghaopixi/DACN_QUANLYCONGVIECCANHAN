export type Priority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED' | 'OVERDUE';
export type CategoryType = 'STUDY' | 'WORK' | 'HEALTH' | 'ENTERTAINMENT';
export type ViewMode = 'day' | 'week' | 'month';

export interface Category {
  id: number;
  name: string;
  type: CategoryType;
  colorCode: string;
  bgColor: string;
  iconName: string;
}

export interface TimeBlock {
  id: number;
  taskId?: number;
  workspaceId?: number;
  title: string;
  description?: string;
  note?: string;
  startTime: string;
  endTime: string;
  categoryType: CategoryType;
  status: TaskStatus;
  priority?: Priority;
  estimatedDurationMinutes?: number;
  isAutoScheduled: boolean;
}

export interface WeekDay {
  date: Date;
  dayName: string;
  dayNumber: number;
  isToday: boolean;
}

export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
  parsedTask?: ParsedTaskInfo;
  conflict?: ConflictInfo;
  suggestion?: string;
}

export interface ParsedTaskInfo {
  title: string;
  dateTimeLabel: string;
  priority: string;
  note: string;
}

export interface ConflictInfo {
  message: string;
  conflictWith: string;
  alternativesFound: number;
}

export interface UserStats {
  totalTasksThisWeek: number;
  completedTasks: number;
  focusHours: number;
  focusLabel: string;
  freeSlots: number;
}
