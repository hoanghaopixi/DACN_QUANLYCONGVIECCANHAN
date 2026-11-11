package com.taskmanager.repository;

import com.taskmanager.entity.LichSuMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MediaHistoryRepository extends JpaRepository<LichSuMedia, Long> {
    List<LichSuMedia> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<LichSuMedia> findByUserIdAndMediaTypeOrderByCreatedAtDesc(Long userId, String mediaType);
    List<LichSuMedia> findByUserIdAndMediaTypeInOrderByCreatedAtDesc(Long userId, List<String> mediaTypes);
}
