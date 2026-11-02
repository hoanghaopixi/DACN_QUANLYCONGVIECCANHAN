package com.taskmanager.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private boolean success;
    private int status;
    private String error;
    private String message;
    private String path;

    @Builder.Default
    private OffsetDateTime timestamp = OffsetDateTime.now();
}

