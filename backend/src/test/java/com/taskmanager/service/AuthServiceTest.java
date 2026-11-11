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
}
