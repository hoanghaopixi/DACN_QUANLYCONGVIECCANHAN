package com.taskmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AuthDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank(message = "Vui lòng nhập địa chỉ email")
        @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "Định dạng email không hợp lệ (Ví dụ: name@example.com)")
        private String email;

        @NotBlank(message = "Vui lòng nhập mật khẩu")
        @Size(max = 50, message = "Mật khẩu không được vượt quá 50 ký tự")
        private String password;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterRequest {
        @NotBlank(message = "Vui lòng nhập họ và tên của bạn")
        @Size(min = 2, max = 100, message = "Họ và tên phải có từ 2 đến 100 ký tự")
        private String fullName;

        @NotBlank(message = "Vui lòng nhập địa chỉ email")
        @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "Định dạng email không hợp lệ (Ví dụ: name@example.com)")
        private String email;

        @NotBlank(message = "Vui lòng nhập mật khẩu")
        @Size(min = 8, max = 50, message = "Mật khẩu phải có từ 8 đến 50 ký tự")
        private String password;

        private String otp;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendRegisterOtpRequest {
        @NotBlank(message = "Vui lòng nhập họ và tên của bạn")
        @Size(min = 2, max = 100, message = "Họ và tên phải có từ 2 đến 100 ký tự")
        private String fullName;

        @NotBlank(message = "Vui lòng nhập địa chỉ email")
        @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "Định dạng email không hợp lệ (Ví dụ: name@example.com)")
        private String email;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChangePasswordRequest {
        @NotBlank(message = "Vui lòng nhập mật khẩu hiện tại")
        private String currentPassword;

        @NotBlank(message = "Vui lòng nhập mật khẩu mới")
        @Size(min = 8, max = 50, message = "Mật khẩu mới phải có từ 8 đến 50 ký tự")
        private String newPassword;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SetPasswordRequest {
        @NotBlank(message = "Vui lòng nhập tên người dùng / họ tên")
        @Size(min = 2, max = 100, message = "Họ và tên phải có từ 2 đến 100 ký tự")
        private String username;

        @NotBlank(message = "Vui lòng nhập mật khẩu mới")
        @Size(min = 8, max = 50, message = "Mật khẩu mới phải có từ 8 đến 50 ký tự")
        private String newPassword;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateProfileRequest {
        private String fullName;
        private String phone;
        private String avatarUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthResponse {
        private String token;
        private String email;
        private String fullName;
        private String role;
        private String avatarUrl;
        private String phone;
        private String authProvider;
        private Boolean hasPassword;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AvatarUploadResponse {
        private boolean success;
        private String message;
        private String avatarUrl;
        private String fileName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ForgotPasswordRequest {
        @NotBlank(message = "Vui lòng nhập địa chỉ email")
        @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "Định dạng email không hợp lệ")
        private String email;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerifyOtpRequest {
        @NotBlank(message = "Vui lòng nhập địa chỉ email")
        private String email;

        @NotBlank(message = "Vui lòng nhập mã OTP")
        @Size(min = 6, max = 6, message = "Mã OTP phải có đúng 6 chữ số")
        private String otp;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResetPasswordRequest {
        @NotBlank(message = "Vui lòng nhập địa chỉ email")
        private String email;

        @NotBlank(message = "Vui lòng nhập mã OTP")
        @Size(min = 6, max = 6, message = "Mã OTP phải có đúng 6 chữ số")
        private String otp;

        @NotBlank(message = "Vui lòng nhập mật khẩu mới")
        @Size(min = 8, max = 50, message = "Mật khẩu mới phải có từ 8 đến 50 ký tự")
        private String newPassword;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GoogleLoginRequest {
        private String idToken;
        private String email;
        private String fullName;
        private String avatarUrl;
    }
}
