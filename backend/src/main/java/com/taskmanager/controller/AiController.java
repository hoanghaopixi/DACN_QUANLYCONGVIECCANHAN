package com.taskmanager.controller;

import com.taskmanager.dto.request.AiParseTaskRequest;
import com.taskmanager.dto.response.AiParseTaskResponse;
import com.taskmanager.service.AiService;
import com.taskmanager.util.UserResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/ai")
@CrossOrigin(origins = "*")
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

        if (request.getUserId() == null) {
            request.setUserId(userResolver.resolveUserId(servletRequest));
        }

        log.info("Nhận yêu cầu bóc tách câu lệnh AI từ user {}: '{}'", request.getUserId(), request.getPrompt());
        AiParseTaskResponse response = aiService.parseTaskFromNaturalLanguage(request);
        return ResponseEntity.ok(response);
    }
}
