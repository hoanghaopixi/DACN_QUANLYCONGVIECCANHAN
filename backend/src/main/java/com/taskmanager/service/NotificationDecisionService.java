package com.taskmanager.service;

import com.taskmanager.entity.Notification;
import com.taskmanager.entity.Task;
import com.taskmanager.entity.TimeBlock;
import com.taskmanager.entity.User;
import com.taskmanager.repository.NotificationRepository;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.repository.TimeBlockRepository;
import com.taskmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Thuật toán Quyết định Thông báo Tự động (Smart Notification Decision Engine)
 * Phân tích dữ liệu thực tế từ CSDL để đưa ra các quyết định tạo thông báo phù hợp,
 * có cơ chế khử trùng lặp vĩnh viễn theo taskId và theo khung 24h đối với gợi ý/nhắc nhở,
 * tránh tình trạng đổ lại thông báo cũ khi người dùng đã đọc hoặc xóa.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDecisionService {

    private final TaskRepository taskRepository;
    private final TimeBlockRepository timeBlockRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM");

    private String formatTime(OffsetDateTime dt) {
        if (dt == null) return "";
        return dt.atZoneSameInstant(VN_ZONE).format(TIME_FORMATTER);
    }

    private String formatDateTime(OffsetDateTime dt) {
        if (dt == null) return "";
        return dt.atZoneSameInstant(VN_ZONE).format(DATE_TIME_FORMATTER);
    }

    /**
     * Chạy định kỳ quét thông báo nền mỗi 3 phút cho tất cả người dùng
     */
    @Scheduled(fixedDelay = 180000)
    public void scheduledEvaluationForAllUsers() {
        try {
            List<User> users = userRepository.findAll();
            for (User u : users) {
                evaluateAndGenerateNotifications(u.getId());
            }
        } catch (Exception e) {
            log.error("Lỗi khi chạy quét định kỳ thuật toán thông báo: {}", e.getMessage());
        }
    }

    @Transactional
    public List<Notification> evaluateAndGenerateNotifications(Long userId) {
        log.info("Running Notification Decision Engine for userId: {}", userId);
        OffsetDateTime now = OffsetDateTime.now(VN_ZONE);
        OffsetDateTime recent24hCutoff = now.minusHours(24);

        List<Task> allTasks = taskRepository.findByUserId(userId);
        List<TimeBlock> allBlocks = timeBlockRepository.findByUserId(userId);

        List<Notification> newlyGenerated = new ArrayList<>();

        Map<Long, List<TimeBlock>> taskBlockMap = new HashMap<>();
        for (TimeBlock b : allBlocks) {
            if (b.getTaskId() != null) {
                taskBlockMap.computeIfAbsent(b.getTaskId(), k -> new ArrayList<>()).add(b);
            }
        }

        // ── THUẬT TOÁN 1: PHÁT HIỆN CÔNG VIỆC QUÁ HẠN (OVERDUE DETECTION ALGORITHM) ──
        for (Task task : allTasks) {
            if ("COMPLETED".equalsIgnoreCase(task.getStatus()) || "CANCELLED".equalsIgnoreCase(task.getStatus())) {
                continue;
            }

            OffsetDateTime due = task.getDueDate();
            if (due == null && task.getFixedEndTime() != null) {
                due = task.getFixedEndTime();
            }

            if (due != null) {
                if (due.isBefore(now)) {
                    // Task quá hạn: Khử trùng lặp vĩnh viễn theo taskId - nếu đã từng có thông báo alert cho task này thì KHÔNG tạo lại
                    boolean alreadyNotified = notificationRepository.existsByUserIdAndTaskIdAndType(
                            userId, task.getId(), "alert");

                    if (!alreadyNotified) {
                        long hoursOverdue = Math.max(1, Duration.between(due, now).toHours());
                        String timeUnit = hoursOverdue >= 24 ? (hoursOverdue / 24) + " ngày" : hoursOverdue + " giờ";

                        Notification overdueNotice = Notification.builder()
                                .userId(userId)
                                .taskId(task.getId())
                                .type("alert")
                                .title("⚠️ Quá hạn deadline: " + task.getTitle())
                                .message("Công việc này đã trễ hạn " + timeUnit + " (hạn chót: " + formatDateTime(due) + "). Hãy ưu tiên xử lý hoặc dời lịch!")
                                .isRead(false)
                                .isDeleted(false)
                                .createdAt(now)
                                .updatedAt(now)
                                .build();
                        newlyGenerated.add(overdueNotice);
                    }
                } else {
                    // ── THUẬT TOÁN 2: CẢNH BÁO DEADLINE SẮP ĐẾN TRONG VÒNG 12 TIẾNG ──
                    long hoursUntilDue = Duration.between(now, due).toHours();
                    if (hoursUntilDue <= 12) {
                        boolean alreadyNotified = notificationRepository.existsByUserIdAndTaskIdAndType(
                                userId, task.getId(), "alert");

                        if (!alreadyNotified) {
                            String remainingText = hoursUntilDue <= 1 
                                    ? Duration.between(now, due).toMinutes() + " phút nữa" 
                                    : hoursUntilDue + " giờ nữa";
                            Notification imminentNotice = Notification.builder()
                                    .userId(userId)
                                    .taskId(task.getId())
                                    .type("alert")
                                    .title("⏳ Sắp đến hạn chót: " + task.getTitle())
                                    .message("Hạn chót vào lúc " + formatTime(due) + " hôm nay (còn khoảng " + remainingText + "). Hãy tập trung hoàn thành đúng tiến độ.")
                                    .isRead(false)
                                    .isDeleted(false)
                                    .createdAt(now)
                                    .updatedAt(now)
                                    .build();
                            newlyGenerated.add(imminentNotice);
                        }
                    }
                }
            } else {
                // Kiểm tra nếu task chưa xong mà khối thời gian của nó đã kết thúc trong quá khứ (> 1 giờ trước)
                List<TimeBlock> blocks = taskBlockMap.get(task.getId());
                if (blocks != null && !blocks.isEmpty()) {
                    for (TimeBlock b : blocks) {
                        if (b.getEndTime() != null && b.getEndTime().isBefore(now.minusHours(1))) {
                            boolean alreadyNotified = notificationRepository.existsByUserIdAndTaskIdAndType(
                                    userId, task.getId(), "alert");
                            if (!alreadyNotified) {
                                String titleKey = "⚠️ Quá hạn lịch trình: " + task.getTitle();
                                Notification blockOverdueNotice = Notification.builder()
                                        .userId(userId)
                                        .taskId(task.getId())
                                        .type("alert")
                                        .title(titleKey)
                                        .message("Khung giờ của công việc này đã kết thúc lúc " + formatTime(b.getEndTime()) + " nhưng vẫn chưa được đánh dấu hoàn thành.")
                                        .isRead(false)
                                        .isDeleted(false)
                                        .createdAt(now)
                                        .updatedAt(now)
                                        .build();
                                newlyGenerated.add(blockOverdueNotice);
                                break;
                            }
                        }
                    }
                }
            }
        }

        // ── THUẬT TOÁN 3: NHẮC NHỞ LỊCH TRÌNH SẮP DIỄN RA (TRONG VÒNG 90 PHÚT) ──
        for (TimeBlock block : allBlocks) {
            OffsetDateTime start = block.getStartTime();
            if (start != null) {
                long minutesUntilStart = Duration.between(now, start).toMinutes();
                if (minutesUntilStart >= -10 && minutesUntilStart <= 90) {
                    String titleKey = "🔔 Nhắc nhở: " + block.getTitle();
                    boolean alreadyNotified = notificationRepository.existsByUserIdAndTitleAndCreatedAtAfter(
                            userId, titleKey, recent24hCutoff);

                    if (!alreadyNotified) {
                        String timeDesc = minutesUntilStart > 0 
                                ? "sẽ bắt đầu lúc " + formatTime(start) + " (khoảng " + minutesUntilStart + " phút nữa)"
                                : "đang diễn ra (bắt đầu lúc " + formatTime(start) + ")";

                        Notification reminderNotice = Notification.builder()
                                .userId(userId)
                                .taskId(block.getTaskId())
                                .type("reminder")
                                .title(titleKey)
                                .message("Sự kiện/công việc \"" + block.getTitle() + "\" " + timeDesc + ". Hãy chuẩn bị sẵn sàng.")
                                .isRead(false)
                                .isDeleted(false)
                                .createdAt(now)
                                .updatedAt(now)
                                .build();
                        newlyGenerated.add(reminderNotice);
                    }
                }
            }
        }

        // ── THUẬT TOÁN 4: PHÁT HIỆN XUNG ĐỘT LỊCH TRÌNH ──
        for (int i = 0; i < allBlocks.size(); i++) {
            TimeBlock b1 = allBlocks.get(i);
            if (b1.getStartTime() == null || b1.getEndTime() == null) continue;
            if (b1.getEndTime().isBefore(now.minusHours(2))) continue;

            for (int j = i + 1; j < allBlocks.size(); j++) {
                TimeBlock b2 = allBlocks.get(j);
                if (b2.getStartTime() == null || b2.getEndTime() == null) continue;

                if (b1.getStartTime().isBefore(b2.getEndTime()) && b2.getStartTime().isBefore(b1.getEndTime())) {
                    String conflictTitle = "⚠️ Xung đột lịch: " + b1.getTitle() + " & " + b2.getTitle();
                    boolean alreadyNotified = notificationRepository.existsByUserIdAndTitleAndCreatedAtAfter(
                            userId, conflictTitle, recent24hCutoff);

                    if (!alreadyNotified) {
                        Notification conflictNotice = Notification.builder()
                                .userId(userId)
                                .type("alert")
                                .title(conflictTitle)
                                .message("Phát hiện 2 khung giờ bị trùng nhau lúc " + formatTime(b1.getStartTime()) + " - " + formatTime(b1.getEndTime()) + ". Hãy dời lịch để tránh xung đột!")
                                .isRead(false)
                                .isDeleted(false)
                                .createdAt(now)
                                .updatedAt(now)
                                .build();
                        newlyGenerated.add(conflictNotice);
                    }
                }
            }
        }

        // ── THUẬT TOÁN 5: ĐỀ XUẤT TỐI ƯU HÓA AI (TỐI ĐA 1 LẦN / 24 GIỜ) ──
        Set<Long> scheduledTaskIds = new HashSet<>();
        for (TimeBlock tb : allBlocks) {
            if (tb.getTaskId() != null) {
                scheduledTaskIds.add(tb.getTaskId());
            }
        }

        long unscheduledHighPriority = allTasks.stream()
                .filter(t -> !"COMPLETED".equalsIgnoreCase(t.getStatus()))
                .filter(t -> "URGENT".equalsIgnoreCase(t.getPriority()) || "HIGH".equalsIgnoreCase(t.getPriority()))
                .filter(t -> !scheduledTaskIds.contains(t.getId()))
                .count();

        if (unscheduledHighPriority > 0) {
            boolean alreadyNotifiedAi = notificationRepository.existsByUserIdAndTypeAndCreatedAtAfter(
                    userId, "ai", recent24hCutoff);

            if (!alreadyNotifiedAi) {
                Notification aiNotice = Notification.builder()
                        .userId(userId)
                        .type("ai")
                        .title("💡 Đề xuất tối ưu hóa lịch trình")
                        .message("Phát hiện " + unscheduledHighPriority + " công việc ưu tiên cao chưa được ấn định khung giờ trên lịch. Hãy dùng tính năng Tự động xếp lịch để tối ưu hóa.")
                        .isRead(false)
                        .isDeleted(false)
                        .createdAt(now)
                        .updatedAt(now)
                        .build();
                newlyGenerated.add(aiNotice);
            }
        }

        // ── THUẬT TOÁN 6: TỔNG KẾT NĂNG SUẤT (TỐI ĐA 1 LẦN / 24 GIỜ) ──
        long completedTasks = allTasks.stream().filter(t -> "COMPLETED".equalsIgnoreCase(t.getStatus())).count();
        if (allTasks.size() > 0 && completedTasks >= 1) {
            double completionRate = ((double) completedTasks / allTasks.size()) * 100;
            if (completionRate >= 50.0) {
                boolean alreadyNotifiedSuccess = notificationRepository.existsByUserIdAndTypeAndCreatedAtAfter(
                        userId, "success", recent24hCutoff);

                if (!alreadyNotifiedSuccess) {
                    Notification successNotice = Notification.builder()
                            .userId(userId)
                            .type("success")
                            .title("🎉 Đã hoàn thành " + (int) completionRate + "% khối lượng công việc")
                            .message("Bạn đã hoàn thành " + completedTasks + "/" + allTasks.size() + " công việc. Giữ vững phong độ tập trung nhé!")
                            .isRead(false)
                            .isDeleted(false)
                            .createdAt(now)
                            .updatedAt(now)
                            .build();
                    newlyGenerated.add(successNotice);
                }
            }
        }

        // Lưu các thông báo mới tạo vào CSDL
        if (!newlyGenerated.isEmpty()) {
            notificationRepository.saveAll(newlyGenerated);
            log.info("Notification Decision Engine created {} new notifications for user {}", newlyGenerated.size(), userId);
        }

        // Trả về danh sách thông báo còn hiệu lực (chưa bị xóa) của người dùng
        return notificationRepository.findActiveByUserId(userId);
    }
}
