package com.taskmanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoScheduleDto {
    private String targetDate; // YYYY-MM-DD or empty for today
    private Long workspaceId;
    private List<Long> taskIds; // optional specific tasks to schedule
    private Boolean applyImmediately; // if true, saves to TimeBlock directly; if false, previews

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduledTaskProposal {
        private Long taskId;
        private String title;
        private String categoryType;
        private String priority;
        private String eisenhowerQuadrant;
        private OffsetDateTime startTime;
        private OffsetDateTime endTime;
        private Integer durationMinutes;
        private String slotRationale; // e.g., "Ưu tiên xếp lúc 08:30 vào khung giờ vàng vì là việc Khẩn cấp & Quan trọng (Q1)"
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String targetDate;
        private Integer scheduledCount;
        private Integer remainingUnscheduled;
        private List<ScheduledTaskProposal> proposals;
        private String aiStrategySummary;
        private Boolean applied;
    }
}
