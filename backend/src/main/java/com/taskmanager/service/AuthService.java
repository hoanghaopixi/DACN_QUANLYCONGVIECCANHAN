package com.taskmanager.service;

import com.taskmanager.dto.AuthDto;
import com.taskmanager.entity.LichSuMedia;
import com.taskmanager.entity.VaiTro;
import com.taskmanager.entity.NguoiDung;
import com.taskmanager.exception.ResourceNotFoundException;
import com.taskmanager.repository.MediaHistoryRepository;
import com.taskmanager.repository.RoleRepository;
import com.taskmanager.repository.UserRepository;
import com.taskmanager.security.JwtTokenService;
import com.taskmanager.util.UploadedMediaValidator;
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
    private final JwtTokenService jwtTokenService;

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
        if (rawPassword.length() > 50 || rawPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Mật khẩu không được vượt quá giới hạn hỗ trợ.");
        }

        // Tìm kiếm người dùng theo email (hỗ trợ phân biệt hoa thường và không phân biệt)
        NguoiDung user = userRepository.findByEmailIgnoreCase(trimmedEmail)
                .orElseGet(() -> userRepository.findByEmail(trimmedEmail)
                        .orElseThrow(() -> new BadCredentialsException("Email hoặc mật khẩu không chính xác.")));

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new BadCredentialsException("Email hoặc mật khẩu không chính xác.");
        }

        // Kiểm tra mật khẩu mã hóa BCrypt
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("Email hoặc mật khẩu không chính xác.");
        }

        String primaryRole = user.getRoles().stream()
                .anyMatch(r -> "ROLE_ADMIN".equals(r.getName())) ? "Quản trị viên" : "Người dùng";

        return AuthDto.AuthResponse.builder()
                .token(jwtTokenService.issueToken(user))
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
        if (rawPassword.length() < 8 || rawPassword.length() > 50
                || rawPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Mật khẩu phải có từ 8 đến 50 ký tự và không vượt quá giới hạn mã hóa.");
        }

        if (userRepository.existsByEmailIgnoreCase(trimmedEmail) || userRepository.existsByEmail(trimmedEmail)) {
            throw new IllegalArgumentException("Địa chỉ email này đã được sử dụng. Vui lòng đăng nhập hoặc dùng email khác.");
        }

        VaiTro userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(VaiTro.builder().name("ROLE_USER").description("User").build()));

        NguoiDung newUser = NguoiDung.builder()
                .email(trimmedEmail)
                .fullName(rawFullName.trim())
                .passwordHash(passwordEncoder.encode(rawPassword))
                .roles(Set.of(userRole))
                .isActive(true)
                .isEmailVerified(true)
                .build();

        NguoiDung saved = userRepository.save(newUser);

        return AuthDto.AuthResponse.builder()
                .token(jwtTokenService.issueToken(saved))
                .email(saved.getEmail())
                .fullName(saved.getFullName())
                .role("Người dùng")
                .avatarUrl(saved.getAvatarUrl())
                .phone(saved.getPhoneNumber())
                .build();
    }

    public void changePassword(Long userId, AuthDto.ChangePasswordRequest request) {
        NguoiDung user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản người dùng"));

        if (request.getCurrentPassword() == null || !passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Mật khẩu hiện tại không chính xác");
        }

        String newPassword = request.getNewPassword();
        if (newPassword == null || newPassword.length() < 8 || newPassword.length() > 50
                || newPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Mật khẩu mới phải có từ 8 đến 50 ký tự và không vượt quá giới hạn mã hóa.");
        }

        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu hiện tại.");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(java.time.OffsetDateTime.now());
        userRepository.save(user);
    }

    public void verifyPassword(Long userId, String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập mật khẩu");
        }
        NguoiDung user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản người dùng"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadCredentialsException("Mật khẩu không chính xác. Vui lòng kiểm tra lại!");
        }
    }

    public AuthDto.AuthResponse updateProfile(Long userId, AuthDto.UpdateProfileRequest request) {
        NguoiDung user = userRepository.findById(userId)
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
        NguoiDung saved = userRepository.save(user);

        String primaryRole = saved.getRoles().stream()
                .anyMatch(r -> "ROLE_ADMIN".equals(r.getName())) ? "Quản trị viên" : "Người dùng";

        return AuthDto.AuthResponse.builder()
                .token(jwtTokenService.issueToken(saved))
                .email(saved.getEmail())
                .fullName(saved.getFullName())
                .role(primaryRole)
                .avatarUrl(saved.getAvatarUrl())
                .phone(saved.getPhoneNumber())
                .build();
    }

    public AuthDto.AuthResponse getProfile(Long userId) {
        NguoiDung user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản"));

        String primaryRole = user.getRoles().stream()
                .anyMatch(r -> "ROLE_ADMIN".equals(r.getName())) ? "Quản trị viên" : "Người dùng";

        return AuthDto.AuthResponse.builder()
                .token(jwtTokenService.issueToken(user))
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

        UploadedMediaValidator.DetectedMedia detectedMedia;
        try {
            detectedMedia = UploadedMediaValidator.detectImage(file);
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException("Không đọc được tệp ảnh tải lên.", e);
        }

        NguoiDung user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản người dùng"));

        try {
            java.nio.file.Path avatarDir = java.nio.file.Paths.get(uploadDir, "avatars").toAbsolutePath().normalize();
            if (!java.nio.file.Files.exists(avatarDir)) {
                java.nio.file.Files.createDirectories(avatarDir);
            }

            String originalFilename = file.getOriginalFilename();
            String extension = detectedMedia.extension();
            String fileName = java.util.UUID.randomUUID() + extension;
            java.nio.file.Path targetPath = avatarDir.resolve(fileName);

            // Save file to disk
            java.nio.file.Files.copy(file.getInputStream(), targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            String publicUrl = "/api/uploads/avatars/" + fileName;
            user.setAvatarUrl(publicUrl);
            user.setUpdatedAt(java.time.OffsetDateTime.now());
            userRepository.save(user);

            // Lưu lịch sử media vào CSDL để qua máy khác vẫn đồng bộ
            try {
                mediaHistoryRepository.save(LichSuMedia.builder()
                        .userId(userId)
                        .mediaType("AVATAR")
                        .url(publicUrl)
                        .fileName(originalFilename != null ? originalFilename.substring(0, Math.min(originalFilename.length(), 255)) : fileName)
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
