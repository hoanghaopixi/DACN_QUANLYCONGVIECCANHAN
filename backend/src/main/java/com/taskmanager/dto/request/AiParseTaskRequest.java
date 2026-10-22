package com.taskmanager.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiParseTaskRequest {
    private String prompt;
    private String currentDateTime; // Chuỗi ISO-8601 từ client, ví dụ: "2026-10-17T14:50:00+07:00"
    private Long workspaceId;
    private Long userId;
}
