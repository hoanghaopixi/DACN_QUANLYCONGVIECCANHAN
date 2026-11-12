import React, { useState, useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import { Lock, Eye, EyeOff, X, CheckCircle2, Loader2, KeyRound } from 'lucide-react';
import { authService } from '../services/authService';
import { formatAuthErrorMessage, validatePassword } from '../utils/authValidation';
import { useModalAnimation } from '../hooks/useModalAnimation';

interface PrivacyPasswordModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export default function PrivacyPasswordModal({
  isOpen,
  onClose,
  onSuccess,
}: PrivacyPasswordModalProps) {
  const { shouldRender, isClosing } = useModalAnimation(isOpen, 200);
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (isOpen) {
      setPassword('');
      setError(null);
      setIsLoading(false);
      setShowPassword(false);
      const timer = setTimeout(() => {
        inputRef.current?.focus();
      }, 80);
      return () => clearTimeout(timer);
    }
  }, [isOpen]);

  useEffect(() => {
    if (!shouldRender) return;
    const prevOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = prevOverflow;
    };
  }, [shouldRender]);

  if (!shouldRender || typeof document === 'undefined') return null;

  const handleSubmit = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    setError(null);

    const validation = validatePassword(password);
    if (!validation.isValid) {
      setError(validation.message || 'Vui lòng nhập mật khẩu chính xác.');
      inputRef.current?.focus();
      return;
    }

    try {
      setIsLoading(true);
      await authService.verifyPassword(password);
      setIsLoading(false);
      onSuccess();
      onClose();
    } catch (err: any) {
      setIsLoading(false);
      const errorMsg = formatAuthErrorMessage(err);
      setError(errorMsg);
      inputRef.current?.focus();
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Escape') {
      onClose();
    }
  };

  return createPortal(
    <div
      className="privacy-modal-overlay"
      onClick={onClose}
      onKeyDown={handleKeyDown}
      style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: 'rgba(15, 23, 42, 0.65)',
        backdropFilter: 'blur(5px)',
        zIndex: 9999,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '16px',
        animation: isClosing ? 'privacyModalFadeOut 0.2s ease-in forwards' : 'privacyModalFadeIn 0.2s ease-out',
        pointerEvents: isClosing ? 'none' : 'auto',
      }}
    >
      <div
        className="privacy-modal-container"
        onClick={(e) => e.stopPropagation()}
        style={{
          width: '100%',
          maxWidth: '430px',
          backgroundColor: 'var(--surface)',
          borderRadius: '18px',
          border: '1px solid var(--border)',
          boxShadow: '0 20px 45px -10px rgba(0, 0, 0, 0.35)',
          overflow: 'hidden',
          display: 'flex',
          flexDirection: 'column',
          animation: isClosing
            ? 'privacyModalScaleDown 0.2s cubic-bezier(0.4, 0, 1, 1) forwards'
            : 'privacyModalScaleUp 0.22s cubic-bezier(0.16, 1.3, 0.3, 1)',
        }}
      >
        {/* Header */}
        <div
          style={{
            padding: '20px 22px 16px',
            borderBottom: '1px solid var(--border)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            background: 'linear-gradient(135deg, rgba(108, 92, 231, 0.08) 0%, rgba(239, 68, 68, 0.05) 100%)',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <div
              style={{
                width: 42,
                height: 42,
                borderRadius: 12,
                backgroundColor: 'rgba(239, 68, 68, 0.12)',
                color: '#EF4444',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                boxShadow: '0 2px 8px rgba(239, 68, 68, 0.2)',
              }}
            >
              <Lock size={22} strokeWidth={2.4} />
            </div>
            <div>
              <h3
                style={{
                  margin: 0,
                  fontSize: '1.08rem',
                  fontWeight: 700,
                  color: 'var(--text)',
                  display: 'flex',
                  alignItems: 'center',
                  gap: 6,
                }}
              >
                Mở khóa lịch trình riêng tư
              </h3>
              <p style={{ margin: '3px 0 0', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                Bảo vệ thông tin lịch trình bí mật của bạn
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            aria-label="Đóng hộp thoại"
            style={{
              background: 'transparent',
              border: 'none',
              padding: 6,
              borderRadius: 8,
              color: 'var(--text-muted)',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              transition: 'background 0.15s, color 0.15s',
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.backgroundColor = 'var(--border-light)';
              e.currentTarget.style.color = 'var(--text)';
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.backgroundColor = 'transparent';
              e.currentTarget.style.color = 'var(--text-muted)';
            }}
          >
            <X size={18} />
          </button>
        </div>

        {/* Content & Form */}
        <form onSubmit={handleSubmit} autoComplete="off" style={{ padding: '22px' }}>
          <div
            style={{
              padding: '12px 14px',
              borderRadius: 12,
              background: 'rgba(245, 158, 11, 0.08)',
              border: '1px solid rgba(245, 158, 11, 0.25)',
              marginBottom: 18,
            }}
          >
            <div style={{ fontSize: '0.82rem', color: 'var(--text)', lineHeight: 1.45 }}>
              Vui lòng nhập mật khẩu đăng nhập của bạn để hiển thị toàn bộ các lịch trình đang được khóa bảo mật.
            </div>
          </div>

          {error && (
            <div
              style={{
                padding: '10px 14px',
                borderRadius: 10,
                background: 'rgba(239, 68, 68, 0.1)',
                border: '1px solid rgba(239, 68, 68, 0.3)',
                color: '#DC2626',
                fontSize: '0.82rem',
                marginBottom: 16,
                fontWeight: 500,
                lineHeight: 1.45,
              }}
            >
              {error}
            </div>
          )}

          <div style={{ marginBottom: 20 }}>
            <label
              htmlFor="privacy-password-input"
              style={{
                display: 'block',
                fontSize: '0.84rem',
                fontWeight: 600,
                color: 'var(--text)',
                marginBottom: 7,
              }}
            >
              Mật khẩu tài khoản
            </label>
            <div style={{ position: 'relative' }}>
              <div
                style={{
                  position: 'absolute',
                  left: 12,
                  top: '50%',
                  transform: 'translateY(-50%)',
                  color: 'var(--text-muted)',
                  pointerEvents: 'none',
                  display: 'flex',
                  alignItems: 'center',
                }}
              >
                <KeyRound size={16} />
              </div>
              <input
                id="privacy-password-input"
                name="privacy_modal_token"
                ref={inputRef}
                type={showPassword ? 'text' : 'password'}
                value={password}
                onChange={(e) => {
                  setPassword(e.target.value);
                  if (error) setError(null);
                }}
                placeholder="Nhập mật khẩu của bạn..."
                autoComplete="new-password"
                data-lpignore="true"
                data-1p-ignore="true"
                disabled={isLoading}
                style={{
                  width: '100%',
                  boxSizing: 'border-box',
                  padding: '11px 40px 11px 36px',
                  borderRadius: 10,
                  border: error ? '1.5px solid #EF4444' : '1px solid var(--border)',
                  background: 'var(--surface)',
                  color: 'var(--text)',
                  fontSize: '0.9rem',
                  outline: 'none',
                  transition: 'border-color 0.15s, box-shadow 0.15s',
                }}
                onFocus={(e) => {
                  if (!error) e.target.style.borderColor = 'var(--primary)';
                  e.target.style.boxShadow = '0 0 0 3px rgba(108, 92, 231, 0.15)';
                }}
                onBlur={(e) => {
                  if (!error) e.target.style.borderColor = 'var(--border)';
                  e.target.style.boxShadow = 'none';
                }}
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                tabIndex={-1}
                style={{
                  position: 'absolute',
                  right: 8,
                  top: '50%',
                  transform: 'translateY(-50%)',
                  background: 'transparent',
                  border: 'none',
                  padding: 6,
                  color: 'var(--text-muted)',
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  borderRadius: 6,
                }}
                title={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
              >
                {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
              </button>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: 10 }}>
            <button
              type="button"
              onClick={onClose}
              disabled={isLoading}
              style={{
                padding: '9px 16px',
                borderRadius: 10,
                border: '1px solid var(--border)',
                background: 'transparent',
                color: 'var(--text)',
                fontSize: '0.86rem',
                fontWeight: 600,
                cursor: 'pointer',
                transition: 'background 0.15s',
              }}
              onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = 'var(--border-light)')}
              onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = 'transparent')}
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={isLoading}
              style={{
                padding: '9px 20px',
                borderRadius: 10,
                border: 'none',
                background: 'linear-gradient(135deg, #6C5CE7 0%, #a29bfe 100%)',
                color: '#FFFFFF',
                fontSize: '0.86rem',
                fontWeight: 600,
                cursor: isLoading ? 'not-allowed' : 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: 7,
                boxShadow: '0 4px 12px rgba(108, 92, 231, 0.3)',
                opacity: isLoading ? 0.75 : 1,
              }}
            >
              {isLoading ? (
                <>
                  <Loader2 size={15} className="spin-animate" />
                  <span>Đang xác thực...</span>
                </>
              ) : (
                <>
                  <CheckCircle2 size={15} />
                  <span>Mở khóa lịch trình</span>
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>,
    document.body
  );
}

