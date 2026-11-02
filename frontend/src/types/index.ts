export type Priority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED' | 'OVERDUE';
export type CategoryType = 'STUDY' | 'WORK' | 'HEALTH' | 'ENTERTAINMENT';
export type ViewMode = 'day' | 'week' | 'month';
export type EisenhowerQuadrant = 'DO_FIRST' | 'SCHEDULE' | 'DELEGATE' | 'DONT_DO';
export type RecurrenceRule = 'DAILY' | 'WEEKDAYS' | 'WEEKLY' | 'MONTHLY';

export interface Category {
  id: number;
  name: string;
  type: CategoryType;
  colorCode: string;
  bgColor: string;
  iconName: string;
}

export interface Subtask {
  id: number;
  taskId: number;
  title: string;
  isCompleted: boolean;
  durationMinutes: number;
  orderIndex: number;
  createdAt?: string;
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
  eisenhowerQuadrant?: EisenhowerQuadrant;
  estimatedDurationMinutes?: number;
  actualDurationMinutes?: number;
  isRecurring?: boolean;
  recurrenceRule?: RecurrenceRule | string;
  streakCount?: number;
  isAutoScheduled: boolean;
  subtasksCount?: number;
  completedSubtasksCount?: number;
  subtext?: string;
  colorOverride?: string;
  isLocked?: boolean;
}

export interface ScheduledTaskProposal {
  taskId: number;
  title: string;
  categoryType: CategoryType;
  priority: Priority;
  eisenhowerQuadrant: EisenhowerQuadrant;
  startTime: string;
  endTime: string;
  durationMinutes: number;
  slotRationale: string;
}

export interface AutoScheduleResponse {
  targetDate: string;
  scheduledCount: number;
  remainingUnscheduled: number;
  proposals: ScheduledTaskProposal[];
  aiStrategySummary: string;
  applied: boolean;
}

export interface DailySummary {
  date: string;
  dayOfWeek: string;
  completedTasks: number;
  focusMinutes: number;
  score: number;
}

export interface ProductivityTaskItem {
  id: number;
  title: string;
  priority: string;
  durationMinutes: number;
  categoryName: string;
  categoryColor: string;
  status: string;
  dueTimeStr: string;
}

export interface ProductivityReport {
  reportDate: string;
  totalCompletedTasks: number;
  totalTasks: number;
  totalFocusMinutes: number;
  totalFocusSessions?: number;
  onTimeRate: number;
  overdueTaskCount: number;
  productivityScore: number;
  currentStreakDays?: number;
  completedTaskItems?: ProductivityTaskItem[];
  pendingTaskItems?: ProductivityTaskItem[];
  aiFeedback: string;
  aiStrengths: string;
  aiImprovementTips: string;
  pastWeekTrends: DailySummary[];
  categoryDistribution: Record<string, number>;
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
