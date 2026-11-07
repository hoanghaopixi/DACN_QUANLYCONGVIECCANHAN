package com.taskmanager.repository;

import com.taskmanager.entity.TimeBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface TimeBlockRepository extends JpaRepository<TimeBlock, Long> {
    List<TimeBlock> findByUserId(Long userId);
    List<TimeBlock> findByUserIdAndStartTimeBetween(Long userId, OffsetDateTime start, OffsetDateTime end);
    List<TimeBlock> findByUserIdAndStartTimeBeforeAndEndTimeAfter(
            Long userId, OffsetDateTime dayEnd, OffsetDateTime dayStart);

    @Query("select distinct b.taskId from TimeBlock b where b.userId = :userId and b.taskId is not null " +
            "and (b.status is null or upper(b.status) not in ('CANCELLED', 'CANCELED', 'DELETED'))")
    List<Long> findScheduledTaskIdsByUserId(@Param("userId") Long userId);

    void deleteByTaskId(Long taskId);
}
