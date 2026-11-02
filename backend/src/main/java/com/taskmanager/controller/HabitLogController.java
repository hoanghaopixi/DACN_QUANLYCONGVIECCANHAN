package com.taskmanager.controller;

import com.taskmanager.dto.HabitLogDto;
import com.taskmanager.service.HabitLogService;
import com.taskmanager.util.UserResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/habits")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class HabitLogController {

    private final HabitLogService habitLogService;
    private final UserResolver userResolver;

    @GetMapping
    public ResponseEntity<List<HabitLogDto>> getHabitLogs(HttpServletRequest request) {
        Long userId = userResolver.resolveUserId(request);
        return ResponseEntity.ok(habitLogService.getHabitLogs(userId));
    }

    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<List<HabitLogDto>> getTaskHabitLogs(@PathVariable Long taskId) {
        return ResponseEntity.ok(habitLogService.getTaskHabitLogs(taskId));
    }

    @PostMapping("/check-in")
    public ResponseEntity<HabitLogDto> checkIn(
            @RequestBody HabitLogDto dto,
            HttpServletRequest request) {
        Long userId = userResolver.resolveUserId(request);
        log.info("Check-in thói quen cho công việc #{} ngày {}", dto.getTaskId(), dto.getCheckInDate());
        return ResponseEntity.ok(habitLogService.checkInHabit(userId, dto));
    }
}
