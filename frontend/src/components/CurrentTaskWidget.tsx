import { Clock, Sparkles, ArrowRight, CheckCircle2, Calendar } from 'lucide-react';
import { TimeBlock, Priority } from '../types';

interface CurrentTaskWidgetProps {
  timeBlocks: TimeBlock[];
  onSelectTask?: (task: TimeBlock) => void;
}

// Hàm lấy thông tin màu sắc và nhãn theo mức độ khẩn cấp
export const getPriorityTheme = (priority?: Priority | string) => {
  switch (priority) {
    case 'URGENT':
      return {
        label: 'Khẩn cấp',
        bgLight: '#FEF2F2',
        borderLight: '#FCA5A5',
        textLight: '#991B1B',
        accent: '#EF4444',
        badgeBg: '#FEE2E2',
        badgeColor: '#B91C1C',
        dotColor: '#EF4444',
      };
    case 'HIGH':
      return {
        label: 'Ưu tiên cao',
        bgLight: '#FFF7ED',
        borderLight: '#FDBA74',
        textLight: '#9A3412',
        accent: '#F97316',
        badgeBg: '#FFEDD5',
        badgeColor: '#C2410C',
        dotColor: '#F97316',
      };
    case 'MEDIUM':
      return {
        label: 'Trung bình',
        bgLight: '#FFFBEB',
        borderLight: '#FCD34D',
        textLight: '#92400E',
        accent: '#F59E0B',
        badgeBg: '#FEF3C7',
        badgeColor: '#B45309',
        dotColor: '#F59E0B',
      };
    case 'LOW':
      return {
        label: 'Thấp',
        bgLight: '#F0FDF4',
        borderLight: '#86EFAC',
        textLight: '#166534',
        accent: '#10B981',
        badgeBg: '#DCFCE7',
        badgeColor: '#15803D',
        dotColor: '#10B981',
      };
    default:
      return {
        label: 'Trung bình',
        bgLight: '#FFFBEB',
        borderLight: '#FCD34D',
        textLight: '#92400E',
        accent: '#F59E0B',
        badgeBg: '#FEF3C7',
        badgeColor: '#B45309',
        dotColor: '#F59E0B',
      };
  }
};

const formatTimeOnly = (isoString: string) => {
  try {
    const d = new Date(isoString);
    return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`;
  } catch {
    return '--:--';
  }
};

export default function CurrentTaskWidget({ timeBlocks, onSelectTask }: CurrentTaskWidgetProps) {
  const now = new Date();
  const nowTime = now.getTime();
  const currentYMD = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;

  // Lọc tất cả công việc trong ngày hôm nay
  const todayTasks = timeBlocks
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

  // 1. Tìm công việc đang diễn ra ngay tại thời điểm hiện tại
  const currentTask =
    todayTasks.find((t) => {
      const s = new Date(t.startTime).getTime();
      const e = new Date(t.endTime).getTime();
      return s <= nowTime && nowTime < e && t.status !== 'COMPLETED' && t.status !== 'CANCELLED';
    }) ||
    todayTasks.find((t) => {
      const s = new Date(t.startTime).getTime();
      const e = new Date(t.endTime).getTime();
      return s <= nowTime && nowTime < e;
    });

  // 2. Lấy danh sách công việc sắp tới trong ngày (bắt đầu sau thời điểm hiện tại hoặc sau công việc hiện tại)
  const upcomingTasks = todayTasks.filter((t) => {
    if (currentTask && t.id === currentTask.id) return false;
    const s = new Date(t.startTime).getTime();
    return s > nowTime && t.status !== 'COMPLETED' && t.status !== 'CANCELLED';
  });

  // =========================================================================
  // TRƯỜNG HỢP 1: CÓ CÔNG VIỆC HIỆN TẠI
  // =========================================================================
  if (currentTask) {
    const currentTheme = getPriorityTheme(currentTask.priority);
    const nextTask = upcomingTasks[0];
    const nextTheme = nextTask ? getPriorityTheme(nextTask.priority) : null;
    const timeRangeStr = `${formatTimeOnly(currentTask.startTime)} – ${formatTimeOnly(currentTask.endTime)}`;

    return (
      <div
        className="current-task-widget has-current"
        style={{
          backgroundColor: currentTheme.bgLight,
          borderColor: currentTheme.borderLight,
        }}
        onClick={() => onSelectTask?.(currentTask)}
        title="Nhấp để xem chi tiết công việc hiện tại"
      >
        {/* Phần trên: Tiêu đề widget + Chi tiết công việc đang chạy */}
        <div className="current-task-header">
          <div className="current-task-badge" style={{ backgroundColor: currentTheme.badgeBg, color: currentTheme.badgeColor }}>
            <span className="pulse-indicator" style={{ backgroundColor: currentTheme.dotColor }} />
            <span className="badge-title">CÔNG VIỆC HIỆN TẠI</span>
            <span className="badge-divider">•</span>
            <span className="badge-priority">{currentTheme.label}</span>
          </div>

          <div className="current-task-time" style={{ color: currentTheme.textLight }}>
            <Clock size={12} style={{ marginRight: 4, display: 'inline', verticalAlign: '-1px' }} />
            <span>{timeRangeStr}</span>
          </div>
        </div>

        {/* Tên công việc & Chú thích */}
        <div className="current-task-body">
          <h4 className="current-task-name" style={{ color: currentTheme.textLight }}>
            {currentTask.title}
          </h4>
          {currentTask.note && (
            <p className="current-task-note" style={{ color: currentTheme.textLight }}>
              <span>💬</span> {currentTask.note}
            </p>
          )}
        </div>

        {/* Phần dưới: Kèm 1 dòng công việc sắp tới (màu nền theo mức độ khẩn cấp của công việc sắp tới) */}
        {nextTask && nextTheme ? (
          <div
            className="next-task-row"
            style={{
              backgroundColor: nextTheme.bgLight,
              borderColor: nextTheme.borderLight,
              color: nextTheme.textLight,
            }}
            onClick={(e) => {
              e.stopPropagation();
              onSelectTask?.(nextTask);
            }}
            title="Nhấp để xem công việc sắp tới"
          >
            <div className="next-task-left">
              <span className="next-label">Sắp tới:</span>
              <span className="next-time">{formatTimeOnly(nextTask.startTime)}</span>
              <span className="next-name">{nextTask.title}</span>
            </div>
            <div className="next-task-right">
              <span
                className="mini-priority-badge"
                style={{ backgroundColor: nextTheme.badgeBg, color: nextTheme.badgeColor }}
              >
                {nextTheme.label}
              </span>
              <ArrowRight size={12} className="next-arrow" />
            </div>
          </div>
        ) : (
          <div className="next-task-empty">
            <CheckCircle2 size={12} style={{ color: currentTheme.accent, marginRight: 5 }} />
            <span>Đã là công việc cuối cùng trong hôm nay</span>
          </div>
        )}
      </div>
    );
  }

  // =========================================================================
  // TRƯỜNG HỢP 2: KHÔNG CÓ CÔNG VIỆC HIỆN TẠI
  // =========================================================================
  const upcomingSlice = upcomingTasks.slice(0, 3);

  return (
    <div className="current-task-widget no-current">
      {/* Header trạng thái */}
      <div className="no-current-header">
        <div className="no-current-title-wrap">
          <Sparkles size={14} className="sparkle-icon" />
          <span className="no-current-title">Hiện tại không có công việc</span>
        </div>
        {upcomingSlice.length > 0 && (
          <span className="upcoming-summary-tag">
            {upcomingTasks.length} việc sắp tới hôm nay
          </span>
        )}
      </div>

      {/* Danh sách công việc sắp tới dạng ngang (2-3 công việc, màu theo mức độ khẩn cấp) */}
      {upcomingSlice.length > 0 ? (
        <div className="upcoming-horizontal-list">
          {upcomingSlice.map((task) => {
            const taskTheme = getPriorityTheme(task.priority);
            const timeStr = `${formatTimeOnly(task.startTime)}–${formatTimeOnly(task.endTime)}`;

            return (
              <div
                key={task.id}
                className="upcoming-item-card"
                style={{
                  backgroundColor: taskTheme.bgLight,
                  borderColor: taskTheme.borderLight,
                  color: taskTheme.textLight,
                }}
                onClick={() => onSelectTask?.(task)}
                title={`Công việc sắp tới: ${task.title} (${timeStr}) · Mức độ: ${taskTheme.label}`}
              >
                <div className="upcoming-item-top">
                  <span className="upcoming-item-time">
                    <Clock size={11} style={{ marginRight: 3, display: 'inline', verticalAlign: '-1px' }} />
                    {timeStr}
                  </span>
                  <span
                    className="upcoming-item-badge"
                    style={{ backgroundColor: taskTheme.badgeBg, color: taskTheme.badgeColor }}
                  >
                    {taskTheme.label}
                  </span>
                </div>
                <div className="upcoming-item-title">{task.title}</div>
              </div>
            );
          })}
        </div>
      ) : (
        <div className="all-clear-banner">
          <Calendar size={13} style={{ marginRight: 6, opacity: 0.7 }} />
          <span>Hôm nay không còn công việc nào sắp tới · Bạn có thể thảnh thơi</span>
        </div>
      )}
    </div>
  );
}
