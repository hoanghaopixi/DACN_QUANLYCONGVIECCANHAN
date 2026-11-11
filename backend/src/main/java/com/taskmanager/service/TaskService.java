package com.taskmanager.service;

import com.taskmanager.dto.TaskDto;
import com.taskmanager.entity.CongViec;
import com.taskmanager.entity.CongViecCon;
import com.taskmanager.entity.DanhMuc;
import com.taskmanager.entity.KhoiThoiGian;
import com.taskmanager.entity.KhongGianLamViec;
import com.taskmanager.exception.ResourceNotFoundException;
import com.taskmanager.repository.CategoryRepository;
import com.taskmanager.repository.SubtaskRepository;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.repository.TimeBlockRepository;
import com.taskmanager.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TimeBlockRepository timeBlockRepository;
    private final CategoryRepository categoryRepository;
    private final SubtaskRepository subtaskRepository;
    private final WorkspaceRepository workspaceRepository;

    @Transactional(readOnly = true)
    public List<TaskDto> getTasks(Long userId, Long workspaceId) {
        List<CongViec> tasks;
        if (workspaceId != null) {
            tasks = taskRepository.findByUserIdAndWorkspaceId(userId, workspaceId);
        } else {
            tasks = taskRepository.findByUserId(userId);
        }
        List<KhoiThoiGian> blocks = timeBlockRepository.findByUserId(userId);
        Map<Long, KhoiThoiGian> blockMap = new HashMap<>();
        for (KhoiThoiGian b : blocks) {
            if (b.getTaskId() != null) {
                blockMap.put(b.getTaskId(), b);
            }
        }

        Map<Long, DanhMuc> categoryMap = new HashMap<>();
        for (DanhMuc c : categoryRepository.findByUserId(userId)) {
            categoryMap.put(c.getId(), c);
        }

        Map<Long, List<CongViecCon>> subtasksByTaskId = new HashMap<>();
        List<Long> taskIds = tasks.stream().map(CongViec::getId).filter(Objects::nonNull).toList();
        if (!taskIds.isEmpty()) {
            for (CongViecCon subtask : subtaskRepository.findByTaskIdInOrderByOrderIndexAsc(taskIds)) {
                subtasksByTaskId.computeIfAbsent(subtask.getTaskId(), ignored -> new ArrayList<>()).add(subtask);
            }
        }

        Map<String, Integer> completedCountsByTitle = new HashMap<>();
        for (CongViec task : tasks) {
            if ("COMPLETED".equalsIgnoreCase(task.getStatus()) && task.getTitle() != null) {
                completedCountsByTitle.merge(task.getTitle().trim().toLowerCase(Locale.ROOT), 1, Integer::sum);
            }
        }

        List<TaskDto> dtos = new ArrayList<>();
        for (CongViec t : tasks) {
            KhoiThoiGian b = blockMap.get(t.getId());
            DanhMuc c = t.getCategoryId() != null ? categoryMap.get(t.getCategoryId()) : null;

            String categoryType = "WORK";
            if (c != null) {
                if ("Học tập".equalsIgnoreCase(c.getName())) categoryType = "STUDY";
                else if ("Sức khỏe".equalsIgnoreCase(c.getName())) categoryType = "HEALTH";
                else if ("Giải trí".equalsIgnoreCase(c.getName())) categoryType = "ENTERTAINMENT";
            }

            OffsetDateTime start = (b != null) ? b.getStartTime() : (t.getFixedStartTime() != null ? t.getFixedStartTime() : OffsetDateTime.now());
            OffsetDateTime end = (b != null) ? b.getEndTime() : (t.getFixedEndTime() != null ? t.getFixedEndTime() : start.plusHours(1));

            String colorCode = (b != null && b.getColorCode() != null) ? b.getColorCode() : (c != null ? c.getColorCode() : "#6C5CE7");

            String effectiveNote = (t.getNote() != null && !t.getNote().trim().isEmpty())
                    ? t.getNote().trim()
                    : (b != null && b.getNote() != null && !b.getNote().trim().isEmpty())
                    ? b.getNote().trim()
                    : t.getDescription();

            // Calculate subtasks progress
            List<CongViecCon> subtasks = subtasksByTaskId.getOrDefault(t.getId(), List.of());
            int subtasksCount = subtasks.size();
            int completedSubtasks = (int) subtasks.stream().filter(s -> Boolean.TRUE.equals(s.getIsCompleted())).count();

            // Calculate streak count for recurring task
            int streak = calculateStreak(t, completedCountsByTitle);

            dtos.add(TaskDto.builder()
                    .id(t.getId())
                    .userId(t.getUserId())
                    .workspaceId(t.getWorkspaceId())
                    .categoryId(t.getCategoryId())
                    .categoryType(categoryType)
                    .title(t.getTitle())
                    .description(effectiveNote)
                    .note(effectiveNote)
                    .priority(t.getPriority())
                    .status(t.getStatus())
                    .eisenhowerQuadrant(t.getEisenhowerQuadrant() != null ? t.getEisenhowerQuadrant() : resolveDefaultQuadrant(t.getPriority()))
                    .startTime(start)
                    .endTime(end)
                    .estimatedDurationMinutes(t.getEstimatedDurationMinutes())
                    .actualDurationMinutes(t.getActualDurationMinutes() != null ? t.getActualDurationMinutes() : 0)
                    .isRecurring(t.getIsRecurring())
                    .recurrenceRule(t.getRecurrenceRule() != null ? t.getRecurrenceRule() : "DAILY")
                    .streakCount(streak)
                    .colorCode(colorCode)
                    .isAutoScheduled(b != null && Boolean.TRUE.equals(b.getIsAutoScheduled()))
                    .subtext("COMPLETED".equalsIgnoreCase(t.getStatus()) ? "✓ Đã hoàn thành" :
                             "IN_PROGRESS".equalsIgnoreCase(t.getStatus()) ? "• Đang làm" : "Cần làm")
                    .subtasksCount(subtasksCount)
                    .completedSubtasksCount(completedSubtasks)
                    .isLocked(Boolean.TRUE.equals(t.getIsLocked()))
                    .build());
        }
        return dtos;
    }

    @Transactional
    public TaskDto createTask(Long userId, TaskDto dto) {
        Long workspaceId = dto.getWorkspaceId();
        if (workspaceId == null) {
            workspaceId = workspaceRepository.findFirstByUserIdOrderByIdAsc(userId)
                    .map(KhongGianLamViec::getId)
                    .orElseThrow(() -> new ResourceNotFoundException("Tài khoản chưa có không gian làm việc."));
        }
        if (!workspaceRepository.existsByIdAndUserId(workspaceId, userId)) {
            throw new ResourceNotFoundException("Không tìm thấy không gian làm việc.");
        }
        if (dto.getTitle() == null || dto.getTitle().isBlank() || dto.getTitle().trim().length() > 255) {
            throw new IllegalArgumentException("Tên công việc không được để trống.");
        }

        // Find matching category by type name
        Long categoryId = dto.getCategoryId();
        if (categoryId == null && dto.getCategoryType() != null) {
            String catName = "Công việc";
            if ("STUDY".equalsIgnoreCase(dto.getCategoryType())) catName = "Học tập";
            else if ("HEALTH".equalsIgnoreCase(dto.getCategoryType())) catName = "Sức khỏe";
            else if ("ENTERTAINMENT".equalsIgnoreCase(dto.getCategoryType())) catName = "Giải trí";

            for (DanhMuc c : categoryRepository.findByUserIdAndWorkspaceId(userId, workspaceId)) {
                if (c.getName().equalsIgnoreCase(catName)) {
                    categoryId = c.getId();
                    break;
                }
            }
        }
        if (categoryId != null) {
            DanhMuc category = categoryRepository.findByIdAndUserId(categoryId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhóm công việc."));
            if (category.getWorkspaceId() != null && !workspaceId.equals(category.getWorkspaceId())) {
                throw new IllegalArgumentException("Nhóm công việc không thuộc không gian đã chọn.");
            }
        }

        String taskNote = dto.getNote() != null && !dto.getNote().trim().isEmpty()
                ? dto.getNote().trim()
                : dto.getDescription();

        String priority = dto.getPriority() != null
                ? normalizeChoice(dto.getPriority(), Set.of("LOW", "MEDIUM", "HIGH", "URGENT"), "Mức độ ưu tiên")
                : "MEDIUM";
        String quadrant = dto.getEisenhowerQuadrant() != null
                ? normalizeChoice(dto.getEisenhowerQuadrant(), Set.of("DO_FIRST", "SCHEDULE", "DELEGATE", "DONT_DO"), "Phân loại ưu tiên")
                : resolveDefaultQuadrant(priority);
        String status = dto.getStatus() != null
                ? normalizeChoice(dto.getStatus(), Set.of("TODO", "IN_PROGRESS", "COMPLETED", "CANCELLED", "OVERDUE"), "Trạng thái công việc")
                : "TODO";
        if (dto.getRecurrenceRule() != null) {
            dto.setRecurrenceRule(normalizeChoice(dto.getRecurrenceRule(), Set.of("DAILY", "WEEKDAYS", "WEEKLY", "MONTHLY"), "Quy tắc lặp lại"));
        }
        if (dto.getCategoryType() != null) {
            normalizeChoice(dto.getCategoryType(), Set.of("WORK", "STUDY", "HEALTH", "ENTERTAINMENT"), "Nhóm công việc");
        }
        if (dto.getEstimatedDurationMinutes() != null
                && (dto.getEstimatedDurationMinutes() < 1 || dto.getEstimatedDurationMinutes() > 1440)) {
            throw new IllegalArgumentException("Thời lượng ước tính phải từ 1 đến 1440 phút.");
        }
        if (dto.getActualDurationMinutes() != null && dto.getActualDurationMinutes() < 0) {
            throw new IllegalArgumentException("Thời lượng thực tế không hợp lệ.");
        }

        CongViec task = CongViec.builder()
                .userId(userId)
                .workspaceId(workspaceId)
                .categoryId(categoryId)
                .title(dto.getTitle())
                .description(taskNote)
                .note(taskNote)
                .priority(priority)
                .status(status)
                .eisenhowerQuadrant(quadrant.trim().toUpperCase(Locale.ROOT))
                .isRecurring(Boolean.TRUE.equals(dto.getIsRecurring()))
                .recurrenceRule(dto.getRecurrenceRule() != null ? dto.getRecurrenceRule().trim().toUpperCase(Locale.ROOT) : (Boolean.TRUE.equals(dto.getIsRecurring()) ? "DAILY" : null))
                .estimatedDurationMinutes(dto.getEstimatedDurationMinutes() != null ? dto.getEstimatedDurationMinutes() : 60)
                .actualDurationMinutes(dto.getActualDurationMinutes() != null ? dto.getActualDurationMinutes() : 0)
                .isLocked(Boolean.TRUE.equals(dto.getIsLocked()))
                .build();

        CongViec savedTask = taskRepository.save(task);

        OffsetDateTime start = dto.getStartTime() != null ? dto.getStartTime() : OffsetDateTime.now();
        OffsetDateTime end = dto.getEndTime() != null ? dto.getEndTime() : start.plusHours(1);
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("Giờ kết thúc phải sau giờ bắt đầu.");
        }

        KhoiThoiGian block = KhoiThoiGian.builder()
                .userId(userId)
                .taskId(savedTask.getId())
                .title(savedTask.getTitle())
                .startTime(start)
                .endTime(end)
                .colorCode(dto.getColorCode() != null ? dto.getColorCode() : "#6C5CE7")
                .isAutoScheduled(Boolean.TRUE.equals(dto.getIsAutoScheduled()))
                .note(taskNote)
                .isLocked(Boolean.TRUE.equals(dto.getIsLocked()))
                .build();

        timeBlockRepository.save(block);

        dto.setId(savedTask.getId());
        dto.setUserId(userId);
        dto.setWorkspaceId(workspaceId);
        dto.setNote(taskNote);
        dto.setDescription(taskNote);
        dto.setEisenhowerQuadrant(quadrant);
        dto.setActualDurationMinutes(savedTask.getActualDurationMinutes());
        dto.setIsLocked(savedTask.getIsLocked());
        return dto;
    }

    @Transactional
    public TaskDto updateTask(Long userId, Long taskId, TaskDto dto) {
        CongViec task = requireOwnedTask(userId, taskId);

        if (dto.getTitle() != null && (dto.getTitle().isBlank() || dto.getTitle().trim().length() > 255)) {
            throw new IllegalArgumentException("Tên công việc phải có từ 1 đến 255 ký tự.");
        }
        String priority = dto.getPriority() != null
                ? normalizeChoice(dto.getPriority(), Set.of("LOW", "MEDIUM", "HIGH", "URGENT"), "Mức độ ưu tiên")
                : null;
        String quadrant = dto.getEisenhowerQuadrant() != null
                ? normalizeChoice(dto.getEisenhowerQuadrant(), Set.of("DO_FIRST", "SCHEDULE", "DELEGATE", "DONT_DO"), "Phân loại ưu tiên")
                : null;
        String status = dto.getStatus() != null
                ? normalizeChoice(dto.getStatus(), Set.of("TODO", "IN_PROGRESS", "COMPLETED", "CANCELLED", "OVERDUE"), "Trạng thái công việc")
                : null;
        String recurrenceRule = dto.getRecurrenceRule() != null
                ? normalizeChoice(dto.getRecurrenceRule(), Set.of("DAILY", "WEEKDAYS", "WEEKLY", "MONTHLY"), "Quy tắc lặp lại")
                : null;
        if (dto.getCategoryType() != null) {
            normalizeChoice(dto.getCategoryType(), Set.of("WORK", "STUDY", "HEALTH", "ENTERTAINMENT"), "Nhóm công việc");
        }
        if (dto.getEstimatedDurationMinutes() != null
                && (dto.getEstimatedDurationMinutes() < 1 || dto.getEstimatedDurationMinutes() > 1440)) {
            throw new IllegalArgumentException("Thời lượng ước tính phải từ 1 đến 1440 phút.");
        }
        if (dto.getActualDurationMinutes() != null && dto.getActualDurationMinutes() < 0) {
            throw new IllegalArgumentException("Thời lượng thực tế không hợp lệ.");
        }

        String oldStatus = task.getStatus();

        if (dto.getTitle() != null && !dto.getTitle().trim().isEmpty()) {
            task.setTitle(dto.getTitle().trim());
        }
        if (dto.getNote() != null) {
            task.setNote(dto.getNote().trim());
            task.setDescription(dto.getNote().trim());
        } else if (dto.getDescription() != null) {
            task.setNote(dto.getDescription().trim());
            task.setDescription(dto.getDescription().trim());
        }
        if (priority != null) {
            task.setPriority(priority);
            // If quadrant not specified, auto-sync when priority changes
            if (quadrant == null) {
                task.setEisenhowerQuadrant(resolveDefaultQuadrant(priority));
            }
        }
        if (quadrant != null) {
            task.setEisenhowerQuadrant(quadrant);
        }
        if (status != null) {
            task.setStatus(status);
            if ("COMPLETED".equals(status) && !"COMPLETED".equalsIgnoreCase(oldStatus)) {
                task.setCompletedAt(OffsetDateTime.now());
            }
        }
        if (dto.getIsRecurring() != null) {
            task.setIsRecurring(dto.getIsRecurring());
        }
        if (recurrenceRule != null) {
            task.setRecurrenceRule(recurrenceRule);
        }
        if (dto.getEstimatedDurationMinutes() != null) {
            task.setEstimatedDurationMinutes(dto.getEstimatedDurationMinutes());
        }
        if (dto.getActualDurationMinutes() != null) {
            task.setActualDurationMinutes(dto.getActualDurationMinutes());
        }
        if (dto.getCategoryId() != null) {
            DanhMuc category = categoryRepository.findByIdAndUserId(dto.getCategoryId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhóm công việc."));
            Long targetWorkspaceId = dto.getWorkspaceId() != null ? dto.getWorkspaceId() : task.getWorkspaceId();
            if (category.getWorkspaceId() != null && targetWorkspaceId != null
                    && !category.getWorkspaceId().equals(targetWorkspaceId)) {
                throw new IllegalArgumentException("Nhóm công việc không thuộc không gian hiện tại.");
            }
            task.setCategoryId(dto.getCategoryId());
        } else if (dto.getCategoryType() != null) {
            String catName = "Công việc";
            if ("STUDY".equalsIgnoreCase(dto.getCategoryType())) catName = "Học tập";
            else if ("HEALTH".equalsIgnoreCase(dto.getCategoryType())) catName = "Sức khỏe";
            else if ("ENTERTAINMENT".equalsIgnoreCase(dto.getCategoryType())) catName = "Giải trí";

            Long targetWorkspaceId = dto.getWorkspaceId() != null ? dto.getWorkspaceId() : task.getWorkspaceId();
            List<DanhMuc> categories = targetWorkspaceId != null
                    ? categoryRepository.findByUserIdAndWorkspaceId(userId, targetWorkspaceId)
                    : categoryRepository.findByUserId(userId);
            for (DanhMuc c : categories) {
                if (c.getName().equalsIgnoreCase(catName)) {
                    task.setCategoryId(c.getId());
                    break;
                }
            }
        }
        if (dto.getWorkspaceId() != null) {
            if (!workspaceRepository.existsByIdAndUserId(dto.getWorkspaceId(), userId)) {
                throw new ResourceNotFoundException("Không tìm thấy không gian làm việc.");
            }
            if (task.getCategoryId() != null) {
                DanhMuc existingCategory = categoryRepository.findByIdAndUserId(task.getCategoryId(), userId)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhóm công việc."));
                if (existingCategory.getWorkspaceId() != null
                        && !existingCategory.getWorkspaceId().equals(dto.getWorkspaceId())) {
                    throw new IllegalArgumentException("Hãy chọn nhóm thuộc không gian làm việc mới.");
                }
            }
            task.setWorkspaceId(dto.getWorkspaceId());
        }
        List<KhoiThoiGian> userBlocks = timeBlockRepository.findByUserId(userId);
        KhoiThoiGian existingTaskBlock = userBlocks.stream()
                .filter(block -> taskId.equals(block.getTaskId())).findFirst().orElse(null);
        OffsetDateTime resultingStart = dto.getStartTime();
        OffsetDateTime resultingEnd = dto.getEndTime();
        if (existingTaskBlock != null) {
            resultingStart = resultingStart != null ? resultingStart : existingTaskBlock.getStartTime();
            resultingEnd = resultingEnd != null ? resultingEnd : existingTaskBlock.getEndTime();
        } else if (resultingStart != null || resultingEnd != null) {
            resultingStart = resultingStart != null ? resultingStart : OffsetDateTime.now();
            resultingEnd = resultingEnd != null ? resultingEnd : resultingStart.plusHours(1);
        }
        if (resultingStart != null && resultingEnd != null && !resultingEnd.isAfter(resultingStart)) {
            throw new IllegalArgumentException("Giờ kết thúc phải sau giờ bắt đầu.");
        }
        if (dto.getIsLocked() != null) {
            task.setIsLocked(dto.getIsLocked());
        }
        CongViec updatedTask = taskRepository.save(task);

        // Update corresponding TimeBlock if time or title changed
        boolean foundBlock = false;
        OffsetDateTime currentBlockStart = null;
        OffsetDateTime currentBlockEnd = null;

        for (KhoiThoiGian b : userBlocks) {
            if (taskId.equals(b.getTaskId())) {
                if (dto.getTitle() != null && !dto.getTitle().trim().isEmpty()) {
                    b.setTitle(dto.getTitle().trim());
                } else {
                    b.setTitle(updatedTask.getTitle());
                }
                if (dto.getStartTime() != null) b.setStartTime(dto.getStartTime());
                if (dto.getEndTime() != null) b.setEndTime(dto.getEndTime());
                if (dto.getColorCode() != null) b.setColorCode(dto.getColorCode());
                if (dto.getIsLocked() != null) b.setIsLocked(dto.getIsLocked());
                if (dto.getNote() != null) {
                    b.setNote(dto.getNote().trim());
                } else if (dto.getDescription() != null) {
                    b.setNote(dto.getDescription().trim());
                }
                timeBlockRepository.save(b);
                currentBlockStart = b.getStartTime();
                currentBlockEnd = b.getEndTime();
                foundBlock = true;
                break;
            }
        }

        // If no TimeBlock exists yet, create one if startTime is specified
        if (!foundBlock && (dto.getStartTime() != null || dto.getEndTime() != null)) {
            OffsetDateTime start = dto.getStartTime() != null ? dto.getStartTime() : OffsetDateTime.now();
            OffsetDateTime end = dto.getEndTime() != null ? dto.getEndTime() : start.plusHours(1);
            KhoiThoiGian newBlock = KhoiThoiGian.builder()
                    .userId(userId)
                    .taskId(taskId)
                    .title(updatedTask.getTitle())
                    .startTime(start)
                    .endTime(end)
                    .colorCode(dto.getColorCode() != null ? dto.getColorCode() : "#6C5CE7")
                    .isAutoScheduled(false)
                    .note(updatedTask.getNote())
                    .isLocked(updatedTask.getIsLocked())
                    .build();
            timeBlockRepository.save(newBlock);
            currentBlockStart = start;
            currentBlockEnd = end;
        }

        // Feature 6: Recurring Task Handling
        // If the task was just completed and is recurring, spawn the next recurrence instance!
        if ("COMPLETED".equals(status) && !"COMPLETED".equalsIgnoreCase(oldStatus)
                && Boolean.TRUE.equals(updatedTask.getIsRecurring())) {
            spawnNextRecurrence(userId, updatedTask, currentBlockStart, currentBlockEnd);
        }

        return TaskDto.builder()
                .id(updatedTask.getId())
                .userId(userId)
                .workspaceId(updatedTask.getWorkspaceId())
                .categoryId(updatedTask.getCategoryId())
                .categoryType(dto.getCategoryType())
                .title(updatedTask.getTitle())
                .description(updatedTask.getDescription())
                .note(updatedTask.getNote())
                .priority(updatedTask.getPriority())
                .status(updatedTask.getStatus())
                .eisenhowerQuadrant(updatedTask.getEisenhowerQuadrant())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .estimatedDurationMinutes(updatedTask.getEstimatedDurationMinutes())
                .actualDurationMinutes(updatedTask.getActualDurationMinutes())
                .isRecurring(updatedTask.getIsRecurring())
                .recurrenceRule(updatedTask.getRecurrenceRule())
                .isLocked(updatedTask.getIsLocked())
                .build();
    }

    /**
     * Tự động tạo lần lặp tiếp theo cho thói quen / công việc lặp lại
     */
    private void spawnNextRecurrence(Long userId, CongViec completedTask, OffsetDateTime baseStart, OffsetDateTime baseEnd) {
        try {
            OffsetDateTime startRef = baseStart != null ? baseStart : OffsetDateTime.now();
            OffsetDateTime endRef = baseEnd != null ? baseEnd : startRef.plusHours(1);

            String rule = completedTask.getRecurrenceRule() != null ? completedTask.getRecurrenceRule().toUpperCase() : "DAILY";
            OffsetDateTime nextStart;
            OffsetDateTime nextEnd;

            if ("WEEKDAYS".equals(rule)) {
                // Next Monday to Friday
                nextStart = startRef.plusDays(1);
                while (nextStart.getDayOfWeek() == DayOfWeek.SATURDAY || nextStart.getDayOfWeek() == DayOfWeek.SUNDAY) {
                    nextStart = nextStart.plusDays(1);
                }
                long duration = java.time.Duration.between(startRef, endRef).toMinutes();
                nextEnd = nextStart.plusMinutes(Math.max(duration, 30));
            } else if ("WEEKLY".equals(rule)) {
                nextStart = startRef.plusDays(7);
                nextEnd = endRef.plusDays(7);
            } else if ("MONTHLY".equals(rule)) {
                nextStart = startRef.plusMonths(1);
                nextEnd = endRef.plusMonths(1);
            } else {
                // Default: DAILY
                nextStart = startRef.plusDays(1);
                nextEnd = endRef.plusDays(1);
            }

            CongViec nextTask = CongViec.builder()
                    .userId(userId)
                    .workspaceId(completedTask.getWorkspaceId())
                    .categoryId(completedTask.getCategoryId())
                    .title(completedTask.getTitle())
                    .description(completedTask.getDescription())
                    .note(completedTask.getNote())
                    .priority(completedTask.getPriority())
                    .status("TODO")
                    .eisenhowerQuadrant(completedTask.getEisenhowerQuadrant())
                    .isRecurring(true)
                    .recurrenceRule(completedTask.getRecurrenceRule())
                    .parentTaskId(completedTask.getParentTaskId() != null ? completedTask.getParentTaskId() : completedTask.getId())
                    .estimatedDurationMinutes(completedTask.getEstimatedDurationMinutes())
                    .actualDurationMinutes(0)
                    .build();

            CongViec savedNext = taskRepository.save(nextTask);

            KhoiThoiGian nextBlock = KhoiThoiGian.builder()
                    .userId(userId)
                    .taskId(savedNext.getId())
                    .title(savedNext.getTitle())
                    .startTime(nextStart)
                    .endTime(nextEnd)
                    .colorCode("#6C5CE7")
                    .isAutoScheduled(false)
                    .note(savedNext.getNote())
                    .build();
            timeBlockRepository.save(nextBlock);

            log.info("Đã tự động tạo lần lặp kế tiếp cho công việc #{} vào {}", savedNext.getId(), nextStart);
        } catch (Exception e) {
            log.error("Lỗi khi tạo lần lặp kế tiếp: {}", e.getMessage());
        }
    }

    /**
     * Tính chuỗi ngày liên tiếp (Streak) của thói quen
     */
    private int calculateStreak(CongViec task, Map<String, Integer> completedCountsByTitle) {
        if (!Boolean.TRUE.equals(task.getIsRecurring())) return 0;
        String title = task.getTitle().trim().toLowerCase(Locale.ROOT);
        int completedCount = completedCountsByTitle.getOrDefault(title, 0);
        return Math.max(completedCount, 1);
    }

    /**
     * Xác định góc phần tư Eisenhower mặc định dựa trên mức độ ưu tiên
     */
    private String resolveDefaultQuadrant(String priority) {
        if ("URGENT".equalsIgnoreCase(priority)) return "DO_FIRST";
        if ("HIGH".equalsIgnoreCase(priority)) return "SCHEDULE";
        if ("LOW".equalsIgnoreCase(priority)) return "DELEGATE";
        return "SCHEDULE"; // MEDIUM
    }

    private String normalizeChoice(String value, Set<String> allowed, String fieldName) {
        String normalized = value != null ? value.trim().toUpperCase(Locale.ROOT) : "";
        if (!allowed.contains(normalized)) {
            throw new IllegalArgumentException(fieldName + " không hợp lệ.");
        }
        return normalized;
    }

    @Transactional
    public void deleteTask(Long userId, Long taskId) {
        requireOwnedTask(userId, taskId);
        subtaskRepository.deleteByTaskId(taskId);
        timeBlockRepository.deleteByTaskId(taskId);
        taskRepository.deleteById(taskId);
    }

    @Transactional
    public TaskDto updateQuadrant(Long userId, Long taskId, String quadrant) {
        CongViec task = requireOwnedTask(userId, taskId);
        if (quadrant == null || !Set.of("DO_FIRST", "SCHEDULE", "DELEGATE", "DONT_DO")
                .contains(quadrant.trim().toUpperCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Phân loại ưu tiên không hợp lệ");
        }
        task.setEisenhowerQuadrant(quadrant.trim().toUpperCase(Locale.ROOT));
        taskRepository.save(task);

        return TaskDto.builder()
                .id(task.getId())
                .userId(userId)
                .title(task.getTitle())
                .eisenhowerQuadrant(task.getEisenhowerQuadrant())
                .priority(task.getPriority())
                .status(task.getStatus())
                .build();
    }

    @Transactional(readOnly = true)
    public Map<String, List<TaskDto>> getEisenhowerMatrix(Long userId, Long workspaceId) {
        List<TaskDto> allTasks = getTasks(userId, workspaceId);

        List<TaskDto> doFirst = new ArrayList<>();
        List<TaskDto> schedule = new ArrayList<>();
        List<TaskDto> delegate = new ArrayList<>();
        List<TaskDto> dontDo = new ArrayList<>();

        for (TaskDto t : allTasks) {
            String q = t.getEisenhowerQuadrant() != null ? t.getEisenhowerQuadrant().toUpperCase() : "SCHEDULE";
            switch (q) {
                case "DO_FIRST":
                    doFirst.add(t);
                    break;
                case "DELEGATE":
                    delegate.add(t);
                    break;
                case "DONT_DO":
                    dontDo.add(t);
                    break;
                case "SCHEDULE":
                default:
                    schedule.add(t);
                    break;
            }
        }

        Map<String, List<TaskDto>> matrix = new HashMap<>();
        matrix.put("doFirst", doFirst);
        matrix.put("schedule", schedule);
        matrix.put("delegate", delegate);
        matrix.put("dontDo", dontDo);
        return matrix;
    }

    private CongViec requireOwnedTask(Long userId, Long taskId) {
        CongViec task = taskRepository.findById(taskId)
                .orElseThrow(() -> new com.taskmanager.exception.ResourceNotFoundException(
                        "Không tìm thấy công việc #" + taskId));
        if (!Objects.equals(task.getUserId(), userId)) {
            // Respond as if the task does not exist so IDs cannot be used to probe other accounts.
            throw new com.taskmanager.exception.ResourceNotFoundException(
                    "Không tìm thấy công việc #" + taskId);
        }
        return task;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStats(Long userId, Long workspaceId) {
        java.time.ZoneId zone = java.time.ZoneId.of("Asia/Ho_Chi_Minh");
        java.time.OffsetDateTime now = java.time.OffsetDateTime.now(zone);
        java.time.DayOfWeek currentDow = now.getDayOfWeek();
        java.time.OffsetDateTime monday = now.minusDays(currentDow.getValue() - 1)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);
        java.time.OffsetDateTime sunday = monday.plusDays(7).minusNanos(1);

        List<CongViec> scopedTasks = workspaceId != null
                ? taskRepository.findByUserIdAndWorkspaceId(userId, workspaceId)
                : taskRepository.findByUserId(userId);
        Map<Long, CongViec> tasksById = new HashMap<>();
        for (CongViec task : scopedTasks) {
            tasksById.put(task.getId(), task);
        }

        List<KhoiThoiGian> blocksInWeek = timeBlockRepository.findByUserId(userId).stream()
                .filter(block -> block.getStartTime() != null && block.getEndTime() != null)
                .filter(block -> !block.getStartTime().isAfter(sunday) && !block.getEndTime().isBefore(monday))
                .filter(block -> workspaceId == null
                        ? block.getTaskId() == null || tasksById.containsKey(block.getTaskId())
                        : block.getTaskId() != null && tasksById.containsKey(block.getTaskId()))
                .toList();

        long totalTasksThisWeek = blocksInWeek.size();
        long completedTasks = 0;
        double totalDurationMinutes = 0;

        for (KhoiThoiGian b : blocksInWeek) {
            if (b.getTaskId() != null) {
                CongViec t = tasksById.get(b.getTaskId());
                if (t != null && "COMPLETED".equalsIgnoreCase(t.getStatus())) {
                    completedTasks++;
                }
            }
            if (b.getStartTime() != null && b.getEndTime() != null) {
                long dur = java.time.Duration.between(b.getStartTime(), b.getEndTime()).toMinutes();
                totalDurationMinutes += Math.max(dur, 0);
            }
        }

        double focusHours = Math.round((totalDurationMinutes / 60.0) * 10.0) / 10.0;
        long freeSlots = Math.max(63 - totalTasksThisWeek, 0);

        Map<String, Object> map = new HashMap<>();
        map.put("totalTasksThisWeek", totalTasksThisWeek);
        map.put("completedTasks", completedTasks);
        map.put("focusHours", focusHours);
        map.put("focusLabel", totalTasksThisWeek > 0 ? "Học tập & Công việc" : "Chưa có lịch tuần này");
        map.put("freeSlots", freeSlots);
        return map;
    }
}
