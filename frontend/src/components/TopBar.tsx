import { useState, useEffect, useCallback, useRef, useMemo } from 'react';
import { Search, Bell, ChevronRight, LogIn, UserCheck, X, Clock, Check } from 'lucide-react';
import ThemeToggle from './ThemeToggle';
import NotificationDropdown from './NotificationDropdown';
import { notificationService } from '../services/notificationService';
import { TimeBlock } from '../types';
import { matchesSearch } from '../utils/vietnameseSearch';

interface TopBarProps {
  currentDateText?: string;
  searchQuery: string;
  onSearchChange: (query: string) => void;
  theme: 'light' | 'dark';
  onToggleTheme: () => void;
  currentUser: { email: string; fullName: string; role: string; avatarUrl?: string } | null;
  onOpenAuthModal: () => void;
  onSelectTask?: (taskId: number) => void;
  onSelectSearchTask?: (taskId: number) => void;
  tasks?: TimeBlock[];
  workspaceName?: string;
  activeNavTitle?: string;
}

function formatGmt7Time(date: Date): string {
  const parts = new Intl.DateTimeFormat('vi-VN', {
    timeZone: 'Asia/Ho_Chi_Minh',
    weekday: 'long',
    day: 'numeric',
    month: 'numeric',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
  }).formatToParts(date);
  const part = (type: Intl.DateTimeFormatPartTypes) => parts.find((item) => item.type === type)?.value ?? '';
  return `${part('weekday')}, ${part('day').padStart(2, '0')}/${part('month').padStart(2, '0')}/${part('year')} · ${part('hour')}:${part('minute')} (GMT+7)`;
}

export default function TopBar({
  searchQuery,
  onSearchChange,
  theme,
  onToggleTheme,
  currentUser,
  onOpenAuthModal,
  onSelectTask,
  onSelectSearchTask,
  tasks = [],
  workspaceName = 'Không gian cá nhân',
  activeNavTitle = 'Lịch của tôi',
}: TopBarProps) {
  const [isNotificationOpen, setIsNotificationOpen] = useState(false);
  const [unreadCount, setUnreadCount] = useState<number>(0);
  const [isSearchOpen, setIsSearchOpen] = useState(false);
  const [isSearchDropdownMounted, setIsSearchDropdownMounted] = useState(false);
  const [isSearchDropdownVisible, setIsSearchDropdownVisible] = useState(false);
  const [activeSearchIndex, setActiveSearchIndex] = useState(0);
  const notifTimerRef = useRef<NodeJS.Timeout | null>(null);
  const searchContainerRef = useRef<HTMLDivElement | null>(null);

  const searchResults = useMemo(() => {
    const query = searchQuery.trim();
    if (!query) return [];

    const now = Date.now();
    const getDeadline = (task: TimeBlock) => {
      const value = new Date(task.endTime).getTime();
      return Number.isFinite(value) ? value : Number.MAX_SAFE_INTEGER;
    };

    return tasks
      .filter((task) => matchesSearch(task.title, query) || matchesSearch(task.description, query) || matchesSearch(task.note, query))
      .sort((a, b) => {
        const deadlineA = getDeadline(a);
        const deadlineB = getDeadline(b);
        const overdueA = deadlineA < now;
        const overdueB = deadlineB < now;
        if (overdueA !== overdueB) return overdueA ? 1 : -1;
        // Show the nearest upcoming deadline first; among overdue tasks, the least overdue first.
        return overdueA ? deadlineB - deadlineA : deadlineA - deadlineB;
      });
  }, [searchQuery, tasks]);
  const visibleSearchResults = searchResults.slice(0, 8);

  const formatDeadline = (value: string) => {
    const date = new Date(value);
    if (!Number.isFinite(date.getTime())) return 'Chưa có hạn';
    const dateText = new Intl.DateTimeFormat('vi-VN', {
      day: '2-digit', month: '2-digit', year: 'numeric',
    }).format(date);
    const timeText = new Intl.DateTimeFormat('vi-VN', {
      hour: '2-digit', minute: '2-digit', hourCycle: 'h23',
    }).format(date);
    return `${dateText} · ${timeText}`;
  };

  const selectSearchResult = (index: number) => {
    const task = searchResults[index];
    if (!task) return;
    (onSelectSearchTask || onSelectTask)?.(task.id);
    onSearchChange('');
    setIsSearchOpen(false);
    setActiveSearchIndex(0);
  };

  const handleSearchKeyDown = (event: React.KeyboardEvent<HTMLInputElement>) => {
    if (event.key === 'Escape') {
      setIsSearchOpen(false);
      return;
    }
    if (event.key === 'ArrowDown' && visibleSearchResults.length) {
      event.preventDefault();
      setIsSearchOpen(true);
      setActiveSearchIndex((index) => (index + 1) % visibleSearchResults.length);
    } else if (event.key === 'ArrowUp' && visibleSearchResults.length) {
      event.preventDefault();
      setIsSearchOpen(true);
      setActiveSearchIndex((index) => (index - 1 + visibleSearchResults.length) % visibleSearchResults.length);
    } else if (event.key === 'Enter' && isSearchOpen && visibleSearchResults.length) {
      event.preventDefault();
      selectSearchResult(activeSearchIndex);
    }
  };

  useEffect(() => {
    if (!isSearchOpen) return;
    const handleOutsideClick = (event: MouseEvent) => {
      if (!searchContainerRef.current?.contains(event.target as Node)) setIsSearchOpen(false);
    };
    document.addEventListener('mousedown', handleOutsideClick);
    return () => document.removeEventListener('mousedown', handleOutsideClick);
  }, [isSearchOpen]);

  useEffect(() => {
    setActiveSearchIndex(0);
    setIsSearchOpen(!!searchQuery.trim());
  }, [searchQuery]);

  useEffect(() => {
    let frame = 0;
    let timeout = 0;
    if (isSearchOpen && searchQuery.trim()) {
      setIsSearchDropdownMounted(true);
      frame = window.requestAnimationFrame(() => setIsSearchDropdownVisible(true));
    } else {
      setIsSearchDropdownVisible(false);
      timeout = window.setTimeout(() => setIsSearchDropdownMounted(false), 240);
    }
    return () => {
      if (frame) window.cancelAnimationFrame(frame);
      if (timeout) window.clearTimeout(timeout);
    };
  }, [isSearchOpen, searchQuery]);

  const handleNotifMouseEnter = () => {
    if (notifTimerRef.current) clearTimeout(notifTimerRef.current);
    setIsNotificationOpen(true);
  };

  const handleNotifMouseLeave = () => {
    if (notifTimerRef.current) clearTimeout(notifTimerRef.current);
    notifTimerRef.current = setTimeout(() => {
      setIsNotificationOpen(false);
    }, 220);
  };

  const loadUnreadCount = useCallback(async () => {
    try {
      const count = await notificationService.getUnreadCount();
      setUnreadCount(count);
    } catch (err) {
      console.error('Lỗi lấy số thông báo chưa đọc từ CSDL:', err);
    }
  }, []);

  useEffect(() => {
    loadUnreadCount();
    const interval = setInterval(loadUnreadCount, 25000);
    return () => clearInterval(interval);
  }, [currentUser, loadUnreadCount]);

  // Live GMT+7 Clock
  const [timeText, setTimeText] = useState(() => formatGmt7Time(new Date()));

  useEffect(() => {
    const interval = setInterval(() => {
      setTimeText(formatGmt7Time(new Date()));
    }, 15000);
    return () => {
      clearInterval(interval);
      if (notifTimerRef.current) clearTimeout(notifTimerRef.current);
    };
  }, []);

  // Close notification dropdown when clicking outside
  useEffect(() => {
    if (!isNotificationOpen) return;
    const handleClickOutside = (e: MouseEvent) => {
      const target = e.target as HTMLElement;
      if (!target.closest('.notification-container')) {
        setIsNotificationOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [isNotificationOpen]);

  return (
    <header className="top-bar">
      {/* ── Breadcrumb ── */}
      <div className="breadcrumb">
        <span>{workspaceName}</span>
        <ChevronRight size={13} className="breadcrumb-sep" />
        <span className="breadcrumb-current">{activeNavTitle}</span>
      </div>

      {/* ── Actions: Search, Theme Toggle, Notification, Auth, Date ── */}
      <div className="top-bar-actions">
        {/* Search box */}
        <div className="search-box search-box-with-results" ref={searchContainerRef}>
          <Search size={14} style={{ color: '#94A3B8', flexShrink: 0 }} />
          <input
            type="text"
            placeholder="Tìm công việc, lịch hẹn..."
            value={searchQuery}
            onChange={(e) => onSearchChange(e.target.value)}
            onFocus={() => searchQuery.trim() && setIsSearchOpen(true)}
            onKeyDown={handleSearchKeyDown}
            role="combobox"
            aria-autocomplete="list"
            aria-expanded={isSearchOpen && !!searchQuery.trim()}
            aria-controls="task-search-results"
          />
          {searchQuery && (
            <button
              type="button"
              onClick={() => onSearchChange('')}
              title="Xóa tìm kiếm"
              style={{
                background: 'none',
                border: 'none',
                color: 'var(--text-muted)',
                cursor: 'pointer',
                padding: 2,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <X size={13} />
            </button>
          )}
          {isSearchDropdownMounted && (
            <div
              className={`task-search-dropdown${isSearchDropdownVisible ? ' is-visible' : ''}`}
              id="task-search-results"
              role="listbox"
              aria-label="Kết quả công việc"
              aria-hidden={!isSearchDropdownVisible}
            >
              {isSearchOpen && <>
              <div className="task-search-heading">
                <span>Công việc phù hợp</span>
                <span>{searchResults.length}</span>
              </div>
              {searchResults.length ? visibleSearchResults.map((task, index) => (
                <button
                  type="button"
                  key={task.id}
                  className={`task-search-result${index === activeSearchIndex ? ' is-active' : ''}`}
                  role="option"
                  aria-selected={index === activeSearchIndex}
                  onMouseEnter={() => setActiveSearchIndex(index)}
                  onMouseDown={(event) => event.preventDefault()}
                  onClick={() => selectSearchResult(index)}
                >
                  <span className="task-search-result-icon">
                    {task.status === 'COMPLETED' ? <Check size={15} /> : <Clock size={15} />}
                  </span>
                  <span className="task-search-result-copy">
                    <span className="task-search-result-title">{task.title}</span>
                    <span className="task-search-result-meta">{formatDeadline(task.endTime)}</span>
                  </span>
                  <span className={`task-search-result-status status-${task.status.toLowerCase()}`}>
                    {task.status === 'COMPLETED' ? 'Hoàn thành' : task.status === 'OVERDUE' || new Date(task.endTime).getTime() < Date.now() ? 'Quá hạn' : 'Hạn gần nhất'}
                  </span>
                </button>
              )) : (
                <div className="task-search-empty">Không tìm thấy công việc phù hợp.</div>
              )}
              {searchResults.length > 8 && <div className="task-search-more">Hiển thị 8 trong {searchResults.length} kết quả</div>}
              <div className="task-search-hint">↑ ↓ di chuyển · Enter để mở · Esc để đóng</div>
              </>}
            </div>
          )}
        </div>

        {/* Cần gạt Dark Mode / Light Mode hiệu ứng đẹp */}
        <ThemeToggle theme={theme} onToggle={onToggleTheme} />

        {/* Notifications Bell Container */}
        <div
          className="notification-container"
          style={{ position: 'relative' }}
          onMouseEnter={handleNotifMouseEnter}
          onMouseLeave={handleNotifMouseLeave}
        >
          <button
            className="icon-btn"
            title={unreadCount > 0 ? `Quản lý thông báo (${unreadCount} chưa đọc)` : 'Quản lý thông báo'}
            aria-label="Thông báo"
            onClick={() => setIsNotificationOpen(!isNotificationOpen)}
          >
            <Bell size={16} />
            {unreadCount > 0 && (
              <span
                className="notification-dot"
                title={`${unreadCount} thông báo chưa đọc`}
              >
                {unreadCount > 99 ? '99+' : unreadCount}
              </span>
            )}
          </button>

          {/* Interactive Notifications Panel */}
          <NotificationDropdown
            isOpen={isNotificationOpen}
            onClose={() => setIsNotificationOpen(false)}
            onSelectTask={onSelectTask}
            onUnreadCountChange={(count) => setUnreadCount(count)}
          />
        </div>

        {/* Auth button */}
        {currentUser ? (
          <button
            onClick={onOpenAuthModal}
            className="auth-status-btn logged-in"
            title={`Đang đăng nhập: ${currentUser.email} (${currentUser.role})`}
          >
            {currentUser.avatarUrl ? (
              <img
                src={currentUser.avatarUrl}
                alt={currentUser.fullName}
                style={{
                  width: 20,
                  height: 20,
                  borderRadius: '50%',
                  objectFit: 'cover',
                  border: '1.5px solid var(--primary)',
                  flexShrink: 0,
                  display: 'block',
                }}
              />
            ) : (
              <UserCheck size={14} style={{ color: 'var(--success)' }} />
            )}
            <span className="auth-user-name">{currentUser.fullName}</span>
          </button>
        ) : (
          <button
            onClick={onOpenAuthModal}
            className="auth-status-btn login-btn"
            title="Đăng nhập / Đăng ký"
          >
            <LogIn size={14} />
            <span>Đăng nhập</span>
          </button>
        )}

        {/* Chuẩn giờ GMT+7 thời gian thực */}
        <div className="date-display" title="Giờ địa phương GMT+7 (Asia/Ho_Chi_Minh)">
          {timeText}
        </div>
      </div>
    </header>
  );
}
