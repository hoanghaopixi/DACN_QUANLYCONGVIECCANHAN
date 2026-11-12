package com.taskmanager.service;

import com.taskmanager.dto.AuthDto;
import com.taskmanager.entity.NguoiDung;
import com.taskmanager.entity.VaiTro;
import com.taskmanager.repository.RoleRepository;
import com.taskmanager.repository.UserRepository;
import com.taskmanager.security.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private com.taskmanager.repository.XacThucOtpRepository xacThucOtpRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private NguoiDung sampleUser;

    @BeforeEach
    void setUp() {
        VaiTro role = VaiTro.builder().id(1).name("ROLE_USER").build();
        sampleUser = NguoiDung.builder()
                .id(1L)
                .email("test@example.com")
                .fullName("Test User")
                .passwordHash("encoded_hash_123")
                .isActive(true)
                .roles(Set.of(role))
                .build();
    }

    @Test
    @DisplayName("Login thành công khi email và mật khẩu chính xác")
    void login_Success() {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest("test@example.com", "password123");

        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("password123", "encoded_hash_123")).thenReturn(true);
        when(jwtTokenService.issueToken(sampleUser)).thenReturn("signed-test-token");

        AuthDto.AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("test@example.com", response.getEmail());
        assertEquals("Test User", response.getFullName());
        assertEquals("signed-test-token", response.getToken());
    }

    @Test
    @DisplayName("Login ném BadCredentialsException khi sai mật khẩu")
    void login_WrongPassword_ThrowsBadCredentialsException() {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest("test@example.com", "wrong_pass");

        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrong_pass", "encoded_hash_123")).thenReturn(false);

        BadCredentialsException ex = assertThrows(BadCredentialsException.class, () -> authService.login(request));
        assertEquals("Email hoặc mật khẩu không chính xác.", ex.getMessage());
    }

    @Test
    @DisplayName("Login dùng cùng thông báo lỗi khi tài khoản không tồn tại")
    void login_EmailNotFound_ThrowsGenericBadCredentialsException() {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest("notfound@example.com", "password123");

        when(userRepository.findByEmailIgnoreCase("notfound@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        BadCredentialsException ex = assertThrows(BadCredentialsException.class, () -> authService.login(request));
        assertEquals("Email hoặc mật khẩu không chính xác.", ex.getMessage());
    }

    @Test
    @DisplayName("Login ném IllegalArgumentException khi email sai regex")
    void login_InvalidEmailRegex_ThrowsIllegalArgumentException() {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest("invalid-email-format", "password123");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.login(request));
        assertTrue(ex.getMessage().contains("Định dạng email không hợp lệ"));
    }

    @Test
    @DisplayName("Login ném IllegalArgumentException khi mật khẩu rỗng")
    void login_EmptyPassword_ThrowsIllegalArgumentException() {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest("test@example.com", "");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.login(request));
        assertTrue(ex.getMessage().contains("Vui lòng nhập mật khẩu"));
    }

    @Test
    @DisplayName("Login dùng thông báo lỗi chung khi tài khoản bị vô hiệu hóa")
    void login_AccountInactive_ThrowsGenericBadCredentialsException() {
        sampleUser.setIsActive(false);
        AuthDto.LoginRequest request = new AuthDto.LoginRequest("test@example.com", "password123");

        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(sampleUser));

        BadCredentialsException ex = assertThrows(BadCredentialsException.class, () -> authService.login(request));
        assertEquals("Email hoặc mật khẩu không chính xác.", ex.getMessage());
    }

    @Test
    @DisplayName("Verify password thành công khi mật khẩu chính xác")
    void verifyPassword_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("password123", "encoded_hash_123")).thenReturn(true);

        assertDoesNotThrow(() -> authService.verifyPassword(1L, "password123"));
    }

    @Test
    @DisplayName("Verify password ném BadCredentialsException khi mật khẩu không đúng")
    void verifyPassword_WrongPassword_ThrowsBadCredentialsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrong_pass", "encoded_hash_123")).thenReturn(false);

        BadCredentialsException ex = assertThrows(BadCredentialsException.class, () -> authService.verifyPassword(1L, "wrong_pass"));
        assertEquals("Mật khẩu không chính xác. Vui lòng kiểm tra lại!", ex.getMessage());
    }

    @Test
    @DisplayName("forgotPassword thành công khi email tồn tại")
    void forgotPassword_Success() {
        when(userRepository.existsByEmailIgnoreCase("test@example.com")).thenReturn(true);
        when(xacThucOtpRepository.countByEmailAndOtpTypeAndCreatedAtAfter(eq("test@example.com"), eq("FORGOT_PASSWORD"), any())).thenReturn(0L);

        java.util.Map<String, Object> response = authService.forgotPassword(new AuthDto.ForgotPasswordRequest("test@example.com"));

        assertEquals(true, response.get("success"));
        verify(emailService, times(1)).sendOtpEmail(eq("test@example.com"), anyString(), any());
        verify(xacThucOtpRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("forgotPassword trả về generic message khi email không tồn tại để bảo mật")
    void forgotPassword_EmailNotFound_StillReturnsSuccessGenericMessage() {
        when(userRepository.existsByEmailIgnoreCase("unknown@example.com")).thenReturn(false);
        when(userRepository.existsByEmail("unknown@example.com")).thenReturn(false);

        java.util.Map<String, Object> response = authService.forgotPassword(new AuthDto.ForgotPasswordRequest("unknown@example.com"));

        assertEquals(true, response.get("success"));
        verify(emailService, never()).sendOtpEmail(any(), any(), any());
    }

    @Test
    @DisplayName("sendRegisterOtp thành công khi email chưa tồn tại")
    void sendRegisterOtp_Success() {
        when(userRepository.existsByEmailIgnoreCase("newuser@example.com")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(xacThucOtpRepository.countByEmailAndOtpTypeAndCreatedAtAfter(eq("newuser@example.com"), eq("REGISTER"), any())).thenReturn(0L);

        java.util.Map<String, Object> response = authService.sendRegisterOtp(new AuthDto.SendRegisterOtpRequest("Nguyen Van A", "newuser@example.com"));

        assertEquals(true, response.get("success"));
        verify(emailService, times(1)).sendRegisterOtpEmail(eq("newuser@example.com"), anyString(), eq("Nguyen Van A"));
        verify(xacThucOtpRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("sendRegisterOtp ném IllegalArgumentException khi email đã được đăng ký")
    void sendRegisterOtp_EmailAlreadyExists_ThrowsException() {
        when(userRepository.existsByEmailIgnoreCase("existing@example.com")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                authService.sendRegisterOtp(new AuthDto.SendRegisterOtpRequest("Nguyen Van A", "existing@example.com")));
        assertTrue(ex.getMessage().contains("đã được sử dụng"));
        verify(emailService, never()).sendRegisterOtpEmail(any(), any(), any());
    }

    @Test
    @DisplayName("register ném IllegalArgumentException khi mã OTP không đúng")
    void register_InvalidOtp_ThrowsException() {
        when(userRepository.existsByEmailIgnoreCase("newuser@example.com")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);

        com.taskmanager.entity.XacThucOtp otpRecord = com.taskmanager.entity.XacThucOtp.builder()
                .email("newuser@example.com")
                .otpCode("123456")
                .otpType("REGISTER")
                .expiresAt(java.time.OffsetDateTime.now().plusMinutes(5))
                .isUsed(false)
                .attempts(0)
                .build();
        when(xacThucOtpRepository.findTopByEmailAndOtpTypeAndIsUsedFalseOrderByCreatedAtDesc("newuser@example.com", "REGISTER"))
                .thenReturn(Optional.of(otpRecord));

        AuthDto.RegisterRequest request = new AuthDto.RegisterRequest("Nguyen Van A", "newuser@example.com", "password123", "999999");

        BadCredentialsException ex = assertThrows(BadCredentialsException.class, () -> authService.register(request));
        assertTrue(ex.getMessage().contains("không chính xác"));
        assertEquals(1, otpRecord.getAttempts());
    }

    @Test
    @DisplayName("register thành công khi mã OTP chính xác")
    void register_ValidOtp_Success() {
        when(userRepository.existsByEmailIgnoreCase("newuser@example.com")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);

        com.taskmanager.entity.XacThucOtp otpRecord = com.taskmanager.entity.XacThucOtp.builder()
                .email("newuser@example.com")
                .otpCode("123456")
                .otpType("REGISTER")
                .expiresAt(java.time.OffsetDateTime.now().plusMinutes(5))
                .isUsed(false)
                .attempts(0)
                .build();
        when(xacThucOtpRepository.findTopByEmailAndOtpTypeAndIsUsedFalseOrderByCreatedAtDesc("newuser@example.com", "REGISTER"))
                .thenReturn(Optional.of(otpRecord));

        VaiTro role = VaiTro.builder().id(1).name("ROLE_USER").build();
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("password123")).thenReturn("hashed_pass");

        NguoiDung savedUser = NguoiDung.builder()
                .id(99L)
                .fullName("Nguyen Van A")
                .email("newuser@example.com")
                .passwordHash("hashed_pass")
                .isActive(true)
                .roles(Set.of(role))
                .build();
        when(userRepository.save(any(NguoiDung.class))).thenReturn(savedUser);
        when(jwtTokenService.issueToken(any(NguoiDung.class))).thenReturn("token-register-123");

        AuthDto.RegisterRequest request = new AuthDto.RegisterRequest("Nguyen Van A", "newuser@example.com", "password123", "123456");
        AuthDto.AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("newuser@example.com", response.getEmail());
        assertEquals("token-register-123", response.getToken());
        assertTrue(otpRecord.getIsUsed());
    }
}
