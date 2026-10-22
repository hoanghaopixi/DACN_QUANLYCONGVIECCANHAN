import { useState, useEffect } from 'react';
import { X, Trash2, AlertTriangle, Clock } from 'lucide-react';
import { Category, Priority, TaskStatus } from '../types';
import { checkWorkHoursConstraint, WorkHoursValidationResult } from '../utils/workHoursConstraint';

interface TaskModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: (taskData: any) => void;
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
  const [priority, setPriority] = useState<Priority>('MEDIUM');
  const [status, setStatus] = useState<TaskStatus>('TODO');
  const [isRecurring, setIsRecurring] = useState(false);
  const [note, setNote] = useState('');

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
      if (initialData.status) setStatus(initialData.status);
      if (initialData.isRecurring !== undefined) setIsRecurring(initialData.isRecurring);
    } else {
      setTitle('');
      setNote('');
      setCategoryType('WORK');
      setDate(getTodayStr());
      setStartTime('09:00');
      setEndTime('10:00');
      setPriority('MEDIUM');
      setStatus('TODO');
      setIsRecurring(false);
    }
    setIsConfirmingDelete(false);
    setOffHoursWarning(null);
  }, [initialData, isOpen]);

  if (!isOpen) return null;

  const doSaveTask = () => {
    const fullStartTime = `${date}T${startTime}:00+07:00`;
    const fullEndTime = `${date}T${endTime}:00+07:00`;

    const selectedCat = categories.find((c) => c.type === categoryType) || (categories.length > 0 ? categories[0] : null);

    onSave({
      ...(initialData?.id ? { id: initialData.id } : initialData?.taskId ? { id: initialData.taskId } : {}),
      title: title.trim(),
      note: note.trim(),
      description: note.trim(),
      categoryId: selectedCat ? selectedCat.id : null,
      categoryType: selectedCat?.type || categoryType || 'WORK',
      startTime: fullStartTime,
      endTime: fullEndTime,
      priority,
      status,
      isRecurring,
    });
    setOffHoursWarning(null);
    onClose();
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim()) return;

    const fullStartTime = `${date}T${startTime}:00+07:00`;
    const fullEndTime = `${date}T${endTime}:00+07:00`;

    // Kiểm tra ràng buộc ngoài giờ nghỉ & quá thời gian kết thúc
    const validation = checkWorkHoursConstraint(fullStartTime, fullEndTime, categoryType);
    if (validation.isOutsideWorkHours) {
      setOffHoursWarning(validation);
      return;
    }

    doSaveTask();
  };

  return (
    <div
      className="modal-backdrop-animated"
      onClick={onClose}
    >
      <div
        className="modal-card-animated"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '16px 20px',
            borderBottom: '1px solid #E2E8F0',
          }}
        >
          <h2 style={{ fontSize: '1.1rem', fontWeight: 700, color: '#0F172A' }}>
            {initialData?.id ? 'Chỉnh sửa công việc' : 'Thêm công việc mới'}
          </h2>
          <button
            onClick={onClose}
            style={{
              width: 32,
              height: 32,
              borderRadius: 8,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#64748B',
            }}
          >
            <X size={18} />
          </button>
        </div>

        {/* Body: Confirm Delete View OR Edit Form */}
        {isConfirmingDelete ? (
          <div style={{ padding: 20 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
              <div
                style={{
                  width: 44,
                  height: 44,
                  borderRadius: 12,
                  background: 'rgba(239, 68, 68, 0.1)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: '#EF4444',
                  flexShrink: 0,
                }}
              >
                <AlertTriangle size={24} />
              </div>
              <div>
                <h4 style={{ margin: 0, fontSize: '1.05rem', fontWeight: 700, color: '#0F172A' }}>
                  Xác nhận xóa công việc?
                </h4>
                <p style={{ margin: '4px 0 0', fontSize: '0.85rem', color: '#64748B' }}>
                  Bạn có chắc muốn xóa "{title || initialData?.title}" khỏi lịch không?
                </p>
              </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 20, paddingTop: 14, borderTop: '1px solid #E2E8F0' }}>
              <button
                type="button"
                onClick={() => setIsConfirmingDelete(false)}
                style={{
                  padding: '9px 16px',
                  borderRadius: 8,
                  border: '1px solid #CBD5E1',
                  background: '#FFFFFF',
                  color: '#64748B',
                  fontSize: '0.85rem',
                  fontWeight: 500,
                  cursor: 'pointer',
                }}
              >
                Hủy bỏ
              </button>
              <button
                type="button"
                onClick={() => {
                  if (initialData?.id && onDelete) {
                    onDelete(initialData.id);
                    onClose();
                  }
                }}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 6,
                  padding: '9px 18px',
                  borderRadius: 8,
                  border: 'none',
                  background: '#EF4444',
                  color: '#fff',
                  fontSize: '0.85rem',
                  fontWeight: 600,
                  cursor: 'pointer',
                }}
              >
                <Trash2 size={14} />
                <span>Xác nhận xóa</span>
              </button>
            </div>
          </div>
        ) : offHoursWarning ? (
          <div style={{ padding: 22 }}>
            <div style={{ display: 'flex', alignItems: 'flex-start', gap: 14, marginBottom: 16 }}>
              <div
                style={{
                  width: 44,
                  height: 44,
                  borderRadius: 12,
                  background: 'rgba(234, 88, 12, 0.12)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: '#EA580C',
                  flexShrink: 0,
                }}
              >
                <AlertTriangle size={24} />
              </div>
              <div>
                <h4 style={{ margin: 0, fontSize: '1.05rem', fontWeight: 700, color: '#0F172A' }}>
                  Xác nhận ngoài giờ nghỉ
                </h4>
                <p style={{ margin: '4px 0 0', fontSize: '0.85rem', color: '#64748B', lineHeight: 1.5 }}>
                  {offHoursWarning.reason}
                </p>
              </div>
            </div>

            <div
              style={{
                padding: '14px 16px',
                background: '#FFF7ED',
                border: '1.5px solid #FDBA74',
                borderRadius: 10,
                color: '#C2410C',
                fontSize: '0.92rem',
                fontWeight: 600,
                marginBottom: 20,
                display: 'flex',
                alignItems: 'center',
                gap: 10,
              }}
            >
              <Clock size={18} style={{ flexShrink: 0 }} />
              <span>{offHoursWarning.promptMessage}</span>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, paddingTop: 14, borderTop: '1px solid #E2E8F0' }}>
              <button
                type="button"
                onClick={() => setOffHoursWarning(null)}
                style={{
                  padding: '9px 16px',
                  borderRadius: 8,
                  border: '1px solid #CBD5E1',
                  background: '#FFFFFF',
                  color: '#64748B',
                  fontSize: '0.85rem',
                  fontWeight: 500,
                  cursor: 'pointer',
                }}
              >
                Quay lại chỉnh sửa
              </button>
              <button
                type="button"
                onClick={doSaveTask}
                style={{
                  padding: '9px 18px',
                  borderRadius: 8,
                  border: 'none',
                  background: '#EA580C',
                  color: '#fff',
                  fontSize: '0.85rem',
                  fontWeight: 600,
                  cursor: 'pointer',
                }}
              >
                Đồng ý tạo ngoài giờ
              </button>
            </div>
          </div>
        ) : (
          <form onSubmit={handleSubmit} style={{ padding: '20px', display: 'flex', flexDirection: 'column', gap: 16 }}>
          {/* Title */}
          <div>
            <label style={{ display: 'block', fontSize: '0.82rem', fontWeight: 600, color: '#475569', marginBottom: 6 }}>
              Tên công việc / Sự kiện *
            </label>
            <input
              type="text"
              required
              placeholder="VD: Làm báo cáo đồ án, Thiết kế giao diện..."
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              style={{
                width: '100%',
                padding: '10px 12px',
                border: '1px solid #CBD5E1',
                borderRadius: 8,
                fontSize: '0.9rem',
                outline: 'none',
              }}
            />
          </div>

          {/* Category */}
          <div>
            <label style={{ display: 'block', fontSize: '0.82rem', fontWeight: 600, color: '#475569', marginBottom: 6 }}>
              Nhóm công việc
            </label>
            {categories.length === 0 ? (
              <div style={{ fontSize: '0.82rem', color: '#64748B', background: '#F8FAFC', padding: '10px 12px', borderRadius: 8, border: '1px dashed #CBD5E1' }}>
                Không gian này chưa tạo nhóm riêng. Công việc sẽ được lưu với nhóm chung.
              </div>
            ) : (
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 8 }}>
                {categories.map((cat) => (
                  <button
                    key={cat.id}
                    type="button"
                    onClick={() => setCategoryType(cat.type)}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: 8,
                      padding: '8px 12px',
                      borderRadius: 8,
                      border: '1px solid',
                      borderColor: categoryType === cat.type ? cat.colorCode : '#E2E8F0',
                      background: categoryType === cat.type ? cat.bgColor : '#FFFFFF',
                      color: categoryType === cat.type ? '#0F172A' : '#64748B',
                      fontSize: '0.85rem',
                      fontWeight: categoryType === cat.type ? 600 : 400,
                    }}
                  >
                    <span style={{ width: 10, height: 10, borderRadius: '50%', background: cat.colorCode }} />
                    <span>{cat.name}</span>
                  </button>
                ))}
              </div>
            )}
          </div>

          {/* Date & Time */}
          <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr 1fr', gap: 10 }}>
            <div>
              <label style={{ display: 'block', fontSize: '0.82rem', fontWeight: 600, color: '#475569', marginBottom: 6 }}>
                Ngày
              </label>
              <input
                type="date"
                value={date}
                onChange={(e) => setDate(e.target.value)}
                style={{
                  width: '100%',
                  padding: '8px 10px',
                  border: '1px solid #CBD5E1',
                  borderRadius: 8,
                  fontSize: '0.85rem',
                }}
              />
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.82rem', fontWeight: 600, color: '#475569', marginBottom: 6 }}>
                Bắt đầu
              </label>
              <input
                type="time"
                value={startTime}
                onChange={(e) => setStartTime(e.target.value)}
                style={{
                  width: '100%',
                  padding: '8px 10px',
                  border: '1px solid #CBD5E1',
                  borderRadius: 8,
                  fontSize: '0.85rem',
                }}
              />
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.82rem', fontWeight: 600, color: '#475569', marginBottom: 6 }}>
                Kết thúc
              </label>
              <input
                type="time"
                value={endTime}
                onChange={(e) => setEndTime(e.target.value)}
                style={{
                  width: '100%',
                  padding: '8px 10px',
                  border: '1px solid #CBD5E1',
                  borderRadius: 8,
                  fontSize: '0.85rem',
                }}
              />
            </div>
          </div>

          {/* Priority & Status */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <div>
              <label style={{ display: 'block', fontSize: '0.82rem', fontWeight: 600, color: '#475569', marginBottom: 6 }}>
                Độ ưu tiên
              </label>
              <select
                value={priority}
                onChange={(e) => setPriority(e.target.value as Priority)}
                style={{
                  width: '100%',
                  padding: '8px 10px',
                  border: '1px solid #CBD5E1',
                  borderRadius: 8,
                  fontSize: '0.85rem',
                }}
              >
                <option value="LOW">Thấp (Low)</option>
                <option value="MEDIUM">Trung bình (Medium)</option>
                <option value="HIGH">Cao (High)</option>
                <option value="URGENT">Khẩn cấp (Urgent)</option>
              </select>
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.82rem', fontWeight: 600, color: '#475569', marginBottom: 6 }}>
                Trạng thái
              </label>
              <select
                value={status}
                onChange={(e) => setStatus(e.target.value as TaskStatus)}
                style={{
                  width: '100%',
                  padding: '8px 10px',
                  border: '1px solid #CBD5E1',
                  borderRadius: 8,
                  fontSize: '0.85rem',
                }}
              >
                <option value="TODO">Cần làm</option>
                <option value="IN_PROGRESS">Đang làm</option>
                <option value="COMPLETED">Đã hoàn thành</option>
              </select>
            </div>
          </div>

          {/* Ghi chú / Chú thích */}
          <div>
            <label style={{ display: 'block', fontSize: '0.82rem', fontWeight: 600, color: '#475569', marginBottom: 6 }}>
              Ghi chú / Chú thích
            </label>
            <textarea
              rows={2}
              placeholder="Thêm ghi chú, địa điểm, chi tiết công việc..."
              value={note}
              onChange={(e) => setNote(e.target.value)}
              style={{
                width: '100%',
                padding: '8px 12px',
                border: '1px solid #CBD5E1',
                borderRadius: 8,
                fontSize: '0.85rem',
                outline: 'none',
                resize: 'vertical',
                fontFamily: 'inherit',
                boxSizing: 'border-box',
              }}
            />
          </div>

          {/* Recurring */}
          <label style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer', fontSize: '0.85rem', color: '#475569' }}>
            <input
              type="checkbox"
              checked={isRecurring}
              onChange={(e) => setIsRecurring(e.target.checked)}
              style={{ width: 16, height: 16 }}
            />
            <span>Lặp lại hàng tuần (Hiển thị biểu tượng ↻)</span>
          </label>

          {/* Action buttons */}
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: 10 }}>
            {initialData?.id && onDelete ? (
              <button
                type="button"
                onClick={() => setIsConfirmingDelete(true)}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 6,
                  padding: '9px 14px',
                  borderRadius: 8,
                  border: '1px solid #FECACA',
                  background: '#FEF2F2',
                  fontSize: '0.85rem',
                  fontWeight: 600,
                  color: '#DC2626',
                  cursor: 'pointer',
                }}
              >
                <Trash2 size={15} />
                <span>Xóa việc</span>
              </button>
            ) : <div />}

            <div style={{ display: 'flex', gap: 10 }}>
              <button
                type="button"
                onClick={onClose}
                style={{
                  padding: '9px 16px',
                  borderRadius: 8,
                  border: '1px solid #CBD5E1',
                  fontSize: '0.85rem',
                  fontWeight: 500,
                  color: '#64748B',
                }}
              >
                Hủy bỏ
              </button>
              <button
                type="submit"
                style={{
                  padding: '9px 20px',
                  borderRadius: 8,
                  background: '#6C5CE7',
                  color: '#FFFFFF',
                  fontSize: '0.85rem',
                  fontWeight: 600,
                  boxShadow: '0 2px 4px rgba(108,92,231,0.25)',
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
  );
}
