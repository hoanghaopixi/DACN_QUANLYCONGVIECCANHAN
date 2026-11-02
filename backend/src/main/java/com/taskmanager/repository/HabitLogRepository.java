package com.taskmanager.repository;

import com.taskmanager.entity.HabitLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HabitLogRepository extends JpaRepository<HabitLog, Long> {
    List<HabitLog> findByUserIdOrderByCheckInDateDesc(Long userId);
    List<HabitLog> findByTaskIdOrderByCheckInDateDesc(Long taskId);
    Optional<HabitLog> findByTaskIdAndCheckInDate(Long taskId, LocalDate checkInDate);
}
