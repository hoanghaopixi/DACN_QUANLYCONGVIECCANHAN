package com.taskmanager.controller;

import com.taskmanager.dto.AuthDto;
import com.taskmanager.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final com.taskmanager.util.UserResolver userResolver;

    @PostMapping("/login")
    public ResponseEntity<AuthDto.AuthResponse> login(@RequestBody @Valid AuthDto.LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthDto.AuthResponse> register(@RequestBody @Valid AuthDto.RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestBody @Valid AuthDto.ChangePasswordRequest request,
            jakarta.servlet.http.HttpServletRequest servletRequest) {
        Long userId = userResolver.resolveUserId(servletRequest);
        authService.changePassword(userId, request);
        return ResponseEntity.ok(java.util.Map.of("success", true, "message", "Đổi mật khẩu thành công"));
    }

    @PostMapping("/verify-password")
    public ResponseEntity<?> verifyPassword(
            @RequestBody java.util.Map<String, String> body,
            jakarta.servlet.http.HttpServletRequest servletRequest) {
        Long userId = userResolver.resolveUserId(servletRequest);
        String password = body != null ? body.get("password") : null;
        authService.verifyPassword(userId, password);
        return ResponseEntity.ok(java.util.Map.of("success", true, "message", "Xác thực mật khẩu thành công"));
    }

    @PutMapping("/profile")
    public ResponseEntity<AuthDto.AuthResponse> updateProfile(
            @RequestBody AuthDto.UpdateProfileRequest request,
            jakarta.servlet.http.HttpServletRequest servletRequest) {
        Long userId = userResolver.resolveUserId(servletRequest);
        return ResponseEntity.ok(authService.updateProfile(userId, request));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthDto.AuthResponse> getProfile(
            jakarta.servlet.http.HttpServletRequest servletRequest) {
        Long userId = userResolver.resolveUserId(servletRequest);
        return ResponseEntity.ok(authService.getProfile(userId));
    }

    @PostMapping(value = {"/upload-avatar", "/avatar"}, consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AuthDto.AvatarUploadResponse> uploadAvatar(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            jakarta.servlet.http.HttpServletRequest servletRequest) {
        Long userId = userResolver.resolveUserId(servletRequest);
        return ResponseEntity.ok(authService.uploadAvatar(userId, file));
    }
}
