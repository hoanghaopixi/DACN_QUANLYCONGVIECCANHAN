package com.taskmanager.controller;

import com.taskmanager.entity.LichSuMedia;
import com.taskmanager.repository.MediaHistoryRepository;
import com.taskmanager.util.UserResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaHistoryRepository mediaHistoryRepository;
    private final UserResolver userResolver;

    @GetMapping("/history")
    public ResponseEntity<List<LichSuMedia>> getMediaHistory(
            @RequestParam(value = "type", required = false) String type,
            HttpServletRequest request) {
        Long userId = userResolver.resolveUserId(request);
        if (type == null || type.isBlank()) {
            return ResponseEntity.ok(mediaHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId));
        }
        if ("AVATAR".equalsIgnoreCase(type.trim())) {
            return ResponseEntity.ok(mediaHistoryRepository.findByUserIdAndMediaTypeOrderByCreatedAtDesc(userId, "AVATAR"));
        }
        if ("BACKGROUND".equalsIgnoreCase(type.trim())) {
            return ResponseEntity.ok(mediaHistoryRepository.findByUserIdAndMediaTypeInOrderByCreatedAtDesc(
                    userId, List.of("BACKGROUND_IMAGE", "BACKGROUND_VIDEO")));
        }
        return ResponseEntity.ok(mediaHistoryRepository.findByUserIdAndMediaTypeOrderByCreatedAtDesc(userId, type.trim().toUpperCase()));
    }

    @DeleteMapping("/history/{id}")
    public ResponseEntity<Map<String, Object>> deleteMediaHistory(
            @PathVariable Long id,
            HttpServletRequest request) {
        Long userId = userResolver.resolveUserId(request);
        return mediaHistoryRepository.findById(id)
                .map(media -> {
                    if (!media.getUserId().equals(userId)) {
                        return ResponseEntity.status(403).body(Map.<String, Object>of(
                                "success", false,
                                "message", "Bạn không có quyền xóa tệp phương tiện này"
                        ));
                    }
                    mediaHistoryRepository.delete(media);
                    return ResponseEntity.ok(Map.<String, Object>of(
                            "success", true,
                            "message", "Đã xóa phương tiện khỏi lịch sử"
                    ));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
