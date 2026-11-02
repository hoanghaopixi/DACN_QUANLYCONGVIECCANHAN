package com.taskmanager.controller;

import com.taskmanager.dto.UserPreferenceDto;
import com.taskmanager.service.UserPreferenceService;
import com.taskmanager.util.UserResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/preferences")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class UserPreferenceController {

    private final UserPreferenceService userPreferenceService;
    private final UserResolver userResolver;

    @GetMapping
    public ResponseEntity<UserPreferenceDto> getPreferences(HttpServletRequest request) {
        Long userId = userResolver.resolveUserId(request);
        return ResponseEntity.ok(userPreferenceService.getPreference(userId));
    }

    @PutMapping
    public ResponseEntity<UserPreferenceDto> updatePreferences(
            @RequestBody UserPreferenceDto dto,
            HttpServletRequest request) {
        Long userId = userResolver.resolveUserId(request);
        log.info("Cập nhật cài đặt sở thích thời gian làm việc cho user {}", userId);
        return ResponseEntity.ok(userPreferenceService.updatePreference(userId, dto));
    }

    @PostMapping(value = "/upload-background", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<java.util.Map<String, Object>> uploadBackground(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            HttpServletRequest request) {
        Long userId = userResolver.resolveUserId(request);
        log.info("Tải hình nền/video cá nhân hóa cho user {}", userId);
        return ResponseEntity.ok(userPreferenceService.uploadBackground(userId, file));
    }
}
