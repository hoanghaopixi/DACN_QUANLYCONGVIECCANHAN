package com.taskmanager.repository;

import com.taskmanager.entity.Subtask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubtaskRepository extends JpaRepository<Subtask, Long> {
    List<Subtask> findByTaskIdOrderByOrderIndexAsc(Long taskId);
    void deleteByTaskId(Long taskId);
}
