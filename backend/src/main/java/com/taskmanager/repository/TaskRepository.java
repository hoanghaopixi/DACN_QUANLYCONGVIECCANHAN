package com.taskmanager.repository;

import com.taskmanager.entity.CongViec;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<CongViec, Long> {
    java.util.Optional<CongViec> findByIdAndUserId(Long id, Long userId);
    List<CongViec> findByIdInAndUserId(List<Long> ids, Long userId);
    List<CongViec> findByUserId(Long userId);
    List<CongViec> findByUserIdAndWorkspaceId(Long userId, Long workspaceId);
    List<CongViec> findByUserIdAndStatus(Long userId, String status);
    long countByUserId(Long userId);
    long countByUserIdAndWorkspaceId(Long userId, Long workspaceId);
    long countByUserIdAndStatus(Long userId, String status);
    long countByUserIdAndWorkspaceIdAndStatus(Long userId, Long workspaceId, String status);
    long countByCategoryId(Long categoryId);
    boolean existsByCategoryId(Long categoryId);
    long countByWorkspaceId(Long workspaceId);
}
