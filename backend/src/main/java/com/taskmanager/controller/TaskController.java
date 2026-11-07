package com.taskmanager.controller;

import com.taskmanager.dto.TaskDto;
import com.taskmanager.service.TaskService;
import com.taskmanager.util.UserResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final UserResolver userResolver;

    @GetMapping
    public ResponseEntity<List<TaskDto>> getTasks(
            @RequestParam(required = false) Long workspaceId,
            HttpServletRequest request) {
        return ResponseEntity.ok(taskService.getTasks(userResolver.resolveUserId(request), workspaceId));
    }

    @PostMapping
    public ResponseEntity<TaskDto> createTask(@RequestBody TaskDto dto, HttpServletRequest request) {
        return ResponseEntity.ok(taskService.createTask(userResolver.resolveUserId(request), dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskDto> updateTask(@PathVariable Long id, @RequestBody TaskDto dto, HttpServletRequest request) {
        return ResponseEntity.ok(taskService.updateTask(userResolver.resolveUserId(request), id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id, HttpServletRequest request) {
        taskService.deleteTask(userResolver.resolveUserId(request), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(
            @RequestParam(required = false) Long workspaceId,
            HttpServletRequest request) {
        return ResponseEntity.ok(taskService.getStats(userResolver.resolveUserId(request), workspaceId));
    }

    @GetMapping("/eisenhower")
    public ResponseEntity<Map<String, List<TaskDto>>> getEisenhowerMatrix(
            @RequestParam(required = false) Long workspaceId,
            HttpServletRequest request) {
        return ResponseEntity.ok(taskService.getEisenhowerMatrix(userResolver.resolveUserId(request), workspaceId));
    }

    @PatchMapping("/{id}/quadrant")
    public ResponseEntity<TaskDto> updateQuadrant(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        String quadrant = body.get("quadrant");
        return ResponseEntity.ok(taskService.updateQuadrant(userResolver.resolveUserId(request), id, quadrant));
    }
}
