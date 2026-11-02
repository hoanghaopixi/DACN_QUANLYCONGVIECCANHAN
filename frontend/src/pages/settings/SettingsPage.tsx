import React, { useState, useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import {
  User, Clock, Sparkles, Bell, ShieldCheck, Moon,
  Check, Info, LogOut, Lock, X, Camera, Upload, Trash2,
  Palette, LucideIcon, Sun
} from 'lucide-react';
import { authService, AuthResponse } from '../../services/authService';
import { preferenceService } from '../../services/preferenceService';
import { mediaService, MediaHistoryItem } from '../../services/mediaService';
import { WorkspaceItem, workspaceService } from '../../services/workspaceService';
import AiPrivacyModal from '../../components/AiPrivacyModal';
import HoverSelect from '../../components/HoverSelect';
import { formatDateDDMMYYYY } from '../../utils/dateTimeFormat';
import { runViewTransition } from '../../utils/viewTransition';
import './SettingsPage.css';

export type SettingsTabId = 'account' | 'schedule' | 'ai' | 'appearance' | 'notifications';

interface SettingsTabItem {
  id: SettingsTabId;
  label: string;
  icon: LucideIcon;
}

export const SETTINGS_TABS: SettingsTabItem[] = [
  { id: 'account', label: 'Hồ sơ & Tài khoản', icon: User },
  { id: 'schedule', label: 'Giờ làm việc & Lịch', icon: Clock },
  { id: 'ai', label: 'Trợ lý AI & Quyền', icon: Sparkles },
  { id: 'appearance', label: 'Hình nền & Giao diện', icon: Palette },
  { id: 'notifications', label: 'Thông báo & Nhắc hẹn', icon: Bell },
];

interface SettingsPageProps {
  currentUser: AuthResponse | null;
  theme: 'light' | 'dark';
  onThemeChange: (theme: 'light' | 'dark') => void;
  onUpdateProfile?: (updated: { fullName: string; phone?: string; avatarUrl?: string }) => void;
  onUpdateBackground?: (bg: { backgroundUrl?: string; backgroundType?: 'NONE' | 'IMAGE' | 'VIDEO'; overlayDarkness?: number }) => void;
  onLogout: () => void;
  onShowToast?: (msg: string) => void;
  onBackToCalendar?: () => void;
}

// Helper to compress avatar image to max 256x256 before saving (keeps base64 small and ultra fast)
const compressImage = (file: File, maxSize = 256): Promise<string> => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      const img = new Image();
      img.onload = () => {
        const canvas = document.createElement('canvas');
        let width = img.width;
        let height = img.height;
        if (width > height) {
          if (width > maxSize) {
            height = Math.round((height * maxSize) / width);
            width = maxSize;
          }
        } else {
          if (height > maxSize) {
            width = Math.round((width * maxSize) / height);
            height = maxSize;
          }
        }
        canvas.width = width;
        canvas.height = height;
        const ctx = canvas.getContext('2d');
        if (!ctx) {
          resolve(e.target?.result as string);
          return;
        }
        ctx.drawImage(img, 0, 0, width, height);
        resolve(canvas.toDataURL('image/jpeg', 0.88));
      };
      img.onerror = reject;
      img.src = e.target?.result as string;
    };
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
};

// Preset placeholders removed - user uploads their own media and saves history to database
export const PRESET_BACKGROUNDS: any[] = [];

export interface UserSettingsData {
  fullName: string;
  email: string;
  phone: string;
  avatarUrl?: string;
  backgroundUrl?: string;
  backgroundType?: 'NONE' | 'IMAGE' | 'VIDEO';
  overlayDarkness?: number; // 0 to 100 percentage (default 50)
  language: string;
  timezone: string;
  workingDays: string[];
  workHours: string;
  lunchBreak: string;
  bufferMinutes: string;
  allowWeekend: boolean;
  autoScheduleUnscheduled: boolean;
  alwaysAskBeforeChange: boolean;
  allowSplitLongTasks: boolean;
  autoMoveFixedTasks: boolean;
  reminderBefore: string;
  browserNotifications: boolean;
  morningSummaryEmail: boolean;
  dndHours: string;
}

const DEFAULT_SETTINGS: UserSettingsData = {
  fullName: 'Minh Anh',
  email: 'minhanh.nguyen@gmail.com',
  phone: '0901234567',
  avatarUrl: '',
  backgroundUrl: '',
  backgroundType: 'NONE',
  overlayDarkness: 50,
  language: 'Tiếng Việt',
  timezone: '(GMT+07:00) Hồ Chí Minh',
  workingDays: ['MON', 'TUE', 'WED', 'THU', 'FRI'],
  workHours: '07:00 - 20:00',
  lunchBreak: '12:00 - 13:00',
  bufferMinutes: '15 phút',
  allowWeekend: true,
  autoScheduleUnscheduled: true,
  alwaysAskBeforeChange: true,
  allowSplitLongTasks: true,
  autoMoveFixedTasks: false,
  reminderBefore: '15 phút',
  browserNotifications: true,
  morningSummaryEmail: false,
  dndHours: '22:00 - 07:00',
};

const DAYS_OF_WEEK = [
  { id: 'MON', label: 'T2' },
  { id: 'TUE', label: 'T3' },
  { id: 'WED', label: 'T4' },
  { id: 'THU', label: 'T5' },
  { id: 'FRI', label: 'T6' },
  { id: 'SAT', label: 'T7' },
  { id: 'SUN', label: 'CN' },
];

const BASE_TIME_OPTIONS = Array.from({ length: 48 }, (_, i) => {
  const h = Math.floor(i / 2);
  const m = i % 2 === 0 ? '00' : '30';
  return `${String(h).padStart(2, '0')}:${m}`;
});

function parseTimeRange(rangeStr: string, defaultStart: string, defaultEnd: string): [string, string] {
  if (!rangeStr) return [defaultStart, defaultEnd];
  const parts = rangeStr.split('-').map((s) => s.trim());
  return [parts[0] || defaultStart, parts[1] || defaultEnd];
}

function getTimeOptions(currentVal?: string) {
  if (!currentVal || BASE_TIME_OPTIONS.includes(currentVal)) {
    return BASE_TIME_OPTIONS;
  }
  return [...BASE_TIME_OPTIONS, currentVal].sort();
}

export default function SettingsPage({
  currentUser,
  theme,
  onThemeChange,
  onUpdateProfile,
  onUpdateBackground,
  onLogout,
  onShowToast,
  onBackToCalendar,
}: SettingsPageProps) {
  // Load saved settings or fall back to default + currentUser
  const [initialSettings, setInitialSettings] = useState<UserSettingsData>(() => {
    const saved = localStorage.getItem('smarttask_settings_full');
    if (saved) {
      try {
        const parsed = JSON.parse(saved);
        if (currentUser) {
          parsed.fullName = currentUser.fullName || parsed.fullName;
          parsed.email = currentUser.email || parsed.email;
          parsed.phone = currentUser.phone || parsed.phone || '';
          parsed.avatarUrl = currentUser.avatarUrl || parsed.avatarUrl || '';
        }
        return { ...DEFAULT_SETTINGS, ...parsed };
      } catch (e) {
        console.error('Error parsing settings:', e);
      }
    }
    return {
      ...DEFAULT_SETTINGS,
      fullName: currentUser?.fullName || DEFAULT_SETTINGS.fullName,
      email: currentUser?.email || DEFAULT_SETTINGS.email,
      phone: currentUser?.phone || DEFAULT_SETTINGS.phone,
      avatarUrl: currentUser?.avatarUrl || DEFAULT_SETTINGS.avatarUrl,
    };
  });

  const [settings, setSettings] = useState<UserSettingsData>(initialSettings);
  const [isDirty, setIsDirty] = useState(false);
  const [isSavedRecently, setIsSavedRecently] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const bgFileInputRef = useRef<HTMLInputElement>(null);

  // Avatar Management State
  const [showPresets, setShowPresets] = useState(false);
  const [avatarHistory, setAvatarHistory] = useState<MediaHistoryItem[]>([]);
  const [bgHistory, setBgHistory] = useState<MediaHistoryItem[]>([]);

  // Load uploaded media history from server
  const loadMediaHistory = async () => {
    try {
      const [avatars, bgs] = await Promise.all([
        mediaService.getHistory('AVATAR'),
        mediaService.getHistory('BACKGROUND'),
      ]);
      setAvatarHistory(avatars);
      setBgHistory(bgs);
    } catch (err) {
      console.error('Lỗi tải lịch sử media từ máy chủ:', err);
    }
  };

  useEffect(() => {
    loadMediaHistory();
  }, []);

  // Password Change Modal
  const [isPasswordModalOpen, setIsPasswordModalOpen] = useState(false);
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [passwordError, setPasswordError] = useState('');
  const [passwordSuccess, setPasswordSuccess] = useState(false);
  const [isChangingPassword, setIsChangingPassword] = useState(false);

  // Active Tab State (Browser-style tabs)
  const [activeTab, setActiveTab] = useState<SettingsTabId>('account');

  // AI Workspace Privacy Modal State
  const [showPrivacyModal, setShowPrivacyModal] = useState(false);
  const [workspaces, setWorkspaces] = useState<WorkspaceItem[]>([]);

  useEffect(() => {
    workspaceService.getWorkspaces().then((list) => {
      if (list) setWorkspaces(list);
    }).catch(() => { });
  }, []);

  // Sync when currentUser changes and load preferences from PostgreSQL
  useEffect(() => {
    if (currentUser) {
      setSettings((prev) => ({
        ...prev,
        fullName: currentUser.fullName || prev.fullName,
        email: currentUser.email || prev.email,
        phone: currentUser.phone || prev.phone,
        avatarUrl: currentUser.avatarUrl || prev.avatarUrl,
      }));
    }

    preferenceService.getPreferences().then((pref) => {
      if (pref) {
        setSettings((prev) => {
          const updated = {
            ...prev,
            workHours: `${pref.workStartTime || '08:00'} - ${pref.workEndTime || '18:00'}`,
            bufferMinutes: `${pref.bufferMinutes || 15} phút`,
            workingDays: pref.workDays ? pref.workDays.split(',') : prev.workingDays,
            backgroundUrl: pref.backgroundUrl || prev.backgroundUrl || '',
            backgroundType: (pref.backgroundType as any) || prev.backgroundType || 'NONE',
            overlayDarkness: pref.overlayDarkness !== undefined ? pref.overlayDarkness : (prev.overlayDarkness ?? 50),
          };
          setInitialSettings(updated);
          return updated;
        });
        if (onUpdateBackground) {
          onUpdateBackground({
            backgroundUrl: pref.backgroundUrl || '',
            backgroundType: (pref.backgroundType as any) || 'NONE',
            overlayDarkness: pref.overlayDarkness !== undefined ? pref.overlayDarkness : 50,
          });
        }
      }
    }).catch(() => { });
  }, [currentUser]);

  // Check if dirty
  useEffect(() => {
    const dirty = JSON.stringify(settings) !== JSON.stringify(initialSettings);
    setIsDirty(dirty);
  }, [settings, initialSettings]);

  const handleChange = <K extends keyof UserSettingsData>(field: K, value: UserSettingsData[K]) => {
    setSettings((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  const handleAvatarFile = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (file.size > 15 * 1024 * 1024) {
      onShowToast?.('⚠️ Kích thước ảnh không được vượt quá 15MB');
      return;
    }
    try {
      onShowToast?.('⏳ Đang tải ảnh lên thư mục máy chủ và lưu lịch sử...');
      const uploadRes = await authService.uploadAvatar(file);
      if (uploadRes.success && uploadRes.avatarUrl) {
        handleChange('avatarUrl', uploadRes.avatarUrl);
        if (onUpdateProfile) {
          onUpdateProfile({ fullName: settings.fullName, phone: settings.phone, avatarUrl: uploadRes.avatarUrl });
        }
        onShowToast?.('✓ Đã tải ảnh và đồng bộ lịch sử máy chủ thành công!');
        await loadMediaHistory();
      }
    } catch (err: any) {
      console.error('Lỗi upload avatar lên backend:', err);
      try {
        const compressedDataUrl = await compressImage(file, 256);
        handleChange('avatarUrl', compressedDataUrl);
        onShowToast?.('✓ Đã tải ảnh đại diện xem trước. Bấm "Lưu thiết lập" để cập nhật.');
      } catch {
        onShowToast?.('⚠️ Không thể đọc tệp ảnh này.');
      }
    }
    if (fileInputRef.current) fileInputRef.current.value = '';
  };




  const handleRemoveAvatar = () => {
    handleChange('avatarUrl', '');
    onShowToast?.('✓ Đã xóa ảnh đại diện. Nhớ bấm "Lưu thiết lập" để cập nhật.');
  };

  const toggleDay = (dayId: string) => {
    const currentDays = settings.workingDays;
    const exists = currentDays.includes(dayId);
    const updated = exists ? currentDays.filter((d) => d !== dayId) : [...currentDays, dayId];
    handleChange('workingDays', updated);
  };

  // ── Background Personalization Handlers ──
  const handleDarknessChange = (value: number) => {
    handleChange('overlayDarkness', value);
    if (onUpdateBackground) {
      onUpdateBackground({
        backgroundUrl: settings.backgroundUrl,
        backgroundType: settings.backgroundType,
        overlayDarkness: value,
      });
    }
  };

  const handleBackgroundFile = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (file.size > 50 * 1024 * 1024) {
      onShowToast?.('⚠️ Kích thước tệp không được vượt quá 50MB');
      return;
    }
    try {
      onShowToast?.('⏳ Đang tải hình nền / video lên máy chủ và lưu lịch sử...');
      const res = await preferenceService.uploadBackground(file);
      if (res.success && res.backgroundUrl) {
        setSettings((prev) => ({
          ...prev,
          backgroundUrl: res.backgroundUrl,
          backgroundType: res.backgroundType,
          overlayDarkness: res.overlayDarkness ?? 50,
        }));
        if (onUpdateBackground) {
          onUpdateBackground({
            backgroundUrl: res.backgroundUrl,
            backgroundType: res.backgroundType,
            overlayDarkness: res.overlayDarkness ?? 50,
          });
        }
        onShowToast?.(`✓ Đã tải và lưu ${res.backgroundType === 'VIDEO' ? 'video' : 'hình ảnh'} nền vào lịch sử thành công!`);
        await loadMediaHistory();
      }
    } catch (err: any) {
      console.error('Lỗi khi tải hình nền:', err);
      onShowToast?.('⚠️ Lỗi khi tải tệp hình nền lên máy chủ backend.');
    }
    if (bgFileInputRef.current) bgFileInputRef.current.value = '';
  };

  const handleSelectHistoryAvatar = (url: string) => {
    handleChange('avatarUrl', url);
    if (onUpdateProfile) {
      onUpdateProfile({ fullName: settings.fullName, phone: settings.phone, avatarUrl: url });
    }
    onShowToast?.('✓ Đã áp dụng ảnh đại diện từ lịch sử!');
  };

  const handleSelectHistoryBg = (item: MediaHistoryItem) => {
    const bgType = item.mediaType === 'BACKGROUND_VIDEO' ? 'VIDEO' : 'IMAGE';
    setSettings((prev) => ({
      ...prev,
      backgroundUrl: item.url,
      backgroundType: bgType,
      overlayDarkness: prev.overlayDarkness ?? 50,
    }));
    if (onUpdateBackground) {
      onUpdateBackground({
        backgroundUrl: item.url,
        backgroundType: bgType,
        overlayDarkness: settings.overlayDarkness ?? 50,
      });
    }
    onShowToast?.(`✓ Đã áp dụng ${bgType === 'VIDEO' ? 'video' : 'hình ảnh'} nền từ lịch sử!`);
  };

  const handleDeleteMediaHistory = async (e: React.MouseEvent, id: number, type: 'AVATAR' | 'BACKGROUND') => {
    e.stopPropagation();
    try {
      await mediaService.deleteHistory(id);
      if (type === 'AVATAR') {
        setAvatarHistory((prev) => prev.filter((item) => item.id !== id));
      } else {
        setBgHistory((prev) => prev.filter((item) => item.id !== id));
      }
      onShowToast?.('✓ Đã xóa khỏi lịch sử lưu trữ');
    } catch (err) {
      console.error('Lỗi xóa media history:', err);
    }
  };

  const handleRemoveBackground = () => {
    setSettings((prev) => ({
      ...prev,
      backgroundUrl: '',
      backgroundType: 'NONE',
    }));
    if (onUpdateBackground) {
      onUpdateBackground({
        backgroundUrl: '',
        backgroundType: 'NONE',
        overlayDarkness: settings.overlayDarkness ?? 50,
      });
    }
    onShowToast?.('✓ Đã xóa hình nền cá nhân. Bấm "Lưu thiết lập" để cập nhật.');
  };

  const handleSave = async () => {
    localStorage.setItem('smarttask_settings_full', JSON.stringify(settings));
    // Also save compatible preferences object
    const legacyPrefs = {
      workStartTime: settings.workHours.split('-')[0]?.trim() || '08:00',
      workEndTime: settings.workHours.split('-')[1]?.trim() || '18:00',
      bufferTime: parseInt(settings.bufferMinutes, 10) || 15,
      workingDays: settings.workingDays,
    };
    localStorage.setItem('smarttask_preferences', JSON.stringify(legacyPrefs));

    try {
      await Promise.all([
        authService.updateProfile({
          fullName: settings.fullName,
          phone: settings.phone,
          avatarUrl: settings.avatarUrl,
        }),
        preferenceService.updatePreferences({
          workStartTime: legacyPrefs.workStartTime,
          workEndTime: legacyPrefs.workEndTime,
          workDays: settings.workingDays.join(','),
          bufferMinutes: legacyPrefs.bufferTime,
          peakProductivityTime: 'MORNING',
          autoReschedule: settings.autoScheduleUnscheduled,
          timeZone: settings.timezone.includes('Hồ Chí Minh') ? 'Asia/Ho_Chi_Minh' : 'Asia/Bangkok',
          backgroundUrl: settings.backgroundUrl,
          backgroundType: settings.backgroundType,
          overlayDarkness: settings.overlayDarkness,
        }),
      ]);
    } catch (err) {
      console.warn('Lỗi khi đồng bộ profile và thiết lập lên máy chủ:', err);
    }

    if (onUpdateProfile) {
      onUpdateProfile({ fullName: settings.fullName, phone: settings.phone, avatarUrl: settings.avatarUrl });
    }

    if (onUpdateBackground) {
      onUpdateBackground({
        backgroundUrl: settings.backgroundUrl,
        backgroundType: settings.backgroundType,
        overlayDarkness: settings.overlayDarkness,
      });
    }

    setInitialSettings(settings);
    setIsDirty(false);
    setIsSavedRecently(true);
    if (onShowToast) {
      onShowToast('✓ Đã lưu thiết lập cá nhân vào cơ sở dữ liệu thành công!');
    }
    setTimeout(() => {
      setIsSavedRecently(false);
    }, 2500);
  };

  const handleCancel = () => {
    setSettings(initialSettings);
    setIsDirty(false);
    if (onShowToast) {
      onShowToast('Đã hủy các thay đổi chưa lưu.');
    }
    if (onBackToCalendar && !isDirty) {
      onBackToCalendar();
    }
  };

  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setPasswordError('');
    if (!currentPassword) {
      setPasswordError('Vui lòng nhập mật khẩu hiện tại');
      return;
    }
    if (newPassword.length < 1) {
      setPasswordError('Mật khẩu mới không được để trống');
      return;
    }
    if (newPassword !== confirmPassword) {
      setPasswordError('Xác nhận mật khẩu mới không khớp');
      return;
    }

    setIsChangingPassword(true);
    try {
      await authService.changePassword({
        currentPassword,
        newPassword,
      });
      setPasswordSuccess(true);
      setTimeout(() => {
        setPasswordSuccess(false);
        runViewTransition(() => setIsPasswordModalOpen(false));
        setCurrentPassword('');
        setNewPassword('');
        setConfirmPassword('');
        if (onShowToast) {
          onShowToast('✓ Đổi mật khẩu tài khoản thành công!');
        }
      }, 1000);
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'Mật khẩu hiện tại không chính xác';
      setPasswordError(msg);
    } finally {
      setIsChangingPassword(false);
    }
  };

  // Get user initials for avatar
  const initials = (settings.fullName || 'Minh Anh')
    .split(' ')
    .filter(Boolean)
    .map((w) => w[0])
    .join('')
    .slice(0, 2)
    .toUpperCase();

  // Parse time ranges for dropdown selection
  const [workStart, workEnd] = parseTimeRange(settings.workHours, '08:00', '18:00');
  const [lunchStart, lunchEnd] = parseTimeRange(settings.lunchBreak, '12:00', '13:00');
  const [dndStart, dndEnd] = parseTimeRange(settings.dndHours, '22:00', '07:00');

  const updateWorkHours = (start: string, end: string) => {
    handleChange('workHours', `${start} - ${end}`);
  };

  const updateLunchBreak = (start: string, end: string) => {
    handleChange('lunchBreak', `${start} - ${end}`);
  };

  const updateDndHours = (start: string, end: string) => {
    handleChange('dndHours', `${start} - ${end}`);
  };


  // ── Settings Cards Definition ──
  // Card 1: Thông tin tài khoản
  const accountInfoCard = (
    <div className="settings-card">
      <div className="settings-card-header">
        <div className="card-icon-badge user-badge">
          <User size={18} />
        </div>
        <div>
          <h2 className="card-title">Thông tin tài khoản</h2>
          <p className="card-subtitle">Thông tin dùng trong không gian cá nhân của bạn.</p>
        </div>
      </div>

      <div className="avatar-section-wrapper">
        <div className="avatar-section">
          <input
            type="file"
            ref={fileInputRef}
            style={{ display: 'none' }}
            accept="image/*"
            onChange={handleAvatarFile}
          />
          <div
            className="avatar-circle-wrapper"
            onClick={() => fileInputRef.current?.click()}
            title="Nhấp để tải ảnh đại diện từ thiết bị"
          >
            <div className="avatar-circle">
              {settings.avatarUrl ? (
                <img
                  src={settings.avatarUrl}
                  alt="Avatar"
                  className="avatar-image-preview"
                />
              ) : (
                initials
              )}
            </div>
            <div className="avatar-badge-btn" title="Thay đổi ảnh">
              <Camera size={13} />
            </div>
          </div>

          <div className="avatar-details">
            <div className="avatar-name">{settings.fullName || 'Người dùng'}</div>
            <div className="avatar-email-sub">{settings.email}</div>
            <div className="avatar-actions-row">
              <button
                type="button"
                className="avatar-action-btn primary"
                onClick={() => fileInputRef.current?.click()}
                title="Chọn tệp ảnh từ máy tính"
              >
                <Upload size={13} />
                <span>Tải ảnh lên</span>
              </button>
              {avatarHistory.length > 0 && (
                <button
                  type="button"
                  className={`avatar-action-btn ${showPresets ? 'active' : ''}`}
                  onClick={() => setShowPresets(!showPresets)}
                  title="Xem lại các ảnh đại diện bạn đã tải lên trước đó"
                >
                  <Clock size={13} />
                  <span>Lịch sử ảnh ({avatarHistory.length})</span>
                </button>
              )}
              {settings.avatarUrl && (
                <button
                  type="button"
                  className="avatar-action-btn danger"
                  onClick={handleRemoveAvatar}
                  title="Xóa ảnh và dùng chữ cái tên"
                >
                  <Trash2 size={13} />
                  <span>Xóa ảnh</span>
                </button>
              )}
            </div>
          </div>

          <div className="verified-badge">
            <Check size={12} />
            <span>Email đã xác minh</span>
          </div>
        </div>

        {/* Lịch sử ảnh đại diện của người dùng (tự tải lên và lưu vào CSDL) */}
        {showPresets && (
          <div className="avatar-drawer">
            <div className="avatar-drawer-title">
              <Clock size={14} />
              <span>Lịch sử ảnh đại diện đã tải lên ({avatarHistory.length}):</span>
            </div>
            {avatarHistory.length === 0 ? (
              <div style={{ fontSize: '0.80rem', color: 'var(--text-muted)', padding: '12px 6px', textAlign: 'center' }}>
                Chưa có ảnh nào trong lịch sử. Hãy bấm <strong>"Tải ảnh lên"</strong> để thêm ảnh từ máy của bạn!
              </div>
            ) : (
              <div className="preset-avatars-grid">
                {avatarHistory.map((item) => {
                  const isSelected = settings.avatarUrl === item.url;
                  return (
                    <div
                      key={item.id}
                      className={`preset-avatar-item ${isSelected ? 'selected' : ''}`}
                      onClick={() => handleSelectHistoryAvatar(item.url)}
                      style={{ position: 'relative' }}
                      title={`Bấm để chọn ảnh này (Tải lên ngày: ${formatDateDDMMYYYY(item.createdAt)})`}
                    >
                      <img src={item.url} alt="Ảnh đại diện lịch sử" />
                      {isSelected && (
                        <div className="preset-selected-check">
                          <Check size={10} />
                        </div>
                      )}
                      <button
                        type="button"
                        onClick={(e) => handleDeleteMediaHistory(e, item.id, 'AVATAR')}
                        title="Xóa ảnh này khỏi lịch sử"
                        style={{
                          position: 'absolute',
                          top: -4,
                          right: -4,
                          width: 16,
                          height: 16,
                          borderRadius: '50%',
                          background: '#EF4444',
                          color: '#fff',
                          border: 'none',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          cursor: 'pointer',
                          padding: 0,
                        }}
                      >
                        <X size={10} />
                      </button>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        )}
      </div>

      <div className="form-grid-2x2">
        <div className="form-group">
          <label className="form-label">Họ và tên</label>
          <input
            type="text"
            className="form-input"
            value={settings.fullName}
            onChange={(e) => handleChange('fullName', e.target.value)}
            placeholder="Nhập họ và tên"
          />
        </div>

        <div className="form-group">
          <label className="form-label">Số điện thoại</label>
          <input
            type="tel"
            className="form-input"
            value={settings.phone || ''}
            onChange={(e) => handleChange('phone', e.target.value)}
            placeholder="VD: 0901234567"
          />
        </div>

        <div className="form-group">
          <label className="form-label">Email (Định danh tài khoản)</label>
          <input
            type="email"
            className="form-input"
            value={settings.email}
            disabled
            readOnly
            title="Email là định danh tài khoản không thể chỉnh sửa"
            style={{ opacity: 0.75, cursor: 'not-allowed', background: 'var(--border-light)' }}
          />
        </div>

        <div className="form-group">
          <label className="form-label">Ngôn ngữ</label>
          <HoverSelect
            className="form-select"
            value={settings.language}
            onChange={(e) => handleChange('language', e.target.value)}
          >
            <option value="Tiếng Việt">Tiếng Việt</option>
            <option value="English">English</option>
          </HoverSelect>
        </div>

        <div className="form-group" style={{ gridColumn: 'span 2' }}>
          <label className="form-label">Múi giờ</label>
          <HoverSelect
            className="form-select"
            value={settings.timezone}
            onChange={(e) => handleChange('timezone', e.target.value)}
          >
            <option value="(GMT+07:00) Hồ Chí Minh">(GMT+07:00) Hồ Chí Minh</option>
            <option value="(GMT+07:00) Bangkok">(GMT+07:00) Bangkok</option>
            <option value="(GMT+08:00) Singapore">(GMT+08:00) Singapore</option>
            <option value="(GMT+09:00) Tokyo">(GMT+09:00) Tokyo</option>
          </HoverSelect>
        </div>
      </div>
    </div>
  );

  // Card 2: Giờ làm việc & khoảng nghỉ
  const workHoursCard = (
    <div className="settings-card">
      <div className="settings-card-header">
        <div className="card-icon-badge clock-badge">
          <Clock size={18} />
        </div>
        <div>
          <h2 className="card-title">Giờ làm việc &amp; khoảng nghỉ</h2>
          <p className="card-subtitle">Các khung giờ ưu tiên cho Học tập và Công việc.</p>
        </div>
      </div>

      {/* Ngày trong tuần */}
      <div className="settings-field-group">
        <div className="field-group-header">
          <span className="field-label">Ngày trong tuần</span>
          <div className="days-picker">
            {DAYS_OF_WEEK.map((d) => {
              const isSelected = settings.workingDays.includes(d.id);
              return (
                <button
                  key={d.id}
                  type="button"
                  className={`day-btn ${isSelected ? 'selected' : ''}`}
                  onClick={() => toggleDay(d.id)}
                >
                  {d.label}
                </button>
              );
            })}
          </div>
        </div>
      </div>

      {/* 3 columns: Bắt đầu - Kết thúc, Nghỉ trưa, Nghỉ giữa công việc */}
      <div className="form-grid-3cols">
        <div className="form-group">
          <label className="form-label">Bắt đầu – Kết thúc</label>
          <div className="time-range-picker">
            <HoverSelect
              className="time-select"
              value={workStart}
              onChange={(e) => updateWorkHours(e.target.value, workEnd)}
              aria-label="Giờ bắt đầu làm việc"
            >
              {getTimeOptions(workStart).map((t) => (
                <option key={t} value={t}>{t}</option>
              ))}
            </HoverSelect>
            <span className="time-range-separator">–</span>
            <HoverSelect
              className="time-select"
              value={workEnd}
              onChange={(e) => updateWorkHours(workStart, e.target.value)}
              aria-label="Giờ kết thúc làm việc"
            >
              {getTimeOptions(workEnd).map((t) => (
                <option key={t} value={t}>{t}</option>
              ))}
            </HoverSelect>
            <Clock size={16} className="time-range-icon" />
          </div>
        </div>

        <div className="form-group">
          <label className="form-label">Nghỉ trưa</label>
          <div className="time-range-picker">
            <HoverSelect
              className="time-select"
              value={lunchStart}
              onChange={(e) => updateLunchBreak(e.target.value, lunchEnd)}
              aria-label="Giờ bắt đầu nghỉ trưa"
            >
              {getTimeOptions(lunchStart).map((t) => (
                <option key={t} value={t}>{t}</option>
              ))}
            </HoverSelect>
            <span className="time-range-separator">–</span>
            <HoverSelect
              className="time-select"
              value={lunchEnd}
              onChange={(e) => updateLunchBreak(lunchStart, e.target.value)}
              aria-label="Giờ kết thúc nghỉ trưa"
            >
              {getTimeOptions(lunchEnd).map((t) => (
                <option key={t} value={t}>{t}</option>
              ))}
            </HoverSelect>
            <Clock size={16} className="time-range-icon" />
          </div>
        </div>

        <div className="form-group">
          <label className="form-label">Nghỉ giữa công việc</label>
          <HoverSelect
            className="form-select"
            value={settings.bufferMinutes}
            onChange={(e) => handleChange('bufferMinutes', e.target.value)}
          >
            <option value="5 phút">5 phút</option>
            <option value="10 phút">10 phút</option>
            <option value="15 phút">15 phút</option>
            <option value="20 phút">20 phút</option>
            <option value="30 phút">30 phút</option>
          </HoverSelect>
        </div>
      </div>

      {/* Weekend toggle */}
      <div className="toggle-row with-border-top">
        <div className="toggle-text">
          <div className="toggle-title">Cho phép Sức khỏe &amp; Giải trí vào cuối tuần</div>
          <div className="toggle-subtitle">
            SmartTask có thể đề xuất đọc sách vào thứ Bảy hoặc Chủ Nhật, 08:00–18:00.
          </div>
        </div>
        <label className="switch">
          <input
            type="checkbox"
            checked={settings.allowWeekend}
            onChange={(e) => handleChange('allowWeekend', e.target.checked)}
          />
          <span className="slider round"></span>
        </label>
      </div>
    </div>
  );

  // Card 3: Quyền kiểm soát trợ lý AI
  const aiControlCard = (
    <div className="settings-card">
      <div className="settings-card-header">
        <div className="card-icon-badge ai-badge">
          <Sparkles size={18} />
        </div>
        <div>
          <h2 className="card-title">Quyền kiểm soát trợ lý AI</h2>
          <p className="card-subtitle">AI đề xuất lịch phù hợp, bạn luôn là người quyết định.</p>
        </div>
      </div>

      <div className="toggle-list">
        <div className="toggle-row">
          <div className="toggle-text">
            <div className="toggle-title">Tự động tìm khung giờ cho việc chưa có lịch</div>
            <div className="toggle-subtitle">Ưu tiên việc quan trọng và tránh giờ nghỉ của bạn.</div>
          </div>
          <label className="switch">
            <input
              type="checkbox"
              checked={settings.autoScheduleUnscheduled}
              onChange={(e) => handleChange('autoScheduleUnscheduled', e.target.checked)}
            />
            <span className="slider round"></span>
          </label>
        </div>

        <div className="toggle-row">
          <div className="toggle-text">
            <div className="toggle-title">Luôn hỏi trước khi thêm hoặc đổi lịch</div>
            <div className="toggle-subtitle">Xem trước toàn bộ thay đổi và xác nhận để áp dụng.</div>
          </div>
          <label className="switch">
            <input
              type="checkbox"
              checked={settings.alwaysAskBeforeChange}
              onChange={(e) => handleChange('alwaysAskBeforeChange', e.target.checked)}
            />
            <span className="slider round"></span>
          </label>
        </div>

        <div className="toggle-row">
          <div className="toggle-text">
            <div className="toggle-title">Cho phép chia nhỏ công việc dài</div>
            <div className="toggle-subtitle">Mỗi phiên tối thiểu 60 phút. Không chia các lịch hẹn cố định.</div>
          </div>
          <label className="switch">
            <input
              type="checkbox"
              checked={settings.allowSplitLongTasks}
              onChange={(e) => handleChange('allowSplitLongTasks', e.target.checked)}
            />
            <span className="slider round"></span>
          </label>
        </div>

        <div className="toggle-row">
          <div className="toggle-text">
            <div className="toggle-title">Tự di chuyển công việc đã có giờ cố định</div>
            <div className="toggle-subtitle">
              {settings.autoMoveFixedTasks
                ? 'Đang bật. Các công việc cố định có thể được điều chỉnh linh hoạt.'
                : 'Đang tắt. Các cuộc họp và lịch cá nhân được giữ nguyên.'}
            </div>
          </div>
          <label className="switch">
            <input
              type="checkbox"
              checked={settings.autoMoveFixedTasks}
              onChange={(e) => handleChange('autoMoveFixedTasks', e.target.checked)}
            />
            <span className="slider round"></span>
          </label>
        </div>

        {/* Row: Quản lý quyền AI truy cập Không gian làm việc */}
        <div
          style={{
            marginTop: 14,
            paddingTop: 14,
            borderTop: '1px solid var(--border-light)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: 12,
          }}
        >
          <div>
            <div style={{ fontWeight: 600, fontSize: '0.86rem', color: 'var(--text)', display: 'flex', alignItems: 'center', gap: 6 }}>
              <ShieldCheck size={16} style={{ color: 'var(--primary)' }} />
              <span>Quản lý quyền AI truy cập Không gian làm việc</span>
            </div>
            <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)', marginTop: 2 }}>
              Kiểm soát không gian nào AI được phép đọc dữ liệu để bảo vệ bí mật công việc cá nhân.
            </div>
          </div>
          <button
            type="button"
            onClick={() => runViewTransition(() => setShowPrivacyModal(true))}
            style={{
              padding: '6px 14px',
              fontSize: '0.78rem',
              fontWeight: 600,
              borderRadius: 8,
              border: '1px solid var(--primary)',
              background: 'var(--primary-bg)',
              color: 'var(--primary)',
              cursor: 'pointer',
              whiteSpace: 'nowrap',
              transition: 'all 0.15s ease',
            }}
          >
            Cấu hình quyền AI
          </button>
        </div>
      </div>
    </div>
  );

  // Card 4: Hình nền cá nhân hóa & Video
  const appearanceCard = (
    <div className="settings-card bg-customizer-card">
      <div className="settings-card-header bg-customizer-header">
        <div className="card-icon-badge" style={{ background: 'var(--primary-bg)', color: 'var(--primary)' }}>
          <Palette size={18} />
        </div>
        <div className="bg-customizer-title">
          <h2 className="card-title">Hình nền cá nhân hóa &amp; Video</h2>
          <p className="card-subtitle">
            Cài đặt ảnh hoặc video động làm hình nền. Kèm bộ lọc phủ tối để giữ độ tương phản.
          </p>
        </div>
        <div className="theme-preference-controls">
          <div className="theme-preference-label">
            <Palette size={16} />
            <span>Giao diện</span>
          </div>
          <div className="theme-preference-options" role="group" aria-label="Chọn giao diện">
            <button
              type="button"
              className={theme === 'light' ? 'active' : ''}
              aria-pressed={theme === 'light'}
              onClick={() => runViewTransition(() => onThemeChange('light'))}
            >
              <Sun size={15} />
              <span>Sáng</span>
            </button>
            <button
              type="button"
              className={theme === 'dark' ? 'active' : ''}
              aria-pressed={theme === 'dark'}
              onClick={() => runViewTransition(() => onThemeChange('dark'))}
            >
              <Moon size={15} />
              <span>Tối</span>
            </button>
          </div>
        </div>
      </div>

      {/* Hidden file input for Image / Video */}
      <input
        type="file"
        ref={bgFileInputRef}
        style={{ display: 'none' }}
        accept="image/*,video/mp4,video/webm,video/ogg,video/quicktime"
        onChange={handleBackgroundFile}
      />

      <div className="bg-customizer-layout">
        <div className="bg-customizer-controls">
      {/* Current Background Preview & Controls */}
      {settings.backgroundUrl && settings.backgroundType !== 'NONE' ? (
        <div className="bg-preview-box">
          {settings.backgroundType === 'VIDEO' ? (
            <video
              src={settings.backgroundUrl}
              autoPlay
              loop
              muted
              playsInline
              className="bg-preview-media"
            />
          ) : (
            <img
              src={settings.backgroundUrl}
              alt="Background preview"
              className="bg-preview-media"
            />
          )}
          {/* Live overlay on preview (Phủ sáng ở light mode, Phủ tối ở dark mode) */}
          <div
            className="bg-preview-overlay"
            style={{
              backgroundColor: (typeof document !== 'undefined' && document.documentElement.getAttribute('data-theme') === 'dark')
                ? `rgba(10, 15, 30, ${Math.max(45, settings.overlayDarkness ?? 50) / 100})`
                : `rgba(248, 250, 252, ${Math.max(45, settings.overlayDarkness ?? 50) / 100})`,
            }}
          >
            <span className="bg-preview-badge">
              {settings.backgroundType === 'VIDEO' ? '▶ Video nền động' : ' Ảnh nền đang áp dụng'}
            </span>
            <span
              className="bg-preview-overlay-info"
              style={{
                color: (typeof document !== 'undefined' && document.documentElement.getAttribute('data-theme') === 'dark') ? '#FFFFFF' : '#17252A',
                backgroundColor: (typeof document !== 'undefined' && document.documentElement.getAttribute('data-theme') === 'dark')
                  ? 'rgba(15, 23, 42, .9)'
                  : 'rgba(255, 255, 255, .96)',
                fontWeight: 700,
              }}
            >
              Độ phủ {(typeof document !== 'undefined' && document.documentElement.getAttribute('data-theme') === 'dark') ? 'tối' : 'sáng'}: {Math.max(45, settings.overlayDarkness ?? 50)}%
            </span>
          </div>
        </div>
      ) : (
        <div className="bg-empty-box">
          <Palette size={26} style={{ opacity: 0.5, marginBottom: 4 }} />
          <div style={{ fontWeight: 600, fontSize: '0.86rem' }}>Chưa có hình nền cá nhân hóa</div>
          <div style={{ fontSize: '0.76rem', opacity: 0.8 }}>Ứng dụng đang dùng giao diện màu nền mặc định.</div>
        </div>
      )}

      {/* Actions: Upload or Remove */}
      <div className="bg-actions-row">
        <button
          type="button"
          className="btn-upload-bg"
          onClick={() => bgFileInputRef.current?.click()}
          title="Tải tệp ảnh hoặc video từ máy tính"
        >
          <Upload size={14} />
          <span>Tải ảnh / Video từ máy</span>
        </button>
        {settings.backgroundUrl && settings.backgroundType !== 'NONE' && (
          <button
            type="button"
            className="btn-remove-bg"
            onClick={handleRemoveBackground}
            title="Xóa hình nền quay về mặc định"
          >
            <Trash2 size={14} />
            <span>Xóa hình nền</span>
          </button>
        )}
      </div>

      {/* Overlay Slider (Độ phủ: Phủ sáng ở giao diện sáng, Phủ tối ở giao diện tối) */}
      <div className="darkness-slider-section">
        <div className="darkness-header">
          <div className="darkness-title-group">
            <span className="darkness-label">
              Độ phủ hình nền ({(typeof document !== 'undefined' && document.documentElement.getAttribute('data-theme') === 'dark') ? 'Phủ tối' : 'Phủ sáng'})
            </span>
          <span className="darkness-value-badge">{Math.max(45, settings.overlayDarkness ?? 50)}%</span>
          </div>
          <div className="darkness-hint">
            {(typeof document !== 'undefined' && document.documentElement.getAttribute('data-theme') === 'dark') ? (
              Math.max(45, settings.overlayDarkness ?? 50) === 50
                ? '50% (Đề xuất cho giao diện tối: nền chìm vừa đủ, chữ rõ)'
                : 'Lớp phủ tối luôn giữ tối thiểu 45% để chữ dễ đọc'
            ) : (
              Math.max(45, settings.overlayDarkness ?? 50) === 50
                ? '50% (Đề xuất cho giao diện sáng: ảnh nền dịu hơn, chữ dễ đọc)'
                : 'Lớp phủ sáng luôn giữ tối thiểu 45% để chữ dễ đọc'
            )}
          </div>
        </div>

        <div className="slider-container">
          <span className="slider-limit">45%</span>
          <input
            type="range"
            min="45"
            max="90"
            step="5"
            value={Math.max(45, settings.overlayDarkness ?? 50)}
            onChange={(e) => handleDarknessChange(parseInt(e.target.value, 10))}
            className="darkness-range-slider"
          />
          <span className="slider-limit">90%</span>
        </div>
      </div>
        </div>

      {/* Lịch sử hình nền & video cá nhân đã tải lên */}
      <div className="preset-bg-section">
        <div className="preset-bg-title">
          <Clock size={14} style={{ color: 'var(--primary)' }} />
          <span>Lịch sử hình nền &amp; video đã tải lên ({bgHistory.length}):</span>
        </div>
        {bgHistory.length === 0 ? (
          <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)', padding: '16px 8px', textAlign: 'center', background: 'var(--border-light)', borderRadius: 10 }}>
            Chưa có hình nền nào trong lịch sử. Hãy bấm <strong>"Tải ảnh / Video từ máy"</strong> để lưu trữ và đồng bộ qua mọi thiết bị!
          </div>
        ) : (
          <div className="preset-bg-grid">
            {bgHistory.map((bg) => {
              const isSelected = settings.backgroundUrl === bg.url;
              const isVideo = bg.mediaType === 'BACKGROUND_VIDEO';
              return (
                <div
                  key={bg.id}
                  className={`preset-bg-card ${isSelected ? 'selected' : ''}`}
                  onClick={() => handleSelectHistoryBg(bg)}
                  title={`Bấm để áp dụng (Tải lên ngày: ${formatDateDDMMYYYY(bg.createdAt)})`}
                  style={{ position: 'relative' }}
                >
                  {isVideo ? (
                    <video src={bg.url} className="preset-bg-thumb" muted />
                  ) : (
                    <img src={bg.url} alt="Hình nền lịch sử" className="preset-bg-thumb" />
                  )}
                  <div className="preset-bg-badge">
                    {isVideo ? '▶ Video' : 'Ảnh'}
                  </div>
                  <div className="preset-bg-name">{bg.fileName || (isVideo ? 'Video nền' : 'Ảnh nền')}</div>
                  {isSelected && (
                    <div className="preset-bg-check">
                      <Check size={12} />
                    </div>
                  )}
                  <button
                    type="button"
                    onClick={(e) => handleDeleteMediaHistory(e, bg.id, 'BACKGROUND')}
                    title="Xóa hình nền này khỏi lịch sử"
                    style={{
                      position: 'absolute',
                      top: 6,
                      right: 6,
                      width: 22,
                      height: 22,
                      borderRadius: 6,
                      background: 'rgba(0, 0, 0, 0.65)',
                      color: '#EF4444',
                      border: 'none',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      cursor: 'pointer',
                      zIndex: 3,
                    }}
                  >
                    <Trash2 size={12} />
                  </button>
                </div>
              );
            })}
          </div>
        )}
      </div>
      </div>
    </div>
  );

  // Card 5: Thông báo
  const notificationsCard = (
    <div className="settings-card notifications-card">
      <div className="settings-card-header">
        <div className="card-icon-badge bell-badge">
          <Bell size={18} />
        </div>
        <div>
          <h2 className="card-title">Thông báo</h2>
          <p className="card-subtitle">Đủ để nhớ, không làm gián đoạn.</p>
        </div>
      </div>

      <div className="notification-settings-columns">
        <div className="notification-reminder-column">
          <div className="form-group">
            <label className="form-label">Nhắc trước khi bắt đầu</label>
            <HoverSelect
              className="form-select"
              value={settings.reminderBefore}
              onChange={(e) => handleChange('reminderBefore', e.target.value)}
            >
              <option value="5 phút">5 phút</option>
              <option value="10 phút">10 phút</option>
              <option value="15 phút">15 phút</option>
              <option value="30 phút">30 phút</option>
              <option value="60 phút">60 phút</option>
            </HoverSelect>
          </div>

          <div className="dnd-section">
            <div className="dnd-header">
              <span className="dnd-label">Không làm phiền</span>
            </div>
            <div className="time-range-picker dnd-picker">
              <HoverSelect
                className="time-select"
                value={dndStart}
                onChange={(e) => updateDndHours(e.target.value, dndEnd)}
                aria-label="Giờ bắt đầu không làm phiền"
              >
                {getTimeOptions(dndStart).map((t) => <option key={t} value={t}>{t}</option>)}
              </HoverSelect>
              <span className="time-range-separator">–</span>
              <HoverSelect
                className="time-select"
                value={dndEnd}
                onChange={(e) => updateDndHours(dndStart, e.target.value)}
                aria-label="Giờ kết thúc không làm phiền"
              >
                {getTimeOptions(dndEnd).map((t) => <option key={t} value={t}>{t}</option>)}
              </HoverSelect>
              <Moon size={16} className="time-range-icon" />
            </div>
            <p className="dnd-note">SmartTask sẽ không gửi nhắc việc trong khung giờ này.</p>
          </div>
        </div>

        <div className="notification-options-column">
          <div className="toggle-row">
            <div className="toggle-text">
              <div className="toggle-title">Thông báo trên trình duyệt</div>
              <div className="toggle-subtitle">Nhắc công việc và cảnh báo trùng lịch.</div>
            </div>
            <label className="switch">
              <input
                type="checkbox"
                checked={settings.browserNotifications}
                onChange={(e) => handleChange('browserNotifications', e.target.checked)}
              />
              <span className="slider round"></span>
            </label>
          </div>

          <div className="toggle-row">
            <div className="toggle-text">
              <div className="toggle-title">Tóm tắt lịch mỗi sáng</div>
              <div className="toggle-subtitle">Gửi email lúc 07:30 mỗi ngày.</div>
            </div>
            <label className="switch">
              <input
                type="checkbox"
                checked={settings.morningSummaryEmail}
                onChange={(e) => handleChange('morningSummaryEmail', e.target.checked)}
              />
              <span className="slider round"></span>
            </label>
          </div>
        </div>
      </div>
    </div>
  );

  // Card 6: Bảo mật tài khoản
  const securityCard = (
    <div className="settings-card">
      <div className="settings-card-header">
        <div className="card-icon-badge shield-badge">
          <ShieldCheck size={18} />
        </div>
        <div>
          <h2 className="card-title">Bảo mật tài khoản</h2>
          <p className="card-subtitle">Dữ liệu lịch chỉ thuộc về bạn.</p>
        </div>
      </div>

      <div className="security-row">
        <div className="security-info">
          <span className="security-label">Mật khẩu</span>
          <span className="security-mask">••••••••</span>
        </div>
        <button
          type="button"
          className="btn-change-password"
          onClick={() => runViewTransition(() => setIsPasswordModalOpen(true))}
        >
          Đổi mật khẩu
        </button>
      </div>

      <div className="device-session-row">
        <div className="device-info">
          <div className="device-title">Chrome · Windows</div>
          <div className="device-subtitle">Phiên hiện tại · Thành phố Hồ Chí Minh</div>
        </div>
      </div>

      <div className="logout-row">
        <button type="button" className="btn-logout" onClick={onLogout}>
          <LogOut size={16} />
          <span>Đăng xuất</span>
        </button>
      </div>
    </div>
  );

  return (
    <div className="settings-page-wrapper">
      {/* ── Page Header ── */}
      <div className="settings-page-header">
        <div className="settings-page-title-group">
          <h1 className="settings-page-title">Tài khoản &amp; Thiết lập cá nhân</h1>
          <p className="settings-page-subtitle">
            Thiết lập kế hoạch làm việc của bạn cùng SmartTask. AI sẽ tôn trọng những giới hạn này khi đề xuất lịch.
          </p>
        </div>

        <div className="settings-status-badge">
          {isDirty ? (
            <span className="status-text dirty">Có thay đổi chưa lưu</span>
          ) : (
            <span className="status-text clean">Đã đồng bộ</span>
          )}
        </div>
      </div>

      {/* ── Browser-style Horizontal Tabs Bar ── */}
      <div className="settings-browser-tabs-container">
        <div className="settings-browser-tabs-bar" role="tablist">
          {SETTINGS_TABS.map((tab) => {
            const Icon = tab.icon;
            const isActive = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                type="button"
                role="tab"
                id={`settings-tab-${tab.id}`}
                aria-selected={isActive}
                className={`browser-tab ${isActive ? 'active' : ''}`}
                onClick={() => runViewTransition(() => setActiveTab(tab.id))}
              >
                <span className="browser-tab-icon">
                  <Icon size={16} />
                </span>
                <span className="browser-tab-label">{tab.label}</span>
              </button>
            );
          })}
        </div>
      </div>

      {/* ── Tab Content Views ── */}
      {activeTab === 'account' && (
        <div className="settings-grid settings-grid-account" key="tab-account">
          <div className="settings-col-left">
            {accountInfoCard}
          </div>
          <div className="settings-col-right">
            {securityCard}
          </div>
        </div>
      )}

      {activeTab === 'schedule' && (
        <div className="settings-grid settings-grid-schedule" key="tab-schedule">
          <div className="settings-col-left">
            {workHoursCard}
          </div>
        </div>
      )}

      {activeTab === 'ai' && (
        <div className="settings-single-tab-view" key="tab-ai">
          {aiControlCard}
        </div>
      )}

      {activeTab === 'appearance' && (
        <div className="settings-single-tab-view" key="tab-appearance">
          {appearanceCard}
        </div>
      )}

      {activeTab === 'notifications' && (
        <div className="settings-single-tab-view" key="tab-notifications">
          {notificationsCard}
        </div>
      )}

      {/* ── Bottom Sticky Action Bar ── */}
      <div className="settings-bottom-bar">
        <div className="bottom-bar-info">
          <Info size={16} className="info-icon" />
          <span>
            {isDirty
              ? 'Bạn có thay đổi chưa lưu. Lịch hiện tại không bị ảnh hưởng.'
              : isSavedRecently
                ? 'Tất cả thay đổi đã được lưu thành công!'
                : 'Các thay đổi đã được đồng bộ với hệ thống.'}
          </span>
        </div>

        <div className="bottom-bar-actions">
          <button
            type="button"
            className="btn-secondary"
            onClick={handleCancel}
          >
            Hủy thay đổi
          </button>

          <button
            type="button"
            className="btn-primary-save"
            onClick={handleSave}
          >
            <Check size={16} />
            <span>Lưu thiết lập</span>
          </button>
        </div>
      </div>

      {/* ── Password Change Modal ── */}
      {isPasswordModalOpen && typeof document !== 'undefined' && createPortal(
        <div className="password-modal-backdrop" onClick={() => runViewTransition(() => setIsPasswordModalOpen(false))}>
          <div className="password-modal" onClick={(e) => e.stopPropagation()}>
            <div className="password-modal-header">
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <Lock size={18} style={{ color: 'var(--primary)' }} />
                <h3 style={{ fontSize: '1.05rem', fontWeight: 600, margin: 0 }}>Đổi mật khẩu tài khoản</h3>
              </div>
              <button
                type="button"
                className="close-btn"
                onClick={() => runViewTransition(() => setIsPasswordModalOpen(false))}
              >
                <X size={18} />
              </button>
            </div>

            <form onSubmit={handleChangePassword}>
              <div style={{ padding: '20px 24px', display: 'flex', flexDirection: 'column', gap: 14 }}>
                {passwordError && (
                  <div className="password-alert error">{passwordError}</div>
                )}
                {passwordSuccess && (
                  <div className="password-alert success">Đổi mật khẩu thành công!</div>
                )}

                <div className="form-group">
                  <label className="form-label">Mật khẩu hiện tại</label>
                  <input
                    type="password"
                    className="form-input"
                    value={currentPassword}
                    onChange={(e) => setCurrentPassword(e.target.value)}
                    placeholder="Nhập mật khẩu hiện tại"
                    autoFocus
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Mật khẩu mới</label>
                  <input
                    type="password"
                    className="form-input"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    placeholder="Nhập mật khẩu mới"
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Xác nhận mật khẩu mới</label>
                  <input
                    type="password"
                    className="form-input"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    placeholder="Nhập lại mật khẩu mới"
                  />
                </div>
              </div>

              <div className="password-modal-footer">
                <button
                  type="button"
                  className="btn-secondary"
                  onClick={() => runViewTransition(() => setIsPasswordModalOpen(false))}
                >
                  Đóng
                </button>
                <button type="submit" className="btn-primary-save" disabled={isChangingPassword}>
                  {isChangingPassword ? 'Đang xác thực...' : 'Xác nhận đổi mật khẩu'}
                </button>
              </div>
            </form>
          </div>
        </div>,
        document.body
      )}

      {/* ── AI Workspace Privacy Permission Modal ── */}
      <AiPrivacyModal
        isOpen={showPrivacyModal}
        onClose={() => runViewTransition(() => setShowPrivacyModal(false))}
        workspaces={workspaces}
        activeWorkspace={null}
        onShowToast={onShowToast}
      />
    </div>
  );
}
