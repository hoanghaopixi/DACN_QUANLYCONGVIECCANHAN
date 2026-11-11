import { CSSProperties, useState, useRef, useEffect, useLayoutEffect } from 'react';
import { createPortal } from 'react-dom';
import { TimeBlock, Category } from '../types';
import { Plus, Trash2, Copy, Check } from 'lucide-react';
import EyeLockIcon from './EyeLockIcon';
import PrivacyCalloutPopover from './PrivacyCalloutPopover';
import { formatDateDDMMYYYY } from '../utils/dateTimeFormat';

interface MonthCalendarProps {
  currentDate: Date;
  timeBlocks: (TimeBlock & { isRecurring?: boolean; subtext?: string; colorOverride?: string; priority?: string })[];
  categories: Category[];
  selectedCategory: string | null;
  statusFilter: string;
  onSelectBlock: (block: TimeBlock) => void;
  onAddAtDate: (dateStr: string) => void;
  onDeleteTask?: (id: number) => void;
  onToggleTaskStatus?: (id: number, status: 'TODO' | 'COMPLETED') => void;
  onDuplicateTask?: (task: TimeBlock) => void;
  onToggleTaskLock?: (id: number, locked: boolean) => void;
  isPrivacyUnlocked?: boolean;
  unlockedScheduleIds?: Set<number>;
  onUnlockSingle?: (id: number) => void;
  onUnlockSuccess?: () => void;
}

interface HoveredMonthDay {
  date: Date;
  dateStr: string;
  anchor: HTMLDivElement;
}

export default function MonthCalendar({
  currentDate,
  timeBlocks,
  categories,
  selectedCategory,
  statusFilter,
  onSelectBlock,
  onAddAtDate,
  onDeleteTask,
  onToggleTaskStatus,
  onDuplicateTask,
  onToggleTaskLock,
  isPrivacyUnlocked = false,
  unlockedScheduleIds,
  onUnlockSingle,
  onUnlockSuccess,
}: MonthCalendarProps) {
  const [hoveredDay, setHoveredDay] = useState<HoveredMonthDay | null>(null);
  const [popoverPosition, setPopoverPosition] = useState<CSSProperties | null>(null);
  const [isPopoverClosing, setIsPopoverClosing] = useState(false);
  const [isDaySwitching, setIsDaySwitching] = useState(false);
  const [isMonthPopoverPinned, setIsMonthPopoverPinned] = useState(false);
  const [activeLockedTaskId, setActiveLockedTaskId] = useState<number | null>(null);
  const [activeLockedTaskRect, setActiveLockedTaskRect] = useState<DOMRect | null>(null);
  const [hoveredActionTask, setHoveredActionTask] = useState<{ id: number; rect: DOMRect } | null>(null);
  const calendarWrapperRef = useRef<HTMLDivElement>(null);
  const popoverRef = useRef<HTMLDivElement>(null);
  const taskHoverCardRef = useRef<HTMLDivElement>(null);
  const hoverShieldRef = useRef<HTMLDivElement>(null);
  const hoverCloseTimer = useRef<number | null>(null);
  const actionCloseTimer = useRef<number | null>(null);
  const daySwitchTimer = useRef<number | null>(null);

  const clearHoverClose = () => {
    if (hoverCloseTimer.current !== null) {
      window.clearTimeout(hoverCloseTimer.current);
      hoverCloseTimer.current = null;
    }
  };

  const clearDaySwitchTimer = () => {
    if (daySwitchTimer.current !== null) {
      window.clearTimeout(daySwitchTimer.current);
      daySwitchTimer.current = null;
    }
  };

  const clearActionCloseTimer = () => {
    if (actionCloseTimer.current !== null) {
      window.clearTimeout(actionCloseTimer.current);
      actionCloseTimer.current = null;
    }
  };

  const scheduleHoverClose = () => {
    clearHoverClose();
    clearDaySwitchTimer();
    clearActionCloseTimer();
    setIsDaySwitching(false);
    setIsMonthPopoverPinned(false);
    setIsPopoverClosing(true);
    hoverCloseTimer.current = window.setTimeout(() => {
      setHoveredDay(null);
      setPopoverPosition(null);
      setIsPopoverClosing(false);
      hoverCloseTimer.current = null;
    }, 260);
  };

  const openLockedTaskUnlock = (taskId: number, target: HTMLElement) => {
    clearActionCloseTimer();
    clearHoverClose();
    setIsMonthPopoverPinned(true);
    setIsPopoverClosing(false);
    setActiveLockedTaskId(taskId);
    setActiveLockedTaskRect(target.getBoundingClientRect());
  };

  // Keep the day list mounted while the password callout is being used. Once it
  // closes, let the user move back to the list; otherwise dismiss it on pointer exit.
  useEffect(() => {
    if (!hoveredDay || !isMonthPopoverPinned || activeLockedTaskId !== null) return;

    const handlePointerMove = (event: PointerEvent) => {
      const target = event.target as Node | null;
      const overDayFloat = Boolean(target && popoverRef.current?.contains(target));
      const overTaskFloat = Boolean(target && taskHoverCardRef.current?.contains(target));
      const overShield = Boolean(target && hoverShieldRef.current?.contains(target));
      const overAnchor = Boolean(target && hoveredDay.anchor.contains(target));
      if (overDayFloat || overTaskFloat || overShield || overAnchor) {
        clearHoverClose();
        setIsPopoverClosing(false);
        setIsMonthPopoverPinned(overDayFloat || overTaskFloat || overShield);
        return;
      }
      scheduleHoverClose();
    };

    document.addEventListener('pointermove', handlePointerMove);
    return () => document.removeEventListener('pointermove', handlePointerMove);
  }, [hoveredDay, isMonthPopoverPinned, activeLockedTaskId]);

  useEffect(() => () => {
    clearHoverClose();
    clearDaySwitchTimer();
    clearActionCloseTimer();
  }, []);

  useLayoutEffect(() => {
    if (!hoveredDay) {
      setPopoverPosition(null);
      return;
    }

    const updatePopoverPosition = () => {
      const anchor = hoveredDay.anchor;
      const popover = popoverRef.current;
      if (!anchor.isConnected || !popover) return;

      const rect = anchor.getBoundingClientRect();
      const viewportWidth = document.documentElement.clientWidth;
      const viewportHeight = window.innerHeight;
      const edge = 10;
      // Slight overlap with the hovered day keeps the cursor from crossing
      // a dead gap before it reaches the floating day list.
      const gap = -6;
      if (rect.bottom <= edge || rect.top >= viewportHeight - edge) {
        scheduleHoverClose();
        return;
      }
      const spaceBelow = Math.max(0, viewportHeight - rect.bottom - edge - gap);
      const spaceAbove = Math.max(0, rect.top - edge - gap);
      const maxHeight = Math.max(1, Math.min(320, viewportHeight - edge * 2));
      const height = Math.min(popover.getBoundingClientRect().height, maxHeight);
      const opensUp = spaceBelow < height && spaceAbove > spaceBelow;
      const width = Math.max(1, Math.min(250, viewportWidth - edge * 2));
      const left = Math.max(edge, Math.min(rect.left, viewportWidth - width - edge));
      const preferredTop = opensUp ? rect.top - height - gap : rect.bottom + gap;
      const top = Math.max(edge, Math.min(preferredTop, viewportHeight - height - edge));

      setPopoverPosition((current) => {
        if (
          current?.top === top &&
          current.left === left &&
          current.width === width &&
          current.maxHeight === maxHeight &&
          current.transformOrigin === (opensUp ? 'bottom left' : 'top left')
        ) return current;
        return {
          position: 'fixed',
          top,
          left,
          width,
          maxHeight,
          zIndex: 99999,
          transformOrigin: opensUp ? 'bottom left' : 'top left',
        };
      });
    };

    updatePopoverPosition();
    window.addEventListener('resize', updatePopoverPosition);
    window.addEventListener('scroll', updatePopoverPosition, true);
    const observer = new ResizeObserver(updatePopoverPosition);
    observer.observe(hoveredDay.anchor);
    if (popoverRef.current) observer.observe(popoverRef.current);
    return () => {
      window.removeEventListener('resize', updatePopoverPosition);
      window.removeEventListener('scroll', updatePopoverPosition, true);
      observer.disconnect();
    };
  }, [hoveredDay]);
  const year = currentDate.getFullYear();
  const month = currentDate.getMonth();

  // First day of month
  const firstDay = new Date(year, month, 1);
  // Last day of month
  const lastDay = new Date(year, month + 1, 0);

  // Day of week for 1st day (0 is Sun, 1 is Mon, ...)
  const startDayOfWeek = firstDay.getDay();
  // Monday-based offset (0 for Mon, 6 for Sun)
  const mondayOffset = startDayOfWeek === 0 ? 6 : startDayOfWeek - 1;

  // Total days to display (padded to multiple of 7)
  const daysInMonth = lastDay.getDate();
  const totalSlots = Math.ceil((mondayOffset + daysInMonth) / 7) * 7;

  const now = new Date();
  const todayStr = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;

  const weekHeaders = ['Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy', 'Chủ Nhật'];

  // Filter blocks
  const filteredBlocks = timeBlocks.filter((b) => {
    if (statusFilter !== 'ALL') {
      if (statusFilter === 'OVERDUE') {
        const isPast = b.status === 'OVERDUE' || (b.status !== 'COMPLETED' && new Date(b.endTime || b.startTime).getTime() < Date.now());
        if (!isPast) return false;
      } else if (b.status !== statusFilter) {
        return false;
      }
    }
    if (selectedCategory) {
      const cat = categories.find((c) => c.name === selectedCategory);
      if (cat && b.categoryType !== cat.type) return false;
    }
    return true;
  });

  const hoveredDayTasks = hoveredDay
    ? filteredBlocks.filter((block) => {
        const date = new Date(block.startTime);
        const dateStr = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
        return dateStr === hoveredDay.dateStr;
      }).sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime())
    : [];

  const hoverShieldBounds = isMonthPopoverPinned && calendarWrapperRef.current
    ? (() => {
        const rect = calendarWrapperRef.current!.getBoundingClientRect();
        const padding = 120;
        const left = Math.max(0, rect.left - padding);
        const top = Math.max(0, rect.top - padding);
        const right = Math.min(document.documentElement.clientWidth, rect.right + padding);
        const bottom = Math.min(window.innerHeight, rect.bottom + padding);
        return { top, left, width: right - left, height: bottom - top };
      })()
    : null;

  const getCategoryColor = (type: string, override?: string) => {
    if (override) return override;
    switch (type) {
      case 'STUDY': return '#3B82F6';
      case 'WORK': return '#8B5CF6';
      case 'HEALTH': return '#10B981';
      case 'ENTERTAINMENT': return '#F59E0B';
      default: return '#6C5CE7';
    }
  };

  return (
    <div
      ref={calendarWrapperRef}
      className="month-calendar-wrapper"
      style={{
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden',
        border: '1px solid var(--border)',
        borderRadius: '14px 14px 0 0',
      }}
    >
      {/* ── Day Headers Wrapper ── */}
      <div
        className="calendar-header-wrapper"
        style={{
          display: 'flex',
          width: '100%',
          borderBottom: '1px solid var(--border)',
          flexShrink: 0,
          boxSizing: 'border-box',
        }}
      >
        <div
          className="calendar-header"
          style={{
            flex: 1,
            display: 'grid',
            gridTemplateColumns: 'repeat(7, minmax(0, 1fr))',
          }}
        >
          {weekHeaders.map((head, idx) => (
            <div
              key={head}
              style={{
                padding: '10px 8px',
                textAlign: 'center',
                fontSize: '0.78rem',
                fontWeight: 600,
                color: 'var(--text-muted)',
                borderRight: idx === 6 ? 'none' : '1px solid var(--border-light)',
                boxSizing: 'border-box',
                minWidth: 0,
                overflow: 'hidden',
                whiteSpace: 'nowrap',
                textOverflow: 'ellipsis',
              }}
            >
              {head}
            </div>
          ))}
        </div>
      </div>

      {/* ── Month Grid Body ── */}
      <div
        style={{
          flex: 1,
          minHeight: 0,
          display: 'grid',
          gridTemplateColumns: 'repeat(7, minmax(0, 1fr))',
          gridTemplateRows: `repeat(${totalSlots / 7}, minmax(0, 1fr))`,
          overflowY: 'hidden',
          overflowX: 'hidden',
        }}
      >
        {Array.from({ length: totalSlots }, (_, index) => {
          const dayNumber = index - mondayOffset + 1;
          const isCurrentMonth = dayNumber >= 1 && dayNumber <= daysInMonth;

          let cellDate: Date;
          if (isCurrentMonth) {
            cellDate = new Date(year, month, dayNumber);
          } else if (dayNumber < 1) {
            cellDate = new Date(year, month, dayNumber);
          } else {
            cellDate = new Date(year, month + 1, dayNumber - daysInMonth);
          }

          const cellDateStr = `${cellDate.getFullYear()}-${String(cellDate.getMonth() + 1).padStart(2, '0')}-${String(cellDate.getDate()).padStart(2, '0')}`;
          const isToday = cellDateStr === todayStr;

          // Find tasks on this date
          const dayTasks = filteredBlocks.filter((b) => {
            const d = new Date(b.startTime);
            const y = d.getFullYear();
            const m = String(d.getMonth() + 1).padStart(2, '0');
            const date = String(d.getDate()).padStart(2, '0');
            return `${y}-${m}-${date}` === cellDateStr;
          }).sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime());

          return (
            <div
              key={index}
              className={`month-day-cell${isToday ? ' is-today' : ''}${!isCurrentMonth ? ' is-outside-month' : ''}`}
              style={{
                borderRight: (index % 7 === 6) ? 'none' : '1px solid var(--border-light)',
                borderBottom: '1px solid var(--border-light)',
                boxSizing: 'border-box',
                padding: '6px 8px',
                minWidth: 0,
                minHeight: 0,
                overflow: 'hidden',
                background: isToday
                  ? 'color-mix(in srgb, var(--primary) 15%, var(--surface))'
                  : isCurrentMonth
                  ? 'transparent'
                  : 'color-mix(in srgb, var(--surface) 42%, transparent)',
                opacity: isCurrentMonth ? 1 : 0.76,
                display: 'flex',
                flexDirection: 'column',
                gap: 4,
                position: 'relative',
                transition: 'background 0.15s',
              }}
              onMouseEnter={(e) => {
                clearHoverClose();
                setHoveredActionTask(null);
                setIsPopoverClosing(false);
                if (activeLockedTaskId === null) setIsMonthPopoverPinned(false);
                if (dayTasks.length > 0) {
                  clearDaySwitchTimer();
                  if (hoveredDay && hoveredDay.dateStr !== cellDateStr) {
                    setIsDaySwitching(true);
                    daySwitchTimer.current = window.setTimeout(() => {
                      setIsDaySwitching(false);
                      daySwitchTimer.current = null;
                    }, 240);
                  } else {
                    setIsDaySwitching(false);
                  }
                  setHoveredDay({ date: cellDate, dateStr: cellDateStr, anchor: e.currentTarget });
                } else if (hoveredDay) {
                  scheduleHoverClose();
                }
              }}
              onMouseLeave={() => {
                if (hoveredDay?.dateStr === cellDateStr && !isMonthPopoverPinned && activeLockedTaskId === null) scheduleHoverClose();
              }}
            >
              {/* Day Header Row */}
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  marginBottom: 2,
                }}
              >
                <span
                  style={{
                    width: 24,
                    height: 24,
                    borderRadius: '50%',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontSize: '0.8rem',
                    fontWeight: isToday ? 700 : 600,
                    color: isToday ? 'var(--on-primary)' : 'var(--text)',
                    background: isToday ? 'var(--primary)' : 'transparent',
                  }}
                >
                  {cellDate.getDate()}
                </span>

                {isCurrentMonth && (
                  <button
                    type="button"
                    onClick={() => onAddAtDate(cellDateStr)}
                  title={`Thêm công việc ngày ${formatDateDDMMYYYY(cellDate)}`}
                    style={{
                      background: 'none',
                      border: 'none',
                      color: 'var(--text-muted)',
                      cursor: 'pointer',
                      padding: 2,
                      borderRadius: 4,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      opacity: 0.6,
                    }}
                  >
                    <Plus size={13} />
                  </button>
                )}
              </div>

              {/* Day Tasks Row: Chips on Left, +N Badge on Right */}
              <div
                style={{
                  display: 'flex',
                  alignItems: 'stretch',
                  gap: 4,
                  overflowY: 'hidden',
                  minWidth: 0,
                  width: '100%',
                  flex: 1,
                }}
              >
                {/* Left: Task chips stack (up to 3 items) */}
                <div
                  style={{
                    display: 'flex',
                    flexDirection: 'column',
                    gap: 3,
                    overflowY: 'hidden',
                    minWidth: 0,
                    flex: 1,
                  }}
                >
                  {dayTasks.slice(0, 3).map((task) => {
                    const color = getCategoryColor(task.categoryType, task.colorOverride);
                    const start = new Date(task.startTime);
                    const timeStr = `${start.getHours().toString().padStart(2, '0')}:${start.getMinutes().toString().padStart(2, '0')}`;
                    const isTaskLocked = Boolean(task.isLocked && !isPrivacyUnlocked && !(unlockedScheduleIds && unlockedScheduleIds.has(task.id)));
                    if (isTaskLocked) {
                      return (
                        <div
                          key={task.id}
                          className="month-task-chip locked-month-chip"
                          onClick={(e) => {
                            e.stopPropagation();
                            openLockedTaskUnlock(task.id, e.currentTarget);
                          }}
                          title="Vui lòng nhập mật khẩu để mở khóa lịch trình này!"
                          style={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: 4,
                            padding: '2px 5px',
                            borderRadius: 4,
                            fontSize: '0.72rem',
                            fontWeight: 600,
                            backgroundColor: 'rgba(148, 163, 184, 0.15)',
                            borderLeft: '2.5px solid #94A3B8',
                            color: 'var(--text-muted, #94A3B8)',
                            cursor: 'pointer',
                            whiteSpace: 'nowrap',
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                            minWidth: 0,
                            maxWidth: '100%',
                          }}
                        >
                          <EyeLockIcon locked={true} size={12} color="#EF4444" />
                          <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', flex: 1, minWidth: 0 }}>
                            Đã khóa
                          </span>
                        </div>
                      );
                    }

                    return (
                      <div
                        key={task.id}
                        className="month-task-chip"
                        onClick={(e) => {
                          e.stopPropagation();
                          onSelectBlock(task);
                        }}
                        title={`${task.title} (${timeStr})`}
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          gap: 4,
                          padding: '2px 5px',
                          borderRadius: 4,
                          fontSize: '0.72rem',
                          fontWeight: 500,
                          backgroundColor: `${color}26`,
                          borderLeft: `2.5px solid ${color}`,
                          color: 'var(--text)',
                          cursor: 'pointer',
                          whiteSpace: 'nowrap',
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                          minWidth: 0,
                          maxWidth: '100%',
                          transition: 'transform 0.1s',
                        }}
                      >
                        <span style={{ fontSize: '0.66rem', color: color, fontWeight: 700, flexShrink: 0 }}>
                          {timeStr}
                        </span>
                        <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', flex: 1, minWidth: 0 }}>
                          {task.title}
                        </span>
                      </div>
                    );
                  })}
                </div>

                {/* Right: +N Badge matching the user's design */}
                {dayTasks.length > 3 && (
                  <div
                    className="month-more-tasks-badge"
                    onClick={(e) => {
                      e.stopPropagation();
                      clearHoverClose();
                      const anchorEl = (e.currentTarget.closest('.month-day-cell') as HTMLDivElement) || e.currentTarget;
                      setHoveredDay({ date: cellDate, dateStr: cellDateStr, anchor: anchorEl });
                      setIsMonthPopoverPinned(true);
                    }}
                    title={`Còn ${dayTasks.length - 3} công việc nữa trong ngày ${formatDateDDMMYYYY(cellDate)} (Bấm để xem tất cả)`}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      minWidth: 26,
                      padding: '0 6px',
                      borderRadius: 8,
                      background: 'color-mix(in srgb, var(--primary) 22%, var(--surface))',
                      color: 'var(--primary)',
                      fontWeight: 800,
                      fontSize: '0.8rem',
                      letterSpacing: '-0.02em',
                      flexShrink: 0,
                      cursor: 'pointer',
                      userSelect: 'none',
                      transition: 'background 0.15s ease, border-color 0.15s ease',
                      transform: 'none',
                      border: '1px solid color-mix(in srgb, var(--primary) 32%, transparent)',
                    }}
                  >
                    +{dayTasks.length - 3}
                  </div>
                )}
              </div>
            </div>
          );
        })}
      </div>
      {hoverShieldBounds && typeof document !== 'undefined' && createPortal(
        <div
          ref={hoverShieldRef}
          aria-hidden="true"
          style={{
            position: 'fixed',
            top: hoverShieldBounds.top,
            left: hoverShieldBounds.left,
            width: hoverShieldBounds.width,
            height: hoverShieldBounds.height,
            zIndex: 99998,
            background: 'transparent',
            backdropFilter: 'none',
            WebkitBackdropFilter: 'none',
            pointerEvents: 'auto',
          }}
        />,
        document.body,
      )}
      {hoveredDay && typeof document !== 'undefined' && createPortal(
        <div
          ref={popoverRef}
          className={`month-day-popover${isPopoverClosing ? ' is-closing' : ''}${isDaySwitching ? ' is-day-switching' : ''}`}
          style={popoverPosition || {
            position: 'fixed',
            top: 0,
            left: 0,
            width: 250,
            maxHeight: 320,
            visibility: 'hidden',
            pointerEvents: 'none',
          }}
          onMouseEnter={() => {
            clearActionCloseTimer();
            clearHoverClose();
            setIsPopoverClosing(false);
            setIsMonthPopoverPinned(hoveredActionTask !== null);
          }}
          onMouseLeave={() => {
            if (!isMonthPopoverPinned && activeLockedTaskId === null) scheduleHoverClose();
          }}
          role="dialog"
          aria-label={`Công việc ngày ${formatDateDDMMYYYY(hoveredDay.date)}`}
        >
          <div key={hoveredDay.dateStr} className="month-day-popover-content">
            <div className="month-day-popover-header">
              <div>
                <strong>{new Intl.DateTimeFormat('vi-VN', { weekday: 'long' }).format(hoveredDay.date)}</strong>
                <span>{formatDateDDMMYYYY(hoveredDay.date)}</span>
              </div>
              <span className="month-day-popover-count">{hoveredDayTasks.length} việc</span>
            </div>
            <div className="month-day-popover-list">
              {hoveredDayTasks.map((task) => {
              const color = getCategoryColor(task.categoryType, task.colorOverride);
              const start = new Date(task.startTime);
              const end = new Date(task.endTime);
              const time = `${start.getHours().toString().padStart(2, '0')}:${start.getMinutes().toString().padStart(2, '0')} – ${end.getHours().toString().padStart(2, '0')}:${end.getMinutes().toString().padStart(2, '0')}`;
              const isTaskLocked = Boolean(task.isLocked && !isPrivacyUnlocked && !(unlockedScheduleIds && unlockedScheduleIds.has(task.id)));
              return (
                <div key={task.id} className="month-popover-task-wrap" onMouseEnter={() => clearActionCloseTimer()} onMouseLeave={() => {
                  actionCloseTimer.current = window.setTimeout(() => {
                    if (activeLockedTaskId !== null) return;
                    setHoveredActionTask(null);
                    setIsMonthPopoverPinned(false);
                    scheduleHoverClose();
                  }, 700);
                }}>
                <button
                  type="button"
                  className="month-day-popover-task"
                  onMouseMove={(event) => {
                    clearActionCloseTimer();
                    clearHoverClose();
                    setIsPopoverClosing(false);
                    setIsMonthPopoverPinned(true);
                    const rect = event.currentTarget.getBoundingClientRect();
                    setHoveredActionTask((current) => current?.id === task.id ? current : { id: task.id, rect });
                  }}
                  title={isTaskLocked ? 'Nhấn để nhập mật khẩu và xem lịch trình' : task.title}
                  onClick={(event) => {
                    if (isTaskLocked) {
                      openLockedTaskUnlock(task.id, event.currentTarget);
                    } else {
                      clearHoverClose();
                      setIsPopoverClosing(true);
                      hoverCloseTimer.current = window.setTimeout(() => {
                        setHoveredDay(null);
                        setPopoverPosition(null);
                        setIsPopoverClosing(false);
                        hoverCloseTimer.current = null;
                      }, 180);
                      onSelectBlock(task);
                    }
                  }}
                >
                  <span className="month-day-popover-color" style={{ background: isTaskLocked ? 'var(--text-muted)' : color }} />
                  <span className="month-day-popover-task-copy">
                    <span className="month-day-popover-time">{time}</span>
                    <span className="month-day-popover-title">
                      {isTaskLocked ? 'Nhập mật khẩu' : task.title}
                    </span>
                  </span>
                  {isTaskLocked && <EyeLockIcon locked size={14} color="var(--text-muted)" />}
                </button>
                </div>
              );
              })}
            </div>
          </div>
        </div>,
        document.body,
      )}
      {hoveredActionTask && hoveredDay && createPortal((() => {
        const task = hoveredDayTasks.find((item) => item.id === hoveredActionTask.id);
        if (!task) return null;
        const isTaskLocked = Boolean(task.isLocked && !isPrivacyUnlocked && !unlockedScheduleIds?.has(task.id));
        const width = 272;
        const height = 116;
        const rect = hoveredActionTask.rect;
        const pointsLeft = rect.right + width + 16 <= window.innerWidth;
        const left = pointsLeft ? rect.right + 16 : Math.max(8, rect.left - width - 16);
        const top = Math.max(8, Math.min(rect.top - 8, window.innerHeight - height - 8));
        const arrowY = Math.max(16, Math.min(height - 16, rect.top + rect.height / 2 - top));
        const start = new Date(task.startTime);
        const end = new Date(task.endTime);
        const categoryColor = getCategoryColor(task.categoryType, task.colorOverride);
        const timeRange = `${start.getHours().toString().padStart(2, '0')}:${start.getMinutes().toString().padStart(2, '0')} – ${end.getHours().toString().padStart(2, '0')}:${end.getMinutes().toString().padStart(2, '0')}`;
        const subtext = task.status === 'COMPLETED' ? 'Đã hoàn thành' : task.status === 'IN_PROGRESS' ? 'Đang làm' : 'Cần làm';
        return <div ref={taskHoverCardRef} className={`event-block is-hovered month-task-hover-card ${pointsLeft ? 'points-left' : 'points-right'}`} style={{ position: 'fixed', top, left, right: 'auto', width, minWidth: 236, maxWidth: width, height: 'auto', zIndex: 100001, borderLeft: `3px solid ${isTaskLocked ? '#64748B' : categoryColor}`, boxSizing: 'border-box', '--month-arrow-y': `${arrowY}px` } as CSSProperties} onMouseEnter={() => {
          clearActionCloseTimer();
          clearHoverClose();
          setIsPopoverClosing(false);
          setIsMonthPopoverPinned(true);
        }} onMouseLeave={() => {
          actionCloseTimer.current = window.setTimeout(() => {
            if (activeLockedTaskId !== null) return;
            setHoveredActionTask(null);
            setIsMonthPopoverPinned(false);
            scheduleHoverClose();
          }, 700);
        }} onClick={(event) => event.stopPropagation()}>
          <div className="event-title" style={{ fontWeight: 700 }}>{isTaskLocked ? 'Lịch trình đã khóa' : task.title}</div>
          <div className="event-time">{timeRange}</div>
          {!isTaskLocked && task.note?.trim() && <div className="event-note">💬 {task.note.trim()}</div>}
          {!isTaskLocked && <div className={`event-status ${task.status === 'COMPLETED' ? 'status-completed' : task.status === 'IN_PROGRESS' ? 'status-in_progress' : 'status-todo'}`}>{subtext}</div>}
          <div className="event-hover-bottom-bar" onClick={(event) => event.stopPropagation()}>
          {isTaskLocked ? (
            <button type="button" className="event-btn-action event-btn-unlock" title="Nhập mật khẩu để mở khóa lịch trình" onClick={(event) => openLockedTaskUnlock(task.id, event.currentTarget)}>
              <EyeLockIcon locked size={12} /><span>Mở khóa</span>
            </button>
          ) : <>
            <button type="button" className="event-btn-action event-btn-delete" title="Xóa công việc khỏi lịch" onClick={() => onDeleteTask?.(task.id)}><Trash2 size={11} /></button>
            <button type="button" className="event-btn-action event-btn-duplicate" title="Nhân bản công việc sang các ngày tiếp theo" onClick={() => onDuplicateTask?.(task)}><Copy size={11} /></button>
            <button type="button" className={`event-btn-action event-btn-complete ${task.status === 'COMPLETED' ? 'is-done' : ''}`} title="Đánh dấu hoàn tất công việc" onClick={() => onToggleTaskStatus?.(task.id, task.status === 'COMPLETED' ? 'TODO' : 'COMPLETED')}><Check size={11} /><span>{task.status === 'COMPLETED' ? 'Đã xong' : 'Hoàn tất'}</span></button>
            {task.isLocked && <button type="button" className="event-btn-action event-btn-unlock" title="Mở khóa chế độ riêng tư" onClick={() => onToggleTaskLock?.(task.id, false)}><EyeLockIcon locked={false} size={11} /><span>Mở khóa</span></button>}
          </>}
          </div>
        </div>;
      })(), document.body)}
      <PrivacyCalloutPopover
        isOpen={activeLockedTaskId !== null}
        title="Vui lòng nhập mật khẩu để mở khóa lịch trình này!"
        targetRect={activeLockedTaskRect}
        onClose={() => {
          setActiveLockedTaskId(null);
          setActiveLockedTaskRect(null);
        }}
        onSuccess={() => {
          const taskId = activeLockedTaskId;
          setActiveLockedTaskId(null);
          setActiveLockedTaskRect(null);
          if (taskId !== null && onUnlockSingle) onUnlockSingle(taskId);
          else onUnlockSuccess?.();
        }}
      />
    </div>
  );
}
