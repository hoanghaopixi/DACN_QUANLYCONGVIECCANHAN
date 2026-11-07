package com.taskmanager.repository;

import com.taskmanager.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {
    java.util.Optional<Workspace> findByIdAndUserId(Long id, Long userId);
    java.util.Optional<Workspace> findFirstByUserIdOrderByIdAsc(Long userId);
    boolean existsByIdAndUserId(Long id, Long userId);
    List<Workspace> findByUserIdOrderByIdAsc(Long userId);
    boolean existsByUserIdAndName(Long userId, String name);
    long countByUserId(Long userId);
}
