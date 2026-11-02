package com.taskmanager.service;

import com.taskmanager.dto.HabitLogDto;
import com.taskmanager.entity.HabitLog;
import com.taskmanager.entity.Task;
import com.taskmanager.repository.HabitLogRepository;
import com.taskmanager.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HabitLogService {

    private final HabitLogRepository habitLogRepository;
    private final TaskRepository taskRepository;
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Transactional(readOnly = true)
    public List<HabitLogDto> getHabitLogs(Long userId) {
        return habitLogRepository.findByUserIdOrderByCheckInDateDesc(userId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<HabitLogDto> getTaskHabitLogs(Long taskId) {
        return habitLogRepository.findByTaskIdOrderByCheckInDateDesc(taskId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public HabitLogDto checkInHabit(Long userId, HabitLogDto dto) {
        LocalDate checkInDate = dto.getCheckInDate() != null ? dto.getCheckInDate() : LocalDate.now(VIETNAM_ZONE);

        Optional<HabitLog> existing = habitLogRepository.findByTaskIdAndCheckInDate(dto.getTaskId(), checkInDate);
        HabitLog logEntry;

        if (existing.isPresent()) {
            logEntry = existing.get();
            logEntry.setStatus(dto.getStatus() != null ? dto.getStatus() : "COMPLETED");
            if (dto.getNote() != null) logEntry.setNote(dto.getNote());
        } else {
            logEntry = HabitLog.builder()
                    .userId(userId)
                    .taskId(dto.getTaskId())
                    .checkInDate(checkInDate)
                    .status(dto.getStatus() != null ? dto.getStatus() : "COMPLETED")
                    .note(dto.getNote())
                    .build();
        }

        HabitLog saved = habitLogRepository.save(logEntry);

        // Update task status and streak
        taskRepository.findById(dto.getTaskId()).ifPresent(task -> {
            task.setStatus("COMPLETED".equalsIgnoreCase(saved.getStatus()) ? "COMPLETED" : "TODO");
            taskRepository.save(task);
        });

        return toDto(saved);
    }

    private HabitLogDto toDto(HabitLog h) {
        String taskTitle = null;
        if (h.getTaskId() != null) {
            taskTitle = taskRepository.findById(h.getTaskId()).map(Task::getTitle).orElse(null);
        }

        return HabitLogDto.builder()
                .id(h.getId())
                .userId(h.getUserId())
                .taskId(h.getTaskId())
                .taskTitle(taskTitle)
                .checkInDate(h.getCheckInDate())
                .status(h.getStatus())
                .note(h.getNote())
                .createdAt(h.getCreatedAt())
                .build();
    }
}
