package com.taskmanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HabitLogDto {
    private Long id;
    private Long userId;
    private Long taskId;
    private String taskTitle;
    private LocalDate checkInDate;
    private String status;
    private String note;
    private OffsetDateTime createdAt;
}
