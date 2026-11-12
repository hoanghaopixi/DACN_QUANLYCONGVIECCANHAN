import React, { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { X, Mail, KeyRound, Lock, ArrowRight, CheckCircle2, AlertCircle, RefreshCw } from 'lucide-react';
import { authService } from '../services/authService';
import { formatApiErrorMessage } from '../utils/apiErrorHandler';
import { useModalAnimation } from '../hooks/useModalAnimation';

interface ForgotPasswordModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccessReturnToLogin?: () => void;
}

export default function ForgotPasswordModal({
  isOpen,
  onClose,
  onSuccessReturnToLogin,
}: ForgotPasswordModalProps) {
  const { shouldRender, isClosing } = useModalAnimation(isOpen, 200);

  // Step 1: 'email', Step 2: 'otp_and_reset', Step 3: 'success'
  const [step, setStep] = useState<'email' | 'otp_and_reset' | 'success'>('email');
  const [email, setEmail] = useState('');
  const [otp, setOtp] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const [countdown, setCountdown] = useState(0);

  useEffect(() => {
    if (!shouldRender) {
      setStep('email');
      setEmail('');
      setOtp('');
      setNewPassword('');
      setConfirmPassword('');
      setError('');
      setCountdown(0);
      return;
    }
    const prevOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = prevOverflow;
    };
  }, [shouldRender]);

  useEffect(() => {
    if (countdown <= 0) return;
    const timer = setInterval(() => {
      setCountdown((prev) => prev - 1);
    }, 1000);
    return () => clearInterval(timer);
  }, [countdown]);

  if (!isOpen || typeof document === 'undefined') return null;

  const handleSendOtp = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    const trimmedEmail = email.trim();
    if (!trimmedEmail) {
      setError('Vui lòng nhập email đăng ký tài khoản');
      return;
    }
    if (!/^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(trimmedEmail)) {
      setError('Định dạng email không hợp lệ (ví dụ: yourname@gmail.com)');
      return;
    }

    try {
      setIsLoading(true);
      const res = await authService.forgotPassword(trimmedEmail);
      if (res.success) {
        setCountdown(60);
        setStep('otp_and_reset');
      } else {
        setError(res.message || 'Không thể gửi mã OTP. Vui lòng thử lại sau.');
      }
    } catch (err: any) {
      const msg = formatApiErrorMessage(err, 'Gửi OTP thất bại. Vui lòng kiểm tra lại email.');
      setError(msg);
    } finally {
      setIsLoading(false);
    }
  };

  const handleResendOtp = async () => {
    if (countdown > 0 || isLoading) return;
    setError('');
    try {
      setIsLoading(true);
      const res = await authService.forgotPassword(email.trim());
      if (res.success) {
        setCountdown(60);
      }
    } catch (err: any) {
      const msg = formatApiErrorMessage(err, 'Không thể gửi lại mã OTP.');
      setError(msg);
    } finally {
      setIsLoading(false);
    }
  };

  const handleResetPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    const trimmedOtp = otp.trim();
    if (!trimmedOtp || trimmedOtp.length < 6) {
      setError('Vui lòng nhập đủ 6 chữ số mã OTP đã gửi qua Gmail');
      return;
    }
    if (!newPassword || newPassword.length < 8) {
      setError('Mật khẩu mới phải có tối thiểu 8 ký tự');
      return;
    }
    if (newPassword !== confirmPassword) {
      setError('Mật khẩu nhập lại không trùng khớp');
      return;
    }

    try {
      setIsLoading(true);
      const res = await authService.resetPassword({
        email: email.trim(),
        otp: trimmedOtp,
        newPassword,
      });
      if (res.success) {
        setStep('success');
      } else {
        setError(res.message || 'Đặt lại mật khẩu không thành công.');
      }
    } catch (err: any) {
      const msg = formatApiErrorMessage(err, 'Mã OTP không đúng hoặc đã hết hạn.');
      setError(msg);
    } finally {
      setIsLoading(false);
    }
  };

  if (!shouldRender || typeof document === 'undefined') return null;

  return createPortal(
    <div
      style={{
        position: 'fixed',
        inset: 0,
        zIndex: 9999,
        background: 'rgba(15, 23, 42, 0.65)',
        backdropFilter: 'blur(8px)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: 16,
        animation: isClosing ? 'modalBackdropFadeOut 0.2s ease-in forwards' : 'modalBackdropFadeIn 0.2s ease-out',
        pointerEvents: isClosing ? 'none' : 'auto',
      }}
      onClick={onClose}
    >
      <div
        style={{
          background: 'var(--surface)',
          borderRadius: 20,
          width: '100%',
          maxWidth: 440,
          boxShadow: 'var(--shadow-xl, 0 25px 50px -12px rgba(0, 0, 0, 0.25))',
          overflow: 'hidden',
          display: 'flex',
          flexDirection: 'column',
          border: '1px solid var(--border)',
          position: 'relative',
          animation: isClosing
            ? 'modalSlideDown 0.2s cubic-bezier(0.4, 0, 1, 1) forwards'
            : 'modalSlideUp 0.22s cubic-bezier(0.16, 1, 0.3, 1)',
        }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div
          style={{
            padding: '20px 24px',
            borderBottom: '1px solid var(--border-light)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <div
              style={{
                width: 38,
                height: 38,
                borderRadius: 10,
                background: 'var(--primary-bg, #EEF2FF)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: 'var(--primary, #4F46E5)',
              }}
            >
              <KeyRound size={20} />
            </div>
            <div>
              <h3 style={{ fontSize: '1.05rem', fontWeight: 700, color: 'var(--text)', margin: 0 }}>
                {step === 'success' ? 'Hoàn tất' : 'Khôi phục mật khẩu'}
              </h3>
              <p style={{ fontSize: '0.76rem', color: 'var(--text-3)', margin: '2px 0 0' }}>
                Xác thực mã OTP gửi về hòm thư Gmail của bạn
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
              background: 'transparent',
              border: 'none',
              color: 'var(--text-muted)',
              cursor: 'pointer',
            }}
          >
            <X size={18} />
          </button>
        </div>

        {/* Content Body */}
        <div style={{ padding: '24px' }}>
          {error && (
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 8,
                padding: '10px 14px',
                borderRadius: 8,
                background: '#FEF2F2',
                border: '1px solid #FECACA',
                color: '#DC2626',
                fontSize: '0.82rem',
                fontWeight: 600,
                marginBottom: 16,
              }}
            >
              <AlertCircle size={16} />
              <span>{error}</span>
            </div>
          )}

          {/* STEP 1: Enter email */}
          {step === 'email' && (
            <form onSubmit={handleSendOtp} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
              <div>
                <label
                  style={{
                    display: 'block',
                    fontSize: '0.82rem',
                    fontWeight: 600,
                    color: 'var(--text-2)',
                    marginBottom: 6,
                  }}
                >
                  Email tài khoản
                </label>
                <div style={{ position: 'relative' }}>
                  <Mail
                    size={16}
                    style={{
                      position: 'absolute',
                      left: 12,
                      top: '50%',
                      transform: 'translateY(-50%)',
                      color: 'var(--text-muted)',
                    }}
                  />
                  <input
                    type="email"
                    required
                    placeholder="name@example.com"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    style={{
                      width: '100%',
                      padding: '10px 12px 10px 38px',
                      borderRadius: 10,
                      border: '1px solid var(--border)',
                      background: 'var(--surface)',
                      color: 'var(--text)',
                      fontSize: '0.88rem',
                      outline: 'none',
                    }}
                  />
                </div>
                <p style={{ fontSize: '0.74rem', color: 'var(--text-3)', margin: '6px 0 0' }}>
                  Mã xác thực gồm 6 chữ số sẽ được gửi đến email này (hiệu lực trong 5 phút).
                </p>
              </div>

              <button
                type="submit"
                disabled={isLoading}
                style={{
                  width: '100%',
                  padding: '11px',
                  borderRadius: 10,
                  background: 'var(--primary, #4F46E5)',
                  color: '#fff',
                  fontSize: '0.88rem',
                  fontWeight: 600,
                  border: 'none',
                  cursor: isLoading ? 'not-allowed' : 'pointer',
                  opacity: isLoading ? 0.7 : 1,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: 8,
                  boxShadow: '0 4px 12px rgba(79, 70, 229, 0.25)',
                }}
              >
                {isLoading ? 'Đang gửi mã...' : 'Gửi mã OTP qua Gmail'}
                <ArrowRight size={16} />
              </button>
            </form>
          )}

          {/* STEP 2: Enter OTP & New Password */}
          {step === 'otp_and_reset' && (
            <form onSubmit={handleResetPassword} style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
              <div
                style={{
                  padding: '10px 12px',
                  borderRadius: 8,
                  background: 'var(--primary-bg, #EEF2FF)',
                  fontSize: '0.78rem',
                  color: 'var(--text-2)',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                }}
              >
                <span>Mã đã gửi tới: <strong>{email}</strong></span>
                <button
                  type="button"
                  onClick={handleResendOtp}
                  disabled={countdown > 0 || isLoading}
                  style={{
                    background: 'none',
                    border: 'none',
                    color: countdown > 0 ? 'var(--text-muted)' : 'var(--primary, #4F46E5)',
                    fontSize: '0.76rem',
                    fontWeight: 600,
                    cursor: countdown > 0 ? 'default' : 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    gap: 4,
                  }}
                >
                  <RefreshCw size={12} />
                  {countdown > 0 ? `Gửi lại (${countdown}s)` : 'Gửi lại mã'}
                </button>
              </div>

              <div>
                <label
                  style={{
                    display: 'block',
                    fontSize: '0.8rem',
                    fontWeight: 600,
                    color: 'var(--text-2)',
                    marginBottom: 6,
                  }}
                >
                  Mã OTP 6 chữ số
                </label>
                <input
                  type="text"
                  maxLength={6}
                  required
                  placeholder="Nhập 6 số..."
                  value={otp}
                  onChange={(e) => setOtp(e.target.value.replace(/\D/g, ''))}
                  style={{
                    width: '100%',
                    padding: '10px 12px',
                    borderRadius: 10,
                    border: '1px solid var(--border)',
                    background: 'var(--surface)',
                    color: 'var(--text)',
                    fontSize: '1.15rem',
                    fontWeight: 700,
                    letterSpacing: '4px',
                    textAlign: 'center',
                    outline: 'none',
                  }}
                />
              </div>

              <div>
                <label
                  style={{
                    display: 'block',
                    fontSize: '0.8rem',
                    fontWeight: 600,
                    color: 'var(--text-2)',
                    marginBottom: 6,
                  }}
                >
                  Mật khẩu mới
                </label>
                <div style={{ position: 'relative' }}>
                  <Lock
                    size={16}
                    style={{
                      position: 'absolute',
                      left: 12,
                      top: '50%',
                      transform: 'translateY(-50%)',
                      color: 'var(--text-muted)',
                    }}
                  />
                  <input
                    type="password"
                    required
                    placeholder="Tối thiểu 8 ký tự..."
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    style={{
                      width: '100%',
                      padding: '10px 12px 10px 38px',
                      borderRadius: 10,
                      border: '1px solid var(--border)',
                      background: 'var(--surface)',
                      color: 'var(--text)',
                      fontSize: '0.88rem',
                      outline: 'none',
                    }}
                  />
                </div>
              </div>

              <div>
                <label
                  style={{
                    display: 'block',
                    fontSize: '0.8rem',
                    fontWeight: 600,
                    color: 'var(--text-2)',
                    marginBottom: 6,
                  }}
                >
                  Xác nhận lại mật khẩu mới
                </label>
                <div style={{ position: 'relative' }}>
                  <Lock
                    size={16}
                    style={{
                      position: 'absolute',
                      left: 12,
                      top: '50%',
                      transform: 'translateY(-50%)',
                      color: 'var(--text-muted)',
                    }}
                  />
                  <input
                    type="password"
                    required
                    placeholder="Nhập lại mật khẩu mới..."
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    style={{
                      width: '100%',
                      padding: '10px 12px 10px 38px',
                      borderRadius: 10,
                      border: '1px solid var(--border)',
                      background: 'var(--surface)',
                      color: 'var(--text)',
                      fontSize: '0.88rem',
                      outline: 'none',
                    }}
                  />
                </div>
              </div>

              <div style={{ display: 'flex', gap: 10, marginTop: 4 }}>
                <button
                  type="button"
                  onClick={() => setStep('email')}
                  style={{
                    flex: 1,
                    padding: '10px',
                    borderRadius: 10,
                    background: 'transparent',
                    border: '1px solid var(--border)',
                    color: 'var(--text-2)',
                    fontSize: '0.84rem',
                    fontWeight: 600,
                    cursor: 'pointer',
                  }}
                >
                  Quay lại
                </button>
                <button
                  type="submit"
                  disabled={isLoading}
                  style={{
                    flex: 2,
                    padding: '10px',
                    borderRadius: 10,
                    background: 'var(--primary, #4F46E5)',
                    color: '#fff',
                    fontSize: '0.84rem',
                    fontWeight: 600,
                    border: 'none',
                    cursor: isLoading ? 'not-allowed' : 'pointer',
                    opacity: isLoading ? 0.7 : 1,
                    boxShadow: '0 4px 12px rgba(79, 70, 229, 0.25)',
                  }}
                >
                  {isLoading ? 'Đang cập nhật...' : 'Đặt lại mật khẩu'}
                </button>
              </div>
            </form>
          )}

          {/* STEP 3: Success */}
          {step === 'success' && (
            <div style={{ textAlign: 'center', padding: '12px 0' }}>
              <div
                style={{
                  width: 52,
                  height: 52,
                  borderRadius: '50%',
                  background: '#ECFDF5',
                  color: '#10B981',
                  display: 'inline-flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  marginBottom: 14,
                }}
              >
                <CheckCircle2 size={30} />
              </div>
              <h4 style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--text)', margin: '0 0 6px' }}>
                Đổi mật khẩu thành công!
              </h4>
              <p style={{ fontSize: '0.84rem', color: 'var(--text-3)', margin: '0 0 20px', lineHeight: 1.5 }}>
                Mật khẩu mới của bạn đã được cập nhật an toàn. Bây giờ bạn có thể đăng nhập bằng mật khẩu mới.
              </p>
              <button
                type="button"
                onClick={() => {
                  onClose();
                  if (onSuccessReturnToLogin) {
                    onSuccessReturnToLogin();
                  }
                }}
                style={{
                  width: '100%',
                  padding: '11px',
                  borderRadius: 10,
                  background: 'var(--primary, #4F46E5)',
                  color: '#fff',
                  fontSize: '0.88rem',
                  fontWeight: 600,
                  border: 'none',
                  cursor: 'pointer',
                  boxShadow: '0 4px 12px rgba(79, 70, 229, 0.25)',
                }}
              >
                Đăng nhập ngay
              </button>
            </div>
          )}
        </div>
      </div>
    </div>,
    document.body
  );
}
