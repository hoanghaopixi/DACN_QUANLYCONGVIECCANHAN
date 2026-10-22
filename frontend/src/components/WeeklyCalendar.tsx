import { useState, useRef, useEffect } from 'react';
import { RotateCw, MoreVertical, GripVertical } from 'lucide-react';
import { TimeBlock, Category } from '../types';
import { checkWorkHoursConstraint } from '../utils/workHoursConstraint';

interface WeeklyCalendarProps {
  timeBlocks: (TimeBlock & { isRecurring?: boolean; subtext?: string; colorOverride?: string })[];
  categories: Category[];
  selectedCategory: string | null;
  statusFilter: string;
  onSelectBlock: (block: TimeBlock) => void;
  onAddAtSlot: (dayIndex: number, hour: number) => void;
  onRescheduleTask?: (blockId: number, newStartTime: string, newEndTime: string) => void;
  days?: { name: string; dateNum: number; dateStr: string; isToday: boolean }[];
}

// Full 24 hours: 0:00 to 23:59
const HOURS = Array.from({ length: 24 }, (_, i) => i);
// 60px per hour = exact 1px per minute
const HOUR_HEIGHT = 60;

const getBaseWeekDays = () => {
  const now = new Date();
  const day = now.getDay();
  const diff = now.getDate() - day + (day === 0 ? -6 : 1);
  const monday = new Date(now.getFullYear(), now.getMonth(), diff);
  const names = ['Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy', 'Chủ Nhật'];
  return Array.from({ length: 7 }, (_, i) => {
    const d = new Date(monday);
    d.setDate(monday.getDate() + i);
    const ymd = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
    return {
      name: names[i],
      dateNum: d.getDate(),
      dateStr: ymd,
    };
  });
};
const DAYS_BASE = getBaseWeekDays();

interface PositionedBlock {
  block: any;
  startHour: number;
  endHour: number;
  durationMs: number;
  top: number;
  height: number;
  colIndex: number;
  totalCols: number;
}

function computeLayoutBlocks(blocks: any[]): PositionedBlock[] {
  if (!blocks || blocks.length === 0) return [];

  const parsed = blocks.map((b) => {
    const s = new Date(b.startTime);
    const e = new Date(b.endTime);
    const startH = s.getHours() + s.getMinutes() / 60;
    let endH = e.getHours() + e.getMinutes() / 60;
    if (endH <= startH) endH = startH + 1;
    const durMs = Math.max(1800000, e.getTime() - s.getTime());
    return {
      block: b,
      startHour: startH,
      endHour: endH,
      durationMs: durMs,
      top: Math.round(startH * HOUR_HEIGHT),
      height: Math.max(34, Math.round((endH - startH) * HOUR_HEIGHT) - 2),
    };
  });

  parsed.sort((a, b) => {
    if (Math.abs(a.startHour - b.startHour) > 0.001) return a.startHour - b.startHour;
    return (b.endHour - b.startHour) - (a.endHour - a.startHour);
  });

  const clusters: (typeof parsed)[] = [];
  let currentCluster: typeof parsed = [];
  let clusterEnd = -1;

  for (const item of parsed) {
    if (currentCluster.length === 0) {
      currentCluster.push(item);
      clusterEnd = item.endHour;
    } else {
      if (item.startHour < clusterEnd - 0.001) {
        currentCluster.push(item);
        clusterEnd = Math.max(clusterEnd, item.endHour);
      } else {
        clusters.push(currentCluster);
        currentCluster = [item];
        clusterEnd = item.endHour;
      }
    }
  }
  if (currentCluster.length > 0) {
    clusters.push(currentCluster);
  }

  const result: PositionedBlock[] = [];

  for (const cluster of clusters) {
    const colEnds: number[] = [];
    const assigned: { item: typeof parsed[0]; colIndex: number }[] = [];

    for (const item of cluster) {
      let placedCol = -1;
      for (let c = 0; c < colEnds.length; c++) {
        if (item.startHour >= colEnds[c] - 0.001) {
          placedCol = c;
          colEnds[c] = item.endHour;
          break;
        }
      }
      if (placedCol === -1) {
        placedCol = colEnds.length;
        colEnds.push(item.endHour);
      }
      assigned.push({ item, colIndex: placedCol });
    }

    const totalCols = Math.max(1, colEnds.length);
    for (const a of assigned) {
      result.push({
        block: a.item.block,
        startHour: a.item.startHour,
        endHour: a.item.endHour,
        durationMs: a.item.durationMs,
        top: a.item.top,
        height: a.item.height,
        colIndex: a.colIndex,
        totalCols,
      });
    }
  }

  return result;
}

export default function WeeklyCalendar({
  timeBlocks,
  categories,
  selectedCategory,
  statusFilter,
  onSelectBlock,
  onAddAtSlot,
  onRescheduleTask,
  days,
}: WeeklyCalendarProps) {
  const [hoveredBlockId, setHoveredBlockId] = useState<number | null>(null);
  const [draggingBlockId, setDraggingBlockId] = useState<number | null>(null);
  const scrollRef = useRef<HTMLDivElement>(null);

  const now = new Date();
  const currentYMD = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
  const DAYS = days && days.length > 0 ? days : DAYS_BASE.map((day) => ({
    ...day,
    isToday: day.dateStr === currentYMD,
  }));

  const currentHour = now.getHours();
  const currentMinute = now.getMinutes();
  // Live GMT+7 line across full 24-hour range
  const currentTimeTop = currentHour * HOUR_HEIGHT + (currentMinute / 60) * HOUR_HEIGHT;

  // Auto-scroll to morning / current time on initial mount
  useEffect(() => {
    if (scrollRef.current) {
      const currentH = now.getHours();
      const targetHour = Math.max(0, Math.min(18, currentH >= 7 ? currentH - 1 : 7));
      scrollRef.current.scrollTop = targetHour * HOUR_HEIGHT;
    }
  }, []);

  // Category / override color mapper
  const getCategoryStyles = (categoryType: string, colorOverride?: string) => {
    if (colorOverride === '#3B82F6') {
      return {
        borderLeft: '3px solid #3B82F6',
        backgroundColor: '#EFF6FF',
        titleColor: '#1E40AF',
        timeColor: '#3B82F6',
      };
    }
    if (colorOverride === '#8B5CF6') {
      return {
        borderLeft: '3px solid #8B5CF6',
        backgroundColor: '#F3E8FF',
        titleColor: '#5B21B6',
        timeColor: '#7C3AED',
      };
    }
    if (colorOverride === '#10B981') {
      return {
        borderLeft: '3px solid #10B981',
        backgroundColor: '#ECFDF5',
        titleColor: '#065F46',
        timeColor: '#059669',
      };
    }
    if (colorOverride === '#F59E0B') {
      return {
        borderLeft: '3px solid #F59E0B',
        backgroundColor: '#FFFBEB',
        titleColor: '#92400E',
        timeColor: '#D97706',
      };
    }
    switch (categoryType) {
      case 'STUDY':
        return {
          borderLeft: '3px solid #3B82F6',
          backgroundColor: '#EFF6FF',
          titleColor: '#1E40AF',
          timeColor: '#3B82F6',
        };
      case 'WORK':
        return {
          borderLeft: '3px solid #8B5CF6',
          backgroundColor: '#F3E8FF',
          titleColor: '#5B21B6',
          timeColor: '#7C3AED',
        };
      case 'HEALTH':
        return {
          borderLeft: '3px solid #10B981',
          backgroundColor: '#ECFDF5',
          titleColor: '#065F46',
          timeColor: '#059669',
        };
      case 'ENTERTAINMENT':
        return {
          borderLeft: '3px solid #F59E0B',
          backgroundColor: '#FFFBEB',
          titleColor: '#92400E',
          timeColor: '#D97706',
        };
      default:
        return {
          borderLeft: '3px solid #6C5CE7',
          backgroundColor: '#F0EDFF',
          titleColor: '#5A4BD1',
          timeColor: '#6C5CE7',
        };
    }
  };

  // Filter blocks
  const filteredBlocks = timeBlocks.filter((b) => {
    if (statusFilter !== 'ALL' && b.status !== statusFilter) return false;
    if (selectedCategory) {
      const cat = categories.find((c) => c.name === selectedCategory);
      if (cat && b.categoryType !== cat.type) return false;
    }
    return true;
  });

  return (
    <div className="weekly-calendar">
      {/* ── Days Header ── */}
      <div className="calendar-header" style={{ gridTemplateColumns: `60px repeat(${DAYS.length}, 1fr)` }}>
        <div className="time-gutter-header">GMT+7</div>
        {DAYS.map((day) => (
          <div key={day.dateStr} className={`day-header ${day.isToday ? 'today' : ''}`}>
            <span className="day-name">{day.name}</span>
            <span className={`day-number ${day.isToday ? 'today-number' : ''}`}>
              {day.dateNum}
            </span>
          </div>
        ))}
      </div>

      {/* ── Scrollable Grid Body ── */}
      <div className="calendar-scroll" ref={scrollRef}>
        <div className="calendar-body">
          {/* Time Gutter (0:00 to 23:59) */}
          <div className="time-gutter">
            {HOURS.map((hour) => (
              <div key={hour} className="time-label">
                {hour.toString().padStart(2, '0')}:00
              </div>
            ))}
            <div className="time-label time-label-end" style={{ height: 24, fontSize: '0.68rem', color: 'var(--text-muted)' }}>
              23:59
            </div>
          </div>

          {/* Days Columns */}
          <div className="days-grid" style={{ gridTemplateColumns: `repeat(${DAYS.length}, 1fr)` }}>
            {DAYS.map((day, dayIndex) => {
              // Find events on this day
              const dayBlocks = filteredBlocks.filter((b) => {
                const d = new Date(b.startTime);
                const y = d.getFullYear();
                const m = String(d.getMonth() + 1).padStart(2, '0');
                const date = String(d.getDate()).padStart(2, '0');
                return `${y}-${m}-${date}` === day.dateStr;
              });

              return (
                <div
                  key={day.dateStr}
                  className={`day-column ${day.isToday ? 'today-column' : ''}`}
                >
                  {/* Hour cells (clickable to add task, droppable for rescheduling) */}
                  {HOURS.map((hour) => (
                    <div
                      key={hour}
                      className="hour-cell"
                      onClick={() => onAddAtSlot(dayIndex, hour)}
                      onDragOver={(e) => {
                        e.preventDefault();
                        e.dataTransfer.dropEffect = 'move';
                        e.currentTarget.classList.add('drop-target');
                      }}
                      onDragLeave={(e) => {
                        e.currentTarget.classList.remove('drop-target');
                      }}
                      onDrop={(e) => {
                        e.preventDefault();
                        e.currentTarget.classList.remove('drop-target');
                        try {
                          const raw = e.dataTransfer.getData('text/plain');
                          if (!raw) return;
                          const { id, durationMs } = JSON.parse(raw);
                          const targetDate = day.dateStr;
                          const startHourStr = hour.toString().padStart(2, '0');
                          const newStartIso = `${targetDate}T${startHourStr}:00:00+07:00`;

                          const startDate = new Date(newStartIso);
                          const endDate = new Date(startDate.getTime() + (durationMs || 3600000));
                          let endHourStr = endDate.getHours().toString().padStart(2, '0');
                          let endMinStr = endDate.getMinutes().toString().padStart(2, '0');
                          if (hour === 23 && endDate.getDate() !== startDate.getDate()) {
                            endHourStr = '23';
                            endMinStr = '59';
                          }
                          const newEndIso = `${targetDate}T${endHourStr}:${endMinStr}:00+07:00`;

                          const targetBlock = timeBlocks.find((b) => b.id === id);
                          const validation = checkWorkHoursConstraint(newStartIso, newEndIso, targetBlock?.categoryType || 'WORK');
                          if (validation.isOutsideWorkHours) {
                            const confirmed = window.confirm(`${validation.reason}\n\n${validation.promptMessage}`);
                            if (!confirmed) return;
                          }

                          if (onRescheduleTask) {
                            onRescheduleTask(id, newStartIso, newEndIso);
                          }
                        } catch (err) {
                          console.error('Error handling task drop:', err);
                        }
                      }}
                      title={`Nhấp để thêm công việc lúc ${hour}:00 · Hoặc thả công việc vào đây để dời giờ`}
                    />
                  ))}

                  {/* Current GMT+7 time indicator line on today */}
                  {day.isToday && currentTimeTop !== null && (
                    <div
                      style={{
                        position: 'absolute',
                        top: `${currentTimeTop}px`,
                        left: 0,
                        right: 0,
                        height: 2,
                        backgroundColor: '#EF4444',
                        zIndex: 25,
                        pointerEvents: 'none',
                        display: 'flex',
                        alignItems: 'center',
                      }}
                      title={`Thời gian hiện tại: ${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')} (GMT+7)`}
                    >
                      <div
                        style={{
                          width: 8,
                          height: 8,
                          borderRadius: '50%',
                          backgroundColor: '#EF4444',
                          marginLeft: -4,
                          boxShadow: '0 0 0 2px rgba(239,68,68,0.25)',
                        }}
                      />
                    </div>
                  )}

                  {/* Event Blocks - Tự động chia cột bên cạnh nhau khi trùng giờ, không chèn đè lên nhau */}
                  {computeLayoutBlocks(dayBlocks).map(({ block, top, height, durationMs, colIndex, totalCols }) => {
                    const start = new Date(block.startTime);
                    const end = new Date(block.endTime);
                    const styles = getCategoryStyles(block.categoryType, block.colorOverride);
                    const timeRangeStr = `${start.getHours().toString().padStart(2, '0')}:${start.getMinutes().toString().padStart(2, '0')}–${end.getHours().toString().padStart(2, '0')}:${end.getMinutes().toString().padStart(2, '0')}`;

                    const isSuperTiny = height < 32;
                    const isHovered = hoveredBlockId === block.id;
                    const isDragging = draggingBlockId === block.id;

                    const colWidthPercent = 100 / totalCols;
                    const leftPercent = colIndex * colWidthPercent;

                    // Tính toán độ nhô to ra 4 phía (trên, dưới, trái, phải) khi hover
                    let calculatedTop = top;
                    let calculatedLeft = '3px';
                    let calculatedWidth = 'calc(100% - 6px)';

                    if (totalCols > 1) {
                      if (isHovered) {
                        calculatedTop = Math.max(0, top - 6);
                        if (colIndex === 0) {
                          calculatedLeft = '2px';
                          calculatedWidth = `max(calc(${colWidthPercent}% + 56px), 175px)`;
                        } else if (colIndex === totalCols - 1) {
                          calculatedLeft = `calc(${leftPercent}% - 56px)`;
                          calculatedWidth = `max(calc(${colWidthPercent}% + 56px), 175px)`;
                        } else {
                          calculatedLeft = `calc(${leftPercent}% - 28px)`;
                          calculatedWidth = `max(calc(${colWidthPercent}% + 56px), 175px)`;
                        }
                      } else {
                        calculatedLeft = `calc(${leftPercent}% + 2px)`;
                        calculatedWidth = `calc(${colWidthPercent}% - 4px)`;
                      }
                    } else {
                      if (isHovered) {
                        calculatedTop = Math.max(0, top - 6);
                        calculatedLeft = '-4px';
                        calculatedWidth = 'calc(100% + 8px)';
                      }
                    }

                    return (
                      <div
                        key={block.id}
                        draggable={true}
                        onDragStart={(e) => {
                          e.dataTransfer.setData(
                            'text/plain',
                            JSON.stringify({ id: block.id, durationMs })
                          );
                          e.dataTransfer.effectAllowed = 'move';
                          setDraggingBlockId(block.id);
                        }}
                        onDragEnd={() => {
                          setDraggingBlockId(null);
                        }}
                        className={`event-block ${isSuperTiny ? 'super-tiny-block' : ''} ${isHovered ? 'is-hovered' : ''} ${isDragging ? 'is-dragging' : ''}`}
                        style={{
                          top: `${calculatedTop}px`,
                          height: isHovered ? 'auto' : `${height}px`,
                          minHeight: isHovered ? `${Math.max(height + 18, 92)}px` : undefined,
                          left: calculatedLeft,
                          width: calculatedWidth,
                          maxWidth: isHovered ? '280px' : undefined,
                          right: 'auto',
                          zIndex: isDragging ? 120 : isHovered ? 100 : 2 + colIndex,
                          borderLeft: styles.borderLeft,
                          backgroundColor: styles.backgroundColor,
                        }}
                        onMouseEnter={() => setHoveredBlockId(block.id)}
                        onMouseLeave={() => setHoveredBlockId(null)}
                        onClick={(e) => {
                          e.stopPropagation();
                          onSelectBlock(block);
                        }}
                        title={`${block.title} (${timeRangeStr})${block.note ? `\n💬 ${block.note}` : ''} · Kéo thả để dời giờ`}
                      >
                        {/* 1. Tên công việc */}
                        <div className="event-title" style={{ color: styles.titleColor }}>
                          {block.title}
                        </div>

                        {/* 2. Thời gian: luôn hiển thị dưới tên công việc */}
                        <div className="event-time" style={{ color: styles.timeColor }}>
                          {timeRangeStr}
                          {block.isRecurring && (
                            <RotateCw
                              size={10}
                              style={{ marginLeft: 4, display: 'inline', verticalAlign: '-1px' }}
                            />
                          )}
                        </div>

                        {/* 3. Chú thích: Khi thẻ ngắn (<90px), chỉ hiện chú thích khi hover bung to ra để không làm chật thẻ */}
                        {block.note && block.note.trim() ? (
                          <div
                            className={`event-note ${height < 90 ? 'hover-only-note' : ''}`}
                            style={{ color: styles.timeColor }}
                            title={block.note}
                          >
                            <span style={{ fontStyle: 'normal', marginRight: 3 }}>💬</span>
                            <span>{block.note.trim()}</span>
                          </div>
                        ) : null}

                        {/* 4. Trạng thái / Subtext badge */}
                        {block.subtext && (height >= 48 || isHovered) && (
                          <div
                            className={`event-status ${
                              block.status === 'COMPLETED'
                                ? 'status-completed'
                                : block.status === 'IN_PROGRESS'
                                ? 'status-in_progress'
                                : 'status-todo'
                            }`}
                            style={{ marginTop: 2 }}
                          >
                            {block.subtext}
                          </div>
                        )}

                        {/* 3 dots icon for active item */}
                        {block.status === 'IN_PROGRESS' && (
                          <MoreVertical
                            size={13}
                            style={{
                              position: 'absolute',
                              bottom: 4,
                              right: 4,
                              color: '#64748B',
                              cursor: 'pointer',
                            }}
                          />
                        )}
                      </div>
                    );
                  })}
                </div>
              );
            })}
          </div>
        </div>
      </div>

      {/* ── Footer Bar ── */}
      <div className="calendar-footer-bar">
        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <GripVertical size={14} />
          <span>Kéo thả để dời giờ · Bấm ô trống để thêm việc</span>
        </div>
        <div>
          <span>{filteredBlocks.length} công việc · Đã đồng bộ</span>
        </div>
      </div>
    </div>
  );
}
