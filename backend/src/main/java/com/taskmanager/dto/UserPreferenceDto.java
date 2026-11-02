package com.taskmanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferenceDto {
    private Long id;
    private Long userId;
    private String workStartTime; // HH:mm format e.g. "08:00"
    private String workEndTime;   // HH:mm format e.g. "18:00"
    private String workDays;      // "MON,TUE,WED,THU,FRI"
    private Integer bufferMinutes;
    private String peakProductivityTime; // MORNING, AFTERNOON, EVENING
    private Boolean autoReschedule;
    private String timeZone;
    private String theme;
    private String backgroundUrl;
    private String backgroundType; // NONE, IMAGE, VIDEO
    private Integer overlayDarkness; // 0 to 100 percentage (e.g. 50)
}
