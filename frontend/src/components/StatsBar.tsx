import { CheckSquare, Clock, Sparkles } from 'lucide-react';
import { UserStats } from '../types';

interface StatsBarProps {
  stats: UserStats;
}

export default function StatsBar({ stats }: StatsBarProps) {
  const formattedHours = Number.isInteger(stats.focusHours)
    ? stats.focusHours
    : Number(stats.focusHours.toFixed(1));

  return (
    <div className="stats-bar">
      <div className="stat-card" title="Số lượng công việc trong tuần được chọn">
        <div className="stat-icon stat-icon-purple">
          <CheckSquare size={20} />
        </div>
        <div className="stat-content">
          <span className="stat-label">Công việc tuần này</span>
          <span className="stat-value">{stats.totalTasksThisWeek}</span>
          <span className="stat-sub">
            {stats.totalTasksThisWeek > 0
              ? `${stats.completedTasks} đã hoàn thành`
              : 'Chưa có công việc'}
          </span>
        </div>
      </div>

      <div className="stat-card" title="Tổng thời gian học tập & công việc được lên lịch trong tuần này">
        <div className="stat-icon stat-icon-blue">
          <Clock size={20} />
        </div>
        <div className="stat-content">
          <span className="stat-label">Thời gian tập trung</span>
          <span className="stat-value">{formattedHours} giờ</span>
          <span className="stat-sub">{stats.focusLabel || 'Chưa có lịch tuần này'}</span>
        </div>
      </div>

      <div className="stat-card" title="Khoảng thời gian trống phù hợp trong khung giờ làm việc">
        <div className="stat-icon stat-icon-green">
          <Sparkles size={20} />
        </div>
        <div className="stat-content">
          <span className="stat-label">Khoảng trống phù hợp</span>
          <span className="stat-value">{stats.freeSlots}</span>
          <span className="stat-sub">
            {stats.freeSlots > 0 ? 'Sẵn sàng để AI xếp' : 'Lịch tuần đã kín'}
          </span>
        </div>
      </div>
    </div>
  );
}
