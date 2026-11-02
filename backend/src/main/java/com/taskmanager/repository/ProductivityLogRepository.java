package com.taskmanager.repository;

import com.taskmanager.entity.ProductivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductivityLogRepository extends JpaRepository<ProductivityLog, Long> {
    Optional<ProductivityLog> findByUserIdAndReportDate(Long userId, LocalDate reportDate);
    List<ProductivityLog> findByUserIdAndReportDateBetweenOrderByReportDateAsc(Long userId, LocalDate startDate, LocalDate endDate);
}
