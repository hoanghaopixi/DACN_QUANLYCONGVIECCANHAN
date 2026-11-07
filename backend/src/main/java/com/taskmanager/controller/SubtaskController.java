package com.taskmanager.controller;

import com.taskmanager.dto.SubtaskDto;
import com.taskmanager.service.SubtaskService;
import com.taskmanager.util.UserResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class SubtaskController {

    private final SubtaskService subtaskService;
    private final UserResolver userResolver;

    @GetMapping("/{taskId}/subtasks")
    public ResponseEntity<List<SubtaskDto>> getSubtasks(@PathVariable Long taskId, HttpServletRequest request) {
        return ResponseEntity.ok(subtaskService.getSubtasks(userResolver.resolveUserId(request), taskId));
    }

    @PostMapping("/{taskId}/subtasks")
    public ResponseEntity<SubtaskDto> createSubtask(
            @PathVariable Long taskId,
            @RequestBody SubtaskDto dto,
            HttpServletRequest request) {
        return ResponseEntity.ok(subtaskService.createSubtask(userResolver.resolveUserId(request), taskId, dto));
    }

    @PutMapping("/subtasks/{subtaskId}")
    public ResponseEntity<SubtaskDto> updateSubtask(
            @PathVariable Long subtaskId,
            @RequestBody SubtaskDto dto,
            HttpServletRequest request) {
        return ResponseEntity.ok(subtaskService.updateSubtask(userResolver.resolveUserId(request), subtaskId, dto));
    }

    @PatchMapping("/subtasks/{subtaskId}/toggle")
    public ResponseEntity<SubtaskDto> toggleSubtask(@PathVariable Long subtaskId, HttpServletRequest request) {
        return ResponseEntity.ok(subtaskService.toggleSubtask(userResolver.resolveUserId(request), subtaskId));
    }

    @DeleteMapping("/subtasks/{subtaskId}")
    public ResponseEntity<Void> deleteSubtask(@PathVariable Long subtaskId, HttpServletRequest request) {
        subtaskService.deleteSubtask(userResolver.resolveUserId(request), subtaskId);
        return ResponseEntity.noContent().build();
    }
}
