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
    <div className="stats-bar-slim" role="region" aria-label="Thống kê tuần">
      {/* Thẻ 1: Công việc tuần này */}
      <div className="stat-card-slim" title="Số lượng công việc trong tuần được chọn">
        <div className="stat-icon-slim stat-icon-purple">
          <CheckSquare size={16} />
        </div>
        <div className="stat-content-slim">
          <span className="stat-label-slim">Công việc tuần này</span>
          <div className="stat-val-row-slim">
            <span className="stat-value-slim">{stats.totalTasksThisWeek}</span>
            <span className="stat-sub-slim">
              {stats.totalTasksThisWeek > 0
                ? `(${stats.completedTasks} đã hoàn thành)`
                : '(Chưa có việc)'}
            </span>
          </div>
        </div>
      </div>

      {/* Thẻ 2: Thời gian tập trung */}
      <div className="stat-card-slim" title="Tổng thời gian học tập & công việc được lên lịch trong tuần">
        <div className="stat-icon-slim stat-icon-blue">
          <Clock size={16} />
        </div>
        <div className="stat-content-slim">
          <span className="stat-label-slim">Thời gian tập trung</span>
          <div className="stat-val-row-slim">
            <span className="stat-value-slim">{formattedHours} giờ</span>
            <span className="stat-sub-slim stat-sub-truncate" title={stats.focusLabel}>
              {stats.focusLabel ? `(${stats.focusLabel})` : ''}
            </span>
          </div>
        </div>
      </div>

      {/* Thẻ 3: Khoảng trống phù hợp */}
      <div className="stat-card-slim" title="Khoảng thời gian trống phù hợp sẵn sàng để AI xếp lịch">
        <div className="stat-icon-slim stat-icon-green">
          <Sparkles size={16} />
        </div>
        <div className="stat-content-slim">
          <span className="stat-label-slim">Khoảng trống phù hợp</span>
          <div className="stat-val-row-slim">
            <span className="stat-value-slim">{stats.freeSlots}</span>
            <span className="stat-sub-slim">
              {stats.freeSlots > 0 ? '(Sẵn sàng để AI xếp)' : '(Lịch đã kín)'}
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}
