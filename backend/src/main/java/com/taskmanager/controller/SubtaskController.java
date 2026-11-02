package com.taskmanager.controller;

import com.taskmanager.dto.SubtaskDto;
import com.taskmanager.service.SubtaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SubtaskController {

    private final SubtaskService subtaskService;

    @GetMapping("/{taskId}/subtasks")
    public ResponseEntity<List<SubtaskDto>> getSubtasks(@PathVariable Long taskId) {
        return ResponseEntity.ok(subtaskService.getSubtasks(taskId));
    }

    @PostMapping("/{taskId}/subtasks")
    public ResponseEntity<SubtaskDto> createSubtask(
            @PathVariable Long taskId,
            @RequestBody SubtaskDto dto) {
        return ResponseEntity.ok(subtaskService.createSubtask(taskId, dto));
    }

    @PutMapping("/subtasks/{subtaskId}")
    public ResponseEntity<SubtaskDto> updateSubtask(
            @PathVariable Long subtaskId,
            @RequestBody SubtaskDto dto) {
        return ResponseEntity.ok(subtaskService.updateSubtask(subtaskId, dto));
    }

    @PatchMapping("/subtasks/{subtaskId}/toggle")
    public ResponseEntity<SubtaskDto> toggleSubtask(@PathVariable Long subtaskId) {
        return ResponseEntity.ok(subtaskService.toggleSubtask(subtaskId));
    }

    @DeleteMapping("/subtasks/{subtaskId}")
    public ResponseEntity<Void> deleteSubtask(@PathVariable Long subtaskId) {
        subtaskService.deleteSubtask(subtaskId);
        return ResponseEntity.noContent().build();
    }
}
