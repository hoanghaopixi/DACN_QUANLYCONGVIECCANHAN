package com.taskmanager.repository;

import com.taskmanager.entity.ThongBao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<ThongBao, Long> {

    @Query("SELECT n FROM ThongBao n WHERE n.userId = :userId AND (n.isDeleted IS NULL OR n.isDeleted = false) ORDER BY n.createdAt DESC")
    List<ThongBao> findActiveByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(n) FROM ThongBao n WHERE n.userId = :userId AND n.isRead = false AND (n.isDeleted IS NULL OR n.isDeleted = false)")
    long countUnreadActiveByUserId(@Param("userId") Long userId);

    @Query("SELECT n FROM ThongBao n WHERE n.id = :id AND n.userId = :userId AND (n.isDeleted IS NULL OR n.isDeleted = false)")
    Optional<ThongBao> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ThongBao n SET n.isRead = true, n.updatedAt = CURRENT_TIMESTAMP WHERE n.userId = :userId AND n.isRead = false AND (n.isDeleted IS NULL OR n.isDeleted = false)")
    void markAllAsReadByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ThongBao n SET n.isDeleted = true, n.isRead = true, n.updatedAt = CURRENT_TIMESTAMP WHERE n.userId = :userId AND (n.isDeleted IS NULL OR n.isDeleted = false)")
    void clearAllByUserId(@Param("userId") Long userId);

    // Kiểm tra trùng lặp theo task: nếu đã từng tạo thông báo loại này cho task (kể cả đã đọc hoặc đã xóa) thì không tạo lại
    boolean existsByUserIdAndTaskIdAndType(Long userId, Long taskId, String type);

    boolean existsByUserIdAndTaskIdAndTypeAndCreatedAtAfter(Long userId, Long taskId, String type, OffsetDateTime after);

    boolean existsByUserIdAndTitleAndCreatedAtAfter(Long userId, String title, OffsetDateTime after);

    boolean existsByUserIdAndTypeAndCreatedAtAfter(Long userId, String type, OffsetDateTime after);
}
