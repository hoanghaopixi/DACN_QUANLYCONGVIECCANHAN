import { useState, useEffect } from 'react';
import {
  X, Trash2, AlertTriangle, Sparkles,
  CheckSquare, Loader2, Flame, CheckCircle2, Circle, EyeOff
} from 'lucide-react';
import { Category, Priority, TaskStatus, EisenhowerQuadrant, RecurrenceRule, Subtask } from '../types';
import { checkWorkHoursConstraint, WorkHoursValidationResult } from '../utils/workHoursConstraint';
import { subtaskService } from '../services/subtaskService';
import EyeLockIcon from './EyeLockIcon';
import Time24Input from './Time24Input';
import DateInput from './DateInput';
import HoverSelect from './HoverSelect';
import './TaskModal.css';

interface TaskModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: (taskData: any) => Promise<any> | void;
  onDelete?: (id: number) => void;
  categories: Category[];
  initialData?: any;
}

export default function TaskModal({
  isOpen,
  onClose,
  onSave,
  onDelete,
  categories,
  initialData,
}: TaskModalProps) {
  const [isConfirmingDelete, setIsConfirmingDelete] = useState(false);
  const [offHoursWarning, setOffHoursWarning] = useState<WorkHoursValidationResult | null>(null);
  const [title, setTitle] = useState('');
  const [categoryType, setCategoryType] = useState('WORK');
  const getTodayStr = () => {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  };

  const [date, setDate] = useState(getTodayStr());
  const [startTime, setStartTime] = useState('09:00');
  const [endTime, setEndTime] = useState('10:00');
  const hasInvalidTimeRange = startTime !== '' && endTime !== '' && endTime <= startTime;
  const [priority, setPriority] = useState<Priority>('MEDIUM');
  const [eisenhowerQuadrant, setEisenhowerQuadrant] = useState<EisenhowerQuadrant>('SCHEDULE');
  const [status, setStatus] = useState<TaskStatus>('TODO');
  const [isRecurring, setIsRecurring] = useState(false);
  const [recurrenceRule, setRecurrenceRule] = useState<RecurrenceRule>('DAILY');
  const [note, setNote] = useState('');
  const [isLocked, setIsLocked] = useState(false);

  // Subtasks state
  const [subtasks, setSubtasks] = useState<Subtask[]>([]);
  const [newSubtaskTitle, setNewSubtaskTitle] = useState('');
  const [isGeneratingSubtasks, setIsGeneratingSubtasks] = useState(false);

  // Load subtasks when opening an existing task
  useEffect(() => {
    const taskId = initialData?.id || initialData?.taskId;
    if (taskId) {
      subtaskService.getSubtasks(taskId)
        .then((data) => setSubtasks(data || []))
        .catch(() => setSubtasks([]));
    } else {
      setSubtasks([]);
    }
  }, [initialData?.id, initialData?.taskId, isOpen]);

  useEffect(() => {
    if (initialData) {
      setTitle(initialData.title || '');
      setNote(initialData.note || initialData.description || '');
      setCategoryType(initialData.categoryType || 'WORK');
      if (initialData.startTime) {
        const d = new Date(initialData.startTime);
        const y = d.getFullYear();
        const m = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        setDate(`${y}-${m}-${day}`);
        setStartTime(
          `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
        );
      }
      if (initialData.endTime) {
        const d = new Date(initialData.endTime);
        setEndTime(
          `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
        );
      }
      if (initialData.priority) setPriority(initialData.priority);
      if (initialData.eisenhowerQuadrant) {
        setEisenhowerQuadrant(initialData.eisenhowerQuadrant);
      } else if (initialData.priority) {
        setEisenhowerQuadrant(
          initialData.priority === 'URGENT' ? 'DO_FIRST' :
          initialData.priority === 'HIGH' ? 'SCHEDULE' :
          initialData.priority === 'LOW' ? 'DELEGATE' : 'SCHEDULE'
        );
      }
      if (initialData.status) setStatus(initialData.status);
      if (initialData.isRecurring !== undefined) setIsRecurring(initialData.isRecurring);
      if (initialData.recurrenceRule) setRecurrenceRule(initialData.recurrenceRule);
      setIsLocked(Boolean(initialData.isLocked));
    } else {
      setTitle('');
      setNote('');
      setCategoryType('WORK');
      setDate(getTodayStr());
      setStartTime('09:00');
      setEndTime('10:00');
      setPriority('MEDIUM');
      setEisenhowerQuadrant('SCHEDULE');
      setStatus('TODO');
      setIsRecurring(false);
      setRecurrenceRule('DAILY');
      setIsLocked(false);
    }
    setIsConfirmingDelete(false);
    setOffHoursWarning(null);
  }, [initialData, isOpen]);

  if (!isOpen) return null;

  const doSaveTask = async () => {
    const fullStartTime = `${date}T${startTime}:00+07:00`;
    const fullEndTime = `${date}T${endTime}:00+07:00`;

    const selectedCat = categories.find((c) => c.type === categoryType) || (categories.length > 0 ? categories[0] : null);

    const taskPayload = {
      ...(initialData?.id ? { id: initialData.id } : initialData?.taskId ? { id: initialData.taskId } : {}),
      title: title.trim(),
      note: note.trim(),
      description: note.trim(),
      categoryId: selectedCat ? selectedCat.id : null,
      categoryType: selectedCat?.type || categoryType || 'WORK',
      startTime: fullStartTime,
      endTime: fullEndTime,
      priority,
      eisenhowerQuadrant,
      status,
      isRecurring,
      recurrenceRule: isRecurring ? recurrenceRule : undefined,
      isLocked,
    };

    try {
      const savedResult = await onSave(taskPayload);
      const targetTaskId = initialData?.id || initialData?.taskId || savedResult?.id || (savedResult as any)?.data?.id;

      // If user added draft subtasks before saving the task, persist them now
      if (targetTaskId) {
        const draftSubtasks = subtasks.filter((s) => s.id < 0);
        for (const st of draftSubtasks) {
          try {
            await subtaskService.createSubtask(targetTaskId, {
              title: st.title,
              durationMinutes: st.durationMinutes || 15,
            });
          } catch (subErr) {
            console.error('Lỗi khi lưu subtask nháp:', subErr);
          }
        }
      }

      setOffHoursWarning(null);
      onClose();
    } catch (err) {
      console.error('Lỗi khi lưu công việc:', err);
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim()) return;
    if (hasInvalidTimeRange) return;

    const fullStartTime = `${date}T${startTime}:00+07:00`;
    const fullEndTime = `${date}T${endTime}:00+07:00`;

    const validation = checkWorkHoursConstraint(fullStartTime, fullEndTime, categoryType);
    if (validation.isOutsideWorkHours) {
      setOffHoursWarning(validation);
      return;
    }

    doSaveTask();
  };

  // Subtask actions
  const handleToggleSubtask = async (st: Subtask) => {
    if (st.id < 0) {
      setSubtasks((prev) =>
        prev.map((s) => (s.id === st.id ? { ...s, isCompleted: !s.isCompleted } : s))
      );
      return;
    }
    try {
      const updated = await subtaskService.toggleSubtask(st.id);
      setSubtasks((prev) => prev.map((s) => (s.id === st.id ? updated : s)));
    } catch (err) {
      console.error('Lỗi khi đổi trạng thái subtask:', err);
    }
  };

  const handleAddSubtask = async () => {
    if (!newSubtaskTitle.trim()) return;
    const taskId = initialData?.id || initialData?.taskId;
    if (!taskId) {
      // Add local draft subtask to be saved together with task
      const draftSubtask: Subtask = {
        id: -Date.now() - Math.floor(Math.random() * 1000),
        taskId: 0,
        title: newSubtaskTitle.trim(),
        isCompleted: false,
        durationMinutes: 15,
        orderIndex: subtasks.length,
        createdAt: new Date().toISOString(),
      };
      setSubtasks((prev) => [...prev, draftSubtask]);
      setNewSubtaskTitle('');
      return;
    }
    try {
      const created = await subtaskService.createSubtask(taskId, {
        title: newSubtaskTitle.trim(),
        durationMinutes: 15,
      });
      setSubtasks((prev) => [...prev, created]);
      setNewSubtaskTitle('');
    } catch (err) {
      console.error('Lỗi khi thêm subtask:', err);
    }
  };

  const handleDeleteSubtask = async (subtaskId: number) => {
    if (subtaskId < 0) {
      setSubtasks((prev) => prev.filter((s) => s.id !== subtaskId));
      return;
    }
    try {
      await subtaskService.deleteSubtask(subtaskId);
      setSubtasks((prev) => prev.filter((s) => s.id !== subtaskId));
    } catch (err) {
      console.error('Lỗi khi xóa subtask:', err);
    }
  };

  const handleAiBreakdown = async () => {
    const taskId = initialData?.id || initialData?.taskId;
    if (!title.trim()) {
      alert('Vui lòng nhập tiêu đề công việc trước để Gemini AI có thể phân tích và phân rã các bước thực hiện!');
      return;
    }

    setIsGeneratingSubtasks(true);
    try {
      const generated = await subtaskService.breakdownWithAi(taskId, {
        title: title.trim(),
        description: note.trim(),
        durationMinutes: 60,
      });
      if (generated && generated.length > 0) {
        setSubtasks((prev) => {
          if (taskId) return generated;
          return [...prev, ...generated];
        });
      }
    } catch (err) {
      console.error('Lỗi khi Gemini AI phân rã công việc:', err);
    } finally {
      setIsGeneratingSubtasks(false);
    }
  };

  const completedSubtasksCount = subtasks.filter((s) => s.isCompleted).length;
  const subtasksProgress = subtasks.length > 0 ? Math.round((completedSubtasksCount / subtasks.length) * 100) : 0;

  return (
    <div className="task-modal-overlay">
      <div className="task-modal-shell">
        {/* Modal Header */}
        <div className="task-modal-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <h2 style={{ fontSize: '1.02rem', fontWeight: 700, color: 'var(--text)', margin: 0 }}>
              {initialData?.id ? 'Chỉnh sửa công việc' : 'Thêm công việc mới'}
            </h2>
          </div>
          <button
            type="button"
            onClick={onClose}
            style={{
              padding: 5,
              borderRadius: 6,
              border: 'none',
              background: 'transparent',
              color: 'var(--text-muted)',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <X size={18} />
          </button>
        </div>

        {/* Modal Body */}
        <div className="task-modal-body">
          {isConfirmingDelete ? (
            <div style={{ padding: '24px 20px', textAlign: 'center' }}>
              <div style={{ width: 44, height: 44, borderRadius: '50%', background: '#FEE2E2', color: '#DC2626', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 12px' }}>
                <Trash2 size={22} />
              </div>
              <h3 style={{ fontSize: '1rem', fontWeight: 700, color: 'var(--text)', marginBottom: 6 }}>
                Xác nhận xóa công việc?
              </h3>
              <p style={{ fontSize: '0.84rem', color: 'var(--text-3)', marginBottom: 20 }}>
                Bạn có chắc chắn muốn xóa công việc "{title}" khỏi cơ sở dữ liệu không?
              </p>
              <div style={{ display: 'flex', justifyContent: 'center', gap: 12 }}>
                <button
                  type="button"
                  onClick={() => setIsConfirmingDelete(false)}
                  style={{ padding: '8px 16px', borderRadius: 8, border: '1px solid var(--border)', background: 'var(--surface)', color: 'var(--text)', fontSize: '0.84rem', fontWeight: 600, cursor: 'pointer' }}
                >
                  Hủy
                </button>
                <button
                  type="button"
                  onClick={() => {
                    if (initialData?.id && onDelete) onDelete(initialData.id);
                    onClose();
                  }}
                  style={{ padding: '8px 18px', borderRadius: 8, border: 'none', background: '#DC2626', color: '#fff', fontSize: '0.84rem', fontWeight: 600, cursor: 'pointer' }}
                >
                  Xác nhận xóa
                </button>
              </div>
            </div>
          ) : offHoursWarning ? (
            <div style={{ padding: '24px 20px', textAlign: 'center' }}>
              <div style={{ width: 44, height: 44, borderRadius: '50%', background: '#FEF3C7', color: '#D97706', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 12px' }}>
                <AlertTriangle size={22} />
              </div>
              <h3 style={{ fontSize: '1rem', fontWeight: 700, color: 'var(--text)', marginBottom: 6 }}>
                Cảnh báo khung giờ làm việc
              </h3>
              <p style={{ fontSize: '0.84rem', color: 'var(--text-3)', marginBottom: 20 }}>
                {offHoursWarning.reason} Bạn có muốn tiếp tục lưu công việc vào khung giờ này không?
              </p>
              <div style={{ display: 'flex', justifyContent: 'center', gap: 12 }}>
                <button
                  type="button"
                  onClick={() => setOffHoursWarning(null)}
                  style={{ padding: '8px 16px', borderRadius: 8, border: '1px solid var(--border)', background: 'var(--surface)', color: 'var(--text)', fontSize: '0.84rem', fontWeight: 600, cursor: 'pointer' }}
                >
                  Chỉnh lại giờ
                </button>
                <button
                  type="button"
                  onClick={doSaveTask}
                  style={{ padding: '8px 18px', borderRadius: 8, border: 'none', background: '#EA580C', color: '#17252A', fontSize: '0.84rem', fontWeight: 600, cursor: 'pointer' }}
                >
                  Đồng ý tạo ngoài giờ
                </button>
              </div>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="task-modal-form">
              {/* Title */}
              <div>
                <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 700, color: 'var(--text-muted)', marginBottom: 4, letterSpacing: '0.3px' }}>
                  TÊN CÔNG VIỆC / SỰ KIỆN *
                </label>
                <input
                  type="text"
                  required
                  placeholder="VD: Làm báo cáo đồ án, Thiết kế giao diện..."
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    border: '1px solid var(--border)',
                    borderRadius: 8,
                    fontSize: '0.88rem',
                    fontWeight: 600,
                    outline: 'none',
                    background: 'var(--surface)',
                    color: 'var(--text)',
                  }}
                />
              </div>

              {/* Category - 4 columns in 1 single row */}
              <div>
                <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 700, color: 'var(--text-muted)', marginBottom: 4, letterSpacing: '0.3px' }}>
                  NHÓM CÔNG VIỆC
                </label>
              <div className="task-modal-categories">
                  {categories.map((cat) => (
                    <button
                      key={cat.id}
                      type="button"
                      onClick={() => setCategoryType(cat.type)}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        gap: 6,
                        padding: '6px 6px',
                        borderRadius: 8,
                        border: '1px solid',
                        borderColor: categoryType === cat.type ? cat.colorCode : 'var(--border)',
                        background: categoryType === cat.type ? `${cat.colorCode}18` : 'var(--surface)',
                        color: categoryType === cat.type ? cat.colorCode : 'var(--text)',
                        fontSize: '0.8rem',
                        fontWeight: categoryType === cat.type ? 700 : 500,
                        cursor: 'pointer',
                        whiteSpace: 'nowrap',
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        transition: 'all 0.15s ease',
                      }}
                    >
                      <span style={{ width: 8, height: 8, borderRadius: '50%', background: cat.colorCode, flexShrink: 0 }} />
                      <span style={{ overflow: 'hidden', textOverflow: 'ellipsis' }}>{cat.name}</span>
                    </button>
                  ))}
                </div>
              </div>

              {/* Date & Time */}
              <div className="task-modal-date-row">
                <div>
                  <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 700, color: 'var(--text-muted)', marginBottom: 4, letterSpacing: '0.3px' }}>
                    NGÀY
                  </label>
                  <DateInput value={date} onChange={setDate} id="task-date" required />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 700, color: 'var(--text-muted)', marginBottom: 4, letterSpacing: '0.3px' }}>
                    BẮT ĐẦU
                  </label>
                  <Time24Input
                    value={startTime}
                    onChange={(val) => setStartTime(val)}
                  />
                  {hasInvalidTimeRange && (
                    <p role="alert" style={{ margin: '4px 0 0', color: 'var(--danger, #dc2626)', fontSize: '0.7rem', fontWeight: 600, lineHeight: 1.25 }}>
                      Giờ bắt đầu phải trước giờ kết thúc.
                    </p>
                  )}
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 700, color: 'var(--text-muted)', marginBottom: 4, letterSpacing: '0.3px' }}>
                    KẾT THÚC
                  </label>
                  <Time24Input
                    value={endTime}
                    align="right"
                    onChange={(val) => setEndTime(val)}
                  />
                  {hasInvalidTimeRange && (
                    <p role="alert" style={{ margin: '4px 0 0', color: 'var(--danger, #dc2626)', fontSize: '0.7rem', fontWeight: 600, lineHeight: 1.25 }}>
                      Giờ kết thúc phải sau giờ bắt đầu.
                    </p>
                  )}
                </div>
              </div>

              {/* Priority, Eisenhower Quadrant & Status */}
              <div className="task-modal-select-row">
                <div>
                  <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 700, color: 'var(--text-muted)', marginBottom: 4, letterSpacing: '0.3px' }}>
                    MA TRẬN EISENHOWER
                  </label>
                  <HoverSelect
                    value={eisenhowerQuadrant}
                    onChange={(e) => setEisenhowerQuadrant(e.target.value as EisenhowerQuadrant)}
                    style={{
                      width: '100%',
                      padding: '6px 8px',
                      border: '1px solid var(--border)',
                      borderRadius: 8,
                      fontSize: '0.82rem',
                      background: 'var(--surface)',
                      color: 'var(--text)',
                      fontWeight: 600,
                    }}
                  >
                    <option value="DO_FIRST">🔴 Q1: Khẩn cấp & Quan trọng (Làm ngay)</option>
                    <option value="SCHEDULE">🔵 Q2: Quan trọng, không gấp (Lên lịch)</option>
                    <option value="DELEGATE">🟡 Q3: Gấp, không quan trọng (Ủy quyền)</option>
                    <option value="DONT_DO">⚪ Q4: Không gấp, không quan trọng (Loại bỏ)</option>
                  </HoverSelect>
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 700, color: 'var(--text-muted)', marginBottom: 4, letterSpacing: '0.3px' }}>
                    TRẠNG THÁI
                  </label>
                  <HoverSelect
                    value={status}
                    onChange={(e) => setStatus(e.target.value as TaskStatus)}
                    style={{
                      width: '100%',
                      padding: '6px 8px',
                      border: '1px solid var(--border)',
                      borderRadius: 8,
                      fontSize: '0.82rem',
                      background: 'var(--surface)',
                      color: 'var(--text)',
                      fontWeight: 600,
                    }}
                  >
                    <option value="TODO">Cần làm</option>
                    <option value="IN_PROGRESS">Đang làm</option>
                    <option value="OVERDUE">Đã qua</option>
                    <option value="COMPLETED">Đã đánh dấu hoàn thành</option>
                  </HoverSelect>
                </div>
              </div>

              {/* 2 Cột Tính năng: Công việc lặp lại & Khóa riêng tư đặt cạnh nhau siêu gọn */}
              <div className="task-modal-feature-row">
                {/* Feature: Recurring Habits */}
                <div className={`task-modal-option-card ${isRecurring ? 'is-selected' : ''}`}>
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <label style={{ display: 'flex', alignItems: 'center', gap: 6, cursor: 'pointer', fontSize: '0.8rem', fontWeight: 700, color: 'var(--text)' }}>
                      <input
                        type="checkbox"
                        checked={isRecurring}
                        onChange={(e) => setIsRecurring(e.target.checked)}
                        style={{ width: 15, height: 15, accentColor: 'var(--primary)' }}
                      />
                      <span>Thói quen lặp lại</span>
                    </label>
                    {isRecurring && (
                      <span style={{ fontSize: '0.7rem', color: '#EA580C', fontWeight: 700, display: 'flex', alignItems: 'center', gap: 2 }}>
                        <Flame size={12} fill="#EA580C" /> Streak
                      </span>
                    )}
                  </div>

                  {isRecurring && (
                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 4, marginTop: 6 }}>
                      {[
                        { id: 'DAILY', label: 'Hằng ngày' },
                        { id: 'WEEKDAYS', label: 'Thứ 2 – T6' },
                        { id: 'WEEKLY', label: 'Hằng tuần' },
                        { id: 'MONTHLY', label: 'Hằng tháng' },
                      ].map((r) => (
                        <button
                          key={r.id}
                          type="button"
                          onClick={() => setRecurrenceRule(r.id as RecurrenceRule)}
                          style={{
                            padding: '4px 6px',
                            borderRadius: 6,
                            fontSize: '0.72rem',
                            fontWeight: recurrenceRule === r.id ? 700 : 500,
                            border: recurrenceRule === r.id ? '1px solid var(--primary)' : '1px solid var(--border)',
                            background: recurrenceRule === r.id ? 'var(--primary-bg)' : 'var(--surface)',
                            color: recurrenceRule === r.id ? 'var(--primary)' : 'var(--text)',
                            cursor: 'pointer',
                          }}
                        >
                          {r.label}
                        </button>
                      ))}
                    </div>
                  )}
                </div>

                {/* Feature: Privacy Lock */}
                <div className={`task-modal-option-card ${isLocked ? 'is-locked' : ''}`}>
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <label style={{ display: 'flex', alignItems: 'center', gap: 6, cursor: 'pointer', fontSize: '0.8rem', fontWeight: 700, color: 'var(--text)' }}>
                      <input
                        type="checkbox"
                        checked={isLocked}
                        onChange={(e) => setIsLocked(e.target.checked)}
                        style={{ width: 15, height: 15, accentColor: '#EF4444' }}
                      />
                      <span style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                        <EyeLockIcon locked={isLocked} size={13} color={isLocked ? '#EF4444' : 'var(--text-muted)'} />
                        <span>Khóa riêng tư</span>
                      </span>
                    </label>
                    {isLocked && (
                      <span style={{ fontSize: '0.7rem', color: '#EF4444', fontWeight: 700, display: 'flex', alignItems: 'center', gap: 2 }}>
                        <EyeOff size={11} /> Cần mã
                      </span>
                    )}
                  </div>
                  <div style={{ margin: '4px 0 0 21px', fontSize: '0.7rem', color: isLocked ? '#EF4444' : 'var(--text-muted)', lineHeight: 1.25 }}>
                    {isLocked ? 'Khóa bảo mật: Cần mật khẩu để mở' : 'Hiện card xám bảo mật trên lịch'}
                  </div>
                </div>
              </div>

              {/* Feature 3: Checklist / Subtasks & AI Task Breakdown */}
              <div className="task-modal-checklist">
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: subtasks.length > 0 ? 6 : 4 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                    <CheckSquare size={14} color="var(--primary)" />
                    <span style={{ fontSize: '0.78rem', fontWeight: 800, color: 'var(--text)' }}>
                      CHECKLIST VIỆC CON {subtasks.length > 0 ? `(${completedSubtasksCount}/${subtasks.length})` : ''}
                    </span>
                  </div>

                  {/* AI Breakdown Button */}
                  <button
                    type="button"
                    onClick={handleAiBreakdown}
                    disabled={isGeneratingSubtasks}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: 4,
                      padding: '4px 9px',
                      borderRadius: 6,
                      background: 'var(--primary)',
                      color: 'var(--on-primary)',
                      fontSize: '0.72rem',
                      fontWeight: 700,
                      border: 'none',
                      cursor: isGeneratingSubtasks ? 'not-allowed' : 'pointer',
                      boxShadow: 'none',
                    }}
                  >
                    {isGeneratingSubtasks ? (
                      <>
                        <Loader2 size={12} className="spin-animate" />
                        <span>Đang phân rã...</span>
                      </>
                    ) : (
                      <>
                        <Sparkles size={12} />
                        <span>AI Phân rã (Gemini)</span>
                      </>
                    )}
                  </button>
                </div>

                {/* Progress bar if subtasks exist */}
                {subtasks.length > 0 && (
                  <div style={{ marginBottom: 6 }}>
                    <div style={{ height: 4, borderRadius: 8, background: 'var(--border-light)', overflow: 'hidden' }}>
                      <div style={{ height: '100%', width: `${subtasksProgress}%`, background: '#10B981', transition: 'width 0.3s ease' }} />
                    </div>
                  </div>
                )}

                {/* Subtasks list */}
                {subtasks.length > 0 && (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 4, maxHeight: 110, overflowY: 'auto', marginBottom: 6 }}>
                    {subtasks.map((st) => (
                      <div
                        key={st.id}
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'space-between',
                          padding: '4px 8px',
                          borderRadius: 6,
                          background: 'var(--border-light)',
                          fontSize: '0.78rem',
                        }}
                      >
                        <div
                          onClick={() => handleToggleSubtask(st)}
                          style={{ display: 'flex', alignItems: 'center', gap: 6, flex: 1, cursor: 'pointer' }}
                        >
                          {st.isCompleted ? (
                            <CheckCircle2 size={14} fill="#10B981" color="#fff" />
                          ) : (
                            <Circle size={14} color="var(--text-muted)" />
                          )}
                          <span
                            style={{
                              color: st.isCompleted ? 'var(--text-muted)' : 'var(--text)',
                              textDecoration: st.isCompleted ? 'line-through' : 'none',
                            }}
                          >
                            {st.title} {st.durationMinutes ? `(${st.durationMinutes}p)` : ''}
                          </span>
                        </div>

                        <button
                          type="button"
                          onClick={() => handleDeleteSubtask(st.id)}
                          style={{ color: 'var(--text-muted)', cursor: 'pointer', padding: 2, border: 'none', background: 'transparent' }}
                        >
                          <X size={13} />
                        </button>
                      </div>
                    ))}
                  </div>
                )}

                {/* Add new subtask input */}
                <div style={{ display: 'flex', gap: 6 }}>
                  <input
                    type="text"
                    placeholder="Thêm việc con mới..."
                    value={newSubtaskTitle}
                    onChange={(e) => setNewSubtaskTitle(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') {
                        e.preventDefault();
                        handleAddSubtask();
                      }
                    }}
                    style={{
                      flex: 1,
                      padding: '5px 8px',
                      borderRadius: 6,
                      border: '1px solid var(--border)',
                      fontSize: '0.78rem',
                      outline: 'none',
                      background: 'var(--surface)',
                      color: 'var(--text)',
                    }}
                  />
                  <button
                    type="button"
                    onClick={handleAddSubtask}
                    style={{
                      padding: '5px 10px',
                      borderRadius: 6,
                      background: 'var(--primary-bg)',
                      color: 'var(--primary)',
                      fontSize: '0.76rem',
                      fontWeight: 700,
                      border: 'none',
                      cursor: 'pointer',
                    }}
                  >
                    + Thêm
                  </button>
                </div>
              </div>

              {/* Note */}
              <div className="task-modal-note">
                <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 700, color: 'var(--text-muted)', marginBottom: 3, letterSpacing: '0.3px' }}>
                  GHI CHÚ / CHÚ THÍCH
                </label>
                <textarea
                  rows={2}
                  placeholder="Thêm ghi chú, địa điểm, chi tiết công việc..."
                  value={note}
                  onChange={(e) => setNote(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '6px 10px',
                    border: '1px solid var(--border)',
                    borderRadius: 8,
                    fontSize: '0.82rem',
                    outline: 'none',
                    resize: 'none',
                    fontFamily: 'inherit',
                    background: 'var(--surface)',
                    color: 'var(--text)',
                  }}
                />
              </div>

              {/* Actions */}
              <div className="task-modal-actions">
                {initialData?.id && onDelete ? (
                  <button
                    type="button"
                    onClick={() => setIsConfirmingDelete(true)}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: 5,
                      padding: '8px 12px',
                      borderRadius: 8,
                      border: '1px solid #FECACA',
                      background: '#FEF2F2',
                      fontSize: '0.82rem',
                      fontWeight: 600,
                      color: '#DC2626',
                      cursor: 'pointer',
                    }}
                  >
                    <Trash2 size={14} />
                    <span>Xóa việc</span>
                  </button>
                ) : <div />}

                <div style={{ display: 'flex', gap: 8 }}>
                  <button
                    type="button"
                    onClick={onClose}
                    style={{
                      padding: '8px 14px',
                      borderRadius: 8,
                      border: '1px solid var(--border)',
                      fontSize: '0.82rem',
                      fontWeight: 500,
                      color: 'var(--text-3)',
                      background: 'transparent',
                      cursor: 'pointer',
                    }}
                  >
                    Hủy bỏ
                  </button>
                  <button
                    type="submit"
                    disabled={hasInvalidTimeRange}
                    style={{
                      padding: '8px 20px',
                      borderRadius: 8,
                      background: 'var(--primary)',
                      color: '#FFFFFF',
                      fontSize: '0.84rem',
                      fontWeight: 700,
                      boxShadow: '0 3px 10px rgba(108,92,231,0.28)',
                      border: 'none',
                      cursor: hasInvalidTimeRange ? 'not-allowed' : 'pointer',
                      opacity: hasInvalidTimeRange ? 0.55 : 1,
                    }}
                  >
                    {initialData?.id ? 'Cập nhật' : 'Thêm công việc'}
                  </button>
                </div>
              </div>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}
