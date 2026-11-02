package com.taskmanager.repository;

import com.taskmanager.entity.AiSchedulingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiSchedulingLogRepository extends JpaRepository<AiSchedulingLog, Long> {
    List<AiSchedulingLog> findByUserIdOrderByCreatedAtDesc(Long userId);
}
