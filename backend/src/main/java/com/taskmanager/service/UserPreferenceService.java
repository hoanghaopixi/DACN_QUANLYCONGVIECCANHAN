package com.taskmanager.service;

import com.taskmanager.dto.UserPreferenceDto;
import com.taskmanager.entity.MediaHistory;
import com.taskmanager.entity.UserPreference;
import com.taskmanager.repository.MediaHistoryRepository;
import com.taskmanager.repository.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserPreferenceService {

    private final UserPreferenceRepository userPreferenceRepository;
    private final MediaHistoryRepository mediaHistoryRepository;

    @Transactional(readOnly = true)
    public UserPreferenceDto getPreference(Long userId) {
        UserPreference pref = userPreferenceRepository.findByUserId(userId)
                .orElseGet(() -> UserPreference.builder()
                        .userId(userId)
                        .workStartTime(LocalTime.of(8, 0))
                        .workEndTime(LocalTime.of(18, 0))
                        .workDays("MON,TUE,WED,THU,FRI")
                        .bufferMinutes(15)
                        .peakProductivityTime("MORNING")
                        .autoReschedule(true)
                        .timeZone("Asia/Ho_Chi_Minh")
                        .theme("LIGHT")
                        .backgroundUrl(null)
                        .backgroundType("NONE")
                        .overlayDarkness(50)
                        .build());

        return toDto(pref);
    }

    @Transactional
    public UserPreferenceDto updatePreference(Long userId, UserPreferenceDto dto) {
        UserPreference pref = userPreferenceRepository.findByUserId(userId)
                .orElseGet(() -> UserPreference.builder().userId(userId).build());

        if (dto.getWorkStartTime() != null) {
            try {
                pref.setWorkStartTime(LocalTime.parse(dto.getWorkStartTime().trim()));
            } catch (Exception ignored) {}
        }
        if (dto.getWorkEndTime() != null) {
            try {
                pref.setWorkEndTime(LocalTime.parse(dto.getWorkEndTime().trim()));
            } catch (Exception ignored) {}
        }
        if (dto.getWorkDays() != null) {
            pref.setWorkDays(dto.getWorkDays());
        }
        if (dto.getBufferMinutes() != null) {
            pref.setBufferMinutes(dto.getBufferMinutes());
        }
        if (dto.getPeakProductivityTime() != null) {
            pref.setPeakProductivityTime(dto.getPeakProductivityTime());
        }
        if (dto.getAutoReschedule() != null) {
            pref.setAutoReschedule(dto.getAutoReschedule());
        }
        if (dto.getTimeZone() != null) {
            pref.setTimeZone(dto.getTimeZone());
        }
        if (dto.getTheme() != null) {
            pref.setTheme(dto.getTheme());
        }
        if (dto.getBackgroundUrl() != null) {
            String trimmed = dto.getBackgroundUrl().trim();
            pref.setBackgroundUrl(trimmed.isEmpty() ? null : trimmed);
        }
        if (dto.getBackgroundType() != null) {
            pref.setBackgroundType(dto.getBackgroundType().trim());
        }
        if (dto.getOverlayDarkness() != null) {
            pref.setOverlayDarkness(Math.max(0, Math.min(100, dto.getOverlayDarkness())));
        }
        pref.setUpdatedAt(OffsetDateTime.now());

        UserPreference saved = userPreferenceRepository.save(pref);
        return toDto(saved);
    }

    @org.springframework.beans.factory.annotation.Value("${app.upload.dir:uploads}")
    private String uploadDir = "uploads";

    @Transactional
    public java.util.Map<String, Object> uploadBackground(Long userId, org.springframework.web.multipart.MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Tệp tải lên không được để trống");
        }

        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }

        String bgType = "IMAGE";
        boolean isVideo = (contentType != null && contentType.startsWith("video/"))
                || extension.matches("^\\.(mp4|webm|ogg|mov)$");
        boolean isImage = (contentType != null && contentType.startsWith("image/"))
                || extension.matches("^\\.(jpe?g|png|webp|gif|svg)$");

        if (isVideo) {
            bgType = "VIDEO";
            if (!extension.matches("^\\.(mp4|webm|ogg|mov)$")) {
                extension = ".mp4";
            }
        } else if (isImage) {
            bgType = "IMAGE";
            if (!extension.matches("^\\.(jpe?g|png|webp|gif|svg)$")) {
                extension = ".jpg";
            }
        } else {
            throw new RuntimeException("Chỉ chấp nhận tệp định dạng hình ảnh (JPG, PNG, WebP, GIF) hoặc video (MP4, WebM)");
        }

        try {
            java.nio.file.Path bgDir = java.nio.file.Paths.get(uploadDir, "backgrounds").toAbsolutePath().normalize();
            if (!java.nio.file.Files.exists(bgDir)) {
                java.nio.file.Files.createDirectories(bgDir);
            }

            String fileName = "bg_user_" + userId + "_" + System.currentTimeMillis() + extension;
            java.nio.file.Path targetPath = bgDir.resolve(fileName);

            java.nio.file.Files.copy(file.getInputStream(), targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            UserPreference pref = userPreferenceRepository.findByUserId(userId)
                    .orElseGet(() -> UserPreference.builder().userId(userId).build());

            String publicUrl = "/api/uploads/backgrounds/" + fileName;
            pref.setBackgroundUrl(publicUrl);
            pref.setBackgroundType(bgType);
            if (pref.getOverlayDarkness() == null) {
                pref.setOverlayDarkness(50);
            }
            pref.setUpdatedAt(OffsetDateTime.now());
            userPreferenceRepository.save(pref);

            // Lưu lịch sử hình nền/video vào CSDL để qua máy khác vẫn đồng bộ
            try {
                mediaHistoryRepository.save(MediaHistory.builder()
                        .userId(userId)
                        .mediaType("BACKGROUND_" + bgType)
                        .url(publicUrl)
                        .fileName(originalFilename != null ? originalFilename : fileName)
                        .fileSize(file.getSize())
                        .createdAt(OffsetDateTime.now())
                        .build());
            } catch (Exception historyEx) {
                // Log and continue
            }

            return java.util.Map.of(
                    "success", true,
                    "message", "Tải hình nền cá nhân hóa thành công",
                    "backgroundUrl", publicUrl,
                    "backgroundType", bgType,
                    "overlayDarkness", pref.getOverlayDarkness()
            );
        } catch (java.io.IOException e) {
            throw new RuntimeException("Lỗi lưu trữ tệp hình nền trên máy chủ: " + e.getMessage(), e);
        }
    }

    private UserPreferenceDto toDto(UserPreference p) {
        return UserPreferenceDto.builder()
                .id(p.getId())
                .userId(p.getUserId())
                .workStartTime(p.getWorkStartTime() != null ? String.format("%02d:%02d", p.getWorkStartTime().getHour(), p.getWorkStartTime().getMinute()) : "08:00")
                .workEndTime(p.getWorkEndTime() != null ? String.format("%02d:%02d", p.getWorkEndTime().getHour(), p.getWorkEndTime().getMinute()) : "18:00")
                .workDays(p.getWorkDays() != null ? p.getWorkDays() : "MON,TUE,WED,THU,FRI")
                .bufferMinutes(p.getBufferMinutes() != null ? p.getBufferMinutes() : 15)
                .peakProductivityTime(p.getPeakProductivityTime() != null ? p.getPeakProductivityTime() : "MORNING")
                .autoReschedule(p.getAutoReschedule() != null ? p.getAutoReschedule() : true)
                .timeZone(p.getTimeZone() != null ? p.getTimeZone() : "Asia/Ho_Chi_Minh")
                .theme(p.getTheme() != null ? p.getTheme() : "LIGHT")
                .backgroundUrl(p.getBackgroundUrl())
                .backgroundType(p.getBackgroundType() != null ? p.getBackgroundType() : "NONE")
                .overlayDarkness(p.getOverlayDarkness() != null ? p.getOverlayDarkness() : 50)
                .build();
    }
}
