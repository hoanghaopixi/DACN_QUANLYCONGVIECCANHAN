package com.taskmanager.repository;

import com.taskmanager.entity.MediaHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MediaHistoryRepository extends JpaRepository<MediaHistory, Long> {
    List<MediaHistory> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<MediaHistory> findByUserIdAndMediaTypeOrderByCreatedAtDesc(Long userId, String mediaType);
    List<MediaHistory> findByUserIdAndMediaTypeInOrderByCreatedAtDesc(Long userId, List<String> mediaTypes);
}
