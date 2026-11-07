package com.taskmanager.repository;

import com.taskmanager.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    java.util.Optional<Category> findByIdAndUserId(Long id, Long userId);
    List<Category> findByUserId(Long userId);
    List<Category> findByUserIdAndWorkspaceId(Long userId, Long workspaceId);
    void deleteByWorkspaceIdAndUserId(Long workspaceId, Long userId);
    long countByWorkspaceId(Long workspaceId);
    void deleteByWorkspaceId(Long workspaceId);
}
