package com.taskmanager.repository;

import com.taskmanager.entity.NhatKyThoiQuen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HabitLogRepository extends JpaRepository<NhatKyThoiQuen, Long> {
    List<NhatKyThoiQuen> findByUserIdOrderByCheckInDateDesc(Long userId);
    List<NhatKyThoiQuen> findByTaskIdOrderByCheckInDateDesc(Long taskId);
    Optional<NhatKyThoiQuen> findByTaskIdAndCheckInDate(Long taskId, LocalDate checkInDate);
    List<NhatKyThoiQuen> findByUserIdAndTaskIdOrderByCheckInDateDesc(Long userId, Long taskId);
    Optional<NhatKyThoiQuen> findByUserIdAndTaskIdAndCheckInDate(Long userId, Long taskId, LocalDate checkInDate);
}
