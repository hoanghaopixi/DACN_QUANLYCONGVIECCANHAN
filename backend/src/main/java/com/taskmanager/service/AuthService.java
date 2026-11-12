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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final MediaHistoryRepository mediaHistoryRepository;
    private final JwtTokenService jwtTokenService;
    private final com.taskmanager.repository.XacThucOtpRepository xacthucOtpRepository;
    private final EmailService emailService;

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
                .authProvider(user.getAuthProvider())
                .hasPassword(!"GOOGLE".equalsIgnoreCase(user.getAuthProvider()))
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

        // Nếu người dùng cung cấp mã OTP (xác thực OTP đăng ký)
        if (request.getOtp() != null && !request.getOtp().trim().isEmpty()) {
            String inputOtp = request.getOtp().trim();
            com.taskmanager.entity.XacThucOtp otp = xacthucOtpRepository
                    .findTopByEmailAndOtpTypeAndIsUsedFalseOrderByCreatedAtDesc(trimmedEmail.toLowerCase(), "REGISTER")
                    .orElseThrow(() -> new IllegalArgumentException("Mã OTP không hợp lệ hoặc đã hết hạn. Vui lòng yêu cầu gửi lại mã."));

            if (otp.getExpiresAt().isBefore(java.time.OffsetDateTime.now())) {
                throw new IllegalArgumentException("Mã OTP đã hết hạn (chỉ có hiệu lực trong 5 phút). Vui lòng yêu cầu mã mới.");
            }

            if (otp.getAttempts() != null && otp.getAttempts() >= 5) {
                throw new IllegalArgumentException("Mã OTP đã bị khóa do nhập sai quá 5 lần. Vui lòng gửi yêu cầu lấy mã mới.");
            }

            if (!otp.getOtpCode().equals(inputOtp)) {
                otp.setAttempts((otp.getAttempts() == null ? 0 : otp.getAttempts()) + 1);
                xacthucOtpRepository.save(otp);
                int remain = Math.max(0, 5 - otp.getAttempts());
                throw new BadCredentialsException("Mã OTP không chính xác. Bạn còn " + remain + " lần thử.");
            }

            // Đánh dấu OTP đã sử dụng
            otp.setIsUsed(true);
            xacthucOtpRepository.save(otp);
        }

        VaiTro userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(VaiTro.builder().name("ROLE_USER").description("User").build()));

        NguoiDung newUser = NguoiDung.builder()
                .email(trimmedEmail)
                .fullName(rawFullName.trim())
                .passwordHash(passwordEncoder.encode(rawPassword))
                .roles(Set.of(userRole))
                .authProvider("LOCAL")
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
                .authProvider(saved.getAuthProvider())
                .hasPassword(true)
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

    public AuthDto.AuthResponse setInitialPassword(Long userId, AuthDto.SetPasswordRequest request) {
        NguoiDung user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản người dùng"));

        if (request == null) {
            throw new IllegalArgumentException("Dữ liệu thiết lập tài khoản không được để trống");
        }

        String username = request.getUsername();
        if (username != null && !username.trim().isEmpty()) {
            if (username.trim().length() < 2 || username.trim().length() > 100) {
                throw new IllegalArgumentException("Tên người dùng / họ tên phải từ 2 đến 100 ký tự");
            }
            user.setFullName(username.trim());
        }

        String newPassword = request.getNewPassword();
        if (newPassword == null || newPassword.length() < 8 || newPassword.length() > 50
                || newPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Mật khẩu mới phải có từ 8 đến 50 ký tự và không vượt quá giới hạn mã hóa.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setAuthProvider("LOCAL_GOOGLE");
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
                .authProvider(saved.getAuthProvider())
                .hasPassword(true)
                .build();
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
                .authProvider(saved.getAuthProvider())
                .hasPassword(!"GOOGLE".equalsIgnoreCase(saved.getAuthProvider()))
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
                .authProvider(user.getAuthProvider())
                .hasPassword(!"GOOGLE".equalsIgnoreCase(user.getAuthProvider()))
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

    public java.util.Map<String, Object> sendRegisterOtp(AuthDto.SendRegisterOtpRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Dữ liệu yêu cầu OTP không được để trống");
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
        String trimmedEmail = rawEmail.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(trimmedEmail).matches()) {
            throw new IllegalArgumentException("Định dạng email không hợp lệ (Ví dụ: name@example.com)");
        }

        if (userRepository.existsByEmailIgnoreCase(trimmedEmail) || userRepository.existsByEmail(trimmedEmail)) {
            throw new IllegalArgumentException("Địa chỉ email này đã được sử dụng. Vui lòng đăng nhập hoặc dùng email khác.");
        }

        // Chống spam: tối đa 5 yêu cầu trong 15 phút
        long recentRequests = xacthucOtpRepository.countByEmailAndOtpTypeAndCreatedAtAfter(
                trimmedEmail, "REGISTER", java.time.OffsetDateTime.now().minusMinutes(15));
        if (recentRequests >= 5) {
            throw new IllegalArgumentException("Bạn đã yêu cầu quá nhiều mã OTP. Vui lòng thử lại sau 15 phút.");
        }

        // Sinh mã OTP 6 chữ số ngẫu nhiên an toàn
        int code = new java.security.SecureRandom().nextInt(900000) + 100000;
        String otpCode = String.valueOf(code);

        com.taskmanager.entity.XacThucOtp otp = com.taskmanager.entity.XacThucOtp.builder()
                .email(trimmedEmail)
                .otpCode(otpCode)
                .otpType("REGISTER")
                .expiresAt(java.time.OffsetDateTime.now().plusMinutes(5))
                .isUsed(false)
                .attempts(0)
                .createdAt(java.time.OffsetDateTime.now())
                .build();
        xacthucOtpRepository.save(otp);

        log.info("🔐 [SMARTTASK REGISTER OTP] Email: {} | Họ tên: {} | Mã OTP: >>> {} <<< (Hiệu lực: 5 phút)", trimmedEmail, rawFullName.trim(), otpCode);

        // Gửi email qua EmailService
        emailService.sendRegisterOtpEmail(trimmedEmail, otpCode, rawFullName.trim());

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("success", true);
        result.put("message", "Mã xác thực OTP đã được gửi đến email của bạn. Vui lòng kiểm tra hộp thư đến (hoặc hòm thư rác).");
        result.put("email", trimmedEmail);
        result.put("expiresInSeconds", 300);
        return result;
    }

    public java.util.Map<String, Object> forgotPassword(AuthDto.ForgotPasswordRequest request) {
        if (request == null || request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập địa chỉ email");
        }
        String trimmedEmail = request.getEmail().trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(trimmedEmail).matches()) {
            throw new IllegalArgumentException("Định dạng email không hợp lệ (Ví dụ: name@example.com)");
        }

        // Chống spam: tối đa 5 yêu cầu trong 15 phút
        long recentRequests = xacthucOtpRepository.countByEmailAndOtpTypeAndCreatedAtAfter(
                trimmedEmail, "FORGOT_PASSWORD", java.time.OffsetDateTime.now().minusMinutes(15));
        if (recentRequests >= 5) {
            throw new IllegalArgumentException("Bạn đã yêu cầu quá nhiều mã OTP. Vui lòng thử lại sau 15 phút.");
        }

        boolean userExists = userRepository.existsByEmailIgnoreCase(trimmedEmail) || userRepository.existsByEmail(trimmedEmail);

        // Sinh mã OTP 6 chữ số ngẫu nhiên an toàn
        int code = new java.security.SecureRandom().nextInt(900000) + 100000;
        String otpCode = String.valueOf(code);

        if (userExists) {
            com.taskmanager.entity.XacThucOtp otp = com.taskmanager.entity.XacThucOtp.builder()
                    .email(trimmedEmail)
                    .otpCode(otpCode)
                    .otpType("FORGOT_PASSWORD")
                    .expiresAt(java.time.OffsetDateTime.now().plusMinutes(5))
                    .isUsed(false)
                    .attempts(0)
                    .createdAt(java.time.OffsetDateTime.now())
                    .build();
            xacthucOtpRepository.save(otp);

            log.info("🔐 [SMARTTASK FORGOT PASSWORD OTP] Email: {} | Mã OTP: >>> {} <<< (Hiệu lực: 5 phút)", trimmedEmail, otpCode);

            String recipientName = userRepository.findByEmailIgnoreCase(trimmedEmail)
                    .or(() -> userRepository.findByEmail(trimmedEmail))
                    .map(NguoiDung::getFullName)
                    .orElse(null);

            // Gửi email qua EmailService
            emailService.sendOtpEmail(trimmedEmail, otpCode, recipientName);
        }

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("success", true);
        result.put("message", "Mã xác nhận OTP đã được gửi đến email của bạn. Vui lòng kiểm tra hộp thư đến (hoặc hòm thư rác).");
        result.put("email", trimmedEmail);
        result.put("expiresInSeconds", 300);
        return result;
    }

    public java.util.Map<String, Object> verifyOtp(AuthDto.VerifyOtpRequest request) {
        if (request == null || request.getEmail() == null || request.getOtp() == null) {
            throw new IllegalArgumentException("Thông tin xác thực OTP không đầy đủ");
        }
        String trimmedEmail = request.getEmail().trim().toLowerCase();
        String inputOtp = request.getOtp().trim();

        com.taskmanager.entity.XacThucOtp otp = xacthucOtpRepository
                .findTopByEmailAndOtpTypeAndIsUsedFalseOrderByCreatedAtDesc(trimmedEmail, "FORGOT_PASSWORD")
                .orElseThrow(() -> new IllegalArgumentException("Mã OTP không hợp lệ hoặc đã hết hạn. Vui lòng yêu cầu mã mới."));

        if (otp.getExpiresAt().isBefore(java.time.OffsetDateTime.now())) {
            throw new IllegalArgumentException("Mã OTP đã hết hạn (chỉ có hiệu lực trong 5 phút). Vui lòng yêu cầu mã mới.");
        }

        if (otp.getAttempts() != null && otp.getAttempts() >= 5) {
            throw new IllegalArgumentException("Mã OTP đã bị khóa do nhập sai quá 5 lần. Vui lòng gửi yêu cầu lấy mã mới.");
        }

        if (!otp.getOtpCode().equals(inputOtp)) {
            otp.setAttempts((otp.getAttempts() == null ? 0 : otp.getAttempts()) + 1);
            xacthucOtpRepository.save(otp);
            int remain = Math.max(0, 5 - otp.getAttempts());
            throw new BadCredentialsException("Mã OTP không chính xác. Bạn còn " + remain + " lần thử.");
        }

        return java.util.Map.of("success", true, "message", "Xác thực mã OTP thành công");
    }

    public java.util.Map<String, Object> resetPassword(AuthDto.ResetPasswordRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Dữ liệu đặt lại mật khẩu không được để trống");
        }
        String trimmedEmail = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        String inputOtp = request.getOtp() != null ? request.getOtp().trim() : "";
        String newPassword = request.getNewPassword();

        if (newPassword == null || newPassword.length() < 8 || newPassword.length() > 50
                || newPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Mật khẩu mới phải có từ 8 đến 50 ký tự và không vượt quá giới hạn mã hóa.");
        }

        com.taskmanager.entity.XacThucOtp otp = xacthucOtpRepository
                .findTopByEmailAndOtpTypeAndIsUsedFalseOrderByCreatedAtDesc(trimmedEmail, "FORGOT_PASSWORD")
                .orElseThrow(() -> new IllegalArgumentException("Mã OTP không hợp lệ hoặc đã được sử dụng."));

        if (otp.getExpiresAt().isBefore(java.time.OffsetDateTime.now())) {
            throw new IllegalArgumentException("Mã OTP đã hết hạn. Vui lòng yêu cầu mã mới.");
        }

        if (!otp.getOtpCode().equals(inputOtp)) {
            throw new BadCredentialsException("Mã OTP không chính xác. Vui lòng kiểm tra lại.");
        }

        NguoiDung user = userRepository.findByEmailIgnoreCase(trimmedEmail)
                .orElseGet(() -> userRepository.findByEmail(trimmedEmail)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản người dùng")));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(java.time.OffsetDateTime.now());
        userRepository.save(user);

        // Đánh dấu OTP đã sử dụng
        otp.setIsUsed(true);
        xacthucOtpRepository.save(otp);

        return java.util.Map.of("success", true, "message", "Đặt lại mật khẩu thành công. Vui lòng đăng nhập bằng mật khẩu mới của bạn!");
    }

    public AuthDto.AuthResponse loginWithGoogle(AuthDto.GoogleLoginRequest request) {
        if (request == null || request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("Thông tin tài khoản Google không hợp lệ");
        }
        String trimmedEmail = request.getEmail().trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(trimmedEmail).matches()) {
            throw new IllegalArgumentException("Email Google không hợp lệ");
        }

        String rawFullName = request.getFullName();
        String fullName = (rawFullName != null && !rawFullName.trim().isEmpty()) ? rawFullName.trim() : "Google User";
        String avatarUrl = request.getAvatarUrl();
        String googleId = resolveGoogleId(request.getIdToken(), trimmedEmail);

        // Tìm người dùng theo googleId hoặc theo email
        NguoiDung user = userRepository.findByGoogleId(googleId)
                .or(() -> userRepository.findByEmailIgnoreCase(trimmedEmail))
                .orElse(null);

        if (user != null) {
            if (user.getGoogleId() == null || user.getGoogleId().isBlank()) {
                user.setGoogleId(googleId);
            }
            if (avatarUrl != null && !avatarUrl.isBlank() && (user.getAvatarUrl() == null || user.getAvatarUrl().isBlank())) {
                user.setAvatarUrl(avatarUrl);
            }
            if (user.getAuthProvider() == null || (!"LOCAL_GOOGLE".equalsIgnoreCase(user.getAuthProvider()) && !"LOCAL".equalsIgnoreCase(user.getAuthProvider()))) {
                user.setAuthProvider("GOOGLE");
            } else if ("LOCAL".equalsIgnoreCase(user.getAuthProvider())) {
                user.setAuthProvider("LOCAL_GOOGLE");
            }
            user.setIsEmailVerified(true);
            user.setUpdatedAt(java.time.OffsetDateTime.now());
            user = userRepository.save(user);
        } else {
            VaiTro userRole = roleRepository.findByName("ROLE_USER")
                    .orElseGet(() -> roleRepository.save(VaiTro.builder().name("ROLE_USER").description("User").build()));

            NguoiDung newUser = NguoiDung.builder()
                    .email(trimmedEmail)
                    .fullName(fullName)
                    .googleId(googleId)
                    .avatarUrl(avatarUrl)
                    .authProvider("GOOGLE")
                    .passwordHash(passwordEncoder.encode(java.util.UUID.randomUUID().toString()))
                    .roles(Set.of(userRole))
                    .isActive(true)
                    .isEmailVerified(true)
                    .createdAt(java.time.OffsetDateTime.now())
                    .updatedAt(java.time.OffsetDateTime.now())
                    .build();

            user = userRepository.save(newUser);
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
                .authProvider(user.getAuthProvider())
                .hasPassword(!"GOOGLE".equalsIgnoreCase(user.getAuthProvider()))
                .build();
    }

    private String resolveGoogleId(String idToken, String email) {
        if (idToken != null && !idToken.isBlank()) {
            String trimmedToken = idToken.trim();
            // Nếu là Google ID token dạng JWT (header.payload.signature)
            if (trimmedToken.contains(".")) {
                try {
                    String[] parts = trimmedToken.split("\\.");
                    if (parts.length >= 2) {
                        byte[] decoded = java.util.Base64.getUrlDecoder().decode(parts[1]);
                        String payload = new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
                        JsonNode node = JSON_MAPPER.readTree(payload);
                        if (node.hasNonNull("sub")) {
                            String sub = node.get("sub").asString();
                            if (sub != null && !sub.isBlank()) {
                                return sub.length() > 100 ? sub.substring(0, 100) : sub;
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Không thể giải mã sub từ Google ID token: {}", e.getMessage());
                }
            }
            // Nếu token có độ dài hợp lệ (<= 100 ký tự)
            if (trimmedToken.length() <= 100) {
                return trimmedToken;
            }
            // Nếu token quá dài (e.g. Access token), băm SHA-256 để đảm bảo duy nhất và <= 100 ký tự
            try {
                java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
                byte[] hash = md.digest(trimmedToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                StringBuilder hex = new StringBuilder("g_hash_");
                for (byte b : hash) {
                    hex.append(String.format("%02x", b));
                }
                return hex.toString();
            } catch (Exception ignored) {
                return ("google_" + email).substring(0, Math.min(100, ("google_" + email).length()));
            }
        }
        String fallback = "google_" + email;
        return fallback.length() > 100 ? fallback.substring(0, 100) : fallback;
    }
}
