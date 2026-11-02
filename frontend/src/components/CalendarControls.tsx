import { useState, useRef, useEffect, useMemo } from 'react';
import {
  ChevronLeft,
  ChevronRight,
  ChevronDown,
  SlidersHorizontal,
  Plus,
  Zap,
  Check,
  CalendarClock,
  CheckCircle2,
  Tag,
} from 'lucide-react';
import { ViewMode, Category, TimeBlock } from '../types';
import EyeLockIcon from './EyeLockIcon';
import PrivacyCalloutPopover from './PrivacyCalloutPopover';

interface CalendarControlsProps {
  userName?: string;
  timeBlocks?: TimeBlock[];
  onSelectTask?: (task: TimeBlock) => void;
  currentWeekText: string;
  viewMode: ViewMode;
  onViewModeChange: (mode: ViewMode) => void;
  onPrevWeek: () => void;
  onNextWeek: () => void;
  onToday: () => void;
  categories: Category[];
  selectedCategory: string | null;
  onToggleCategory: (categoryName: string) => void;
  statusFilter: string;
  onStatusFilterChange: (status: string) => void;
  onAddTaskClick?: () => void;
  onAutoScheduleClick?: () => void;
  isPrivacyUnlocked?: boolean;
  isGlobalPrivacyUnlocked?: boolean;
  unlockedScheduleIds?: Set<number>;
  onUnlockSingle?: (id: number) => void;
  onTogglePrivacyLock?: () => void;
  onUnlockSuccess?: () => void;
}

const STATUS_OPTIONS = [
  { id: 'ALL', label: 'Tất cả trạng thái', dotColor: '#6C5CE7' },
  { id: 'TODO', label: 'Cần làm', dotColor: '#64748B' },
  { id: 'IN_PROGRESS', label: 'Đang làm', dotColor: '#D97706' },
  { id: 'OVERDUE', label: 'Đã qua', dotColor: '#EF4444' },
  { id: 'COMPLETED', label: 'Đã hoàn thành', dotColor: '#10B981' },
];

const formatTimeOnly = (isoString: string) => {
  try {
    const d = new Date(isoString);
    return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`;
  } catch {
    return '--:--';
  }
};

export default function CalendarControls({
  userName = 'Người dùng',
  timeBlocks = [],
  onSelectTask,
  currentWeekText = '12/10/2026 – 18/10/2026',
  viewMode,
  onViewModeChange,
  onPrevWeek,
  onNextWeek,
  onToday,
  categories,
  selectedCategory,
  onToggleCategory,
  statusFilter,
  onStatusFilterChange,
  onAddTaskClick,
  onAutoScheduleClick,
  isPrivacyUnlocked = false,
  isGlobalPrivacyUnlocked = false,
  unlockedScheduleIds,
  onUnlockSingle,
  onTogglePrivacyLock,
  onUnlockSuccess,
}: CalendarControlsProps) {
  const [isPrivacyPopoverOpen, setIsPrivacyPopoverOpen] = useState(false);
  const [activeLockedTaskId, setActiveLockedTaskId] = useState<number | null>(null);
  const [activeLockedTaskRect, setActiveLockedTaskRect] = useState<DOMRect | null>(null);
  const [isFilterOpen, setIsFilterOpen] = useState(false);
  const [shouldRenderFilter, setShouldRenderFilter] = useState(false);
  const [isFilterExiting, setIsFilterExiting] = useState(false);
  const filterMenuRef = useRef<HTMLDivElement>(null);
  const filterTimerRef = useRef<NodeJS.Timeout | null>(null);

  const [isCategoryOpen, setIsCategoryOpen] = useState(false);
  const [shouldRenderCategory, setShouldRenderCategory] = useState(false);
  const [isCategoryExiting, setIsCategoryExiting] = useState(false);
  const categoryMenuRef = useRef<HTMLDivElement>(null);
  const categoryTimerRef = useRef<NodeJS.Timeout | null>(null);

  // Auto-tick every 15s so upcoming task list refreshes smoothly in realtime
  const [currentTimeMs, setCurrentTimeMs] = useState(() => Date.now());

  useEffect(() => {
    const interval = setInterval(() => {
      setCurrentTimeMs(Date.now());
    }, 15000);
    return () => clearInterval(interval);
  }, []);

  // Filter dropdown open/close animation lifecycle
  useEffect(() => {
    if (isFilterOpen) {
      setShouldRenderFilter(true);
      setIsFilterExiting(false);
    } else if (shouldRenderFilter) {
      setIsFilterExiting(true);
      const timer = setTimeout(() => {
        setShouldRenderFilter(false);
        setIsFilterExiting(false);
      }, 190);
      return () => clearTimeout(timer);
    }
  }, [isFilterOpen, shouldRenderFilter]);

  const handleFilterMouseEnter = () => {
    if (filterTimerRef.current) clearTimeout(filterTimerRef.current);
    setIsFilterOpen(true);
  };

  const handleFilterMouseLeave = () => {
    if (filterTimerRef.current) clearTimeout(filterTimerRef.current);
    filterTimerRef.current = setTimeout(() => {
      setIsFilterOpen(false);
    }, 220);
  };

  useEffect(() => {
    if (!isFilterOpen) return;
    const handleOutsideClick = (e: MouseEvent) => {
      if (filterMenuRef.current && !filterMenuRef.current.contains(e.target as Node)) {
        setIsFilterOpen(false);
      }
    };
    document.addEventListener('mousedown', handleOutsideClick);
    return () => document.removeEventListener('mousedown', handleOutsideClick);
  }, [isFilterOpen]);

  // Category dropdown open/close animation lifecycle
  useEffect(() => {
    if (isCategoryOpen) {
      setShouldRenderCategory(true);
      setIsCategoryExiting(false);
    } else if (shouldRenderCategory) {
      setIsCategoryExiting(true);
      const timer = setTimeout(() => {
        setShouldRenderCategory(false);
        setIsCategoryExiting(false);
      }, 190);
      return () => clearTimeout(timer);
    }
  }, [isCategoryOpen, shouldRenderCategory]);

  const handleCategoryMouseEnter = () => {
    if (categoryTimerRef.current) clearTimeout(categoryTimerRef.current);
    setIsCategoryOpen(true);
  };

  const handleCategoryMouseLeave = () => {
    if (categoryTimerRef.current) clearTimeout(categoryTimerRef.current);
    categoryTimerRef.current = setTimeout(() => {
      setIsCategoryOpen(false);
    }, 220);
  };

  useEffect(() => {
    if (!isCategoryOpen) return;
    const handleOutsideClick = (e: MouseEvent) => {
      if (categoryMenuRef.current && !categoryMenuRef.current.contains(e.target as Node)) {
        setIsCategoryOpen(false);
      }
    };
    document.addEventListener('mousedown', handleOutsideClick);
    return () => document.removeEventListener('mousedown', handleOutsideClick);
  }, [isCategoryOpen]);

  // ── Tính toán công việc sắp tới hôm nay (Realtime không cache stale tasks) ──
  const now = new Date(currentTimeMs);
  const nowTime = currentTimeMs;
  const currentYMD = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;

  const todayTasks = useMemo(() => {
    return timeBlocks
      .filter((b) => {
        try {
          const d = new Date(b.startTime);
          const ymd = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
          return ymd === currentYMD;
        } catch {
          return false;
        }
      })
      .sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime());
  }, [timeBlocks, currentYMD]);

  // Công việc đang diễn ra: chỉ chọn công việc CHƯA hoàn thành và CHƯA hủy
  const currentTask = useMemo(() => {
    return todayTasks.find((t) => {
      const s = new Date(t.startTime).getTime();
      const e = new Date(t.endTime).getTime();
      return s <= nowTime && nowTime < e && t.status !== 'COMPLETED' && t.status !== 'CANCELLED';
    });
  }, [todayTasks, nowTime]);

  // Công việc sắp tới hôm nay: loại bỏ công việc đã hoàn thành hoặc đã hủy
  const upcomingTasks = useMemo(() => {
    return todayTasks.filter((t) => {
      if (currentTask && t.id === currentTask.id) return false;
      const s = new Date(t.startTime).getTime();
      return s > nowTime && t.status !== 'COMPLETED' && t.status !== 'CANCELLED';
    });
  }, [todayTasks, currentTask, nowTime]);

  const isTaskLocked = (task: TimeBlock) =>
    Boolean(task.isLocked && !isGlobalPrivacyUnlocked && !unlockedScheduleIds?.has(task.id));

  const openTaskUnlock = (task: TimeBlock, target: HTMLElement) => {
    setActiveLockedTaskId(task.id);
    setActiveLockedTaskRect(target.getBoundingClientRect());
  };

  const closeTaskUnlock = () => {
    setActiveLockedTaskId(null);
    setActiveLockedTaskRect(null);
  };

  const totalUpcomingCount = (currentTask ? 1 : 0) + upcomingTasks.length;

  return (
    <div className="calendar-controls-wrapper">
      {/* ── CỘT BÊN TRÁI: Lời chào + Hàng 1 (Date nav & 2 Action btns) + Hàng 2 (View switcher & 2 Filter btns) ── */}
      <div className="calendar-controls-left-col">
        {/* Lời chào trang chủ */}
        <div className="calendar-hero-greeting">
          <h1 className="hero-greeting-title">Xin chào {userName}</h1>
          <p className="hero-greeting-sub">Chúc bạn một ngày tốt lành !</p>
        </div>

        {/* Hàng 1: Điều hướng ngày bên trái, 2 nút tác vụ bên phải */}
        <div className="calendar-controls-row1">
          <div className="date-nav-group">
            <span className="current-date-range-text">{currentWeekText}</span>
            <div className="date-arrows">
              <button className="date-nav-btn" onClick={onPrevWeek} title="Khoảng trước" aria-label="Khoảng trước">
                <ChevronLeft size={16} />
              </button>
              <button className="date-nav-btn" onClick={onNextWeek} title="Khoảng sau" aria-label="Khoảng sau">
                <ChevronRight size={16} />
              </button>
            </div>
            <button className="today-btn" onClick={onToday}>
              Hôm nay
            </button>
          </div>

          <div className="action-buttons-group">
            {onAutoScheduleClick && (
              <button
                type="button"
                onClick={onAutoScheduleClick}
                className="auto-schedule-btn"
                title="Tự động xếp lịch thông minh 1 chạm bằng AI"
              >
                <Zap size={14} fill="currentColor" />
                <span className="btn-label-text">Tự động xếp lịch AI</span>
              </button>
            )}

            {onAddTaskClick && (
              <button className="add-task-btn" onClick={onAddTaskClick} id="add-task-btn" title="Thêm công việc mới">
                <Plus size={15} strokeWidth={2.5} />
                <span className="btn-label-text">Thêm công việc</span>
              </button>
            )}
          </div>
        </div>

        {/* Hàng 2: Chuyển Ngày/Tuần/Tháng bên trái, 2 nút Phân loại & Tất cả trạng thái bên phải */}
        <div className="calendar-controls-row2">
          <div className="view-toggle">
            <button
              className={`view-btn ${viewMode === 'day' ? 'active' : ''}`}
              onClick={() => onViewModeChange('day')}
            >
              Ngày
            </button>
            <button
              className={`view-btn ${viewMode === 'week' ? 'active' : ''}`}
              onClick={() => onViewModeChange('week')}
            >
              Tuần
            </button>
            <button
              className={`view-btn ${viewMode === 'month' ? 'active' : ''}`}
              onClick={() => onViewModeChange('month')}
            >
              Tháng
            </button>
          </div>

          <div className="filter-buttons-group">
            {/* 1. Nút Ổ khóa riêng tư (Icon gộp lock+eye, nằm bên trái 2 nút) */}
            <div className="privacy-lock-wrapper" style={{ position: 'relative' }}>
              <button
                type="button"
                className={`privacy-lock-btn ${isPrivacyUnlocked ? 'is-unlocked' : 'is-locked'}`}
                onClick={() => {
                  if (isPrivacyUnlocked) {
                    onTogglePrivacyLock?.();
                  } else {
                    setIsPrivacyPopoverOpen((prev) => !prev);
                  }
                }}
                title={
                  isPrivacyUnlocked
                    ? 'Lịch trình riêng tư đang MỞ. Nhấn để khóa lại ngay!'
                    : 'Lịch trình riêng tư đang KHÓA. Nhấn để mở khóa toàn bộ lịch trình!'
                }
              >
                <EyeLockIcon locked={!isPrivacyUnlocked} size={15} />
              </button>

              <PrivacyCalloutPopover
                isOpen={isPrivacyPopoverOpen}
                title="Vui lòng nhập mật khẩu để mở khóa toàn bộ lịch trình!"
                onClose={() => setIsPrivacyPopoverOpen(false)}
                onSuccess={() => {
                  setIsPrivacyPopoverOpen(false);
                  onUnlockSuccess?.();
                }}
              />
            </div>
            {/* 1. Nút Phân loại (Dropdown) */}
            <div
              className="category-dropdown-wrapper"
              ref={categoryMenuRef}
              onMouseEnter={handleCategoryMouseEnter}
              onMouseLeave={handleCategoryMouseLeave}
            >
              <button
                type="button"
                className="category-btn"
                onClick={() => setIsCategoryOpen(!isCategoryOpen)}
                title="Lọc theo nhóm phân loại"
              >
                {selectedCategory ? (
                  <span
                    className="category-dot"
                    style={{
                      background: categories.find((c) => c.name === selectedCategory)?.colorCode || 'var(--primary)',
                    }}
                  />
                ) : (
                  <Tag size={13} />
                )}
                <span className="btn-label-text">
                  {selectedCategory || 'Phân loại'}
                </span>
                <ChevronDown
                  size={13}
                  style={{
                    transform: isCategoryOpen ? 'rotate(180deg)' : 'none',
                    transition: 'transform 0.2s ease',
                  }}
                />
              </button>

              {shouldRenderCategory && (
                <div
                  className="category-dropdown-menu"
                  style={{
                    position: 'absolute',
                    top: 'calc(100% + 6px)',
                    right: 0,
                    width: 190,
                    background: 'var(--surface)',
                    border: '1px solid var(--border)',
                    borderRadius: 12,
                    boxShadow: '0 10px 25px rgba(0,0,0,0.18)',
                    padding: '6px 0',
                    zIndex: 1000,
                    display: 'flex',
                    flexDirection: 'column',
                    gap: 2,
                    animation: isCategoryExiting
                      ? 'dropdownFadeOut 0.2s cubic-bezier(0.16, 1, 0.3, 1) forwards'
                      : 'dropdownSpringDown 0.24s cubic-bezier(0.16, 1.25, 0.3, 1) both',
                  }}
                >
                  {/* Option: Tất cả phân loại */}
                  <button
                    type="button"
                    onClick={() => {
                      if (selectedCategory) {
                        onToggleCategory(selectedCategory);
                      }
                      setIsCategoryOpen(false);
                    }}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      padding: '8px 14px',
                      border: 'none',
                      background: !selectedCategory ? 'var(--primary-bg)' : 'transparent',
                      color: !selectedCategory ? 'var(--primary)' : 'var(--text)',
                      fontSize: '0.82rem',
                      fontWeight: !selectedCategory ? 700 : 500,
                      cursor: 'pointer',
                      textAlign: 'left',
                      transition: 'background 0.15s',
                    }}
                    onMouseEnter={(e) => {
                      if (selectedCategory) e.currentTarget.style.backgroundColor = 'var(--border-light)';
                    }}
                    onMouseLeave={(e) => {
                      if (selectedCategory) e.currentTarget.style.backgroundColor = 'transparent';
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      <Tag size={13} style={{ opacity: 0.7 }} />
                      <span>Tất cả phân loại</span>
                    </div>
                    {!selectedCategory && <Check size={14} style={{ color: 'var(--primary)', flexShrink: 0 }} />}
                  </button>

                  {/* Danh sách từng phân loại */}
                  {categories.map((cat) => {
                    const isSelected = selectedCategory === cat.name;
                    return (
                      <button
                        key={cat.id}
                        type="button"
                        onClick={() => {
                          onToggleCategory(cat.name);
                          setIsCategoryOpen(false);
                        }}
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'space-between',
                          padding: '8px 14px',
                          border: 'none',
                          background: isSelected ? 'var(--primary-bg)' : 'transparent',
                          color: isSelected ? 'var(--primary)' : 'var(--text)',
                          fontSize: '0.82rem',
                          fontWeight: isSelected ? 700 : 500,
                          cursor: 'pointer',
                          textAlign: 'left',
                          transition: 'background 0.15s',
                        }}
                        onMouseEnter={(e) => {
                          if (!isSelected) e.currentTarget.style.backgroundColor = 'var(--border-light)';
                        }}
                        onMouseLeave={(e) => {
                          if (!isSelected) e.currentTarget.style.backgroundColor = 'transparent';
                        }}
                      >
                        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                          <span
                            style={{
                              width: 8,
                              height: 8,
                              borderRadius: '50%',
                              backgroundColor: cat.colorCode,
                              flexShrink: 0,
                            }}
                          />
                          <span>{cat.name}</span>
                        </div>
                        {isSelected && <Check size={14} style={{ color: 'var(--primary)', flexShrink: 0 }} />}
                      </button>
                    );
                  })}
                </div>
              )}
            </div>

            {/* 2. Nút Tất cả trạng thái (Dropdown) */}
            <div
              className="filter-dropdown-wrapper"
              ref={filterMenuRef}
              onMouseEnter={handleFilterMouseEnter}
              onMouseLeave={handleFilterMouseLeave}
            >
              <button
                type="button"
                className="filter-btn"
                onClick={() => setIsFilterOpen(!isFilterOpen)}
                title="Lọc theo trạng thái công việc"
              >
                <SlidersHorizontal size={13} />
                <span className="btn-label-text">
                  {statusFilter === 'ALL' && 'Tất cả trạng thái'}
                  {statusFilter === 'TODO' && 'Cần làm'}
                  {statusFilter === 'IN_PROGRESS' && 'Đang làm'}
                  {statusFilter === 'OVERDUE' && 'Đã qua'}
                  {statusFilter === 'COMPLETED' && 'Đã hoàn thành'}
                </span>
                <ChevronDown
                  size={13}
                  style={{
                    transform: isFilterOpen ? 'rotate(180deg)' : 'none',
                    transition: 'transform 0.2s ease',
                  }}
                />
              </button>

              {shouldRenderFilter && (
                <div
                  className="filter-dropdown-menu"
                  style={{
                    position: 'absolute',
                    top: 'calc(100% + 6px)',
                    right: 0,
                    width: 200,
                    background: 'var(--surface)',
                    border: '1px solid var(--border)',
                    borderRadius: 12,
                    boxShadow: '0 10px 25px rgba(0,0,0,0.18)',
                    padding: '6px 0',
                    zIndex: 1000,
                    display: 'flex',
                    flexDirection: 'column',
                    gap: 2,
                    animation: isFilterExiting
                      ? 'dropdownFadeOut 0.2s cubic-bezier(0.16, 1, 0.3, 1) forwards'
                      : 'dropdownSpringDown 0.24s cubic-bezier(0.16, 1.25, 0.3, 1) both',
                  }}
                >
                  {STATUS_OPTIONS.map((opt) => {
                    const isSelected = statusFilter === opt.id;
                    return (
                      <button
                        key={opt.id}
                        type="button"
                        onClick={() => {
                          onStatusFilterChange(opt.id);
                          setIsFilterOpen(false);
                        }}
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'space-between',
                          padding: '8px 14px',
                          border: 'none',
                          background: isSelected ? 'var(--primary-bg)' : 'transparent',
                          color: isSelected ? 'var(--primary)' : 'var(--text)',
                          fontSize: '0.82rem',
                          fontWeight: isSelected ? 700 : 500,
                          cursor: 'pointer',
                          textAlign: 'left',
                          transition: 'background 0.15s',
                        }}
                        onMouseEnter={(e) => {
                          if (!isSelected) e.currentTarget.style.backgroundColor = 'var(--border-light)';
                        }}
                        onMouseLeave={(e) => {
                          if (!isSelected) e.currentTarget.style.backgroundColor = 'transparent';
                        }}
                      >
                        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                          <span
                            style={{
                              width: 8,
                              height: 8,
                              borderRadius: '50%',
                              backgroundColor: opt.dotColor,
                              flexShrink: 0,
                            }}
                          />
                          <span>{opt.label}</span>
                        </div>
                        {isSelected && <Check size={14} style={{ color: 'var(--primary)', flexShrink: 0 }} />}
                      </button>
                    );
                  })}
                </div>
              )}
            </div>

          </div>
        </div>
      </div>

      {/* ── CỘT BÊN PHẢI: Thẻ dọc "CÔNG VIỆC SẮP TỚI" kèm danh sách cuộn được ── */}
      <div className="upcoming-tasks-card" title="Danh sách công việc sắp tới hôm nay">
        <div className="upcoming-tasks-header">
          <div className="upcoming-tasks-title">
            <CalendarClock size={15} className="upcoming-icon-amber" />
            <span>CÔNG VIỆC SẮP TỚI</span>
          </div>
          {totalUpcomingCount > 0 && (
            <span className="upcoming-count-badge">
              {currentTask ? (isTaskLocked(currentTask) ? '1 đã khóa · ' : '1 đang làm · ') : ''}
              {upcomingTasks.length > 0 ? `${upcomingTasks.length} sắp tới` : '1 việc'}
            </span>
          )}
        </div>

        {/* Danh sách nhỏ cuộn được */}
        <div className="upcoming-tasks-scroll">
          {currentTask && (
            <div
              className={`upcoming-task-item${isTaskLocked(currentTask) ? '' : ' is-live'}`}
              onClick={(event) => {
                if (isTaskLocked(currentTask)) openTaskUnlock(currentTask, event.currentTarget);
                else onSelectTask?.(currentTask);
              }}
              title={isTaskLocked(currentTask)
                ? `Đã khóa (${formatTimeOnly(currentTask.startTime)}–${formatTimeOnly(currentTask.endTime)})`
                : `Đang diễn ra: ${currentTask.title} (${formatTimeOnly(currentTask.startTime)}–${formatTimeOnly(currentTask.endTime)})`}
            >
              <span className={`upcoming-dot${isTaskLocked(currentTask) ? '' : ' live-dot'}`} />
              <span className="upcoming-time">
                {isTaskLocked(currentTask)
                  ? `${formatTimeOnly(currentTask.startTime)}–${formatTimeOnly(currentTask.endTime)}`
                  : formatTimeOnly(currentTask.startTime)}
              </span>
              <span className="upcoming-name">{isTaskLocked(currentTask) ? 'Đã khóa' : currentTask.title}</span>
              {!isTaskLocked(currentTask) && <span className="upcoming-live-tag">Đang làm</span>}
            </div>
          )}

          {upcomingTasks.map((task) => (
            <div
              key={task.id}
              className="upcoming-task-item"
              onClick={(event) => {
                if (isTaskLocked(task)) openTaskUnlock(task, event.currentTarget);
                else onSelectTask?.(task);
              }}
              title={isTaskLocked(task)
                ? `Đã khóa (${formatTimeOnly(task.startTime)}–${formatTimeOnly(task.endTime)})`
                : `Sắp tới: ${task.title} (${formatTimeOnly(task.startTime)}–${formatTimeOnly(task.endTime)}) · Nhấp để xem`}
            >
              <span className="upcoming-dot" />
              <span className="upcoming-time">
                {isTaskLocked(task)
                  ? `${formatTimeOnly(task.startTime)}–${formatTimeOnly(task.endTime)}`
                  : formatTimeOnly(task.startTime)}
              </span>
              <span className="upcoming-name">{isTaskLocked(task) ? 'Đã khóa' : task.title}</span>
            </div>
          ))}

          {totalUpcomingCount === 0 && (
            <div className="upcoming-empty">
              <CheckCircle2 size={13} />
              <span>Không còn việc sắp tới hôm nay</span>
            </div>
          )}
        </div>
      </div>

      <PrivacyCalloutPopover
        isOpen={activeLockedTaskId !== null}
        title="Vui lòng nhập mật khẩu để mở khóa lịch trình này!"
        targetRect={activeLockedTaskRect}
        onClose={closeTaskUnlock}
        onSuccess={() => {
          const taskId = activeLockedTaskId;
          closeTaskUnlock();
          if (taskId !== null && onUnlockSingle) onUnlockSingle(taskId);
          else onUnlockSuccess?.();
        }}
      />
    </div>
  );
}
