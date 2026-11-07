package com.taskmanager.controller;

import com.taskmanager.dto.AutoScheduleDto;
import com.taskmanager.dto.SubtaskDto;
import com.taskmanager.dto.request.AiParseTaskRequest;
import com.taskmanager.dto.response.AiParseTaskResponse;
import com.taskmanager.service.AiService;
import com.taskmanager.util.UserResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/ai")
public class AiController {

    private final AiService aiService;
    private final UserResolver userResolver;

    public AiController(AiService aiService, UserResolver userResolver) {
        this.aiService = aiService;
        this.userResolver = userResolver;
    }

    @PostMapping("/parse-task")
    public ResponseEntity<AiParseTaskResponse> parseTask(
            @RequestBody AiParseTaskRequest request,
            HttpServletRequest servletRequest) {

        Long userId = userResolver.resolveUserId(servletRequest);
        request.setUserId(userId);
        log.info("Nhận yêu cầu bóc tách câu lệnh AI từ user {}", userId);
        AiParseTaskResponse response = aiService.parseTaskFromNaturalLanguage(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/tasks/{taskId}/breakdown")
    public ResponseEntity<List<SubtaskDto>> breakdownTask(
            @PathVariable Long taskId,
            HttpServletRequest servletRequest) {
        Long userId = userResolver.resolveUserId(servletRequest);
        log.info("Nhận yêu cầu phân rã công việc #{} bằng AI từ user {}", taskId, userId);
        return ResponseEntity.ok(aiService.breakdownTask(userId, taskId));
    }

    @PostMapping("/breakdown")
    public ResponseEntity<List<SubtaskDto>> breakdownTaskByPrompt(
            @RequestBody com.taskmanager.dto.request.AiBreakdownRequest request,
            HttpServletRequest servletRequest) {
        Long userId = userResolver.resolveUserId(servletRequest);
        log.info("Nhận yêu cầu phân rã công việc trực tiếp bằng Gemini AI từ user {}", userId);
        return ResponseEntity.ok(aiService.breakdownTaskByPrompt(userId, request));
    }

    @PostMapping("/auto-schedule")
    public ResponseEntity<AutoScheduleDto.Response> autoSchedule(
            @RequestBody(required = false) AutoScheduleDto request,
            HttpServletRequest servletRequest) {
        Long userId = userResolver.resolveUserId(servletRequest);
        if (request == null) {
            request = new AutoScheduleDto();
        }
        log.info("Nhận yêu cầu tự động xếp lịch AI từ user {}", userId);
        return ResponseEntity.ok(aiService.autoSchedule(userId, request));
    }
}
