package com.taskmanager.repository;

import com.taskmanager.entity.KhoiThoiGian;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface TimeBlockRepository extends JpaRepository<KhoiThoiGian, Long> {
    List<KhoiThoiGian> findByUserId(Long userId);
    List<KhoiThoiGian> findByUserIdAndStartTimeBetween(Long userId, OffsetDateTime start, OffsetDateTime end);
    List<KhoiThoiGian> findByUserIdAndStartTimeBeforeAndEndTimeAfter(
            Long userId, OffsetDateTime dayEnd, OffsetDateTime dayStart);

    @Query("select distinct b.taskId from KhoiThoiGian b where b.userId = :userId and b.taskId is not null " +
            "and (b.status is null or upper(b.status) not in ('CANCELLED', 'CANCELED', 'DELETED'))")
    List<Long> findScheduledTaskIdsByUserId(@Param("userId") Long userId);

    void deleteByTaskId(Long taskId);
}
