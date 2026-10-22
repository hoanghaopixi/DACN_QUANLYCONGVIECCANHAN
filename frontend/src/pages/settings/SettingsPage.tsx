import React, { useState, useEffect } from 'react';
import {
  User, Clock, Sparkles, Bell, ShieldCheck, Heart, Moon,
  Check, Info, LogOut, Lock, X
} from 'lucide-react';
import { AuthResponse } from '../../services/authService';
import './SettingsPage.css';

interface SettingsPageProps {
  currentUser: AuthResponse | null;
  onUpdateProfile?: (updated: { fullName: string; phone?: string }) => void;
  onLogout: () => void;
  onShowToast?: (msg: string) => void;
  onBackToCalendar?: () => void;
}

export interface UserSettingsData {
  fullName: string;
  email: string;
  phone: string;
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
  onUpdateProfile,
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
    };
  });

  const [settings, setSettings] = useState<UserSettingsData>(initialSettings);
  const [isDirty, setIsDirty] = useState(false);
  const [isSavedRecently, setIsSavedRecently] = useState(false);

  // Password Change Modal
  const [isPasswordModalOpen, setIsPasswordModalOpen] = useState(false);
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [passwordError, setPasswordError] = useState('');
  const [passwordSuccess, setPasswordSuccess] = useState(false);

  // Sync when currentUser changes
  useEffect(() => {
    if (currentUser) {
      setSettings((prev) => ({
        ...prev,
        fullName: currentUser.fullName || prev.fullName,
        email: currentUser.email || prev.email,
      }));
    }
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

  const toggleDay = (dayId: string) => {
    const currentDays = settings.workingDays;
    const exists = currentDays.includes(dayId);
    const updated = exists ? currentDays.filter((d) => d !== dayId) : [...currentDays, dayId];
    handleChange('workingDays', updated);
  };

  const handleSave = () => {
    localStorage.setItem('smarttask_settings_full', JSON.stringify(settings));
    // Also save compatible preferences object
    const legacyPrefs = {
      workStartTime: settings.workHours.split('-')[0]?.trim() || '08:00',
      workEndTime: settings.workHours.split('-')[1]?.trim() || '18:00',
      bufferTime: parseInt(settings.bufferMinutes, 10) || 15,
      workingDays: settings.workingDays,
    };
    localStorage.setItem('smarttask_preferences', JSON.stringify(legacyPrefs));

    if (onUpdateProfile) {
      onUpdateProfile({ fullName: settings.fullName, phone: settings.phone });
    }

    setInitialSettings(settings);
    setIsDirty(false);
    setIsSavedRecently(true);
    if (onShowToast) {
      onShowToast('Đã lưu thiết lập cá nhân thành công!');
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

  const handleChangePassword = (e: React.FormEvent) => {
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

    setPasswordSuccess(true);
    setTimeout(() => {
      setPasswordSuccess(false);
      setIsPasswordModalOpen(false);
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
      if (onShowToast) {
        onShowToast('Đổi mật khẩu thành công!');
      }
    }, 1000);
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

      {/* ── 2-Column Responsive Layout ── */}
      <div className="settings-grid">
        {/* ── Left Column ── */}
        <div className="settings-col-left">
          {/* Card 1: Thông tin tài khoản */}
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

            <div className="avatar-section">
              <div className="avatar-circle">{initials}</div>
              <div className="avatar-details">
                <div className="avatar-name">{settings.fullName}</div>
                <button
                  type="button"
                  className="change-avatar-btn"
                  onClick={() => {
                    const newName = prompt('Cập nhật họ và tên hiển thị:', settings.fullName);
                    if (newName && newName.trim()) {
                      handleChange('fullName', newName.trim());
                    }
                  }}
                >
                  Thay ảnh đại diện
                </button>
              </div>

              <div className="verified-badge">
                <Check size={12} />
                <span>Email đã xác minh</span>
              </div>
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
                <label className="form-label">Email</label>
                <input
                  type="email"
                  className="form-input"
                  value={settings.email}
                  onChange={(e) => handleChange('email', e.target.value)}
                  placeholder="name@example.com"
                />
              </div>

              <div className="form-group">
                <label className="form-label">Ngôn ngữ</label>
                <select
                  className="form-select"
                  value={settings.language}
                  onChange={(e) => handleChange('language', e.target.value)}
                >
                  <option value="Tiếng Việt">Tiếng Việt</option>
                  <option value="English">English</option>
                </select>
              </div>

              <div className="form-group">
                <label className="form-label">Múi giờ</label>
                <select
                  className="form-select"
                  value={settings.timezone}
                  onChange={(e) => handleChange('timezone', e.target.value)}
                >
                  <option value="(GMT+07:00) Hồ Chí Minh">(GMT+07:00) Hồ Chí Minh</option>
                  <option value="(GMT+07:00) Bangkok">(GMT+07:00) Bangkok</option>
                  <option value="(GMT+08:00) Singapore">(GMT+08:00) Singapore</option>
                  <option value="(GMT+09:00) Tokyo">(GMT+09:00) Tokyo</option>
                </select>
              </div>
            </div>
          </div>

          {/* Card 2: Giờ làm việc & khoảng nghỉ */}
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
                  <select
                    className="time-select"
                    value={workStart}
                    onChange={(e) => updateWorkHours(e.target.value, workEnd)}
                    aria-label="Giờ bắt đầu làm việc"
                  >
                    {getTimeOptions(workStart).map((t) => (
                      <option key={t} value={t}>{t}</option>
                    ))}
                  </select>
                  <span className="time-range-separator">–</span>
                  <select
                    className="time-select"
                    value={workEnd}
                    onChange={(e) => updateWorkHours(workStart, e.target.value)}
                    aria-label="Giờ kết thúc làm việc"
                  >
                    {getTimeOptions(workEnd).map((t) => (
                      <option key={t} value={t}>{t}</option>
                    ))}
                  </select>
                  <Clock size={16} className="time-range-icon" />
                </div>
              </div>

              <div className="form-group">
                <label className="form-label">Nghỉ trưa</label>
                <div className="time-range-picker">
                  <select
                    className="time-select"
                    value={lunchStart}
                    onChange={(e) => updateLunchBreak(e.target.value, lunchEnd)}
                    aria-label="Giờ bắt đầu nghỉ trưa"
                  >
                    {getTimeOptions(lunchStart).map((t) => (
                      <option key={t} value={t}>{t}</option>
                    ))}
                  </select>
                  <span className="time-range-separator">–</span>
                  <select
                    className="time-select"
                    value={lunchEnd}
                    onChange={(e) => updateLunchBreak(lunchStart, e.target.value)}
                    aria-label="Giờ kết thúc nghỉ trưa"
                  >
                    {getTimeOptions(lunchEnd).map((t) => (
                      <option key={t} value={t}>{t}</option>
                    ))}
                  </select>
                  <Clock size={16} className="time-range-icon" />
                </div>
              </div>

              <div className="form-group">
                <label className="form-label">Nghỉ giữa công việc</label>
                <select
                  className="form-select"
                  value={settings.bufferMinutes}
                  onChange={(e) => handleChange('bufferMinutes', e.target.value)}
                >
                  <option value="5 phút">5 phút</option>
                  <option value="10 phút">10 phút</option>
                  <option value="15 phút">15 phút</option>
                  <option value="20 phút">20 phút</option>
                  <option value="30 phút">30 phút</option>
                </select>
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

          {/* Card 3: Quyền kiểm soát trợ lý AI */}
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
            </div>
          </div>
        </div>

        {/* ── Right Column ── */}
        <div className="settings-col-right">
          {/* Card 1: Thông báo */}
          <div className="settings-card">
            <div className="settings-card-header">
              <div className="card-icon-badge bell-badge">
                <Bell size={18} />
              </div>
              <div>
                <h2 className="card-title">Thông báo</h2>
                <p className="card-subtitle">Đủ để nhớ, không làm gián đoạn.</p>
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">Nhắc trước khi bắt đầu</label>
              <select
                className="form-select"
                value={settings.reminderBefore}
                onChange={(e) => handleChange('reminderBefore', e.target.value)}
              >
                <option value="5 phút">5 phút</option>
                <option value="10 phút">10 phút</option>
                <option value="15 phút">15 phút</option>
                <option value="30 phút">30 phút</option>
                <option value="60 phút">60 phút</option>
              </select>
            </div>

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

            {/* Không làm phiền */}
            <div className="dnd-section">
              <div className="dnd-header">
                <span className="dnd-label">Không làm phiền</span>
              </div>
              <div className="time-range-picker dnd-picker">
                <select
                  className="time-select"
                  value={dndStart}
                  onChange={(e) => updateDndHours(e.target.value, dndEnd)}
                  aria-label="Giờ bắt đầu không làm phiền"
                >
                  {getTimeOptions(dndStart).map((t) => (
                    <option key={t} value={t}>{t}</option>
                  ))}
                </select>
                <span className="time-range-separator">–</span>
                <select
                  className="time-select"
                  value={dndEnd}
                  onChange={(e) => updateDndHours(dndStart, e.target.value)}
                  aria-label="Giờ kết thúc không làm phiền"
                >
                  {getTimeOptions(dndEnd).map((t) => (
                    <option key={t} value={t}>{t}</option>
                  ))}
                </select>
                <Moon size={16} className="time-range-icon" />
              </div>
              <p className="dnd-note">SmartTask sẽ không gửi nhắc việc trong khung giờ này.</p>
            </div>
          </div>

          {/* Card 2: Bảo mật tài khoản */}
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
                onClick={() => setIsPasswordModalOpen(true)}
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

          {/* Card 3: Một lịch trình vừa sức. */}
          <div className="settings-card inspiration-card">
            <div className="inspiration-icon-wrapper">
              <Heart size={20} className="inspiration-icon" />
            </div>
            <h3 className="inspiration-title">Một lịch trình vừa sức.</h3>
            <p className="inspiration-body">
              Giờ nghỉ cũng quan trọng như giờ làm. SmartTask không xếp việc vào giờ nghỉ trưa và luôn giữ lại thời gian chuyển tiếp.
            </p>
            <div className="inspiration-footer">Cài đặt áp dụng cho các đề xuất mới.</div>
          </div>
        </div>
      </div>

      {/* ── Bottom Sticky Action Bar ── */}
      <div className="settings-bottom-bar">
        <div className="bottom-bar-info">
          <Info size={16} className="info-icon" />
          <span>
            {isDirty
              ? 'Bạn có thay đổi chưa lưu. Lịch hiện tại không bị ảnh hưởng.'
              : isSavedRecently
              ? 'Tất cả thay đổi đã được lưu thành công!'
              : 'Tất cả thiết lập đã được đồng bộ với hệ thống.'}
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
      {isPasswordModalOpen && (
        <div className="password-modal-backdrop" onClick={() => setIsPasswordModalOpen(false)}>
          <div className="password-modal" onClick={(e) => e.stopPropagation()}>
            <div className="password-modal-header">
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <Lock size={18} style={{ color: 'var(--primary)' }} />
                <h3 style={{ fontSize: '1.05rem', fontWeight: 600, margin: 0 }}>Đổi mật khẩu tài khoản</h3>
              </div>
              <button
                type="button"
                className="close-btn"
                onClick={() => setIsPasswordModalOpen(false)}
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
                  onClick={() => setIsPasswordModalOpen(false)}
                >
                  Đóng
                </button>
                <button type="submit" className="btn-primary-save">
                  Xác nhận đổi mật khẩu
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
