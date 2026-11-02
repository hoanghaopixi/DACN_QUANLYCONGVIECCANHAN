import { Clock, Sparkles, Calendar } from 'lucide-react';
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
    const timeRangeStr = `${formatTimeOnly(currentTask.startTime)} – ${formatTimeOnly(currentTask.endTime)}`;

    return (
      <div className="compact-widget-container">
        <button
          type="button"
          className="compact-current-pill"
          style={{
            backgroundColor: currentTheme.bgLight,
            borderColor: currentTheme.borderLight,
            color: currentTheme.textLight,
          }}
          onClick={() => onSelectTask?.(currentTask)}
          title={`Đang diễn ra: ${currentTask.title} (${timeRangeStr}) · Nhấp để xem chi tiết`}
        >
          <span className="compact-pulse-dot" style={{ backgroundColor: currentTheme.dotColor }} />
          <span className="compact-pill-tag">ĐANG DIỄN RA:</span>
          <span className="compact-pill-name">{currentTask.title}</span>
          <span className="compact-pill-time">
            <Clock size={11} style={{ marginRight: 3, verticalAlign: '-1px' }} />
            {timeRangeStr}
          </span>
        </button>

        {nextTask && (
          <button
            type="button"
            className="compact-next-chip"
            onClick={() => onSelectTask?.(nextTask)}
            title={`Việc kế tiếp: ${nextTask.title} (${formatTimeOnly(nextTask.startTime)})`}
          >
            <span className="compact-next-label">Kế tiếp:</span>
            <span className="compact-next-title">{nextTask.title}</span>
            <span className="compact-next-time">{formatTimeOnly(nextTask.startTime)}</span>
          </button>
        )}
      </div>
    );
  }

  // =========================================================================
  // TRƯỜNG HỢP 2: KHÔNG CÓ CÔNG VIỆC HIỆN TẠI -> HIỂN THỊ DẠNG CHIP NGANG GỌN GÀNG
  // =========================================================================
  const upcomingSlice = upcomingTasks.slice(0, 3);

  return (
    <div className="compact-widget-container">
      {upcomingSlice.length > 0 ? (
        <div className="compact-chips-wrap">
          <div className="compact-upcoming-label">
            <Sparkles size={13} className="sparkle-icon" />
            <span>Sắp tới:</span>
          </div>

          <div className="compact-chips-list">
            {upcomingSlice.map((task) => {
              const taskTheme = getPriorityTheme(task.priority);
              const timeStr = `${formatTimeOnly(task.startTime)}–${formatTimeOnly(task.endTime)}`;

              return (
                <button
                  key={task.id}
                  type="button"
                  className="compact-task-chip"
                  style={{
                    backgroundColor: taskTheme.bgLight,
                    borderColor: taskTheme.borderLight,
                    color: taskTheme.textLight,
                  }}
                  onClick={() => onSelectTask?.(task)}
                  title={`Công việc sắp tới: ${task.title} (${timeStr}) · Mức độ: ${taskTheme.label}`}
                >
                  <span className="compact-chip-dot" style={{ backgroundColor: taskTheme.dotColor }} />
                  <span className="compact-chip-time">{formatTimeOnly(task.startTime)}</span>
                  <span className="compact-chip-title">{task.title}</span>
                </button>
              );
            })}
          </div>
        </div>
      ) : (
        <div className="compact-all-clear">
          <Calendar size={13} style={{ opacity: 0.7 }} />
          <span>Hôm nay không còn việc nào sắp tới !</span>
        </div>
      )}
    </div>
  );
}
