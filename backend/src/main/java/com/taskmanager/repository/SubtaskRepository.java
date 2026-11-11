package com.taskmanager.repository;

import com.taskmanager.entity.CongViecCon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubtaskRepository extends JpaRepository<CongViecCon, Long> {
    List<CongViecCon> findByTaskIdOrderByOrderIndexAsc(Long taskId);
    List<CongViecCon> findByTaskIdInOrderByOrderIndexAsc(List<Long> taskIds);
    void deleteByTaskId(Long taskId);
}
