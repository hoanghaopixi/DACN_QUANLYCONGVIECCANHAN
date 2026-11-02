package com.taskmanager.service;

import com.taskmanager.dto.AuthDto;
import com.taskmanager.entity.MediaHistory;
import com.taskmanager.entity.Role;
import com.taskmanager.entity.User;
import com.taskmanager.exception.AccountLockedException;
import com.taskmanager.exception.ResourceNotFoundException;
import com.taskmanager.repository.MediaHistoryRepository;
import com.taskmanager.repository.RoleRepository;
import com.taskmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final MediaHistoryRepository mediaHistoryRepository;

    public AuthDto.AuthResponse login(AuthDto.LoginRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Dữ liệu đăng nhập không được để trống");
        }

        String rawEmail = request.getEmail();
        if (rawEmail == null || rawEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập địa chỉ email");
        }

        String trimmedEmail = rawEmail.trim();
        if (!EMAIL_PATTERN.matcher(trimmedEmail).matches()) {
            throw new IllegalArgumentException("Định dạng email không hợp lệ (Ví dụ: name@example.com)");
        }

        String rawPassword = request.getPassword();
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập mật khẩu");
        }

        // Tìm kiếm người dùng theo email (hỗ trợ phân biệt hoa thường và không phân biệt)
        User user = userRepository.findByEmailIgnoreCase(trimmedEmail)
                .orElseGet(() -> userRepository.findByEmail(trimmedEmail)
                        .orElseThrow(() -> new ResourceNotFoundException("Tài khoản email này chưa được đăng ký trong hệ thống")));

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new AccountLockedException("Tài khoản của bạn đã bị khóa hoặc chưa kích hoạt. Vui lòng liên hệ quản trị viên.");
        }

        // Kiểm tra mật khẩu mã hóa BCrypt
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("Mật khẩu không chính xác. Vui lòng kiểm tra lại!");
        }

        String primaryRole = user.getRoles().stream()
                .anyMatch(r -> "ROLE_ADMIN".equals(r.getName())) ? "Quản trị viên" : "Người dùng";

        String mockJwt = "jwt_token_" + user.getId() + "_" + System.currentTimeMillis();

        return AuthDto.AuthResponse.builder()
                .token(mockJwt)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(primaryRole)
                .avatarUrl(user.getAvatarUrl())
                .phone(user.getPhoneNumber())
                .build();
    }

    public AuthDto.AuthResponse register(AuthDto.RegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Dữ liệu đăng ký không được để trống");
        }

        String rawFullName = request.getFullName();
        if (rawFullName == null || rawFullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập họ và tên của bạn");
        }
        if (rawFullName.trim().length() < 2) {
            throw new IllegalArgumentException("Họ và tên phải có từ 2 đến 100 ký tự");
        }

        String rawEmail = request.getEmail();
        if (rawEmail == null || rawEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập địa chỉ email");
        }
        String trimmedEmail = rawEmail.trim();
        if (!EMAIL_PATTERN.matcher(trimmedEmail).matches()) {
            throw new IllegalArgumentException("Định dạng email không hợp lệ (Ví dụ: name@example.com)");
        }

        String rawPassword = request.getPassword();
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập mật khẩu");
        }

        if (userRepository.existsByEmailIgnoreCase(trimmedEmail) || userRepository.existsByEmail(trimmedEmail)) {
            throw new IllegalArgumentException("Địa chỉ email này đã được sử dụng. Vui lòng đăng nhập hoặc dùng email khác.");
        }

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_USER").description("User").build()));

        User newUser = User.builder()
                .email(trimmedEmail)
                .fullName(rawFullName.trim())
                .passwordHash(passwordEncoder.encode(rawPassword))
                .roles(Set.of(userRole))
                .isActive(true)
                .isEmailVerified(true)
                .build();

        User saved = userRepository.save(newUser);

        return AuthDto.AuthResponse.builder()
                .token("jwt_token_" + saved.getId() + "_" + System.currentTimeMillis())
                .email(saved.getEmail())
                .fullName(saved.getFullName())
                .role("Người dùng")
                .avatarUrl(saved.getAvatarUrl())
                .phone(saved.getPhoneNumber())
                .build();
    }

    public void changePassword(Long userId, AuthDto.ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản người dùng"));

        if (request.getCurrentPassword() == null || !passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Mật khẩu hiện tại không chính xác");
        }

        if (request.getNewPassword() == null || request.getNewPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập mật khẩu mới");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword().trim()));
        user.setUpdatedAt(java.time.OffsetDateTime.now());
        userRepository.save(user);
    }

    public void verifyPassword(Long userId, String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập mật khẩu");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản người dùng"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadCredentialsException("Mật khẩu không chính xác. Vui lòng kiểm tra lại!");
        }
    }

    public AuthDto.AuthResponse updateProfile(Long userId, AuthDto.UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản người dùng"));

        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getPhone() != null) {
            user.setPhoneNumber(request.getPhone().trim());
        }
        if (request.getAvatarUrl() != null) {
            String trimmedAvatar = request.getAvatarUrl().trim();
            user.setAvatarUrl(trimmedAvatar.isEmpty() ? null : trimmedAvatar);
        }
        user.setUpdatedAt(java.time.OffsetDateTime.now());
        User saved = userRepository.save(user);

        String primaryRole = saved.getRoles().stream()
                .anyMatch(r -> "ROLE_ADMIN".equals(r.getName())) ? "Quản trị viên" : "Người dùng";

        return AuthDto.AuthResponse.builder()
                .token("jwt_token_" + saved.getId() + "_" + System.currentTimeMillis())
                .email(saved.getEmail())
                .fullName(saved.getFullName())
                .role(primaryRole)
                .avatarUrl(saved.getAvatarUrl())
                .phone(saved.getPhoneNumber())
                .build();
    }

    public AuthDto.AuthResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản"));

        String primaryRole = user.getRoles().stream()
                .anyMatch(r -> "ROLE_ADMIN".equals(r.getName())) ? "Quản trị viên" : "Người dùng";

        return AuthDto.AuthResponse.builder()
                .token("jwt_token_" + user.getId() + "_" + System.currentTimeMillis())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(primaryRole)
                .avatarUrl(user.getAvatarUrl())
                .phone(user.getPhoneNumber())
                .build();
    }

    @org.springframework.beans.factory.annotation.Value("${app.upload.dir:uploads}")
    private String uploadDir = "uploads";

    public AuthDto.AvatarUploadResponse uploadAvatar(Long userId, org.springframework.web.multipart.MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Tệp tải lên không được để trống");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Chỉ chấp nhận các tệp định dạng hình ảnh (JPEG, PNG, WebP, GIF, SVG)");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản người dùng"));

        try {
            java.nio.file.Path avatarDir = java.nio.file.Paths.get(uploadDir, "avatars").toAbsolutePath().normalize();
            if (!java.nio.file.Files.exists(avatarDir)) {
                java.nio.file.Files.createDirectories(avatarDir);
            }

            String originalFilename = file.getOriginalFilename();
            String extension = ".jpg";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
                if (!extension.matches("^\\.(jpe?g|png|webp|gif|svg)$")) {
                    extension = ".jpg";
                }
            }

            String fileName = "avatar_user_" + userId + "_" + System.currentTimeMillis() + extension;
            java.nio.file.Path targetPath = avatarDir.resolve(fileName);

            // Save file to disk
            java.nio.file.Files.copy(file.getInputStream(), targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            String publicUrl = "/api/uploads/avatars/" + fileName;
            user.setAvatarUrl(publicUrl);
            user.setUpdatedAt(java.time.OffsetDateTime.now());
            userRepository.save(user);

            // Lưu lịch sử media vào CSDL để qua máy khác vẫn đồng bộ
            try {
                mediaHistoryRepository.save(MediaHistory.builder()
                        .userId(userId)
                        .mediaType("AVATAR")
                        .url(publicUrl)
                        .fileName(originalFilename != null ? originalFilename : fileName)
                        .fileSize(file.getSize())
                        .createdAt(java.time.OffsetDateTime.now())
                        .build());
            } catch (Exception historyEx) {
                // Log and continue
            }

            return AuthDto.AvatarUploadResponse.builder()
                    .success(true)
                    .message("Tải ảnh đại diện thành công")
                    .avatarUrl(publicUrl)
                    .fileName(fileName)
                    .build();
        } catch (java.io.IOException e) {
            throw new RuntimeException("Lỗi khi lưu tệp ảnh trên máy chủ: " + e.getMessage(), e);
        }
    }
}
