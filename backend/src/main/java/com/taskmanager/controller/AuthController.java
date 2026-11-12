package com.taskmanager.controller;

import com.taskmanager.dto.AuthDto;
import com.taskmanager.service.AuthService;
import com.taskmanager.util.UserResolver;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserResolver userResolver;

    @PostMapping("/login")
    public ResponseEntity<AuthDto.AuthResponse> login(@RequestBody @Valid AuthDto.LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register/send-otp")
    public ResponseEntity<?> sendRegisterOtp(@RequestBody @Valid AuthDto.SendRegisterOtpRequest request) {
        return ResponseEntity.ok(authService.sendRegisterOtp(request));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthDto.AuthResponse> register(@RequestBody @Valid AuthDto.RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/google")
    public ResponseEntity<AuthDto.AuthResponse> loginWithGoogle(@RequestBody AuthDto.GoogleLoginRequest request) {
        return ResponseEntity.ok(authService.loginWithGoogle(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody @Valid AuthDto.ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody @Valid AuthDto.VerifyOtpRequest request) {
        return ResponseEntity.ok(authService.verifyOtp(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody @Valid AuthDto.ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestBody @Valid AuthDto.ChangePasswordRequest request,
            jakarta.servlet.http.HttpServletRequest servletRequest) {
        Long userId = userResolver.resolveUserId(servletRequest);
        authService.changePassword(userId, request);
        return ResponseEntity.ok(java.util.Map.of("success", true, "message", "Đổi mật khẩu thành công"));
    }

    @PostMapping("/set-password")
    public ResponseEntity<AuthDto.AuthResponse> setInitialPassword(
            @RequestBody @Valid AuthDto.SetPasswordRequest request,
            jakarta.servlet.http.HttpServletRequest servletRequest) {
        Long userId = userResolver.resolveUserId(servletRequest);
        return ResponseEntity.ok(authService.setInitialPassword(userId, request));
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
