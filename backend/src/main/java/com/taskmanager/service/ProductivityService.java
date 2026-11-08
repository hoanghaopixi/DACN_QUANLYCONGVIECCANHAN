package com.taskmanager.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import com.taskmanager.client.gemini.GeminiApiClient;
import com.taskmanager.dto.ProductivityReportDto;
import com.taskmanager.entity.Category;
import com.taskmanager.entity.ProductivityLog;
import com.taskmanager.entity.Task;
import com.taskmanager.entity.TimeBlock;
import com.taskmanager.repository.CategoryRepository;
import com.taskmanager.repository.ProductivityLogRepository;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.repository.TimeBlockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductivityService {

    private final TaskRepository taskRepository;
    private final ProductivityLogRepository productivityLogRepository;
    private final CategoryRepository categoryRepository;
    private final TimeBlockRepository timeBlockRepository;
    private final GeminiApiClient geminiApiClient;
    private final JsonMapper objectMapper;

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @Transactional
    public ProductivityReportDto getDailyReport(Long userId, String dateStr) {
        LocalDate reportDate = (dateStr != null && !dateStr.isBlank())
                ? LocalDate.parse(dateStr.trim()) : LocalDate.now(VIETNAM_ZONE);

        // Fetch user tasks & categories
        List<Task> allUserTasks = taskRepository.findByUserId(userId);
        List<Category> allCategories = categoryRepository.findByUserId(userId);
        Map<Long, Category> categoryMap = allCategories.stream()
                .collect(Collectors.toMap(Category::getId, c -> c, (a, b) -> a));

        // Fetch time blocks in 7-day range for accurate focus duration & trend
        OffsetDateTime weekStart = reportDate.minusDays(7).atStartOfDay(VIETNAM_ZONE).toOffsetDateTime();
        OffsetDateTime weekEnd = reportDate.plusDays(1).atStartOfDay(VIETNAM_ZONE).toOffsetDateTime();
        List<TimeBlock> recentTimeBlocks = timeBlockRepository.findByUserIdAndStartTimeBetween(userId, weekStart, weekEnd);

        // 1. Separate tasks strictly for reportDate
        List<Task> completedTodayList = allUserTasks.stream()
                .filter(t -> isCompletedOnDate(t, reportDate))
                .toList();

        List<Task> scheduledTodayList = allUserTasks.stream()
                .filter(t -> isScheduledOrDueOnDate(t, reportDate))
                .toList();

        List<Task> overdueList = allUserTasks.stream()
                .filter(t -> isOverdueOnDate(t, reportDate))
                .toList();

        int completedCount = completedTodayList.size();
        int overdueCount = overdueList.size();

        // Total tasks for the day: tasks scheduled/due today or completed today
        Set<Long> todayTaskIds = new HashSet<>();
        for (Task t : completedTodayList) {
            if (t.getId() != null) todayTaskIds.add(t.getId());
        }
        for (Task t : scheduledTodayList) {
            if (t.getId() != null) todayTaskIds.add(t.getId());
        }
        int totalTasksToday = Math.max(todayTaskIds.size(), completedCount);

        // 2. Focus minutes & focus sessions calculation
        List<TimeBlock> todayBlocks = recentTimeBlocks.stream()
                .filter(tb -> tb.getStartTime() != null && tb.getStartTime().atZoneSameInstant(VIETNAM_ZONE).toLocalDate().isEqual(reportDate))
                .toList();

        int taskFocusMins = completedTodayList.stream()
                .mapToInt(t -> (t.getActualDurationMinutes() != null && t.getActualDurationMinutes() > 0)
                        ? t.getActualDurationMinutes()
                        : (t.getEstimatedDurationMinutes() != null && t.getEstimatedDurationMinutes() > 0 ? t.getEstimatedDurationMinutes() : 30))
                .sum();

        int blockFocusMins = (int) todayBlocks.stream()
                .mapToLong(tb -> {
                    if (tb.getStartTime() != null && tb.getEndTime() != null) {
                        return Math.max(0, Duration.between(tb.getStartTime(), tb.getEndTime()).toMinutes());
                    }
                    return 0;
                }).sum();

        int totalFocusMinutes = Math.max(taskFocusMins, blockFocusMins);
        int totalFocusSessions = Math.max(completedCount, todayBlocks.size());

        // 3. On-time rate
        BigDecimal onTimeRate = BigDecimal.valueOf(100.0);
        if (completedCount + overdueCount > 0) {
            double rate = ((double) completedCount / (completedCount + overdueCount)) * 100.0;
            onTimeRate = BigDecimal.valueOf(Math.min(100.0, Math.max(0.0, rate))).setScale(1, RoundingMode.HALF_UP);
        }

        // 4. Productivity score (0 - 100)
        int score = calculateScore(totalTasksToday, completedCount, totalFocusMinutes, onTimeRate.doubleValue(), overdueCount);

        // 5. Streak calculation
        int streak = calculateStreak(allUserTasks, reportDate);

        // 6. Map completed and pending task items
        List<ProductivityReportDto.TaskItemDto> completedItems = completedTodayList.stream()
                .map(t -> mapToTaskItemDto(t, categoryMap))
                .toList();

        List<ProductivityReportDto.TaskItemDto> pendingItems = scheduledTodayList.stream()
                .filter(t -> !completedTodayList.contains(t) && !"COMPLETED".equalsIgnoreCase(t.getStatus()) && !"CANCELLED".equalsIgnoreCase(t.getStatus()))
                .map(t -> mapToTaskItemDto(t, categoryMap))
                .toList();

        // 7. AI Coach feedback generation
        String aiFeedback = generateFallbackFeedback(score, completedCount, totalTasksToday, totalFocusMinutes);
        String aiStrengths = generateFallbackStrengths(onTimeRate.doubleValue(), completedCount, totalFocusMinutes);
        String aiTips = generateFallbackTips(overdueCount, totalFocusMinutes, totalTasksToday);

        if (geminiApiClient.isConfigured()) {
            try {
                String completedTitles = completedTodayList.stream()
                        .map(Task::getTitle)
                        .limit(3)
                        .collect(Collectors.joining(", "));

                String prompt = String.format(
                        "Dữ liệu năng suất ngày %s: Hoàn thành: %d việc (%s). Tổng kế hoạch: %d việc. Thời gian làm việc tập trung: %d phút (%d phiên). Việc trễ hạn: %d. Tỷ lệ đúng hạn: %s%%. Điểm năng suất: %d/100. Chuỗi ngày liên tục: %d.",
                        reportDate, completedCount, completedTitles.isBlank() ? "không có" : completedTitles, totalTasksToday, totalFocusMinutes, totalFocusSessions, overdueCount, onTimeRate, score, streak);

                String systemInstruction = "Vai trò: Bạn là AI Huấn luyện viên Năng suất Cá nhân (Productivity Coach) của SmartTask AI.\n"
                        + "Nhiệm vụ: Dựa vào số liệu thực tế, hãy đưa ra đánh giá khích lệ, phân tích điểm mạnh và lời khuyên thiết thực cho ngày mai.\n"
                        + "Trả về đúng định dạng JSON:\n"
                        + "{\n"
                        + "  \"feedback\": \"Đánh giá tổng quan 1-2 câu ngắn gọn, truyền cảm hứng và bám sát số liệu\",\n"
                        + "  \"strengths\": \"1 điểm mạnh nổi bật nhất của người dùng hôm nay\",\n"
                        + "  \"tips\": \"1-2 lời khuyên hành động cụ thể cho ngày mai\"\n"
                        + "}";

                Optional<String> jsonOpt = geminiApiClient.generateJsonResponse(systemInstruction, prompt);
                if (jsonOpt.isPresent()) {
                    String clean = jsonOpt.get().trim();
                    if (clean.startsWith("```json")) clean = clean.substring(7);
                    if (clean.startsWith("```")) clean = clean.substring(3);
                    if (clean.endsWith("```")) clean = clean.substring(0, clean.length() - 3);
                    clean = clean.trim();

                    JsonNode node = objectMapper.readTree(clean);
                    if (node.has("feedback") && !node.path("feedback").asString().isBlank()) {
                        aiFeedback = node.path("feedback").asString();
                    }
                    if (node.has("strengths") && !node.path("strengths").asString().isBlank()) {
                        aiStrengths = node.path("strengths").asString();
                    }
                    if (node.has("tips") && !node.path("tips").asString().isBlank()) {
                        aiTips = node.path("tips").asString();
                    }
                }
            } catch (Exception e) {
                log.warn("Lỗi khi sinh nhận xét AI Coach qua Gemini ({}).", e.getClass().getSimpleName());
            }
        }

        // 8. Save or update ProductivityLog in PostgreSQL
        try {
            ProductivityLog logEntity = productivityLogRepository.findByUserIdAndReportDate(userId, reportDate)
                    .orElse(ProductivityLog.builder()
                            .userId(userId)
                            .reportDate(reportDate)
                            .build());

            logEntity.setTotalCompletedTasks(completedCount);
            logEntity.setTotalFocusMinutes(totalFocusMinutes);
            logEntity.setOnTimeRate(onTimeRate);
            logEntity.setOverdueTaskCount(overdueCount);
            logEntity.setAiFeedback(aiFeedback);
            productivityLogRepository.save(logEntity);
        } catch (Exception e) {
            log.warn("Không thể lưu ProductivityLog ({}).", e.getClass().getSimpleName());
        }

        // 9. Build past 7-day trend with real historical data
        List<ProductivityReportDto.DailySummaryDto> trends = buildWeekTrends(allUserTasks, recentTimeBlocks, reportDate);

        // 10. Dynamic category distribution
        Map<String, Integer> catDist = buildCategoryDistribution(allUserTasks, categoryMap, reportDate);

        return ProductivityReportDto.builder()
                .reportDate(reportDate)
                .totalCompletedTasks(completedCount)
                .totalTasks(totalTasksToday)
                .totalFocusMinutes(totalFocusMinutes)
                .totalFocusSessions(totalFocusSessions)
                .onTimeRate(onTimeRate)
                .overdueTaskCount(overdueCount)
                .productivityScore(score)
                .currentStreakDays(streak)
                .completedTaskItems(completedItems)
                .pendingTaskItems(pendingItems)
                .aiFeedback(aiFeedback)
                .aiStrengths(aiStrengths)
                .aiImprovementTips(aiTips)
                .pastWeekTrends(trends)
                .categoryDistribution(catDist)
                .build();
    }

    private boolean isCompletedOnDate(Task t, LocalDate date) {
        if (!"COMPLETED".equalsIgnoreCase(t.getStatus())) {
            return false;
        }
        if (t.getCompletedAt() != null) {
            return t.getCompletedAt().atZoneSameInstant(VIETNAM_ZONE).toLocalDate().isEqual(date);
        }
        if (t.getDueDate() != null && t.getDueDate().atZoneSameInstant(VIETNAM_ZONE).toLocalDate().isEqual(date)) {
            return true;
        }
        if (t.getFixedStartTime() != null && t.getFixedStartTime().atZoneSameInstant(VIETNAM_ZONE).toLocalDate().isEqual(date)) {
            return true;
        }
        if (t.getUpdatedAt() != null && t.getUpdatedAt().atZoneSameInstant(VIETNAM_ZONE).toLocalDate().isEqual(date)) {
            return true;
        }
        return false;
    }

    private boolean isScheduledOrDueOnDate(Task t, LocalDate date) {
        if (t.getDueDate() != null && t.getDueDate().atZoneSameInstant(VIETNAM_ZONE).toLocalDate().isEqual(date)) {
            return true;
        }
        if (t.getFixedStartTime() != null && t.getFixedStartTime().atZoneSameInstant(VIETNAM_ZONE).toLocalDate().isEqual(date)) {
            return true;
        }
        if (t.getCreatedAt() != null && t.getCreatedAt().atZoneSameInstant(VIETNAM_ZONE).toLocalDate().isEqual(date)) {
            return true;
        }
        return false;
    }

    private boolean isOverdueOnDate(Task t, LocalDate date) {
        if ("COMPLETED".equalsIgnoreCase(t.getStatus()) || "CANCELLED".equalsIgnoreCase(t.getStatus())) {
            return false;
        }
        if ("OVERDUE".equalsIgnoreCase(t.getStatus())) {
            return true;
        }
        if (t.getDueDate() != null) {
            LocalDate dueLocalDate = t.getDueDate().atZoneSameInstant(VIETNAM_ZONE).toLocalDate();
            return dueLocalDate.isBefore(date);
        }
        return false;
    }

    private int calculateScore(int total, int completed, int focusMinutes, double onTimeRate, int overdue) {
        if (total == 0 && completed == 0) {
            return 75; // Baseline neutral score for free/rest days
        }
        double completionPart = Math.min(1.0, (double) completed / Math.max(total, 1)) * 50.0;
        double focusPart = Math.min(focusMinutes / 3.0, 30.0);
        double punctualityPart = (onTimeRate / 100.0) * 20.0;
        double totalScore = completionPart + focusPart + punctualityPart;
        if (overdue > 0) {
            totalScore -= Math.min(overdue * 5.0, 20.0);
        }
        return (int) Math.max(10, Math.min(100, Math.round(totalScore)));
    }

    private int calculateStreak(List<Task> allTasks, LocalDate anchorDate) {
        int streak = 0;
        LocalDate cur = anchorDate;
        // Check if anchorDate has any completed tasks; if not and anchorDate is today, start check from yesterday
        boolean anchorHasTasks = allTasks.stream().anyMatch(t -> isCompletedOnDate(t, anchorDate));
        if (!anchorHasTasks && cur.isEqual(LocalDate.now(VIETNAM_ZONE))) {
            cur = cur.minusDays(1);
        }

        while (streak < 365) {
            final LocalDate checkDate = cur;
            boolean hasCompleted = allTasks.stream().anyMatch(t -> isCompletedOnDate(t, checkDate));
            if (hasCompleted) {
                streak++;
                cur = cur.minusDays(1);
            } else {
                break;
            }
        }
        return streak;
    }

    private List<ProductivityReportDto.DailySummaryDto> buildWeekTrends(
            List<Task> allTasks,
            List<TimeBlock> recentTimeBlocks,
            LocalDate anchorDate) {
        List<ProductivityReportDto.DailySummaryDto> list = new ArrayList<>();
        String[] dayNames = {"CN", "T2", "T3", "T4", "T5", "T6", "T7"};

        for (int i = 6; i >= 0; i--) {
            LocalDate d = anchorDate.minusDays(i);
            int dayOfWeekIdx = d.getDayOfWeek().getValue() % 7;
            String dayName = dayNames[dayOfWeekIdx];

            int dayCompleted = (int) allTasks.stream().filter(t -> isCompletedOnDate(t, d)).count();
            int dayTotal = (int) allTasks.stream().filter(t -> isCompletedOnDate(t, d) || isScheduledOrDueOnDate(t, d)).count();
            if (dayTotal < dayCompleted) dayTotal = dayCompleted;

            int dayTaskFocus = allTasks.stream()
                    .filter(t -> isCompletedOnDate(t, d))
                    .mapToInt(t -> (t.getActualDurationMinutes() != null && t.getActualDurationMinutes() > 0)
                            ? t.getActualDurationMinutes()
                            : (t.getEstimatedDurationMinutes() != null && t.getEstimatedDurationMinutes() > 0 ? t.getEstimatedDurationMinutes() : 30))
                    .sum();

            int dayBlockFocus = (int) recentTimeBlocks.stream()
                    .filter(tb -> tb.getStartTime() != null && tb.getStartTime().atZoneSameInstant(VIETNAM_ZONE).toLocalDate().isEqual(d))
                    .mapToLong(tb -> {
                        if (tb.getStartTime() != null && tb.getEndTime() != null) {
                            return Math.max(0, Duration.between(tb.getStartTime(), tb.getEndTime()).toMinutes());
                        }
                        return 0;
                    }).sum();

            int actualFocus = Math.max(dayTaskFocus, dayBlockFocus);
            int dayScore = calculateScore(dayTotal, dayCompleted, actualFocus, 100.0, 0);

            list.add(ProductivityReportDto.DailySummaryDto.builder()
                    .date(d)
                    .dayOfWeek(dayName)
                    .completedTasks(dayCompleted)
                    .focusMinutes(actualFocus)
                    .score(dayScore)
                    .build());
        }
        return list;
    }

    private Map<String, Integer> buildCategoryDistribution(
            List<Task> allTasks,
            Map<Long, Category> categoryMap,
            LocalDate reportDate) {
        Map<String, Integer> catDist = new LinkedHashMap<>();

        // Group tasks scheduled/due/completed on reportDate
        List<Task> targetTasks = allTasks.stream()
                .filter(t -> isCompletedOnDate(t, reportDate) || isScheduledOrDueOnDate(t, reportDate))
                .toList();

        // If today has no tasks, fallback to non-cancelled recent tasks so chart is never completely blank
        if (targetTasks.isEmpty()) {
            targetTasks = allTasks.stream()
                    .filter(t -> !"CANCELLED".equalsIgnoreCase(t.getStatus()))
                    .limit(30)
                    .toList();
        }

        for (Task t : targetTasks) {
            String catName = "Chung";
            if (t.getCategoryId() != null && categoryMap.containsKey(t.getCategoryId())) {
                catName = categoryMap.get(t.getCategoryId()).getName();
            }
            catDist.put(catName, catDist.getOrDefault(catName, 0) + 1);
        }

        if (catDist.isEmpty()) {
            catDist.put("Công việc", 0);
            catDist.put("Học tập", 0);
            catDist.put("Sức khỏe", 0);
            catDist.put("Giải trí", 0);
        }

        return catDist;
    }

    private ProductivityReportDto.TaskItemDto mapToTaskItemDto(Task t, Map<Long, Category> categoryMap) {
        String catName = "Chung";
        String catColor = "#6C5CE7";
        if (t.getCategoryId() != null && categoryMap.containsKey(t.getCategoryId())) {
            Category c = categoryMap.get(t.getCategoryId());
            catName = c.getName();
            if (c.getColorCode() != null && !c.getColorCode().isBlank()) {
                catColor = c.getColorCode();
            }
        }

        String dueStr = "Hôm nay";
        if (t.getDueDate() != null) {
            dueStr = t.getDueDate().atZoneSameInstant(VIETNAM_ZONE).format(TIME_FMT);
        } else if (t.getFixedStartTime() != null) {
            dueStr = t.getFixedStartTime().atZoneSameInstant(VIETNAM_ZONE).format(TIME_FMT);
        }

        int duration = (t.getActualDurationMinutes() != null && t.getActualDurationMinutes() > 0)
                ? t.getActualDurationMinutes()
                : (t.getEstimatedDurationMinutes() != null && t.getEstimatedDurationMinutes() > 0 ? t.getEstimatedDurationMinutes() : 30);

        return ProductivityReportDto.TaskItemDto.builder()
                .id(t.getId())
                .title(t.getTitle())
                .priority(t.getPriority() != null ? t.getPriority() : "MEDIUM")
                .durationMinutes(duration)
                .categoryName(catName)
                .categoryColor(catColor)
                .status(t.getStatus())
                .dueTimeStr(dueStr)
                .build();
    }

    private String generateFallbackFeedback(int score, int completed, int total, int focusMins) {
        if (score >= 85) {
            return "Bạn đã có một ngày làm việc bùng nổ với điểm số năng suất xuất sắc (" + score + "/100)! Nhịp độ tập trung và hoàn thành công việc được kiểm soát hoàn hảo.";
        } else if (completed > 0) {
            return "Bạn đã hoàn thành tốt " + completed + " công việc với tổng cộng " + focusMins + " phút tập trung sâu. Hãy tiếp tục duy trì phong độ này!";
        } else if (total > 0) {
            return "Hôm nay bạn có " + total + " công việc trong danh sách. Hãy chọn ngay 1 việc quan trọng nhất để khởi động ngày mới!";
        } else {
            return "Hôm nay là khoảng thời gian lý tưởng để bạn nạp lại năng lượng hoặc lập kế hoạch cho các mục tiêu mới sắp tới.";
        }
    }

    private String generateFallbackStrengths(double onTimeRate, int completed, int focusMins) {
        if (onTimeRate >= 95.0 && completed > 0) {
            return "Kỷ luật thời gian cực tốt, duy trì tỷ lệ đúng hạn " + onTimeRate + "%.";
        } else if (focusMins >= 60) {
            return "Duy trì sự tập trung làm việc sâu ổn định trong suốt " + focusMins + " phút.";
        } else if (completed > 0) {
            return "Chủ động xử lý dứt điểm các mục tiêu công việc quan trọng.";
        } else {
            return "Sẵn sàng khởi động và thiết lập nhịp làm việc hiệu quả cho các dự án.";
        }
    }

    private String generateFallbackTips(int overdue, int focusMins, int total) {
        if (overdue > 0) {
            return "Ưu tiên giải quyết hoặc dời lại " + overdue + " công việc quá hạn để tránh tích tụ áp lực.";
        } else if (focusMins < 45 && total > 0) {
            return "Thử áp dụng kỹ thuật Pomodoro (25 phút tập trung - 5 phút nghỉ) để tối ưu thời lượng làm việc sâu.";
        } else {
            return "Dành 10 phút cuối ngày để rà soát mục tiêu và sắp xếp lịch trình cho ngày mai theo ma trận Eisenhower.";
        }
    }
}
