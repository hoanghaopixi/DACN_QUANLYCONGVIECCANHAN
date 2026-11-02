package com.taskmanager.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiBreakdownRequest {
    private Long taskId;
    private String title;
    private String description;
    private Integer durationMinutes;
}
