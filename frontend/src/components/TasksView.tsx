import { useEffect, useRef, useState } from 'react';
import { flushSync } from 'react-dom';
import {
  CheckCircle2, Circle, Clock, Tag, AlertCircle, Plus,
  Search, Edit3, Trash2, CheckSquare, Flame, ChevronDown, Check
} from 'lucide-react';
import { TimeBlock, Category, EisenhowerQuadrant } from '../types';
import { matchesSearch } from '../utils/vietnameseSearch';
import { formatDateDDMMYYYY } from '../utils/dateTimeFormat';
import ConfirmModal from './ConfirmModal';
import EisenhowerMatrix from './EisenhowerMatrix';
import HoverSelect from './HoverSelect';

interface TasksViewProps {
  tasks: (TimeBlock & { isRecurring?: boolean; subtext?: string; colorOverride?: string; priority?: string })[];
  categories: Category[];
  onAddTask: () => void;
  onAddQuadrantTask?: (quadrant: EisenhowerQuadrant) => void;
  onEditTask: (task: TimeBlock) => void;
  onDeleteTask: (id: number) => void;
  onToggleStatus: (task: TimeBlock) => void;
  onShowToast?: (msg: string) => void;
  onReload?: () => void;
}

export default function TasksView({
  tasks,
  categories,
  onAddTask,
  onAddQuadrantTask,
  onEditTask,
  onDeleteTask,
  onToggleStatus,
  onShowToast,
  onReload,
}: TasksViewProps) {
  const [viewLayout, setViewLayout] = useState<'LIST' | 'EISENHOWER'>('LIST');
  const [isLayoutTransitioning, setIsLayoutTransitioning] = useState(false);
  const layoutStageRef = useRef<HTMLDivElement | null>(null);
  const [isLayoutMenuOpen, setIsLayoutMenuOpen] = useState(false);
  const [shouldRenderLayoutMenu, setShouldRenderLayoutMenu] = useState(false);
  const [isLayoutMenuExiting, setIsLayoutMenuExiting] = useState(false);
  const layoutMenuRef = useRef<HTMLDivElement | null>(null);
  const layoutMenuTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const [taskToDelete, setTaskToDelete] = useState<TimeBlock | null>(null);
  const [search, setSearch] = useState('');
  const [statusTab, setStatusTab] = useState<'ALL' | 'TODO' | 'IN_PROGRESS' | 'OVERDUE' | 'COMPLETED' | 'HABITS'>('ALL');
  const [selectedPriority, setSelectedPriority] = useState<string>('ALL');

  useEffect(() => () => {
    if (layoutMenuTimer.current) clearTimeout(layoutMenuTimer.current);
  }, []);

  useEffect(() => {
    if (isLayoutMenuOpen) {
      setShouldRenderLayoutMenu(true);
      setIsLayoutMenuExiting(false);
      return;
    }
    if (!shouldRenderLayoutMenu) return;
    setIsLayoutMenuExiting(true);
    const timeout = setTimeout(() => {
      setShouldRenderLayoutMenu(false);
      setIsLayoutMenuExiting(false);
    }, 190);
    return () => clearTimeout(timeout);
  }, [isLayoutMenuOpen, shouldRenderLayoutMenu]);

  useEffect(() => {
    if (!isLayoutMenuOpen) return;
    const closeOnOutsideClick = (event: MouseEvent) => {
      if (!layoutMenuRef.current?.contains(event.target as Node)) setIsLayoutMenuOpen(false);
    };
    document.addEventListener('mousedown', closeOnOutsideClick);
    return () => document.removeEventListener('mousedown', closeOnOutsideClick);
  }, [isLayoutMenuOpen]);

  const handleViewLayoutChange = (nextLayout: 'LIST' | 'EISENHOWER') => {
    if (nextLayout === viewLayout || isLayoutTransitioning) return;
    setIsLayoutMenuOpen(false);

    const stage = layoutStageRef.current;
    if (!stage?.animate) {
      setViewLayout(nextLayout);
      return;
    }

    setIsLayoutTransitioning(true);
    stage.getAnimations().forEach((animation) => animation.cancel());
    const exitAnimation = stage.animate(
      [
        { opacity: 1, transform: 'scale(1)' },
        { opacity: 0, transform: 'scale(.92)' },
      ],
      { duration: 220, easing: 'cubic-bezier(.4, 0, .8, .3)', fill: 'forwards' },
    );

    exitAnimation.onfinish = () => {
      flushSync(() => setViewLayout(nextLayout));
      requestAnimationFrame(() => {
        const updatedStage = layoutStageRef.current;
        if (!updatedStage) {
          setIsLayoutTransitioning(false);
          return;
        }
        const enterAnimation = updatedStage.animate(
          [
            { opacity: 0, transform: 'scale(.92)' },
            { opacity: 1, transform: 'scale(1)' },
          ],
          { duration: 360, easing: 'cubic-bezier(.18, .72, .24, 1)', fill: 'both' },
        );
        enterAnimation.onfinish = () => {
          exitAnimation.cancel();
          enterAnimation.cancel();
          setIsLayoutTransitioning(false);
        };
      });
    };
  };

  // Helper check if task scheduled time has passed without being completed
  const isTaskPast = (t: TimeBlock) => {
    if (t.status === 'COMPLETED') return false;
    if (t.status === 'OVERDUE') return true;
    const end = t.endTime ? new Date(t.endTime).getTime() : new Date(t.startTime).getTime();
    return end < Date.now();
  };

  // Helper to get numeric timestamp for chronological sorting
  const getTimeValue = (t: TimeBlock) => {
    if (t.startTime) {
      const ms = new Date(t.startTime).getTime();
      if (!isNaN(ms)) return ms;
    }
    if (t.endTime) {
      const ms = new Date(t.endTime).getTime();
      if (!isNaN(ms)) return ms;
    }
    return 0;
  };

  // Stats
  const totalCount = tasks.length;
  const completedCount = tasks.filter((t) => t.status === 'COMPLETED').length;
  const pastCount = tasks.filter((t) => isTaskPast(t)).length;
  const todoCount = tasks.filter((t) => t.status === 'TODO' && !isTaskPast(t)).length;
  const habitCount = tasks.filter((t) => t.isRecurring).length;

  // Filtered tasks
  const filteredTasks = tasks.filter((t) => {
    if (statusTab === 'TODO') {
      if (t.status !== 'TODO' || isTaskPast(t)) return false;
    } else if (statusTab === 'IN_PROGRESS') {
      if (t.status !== 'IN_PROGRESS' || isTaskPast(t)) return false;
    } else if (statusTab === 'OVERDUE') {
      if (!isTaskPast(t)) return false;
    } else if (statusTab === 'COMPLETED') {
      if (t.status !== 'COMPLETED') return false;
    } else if (statusTab === 'HABITS') {
      if (!t.isRecurring) return false;
    }
    if (selectedPriority !== 'ALL' && t.priority !== selectedPriority) return false;
    if (search.trim() && !matchesSearch(t.title, search) && !matchesSearch(t.description, search)) return false;
    return true;
  });

  // Sort by time, and push completed tasks to the bottom of the list
  const sortedTasks = [...filteredTasks].sort((a, b) => {
    const aCompleted = a.status === 'COMPLETED';
    const bCompleted = b.status === 'COMPLETED';
    // 1. Incompleted tasks first, completed tasks pushed down to the end!
    if (aCompleted !== bCompleted) {
      return aCompleted ? 1 : -1;
    }
    // 2. Both incompleted or both completed: sort chronologically by time
    const timeA = getTimeValue(a);
    const timeB = getTimeValue(b);
    if (timeA !== timeB) {
      return timeA - timeB;
    }
    return a.id - b.id;
  });

  const getPriorityLabel = (p?: string) => {
    switch (p) {
      case 'URGENT':
        return { label: 'Khẩn cấp', color: '#DC2626', bg: '#FEE2E2' };
      case 'HIGH':
        return { label: 'Cao', color: '#EA580C', bg: '#FFEDD5' };
      case 'MEDIUM':
        return { label: 'Trung bình', color: '#D97706', bg: '#FEF3C7' };
      case 'LOW':
        return { label: 'Thấp', color: '#16A34A', bg: '#DCFCE7' };
      default:
        return { label: 'Trung bình', color: '#D97706', bg: '#FEF3C7' };
    }
  };

  const getCategoryMeta = (type: string) => {
    const found = categories.find((c) => c.type === type);
    return found || { name: 'Công việc', colorCode: '#8B5CF6' };
  };

  const formatDateTimeRange = (startStr: string, endStr: string) => {
    const s = new Date(startStr);
    const e = new Date(endStr);
    const dayNames = ['Chủ Nhật', 'Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy'];
    const dayName = dayNames[s.getDay()];
    const dateFormatted = formatDateDDMMYYYY(s);
    const timeFormatted = `${s.getHours().toString().padStart(2, '0')}:${s.getMinutes().toString().padStart(2, '0')} – ${e.getHours().toString().padStart(2, '0')}:${e.getMinutes().toString().padStart(2, '0')}`;
    return `${dayName}, ${dateFormatted} · ${timeFormatted}`;
  };

  return (
    <div
      className={`tasks-view${viewLayout === 'EISENHOWER' ? ' tasks-view-eisenhower' : ''}`}
      style={viewLayout === 'EISENHOWER'
        ? { padding: '12px 20px', maxWidth: 1300, margin: '0 auto', width: '100%', height: '100%', minHeight: 0, boxSizing: 'border-box', display: 'flex', flexDirection: 'column', overflow: 'hidden' }
        : { padding: '18px 24px', maxWidth: 1200, margin: '0 auto', width: '100%' }}
    >
      {/* ── Top Header Title ── */}
      <div className="tasks-page-heading" style={{ marginBottom: viewLayout === 'EISENHOWER' ? 8 : 12 }}>
        <h1 style={{ fontSize: viewLayout === 'EISENHOWER' ? '1.35rem' : '1.6rem', fontWeight: 800, color: 'var(--text)', margin: 0 }}>
          Quản lý Công việc
        </h1>
        <p className="tasks-page-description" style={{ fontSize: '0.85rem', color: 'var(--text-3)', margin: '4px 0 0' }}>
          Toàn bộ công việc trong không gian cá nhân được đồng bộ với cơ sở dữ liệu
        </p>
      </div>

      {/* ── Controls Row: View mode on Left, + Thêm công việc on Far Right ── */}
      <div
        className="tasks-view-controls"
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: 12,
          marginBottom: viewLayout === 'EISENHOWER' ? 8 : 12,
        }}
      >
        <div className="tasks-view-mode-picker">
          <span style={{ fontSize: '0.84rem', fontWeight: 600, color: 'var(--text-3)', whiteSpace: 'nowrap' }}>
            Chế độ xem:
          </span>
          <div
            className="tasks-layout-dropdown"
            ref={layoutMenuRef}
            onMouseEnter={() => {
              if (layoutMenuTimer.current) clearTimeout(layoutMenuTimer.current);
              if (!isLayoutTransitioning) setIsLayoutMenuOpen(true);
            }}
            onMouseLeave={() => {
              if (layoutMenuTimer.current) clearTimeout(layoutMenuTimer.current);
              layoutMenuTimer.current = setTimeout(() => setIsLayoutMenuOpen(false), 220);
            }}
          >
            <button
              type="button"
              className="tasks-layout-trigger"
              aria-haspopup="menu"
              aria-expanded={isLayoutMenuOpen}
              onClick={() => setIsLayoutMenuOpen((open) => !open)}
              onKeyDown={(event) => {
                if (event.key === 'ArrowDown') {
                  event.preventDefault();
                  setIsLayoutMenuOpen(true);
                } else if (event.key === 'Escape') {
                  setIsLayoutMenuOpen(false);
                }
              }}
            >
              <span>{viewLayout === 'LIST' ? 'Xem dưới dạng danh sách' : 'Xem dạng lưới (Ma trận Eisenhower)'}</span>
              <ChevronDown size={15} className={isLayoutMenuOpen ? 'is-open' : ''} />
            </button>
            {shouldRenderLayoutMenu && (
              <div className={`tasks-layout-menu${isLayoutMenuExiting ? ' is-exiting' : ''}`} role="menu">
                {([
                  { value: 'LIST' as const, label: 'Xem dưới dạng danh sách' },
                  { value: 'EISENHOWER' as const, label: 'Xem dạng lưới (Ma trận Eisenhower)' },
                ]).map((option) => (
                  <button
                    key={option.value}
                    type="button"
                    className={`tasks-layout-option${viewLayout === option.value ? ' is-selected' : ''}`}
                    role="menuitemradio"
                    aria-checked={viewLayout === option.value}
                    onClick={() => handleViewLayoutChange(option.value)}
                  >
                    <span>{option.label}</span>
                    {viewLayout === option.value && <Check size={15} />}
                  </button>
                ))}
              </div>
            )}
          </div>
        </div>

        <button
          className={`tasks-add-task-button${viewLayout === 'EISENHOWER' ? ' is-compact' : ''}`}
          onClick={onAddTask}
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 8,
            padding: viewLayout === 'EISENHOWER' ? '8px 15px' : '8px 16px',
            borderRadius: 10,
            background: 'var(--primary)',
            color: '#fff',
            fontSize: viewLayout === 'EISENHOWER' ? '0.82rem' : '0.84rem',
            fontWeight: 600,
            boxShadow: '0 2px 8px rgba(108, 92, 231, 0.3)',
            cursor: 'pointer',
            transition: 'all 0.2s',
            whiteSpace: 'nowrap',
          }}
        >
          <Plus size={16} />
          <span>Thêm công việc</span>
        </button>
      </div>

      <div
        ref={layoutStageRef}
        className="tasks-layout-stage"
        style={viewLayout === 'EISENHOWER'
          ? { display: 'flex', flexDirection: 'column', flex: '1 1 auto', minHeight: 0, overflow: 'hidden' }
          : undefined}
      >
      {viewLayout === 'EISENHOWER' ? (
        <div className="eisenhower-matrix-host" style={{ marginTop: 0, flex: 1, minHeight: 0, overflow: 'hidden' }}>
          <EisenhowerMatrix
            tasks={tasks}
            categories={categories}
            showHeader={false}
            onAddTask={(quadrant) => {
              if (onAddQuadrantTask) {
                onAddQuadrantTask(quadrant);
              } else {
                onAddTask();
              }
            }}
            onEditTask={onEditTask}
            onToggleStatus={onToggleStatus}
            onShowToast={onShowToast || (() => {})}
            onReload={onReload || (() => {})}
          />
        </div>
      ) : (
        <>
          {/* ── 5 Summary Mini Cards - Always 1 single line without wrapping ── */}
      {/* ── 5 Summary Mini Cards - Centered & Compact ── */}
      <div
        className="tasks-summary-grid"
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(5, minmax(0, 1fr))',
          gap: 8,
          marginBottom: 10,
          width: '100%',
        }}
      >
        <div
          onClick={() => setStatusTab('ALL')}
          style={{
            background: 'var(--surface)',
            border: statusTab === 'ALL' ? '2px solid var(--primary)' : '1px solid var(--border)',
            borderRadius: 10,
            padding: '7px 8px',
            cursor: 'pointer',
            boxShadow: 'var(--shadow-xs)',
            transition: 'all 0.2s',
            minWidth: 0,
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            textAlign: 'center',
          }}
        >
          <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)', fontWeight: 700, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', maxWidth: '100%' }}>TỔNG CÔNG VIỆC</div>
          <div style={{ fontSize: '1.35rem', fontWeight: 800, color: 'var(--text)', marginTop: 2, lineHeight: 1.1 }}>
            {totalCount}
          </div>
        </div>

        <div
          onClick={() => setStatusTab('TODO')}
          style={{
            background: 'var(--surface)',
            border: statusTab === 'TODO' ? '2px solid var(--primary)' : '1px solid var(--border)',
            borderRadius: 10,
            padding: '7px 8px',
            cursor: 'pointer',
            boxShadow: 'var(--shadow-xs)',
            transition: 'all 0.2s',
            minWidth: 0,
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            textAlign: 'center',
          }}
        >
          <div style={{ fontSize: '0.68rem', color: '#64748B', fontWeight: 700, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', maxWidth: '100%' }}>CẦN LÀM (TODO)</div>
          <div style={{ fontSize: '1.35rem', fontWeight: 800, color: '#64748B', marginTop: 2, lineHeight: 1.1 }}>
            {todoCount}
          </div>
        </div>

        <div
          onClick={() => setStatusTab('OVERDUE')}
          style={{
            background: 'var(--surface)',
            border: statusTab === 'OVERDUE' ? '2px solid #EF4444' : '1px solid var(--border)',
            borderRadius: 10,
            padding: '7px 8px',
            cursor: 'pointer',
            boxShadow: 'var(--shadow-xs)',
            transition: 'all 0.2s',
            minWidth: 0,
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            textAlign: 'center',
          }}
        >
          <div style={{ fontSize: '0.68rem', color: '#EF4444', fontWeight: 700, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', maxWidth: '100%' }}>ĐÃ QUA</div>
          <div style={{ fontSize: '1.35rem', fontWeight: 800, color: '#EF4444', marginTop: 2, lineHeight: 1.1 }}>
            {pastCount}
          </div>
        </div>

        <div
          onClick={() => setStatusTab('COMPLETED')}
          style={{
            background: 'var(--surface)',
            border: statusTab === 'COMPLETED' ? '2px solid #10B981' : '1px solid var(--border)',
            borderRadius: 10,
            padding: '7px 8px',
            cursor: 'pointer',
            boxShadow: 'var(--shadow-xs)',
            transition: 'all 0.2s',
            minWidth: 0,
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            textAlign: 'center',
          }}
        >
          <div style={{ fontSize: '0.64rem', letterSpacing: '-0.2px', color: '#10B981', fontWeight: 700, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', maxWidth: '100%' }} title="ĐÃ ĐÁNH DẤU HOÀN THÀNH">ĐÃ ĐÁNH DẤU HOÀN THÀNH</div>
          <div style={{ fontSize: '1.35rem', fontWeight: 800, color: '#10B981', marginTop: 2, lineHeight: 1.1 }}>
            {completedCount}
          </div>
        </div>

        <div
          onClick={() => setStatusTab('HABITS')}
          style={{
            background: 'var(--surface)',
            border: statusTab === 'HABITS' ? '2px solid #EA580C' : '1px solid var(--border)',
            borderRadius: 10,
            padding: '7px 8px',
            cursor: 'pointer',
            boxShadow: 'var(--shadow-xs)',
            transition: 'all 0.2s',
            minWidth: 0,
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            textAlign: 'center',
          }}
        >
          <div style={{ fontSize: '0.66rem', color: '#EA580C', fontWeight: 700, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', maxWidth: '100%' }}>
            <span>THÓI QUEN LẶP LẠI</span>
          </div>
          <div style={{ fontSize: '1.35rem', fontWeight: 800, color: '#EA580C', marginTop: 2, lineHeight: 1.1 }}>
            {habitCount}
          </div>
        </div>
      </div>

      {/* ── Compact Filter Bar (Search on Left, Priority on Right - matching Image 2) ── */}
      <div
        className="tasks-filter-bar"
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: 12,
          background: 'var(--surface)',
          padding: '7px 12px',
          borderRadius: 12,
          border: '1px solid var(--border)',
          marginBottom: 12,
          boxShadow: 'var(--shadow-xs)',
        }}
      >
        {/* Left: Search input */}
        <div style={{ position: 'relative', width: 340, maxWidth: '100%' }}>
          <Search size={16} style={{ position: 'absolute', left: 12, top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
          <input
            type="text"
            placeholder="Tìm theo tên công việc..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            style={{
              width: '100%',
              padding: '8px 12px 8px 36px',
              borderRadius: 8,
              border: '1px solid var(--border)',
              background: 'transparent',
              color: 'var(--text)',
              fontSize: '0.84rem',
              outline: 'none',
              transition: 'border-color 0.2s',
            }}
          />
        </div>

        {/* Right: Priority select */}
        <HoverSelect
          value={selectedPriority}
          onChange={(e) => setSelectedPriority(e.target.value)}
          style={{
            padding: '8px 14px',
            borderRadius: 8,
            border: '1px solid var(--border)',
            background: 'var(--surface)',
            color: 'var(--text)',
            fontSize: '0.84rem',
            outline: 'none',
            cursor: 'pointer',
            fontWeight: 500,
          }}
        >
          <option value="ALL">Tất cả mức ưu tiên</option>
          <option value="URGENT">Khẩn cấp</option>
          <option value="HIGH">Cao</option>
          <option value="MEDIUM">Trung bình</option>
          <option value="LOW">Thấp</option>
        </HoverSelect>
      </div>

      {/* ── Tasks List ── */}
      <div className={`tasks-list ${sortedTasks.length === 0 ? 'empty' : ''}`}>
        {sortedTasks.length === 0 ? (
          <div
            style={{
              textAlign: 'center',
              padding: '48px 24px',
              background: 'var(--surface)',
              border: '1px dashed var(--border)',
              borderRadius: 14,
              color: 'var(--text-muted)',
            }}
          >
            <CheckSquare size={36} style={{ margin: '0 auto 12px', opacity: 0.5 }} />
            <div style={{ fontSize: '0.95rem', fontWeight: 600, color: 'var(--text-2)' }}>
              Không có công việc nào phù hợp
            </div>
            <p style={{ fontSize: '0.8rem', margin: '4px 0 0' }}>
              Hãy thử chọn bộ lọc khác hoặc thêm công việc mới.
            </p>
          </div>
        ) : (
          <>
          <div className="task-list-header" role="row">
            <span aria-hidden="true" />
            <span role="columnheader">Công việc</span>
            <span role="columnheader">Thời gian</span>
            <span role="columnheader">Nhóm</span>
            <span role="columnheader">Ưu tiên</span>
            <span role="columnheader">Ma trận</span>
            <span role="columnheader">Trạng thái</span>
            <span aria-hidden="true" />
          </div>
          {sortedTasks.map((task, index) => {
            const cat = getCategoryMeta(task.categoryType);
            const prio = getPriorityLabel(task.priority);
            const isCompleted = task.status === 'COMPLETED';
            const isFirstCompleted = isCompleted && index > 0 && sortedTasks[index - 1].status !== 'COMPLETED';

            return (
              <div key={task.id}>
                {isFirstCompleted && (
                  <div
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: 12,
                      margin: '10px 0 6px',
                      color: 'var(--text-muted)',
                      fontSize: '0.78rem',
                      fontWeight: 700,
                      letterSpacing: 0.5,
                    }}
                  >
                    <div style={{ flex: 1, height: 1, background: 'var(--border)' }} />
                    <span>ĐÃ ĐÁNH DẤU HOÀN THÀNH ({completedCount})</span>
                    <div style={{ flex: 1, height: 1, background: 'var(--border)' }} />
                  </div>
                )}
                <div className="task-row-card task-list-grid-row" role="row" style={{ opacity: isCompleted ? 0.9 : 1 }}>
                  <button
                    className={`task-list-complete-toggle${isCompleted ? ' is-completed' : ''}`}
                    onClick={() => onToggleStatus(task)}
                    title={isCompleted ? 'Đánh dấu chưa hoàn thành' : 'Đánh dấu hoàn thành'}
                    aria-label={isCompleted ? 'Đánh dấu chưa hoàn thành' : 'Đánh dấu hoàn thành'}
                  >
                    {isCompleted ? <CheckCircle2 size={20} /> : <Circle size={20} />}
                  </button>

                  <div className="task-list-title-cell">
                    <div className={`task-list-title${isCompleted ? ' is-completed' : ''}`} title={task.title}>{task.title}</div>
                    {task.note && <div className="task-list-note" title={task.note}>{task.note}</div>}
                    {(task.subtasksCount || task.isRecurring) && (
                      <div className="task-list-title-extras">
                        {task.subtasksCount ? (
                          <span><CheckSquare size={11} /> {task.completedSubtasksCount || 0}/{task.subtasksCount} việc con</span>
                        ) : null}
                        {task.isRecurring && (
                          <span className="recurring"><Flame size={11} fill="currentColor" /> {task.streakCount || 1} ngày chuỗi</span>
                        )}
                      </div>
                    )}
                  </div>

                  <div className="task-list-date-cell" title={formatDateTimeRange(task.startTime, task.endTime)}>
                    <Clock size={13} />
                    <span>{formatDateTimeRange(task.startTime, task.endTime)}</span>
                  </div>

                  <div className="task-list-data-cell">
                    <span className="task-list-pill" style={{ background: `${cat.colorCode}18`, color: cat.colorCode }} title={cat.name}>
                      <Tag size={11} /><span>{cat.name}</span>
                    </span>
                  </div>

                  <div className="task-list-data-cell">
                    <span className="task-list-pill" style={{ background: prio.bg, color: prio.color }} title={prio.label}>
                      <AlertCircle size={11} /><span>{prio.label}</span>
                    </span>
                  </div>

                  <div className="task-list-data-cell">
                    <span
                      className="task-list-pill"
                      title={task.eisenhowerQuadrant === 'DO_FIRST' ? 'Q1: Làm ngay' : task.eisenhowerQuadrant === 'DELEGATE' ? 'Q3: Xử lý nhanh' : task.eisenhowerQuadrant === 'DONT_DO' ? 'Q4: Loại bỏ' : 'Q2: Lên lịch'}
                      style={{
                        background: task.eisenhowerQuadrant === 'DO_FIRST' ? '#FEE2E2' : task.eisenhowerQuadrant === 'DELEGATE' ? '#FEF3C7' : task.eisenhowerQuadrant === 'DONT_DO' ? '#F1F5F9' : '#EDE9FE',
                        color: task.eisenhowerQuadrant === 'DO_FIRST' ? '#DC2626' : task.eisenhowerQuadrant === 'DELEGATE' ? '#D97706' : task.eisenhowerQuadrant === 'DONT_DO' ? '#64748B' : '#4F46E5',
                      }}
                    >
                      {task.eisenhowerQuadrant === 'DO_FIRST' ? 'Q1: Làm ngay' : task.eisenhowerQuadrant === 'DELEGATE' ? 'Q3: Xử lý nhanh' : task.eisenhowerQuadrant === 'DONT_DO' ? 'Q4: Loại bỏ' : 'Q2: Lên lịch'}
                    </span>
                  </div>

                  <div className="task-list-data-cell">
                    <span className={`task-list-status-pill ${isCompleted ? 'completed' : isTaskPast(task) ? 'overdue' : task.status === 'IN_PROGRESS' ? 'in-progress' : 'todo'}`}>
                      {isCompleted ? 'Hoàn thành' : isTaskPast(task) ? 'Quá hạn' : task.status === 'IN_PROGRESS' ? 'Đang làm' : 'Cần làm'}
                    </span>
                  </div>

                  <div className="task-list-actions">
                  <button
                    onClick={() => onEditTask(task)}
                    title="Chỉnh sửa công việc"
                    aria-label="Chỉnh sửa công việc"
                    style={{
                      width: 32,
                      height: 32,
                      borderRadius: 8,
                      border: '1px solid var(--border)',
                      background: 'var(--surface)',
                      color: 'var(--text-2)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      cursor: 'pointer',
                      transition: 'all 0.15s',
                    }}
                  >
                    <Edit3 size={15} />
                  </button>

                  <button
                    onClick={() => setTaskToDelete(task)}
                    title="Xóa công việc"
                    aria-label="Xóa công việc"
                    style={{
                      width: 32,
                      height: 32,
                      borderRadius: 8,
                      border: '1px solid var(--border)',
                      background: 'var(--surface)',
                      color: '#EF4444',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      cursor: 'pointer',
                      transition: 'all 0.15s',
                    }}
                  >
                    <Trash2 size={15} />
                  </button>
                </div>
              </div>
            </div>
            );
          })}
          </>
        )}
      </div>
        </>
      )}
      </div>

      {/* In-App Confirm Delete Task Modal */}
      <ConfirmModal
        isOpen={taskToDelete !== null}
        title="Xóa công việc"
        message={`Bạn có chắc chắn muốn xóa công việc "${taskToDelete?.title}" khỏi lịch không?`}
        confirmText="Xóa công việc"
        onConfirm={() => {
          if (taskToDelete) {
            onDeleteTask(taskToDelete.id);
            setTaskToDelete(null);
          }
        }}
        onCancel={() => setTaskToDelete(null)}
      />
    </div>
  );
}
