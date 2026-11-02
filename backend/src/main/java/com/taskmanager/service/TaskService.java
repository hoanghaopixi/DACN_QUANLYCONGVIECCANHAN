package com.taskmanager.service;

import com.taskmanager.dto.TaskDto;
import com.taskmanager.entity.Category;
import com.taskmanager.entity.Subtask;
import com.taskmanager.entity.Task;
import com.taskmanager.entity.TimeBlock;
import com.taskmanager.repository.CategoryRepository;
import com.taskmanager.repository.SubtaskRepository;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.repository.TimeBlockRepository;
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

    @jakarta.annotation.PostConstruct
    public void migrateWorkspacesForExistingData() {
        try {
            List<Task> allTasks = taskRepository.findAll();
            for (Task t : allTasks) {
                if (t.getWorkspaceId() == null) {
                    t.setWorkspaceId(1L);
                    taskRepository.save(t);
                }
            }
            List<Category> allCats = categoryRepository.findAll();
            for (Category c : allCats) {
                if (c.getWorkspaceId() == null) {
                    c.setWorkspaceId(1L);
                    categoryRepository.save(c);
                }
            }
        } catch (Exception e) {
            // ignore if table not ready yet
        }
    }

    @Transactional(readOnly = true)
    public List<TaskDto> getTasks(Long userId, Long workspaceId) {
        List<Task> tasks;
        if (workspaceId != null) {
            tasks = taskRepository.findByUserIdAndWorkspaceId(userId, workspaceId);
        } else {
            tasks = taskRepository.findByUserId(userId);
        }
        List<TimeBlock> blocks = timeBlockRepository.findByUserId(userId);
        Map<Long, TimeBlock> blockMap = new HashMap<>();
        for (TimeBlock b : blocks) {
            if (b.getTaskId() != null) {
                blockMap.put(b.getTaskId(), b);
            }
        }

        Map<Long, Category> categoryMap = new HashMap<>();
        for (Category c : categoryRepository.findByUserId(userId)) {
            categoryMap.put(c.getId(), c);
        }

        List<TaskDto> dtos = new ArrayList<>();
        for (Task t : tasks) {
            TimeBlock b = blockMap.get(t.getId());
            Category c = t.getCategoryId() != null ? categoryMap.get(t.getCategoryId()) : null;

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
            List<Subtask> subtasks = subtaskRepository.findByTaskIdOrderByOrderIndexAsc(t.getId());
            int subtasksCount = subtasks.size();
            int completedSubtasks = (int) subtasks.stream().filter(s -> Boolean.TRUE.equals(s.getIsCompleted())).count();

            // Calculate streak count for recurring task
            int streak = calculateStreak(t, tasks);

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
            workspaceId = 1L; // Mặc định workspace 1 (Không gian cá nhân)
        }

        // Find matching category by type name
        Long categoryId = dto.getCategoryId();
        if (categoryId == null && dto.getCategoryType() != null) {
            String catName = "Công việc";
            if ("STUDY".equalsIgnoreCase(dto.getCategoryType())) catName = "Học tập";
            else if ("HEALTH".equalsIgnoreCase(dto.getCategoryType())) catName = "Sức khỏe";
            else if ("ENTERTAINMENT".equalsIgnoreCase(dto.getCategoryType())) catName = "Giải trí";

            for (Category c : categoryRepository.findByUserId(userId)) {
                if (c.getName().equalsIgnoreCase(catName)) {
                    categoryId = c.getId();
                    break;
                }
            }
        }

        String taskNote = dto.getNote() != null && !dto.getNote().trim().isEmpty()
                ? dto.getNote().trim()
                : dto.getDescription();

        String priority = dto.getPriority() != null ? dto.getPriority() : "MEDIUM";
        String quadrant = dto.getEisenhowerQuadrant() != null ? dto.getEisenhowerQuadrant() : resolveDefaultQuadrant(priority);

        Task task = Task.builder()
                .userId(userId)
                .workspaceId(workspaceId)
                .categoryId(categoryId)
                .title(dto.getTitle())
                .description(taskNote)
                .note(taskNote)
                .priority(priority)
                .status(dto.getStatus() != null ? dto.getStatus() : "TODO")
                .eisenhowerQuadrant(quadrant)
                .isRecurring(Boolean.TRUE.equals(dto.getIsRecurring()))
                .recurrenceRule(dto.getRecurrenceRule() != null ? dto.getRecurrenceRule() : (Boolean.TRUE.equals(dto.getIsRecurring()) ? "DAILY" : null))
                .estimatedDurationMinutes(dto.getEstimatedDurationMinutes() != null ? dto.getEstimatedDurationMinutes() : 60)
                .actualDurationMinutes(dto.getActualDurationMinutes() != null ? dto.getActualDurationMinutes() : 0)
                .isLocked(Boolean.TRUE.equals(dto.getIsLocked()))
                .build();

        Task savedTask = taskRepository.save(task);

        OffsetDateTime start = dto.getStartTime() != null ? dto.getStartTime() : OffsetDateTime.now();
        OffsetDateTime end = dto.getEndTime() != null ? dto.getEndTime() : start.plusHours(1);

        TimeBlock block = TimeBlock.builder()
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
        Task task = requireOwnedTask(userId, taskId);

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
        if (dto.getPriority() != null) {
            task.setPriority(dto.getPriority());
            // If quadrant not specified, auto-sync when priority changes
            if (dto.getEisenhowerQuadrant() == null) {
                task.setEisenhowerQuadrant(resolveDefaultQuadrant(dto.getPriority()));
            }
        }
        if (dto.getEisenhowerQuadrant() != null) {
            task.setEisenhowerQuadrant(dto.getEisenhowerQuadrant());
        }
        if (dto.getStatus() != null) {
            task.setStatus(dto.getStatus());
            if ("COMPLETED".equalsIgnoreCase(dto.getStatus()) && !"COMPLETED".equalsIgnoreCase(oldStatus)) {
                task.setCompletedAt(OffsetDateTime.now());
            }
        }
        if (dto.getIsRecurring() != null) {
            task.setIsRecurring(dto.getIsRecurring());
        }
        if (dto.getRecurrenceRule() != null) {
            task.setRecurrenceRule(dto.getRecurrenceRule());
        }
        if (dto.getEstimatedDurationMinutes() != null) {
            task.setEstimatedDurationMinutes(dto.getEstimatedDurationMinutes());
        }
        if (dto.getActualDurationMinutes() != null) {
            task.setActualDurationMinutes(dto.getActualDurationMinutes());
        }
        if (dto.getCategoryId() != null) {
            task.setCategoryId(dto.getCategoryId());
        } else if (dto.getCategoryType() != null) {
            String catName = "Công việc";
            if ("STUDY".equalsIgnoreCase(dto.getCategoryType())) catName = "Học tập";
            else if ("HEALTH".equalsIgnoreCase(dto.getCategoryType())) catName = "Sức khỏe";
            else if ("ENTERTAINMENT".equalsIgnoreCase(dto.getCategoryType())) catName = "Giải trí";

            for (Category c : categoryRepository.findByUserId(userId)) {
                if (c.getName().equalsIgnoreCase(catName)) {
                    task.setCategoryId(c.getId());
                    break;
                }
            }
        }
        if (dto.getWorkspaceId() != null) {
            task.setWorkspaceId(dto.getWorkspaceId());
        }
        if (dto.getIsLocked() != null) {
            task.setIsLocked(dto.getIsLocked());
        }
        Task updatedTask = taskRepository.save(task);

        // Update corresponding TimeBlock if time or title changed
        boolean foundBlock = false;
        List<TimeBlock> userBlocks = timeBlockRepository.findByUserId(userId);
        OffsetDateTime currentBlockStart = null;
        OffsetDateTime currentBlockEnd = null;

        for (TimeBlock b : userBlocks) {
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
            TimeBlock newBlock = TimeBlock.builder()
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
        if ("COMPLETED".equalsIgnoreCase(dto.getStatus()) && !"COMPLETED".equalsIgnoreCase(oldStatus)
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
    private void spawnNextRecurrence(Long userId, Task completedTask, OffsetDateTime baseStart, OffsetDateTime baseEnd) {
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

            Task nextTask = Task.builder()
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

            Task savedNext = taskRepository.save(nextTask);

            TimeBlock nextBlock = TimeBlock.builder()
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

            log.info("Đã tự động tạo lần lặp kế tiếp cho công việc '{}' vào {}", savedNext.getTitle(), nextStart);
        } catch (Exception e) {
            log.error("Lỗi khi tạo lần lặp kế tiếp: {}", e.getMessage());
        }
    }

    /**
     * Tính chuỗi ngày liên tiếp (Streak) của thói quen
     */
    private int calculateStreak(Task task, List<Task> allUserTasks) {
        if (!Boolean.TRUE.equals(task.getIsRecurring())) return 0;
        String title = task.getTitle().trim().toLowerCase();
        int completedCount = 0;
        for (Task t : allUserTasks) {
            if (t.getTitle().trim().equalsIgnoreCase(title) && "COMPLETED".equalsIgnoreCase(t.getStatus())) {
                completedCount++;
            }
        }
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

    @Transactional
    public void deleteTask(Long userId, Long taskId) {
        requireOwnedTask(userId, taskId);
        subtaskRepository.deleteByTaskId(taskId);
        timeBlockRepository.deleteByTaskId(taskId);
        taskRepository.deleteById(taskId);
    }

    @Transactional
    public TaskDto updateQuadrant(Long userId, Long taskId, String quadrant) {
        Task task = requireOwnedTask(userId, taskId);
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

    private Task requireOwnedTask(Long userId, Long taskId) {
        Task task = taskRepository.findById(taskId)
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

        List<Task> scopedTasks = workspaceId != null
                ? taskRepository.findByUserIdAndWorkspaceId(userId, workspaceId)
                : taskRepository.findByUserId(userId);
        Map<Long, Task> tasksById = new HashMap<>();
        for (Task task : scopedTasks) {
            tasksById.put(task.getId(), task);
        }

        List<TimeBlock> blocksInWeek = timeBlockRepository.findByUserId(userId).stream()
                .filter(block -> block.getStartTime() != null && block.getEndTime() != null)
                .filter(block -> !block.getStartTime().isAfter(sunday) && !block.getEndTime().isBefore(monday))
                .filter(block -> workspaceId == null
                        ? block.getTaskId() == null || tasksById.containsKey(block.getTaskId())
                        : block.getTaskId() != null && tasksById.containsKey(block.getTaskId()))
                .toList();

        long totalTasksThisWeek = blocksInWeek.size();
        long completedTasks = 0;
        double totalDurationMinutes = 0;

        for (TimeBlock b : blocksInWeek) {
            if (b.getTaskId() != null) {
                Task t = tasksById.get(b.getTaskId());
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
