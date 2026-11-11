package com.taskmanager.service;

import com.taskmanager.dto.SubtaskDto;
import com.taskmanager.entity.CongViecCon;
import com.taskmanager.exception.ResourceNotFoundException;
import com.taskmanager.repository.SubtaskRepository;
import com.taskmanager.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubtaskService {

    private final SubtaskRepository subtaskRepository;
    private final TaskRepository taskRepository;

    @Transactional(readOnly = true)
    public List<SubtaskDto> getSubtasks(Long userId, Long taskId) {
        requireOwnedTask(userId, taskId);
        return subtaskRepository.findByTaskIdOrderByOrderIndexAsc(taskId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public SubtaskDto createSubtask(Long userId, Long taskId, SubtaskDto dto) {
        requireOwnedTask(userId, taskId);
        if (dto.getTitle() == null || dto.getTitle().isBlank() || dto.getTitle().trim().length() > 255) {
            throw new IllegalArgumentException("Tên công việc con phải có từ 1 đến 255 ký tự.");
        }
        if (dto.getDurationMinutes() != null && (dto.getDurationMinutes() < 1 || dto.getDurationMinutes() > 1440)) {
            throw new IllegalArgumentException("Thời lượng công việc con phải từ 1 đến 1.440 phút.");
        }
        if (dto.getOrderIndex() != null && (dto.getOrderIndex() < 0 || dto.getOrderIndex() > 10_000)) {
            throw new IllegalArgumentException("Thứ tự công việc con không hợp lệ.");
        }
        List<CongViecCon> existing = subtaskRepository.findByTaskIdOrderByOrderIndexAsc(taskId);
        int nextOrder = existing.size();

        CongViecCon subtask = CongViecCon.builder()
                .taskId(taskId)
                .title(dto.getTitle().trim())
                .isCompleted(Boolean.TRUE.equals(dto.getIsCompleted()))
                .durationMinutes(dto.getDurationMinutes() != null ? dto.getDurationMinutes() : 15)
                .orderIndex(dto.getOrderIndex() != null ? dto.getOrderIndex() : nextOrder)
                .build();

        return mapToDto(subtaskRepository.save(subtask));
    }

    @Transactional
    public SubtaskDto updateSubtask(Long userId, Long subtaskId, SubtaskDto dto) {
        CongViecCon subtask = subtaskRepository.findById(subtaskId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công việc con #" + subtaskId));
        requireOwnedTask(userId, subtask.getTaskId());

        if (dto.getTitle() != null && (dto.getTitle().isBlank() || dto.getTitle().trim().length() > 255)) {
            throw new IllegalArgumentException("Tên công việc con phải có từ 1 đến 255 ký tự.");
        }
        if (dto.getDurationMinutes() != null && (dto.getDurationMinutes() < 1 || dto.getDurationMinutes() > 1440)) {
            throw new IllegalArgumentException("Thời lượng công việc con phải từ 1 đến 1.440 phút.");
        }
        if (dto.getOrderIndex() != null && (dto.getOrderIndex() < 0 || dto.getOrderIndex() > 10_000)) {
            throw new IllegalArgumentException("Thứ tự công việc con không hợp lệ.");
        }

        if (dto.getTitle() != null) {
            subtask.setTitle(dto.getTitle().trim());
        }
        if (dto.getIsCompleted() != null) {
            subtask.setIsCompleted(dto.getIsCompleted());
        }
        if (dto.getDurationMinutes() != null) {
            subtask.setDurationMinutes(dto.getDurationMinutes());
        }
        if (dto.getOrderIndex() != null) {
            subtask.setOrderIndex(dto.getOrderIndex());
        }

        return mapToDto(subtaskRepository.save(subtask));
    }

    @Transactional
    public SubtaskDto toggleSubtask(Long userId, Long subtaskId) {
        CongViecCon subtask = subtaskRepository.findById(subtaskId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công việc con #" + subtaskId));
        requireOwnedTask(userId, subtask.getTaskId());
        subtask.setIsCompleted(!Boolean.TRUE.equals(subtask.getIsCompleted()));
        return mapToDto(subtaskRepository.save(subtask));
    }

    @Transactional
    public void deleteSubtask(Long userId, Long subtaskId) {
        CongViecCon subtask = subtaskRepository.findById(subtaskId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công việc con #" + subtaskId));
        requireOwnedTask(userId, subtask.getTaskId());
        subtaskRepository.deleteById(subtaskId);
    }

    private void requireOwnedTask(Long userId, Long taskId) {
        if (taskRepository.findByIdAndUserId(taskId, userId).isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy công việc #" + taskId);
        }
    }

    private SubtaskDto mapToDto(CongViecCon s) {
        return SubtaskDto.builder()
                .id(s.getId())
                .taskId(s.getTaskId())
                .title(s.getTitle())
                .isCompleted(s.getIsCompleted())
                .durationMinutes(s.getDurationMinutes())
                .orderIndex(s.getOrderIndex())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
