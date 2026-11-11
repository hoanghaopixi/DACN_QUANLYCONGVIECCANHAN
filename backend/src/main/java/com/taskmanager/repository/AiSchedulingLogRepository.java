package com.taskmanager.repository;

import com.taskmanager.entity.NhatKyAi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiSchedulingLogRepository extends JpaRepository<NhatKyAi, Long> {
    List<NhatKyAi> findByUserIdOrderByCreatedAtDesc(Long userId);
}
