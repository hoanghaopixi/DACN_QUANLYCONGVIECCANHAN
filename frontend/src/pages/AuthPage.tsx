import React, { useState } from 'react';
import {
  Lock, Mail, User, Eye, EyeOff,
  Sparkles, CheckCircle2, ArrowRight, Sun, Moon
} from 'lucide-react';
import { authService, AuthResponse } from '../services/authService';
import { validateLoginForm, validateRegisterForm, formatAuthErrorMessage } from '../utils/authValidation';

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
  const [errorField, setErrorField] = useState<'email' | 'password' | 'fullName' | 'confirmPassword' | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const switchMode = (newMode: 'login' | 'register') => {
    setMode(newMode);
    setErrorMessage(null);
    setErrorField(null);
    setSuccessMessage(null);
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
    } else {
      const validation = validateRegisterForm(fullName, email, password, confirmPassword);
      if (!validation.isValid) {
        setErrorMessage(validation.message || 'Thông tin đăng ký không hợp lệ.');
        setErrorField(validation.field || null);
        return;
      }
    }

    setIsLoading(true);

    try {
      if (mode === 'login') {
        const response = await authService.login({
          email: email.trim(),
          password: password,
        });

        setSuccessMessage(`Đăng nhập thành công! Chào mừng ${response.fullName}`);
        setTimeout(() => {
          onLoginSuccess(response);
        }, 500);
      } else {
        const response = await authService.register({
          fullName: fullName.trim(),
          email: email.trim(),
          password: password,
        });

        setSuccessMessage('Đăng ký tài khoản thành công! Đang chuyển hướng...');
        setTimeout(() => {
          onLoginSuccess(response);
        }, 700);
      }
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
                    <span style={{ fontSize: '0.75rem', color: 'var(--primary)', cursor: 'pointer' }}>
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
                disabled={isLoading}
                style={{
                  marginTop: 6,
                  padding: '12px 20px',
                  borderRadius: 10,
                  border: 'none',
                  background: 'var(--primary)',
                  color: '#FFFFFF',
                  fontSize: '0.92rem',
                  fontWeight: 700,
                  cursor: isLoading ? 'not-allowed' : 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: 8,
                  boxShadow: '0 4px 14px rgba(108, 92, 231, 0.4)',
                  transition: 'all 0.2s ease',
                  opacity: isLoading ? 0.75 : 1,
                }}
              >
                <span>{isLoading ? 'Đang xác thực...' : mode === 'login' ? 'Đăng nhập vào SmartTask' : 'Đăng ký tài khoản mới'}</span>
                {!isLoading && <ArrowRight size={16} />}
              </button>
            </form>

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
