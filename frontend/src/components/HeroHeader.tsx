import { Plus } from 'lucide-react';
import CurrentTaskWidget from './CurrentTaskWidget';
import { TimeBlock } from '../types';

interface HeroHeaderProps {
  userName?: string;
  todayTasksCount?: number;
  timeBlocks?: TimeBlock[];
  onAddTaskClick: () => void;
  onSelectTask?: (task: TimeBlock) => void;
}

export default function HeroHeader({
  userName = 'Minh Anh',
  todayTasksCount = 3,
  timeBlocks = [],
  onAddTaskClick,
  onSelectTask,
}: HeroHeaderProps) {
  return (
    <div className="hero-section">
      <div className="hero-left">
        <h1 className="hero-title">Một tuần hiệu quả cùng SmartTask.</h1>
        <p className="hero-subtitle">
          Chào {userName}, hôm nay có {todayTasksCount} công việc. SmartTask giúp bạn cân bằng việc cần làm và thời gian cho mình.
        </p>
      </div>

      <div className="hero-right">
        <CurrentTaskWidget timeBlocks={timeBlocks} onSelectTask={onSelectTask} />
        <button className="add-task-btn" onClick={onAddTaskClick} id="add-task-btn">
          <Plus size={18} strokeWidth={2.5} />
          <span>Thêm công việc</span>
        </button>
      </div>
    </div>
  );
}
