package com.taskmanager.repository;

import com.taskmanager.entity.NhatKyNangSuat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductivityLogRepository extends JpaRepository<NhatKyNangSuat, Long> {
    Optional<NhatKyNangSuat> findByUserIdAndReportDate(Long userId, LocalDate reportDate);
    List<NhatKyNangSuat> findByUserIdAndReportDateBetweenOrderByReportDateAsc(Long userId, LocalDate startDate, LocalDate endDate);
}
