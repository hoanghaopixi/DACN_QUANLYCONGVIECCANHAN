package com.taskmanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDto {
    private Long id;
    private Long userId;
    private Long workspaceId;
    private Long categoryId;
    private String categoryType;
    private String title;
    private String description;
    private String note;
    private String priority;
    private String status;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private Integer estimatedDurationMinutes;
    private Boolean isRecurring;
    private String colorCode;
    private Boolean isAutoScheduled;
    private String subtext;
}
