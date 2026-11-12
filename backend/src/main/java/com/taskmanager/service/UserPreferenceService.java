package com.taskmanager.service;

import com.taskmanager.dto.UserPreferenceDto;
import com.taskmanager.entity.LichSuMedia;
import com.taskmanager.entity.CaiDatNguoiDung;
import com.taskmanager.repository.MediaHistoryRepository;
import com.taskmanager.repository.UserPreferenceRepository;
import com.taskmanager.util.UploadedMediaValidator;
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
    private final com.taskmanager.repository.UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserPreferenceDto getPreference(Long userId) {
        CaiDatNguoiDung pref = userPreferenceRepository.findByUserId(userId)
                .orElseGet(() -> CaiDatNguoiDung.builder()
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
                        .lunchBreakStart(LocalTime.of(12, 0))
                        .lunchBreakEnd(LocalTime.of(13, 0))
                        .allowWeekendTasks(true)
                        .aiAutoFindSlot(true)
                        .aiAlwaysAskBeforeChange(true)
                        .aiAllowSplitLongTasks(true)
                        .aiAutoMoveFixedTasks(false)
                        .reminderMinutesBefore(15)
                        .dndStartTime(LocalTime.of(22, 0))
                        .dndEndTime(LocalTime.of(7, 0))
                        .enableDeviceNotifications(true)
                        .enableMorningSummaryEmail(false)
                        .language("Tiếng Việt")
                        .build());

        return toDto(pref);
    }

    @Transactional
    public UserPreferenceDto updatePreference(Long userId, UserPreferenceDto dto) {
        CaiDatNguoiDung pref = userPreferenceRepository.findByUserId(userId)
                .orElseGet(() -> CaiDatNguoiDung.builder().userId(userId).build());

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
        if (dto.getLunchBreakStart() != null) {
            try {
                pref.setLunchBreakStart(LocalTime.parse(dto.getLunchBreakStart().trim()));
            } catch (Exception ignored) {}
        }
        if (dto.getLunchBreakEnd() != null) {
            try {
                pref.setLunchBreakEnd(LocalTime.parse(dto.getLunchBreakEnd().trim()));
            } catch (Exception ignored) {}
        }
        if (dto.getAllowWeekendTasks() != null) {
            pref.setAllowWeekendTasks(dto.getAllowWeekendTasks());
        }
        if (dto.getAiAutoFindSlot() != null) {
            pref.setAiAutoFindSlot(dto.getAiAutoFindSlot());
        }
        if (dto.getAiAlwaysAskBeforeChange() != null) {
            pref.setAiAlwaysAskBeforeChange(dto.getAiAlwaysAskBeforeChange());
        }
        if (dto.getAiAllowSplitLongTasks() != null) {
            pref.setAiAllowSplitLongTasks(dto.getAiAllowSplitLongTasks());
        }
        if (dto.getAiAutoMoveFixedTasks() != null) {
            pref.setAiAutoMoveFixedTasks(dto.getAiAutoMoveFixedTasks());
        }
        if (dto.getReminderMinutesBefore() != null) {
            pref.setReminderMinutesBefore(dto.getReminderMinutesBefore());
        }
        if (dto.getDndStartTime() != null) {
            try {
                pref.setDndStartTime(LocalTime.parse(dto.getDndStartTime().trim()));
            } catch (Exception ignored) {}
        }
        if (dto.getDndEndTime() != null) {
            try {
                pref.setDndEndTime(LocalTime.parse(dto.getDndEndTime().trim()));
            } catch (Exception ignored) {}
        }
        if (dto.getEnableDeviceNotifications() != null) {
            pref.setEnableDeviceNotifications(dto.getEnableDeviceNotifications());
        }
        if (dto.getEnableMorningSummaryEmail() != null) {
            pref.setEnableMorningSummaryEmail(dto.getEnableMorningSummaryEmail());
        }
        if (dto.getLanguage() != null && !dto.getLanguage().isBlank()) {
            pref.setLanguage(dto.getLanguage().trim());
        }
        pref.setUpdatedAt(OffsetDateTime.now());

        // Đồng bộ họ tên, số điện thoại và ảnh đại diện vào bảng nguoidung nếu có gửi kèm
        if (dto.getFullName() != null || dto.getPhone() != null || dto.getAvatarUrl() != null) {
            userRepository.findById(userId).ifPresent(user -> {
                boolean changed = false;
                if (dto.getFullName() != null && !dto.getFullName().isBlank()) {
                    user.setFullName(dto.getFullName().trim());
                    changed = true;
                }
                if (dto.getPhone() != null) {
                    user.setPhoneNumber(dto.getPhone().trim());
                    changed = true;
                }
                if (dto.getAvatarUrl() != null) {
                    user.setAvatarUrl(dto.getAvatarUrl().trim().isEmpty() ? null : dto.getAvatarUrl().trim());
                    changed = true;
                }
                if (changed) {
                    user.setUpdatedAt(OffsetDateTime.now());
                    userRepository.save(user);
                }
            });
        }

        CaiDatNguoiDung saved = userPreferenceRepository.save(pref);
        return toDto(saved);
    }

    @org.springframework.beans.factory.annotation.Value("${app.upload.dir:uploads}")
    private String uploadDir = "uploads";

    @Transactional
    public java.util.Map<String, Object> uploadBackground(Long userId, org.springframework.web.multipart.MultipartFile file) {
        UploadedMediaValidator.DetectedMedia detectedMedia;
        try {
            detectedMedia = UploadedMediaValidator.detectBackground(file);
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException("Không đọc được tệp hình nền/video tải lên.", e);
        }
        String originalFilename = file.getOriginalFilename();
        String extension = detectedMedia.extension();
        String bgType = detectedMedia.type();

        try {
            java.nio.file.Path bgDir = java.nio.file.Paths.get(uploadDir, "backgrounds").toAbsolutePath().normalize();
            if (!java.nio.file.Files.exists(bgDir)) {
                java.nio.file.Files.createDirectories(bgDir);
            }

            String fileName = java.util.UUID.randomUUID() + extension;
            java.nio.file.Path targetPath = bgDir.resolve(fileName);

            java.nio.file.Files.copy(file.getInputStream(), targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            CaiDatNguoiDung pref = userPreferenceRepository.findByUserId(userId)
                    .orElseGet(() -> CaiDatNguoiDung.builder().userId(userId).build());

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
                mediaHistoryRepository.save(LichSuMedia.builder()
                        .userId(userId)
                        .mediaType("BACKGROUND_" + bgType)
                        .url(publicUrl)
                        .fileName(originalFilename != null ? originalFilename.substring(0, Math.min(originalFilename.length(), 255)) : fileName)
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

    private UserPreferenceDto toDto(CaiDatNguoiDung p) {
        com.taskmanager.entity.NguoiDung user = userRepository.findById(p.getUserId()).orElse(null);
        return UserPreferenceDto.builder()
                .id(p.getId())
                .userId(p.getUserId())
                .fullName(user != null ? user.getFullName() : null)
                .phone(user != null ? user.getPhoneNumber() : null)
                .avatarUrl(user != null ? user.getAvatarUrl() : null)
                .language(p.getLanguage() != null ? p.getLanguage() : "Tiếng Việt")
                .workStartTime(p.getWorkStartTime() != null ? String.format("%02d:%02d", p.getWorkStartTime().getHour(), p.getWorkStartTime().getMinute()) : "08:00")
                .workEndTime(p.getWorkEndTime() != null ? String.format("%02d:%02d", p.getWorkEndTime().getHour(), p.getWorkEndTime().getMinute()) : "18:00")
                .lunchBreakStart(p.getLunchBreakStart() != null ? String.format("%02d:%02d", p.getLunchBreakStart().getHour(), p.getLunchBreakStart().getMinute()) : "12:00")
                .lunchBreakEnd(p.getLunchBreakEnd() != null ? String.format("%02d:%02d", p.getLunchBreakEnd().getHour(), p.getLunchBreakEnd().getMinute()) : "13:00")
                .workDays(p.getWorkDays() != null ? p.getWorkDays() : "MON,TUE,WED,THU,FRI")
                .bufferMinutes(p.getBufferMinutes() != null ? p.getBufferMinutes() : 15)
                .allowWeekendTasks(p.getAllowWeekendTasks() != null ? p.getAllowWeekendTasks() : true)
                .peakProductivityTime(p.getPeakProductivityTime() != null ? p.getPeakProductivityTime() : "MORNING")
                .autoReschedule(p.getAutoReschedule() != null ? p.getAutoReschedule() : true)
                .aiAutoFindSlot(p.getAiAutoFindSlot() != null ? p.getAiAutoFindSlot() : true)
                .aiAlwaysAskBeforeChange(p.getAiAlwaysAskBeforeChange() != null ? p.getAiAlwaysAskBeforeChange() : true)
                .aiAllowSplitLongTasks(p.getAiAllowSplitLongTasks() != null ? p.getAiAllowSplitLongTasks() : true)
                .aiAutoMoveFixedTasks(p.getAiAutoMoveFixedTasks() != null ? p.getAiAutoMoveFixedTasks() : false)
                .timeZone(p.getTimeZone() != null ? p.getTimeZone() : "Asia/Ho_Chi_Minh")
                .theme(p.getTheme() != null ? p.getTheme() : "LIGHT")
                .backgroundUrl(p.getBackgroundUrl())
                .backgroundType(p.getBackgroundType() != null ? p.getBackgroundType() : "NONE")
                .overlayDarkness(p.getOverlayDarkness() != null ? p.getOverlayDarkness() : 50)
                .reminderMinutesBefore(p.getReminderMinutesBefore() != null ? p.getReminderMinutesBefore() : 15)
                .dndStartTime(p.getDndStartTime() != null ? String.format("%02d:%02d", p.getDndStartTime().getHour(), p.getDndStartTime().getMinute()) : "22:00")
                .dndEndTime(p.getDndEndTime() != null ? String.format("%02d:%02d", p.getDndEndTime().getHour(), p.getDndEndTime().getMinute()) : "07:00")
                .enableDeviceNotifications(p.getEnableDeviceNotifications() != null ? p.getEnableDeviceNotifications() : true)
                .enableMorningSummaryEmail(p.getEnableMorningSummaryEmail() != null ? p.getEnableMorningSummaryEmail() : false)
                .build();
    }
}
