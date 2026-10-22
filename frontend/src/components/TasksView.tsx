import { useState } from 'react';
import {
  CheckCircle2, Circle, Clock, Tag, AlertCircle, Plus,
  Search, Edit3, Trash2, CheckSquare
} from 'lucide-react';
import { TimeBlock, Category } from '../types';
import { matchesSearch } from '../utils/vietnameseSearch';
import ConfirmModal from './ConfirmModal';

interface TasksViewProps {
  tasks: (TimeBlock & { isRecurring?: boolean; subtext?: string; colorOverride?: string; priority?: string })[];
  categories: Category[];
  onAddTask: () => void;
  onEditTask: (task: TimeBlock) => void;
  onDeleteTask: (id: number) => void;
  onToggleStatus: (task: TimeBlock) => void;
}

export default function TasksView({
  tasks,
  categories,
  onAddTask,
  onEditTask,
  onDeleteTask,
  onToggleStatus,
}: TasksViewProps) {
  const [taskToDelete, setTaskToDelete] = useState<TimeBlock | null>(null);
  const [search, setSearch] = useState('');
  const [statusTab, setStatusTab] = useState<'ALL' | 'TODO' | 'IN_PROGRESS' | 'COMPLETED'>('ALL');
  const [selectedCat, setSelectedCat] = useState<string | null>(null);
  const [selectedPriority, setSelectedPriority] = useState<string>('ALL');

  // Stats
  const totalCount = tasks.length;
  const todoCount = tasks.filter((t) => t.status === 'TODO').length;
  const inProgressCount = tasks.filter((t) => t.status === 'IN_PROGRESS').length;
  const completedCount = tasks.filter((t) => t.status === 'COMPLETED').length;

  // Filtered tasks
  const filteredTasks = tasks.filter((t) => {
    if (statusTab !== 'ALL' && t.status !== statusTab) return false;
    if (selectedCat && t.categoryType !== selectedCat) return false;
    if (selectedPriority !== 'ALL' && t.priority !== selectedPriority) return false;
    if (search.trim() && !matchesSearch(t.title, search) && !matchesSearch(t.description, search)) return false;
    return true;
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
    const dateFormatted = `${s.getDate()}/${s.getMonth() + 1}/${s.getFullYear()}`;
    const timeFormatted = `${s.getHours().toString().padStart(2, '0')}:${s.getMinutes().toString().padStart(2, '0')} – ${e.getHours().toString().padStart(2, '0')}:${e.getMinutes().toString().padStart(2, '0')}`;
    return `${dayName}, ${dateFormatted} · ${timeFormatted}`;
  };

  return (
    <div style={{ padding: '24px 28px', maxWidth: 1200, margin: '0 auto', width: '100%' }}>
      {/* ── Top Header ── */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          flexWrap: 'wrap',
          gap: 16,
          marginBottom: 24,
        }}
      >
        <div>
          <h1 style={{ fontSize: '1.6rem', fontWeight: 800, color: 'var(--text)', margin: 0 }}>
            Quản lý Công việc
          </h1>
          <p style={{ fontSize: '0.85rem', color: 'var(--text-3)', margin: '4px 0 0' }}>
            Toàn bộ công việc trong không gian cá nhân được đồng bộ với cơ sở dữ liệu
          </p>
        </div>

        <button
          onClick={onAddTask}
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 8,
            padding: '10px 18px',
            borderRadius: 10,
            background: 'var(--primary)',
            color: '#fff',
            fontSize: '0.88rem',
            fontWeight: 600,
            boxShadow: '0 2px 8px rgba(108, 92, 231, 0.3)',
            cursor: 'pointer',
            transition: 'all 0.2s',
          }}
        >
          <Plus size={16} />
          <span>Thêm công việc</span>
        </button>
      </div>

      {/* ── 4 Summary Mini Cards ── */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
          gap: 14,
          marginBottom: 24,
        }}
      >
        <div
          onClick={() => setStatusTab('ALL')}
          style={{
            background: 'var(--surface)',
            border: statusTab === 'ALL' ? '2px solid var(--primary)' : '1px solid var(--border)',
            borderRadius: 12,
            padding: '14px 16px',
            cursor: 'pointer',
            boxShadow: 'var(--shadow-xs)',
            transition: 'all 0.2s',
          }}
        >
          <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)', fontWeight: 600 }}>TỔNG CÔNG VIỆC</div>
          <div style={{ fontSize: '1.45rem', fontWeight: 800, color: 'var(--text)', marginTop: 4 }}>
            {totalCount}
          </div>
        </div>

        <div
          onClick={() => setStatusTab('TODO')}
          style={{
            background: 'var(--surface)',
            border: statusTab === 'TODO' ? '2px solid var(--primary)' : '1px solid var(--border)',
            borderRadius: 12,
            padding: '14px 16px',
            cursor: 'pointer',
            boxShadow: 'var(--shadow-xs)',
            transition: 'all 0.2s',
          }}
        >
          <div style={{ fontSize: '0.78rem', color: '#64748B', fontWeight: 600 }}>CẦN LÀM (TODO)</div>
          <div style={{ fontSize: '1.45rem', fontWeight: 800, color: '#64748B', marginTop: 4 }}>
            {todoCount}
          </div>
        </div>

        <div
          onClick={() => setStatusTab('IN_PROGRESS')}
          style={{
            background: 'var(--surface)',
            border: statusTab === 'IN_PROGRESS' ? '2px solid var(--primary)' : '1px solid var(--border)',
            borderRadius: 12,
            padding: '14px 16px',
            cursor: 'pointer',
            boxShadow: 'var(--shadow-xs)',
            transition: 'all 0.2s',
          }}
        >
          <div style={{ fontSize: '0.78rem', color: '#D97706', fontWeight: 600 }}>ĐANG THỰC HIỆN</div>
          <div style={{ fontSize: '1.45rem', fontWeight: 800, color: '#D97706', marginTop: 4 }}>
            {inProgressCount}
          </div>
        </div>

        <div
          onClick={() => setStatusTab('COMPLETED')}
          style={{
            background: 'var(--surface)',
            border: statusTab === 'COMPLETED' ? '2px solid var(--primary)' : '1px solid var(--border)',
            borderRadius: 12,
            padding: '14px 16px',
            cursor: 'pointer',
            boxShadow: 'var(--shadow-xs)',
            transition: 'all 0.2s',
          }}
        >
          <div style={{ fontSize: '0.78rem', color: '#10B981', fontWeight: 600 }}>ĐÃ HOÀN THÀNH</div>
          <div style={{ fontSize: '1.45rem', fontWeight: 800, color: '#10B981', marginTop: 4 }}>
            {completedCount}
          </div>
        </div>
      </div>

      {/* ── Filters & Search Controls Bar ── */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          flexWrap: 'wrap',
          gap: 12,
          background: 'var(--surface)',
          padding: '12px 16px',
          borderRadius: 12,
          border: '1px solid var(--border)',
          marginBottom: 20,
        }}
      >
        {/* Status Tab buttons */}
        <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
          {[
            { id: 'ALL', label: 'Tất cả' },
            { id: 'TODO', label: 'Cần làm' },
            { id: 'IN_PROGRESS', label: 'Đang làm' },
            { id: 'COMPLETED', label: 'Đã hoàn thành' },
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setStatusTab(tab.id as any)}
              style={{
                padding: '6px 14px',
                borderRadius: 8,
                fontSize: '0.8rem',
                fontWeight: 600,
                color: statusTab === tab.id ? 'var(--primary)' : 'var(--text-3)',
                background: statusTab === tab.id ? 'var(--primary-bg)' : 'transparent',
                border: statusTab === tab.id ? '1px solid rgba(108, 92, 231, 0.25)' : '1px solid transparent',
                cursor: 'pointer',
                transition: 'all 0.2s',
              }}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {/* Priority select & Search input */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap', flex: 1, justifyContent: 'flex-end' }}>
          <select
            value={selectedPriority}
            onChange={(e) => setSelectedPriority(e.target.value)}
            style={{
              padding: '7px 10px',
              borderRadius: 8,
              border: '1px solid var(--border)',
              background: 'var(--surface)',
              color: 'var(--text)',
              fontSize: '0.8rem',
              outline: 'none',
              cursor: 'pointer',
            }}
          >
            <option value="ALL">Tất cả mức ưu tiên</option>
            <option value="URGENT">Khẩn cấp</option>
            <option value="HIGH">Cao</option>
            <option value="MEDIUM">Trung bình</option>
            <option value="LOW">Thấp</option>
          </select>

          <div style={{ position: 'relative', minWidth: 220, flex: 1, maxWidth: 300 }}>
            <Search size={15} style={{ position: 'absolute', left: 10, top: 10, color: 'var(--text-muted)' }} />
            <input
              type="text"
              placeholder="Tìm theo tên công việc..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              style={{
                width: '100%',
                padding: '8px 12px 8px 32px',
                borderRadius: 8,
                border: '1px solid var(--border)',
                background: 'var(--surface)',
                color: 'var(--text)',
                fontSize: '0.82rem',
                outline: 'none',
              }}
            />
          </div>
        </div>
      </div>

      {/* ── Category Pills Filter ── */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap', marginBottom: 16 }}>
        <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)', fontWeight: 600 }}>Phân loại:</span>
        <button
          onClick={() => setSelectedCat(null)}
          style={{
            padding: '4px 12px',
            borderRadius: 20,
            fontSize: '0.78rem',
            fontWeight: 600,
            background: selectedCat === null ? 'var(--primary)' : 'var(--border-light)',
            color: selectedCat === null ? '#fff' : 'var(--text-2)',
            cursor: 'pointer',
            transition: 'all 0.2s',
          }}
        >
          Tất cả
        </button>
        {categories.map((c) => {
          const isSelected = selectedCat === c.type;
          return (
            <button
              key={c.id}
              onClick={() => setSelectedCat(isSelected ? null : c.type)}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 6,
                padding: '4px 12px',
                borderRadius: 20,
                fontSize: '0.78rem',
                fontWeight: 600,
                background: isSelected ? c.colorCode : 'var(--border-light)',
                color: isSelected ? '#fff' : 'var(--text-2)',
                cursor: 'pointer',
                transition: 'all 0.2s',
              }}
            >
              <span style={{ width: 8, height: 8, borderRadius: '50%', background: isSelected ? '#fff' : c.colorCode }} />
              <span>{c.name}</span>
            </button>
          );
        })}
      </div>

      {/* ── Tasks List ── */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        {filteredTasks.length === 0 ? (
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
          filteredTasks.map((task) => {
            const cat = getCategoryMeta(task.categoryType);
            const prio = getPriorityLabel(task.priority);
            const isCompleted = task.status === 'COMPLETED';

            return (
              <div
                key={task.id}
                className="task-row-card"
                style={{
                  borderLeft: `4px solid ${cat.colorCode}`,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  gap: 16,
                }}
              >
                {/* Left: Checkbox + Title + Meta info */}
                <div style={{ display: 'flex', alignItems: 'center', gap: 14, flex: 1, minWidth: 0 }}>
                  <button
                    onClick={() => onToggleStatus(task)}
                    title={isCompleted ? 'Đánh dấu chưa hoàn thành' : 'Đánh dấu hoàn thành'}
                    style={{
                      background: 'none',
                      border: 'none',
                      cursor: 'pointer',
                      color: isCompleted ? '#10B981' : 'var(--text-muted)',
                      padding: 2,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      flexShrink: 0,
                    }}
                  >
                    {isCompleted ? <CheckCircle2 size={22} /> : <Circle size={22} />}
                  </button>

                  <div style={{ minWidth: 0, flex: 1 }}>
                    <div
                      style={{
                        fontSize: '0.95rem',
                        fontWeight: 600,
                        color: isCompleted ? 'var(--text-muted)' : 'var(--text)',
                        textDecoration: isCompleted ? 'line-through' : 'none',
                        whiteSpace: 'nowrap',
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                      }}
                    >
                      {task.title}
                    </div>

                    {task.note && (
                      <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)', fontStyle: 'italic', marginTop: 2, display: 'flex', alignItems: 'center', gap: 4 }}>
                        <span>💬</span>
                        <span>{task.note}</span>
                      </div>
                    )}

                    <div
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: 12,
                        marginTop: 4,
                        fontSize: '0.75rem',
                        color: 'var(--text-3)',
                        flexWrap: 'wrap',
                      }}
                    >
                      <span style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                        <Clock size={13} style={{ color: 'var(--text-muted)' }} />
                        <span>{formatDateTimeRange(task.startTime, task.endTime)}</span>
                      </span>

                      <span
                        style={{
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: 4,
                          padding: '2px 8px',
                          borderRadius: 6,
                          background: `${cat.colorCode}18`,
                          color: cat.colorCode,
                          fontWeight: 600,
                          fontSize: '0.72rem',
                        }}
                      >
                        <Tag size={11} />
                        <span>{cat.name}</span>
                      </span>

                      <span
                        style={{
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: 4,
                          padding: '2px 8px',
                          borderRadius: 6,
                          background: prio.bg,
                          color: prio.color,
                          fontWeight: 600,
                          fontSize: '0.72rem',
                        }}
                      >
                        <AlertCircle size={11} />
                        <span>{prio.label}</span>
                      </span>
                    </div>
                  </div>
                </div>

                {/* Right: Actions */}
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexShrink: 0 }}>
                  <button
                    onClick={() => onEditTask(task)}
                    title="Chỉnh sửa công việc"
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
            );
          })
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
