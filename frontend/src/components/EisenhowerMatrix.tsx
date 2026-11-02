import {
  CheckCircle2, Circle, Flame, Plus,
  CheckSquare, Clock
} from 'lucide-react';
import { TimeBlock, EisenhowerQuadrant, Category } from '../types';
import { taskService } from '../services/taskService';
import { formatDateDDMMYYYY, formatTime24 } from '../utils/dateTimeFormat';
import HoverSelect from './HoverSelect';

interface EisenhowerMatrixProps {
  tasks: TimeBlock[];
  categories?: Category[];
  showHeader?: boolean;
  onAddTask: (quadrant: EisenhowerQuadrant) => void;
  onEditTask: (task: TimeBlock) => void;
  onToggleStatus: (task: TimeBlock) => void;
  onShowToast: (msg: string) => void;
  onReload: () => void;
}

interface QuadrantConfig {
  id: EisenhowerQuadrant;
  number: string;
  title: string;
  subtitle: string;
  headerColor: string;
  headerBg: string;
  borderColor: string;
  badgeBg: string;
  badgeColor: string;
  description: string;
}

const QUADRANTS: QuadrantConfig[] = [
  {
    id: 'DO_FIRST',
    number: 'Q1',
    title: 'LÀM NGAY',
    subtitle: 'Khẩn cấp & Quan trọng',
    headerColor: '#DC2626',
    headerBg: 'rgba(239, 68, 68, 0.08)',
    borderColor: 'rgba(239, 68, 68, 0.25)',
    badgeBg: '#FEE2E2',
    badgeColor: '#B91C1C',
    description: 'Ưu tiên giải quyết ngay: Deadline gấp, sự cố đột xuất, việc sống còn.',
  },
  {
    id: 'SCHEDULE',
    number: 'Q2',
    title: 'LÊN LỊCH',
    subtitle: 'Quan trọng, Không khẩn cấp',
    headerColor: '#4F46E5',
    headerBg: 'rgba(79, 70, 229, 0.08)',
    borderColor: 'rgba(79, 70, 229, 0.25)',
    badgeBg: '#EDE9FE',
    badgeColor: '#4338CA',
    description: 'Đầu tư phát triển bản thân: Học tập, rèn luyện sức khỏe, kế hoạch tương lai.',
  },
  {
    id: 'DELEGATE',
    number: 'Q3',
    title: 'ỦY QUYỀN / LÀM NHANH',
    subtitle: 'Khẩn cấp, Không quan trọng',
    headerColor: '#D97706',
    headerBg: 'rgba(245, 158, 11, 0.08)',
    borderColor: 'rgba(245, 158, 11, 0.25)',
    badgeBg: '#FEF3C7',
    badgeColor: '#B45309',
    description: 'Xử lý gọn lẹ hoặc nhờ hỗ trợ: Tin nhắn bất chợt, việc lặt vặt chen ngang.',
  },
  {
    id: 'DONT_DO',
    number: 'Q4',
    title: 'CÂN NHẮC LOẠI BỎ',
    subtitle: 'Không khẩn cấp & Không quan trọng',
    headerColor: '#64748B',
    headerBg: 'rgba(100, 116, 139, 0.08)',
    borderColor: 'rgba(100, 116, 139, 0.25)',
    badgeBg: '#F1F5F9',
    badgeColor: '#475569',
    description: 'Hạn chế tối đa: Lướt mạng vô định, làm phiền, thói quen lãng phí thời gian.',
  },
];

export default function EisenhowerMatrix({
  tasks,
  categories: _categories,
  showHeader = true,
  onAddTask,
  onEditTask,
  onToggleStatus,
  onShowToast,
  onReload,
}: EisenhowerMatrixProps) {
  const handleMoveQuadrant = async (task: TimeBlock, targetQuadrant: EisenhowerQuadrant) => {
    try {
      await taskService.updateQuadrant(task.id, targetQuadrant);
      const qCfg = QUADRANTS.find((q) => q.id === targetQuadrant);
      onShowToast(`✓ Đã chuyển "${task.title}" sang ô ${qCfg?.number}: ${qCfg?.title}!`);
      onReload();
    } catch (err) {
      console.error('Lỗi khi chuyển góc phần tư:', err);
      onShowToast('Lỗi khi cập nhật góc phần tư');
    }
  };

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

  const formatTaskTime = (t: TimeBlock) => {
    if (!t.startTime) return null;
    try {
      const d = new Date(t.startTime);
      return `${formatTime24(d)} · ${formatDateDDMMYYYY(d)}`;
    } catch {
      return null;
    }
  };

  const getQuadrantTasks = (qId: EisenhowerQuadrant) => {
    const list = tasks.filter((t) => {
      // Default to SCHEDULE if not specified
      const quad = t.eisenhowerQuadrant || 'SCHEDULE';
      return quad === qId;
    });

    // Sort by time, and push completed tasks to the bottom!
    return list.sort((a, b) => {
      const aDone = a.status === 'COMPLETED';
      const bDone = b.status === 'COMPLETED';
      if (aDone !== bDone) {
        return aDone ? 1 : -1;
      }
      return getTimeValue(a) - getTimeValue(b);
    });
  };

  const totalTasks = tasks.length;
  const completedTasks = tasks.filter((t) => t.status === 'COMPLETED').length;

  return (
    <div
      className="eisenhower-matrix-page"
      style={{ padding: showHeader ? '24px 28px' : 0, maxWidth: 1300, margin: '0 auto', width: '100%', height: '100%', minHeight: 0, display: 'flex', flexDirection: 'column', boxSizing: 'border-box' }}
    >
      {/* Top Header - only shown if showHeader is true */}
      {showHeader && (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 20 }}>
          <div>
            <h1 style={{ fontSize: '1.45rem', fontWeight: 800, color: 'var(--text)', display: 'flex', alignItems: 'center', gap: 10 }}>
              <span>Ma trận Eisenhower (4 Góc phần tư Ưu tiên)</span>
            </h1>
            <p style={{ fontSize: '0.86rem', color: 'var(--text-3)', marginTop: 4 }}>
              Phương pháp quản lý thời gian đỉnh cao của Tổng thống Eisenhower: Phân loại theo tính Khẩn cấp & Độ Quan trọng.
            </p>
          </div>

          {/* Quick summary stats */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <div style={{ padding: '6px 14px', borderRadius: 10, background: 'var(--surface)', border: '1px solid var(--border)', fontSize: '0.82rem', fontWeight: 600 }}>
              <span style={{ color: 'var(--text-3)' }}>Tổng việc: </span>
              <span style={{ color: 'var(--primary)', fontWeight: 700 }}>{totalTasks}</span>
            </div>
            <div style={{ padding: '6px 14px', borderRadius: 10, background: 'var(--surface)', border: '1px solid var(--border)', fontSize: '0.82rem', fontWeight: 600 }}>
              <span style={{ color: 'var(--text-3)' }}>Đã xong: </span>
              <span style={{ color: '#10B981', fontWeight: 700 }}>{completedTasks}</span>
            </div>
          </div>
        </div>
      )}

      {/* 2x2 Matrix Grid */}
      <div
        className="eisenhower-grid"
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(2, minmax(0, 1fr))',
          gap: 16,
          width: '100%',
          height: '100%',
          minHeight: 0,
        }}
      >
        {QUADRANTS.map((quad) => {
          const quadTasks = getQuadrantTasks(quad.id);
          const activeTasks = quadTasks.filter((t) => t.status !== 'COMPLETED');
          const doneTasks = quadTasks.filter((t) => t.status === 'COMPLETED');

          return (
            <div
              key={quad.id}
              className="eisenhower-quadrant"
              style={{
                background: 'var(--surface)',
                borderRadius: 18,
                border: `1.5px solid ${quad.borderColor}`,
                display: 'flex',
                flexDirection: 'column',
                overflow: 'hidden',
                boxShadow: 'var(--shadow-sm)',
                height: 'auto',
                maxHeight: 'none',
                minHeight: 0,
              }}
            >
              {/* Quadrant Header */}
              <div
                className="eisenhower-quadrant-header"
                style={{
                  padding: '12px 18px',
                  background: quad.headerBg,
                  borderBottom: `1px solid ${quad.borderColor}`,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                  <span
                    style={{
                      background: quad.badgeBg,
                      color: quad.badgeColor,
                      fontSize: '0.78rem',
                      fontWeight: 800,
                      padding: '2px 8px',
                      borderRadius: 6,
                    }}
                  >
                    {quad.number}
                  </span>
                  <div>
                    <div style={{ fontSize: '0.92rem', fontWeight: 800, color: quad.headerColor, letterSpacing: -0.2 }}>
                      {quad.title}
                    </div>
                    <div style={{ fontSize: '0.74rem', color: 'var(--text-3)', fontWeight: 500 }}>
                      {quad.subtitle}
                    </div>
                  </div>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <span
                    style={{
                      fontSize: '0.76rem',
                      fontWeight: 700,
                      padding: '2px 8px',
                      borderRadius: 20,
                      background: 'var(--surface)',
                      color: quad.headerColor,
                      border: '1px solid var(--border)',
                    }}
                  >
                    {activeTasks.length} việc{doneTasks.length > 0 ? ` · ${doneTasks.length} xong` : ''}
                  </span>
                  <button
                    type="button"
                    onClick={() => onAddTask(quad.id)}
                    title={`Thêm công việc vào ô ${quad.number}`}
                    style={{
                      width: 28,
                      height: 28,
                      borderRadius: 8,
                      background: quad.headerColor,
                      color: '#fff',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      cursor: 'pointer',
                      boxShadow: `0 2px 6px ${quad.headerColor}44`,
                    }}
                  >
                    <Plus size={15} />
                  </button>
                </div>
              </div>

              {/* Quadrant Task List */}
              <div
                className="eisenhower-quadrant-list"
                style={{
                  flex: 1,
                  padding: '12px 14px',
                  overflowY: 'auto',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: 8,
                }}
              >
                {quadTasks.length === 0 ? (
                  <div
                    style={{
                      flex: 1,
                      display: 'flex',
                      flexDirection: 'column',
                      alignItems: 'center',
                      justifyContent: 'center',
                      color: 'var(--text-muted)',
                      fontSize: '0.82rem',
                      textAlign: 'center',
                      padding: '20px 10px',
                    }}
                  >
                    <div style={{ fontSize: '1.4rem', marginBottom: 6 }}>
                      {quad.id === 'DO_FIRST' ? '🚀' : quad.id === 'SCHEDULE' ? '📅' : quad.id === 'DELEGATE' ? '🤝' : '🧹'}
                    </div>
                    <div>Chưa có công việc trong ô này</div>
                    <div style={{ fontSize: '0.74rem', marginTop: 4, maxWidth: 260, color: 'var(--text-muted)' }}>
                      {quad.description}
                    </div>
                  </div>
                ) : (
                  quadTasks.map((t) => {
                    const isCompleted = t.status === 'COMPLETED';
                    return (
                      <div
                        key={t.id}
                        className="eisenhower-task-row"
                        onClick={() => onEditTask(t)}
                        style={{
                          background: isCompleted ? 'var(--border-light)' : 'var(--surface)',
                          borderRadius: 12,
                          border: '1px solid var(--border)',
                          padding: '10px 12px',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'space-between',
                          gap: 10,
                          cursor: 'pointer',
                          transition: 'all 0.15s ease',
                          opacity: isCompleted ? 0.65 : 1,
                        }}
                      >
                        {/* Left Checkbox & Title */}
                        <div style={{ display: 'flex', alignItems: 'center', gap: 10, minWidth: 0, flex: 1 }}>
                          <button
                            type="button"
                            onClick={(e) => {
                              e.stopPropagation();
                              onToggleStatus(t);
                            }}
                            style={{
                              color: isCompleted ? '#10B981' : 'var(--text-muted)',
                              display: 'flex',
                              alignItems: 'center',
                              justifyContent: 'center',
                              flexShrink: 0,
                            }}
                          >
                            {isCompleted ? <CheckCircle2 size={18} fill="#10B981" color="#fff" /> : <Circle size={18} />}
                          </button>

                          <div style={{ minWidth: 0, flex: 1 }}>
                            <div
                              className="eisenhower-task-title"
                              style={{
                                fontSize: '0.86rem',
                                fontWeight: 600,
                                color: 'var(--text)',
                                textDecoration: isCompleted ? 'line-through' : 'none',
                                whiteSpace: 'nowrap',
                                overflow: 'hidden',
                                textOverflow: 'ellipsis',
                              }}
                            >
                              {t.title}
                            </div>
                            <div className="eisenhower-task-meta" style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 3 }}>
                              {formatTaskTime(t) && (
                                <span style={{ fontSize: '0.7rem', color: 'var(--text-3)', display: 'flex', alignItems: 'center', gap: 3 }}>
                                  <Clock size={11} />
                                  <span>{formatTaskTime(t)}</span>
                                </span>
                              )}

                              {t.subtasksCount && t.subtasksCount > 0 ? (
                                <span style={{ fontSize: '0.7rem', color: 'var(--text-3)', display: 'flex', alignItems: 'center', gap: 3 }}>
                                  <CheckSquare size={11} /> {t.completedSubtasksCount || 0}/{t.subtasksCount}
                                </span>
                              ) : null}

                              {t.isRecurring && (
                                <span style={{ fontSize: '0.7rem', color: '#EA580C', fontWeight: 700, display: 'flex', alignItems: 'center', gap: 2 }}>
                                  <Flame size={12} fill="#EA580C" /> {t.streakCount || 1}d
                                </span>
                              )}
                            </div>
                          </div>
                        </div>

                        {/* Right Quick Actions: Move Quadrant */}
                        <div style={{ display: 'flex', alignItems: 'center', gap: 6, flexShrink: 0 }} onClick={(e) => e.stopPropagation()}>

                          {/* Quick Quadrant Switcher Dropdown */}
                          <HoverSelect
                            value={quad.id}
                            onChange={(e) => handleMoveQuadrant(t, e.target.value as EisenhowerQuadrant)}
                            title="Di chuyển sang góc phần tư khác"
                            style={{
                              fontSize: '0.72rem',
                              fontWeight: 700,
                              padding: '3px 6px',
                              borderRadius: 6,
                              border: '1px solid var(--border)',
                              background: 'var(--surface)',
                              color: 'var(--text-3)',
                              outline: 'none',
                              cursor: 'pointer',
                            }}
                          >
                            <option value="DO_FIRST">Q1: Làm ngay</option>
                            <option value="SCHEDULE">Q2: Lên lịch</option>
                            <option value="DELEGATE">Q3: Xử lý nhanh</option>
                            <option value="DONT_DO">Q4: Loại bỏ</option>
                          </HoverSelect>
                        </div>
                      </div>
                    );
                  })
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
