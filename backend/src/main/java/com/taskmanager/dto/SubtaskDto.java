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
public class SubtaskDto {
    private Long id;
    private Long taskId;
    private String title;
    private Boolean isCompleted;
    private Integer durationMinutes;
    private Integer orderIndex;
    private OffsetDateTime createdAt;
}
