package com.taskmanager.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferenceDto {
    private Long id;
    private Long userId;
    private String fullName;
    private String phone;
    private String avatarUrl;
    private String language;
    private String workStartTime; // HH:mm format e.g. "08:00"
    private String workEndTime;   // HH:mm format e.g. "18:00"
    private String lunchBreakStart; // e.g. "12:00"
    private String lunchBreakEnd;   // e.g. "13:00"
    private String workDays;      // "MON,TUE,WED,THU,FRI"
    private Integer bufferMinutes;

    @JsonAlias({"allowWeekends", "allowWeekend"})
    private Boolean allowWeekendTasks;

    private String peakProductivityTime; // MORNING, AFTERNOON, EVENING
    private Boolean autoReschedule;

    @JsonAlias({"autoFindGaps", "autoScheduleUnscheduled"})
    private Boolean aiAutoFindSlot;

    @JsonAlias({"alwaysAskBeforeReschedule", "alwaysAskBeforeChange"})
    private Boolean aiAlwaysAskBeforeChange;

    @JsonAlias({"allowTaskBreakdown", "allowSplitLongTasks"})
    private Boolean aiAllowSplitLongTasks;

    @JsonAlias({"autoRescheduleFixedTasks", "autoMoveFixedTasks"})
    private Boolean aiAutoMoveFixedTasks;

    private String timeZone;
    private String theme;
    private String backgroundUrl;
    private String backgroundType; // NONE, IMAGE, VIDEO
    private Integer overlayDarkness; // 0 to 100 percentage (e.g. 50)

    @JsonAlias({"reminderBeforeMinutes", "reminderBefore"})
    private Integer reminderMinutesBefore;

    @JsonAlias({"doNotDisturbStart", "dndStart"})
    private String dndStartTime; // e.g. "22:00"

    @JsonAlias({"doNotDisturbEnd", "dndEnd"})
    private String dndEndTime;   // e.g. "07:00"

    @JsonAlias({"deviceNotifications", "browserNotifications"})
    private Boolean enableDeviceNotifications;

    @JsonAlias({"morningBriefingEmail", "morningSummaryEmail"})
    private Boolean enableMorningSummaryEmail;
}
