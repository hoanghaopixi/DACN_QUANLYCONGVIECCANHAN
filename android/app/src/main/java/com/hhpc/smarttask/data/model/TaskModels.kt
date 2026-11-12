package com.hhpc.smarttask.data.model

data class TaskDto(
    val id: Long? = null,
    val userId: Long? = null,
    val workspaceId: Long? = null,
    val categoryId: Long? = null,
    val categoryType: String? = null,
    val title: String,
    val description: String? = null,
    val note: String? = null,
    val priority: String? = "MEDIUM", // LOW, MEDIUM, HIGH, URGENT
    val status: String? = "TODO",     // TODO, IN_PROGRESS, COMPLETED, CANCELLED, OVERDUE
    val eisenhowerQuadrant: String? = "SCHEDULE", // DO_FIRST, SCHEDULE, DELEGATE, DONT_DO
    val startTime: String? = null,
    val endTime: String? = null,
    val estimatedDurationMinutes: Int? = null,
    val actualDurationMinutes: Int? = null,
    val isRecurring: Boolean? = false,
    val recurrenceRule: String? = null,
    val streakCount: Int? = 0,
    val colorCode: String? = null,
    val isAutoScheduled: Boolean? = false,
    val subtext: String? = null,
    val subtasksCount: Int? = 0,
    val completedSubtasksCount: Int? = 0,
    val isLocked: Boolean? = false
)

data class SubtaskDto(
    val id: Long? = null,
    val taskId: Long? = null,
    val title: String,
    val isCompleted: Boolean? = false,
    val durationMinutes: Int? = null,
    val orderIndex: Int? = 0
)

data class AiParseTaskRequest(
    val prompt: String,
    val currentDateTime: String? = null,
    val workspaceId: Long? = null,
    val userId: Long? = null
)

data class AiParseTaskResponse(
    val title: String? = null,
    val description: String? = null,
    val categoryType: String? = "WORK",
    val startTime: String? = null,
    val endTime: String? = null,
    val estimatedDurationMinutes: Int? = 30,
    val priority: String? = "MEDIUM",
    val eisenhowerQuadrant: String? = "SCHEDULE",
    val isFixedTime: Boolean? = false,
    val formattedDateLabel: String? = null,
    val note: String? = null,
    val hasConflict: Boolean? = false,
    val conflictMessage: String? = null,
    val conflictingTaskTitle: String? = null,
    val suggestedAlternativeStartTime: String? = null,
    val suggestedAlternativeEndTime: String? = null,
    val isValidTask: Boolean? = true,
    val validationMessage: String? = null,
    val validationType: String? = null,
    val tasks: List<AiParseTaskResponse>? = null
)

data class AutoScheduleRequest(
    val targetDate: String? = null,
    val workspaceId: Long? = null,
    val taskIds: List<Long>? = null,
    val applyImmediately: Boolean? = true,
    val breakStartTime: String? = null,
    val breakEndTime: String? = null
)

data class ScheduledTaskProposal(
    val taskId: Long? = null,
    val title: String? = null,
    val categoryType: String? = null,
    val priority: String? = null,
    val eisenhowerQuadrant: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val durationMinutes: Int? = null,
    val slotRationale: String? = null
)

data class AutoScheduleResponse(
    val targetDate: String? = null,
    val scheduledCount: Int? = 0,
    val remainingUnscheduled: Int? = 0,
    val proposals: List<ScheduledTaskProposal>? = emptyList(),
    val aiStrategySummary: String? = null,
    val applied: Boolean? = true
)

data class UserPreferenceDto(
    val id: Long? = null,
    val userId: Long? = null,
    val fullName: String? = null,
    val phone: String? = null,
    val avatarUrl: String? = null,
    val language: String? = "Tiếng Việt",
    val workStartTime: String? = "08:00",
    val workEndTime: String? = "18:00",
    val lunchBreakStart: String? = "12:00",
    val lunchBreakEnd: String? = "13:00",
    val workDays: String? = "T2,T3,T4,T5,T6",
    val bufferMinutes: Int? = 15,
    val allowWeekendTasks: Boolean? = true,
    val peakProductivityTime: String? = "MORNING", // MORNING, AFTERNOON, EVENING
    val autoReschedule: Boolean? = true,
    val aiAutoFindSlot: Boolean? = true,
    val aiAlwaysAskBeforeChange: Boolean? = true,
    val aiAllowSplitLongTasks: Boolean? = true,
    val aiAutoMoveFixedTasks: Boolean? = false,
    val timeZone: String? = "(GMT+07:00) Hồ Chí Minh",
    val theme: String? = "LIGHT",
    val backgroundUrl: String? = null,
    val backgroundType: String? = "NONE",
    val overlayDarkness: Int? = 50,
    val reminderMinutesBefore: Int? = 15,
    val dndStartTime: String? = "22:00",
    val dndEndTime: String? = "07:00",
    val enableDeviceNotifications: Boolean? = true,
    val enableMorningSummaryEmail: Boolean? = false
)

data class ProductivityReportDto(
    val reportDate: String? = null,
    val totalCompletedTasks: Int? = 0,
    val totalTasks: Int? = 0,
    val totalFocusMinutes: Int? = 0,
    val totalFocusSessions: Int? = 0,
    val onTimeRate: Double? = 0.0,
    val overdueTaskCount: Int? = 0,
    val productivityScore: Int? = 85,
    val aiFeedback: String? = null,
    val aiStrengths: String? = null,
    val aiImprovementTips: String? = null,
    val pastWeekTrends: List<DailySummaryDto>? = emptyList(),
    val categoryDistribution: Map<String, Int>? = emptyMap(),
    val currentStreakDays: Int? = 0
)

data class DailySummaryDto(
    val date: String? = null,
    val dayOfWeek: String? = null,
    val completedTasks: Int? = 0,
    val focusMinutes: Int? = 0,
    val score: Int? = 0
)
