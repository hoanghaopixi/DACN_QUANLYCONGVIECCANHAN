package com.taskmanager.repository;

import com.taskmanager.entity.KhongGianLamViec;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkspaceRepository extends JpaRepository<KhongGianLamViec, Long> {
    java.util.Optional<KhongGianLamViec> findByIdAndUserId(Long id, Long userId);
    java.util.Optional<KhongGianLamViec> findFirstByUserIdOrderByIdAsc(Long userId);
    boolean existsByIdAndUserId(Long id, Long userId);
    List<KhongGianLamViec> findByUserIdOrderByIdAsc(Long userId);
    boolean existsByUserIdAndName(Long userId, String name);
    long countByUserId(Long userId);
}
