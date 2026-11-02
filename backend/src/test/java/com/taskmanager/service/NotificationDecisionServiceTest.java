package com.taskmanager.service;

import com.taskmanager.entity.Notification;
import com.taskmanager.entity.Task;
import com.taskmanager.entity.TimeBlock;
import com.taskmanager.repository.NotificationRepository;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.repository.TimeBlockRepository;
import com.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationDecisionServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TimeBlockRepository timeBlockRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationDecisionService decisionService;

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @BeforeEach
    void setUp() {
    }

    @Test
    @DisplayName("Không tạo lại thông báo quá hạn nếu đã từng thông báo cho task đó (kể cả đã xóa hoặc đã đọc)")
    void evaluate_DoesNotRecreateIfAlreadyNotifiedForTask() {
        Long userId = 1L;
        Task overdueTask = Task.builder()
                .id(100L)
                .title("Nộp báo cáo đồ án")
                .status("IN_PROGRESS")
                .dueDate(OffsetDateTime.now(VN_ZONE).minusDays(1))
                .build();

        when(taskRepository.findByUserId(userId)).thenReturn(List.of(overdueTask));
        when(timeBlockRepository.findByUserId(userId)).thenReturn(Collections.emptyList());
        // Giả lập thông báo đã từng tồn tại trong CSDL (kể cả đã bị xóa hoặc đọc)
        when(notificationRepository.existsByUserIdAndTaskIdAndType(userId, 100L, "alert")).thenReturn(true);
        when(notificationRepository.findActiveByUserId(userId)).thenReturn(Collections.emptyList());

        List<Notification> result = decisionService.evaluateAndGenerateNotifications(userId);

        // Không được gọi saveAll để sinh thông báo mới
        verify(notificationRepository, never()).saveAll(anyList());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Tạo thông báo mới khi có task quá hạn chưa từng được thông báo")
    void evaluate_CreatesNotificationForNewOverdueTask() {
        Long userId = 1L;
        Task overdueTask = Task.builder()
                .id(200L)
                .title("Làm bài tập lớn")
                .status("TODO")
                .dueDate(OffsetDateTime.now(VN_ZONE).minusHours(2))
                .build();

        when(taskRepository.findByUserId(userId)).thenReturn(List.of(overdueTask));
        when(timeBlockRepository.findByUserId(userId)).thenReturn(Collections.emptyList());
        when(notificationRepository.existsByUserIdAndTaskIdAndType(userId, 200L, "alert")).thenReturn(false);
        when(notificationRepository.findActiveByUserId(userId)).thenReturn(List.of(
                Notification.builder().id(1L).title("⚠️ Quá hạn deadline: Làm bài tập lớn").build()
        ));

        List<Notification> result = decisionService.evaluateAndGenerateNotifications(userId);

        verify(notificationRepository, times(1)).saveAll(anyList());
        assertEquals(1, result.size());
    }
}

