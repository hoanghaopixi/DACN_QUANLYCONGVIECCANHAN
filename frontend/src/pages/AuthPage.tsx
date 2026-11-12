import React, { useState, useEffect } from 'react';
import {
  Lock, Mail, User, Eye, EyeOff,
  Sparkles, CheckCircle2, ArrowRight, Sun, Moon,
  RefreshCw, ArrowLeft
} from 'lucide-react';
import { authService, AuthResponse } from '../services/authService';
import { validateLoginForm, validateRegisterForm, formatAuthErrorMessage } from '../utils/authValidation';
import ForgotPasswordModal from '../components/ForgotPasswordModal';
import { openGoogleLoginPopup } from '../services/googleAuth';

const GoogleIcon = () => (
  <svg width="18" height="18" viewBox="0 0 24 24">
    <path
      fill="#4285F4"
      d="M23.745 12.27c0-.7-.06-1.4-.19-2.07H12v4.51h6.6c-.29 1.52-1.14 2.8-2.4 3.65v3.03h3.88c2.27-2.09 3.665-5.17 3.665-9.12z"
    />
    <path
      fill="#34A853"
      d="M12 24c3.24 0 5.95-1.08 7.93-2.91l-3.88-3.03c-1.08.72-2.45 1.16-4.05 1.16-3.12 0-5.77-2.1-6.72-4.93H1.26v3.13C3.27 21.39 7.33 24 12 24z"
    />
    <path
      fill="#FBBC05"
      d="M5.28 14.29c-.25-.72-.38-1.49-.38-2.29s.13-1.57.38-2.29V6.58H1.26C.46 8.17 0 9.99 0 12s.46 3.83 1.26 5.42l4.02-3.13z"
    />
    <path
      fill="#EA4335"
      d="M12 4.75c1.77 0 3.35.61 4.6 1.8l3.42-3.42C17.95 1.19 15.24 0 12 0 7.33 0 3.27 2.61 1.26 6.58l4.02 3.13c.95-2.83 3.6-4.96 6.72-4.96z"
    />
  </svg>
);

interface AuthPageProps {
  onLoginSuccess: (user: AuthResponse) => void;
  theme: 'light' | 'dark';
  onToggleTheme: () => void;
}

export default function AuthPage({ onLoginSuccess, theme, onToggleTheme }: AuthPageProps) {
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [errorField, setErrorField] = useState<'email' | 'password' | 'fullName' | 'confirmPassword' | 'otp' | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isForgotPasswordOpen, setIsForgotPasswordOpen] = useState(false);

  // OTP Verification for Registration
  const [isRegisterOtpStep, setIsRegisterOtpStep] = useState(false);
  const [registerOtp, setRegisterOtp] = useState('');
  const [resendCountdown, setResendCountdown] = useState(0);
  const [isSendingRegisterOtp, setIsSendingRegisterOtp] = useState(false);

  useEffect(() => {
    if (resendCountdown <= 0) return;
    const interval = setInterval(() => {
      setResendCountdown((prev) => prev - 1);
    }, 1000);
    return () => clearInterval(interval);
  }, [resendCountdown]);

  const handleGoogleLogin = async () => {
    setIsLoading(true);
    setErrorMessage(null);
    try {
      const response = await openGoogleLoginPopup();
      setSuccessMessage(`Đăng nhập Google thành công! Chào mừng ${response.fullName}`);
      setTimeout(() => {
        onLoginSuccess(response);
      }, 500);
    } catch (err: any) {
      const msg = formatAuthErrorMessage(err);
      if (
        msg.includes('đã được đóng') ||
        msg.includes('popup_closed') ||
        msg.includes('closed') ||
        msg.includes('user_closed_popup')
      ) {
        // Người dùng chủ động tắt popup, reset trạng thái mà không báo lỗi đỏ
      } else {
        setErrorMessage(msg);
      }
    } finally {
      setIsLoading(false);
    }
  };

  const switchMode = (newMode: 'login' | 'register') => {
    setMode(newMode);
    setIsRegisterOtpStep(false);
    setRegisterOtp('');
    setResendCountdown(0);
    setErrorMessage(null);
    setErrorField(null);
    setSuccessMessage(null);
  };

  const handleResendRegisterOtp = async () => {
    if (resendCountdown > 0 || isSendingRegisterOtp) return;
    setErrorMessage(null);
    setIsSendingRegisterOtp(true);
    try {
      await authService.sendRegisterOtp({
        fullName: fullName.trim(),
        email: email.trim(),
      });
      setResendCountdown(60);
      setSuccessMessage('✓ Đã gửi lại mã OTP mới đến ' + email.trim() + '. Vui lòng kiểm tra hộp thư!');
    } catch (err: any) {
      const msg = formatAuthErrorMessage(err);
      setErrorMessage(msg);
    } finally {
      setIsSendingRegisterOtp(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    setErrorField(null);
    setSuccessMessage(null);

    // Kiểm tra định dạng & regex trước khi gửi yêu cầu lên máy chủ
    if (mode === 'login') {
      const validation = validateLoginForm(email, password);
      if (!validation.isValid) {
        setErrorMessage(validation.message || 'Thông tin đăng nhập không hợp lệ.');
        setErrorField(validation.field || null);
        return;
      }

      setIsLoading(true);
      try {
        const response = await authService.login({
          email: email.trim(),
          password: password,
        });

        setSuccessMessage(`Đăng nhập thành công! Chào mừng ${response.fullName}`);
        setTimeout(() => {
          onLoginSuccess(response);
        }, 500);
      } catch (err: any) {
        const msg = formatAuthErrorMessage(err);
        setErrorMessage(msg);

        if (err.response?.status === 401) {
          setErrorField('password');
        } else if (err.response?.status === 404 || err.response?.status === 409) {
          setErrorField('email');
        }
      } finally {
        setIsLoading(false);
      }
    } else {
      // Register mode
      if (!isRegisterOtpStep) {
        const validation = validateRegisterForm(fullName, email, password, confirmPassword);
        if (!validation.isValid) {
          setErrorMessage(validation.message || 'Thông tin đăng ký không hợp lệ.');
          setErrorField(validation.field || null);
          return;
        }

        setIsSendingRegisterOtp(true);
        try {
          const res = await authService.sendRegisterOtp({
            fullName: fullName.trim(),
            email: email.trim(),
          });
          setIsRegisterOtpStep(true);
          setResendCountdown(60);
          setSuccessMessage(res.message || `Mã xác thực OTP đã được gửi đến ${email.trim()}. Vui lòng kiểm tra email!`);
        } catch (err: any) {
          const msg = formatAuthErrorMessage(err);
          setErrorMessage(msg);
          if (err.response?.status === 409) {
            setErrorField('email');
          }
        } finally {
          setIsSendingRegisterOtp(false);
        }
      } else {
        // Step 2: Validate and submit OTP to complete registration
        const trimmedOtp = registerOtp.trim();
        if (!trimmedOtp || trimmedOtp.length < 6) {
          setErrorMessage('Vui lòng nhập đủ 6 chữ số mã OTP xác thực');
          setErrorField('otp');
          return;
        }

        setIsLoading(true);
        try {
          const response = await authService.register({
            fullName: fullName.trim(),
            email: email.trim(),
            password: password,
            otp: trimmedOtp,
          });

          setSuccessMessage('✓ Đăng ký tài khoản và xác thực thành công! Đang chuyển hướng...');
          setTimeout(() => {
            onLoginSuccess(response);
          }, 600);
        } catch (err: any) {
          const msg = formatAuthErrorMessage(err);
          setErrorMessage(msg);
          setErrorField('otp');
        } finally {
          setIsLoading(false);
        }
      }
    }
  };

  return (
    <div
      style={{
        minHeight: '100vh',
        width: '100vw',
        background: 'var(--bg)',
        color: 'var(--text)',
        display: 'flex',
        flexDirection: 'column',
        position: 'relative',
        overflowY: 'auto',
      }}
    >
      {/* ── Top Header Navigation ── */}
      <header
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '16px 32px',
          borderBottom: '1px solid var(--border-light)',
          background: 'var(--surface)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <div
            style={{
              width: 36,
              height: 36,
              borderRadius: 10,
              background: '#6C5CE7',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: '0 4px 12px rgba(108, 92, 231, 0.3)',
            }}
          >
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
              <path d="M7 10V14M10 7V17M14 5V19M17 10V14" stroke="white" strokeWidth="2.2" strokeLinecap="round" />
            </svg>
          </div>
          <div>
            <span style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--primary)', letterSpacing: '-0.02em' }}>
              SmartTask AI
            </span>
          </div>
        </div>

        {/* Theme Toggle */}
        <button
          onClick={onToggleTheme}
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 6,
            padding: '7px 12px',
            borderRadius: 8,
            border: '1px solid var(--border)',
            background: 'var(--surface)',
            color: 'var(--text-2)',
            fontSize: '0.82rem',
            fontWeight: 500,
            cursor: 'pointer',
          }}
          title="Chuyển chế độ Sáng / Tối"
        >
          {theme === 'dark' ? <Sun size={15} color="#F59E0B" /> : <Moon size={15} color="#6C5CE7" />}
          <span>{theme === 'dark' ? 'Giao diện Sáng' : 'Giao diện Tối'}</span>
        </button>
      </header>

      {/* ── Main Content Body ── */}
      <main
        style={{
          flex: 1,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          padding: '40px 20px',
        }}
      >
        <div
          style={{
            width: '100%',
            maxWidth: 1000,
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))',
            gap: 40,
            alignItems: 'center',
          }}
        >
          {/* ── Left Column: Brand, Slogan, Features ── */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
            {/* App Logo */}
            <div
              style={{
                width: 64,
                height: 64,
                borderRadius: 18,
                background: 'linear-gradient(135deg, #6C5CE7 0%, #A29BFE 100%)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                boxShadow: '0 8px 24px rgba(108, 92, 231, 0.35)',
              }}
            >
              <svg width="36" height="36" viewBox="0 0 24 24" fill="none">
                <path d="M7 10V14M10 7V17M14 5V19M17 10V14" stroke="white" strokeWidth="2.5" strokeLinecap="round" />
              </svg>
            </div>

            <div>
              <div
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: 6,
                  padding: '4px 10px',
                  borderRadius: 20,
                  background: 'var(--primary-bg)',
                  color: 'var(--primary)',
                  fontSize: '0.78rem',
                  fontWeight: 700,
                  marginBottom: 10,
                }}
              >
                <Sparkles size={13} />
                <span>NỀN TẢNG QUẢN LÝ CÔNG VIỆC THÔNG MINH</span>
              </div>
              <h1
                style={{
                  fontSize: '2.2rem',
                  fontWeight: 800,
                  color: 'var(--text)',
                  lineHeight: 1.25,
                  margin: 0,
                  letterSpacing: '-0.02em',
                }}
              >
                SmartTask AI
              </h1>
              <h2
                style={{
                  fontSize: '1.25rem',
                  fontWeight: 600,
                  color: 'var(--primary)',
                  margin: '8px 0 0',
                  lineHeight: 1.4,
                }}
              >
                Quản lý và Tự động Lập lịch Công việc Cá nhân
              </h2>
            </div>

            <p style={{ fontSize: '0.95rem', color: 'var(--text-3)', lineHeight: 1.6, margin: 0 }}>
              Trợ lý AI tự động phân tích thời gian biểu, tìm kiếm khung giờ trống tối ưu và đồng bộ độc lập cho từng không gian làm việc của bạn.
            </p>

            {/* Feature Bullets */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: 12, marginTop: 4 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10, fontSize: '0.88rem', color: 'var(--text-2)' }}>
                <CheckCircle2 size={18} color="#10B981" />
                <span>Không gian làm việc & nhóm danh mục độc lập cho từng tài khoản</span>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10, fontSize: '0.88rem', color: 'var(--text-2)' }}>
                <CheckCircle2 size={18} color="#10B981" />
                <span>Kéo thả dời giờ thông minh và cảnh báo xung đột lịch trình</span>
              </div>
            </div>
          </div>

          {/* ── Right Column: Login / Register Form Box ── */}
          <div
            style={{
              background: 'var(--surface)',
              borderRadius: 20,
              padding: '32px 28px',
              border: '1px solid var(--border)',
              boxShadow: 'var(--shadow-lg), 0 20px 30px rgba(0,0,0,0.06)',
            }}
          >
            {/* Mode Switcher Tabs */}
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: '1fr 1fr',
                gap: 4,
                padding: 4,
                background: 'var(--border-light)',
                borderRadius: 12,
                marginBottom: 24,
              }}
            >
              <button
                type="button"
                onClick={() => switchMode('login')}
                style={{
                  padding: '9px 12px',
                  borderRadius: 8,
                  fontSize: '0.88rem',
                  fontWeight: mode === 'login' ? 700 : 500,
                  background: mode === 'login' ? 'var(--surface)' : 'transparent',
                  color: mode === 'login' ? 'var(--primary)' : 'var(--text-muted)',
                  border: 'none',
                  cursor: 'pointer',
                  boxShadow: mode === 'login' ? '0 1px 3px rgba(0,0,0,0.08)' : 'none',
                  transition: 'all 0.15s ease',
                }}
              >
                Đăng nhập
              </button>

              <button
                type="button"
                onClick={() => switchMode('register')}
                style={{
                  padding: '9px 12px',
                  borderRadius: 8,
                  fontSize: '0.88rem',
                  fontWeight: mode === 'register' ? 700 : 500,
                  background: mode === 'register' ? 'var(--surface)' : 'transparent',
                  color: mode === 'register' ? 'var(--primary)' : 'var(--text-muted)',
                  border: 'none',
                  cursor: 'pointer',
                  boxShadow: mode === 'register' ? '0 1px 3px rgba(0,0,0,0.08)' : 'none',
                  transition: 'all 0.15s ease',
                }}
              >
                Đăng ký tài khoản
              </button>
            </div>

            {/* Error & Success Messages */}
            {errorMessage && (
              <div
                style={{
                  background: 'rgba(239, 68, 68, 0.1)',
                  border: '1px solid rgba(239, 68, 68, 0.35)',
                  color: '#DC2626',
                  borderRadius: 10,
                  padding: '11px 14px',
                  fontSize: '0.84rem',
                  marginBottom: 16,
                  fontWeight: 600,
                  lineHeight: 1.45,
                }}
              >
                {errorMessage}
              </div>
            )}

            {successMessage && (
              <div
                style={{
                  background: '#D1FAE5',
                  border: '1px solid #A7F3D0',
                  color: '#065F46',
                  borderRadius: 10,
                  padding: '10px 14px',
                  fontSize: '0.82rem',
                  marginBottom: 16,
                  fontWeight: 600,
                  lineHeight: 1.45,
                }}
              >
                {successMessage}
              </div>
            )}

            {/* The Form */}
            {mode === 'register' && isRegisterOtpStep ? (
              <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                {/* Email Confirmation Notice */}
                <div
                  style={{
                    padding: '12px 14px',
                    borderRadius: 10,
                    background: 'var(--primary-bg)',
                    border: '1px solid color-mix(in srgb, var(--primary) 25%, transparent)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    gap: 10,
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8, overflow: 'hidden' }}>
                    <Mail size={16} style={{ color: 'var(--primary)', flexShrink: 0 }} />
                    <div style={{ fontSize: '0.82rem', color: 'var(--text-2)', textOverflow: 'ellipsis', overflow: 'hidden', whiteSpace: 'nowrap' }}>
                      Mã xác thực gửi tới: <strong style={{ color: 'var(--text)' }}>{email}</strong>
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={() => {
                      setIsRegisterOtpStep(false);
                      setRegisterOtp('');
                      setErrorMessage(null);
                    }}
                    style={{
                      background: 'none',
                      border: 'none',
                      color: 'var(--primary)',
                      fontSize: '0.78rem',
                      fontWeight: 600,
                      cursor: 'pointer',
                      flexShrink: 0,
                      textDecoration: 'underline',
                    }}
                  >
                    Đổi email
                  </button>
                </div>

                {/* OTP Input Field */}
                <div>
                  <label
                    style={{
                      display: 'block',
                      fontSize: '0.84rem',
                      fontWeight: 600,
                      color: 'var(--text)',
                      marginBottom: 8,
                      textAlign: 'center',
                    }}
                  >
                    Nhập mã xác thực OTP (6 chữ số) *
                  </label>
                  <div style={{ position: 'relative', maxWidth: 280, margin: '0 auto' }}>
                    <input
                      type="text"
                      inputMode="numeric"
                      pattern="[0-9]*"
                      maxLength={6}
                      autoFocus
                      required
                      placeholder="••••••"
                      value={registerOtp}
                      onChange={(e) => {
                        const val = e.target.value.replace(/\D/g, '').slice(0, 6);
                        setRegisterOtp(val);
                        if (errorMessage) setErrorMessage(null);
                        if (errorField === 'otp') setErrorField(null);
                      }}
                      style={{
                        width: '100%',
                        padding: '12px 16px',
                        borderRadius: 12,
                        border: errorField === 'otp' ? '2px solid #EF4444' : '1.5px solid var(--border)',
                        boxShadow: errorField === 'otp' ? '0 0 0 3px rgba(239, 68, 68, 0.15)' : 'none',
                        background: 'var(--surface)',
                        color: 'var(--text)',
                        fontSize: '1.6rem',
                        fontWeight: 800,
                        letterSpacing: '12px',
                        textAlign: 'center',
                        outline: 'none',
                        transition: 'all 0.2s ease',
                      }}
                    />
                  </div>
                  <p style={{ fontSize: '0.76rem', color: 'var(--text-3)', textAlign: 'center', margin: '8px 0 0' }}>
                    Mã xác thực có hiệu lực trong vòng <strong>5 phút</strong>.
                  </p>
                </div>

                {/* Resend OTP Row */}
                <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: 6, fontSize: '0.82rem' }}>
                  <span style={{ color: 'var(--text-muted)' }}>Chưa nhận được mã?</span>
                  {resendCountdown > 0 ? (
                    <span style={{ color: 'var(--text-muted)', fontWeight: 600 }}>
                      Gửi lại sau ({resendCountdown}s)
                    </span>
                  ) : (
                    <button
                      type="button"
                      onClick={handleResendRegisterOtp}
                      disabled={isSendingRegisterOtp}
                      style={{
                        background: 'none',
                        border: 'none',
                        color: 'var(--primary)',
                        fontWeight: 700,
                        cursor: isSendingRegisterOtp ? 'not-allowed' : 'pointer',
                        display: 'flex',
                        alignItems: 'center',
                        gap: 4,
                        padding: 0,
                      }}
                    >
                      <RefreshCw size={13} className={isSendingRegisterOtp ? 'spin' : ''} />
                      <span>{isSendingRegisterOtp ? 'Đang gửi...' : 'Gửi lại mã'}</span>
                    </button>
                  )}
                </div>

                {/* Submit & Back Buttons */}
                <div style={{ display: 'flex', flexDirection: 'column', gap: 10, marginTop: 4 }}>
                  <button
                    type="submit"
                    disabled={isLoading || registerOtp.length < 6}
                    style={{
                      padding: '12px 20px',
                      borderRadius: 10,
                      border: 'none',
                      background: 'var(--primary)',
                      color: '#FFFFFF',
                      fontSize: '0.92rem',
                      fontWeight: 700,
                      cursor: (isLoading || registerOtp.length < 6) ? 'not-allowed' : 'pointer',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      gap: 8,
                      boxShadow: '0 4px 14px rgba(108, 92, 231, 0.4)',
                      transition: 'all 0.2s ease',
                      opacity: (isLoading || registerOtp.length < 6) ? 0.65 : 1,
                    }}
                  >
                    <span>{isLoading ? 'Đang xác thực...' : 'Xác nhận & Hoàn tất đăng ký'}</span>
                    {!isLoading && <CheckCircle2 size={16} />}
                  </button>

                  <button
                    type="button"
                    onClick={() => {
                      setIsRegisterOtpStep(false);
                      setRegisterOtp('');
                      setErrorMessage(null);
                    }}
                    style={{
                      padding: '10px 16px',
                      borderRadius: 10,
                      border: '1px solid var(--border)',
                      background: 'transparent',
                      color: 'var(--text-2)',
                      fontSize: '0.84rem',
                      fontWeight: 600,
                      cursor: 'pointer',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      gap: 6,
                    }}
                  >
                    <ArrowLeft size={15} />
                    <span>Quay lại chỉnh sửa thông tin</span>
                  </button>
                </div>
              </form>
            ) : (
            <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
              {/* Full Name (Only on Register) */}
              {mode === 'register' && (
                <div>
                  <label
                    style={{
                      display: 'block',
                      fontSize: '0.82rem',
                      fontWeight: 600,
                      color: 'var(--text)',
                      marginBottom: 6,
                    }}
                  >
                    Họ và tên *
                  </label>
                  <div style={{ position: 'relative' }}>
                    <User
                      size={16}
                      style={{ position: 'absolute', left: 12, top: 12, color: 'var(--text-muted)' }}
                    />
                    <input
                      type="text"
                      required
                      placeholder="VD: Nguyễn Văn A"
                      value={fullName}
                      onChange={(e) => {
                        setFullName(e.target.value);
                        if (errorMessage) setErrorMessage(null);
                        if (errorField === 'fullName') setErrorField(null);
                      }}
                      style={{
                        width: '100%',
                        padding: '10px 12px 10px 38px',
                        borderRadius: 10,
                        border: errorField === 'fullName' ? '1.5px solid #EF4444' : '1px solid var(--border)',
                        boxShadow: errorField === 'fullName' ? '0 0 0 3px rgba(239, 68, 68, 0.15)' : 'none',
                        background: 'var(--surface)',
                        color: 'var(--text)',
                        fontSize: '0.88rem',
                        outline: 'none',
                        transition: 'all 0.2s ease',
                      }}
                    />
                  </div>
                </div>
              )}

              {/* Email / Username Box */}
              <div>
                <label
                  style={{
                    display: 'block',
                    fontSize: '0.82rem',
                    fontWeight: 600,
                    color: 'var(--text)',
                    marginBottom: 6,
                  }}
                >
                  {mode === 'login' ? 'Email tài khoản *' : 'Địa chỉ Email *'}
                </label>
                <div style={{ position: 'relative' }}>
                  <Mail
                    size={16}
                    style={{ position: 'absolute', left: 12, top: 12, color: 'var(--text-muted)' }}
                  />
                  <input
                    type="email"
                    required
                    placeholder="Nhập tài khoản..."
                    value={email}
                    onChange={(e) => {
                      setEmail(e.target.value);
                      if (errorMessage) setErrorMessage(null);
                      if (errorField === 'email') setErrorField(null);
                    }}
                    style={{
                      width: '100%',
                      padding: '10px 12px 10px 38px',
                      borderRadius: 10,
                      border: errorField === 'email' ? '1.5px solid #EF4444' : '1px solid var(--border)',
                      boxShadow: errorField === 'email' ? '0 0 0 3px rgba(239, 68, 68, 0.15)' : 'none',
                      background: 'var(--surface)',
                      color: 'var(--text)',
                      fontSize: '0.88rem',
                      outline: 'none',
                      transition: 'all 0.2s ease',
                    }}
                  />
                </div>
              </div>

              {/* Password Box */}
              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
                  <label style={{ fontSize: '0.82rem', fontWeight: 600, color: 'var(--text)' }}>
                    Mật khẩu *
                  </label>
                  {mode === 'login' && (
                    <span
                      onClick={() => setIsForgotPasswordOpen(true)}
                      style={{ fontSize: '0.75rem', color: 'var(--primary)', cursor: 'pointer', fontWeight: 600 }}
                    >
                      Quên mật khẩu?
                    </span>
                  )}
                </div>
                <div style={{ position: 'relative' }}>
                  <Lock
                    size={16}
                    style={{ position: 'absolute', left: 12, top: 12, color: 'var(--text-muted)' }}
                  />
                  <input
                    type={showPassword ? 'text' : 'password'}
                    required
                    placeholder={mode === 'login' ? 'Nhập mật khẩu...' : 'Tạo mật khẩu...'}
                    value={password}
                    onChange={(e) => {
                      setPassword(e.target.value);
                      if (errorMessage) setErrorMessage(null);
                      if (errorField === 'password') setErrorField(null);
                    }}
                    style={{
                      width: '100%',
                      padding: '10px 38px 10px 38px',
                      borderRadius: 10,
                      border: errorField === 'password' ? '1.5px solid #EF4444' : '1px solid var(--border)',
                      boxShadow: errorField === 'password' ? '0 0 0 3px rgba(239, 68, 68, 0.15)' : 'none',
                      background: 'var(--surface)',
                      color: 'var(--text)',
                      fontSize: '0.88rem',
                      outline: 'none',
                      transition: 'all 0.2s ease',
                    }}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    style={{
                      position: 'absolute',
                      right: 10,
                      top: 10,
                      background: 'none',
                      border: 'none',
                      color: 'var(--text-muted)',
                      cursor: 'pointer',
                      padding: 2,
                    }}
                  >
                    {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                  </button>
                </div>
              </div>

              {/* Confirm Password (Only on Register) */}
              {mode === 'register' && (
                <div>
                  <label
                    style={{
                      display: 'block',
                      fontSize: '0.82rem',
                      fontWeight: 600,
                      color: 'var(--text)',
                      marginBottom: 6,
                    }}
                  >
                    Xác nhận lại mật khẩu *
                  </label>
                  <div style={{ position: 'relative' }}>
                    <Lock
                      size={16}
                      style={{ position: 'absolute', left: 12, top: 12, color: 'var(--text-muted)' }}
                    />
                    <input
                      type={showPassword ? 'text' : 'password'}
                      required
                      placeholder="Nhập lại mật khẩu để xác nhận..."
                      value={confirmPassword}
                      onChange={(e) => {
                        setConfirmPassword(e.target.value);
                        if (errorMessage) setErrorMessage(null);
                        if (errorField === 'confirmPassword') setErrorField(null);
                      }}
                      style={{
                        width: '100%',
                        padding: '10px 12px 10px 38px',
                        borderRadius: 10,
                        border: errorField === 'confirmPassword' ? '1.5px solid #EF4444' : '1px solid var(--border)',
                        boxShadow: errorField === 'confirmPassword' ? '0 0 0 3px rgba(239, 68, 68, 0.15)' : 'none',
                        background: 'var(--surface)',
                        color: 'var(--text)',
                        fontSize: '0.88rem',
                        outline: 'none',
                        transition: 'all 0.2s ease',
                      }}
                    />
                  </div>
                </div>
              )}

              {/* Submit Button */}
              <button
                type="submit"
                disabled={isLoading || isSendingRegisterOtp}
                style={{
                  marginTop: 6,
                  padding: '12px 20px',
                  borderRadius: 10,
                  border: 'none',
                  background: 'var(--primary)',
                  color: '#FFFFFF',
                  fontSize: '0.92rem',
                  fontWeight: 700,
                  cursor: (isLoading || isSendingRegisterOtp) ? 'not-allowed' : 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: 8,
                  boxShadow: '0 4px 14px rgba(108, 92, 231, 0.4)',
                  transition: 'all 0.2s ease',
                  opacity: (isLoading || isSendingRegisterOtp) ? 0.75 : 1,
                }}
              >
                <span>
                  {isLoading || isSendingRegisterOtp
                    ? 'Đang xử lý...'
                    : mode === 'login'
                    ? 'Đăng nhập vào SmartTask'
                    : 'Tiếp tục xác thực OTP'}
                </span>
                {!isLoading && !isSendingRegisterOtp && <ArrowRight size={16} />}
              </button>

              {/* Divider */}
              <div style={{ display: 'flex', alignItems: 'center', margin: '4px 0', gap: 12 }}>
                <div style={{ flex: 1, height: 1, background: 'var(--border-light)' }} />
                <span style={{ fontSize: '0.74rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                  hoặc
                </span>
                <div style={{ flex: 1, height: 1, background: 'var(--border-light)' }} />
              </div>

              {/* Google Sign-In Button */}
              <button
                type="button"
                onClick={handleGoogleLogin}
                disabled={isLoading}
                style={{
                  padding: '11px 16px',
                  borderRadius: 10,
                  border: '1px solid var(--border)',
                  background: 'var(--surface)',
                  color: 'var(--text)',
                  fontSize: '0.88rem',
                  fontWeight: 600,
                  cursor: isLoading ? 'not-allowed' : 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: 10,
                  boxShadow: 'var(--shadow-sm, 0 1px 2px rgba(0,0,0,0.05))',
                  transition: 'all 0.2s ease',
                  opacity: isLoading ? 0.75 : 1,
                }}
              >
                <GoogleIcon />
                <span>Đăng nhập với Google</span>
              </button>
            </form>
          )}

            {/* Bottom Toggle Note */}
            <div style={{ textAlign: 'center', marginTop: 20, fontSize: '0.82rem', color: 'var(--text-3)' }}>
              {mode === 'login' ? (
                <>
                  Chưa có tài khoản SmartTask?{' '}
                  <span
                    onClick={() => switchMode('register')}
                    style={{ color: 'var(--primary)', fontWeight: 700, cursor: 'pointer' }}
                  >
                    Đăng ký miễn phí
                  </span>
                </>
              ) : (
                <>
                  Đã có tài khoản SmartTask?{' '}
                  <span
                    onClick={() => switchMode('login')}
                    style={{ color: 'var(--primary)', fontWeight: 700, cursor: 'pointer' }}
                  >
                    Đăng nhập ngay
                  </span>
                </>
              )}
            </div>
          </div>
        </div>
      </main>

      {/* Forgot Password OTP Modal */}
      <ForgotPasswordModal
        isOpen={isForgotPasswordOpen}
        onClose={() => setIsForgotPasswordOpen(false)}
        onSuccessReturnToLogin={() => {
          setIsForgotPasswordOpen(false);
          setMode('login');
        }}
      />

      {/* ── Footer ── */}
      <footer
        style={{
          textAlign: 'center',
          padding: '16px 20px',
          fontSize: '0.75rem',
          color: 'var(--text-muted)',
          borderTop: '1px solid var(--border-light)',
        }}
      >
        © 2026 SmartTask AI · Đồ án Chuyên ngành Quản lý Công việc Cá nhân
      </footer>
    </div>
  );
}
