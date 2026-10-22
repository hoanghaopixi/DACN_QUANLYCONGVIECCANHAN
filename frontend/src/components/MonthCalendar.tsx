import { TimeBlock, Category } from '../types';
import { Plus } from 'lucide-react';

interface MonthCalendarProps {
  currentDate: Date;
  timeBlocks: (TimeBlock & { isRecurring?: boolean; subtext?: string; colorOverride?: string; priority?: string })[];
  categories: Category[];
  selectedCategory: string | null;
  statusFilter: string;
  onSelectBlock: (block: TimeBlock) => void;
  onAddAtDate: (dateStr: string) => void;
}

export default function MonthCalendar({
  currentDate,
  timeBlocks,
  categories,
  selectedCategory,
  statusFilter,
  onSelectBlock,
  onAddAtDate,
}: MonthCalendarProps) {
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
    if (statusFilter !== 'ALL' && b.status !== statusFilter) return false;
    if (selectedCategory) {
      const cat = categories.find((c) => c.name === selectedCategory);
      if (cat && b.categoryType !== cat.type) return false;
    }
    return true;
  });

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
      style={{
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden',
        background: 'var(--surface)',
        border: '1px solid var(--border)',
        borderRadius: '14px 14px 0 0',
      }}
    >
      {/* ── Day Headers ── */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(7, 1fr)',
          borderBottom: '1px solid var(--border)',
          background: 'var(--border-light)',
          flexShrink: 0,
        }}
      >
        {weekHeaders.map((head) => (
          <div
            key={head}
            style={{
              padding: '10px 8px',
              textAlign: 'center',
              fontSize: '0.78rem',
              fontWeight: 600,
              color: 'var(--text-muted)',
            }}
          >
            {head}
          </div>
        ))}
      </div>

      {/* ── Month Grid Body ── */}
      <div
        style={{
          flex: 1,
          display: 'grid',
          gridTemplateColumns: 'repeat(7, 1fr)',
          gridAutoRows: 'minmax(110px, 1fr)',
          overflowY: 'auto',
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
          });

          return (
            <div
              key={index}
              style={{
                borderRight: '1px solid var(--border-light)',
                borderBottom: '1px solid var(--border-light)',
                padding: '6px 8px',
                background: isToday
                  ? 'rgba(108, 92, 231, 0.03)'
                  : isCurrentMonth
                  ? 'transparent'
                  : 'rgba(0, 0, 0, 0.015)',
                opacity: isCurrentMonth ? 1 : 0.45,
                display: 'flex',
                flexDirection: 'column',
                gap: 4,
                position: 'relative',
                transition: 'background 0.15s',
              }}
              onMouseEnter={(e) => {
                if (isCurrentMonth) (e.currentTarget.style.backgroundColor = 'var(--border-light)');
              }}
              onMouseLeave={(e) => {
                if (isCurrentMonth) {
                  e.currentTarget.style.backgroundColor = isToday ? 'rgba(108, 92, 231, 0.03)' : 'transparent';
                }
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
                    color: isToday ? '#fff' : 'var(--text)',
                    background: isToday ? 'var(--primary)' : 'transparent',
                  }}
                >
                  {cellDate.getDate()}
                </span>

                {isCurrentMonth && (
                  <button
                    type="button"
                    onClick={() => onAddAtDate(cellDateStr)}
                    title={`Thêm công việc ngày ${cellDate.getDate()}/${month + 1}`}
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

              {/* Day Tasks List (Chips) */}
              <div style={{ display: 'flex', flexDirection: 'column', gap: 3, overflowY: 'hidden' }}>
                {dayTasks.slice(0, 3).map((task) => {
                  const color = getCategoryColor(task.categoryType, task.colorOverride);
                  const start = new Date(task.startTime);
                  const timeStr = `${start.getHours().toString().padStart(2, '0')}:${start.getMinutes().toString().padStart(2, '0')}`;
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
                        backgroundColor: `${color}18`,
                        borderLeft: `2.5px solid ${color}`,
                        color: 'var(--text)',
                        cursor: 'pointer',
                        whiteSpace: 'nowrap',
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        transition: 'transform 0.1s',
                      }}
                    >
                      <span style={{ fontSize: '0.66rem', color: color, fontWeight: 700 }}>
                        {timeStr}
                      </span>
                      <span style={{ overflow: 'hidden', textOverflow: 'ellipsis' }}>
                        {task.title}
                      </span>
                    </div>
                  );
                })}

                {dayTasks.length > 3 && (
                  <span
                    style={{
                      fontSize: '0.68rem',
                      color: 'var(--primary)',
                      fontWeight: 600,
                      paddingLeft: 4,
                    }}
                  >
                    +{dayTasks.length - 3} việc nữa
                  </span>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
