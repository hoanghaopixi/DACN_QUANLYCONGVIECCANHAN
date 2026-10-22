package com.taskmanager.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiParseTaskResponse {
    private String title;
    private String description;
    private String categoryType; // WORK, STUDY, HEALTH, ENTERTAINMENT
    private String startTime;    // ISO-8601 string: "2026-10-18T15:00:00+07:00"
    private String endTime;      // ISO-8601 string: "2026-10-18T16:30:00+07:00"
    private Integer estimatedDurationMinutes;
    private String priority;     // LOW, MEDIUM, HIGH, URGENT
    private Boolean isFixedTime;
    private String formattedDateLabel; // "Thứ Tư, 14/10 · 15:00–16:30"
    private String note;

    // Phát hiện và giải quyết xung đột lịch
    private Boolean hasConflict;
    private String conflictMessage;
    private String conflictingTaskTitle;
    private String suggestedAlternativeStartTime;
    private String suggestedAlternativeEndTime;

    // Kiểm tra tính hợp lệ của nội dung yêu cầu
    private Boolean isValidTask;
    private String validationMessage;
    private String validationType; // GREETING, INAPPROPRIATE, NONSENSE, VALID

    // Danh sách TẤT CẢ các công việc nếu câu lệnh chứa nhiều lịch trình
    private java.util.List<AiParseTaskResponse> tasks;
}
