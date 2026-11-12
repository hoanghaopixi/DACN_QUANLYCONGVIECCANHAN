import { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { X, Lock, Mail, User, Eye, EyeOff, Sparkles, KeyRound, RefreshCw, CheckCircle2, ArrowLeft } from 'lucide-react';
import { authService } from '../services/authService';
import { validateLoginForm, validateRegisterForm, formatAuthErrorMessage } from '../utils/authValidation';
import ForgotPasswordModal from './ForgotPasswordModal';
import { openGoogleLoginPopup } from '../services/googleAuth';
import { useModalAnimation } from '../hooks/useModalAnimation';

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

interface AuthModalProps {
  isOpen: boolean;
  onClose: () => void;
  onLoginSuccess: (user: { email: string; fullName: string; role: string; token: string }) => void;
}

export default function AuthModal({ isOpen, onClose, onLoginSuccess }: AuthModalProps) {
  const { shouldRender, isClosing } = useModalAnimation(isOpen, 200);
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

  // Trạng thái xác thực OTP đăng ký tài khoản
  const [isRegisterOtpStep, setIsRegisterOtpStep] = useState(false);
  const [registerOtp, setRegisterOtp] = useState('');
  const [resendCountdown, setResendCountdown] = useState(0);
  const [isSendingRegisterOtp, setIsSendingRegisterOtp] = useState(false);

  useEffect(() => {
    if (!shouldRender) return;
    const prevOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = prevOverflow;
    };
  }, [shouldRender]);

  // Bộ đếm lùi thời gian gửi lại OTP đăng ký
  useEffect(() => {
    if (resendCountdown <= 0) return;
    const interval = setInterval(() => {
      setResendCountdown((prev) => prev - 1);
    }, 1000);
    return () => clearInterval(interval);
  }, [resendCountdown]);

  if (!shouldRender || typeof document === 'undefined') return null;

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

    // Kiểm tra định dạng & regex trước khi gửi request
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
          onClose();
        }, 600);
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
      // Chế độ đăng ký tài khoản (2 bước: Điền form -> Xác thực OTP)
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
          setSuccessMessage(res.message || `Mã OTP đã được gửi đến ${email.trim()}. Vui lòng kiểm tra hộp thư!`);
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
        // Bước 2: Nhập OTP và hoàn tất đăng ký
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

          setSuccessMessage('Đăng ký tài khoản thành công! Tự động đăng nhập...');
          setTimeout(() => {
            onLoginSuccess(response);
            onClose();
          }, 800);
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

  const handleGoogleLogin = async () => {
    setIsLoading(true);
    setErrorMessage(null);
    try {
      const response = await openGoogleLoginPopup();
      setSuccessMessage(`Đăng nhập Google thành công! Chào mừng ${response.fullName}`);
      setTimeout(() => {
        onLoginSuccess(response);
        onClose();
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

  return createPortal(
    <div
      style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: 'rgba(15, 23, 42, 0.55)',
        backdropFilter: 'blur(5px)',
        zIndex: 10000,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: 20,
        animation: isClosing ? 'modalBackdropFadeOut 0.2s ease-in forwards' : 'modalBackdropFadeIn 0.2s ease-out',
        pointerEvents: isClosing ? 'none' : 'auto',
      }}
      onClick={onClose}
    >
      <div
        style={{
          background: 'var(--surface)',
          borderRadius: 18,
          width: '100%',
          maxWidth: 440,
          boxShadow: 'var(--shadow-lg)',
          overflow: 'hidden',
          display: 'flex',
          flexDirection: 'column',
          border: '1px solid var(--border)',
          animation: isClosing
            ? 'modalSlideDown 0.2s cubic-bezier(0.4, 0, 1, 1) forwards'
            : 'modalSlideUp 0.25s cubic-bezier(0.16, 1, 0.3, 1)',
        }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div
          style={{
            padding: '20px 24px 16px',
            borderBottom: '1px solid var(--border-light)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <div
              style={{
                width: 36,
                height: 36,
                borderRadius: 10,
                background: isRegisterOtpStep
                  ? 'linear-gradient(135deg, #10B981, #059669)'
                  : 'linear-gradient(135deg, #6C5CE7, #A29BFE)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#fff',
              }}
            >
              {isRegisterOtpStep ? <KeyRound size={18} /> : <Sparkles size={18} />}
            </div>
            <div>
              <h2 style={{ fontSize: '1.15rem', fontWeight: 700, color: 'var(--text)', margin: 0 }}>
                {isRegisterOtpStep
                  ? 'Xác thực mã OTP'
                  : mode === 'login'
                  ? 'Đăng nhập SmartTask'
                  : 'Tạo tài khoản mới'}
              </h2>
              <p style={{ fontSize: '0.76rem', color: 'var(--text-3)', margin: '2px 0 0' }}>
                {isRegisterOtpStep
                  ? 'Nhập mã gồm 6 chữ số đã gửi về email'
                  : 'Hệ thống Quản lý và Tự động Lập lịch Công việc'}
              </p>
            </div>
          </div>

          <button
            onClick={onClose}
            style={{
              width: 32,
              height: 32,
              borderRadius: 8,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: 'var(--text-muted)',
              cursor: 'pointer',
            }}
          >
            <X size={18} />
          </button>
        </div>

        {/* Tab switcher: Đăng nhập / Đăng ký (chỉ hiện khi chưa vào bước OTP) */}
        {!isRegisterOtpStep ? (
          <div
            style={{
              display: 'flex',
              borderBottom: '1px solid var(--border-light)',
              background: 'var(--border-light)',
              padding: 4,
              margin: '16px 24px 0',
              borderRadius: 10,
            }}
          >
            <button
              type="button"
              onClick={() => switchMode('login')}
              style={{
                flex: 1,
                padding: '8px 12px',
                borderRadius: 8,
                fontSize: '0.85rem',
                fontWeight: 600,
                color: mode === 'login' ? 'var(--primary)' : 'var(--text-3)',
                background: mode === 'login' ? 'var(--surface)' : 'transparent',
                boxShadow: mode === 'login' ? '0 1px 3px rgba(0,0,0,0.08)' : 'none',
                transition: 'all 0.2s',
              }}
            >
              Đăng nhập
            </button>
            <button
              type="button"
              onClick={() => switchMode('register')}
              style={{
                flex: 1,
                padding: '8px 12px',
                borderRadius: 8,
                fontSize: '0.85rem',
                fontWeight: 600,
                color: mode === 'register' ? 'var(--primary)' : 'var(--text-3)',
                background: mode === 'register' ? 'var(--surface)' : 'transparent',
                boxShadow: mode === 'register' ? '0 1px 3px rgba(0,0,0,0.08)' : 'none',
                transition: 'all 0.2s',
              }}
            >
              Đăng ký
            </button>
          </div>
        ) : (
          <div style={{ margin: '14px 24px 0' }}>
            <button
              type="button"
              onClick={() => {
                setIsRegisterOtpStep(false);
                setRegisterOtp('');
                setErrorMessage(null);
              }}
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: 6,
                background: 'none',
                border: 'none',
                color: 'var(--primary)',
                fontSize: '0.82rem',
                fontWeight: 600,
                cursor: 'pointer',
                padding: 0,
              }}
            >
              <ArrowLeft size={14} />
              <span>Quay lại chỉnh sửa thông tin</span>
            </button>
          </div>
        )}

        {/* Form Body */}
        <form onSubmit={handleSubmit} style={{ padding: '16px 24px 24px', display: 'flex', flexDirection: 'column', gap: 14 }}>
          {isRegisterOtpStep ? (
            /* Bước OTP xác thực đăng ký */
            <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
              <div
                style={{
                  padding: '12px 14px',
                  borderRadius: 10,
                  background: 'rgba(16, 185, 129, 0.08)',
                  border: '1px solid rgba(16, 185, 129, 0.25)',
                  color: 'var(--text-2)',
                  fontSize: '0.82rem',
                  lineHeight: 1.5,
                }}
              >
                Mã xác thực đã được gửi đến email <strong>{email}</strong>. Vui lòng nhập mã để hoàn tất đăng ký:
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-2)', marginBottom: 6 }}>
                  Mã OTP (6 chữ số) *
                </label>
                <div style={{ position: 'relative' }}>
                  <KeyRound size={16} style={{ position: 'absolute', left: 12, top: 14, color: 'var(--text-muted)' }} />
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
                      padding: '10px 12px 10px 38px',
                      borderRadius: 10,
                      border: errorField === 'otp' ? '1.5px solid #EF4444' : '1px solid var(--border)',
                      boxShadow: errorField === 'otp' ? '0 0 0 3px rgba(239, 68, 68, 0.15)' : 'none',
                      background: 'var(--surface)',
                      color: 'var(--text)',
                      fontSize: '1.25rem',
                      fontWeight: 700,
                      letterSpacing: '8px',
                      textAlign: 'center',
                      outline: 'none',
                    }}
                  />
                </div>
                <p style={{ fontSize: '0.74rem', color: 'var(--text-3)', margin: '6px 0 0', textAlign: 'center' }}>
                  Mã xác thực có hiệu lực trong vòng <strong>5 phút</strong>.
                </p>
              </div>

              {/* Resend button */}
              <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: 6, fontSize: '0.8rem' }}>
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
                    <RefreshCw size={12} className={isSendingRegisterOtp ? 'spin' : ''} />
                    <span>{isSendingRegisterOtp ? 'Đang gửi...' : 'Gửi lại mã'}</span>
                  </button>
                )}
              </div>
            </div>
          ) : (
            <>
              {/* Full Name for register */}
              {mode === 'register' && (
                <div>
                  <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-2)', marginBottom: 6 }}>
                    Họ và tên *
                  </label>
                  <div style={{ position: 'relative' }}>
                    <User size={16} style={{ position: 'absolute', left: 12, top: 12, color: 'var(--text-muted)' }} />
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
                        borderRadius: 8,
                        border: errorField === 'fullName' ? '1.5px solid #EF4444' : '1px solid var(--border)',
                        boxShadow: errorField === 'fullName' ? '0 0 0 3px rgba(239, 68, 68, 0.15)' : 'none',
                        background: 'var(--surface)',
                        color: 'var(--text)',
                        fontSize: '0.88rem',
                        outline: 'none',
                      }}
                    />
                  </div>
                </div>
              )}

              {/* Email */}
              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-2)', marginBottom: 6 }}>
                  Địa chỉ Email *
                </label>
                <div style={{ position: 'relative' }}>
                  <Mail size={16} style={{ position: 'absolute', left: 12, top: 12, color: 'var(--text-muted)' }} />
                  <input
                    type="email"
                    required
                    placeholder="name@example.com"
                    value={email}
                    onChange={(e) => {
                      setEmail(e.target.value);
                      if (errorMessage) setErrorMessage(null);
                      if (errorField === 'email') setErrorField(null);
                    }}
                    style={{
                      width: '100%',
                      padding: '10px 12px 10px 38px',
                      borderRadius: 8,
                      border: errorField === 'email' ? '1.5px solid #EF4444' : '1px solid var(--border)',
                      boxShadow: errorField === 'email' ? '0 0 0 3px rgba(239, 68, 68, 0.15)' : 'none',
                      background: 'var(--surface)',
                      color: 'var(--text)',
                      fontSize: '0.88rem',
                      outline: 'none',
                    }}
                  />
                </div>
              </div>

              {/* Password */}
              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-2)' }}>
                    Mật khẩu *
                  </label>
                  {mode === 'login' && (
                    <span
                      onClick={() => setIsForgotPasswordOpen(true)}
                      style={{ fontSize: '0.74rem', color: 'var(--primary)', cursor: 'pointer', fontWeight: 600 }}
                    >
                      Quên mật khẩu?
                    </span>
                  )}
                </div>
                <div style={{ position: 'relative' }}>
                  <Lock size={16} style={{ position: 'absolute', left: 12, top: 12, color: 'var(--text-muted)' }} />
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
                      borderRadius: 8,
                      border: errorField === 'password' ? '1.5px solid #EF4444' : '1px solid var(--border)',
                      boxShadow: errorField === 'password' ? '0 0 0 3px rgba(239, 68, 68, 0.15)' : 'none',
                      background: 'var(--surface)',
                      color: 'var(--text)',
                      fontSize: '0.88rem',
                      outline: 'none',
                    }}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    style={{
                      position: 'absolute',
                      right: 12,
                      top: 10,
                      color: 'var(--text-muted)',
                      cursor: 'pointer',
                      background: 'none',
                      border: 'none',
                    }}
                  >
                    {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                  </button>
                </div>
              </div>

              {/* Confirm Password for register */}
              {mode === 'register' && (
                <div>
                  <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-2)', marginBottom: 6 }}>
                    Xác nhận Mật khẩu *
                  </label>
                  <div style={{ position: 'relative' }}>
                    <Lock size={16} style={{ position: 'absolute', left: 12, top: 12, color: 'var(--text-muted)' }} />
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
                        borderRadius: 8,
                        border: errorField === 'confirmPassword' ? '1.5px solid #EF4444' : '1px solid var(--border)',
                        boxShadow: errorField === 'confirmPassword' ? '0 0 0 3px rgba(239, 68, 68, 0.15)' : 'none',
                        background: 'var(--surface)',
                        color: 'var(--text)',
                        fontSize: '0.88rem',
                        outline: 'none',
                      }}
                    />
                  </div>
                </div>
              )}
            </>
          )}

          {/* Error / Success alert */}
          {errorMessage && (
            <div
              style={{
                padding: '9px 12px',
                borderRadius: 8,
                background: '#FEF2F2',
                border: '1px solid #FCA5A5',
                color: '#DC2626',
                fontSize: '0.78rem',
              }}
            >
              {errorMessage}
            </div>
          )}

          {successMessage && (
            <div
              style={{
                padding: '9px 12px',
                borderRadius: 8,
                background: '#ECFDF5',
                border: '1px solid #A7F3D0',
                color: '#059669',
                fontSize: '0.78rem',
                fontWeight: 600,
              }}
            >
              {successMessage}
            </div>
          )}

          {/* Submit Button */}
          <button
            type="submit"
            disabled={isLoading || isSendingRegisterOtp || (isRegisterOtpStep && registerOtp.length < 6)}
            style={{
              marginTop: 6,
              padding: '11px 16px',
              borderRadius: 10,
              background: isRegisterOtpStep ? '#059669' : 'var(--primary)',
              color: '#fff',
              fontSize: '0.9rem',
              fontWeight: 600,
              boxShadow: isRegisterOtpStep
                ? '0 2px 8px rgba(5, 150, 105, 0.3)'
                : '0 2px 8px rgba(108, 92, 231, 0.3)',
              cursor: (isLoading || isSendingRegisterOtp || (isRegisterOtpStep && registerOtp.length < 6))
                ? 'not-allowed'
                : 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: 8,
              transition: 'all 0.2s',
              opacity: (isLoading || isSendingRegisterOtp || (isRegisterOtpStep && registerOtp.length < 6)) ? 0.7 : 1,
            }}
          >
            {isLoading || isSendingRegisterOtp ? (
              <span>{isSendingRegisterOtp ? 'Đang gửi mã OTP...' : 'Đang xử lý...'}</span>
            ) : isRegisterOtpStep ? (
              <>
                <span>Xác nhận & Hoàn tất Đăng ký</span>
                <CheckCircle2 size={16} />
              </>
            ) : mode === 'login' ? (
              <span>Đăng nhập ngay</span>
            ) : (
              <span>Tiếp tục xác thực OTP</span>
            )}
          </button>

          {/* Divider & Google Login (chỉ hiện khi chưa vào bước OTP) */}
          {!isRegisterOtpStep && (
            <>
              <div style={{ display: 'flex', alignItems: 'center', margin: '2px 0', gap: 10 }}>
                <div style={{ flex: 1, height: 1, background: 'var(--border-light)' }} />
                <span style={{ fontSize: '0.72rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>
                  hoặc
                </span>
                <div style={{ flex: 1, height: 1, background: 'var(--border-light)' }} />
              </div>

              {/* Google Sign-in button */}
              <button
                type="button"
                onClick={handleGoogleLogin}
                disabled={isLoading}
                style={{
                  padding: '10px 16px',
                  borderRadius: 10,
                  border: '1px solid var(--border)',
                  background: 'var(--surface)',
                  color: 'var(--text)',
                  fontSize: '0.86rem',
                  fontWeight: 600,
                  cursor: isLoading ? 'not-allowed' : 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: 8,
                  transition: 'all 0.2s',
                  opacity: isLoading ? 0.7 : 1,
                }}
              >
                <GoogleIcon />
                <span>Đăng nhập với Google</span>
              </button>
            </>
          )}
        </form>
      </div>

      {/* Forgot Password Modal */}
      <ForgotPasswordModal
        isOpen={isForgotPasswordOpen}
        onClose={() => setIsForgotPasswordOpen(false)}
        onSuccessReturnToLogin={() => {
          setIsForgotPasswordOpen(false);
          setMode('login');
        }}
      />
    </div>,
    document.body
  );
}
