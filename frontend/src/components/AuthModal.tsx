import { useState } from 'react';
import { X, Lock, Mail, User, Eye, EyeOff, Sparkles } from 'lucide-react';
import { authService } from '../services/authService';
import { validateLoginForm, validateRegisterForm, formatAuthErrorMessage } from '../utils/authValidation';

interface AuthModalProps {
  isOpen: boolean;
  onClose: () => void;
  onLoginSuccess: (user: { email: string; fullName: string; role: string; token: string }) => void;
}

export default function AuthModal({ isOpen, onClose, onLoginSuccess }: AuthModalProps) {
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

  if (!isOpen) return null;

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

    // Kiểm tra định dạng & regex trước khi gửi request
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
          onClose();
        }, 600);
      } else {
        const response = await authService.register({
          fullName: fullName.trim(),
          email: email.trim(),
          password: password,
        });

        setSuccessMessage('Đăng ký tài khoản thành công! Tự động đăng nhập...');
        setTimeout(() => {
          onLoginSuccess(response);
          onClose();
        }, 800);
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
        position: 'fixed',
        inset: 0,
        backgroundColor: 'rgba(15, 23, 42, 0.55)',
        backdropFilter: 'blur(5px)',
        zIndex: 10000,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: 20,
        animation: 'fadeIn 0.2s ease-out',
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
                background: 'linear-gradient(135deg, #6C5CE7, #A29BFE)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#fff',
              }}
            >
              <Sparkles size={18} />
            </div>
            <div>
              <h2 style={{ fontSize: '1.15rem', fontWeight: 700, color: 'var(--text)', margin: 0 }}>
                {mode === 'login' ? 'Đăng nhập SmartTask' : 'Tạo tài khoản mới'}
              </h2>
              <p style={{ fontSize: '0.76rem', color: 'var(--text-3)', margin: '2px 0 0' }}>
                Hệ thống Quản lý và Tự động Lập lịch Công việc
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

        {/* Tab switcher: Đăng nhập / Đăng ký */}
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

        {/* Form Body */}
        <form onSubmit={handleSubmit} style={{ padding: '16px 24px 24px', display: 'flex', flexDirection: 'column', gap: 14 }}>
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
            <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-2)', marginBottom: 6 }}>
              Mật khẩu *
            </label>
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
            disabled={isLoading}
            style={{
              marginTop: 6,
              padding: '11px 16px',
              borderRadius: 10,
              background: 'var(--primary)',
              color: '#fff',
              fontSize: '0.9rem',
              fontWeight: 600,
              boxShadow: '0 2px 8px rgba(108, 92, 231, 0.3)',
              cursor: 'pointer',
              transition: 'all 0.2s',
              opacity: isLoading ? 0.7 : 1,
            }}
          >
            {isLoading
              ? 'Đang xử lý...'
              : mode === 'login'
              ? 'Đăng nhập ngay'
              : 'Hoàn tất Đăng ký'}
          </button>
        </form>
      </div>
    </div>
  );
}
