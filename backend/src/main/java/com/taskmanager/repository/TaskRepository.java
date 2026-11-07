package com.taskmanager.repository;

import com.taskmanager.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    java.util.Optional<Task> findByIdAndUserId(Long id, Long userId);
    List<Task> findByIdInAndUserId(List<Long> ids, Long userId);
    List<Task> findByUserId(Long userId);
    List<Task> findByUserIdAndWorkspaceId(Long userId, Long workspaceId);
    List<Task> findByUserIdAndStatus(Long userId, String status);
    long countByUserId(Long userId);
    long countByUserIdAndWorkspaceId(Long userId, Long workspaceId);
    long countByUserIdAndStatus(Long userId, String status);
    long countByUserIdAndWorkspaceIdAndStatus(Long userId, Long workspaceId, String status);
    long countByCategoryId(Long categoryId);
    boolean existsByCategoryId(Long categoryId);
    long countByWorkspaceId(Long workspaceId);
}
