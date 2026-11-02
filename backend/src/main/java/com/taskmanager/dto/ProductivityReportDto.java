package com.taskmanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductivityReportDto {
    private LocalDate reportDate;
    private Integer totalCompletedTasks;
    private Integer totalTasks;
    private Integer totalFocusMinutes;
    private Integer totalFocusSessions;
    private BigDecimal onTimeRate;
    private Integer overdueTaskCount;
    private Integer productivityScore; // 0 to 100
    private String aiFeedback;
    private String aiStrengths;
    private String aiImprovementTips;
    private List<DailySummaryDto> pastWeekTrends;
    private Map<String, Integer> categoryDistribution;

    private Integer currentStreakDays;
    private List<TaskItemDto> completedTaskItems;
    private List<TaskItemDto> pendingTaskItems;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailySummaryDto {
        private LocalDate date;
        private String dayOfWeek;
        private Integer completedTasks;
        private Integer focusMinutes;
        private Integer score;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskItemDto {
        private Long id;
        private String title;
        private String priority;
        private Integer durationMinutes;
        private String categoryName;
        private String categoryColor;
        private String status;
        private String dueTimeStr;
    }
}
