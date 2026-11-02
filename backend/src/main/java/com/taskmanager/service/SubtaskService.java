package com.taskmanager.service;

import com.taskmanager.dto.SubtaskDto;
import com.taskmanager.entity.Subtask;
import com.taskmanager.repository.SubtaskRepository;
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

    @Transactional(readOnly = true)
    public List<SubtaskDto> getSubtasks(Long taskId) {
        return subtaskRepository.findByTaskIdOrderByOrderIndexAsc(taskId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public SubtaskDto createSubtask(Long taskId, SubtaskDto dto) {
        List<Subtask> existing = subtaskRepository.findByTaskIdOrderByOrderIndexAsc(taskId);
        int nextOrder = existing.size();

        Subtask subtask = Subtask.builder()
                .taskId(taskId)
                .title(dto.getTitle().trim())
                .isCompleted(Boolean.TRUE.equals(dto.getIsCompleted()))
                .durationMinutes(dto.getDurationMinutes() != null ? dto.getDurationMinutes() : 15)
                .orderIndex(dto.getOrderIndex() != null ? dto.getOrderIndex() : nextOrder)
                .build();

        return mapToDto(subtaskRepository.save(subtask));
    }

    @Transactional
    public SubtaskDto updateSubtask(Long subtaskId, SubtaskDto dto) {
        Subtask subtask = subtaskRepository.findById(subtaskId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc con #" + subtaskId));

        if (dto.getTitle() != null && !dto.getTitle().trim().isEmpty()) {
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
    public SubtaskDto toggleSubtask(Long subtaskId) {
        Subtask subtask = subtaskRepository.findById(subtaskId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc con #" + subtaskId));
        subtask.setIsCompleted(!Boolean.TRUE.equals(subtask.getIsCompleted()));
        return mapToDto(subtaskRepository.save(subtask));
    }

    @Transactional
    public void deleteSubtask(Long subtaskId) {
        subtaskRepository.deleteById(subtaskId);
    }

    private SubtaskDto mapToDto(Subtask s) {
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
