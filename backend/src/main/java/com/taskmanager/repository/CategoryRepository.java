package com.taskmanager.repository;

import com.taskmanager.entity.DanhMuc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<DanhMuc, Long> {
    java.util.Optional<DanhMuc> findByIdAndUserId(Long id, Long userId);
    List<DanhMuc> findByUserId(Long userId);
    List<DanhMuc> findByUserIdAndWorkspaceId(Long userId, Long workspaceId);
    void deleteByWorkspaceIdAndUserId(Long workspaceId, Long userId);
    long countByWorkspaceId(Long workspaceId);
    void deleteByWorkspaceId(Long workspaceId);
}
