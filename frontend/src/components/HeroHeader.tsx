import { useState, useMemo } from 'react';
import {
  CalendarClock,
  ChevronUp,
  ChevronDown,
  CheckCircle2,
} from 'lucide-react';
import { TimeBlock } from '../types';

interface HeroHeaderProps {
  userName?: string;
  todayTasksCount?: number;
  timeBlocks?: TimeBlock[];
  onAddTaskClick?: () => void;
  onSelectTask?: (task: TimeBlock) => void;
}

const formatTimeOnly = (isoString: string) => {
  try {
    const d = new Date(isoString);
    return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`;
  } catch {
    return '--:--';
  }
};

export default function HeroHeader({
  userName = 'Minh Anh',
  todayTasksCount = 3,
  timeBlocks = [],
  onSelectTask,
}: HeroHeaderProps) {
  const [isCollapsed, setIsCollapsed] = useState<boolean>(() => {
    try {
      return localStorage.getItem('smarttask_hero_collapsed') === 'true';
    } catch {
      return false;
    }
  });

  const toggleCollapse = () => {
    setIsCollapsed((prev) => {
      const next = !prev;
      try {
        localStorage.setItem('smarttask_hero_collapsed', String(next));
      } catch {}
      return next;
    });
  };

  const now = new Date();
  const nowTime = now.getTime();
  const currentYMD = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;

  // Lọc danh sách công việc trong ngày hôm nay
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

  // Công việc đang diễn ra tại thời điểm hiện tại
  const currentTask = useMemo(() => {
    return (
      todayTasks.find((t) => {
        const s = new Date(t.startTime).getTime();
        const e = new Date(t.endTime).getTime();
        return s <= nowTime && nowTime < e && t.status !== 'COMPLETED' && t.status !== 'CANCELLED';
      }) ||
      todayTasks.find((t) => {
        const s = new Date(t.startTime).getTime();
        const e = new Date(t.endTime).getTime();
        return s <= nowTime && nowTime < e;
      })
    );
  }, [todayTasks, nowTime]);

  // Danh sách công việc sắp tới trong ngày
  const upcomingTasks = useMemo(() => {
    return todayTasks.filter((t) => {
      if (currentTask && t.id === currentTask.id) return false;
      const s = new Date(t.startTime).getTime();
      return s > nowTime && t.status !== 'COMPLETED' && t.status !== 'CANCELLED';
    });
  }, [todayTasks, currentTask, nowTime]);

  const totalUpcomingCount = (currentTask ? 1 : 0) + upcomingTasks.length;

  return (
    <div className={`hero-banner-bar ${isCollapsed ? 'collapsed' : ''}`}>
      {/* ── Bên trái: Lời chào & Nút thu gọn ── */}
      <div className="hero-banner-left">
        <div className="hero-banner-title-wrap">
          <span className="hero-banner-title">Một tuần hiệu quả</span>
          <span className="hero-banner-badge">
            Chào {userName} · {todayTasksCount} việc hôm nay
          </span>
        </div>

        <button
          type="button"
          className="hero-banner-toggle-btn"
          onClick={toggleCollapse}
          title={isCollapsed ? 'Mở rộng thanh tóm tắt' : 'Thu gọn thanh tóm tắt để lịch rộng hơn'}
          aria-label={isCollapsed ? 'Mở rộng tóm tắt' : 'Thu gọn tóm tắt'}
        >
          {isCollapsed ? <ChevronDown size={13} /> : <ChevronUp size={13} />}
          <span>{isCollapsed ? 'Mở rộng' : 'Thu gọn'}</span>
        </button>
      </div>

      {/* ── Bên phải: Hộp công việc sắp tới riêng biệt có danh sách cuộn nhỏ ── */}
      {!isCollapsed && (
        <div className="hero-banner-right">
          <div className="hero-upcoming-box" title="Danh sách công việc sắp tới hôm nay">
            <div className="hero-upcoming-box-header">
              <div className="hero-upcoming-box-title">
                <CalendarClock size={13} className="hero-upcoming-icon" />
                <span>Công việc sắp tới</span>
              </div>
              {totalUpcomingCount > 0 ? (
                <span className="hero-upcoming-box-count">
                  {currentTask ? '1 đang làm · ' : ''}
                  {upcomingTasks.length > 0 ? `${upcomingTasks.length} sắp tới` : '1 việc'}
                </span>
              ) : null}
            </div>

            {/* Danh sách nhỏ cuộn được */}
            <div className="hero-upcoming-scroll-list">
              {currentTask && (
                <div
                  className="hero-upcoming-item is-live"
                  onClick={() => onSelectTask?.(currentTask)}
                  title={`Đang diễn ra: ${currentTask.title} (${formatTimeOnly(currentTask.startTime)}–${formatTimeOnly(currentTask.endTime)})`}
                >
                  <span className="hero-upcoming-dot live-dot" />
                  <span className="hero-upcoming-time">{formatTimeOnly(currentTask.startTime)}</span>
                  <span className="hero-upcoming-name">{currentTask.title}</span>
                  <span className="hero-upcoming-tag">Đang làm</span>
                </div>
              )}

              {upcomingTasks.map((task) => (
                <div
                  key={task.id}
                  className="hero-upcoming-item"
                  onClick={() => onSelectTask?.(task)}
                  title={`Sắp tới: ${task.title} (${formatTimeOnly(task.startTime)}–${formatTimeOnly(task.endTime)}) · Nhấp để xem`}
                >
                  <span className="hero-upcoming-dot" />
                  <span className="hero-upcoming-time">{formatTimeOnly(task.startTime)}</span>
                  <span className="hero-upcoming-name">{task.title}</span>
                </div>
              ))}

              {totalUpcomingCount === 0 && (
                <div className="hero-upcoming-empty">
                  <CheckCircle2 size={12} />
                  <span>Không còn việc sắp tới hôm nay</span>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
