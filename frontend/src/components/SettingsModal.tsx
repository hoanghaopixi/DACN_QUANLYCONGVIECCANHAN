import { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';
import {
  X, User, Mail, Phone, ShieldCheck, Clock,
  Sliders, Save, CheckCircle2, Moon, Sun, AlertCircle
} from 'lucide-react';
import { authService } from '../services/authService';
import { formatApiErrorMessage } from '../utils/apiErrorHandler';
import Time24Input from './Time24Input';
import HoverSelect from './HoverSelect';
import { useModalAnimation } from '../hooks/useModalAnimation';

interface SettingsModalProps {
  isOpen: boolean;
  onClose: () => void;
  currentUser: { email: string; fullName: string; role: string } | null;
  theme: 'light' | 'dark';
  onToggleTheme: () => void;
  onUpdateProfile?: (updated: { fullName: string; phone?: string }) => void;
}

export default function SettingsModal({
  isOpen,
  onClose,
  currentUser,
  theme,
  onToggleTheme,
  onUpdateProfile,
}: SettingsModalProps) {
  const { shouldRender, isClosing } = useModalAnimation(isOpen, 200);
  const [activeTab, setActiveTab] = useState<'profile' | 'schedule' | 'security'>('profile');
  const [fullName, setFullName] = useState(currentUser?.fullName || 'Minh Anh');
  const [phone, setPhone] = useState('0901234567');
  const [workStartTime, setWorkStartTime] = useState('08:00');
  const [workEndTime, setWorkEndTime] = useState('18:00');
  const [bufferTime, setBufferTime] = useState(15);
  const [peakTime, setPeakTime] = useState<'MORNING' | 'AFTERNOON' | 'EVENING'>('MORNING');
  const [workingDays, setWorkingDays] = useState(['MON', 'TUE', 'WED', 'THU', 'FRI']);
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [saveSuccess, setSaveSuccess] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (!shouldRender) return;
    setErrorMessage('');
    const prevOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = prevOverflow;
    };
  }, [shouldRender]);

  if (!shouldRender || typeof document === 'undefined') return null;

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage('');

    if (activeTab === 'security' && (currentPassword || newPassword)) {
      if (!currentPassword) {
        setErrorMessage('Vui lòng nhập mật khẩu hiện tại');
        return;
      }
      if (!newPassword || newPassword.length < 8) {
        setErrorMessage('Mật khẩu mới phải có tối thiểu 8 ký tự');
        return;
      }
      try {
        setIsSubmitting(true);
        const res = await authService.changePassword({ currentPassword, newPassword });
        if (res.success) {
          setCurrentPassword('');
          setNewPassword('');
          setSaveSuccess(true);
          setTimeout(() => {
            setSaveSuccess(false);
            onClose();
          }, 1200);
          return;
        }
      } catch (err: any) {
        const msg = formatApiErrorMessage(err, 'Đổi mật khẩu thất bại. Vui lòng kiểm tra lại.');
        setErrorMessage(msg);
        return;
      } finally {
        setIsSubmitting(false);
      }
    }

    if (onUpdateProfile) {
      onUpdateProfile({ fullName, phone });
    }
    // Save preferences to local storage or API
    const prefs = {
      workStartTime,
      workEndTime,
      bufferTime,
      peakTime,
      workingDays,
    };
    localStorage.setItem('smarttask_preferences', JSON.stringify(prefs));
    setSaveSuccess(true);
    setTimeout(() => {
      setSaveSuccess(false);
      onClose();
    }, 900);
  };

  const toggleDay = (day: string) => {
    setWorkingDays((prev) =>
      prev.includes(day) ? prev.filter((d) => d !== day) : [...prev, day]
    );
  };

  const DAYS_LIST = [
    { id: 'MON', label: 'T2' },
    { id: 'TUE', label: 'T3' },
    { id: 'WED', label: 'T4' },
    { id: 'THU', label: 'T5' },
    { id: 'FRI', label: 'T6' },
    { id: 'SAT', label: 'T7' },
    { id: 'SUN', label: 'CN' },
  ];

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
          maxWidth: 620,
          boxShadow: 'var(--shadow-lg)',
          overflow: 'hidden',
          display: 'flex',
          flexDirection: 'column',
          border: '1px solid var(--border)',
          maxHeight: '90vh',
          animation: isClosing
            ? 'modalSlideDown 0.2s cubic-bezier(0.4, 0, 1, 1) forwards'
            : 'modalSlideUp 0.25s cubic-bezier(0.16, 1, 0.3, 1)',
        }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div
          style={{
            padding: '18px 24px',
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
                background: 'var(--primary-bg)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: 'var(--primary)',
              }}
            >
              <Sliders size={18} />
            </div>
            <div>
              <h2 style={{ fontSize: '1.2rem', fontWeight: 700, color: 'var(--text)', margin: 0 }}>
                Tài khoản & Thiết lập
              </h2>
              <p style={{ fontSize: '0.78rem', color: 'var(--text-3)', margin: '2px 0 0' }}>
                Quản lý hồ sơ cá nhân và thuật toán tự động lập lịch SmartTask
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

        {/* Tab navigation */}
        <div
          style={{
            display: 'flex',
            borderBottom: '1px solid var(--border-light)',
            padding: '0 24px',
            gap: 20,
            background: 'var(--border-light)',
          }}
        >
          {[
            { id: 'profile', label: 'Thông tin cá nhân', icon: User },
            { id: 'schedule', label: 'Lập lịch & Năng suất', icon: Clock },
            { id: 'security', label: 'Bảo mật & Mật khẩu', icon: ShieldCheck },
          ].map((tab) => {
            const Icon = tab.icon;
            const isActive = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id as any)}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 8,
                  padding: '12px 4px',
                  borderBottom: isActive ? '2px solid var(--primary)' : '2px solid transparent',
                  color: isActive ? 'var(--primary)' : 'var(--text-3)',
                  fontWeight: isActive ? 700 : 500,
                  fontSize: '0.85rem',
                  background: 'none',
                  border: 'none',
                  cursor: 'pointer',
                  transition: 'all 0.2s',
                }}
              >
                <Icon size={16} />
                <span>{tab.label}</span>
              </button>
            );
          })}
        </div>

        {/* Body Form */}
        <form onSubmit={handleSave} style={{ padding: 24, overflowY: 'auto', flex: 1, display: 'flex', flexDirection: 'column', gap: 18 }}>
          {/* TAB 1: PROFILE */}
          {activeTab === 'profile' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-2)', marginBottom: 6 }}>
                  Họ và tên
                </label>
                <div style={{ position: 'relative' }}>
                  <User size={16} style={{ position: 'absolute', left: 12, top: 12, color: 'var(--text-muted)' }} />
                  <input
                    type="text"
                    value={fullName}
                    onChange={(e) => setFullName(e.target.value)}
                    style={{
                      width: '100%',
                      padding: '10px 12px 10px 38px',
                      borderRadius: 8,
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
                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-2)', marginBottom: 6 }}>
                  Email tài khoản
                </label>
                <div style={{ position: 'relative' }}>
                  <Mail size={16} style={{ position: 'absolute', left: 12, top: 12, color: 'var(--text-muted)' }} />
                  <input
                    type="email"
                    disabled
                    value={currentUser?.email || 'admin@gmail.com'}
                    style={{
                      width: '100%',
                      padding: '10px 12px 10px 38px',
                      borderRadius: 8,
                      border: '1px solid var(--border)',
                      background: 'var(--border-light)',
                      color: 'var(--text-muted)',
                      fontSize: '0.88rem',
                      outline: 'none',
                      cursor: 'not-allowed',
                    }}
                  />
                </div>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-2)', marginBottom: 6 }}>
                  Số điện thoại
                </label>
                <div style={{ position: 'relative' }}>
                  <Phone size={16} style={{ position: 'absolute', left: 12, top: 12, color: 'var(--text-muted)' }} />
                  <input
                    type="tel"
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                    style={{
                      width: '100%',
                      padding: '10px 12px 10px 38px',
                      borderRadius: 8,
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
                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-2)', marginBottom: 6 }}>
                  Vai trò hệ thống
                </label>
                <div
                  style={{
                    padding: '8px 14px',
                    borderRadius: 8,
                    background: 'var(--primary-bg)',
                    color: 'var(--primary)',
                    fontWeight: 600,
                    fontSize: '0.85rem',
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: 6,
                  }}
                >
                  <ShieldCheck size={16} />
                  <span>{currentUser?.role || 'Quản trị viên (ROLE_ADMIN)'}</span>
                </div>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-2)', marginBottom: 6 }}>
                  Giao diện hiển thị
                </label>
                <button
                  type="button"
                  onClick={onToggleTheme}
                  style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: 8,
                    padding: '8px 14px',
                    borderRadius: 8,
                    border: '1px solid var(--border)',
                    background: 'var(--surface)',
                    color: 'var(--text)',
                    fontSize: '0.85rem',
                    fontWeight: 600,
                    cursor: 'pointer',
                  }}
                >
                  {theme === 'dark' ? <Moon size={16} style={{ color: '#F59E0B' }} /> : <Sun size={16} style={{ color: '#F59E0B' }} />}
                  <span>Chế độ hiện tại: {theme === 'dark' ? 'Tối (Dark)' : 'Sáng (Light)'}</span>
                </button>
              </div>
            </div>
          )}

          {/* TAB 2: SCHEDULING PREFERENCES */}
          {activeTab === 'schedule' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 14 }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-2)', marginBottom: 6 }}>
                    Giờ bắt đầu làm việc
                  </label>
                  <Time24Input
                    value={workStartTime}
                    onChange={(val) => setWorkStartTime(val)}
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-2)', marginBottom: 6 }}>
                    Giờ kết thúc làm việc
                  </label>
                  <Time24Input
                    value={workEndTime}
                    align="right"
                    onChange={(val) => setWorkEndTime(val)}
                  />
                </div>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-2)', marginBottom: 6 }}>
                  Khung giờ năng suất cao nhất (Peak Productivity)
                </label>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 8 }}>
                  {[
                    { id: 'MORNING', label: 'Buổi sáng (08h - 12h)' },
                    { id: 'AFTERNOON', label: 'Buổi chiều (13h - 17h)' },
                    { id: 'EVENING', label: 'Buổi tối (18h - 22h)' },
                  ].map((p) => (
                    <button
                      key={p.id}
                      type="button"
                      onClick={() => setPeakTime(p.id as any)}
                      style={{
                        padding: '10px 8px',
                        borderRadius: 8,
                        fontSize: '0.8rem',
                        fontWeight: 600,
                        border: peakTime === p.id ? '2px solid var(--primary)' : '1px solid var(--border)',
                        background: peakTime === p.id ? 'var(--primary-bg)' : 'var(--surface)',
                        color: peakTime === p.id ? 'var(--primary)' : 'var(--text-3)',
                        cursor: 'pointer',
                        transition: 'all 0.15s',
                      }}
                    >
                      {p.label}
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-2)', marginBottom: 6 }}>
                  Thời gian đệm nghỉ giữa các việc
                </label>
                <HoverSelect
                  value={bufferTime}
                  onChange={(e) => setBufferTime(Number(e.target.value))}
                  style={{
                    width: '100%',
                    padding: '9px 12px',
                    borderRadius: 8,
                    border: '1px solid var(--border)',
                    background: 'var(--surface)',
                    color: 'var(--text)',
                    fontSize: '0.88rem',
                    outline: 'none',
                  }}
                >
                  <option value={5}>5 phút (Nghỉ ngắn)</option>
                  <option value={10}>10 phút</option>
                  <option value={15}>15 phút (Khuyên dùng)</option>
                  <option value={30}>30 phút (Nghỉ dài)</option>
                </HoverSelect>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-2)', marginBottom: 6 }}>
                  Các ngày làm việc trong tuần
                </label>
                <div style={{ display: 'flex', gap: 8 }}>
                  {DAYS_LIST.map((d) => {
                    const isSelected = workingDays.includes(d.id);
                    return (
                      <button
                        key={d.id}
                        type="button"
                        onClick={() => toggleDay(d.id)}
                        style={{
                          width: 38,
                          height: 38,
                          borderRadius: 8,
                          fontSize: '0.82rem',
                          fontWeight: 700,
                          border: isSelected ? '1px solid var(--primary)' : '1px solid var(--border)',
                          background: isSelected ? 'var(--primary)' : 'var(--surface)',
                          color: isSelected ? '#fff' : 'var(--text-3)',
                          cursor: 'pointer',
                          transition: 'all 0.15s',
                        }}
                      >
                        {d.label}
                      </button>
                    );
                  })}
                </div>
              </div>
            </div>
          )}

          {/* TAB 3: SECURITY */}
          {activeTab === 'security' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
              <div
                style={{
                  padding: '12px 16px',
                  borderRadius: 10,
                  background: 'var(--primary-bg)',
                  border: '1px solid color-mix(in srgb, var(--primary) 20%, transparent)',
                  fontSize: '0.82rem',
                  color: 'var(--text-2)',
                  lineHeight: 1.5,
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontWeight: 700, color: 'var(--primary)', marginBottom: 4 }}>
                  <ShieldCheck size={16} />
                  <span>Cơ chế Bảo mật Chuẩn Quốc tế</span>
                </div>
                Hệ thống lưu trữ mật khẩu sử dụng giải thuật <strong>BCrypt Password Hashing (10 rounds)</strong> với Salt ngẫu nhiên trên cơ sở dữ liệu PostgreSQL. Mật khẩu không bao giờ được lưu dưới dạng bản rõ (plaintext).
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-2)', marginBottom: 6 }}>
                  Mật khẩu hiện tại
                </label>
                <input
                  type="password"
                  placeholder="Nhập mật khẩu hiện tại..."
                  value={currentPassword}
                  onChange={(e) => setCurrentPassword(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '10px 12px',
                    borderRadius: 8,
                    border: '1px solid var(--border)',
                    background: 'var(--surface)',
                    color: 'var(--text)',
                    fontSize: '0.88rem',
                    outline: 'none',
                  }}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-2)', marginBottom: 6 }}>
                  Mật khẩu mới
                </label>
                <input
                  type="password"
                  placeholder="Nhập mật khẩu mới..."
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '10px 12px',
                    borderRadius: 8,
                    border: '1px solid var(--border)',
                    background: 'var(--surface)',
                    color: 'var(--text)',
                    fontSize: '0.88rem',
                    outline: 'none',
                  }}
                />
              </div>
            </div>
          )}

          {/* Error alert */}
          {errorMessage && (
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
              }}
            >
              <AlertCircle size={16} />
              <span>{errorMessage}</span>
            </div>
          )}

          {/* Success alert */}
          {saveSuccess && (
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 8,
                padding: '10px 14px',
                borderRadius: 8,
                background: '#ECFDF5',
                border: '1px solid #A7F3D0',
                color: '#059669',
                fontSize: '0.82rem',
                fontWeight: 600,
              }}
            >
              <CheckCircle2 size={16} />
              <span>Đã lưu thiết lập thành công!</span>
            </div>
          )}

          {/* Footer action buttons */}
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'flex-end',
              gap: 10,
              paddingTop: 14,
              borderTop: '1px solid var(--border-light)',
              marginTop: 10,
            }}
          >
            <button
              type="button"
              onClick={onClose}
              style={{
                padding: '10px 16px',
                borderRadius: 8,
                border: '1px solid var(--border)',
                background: 'var(--surface)',
                color: 'var(--text-2)',
                fontSize: '0.88rem',
                fontWeight: 600,
                cursor: 'pointer',
              }}
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 6,
                padding: '10px 20px',
                borderRadius: 8,
                background: 'var(--primary)',
                color: 'var(--on-primary)',
                fontSize: '0.88rem',
                fontWeight: 600,
                cursor: isSubmitting ? 'not-allowed' : 'pointer',
                opacity: isSubmitting ? 0.75 : 1,
                boxShadow: 'none',
              }}
            >
              <Save size={16} />
              <span>{isSubmitting ? 'Đang lưu...' : 'Lưu thiết lập'}</span>
            </button>
          </div>
        </form>
      </div>
    </div>,
    document.body
  );
}
