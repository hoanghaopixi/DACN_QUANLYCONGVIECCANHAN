package com.taskmanager.controller;

import com.taskmanager.entity.Notification;
import com.taskmanager.repository.NotificationRepository;
import com.taskmanager.service.NotificationDecisionService;
import com.taskmanager.util.UserResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final NotificationDecisionService notificationDecisionService;
    private final UserResolver userResolver;

    /**
     * Lấy danh sách toàn bộ thông báo còn hiệu lực (chưa bị xóa) của người dùng hiện tại
     * Thao tác GET là idempotent, chỉ đọc dữ liệu từ CSDL, không gây side-effect tạo lại thông báo
     */
    @GetMapping
    public ResponseEntity<List<Notification>> getNotifications(HttpServletRequest request) {
        Long userId = userResolver.resolveUserId(request);
        List<Notification> list = notificationRepository.findActiveByUserId(userId);
        return ResponseEntity.ok(list);
    }

    /**
     * Kích hoạt chạy thuật toán quyết định thông báo chủ động
     */
    @PostMapping("/evaluate")
    public ResponseEntity<List<Notification>> evaluateNotifications(HttpServletRequest request) {
        Long userId = userResolver.resolveUserId(request);
        List<Notification> list = notificationDecisionService.evaluateAndGenerateNotifications(userId);
        return ResponseEntity.ok(list);
    }

    /**
     * Lấy số lượng thông báo chưa đọc từ CSDL (Chỉ đếm các thông báo chưa đọc và chưa bị xóa)
     * Thao tác đọc an toàn cho chu kỳ polling của giao diện
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount(HttpServletRequest request) {
        Long userId = userResolver.resolveUserId(request);
        long count = notificationRepository.countUnreadActiveByUserId(userId);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    /**
     * Cập nhật trạng thái đã đọc cho một thông báo cụ thể
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id, HttpServletRequest request) {
        Long userId = userResolver.resolveUserId(request);
        return notificationRepository.findByIdAndUserId(id, userId)
                .map(n -> {
                    n.setIsRead(true);
                    n.setUpdatedAt(OffsetDateTime.now());
                    Notification updated = notificationRepository.save(n);
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Đánh dấu toàn bộ thông báo chưa đọc của người dùng là đã đọc trong CSDL
     */
    @PutMapping("/read-all")
    @Transactional
    public ResponseEntity<?> markAllAsRead(HttpServletRequest request) {
        Long userId = userResolver.resolveUserId(request);
        notificationRepository.markAllAsReadByUserId(userId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Đã đánh dấu tất cả thông báo là đã đọc"));
    }

    /**
     * Xóa một thông báo (Soft-delete để lưu vết khử trùng lặp, không bị đổ lại khi engine chạy)
     */
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteNotification(@PathVariable Long id, HttpServletRequest request) {
        Long userId = userResolver.resolveUserId(request);
        return notificationRepository.findByIdAndUserId(id, userId)
                .map(n -> {
                    n.setIsDeleted(true);
                    n.setIsRead(true);
                    n.setUpdatedAt(OffsetDateTime.now());
                    notificationRepository.save(n);
                    return ResponseEntity.ok(Map.of("success", true, "message", "Đã xóa thông báo thành công"));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Xóa toàn bộ thông báo của người dùng (Đánh dấu đã xóa toàn bộ)
     */
    @DeleteMapping("/clear-all")
    @Transactional
    public ResponseEntity<?> clearAllNotifications(HttpServletRequest request) {
        Long userId = userResolver.resolveUserId(request);
        notificationRepository.clearAllByUserId(userId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Đã xóa toàn bộ thông báo thành công"));
    }

    /**
     * Tạo thông báo mới vào CSDL
     */
    @PostMapping
    public ResponseEntity<Notification> createNotification(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = userResolver.resolveUserId(request);
        String title = (String) body.getOrDefault("title", "Thông báo mới");
        String message = (String) body.getOrDefault("message", "");
        String type = (String) body.getOrDefault("type", "reminder");
        Long taskId = body.get("taskId") != null ? Long.valueOf(body.get("taskId").toString()) : null;

        Notification notification = Notification.builder()
                .userId(userId)
                .taskId(taskId)
                .title(title)
                .message(message)
                .type(type)
                .isRead(false)
                .isDeleted(false)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        Notification saved = notificationRepository.save(notification);
        return ResponseEntity.ok(saved);
    }
}
