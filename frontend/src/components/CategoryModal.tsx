import { useState, useEffect } from 'react';
import { X, Tag, Trash2, AlertTriangle } from 'lucide-react';
import { Category } from '../types';

interface CategoryModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: (data: { id?: number; name: string; colorCode: string }) => Promise<void>;
  onDelete?: (id: number) => Promise<void>;
  category?: Category | null;
}

const PRESET_COLORS = [
  { color: '#6C5CE7', label: 'Tím SmartTask' },
  { color: '#3B82F6', label: 'Xanh dương' },
  { color: '#10B981', label: 'Xanh lá' },
  { color: '#F59E0B', label: 'Vàng cam' },
  { color: '#EF4444', label: 'Đỏ khẩn cấp' },
  { color: '#EC4899', label: 'Hồng phấn' },
  { color: '#06B6D4', label: 'Xanh cyan' },
  { color: '#8B5CF6', label: 'Tím lavender' },
];

export default function CategoryModal({
  isOpen,
  onClose,
  onSave,
  onDelete,
  category,
}: CategoryModalProps) {
  const [name, setName] = useState('');
  const [colorCode, setColorCode] = useState('#6C5CE7');
  const [loading, setLoading] = useState(false);

  const [isConfirmingDelete, setIsConfirmingDelete] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  useEffect(() => {
    setIsConfirmingDelete(false);
    setDeleteError(null);
    if (category) {
      setName(category.name);
      setColorCode(category.colorCode || '#6C5CE7');
    } else {
      setName('');
      setColorCode('#6C5CE7');
    }
  }, [category, isOpen]);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    try {
      setLoading(true);
      await onSave({
        id: category?.id,
        name: name.trim(),
        colorCode,
      });
      onClose();
    } finally {
      setLoading(false);
    }
  };

  const handleExecuteDelete = async () => {
    if (!category || !onDelete) return;
    try {
      setLoading(true);
      setDeleteError(null);
      await onDelete(category.id);
      onClose();
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Không thể xóa nhóm này vì đang có công việc đang sử dụng!';
      setDeleteError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.55)',
        backdropFilter: 'blur(4px)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 2000,
        padding: 16,
      }}
      onClick={onClose}
    >
      <div
        style={{
          width: '100%',
          maxWidth: 440,
          background: 'var(--surface)',
          borderRadius: 16,
          boxShadow: 'var(--shadow-lg), 0 25px 50px -12px rgba(0,0,0,0.25)',
          border: '1px solid var(--border)',
          overflow: 'hidden',
          animation: 'slideUpToast 0.2s cubic-bezier(0.16, 1, 0.3, 1)',
        }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '16px 20px',
            borderBottom: '1px solid var(--border-light)',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <div
              style={{
                width: 32,
                height: 32,
                borderRadius: 8,
                background: colorCode,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#fff',
                transition: 'background 0.2s',
              }}
            >
              <Tag size={16} />
            </div>
            <h3 style={{ fontSize: '1.05rem', fontWeight: 700, color: 'var(--text)' }}>
              {category ? 'Chỉnh sửa nhóm' : 'Thêm nhóm mới'}
            </h3>
          </div>
          <button
            onClick={onClose}
            style={{
              background: 'none',
              border: 'none',
              color: 'var(--text-muted)',
              cursor: 'pointer',
              padding: 4,
              borderRadius: 6,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <X size={18} />
          </button>
        </div>

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
                <h4 style={{ margin: 0, fontSize: '1.05rem', fontWeight: 700, color: 'var(--text)' }}>
                  Xác nhận xóa nhóm "{category?.name}"?
                </h4>
                <p style={{ margin: '4px 0 0', fontSize: '0.82rem', color: 'var(--text-3)' }}>
                  Hành động này sẽ xóa nhóm danh mục khỏi cơ sở dữ liệu.
                </p>
              </div>
            </div>

            <div
              style={{
                background: 'rgba(245, 158, 11, 0.08)',
                border: '1px solid rgba(245, 158, 11, 0.3)',
                borderRadius: 8,
                padding: '10px 14px',
                fontSize: '0.8rem',
                color: '#D97706',
                lineHeight: 1.5,
                marginBottom: 16,
              }}
            >
              Lưu ý: Hệ thống không cho phép xóa nhóm nếu đang có công việc sử dụng nhóm này.
            </div>

            {deleteError && (
              <div
                style={{
                  background: 'rgba(239, 68, 68, 0.08)',
                  border: '1px solid rgba(239, 68, 68, 0.3)',
                  borderRadius: 8,
                  padding: '10px 14px',
                  fontSize: '0.82rem',
                  color: '#EF4444',
                  lineHeight: 1.5,
                  marginBottom: 16,
                  fontWeight: 600,
                }}
              >
                {deleteError}
              </div>
            )}

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, paddingTop: 12, borderTop: '1px solid var(--border-light)' }}>
              <button
                type="button"
                onClick={() => {
                  setIsConfirmingDelete(false);
                  setDeleteError(null);
                }}
                disabled={loading}
                style={{
                  padding: '8px 16px',
                  borderRadius: 8,
                  border: '1px solid var(--border)',
                  background: 'transparent',
                  color: 'var(--text)',
                  fontSize: '0.85rem',
                  fontWeight: 500,
                  cursor: 'pointer',
                }}
              >
                Quay lại
              </button>
              <button
                type="button"
                onClick={handleExecuteDelete}
                disabled={loading}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 6,
                  padding: '8px 18px',
                  borderRadius: 8,
                  border: 'none',
                  background: '#EF4444',
                  color: '#fff',
                  fontSize: '0.85rem',
                  fontWeight: 600,
                  cursor: 'pointer',
                  opacity: loading ? 0.7 : 1,
                }}
              >
                <Trash2 size={14} />
                <span>{loading ? 'Đang xóa...' : 'Xóa nhóm'}</span>
              </button>
            </div>
          </div>
        ) : (
          <form onSubmit={handleSubmit} style={{ padding: 20 }}>
          <div style={{ marginBottom: 16 }}>
            <label
              style={{
                display: 'block',
                fontSize: '0.82rem',
                fontWeight: 600,
                color: 'var(--text)',
                marginBottom: 6,
              }}
            >
              Tên nhóm công việc <span style={{ color: '#EF4444' }}>*</span>
            </label>
            <input
              type="text"
              placeholder="VD: Dự án cá nhân, Gia đình, Thể thao..."
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
              autoFocus
              style={{
                width: '100%',
                padding: '9px 12px',
                border: '1px solid var(--border)',
                borderRadius: 8,
                fontSize: '0.9rem',
                background: 'var(--surface)',
                color: 'var(--text)',
                outline: 'none',
              }}
            />
          </div>

          {/* Color Selector */}
          <div style={{ marginBottom: 20 }}>
            <label
              style={{
                display: 'block',
                fontSize: '0.82rem',
                fontWeight: 600,
                color: 'var(--text)',
                marginBottom: 8,
              }}
            >
              Màu sắc nhận diện
            </label>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 8 }}>
              {PRESET_COLORS.map((item) => {
                const isSelected = colorCode === item.color;
                return (
                  <button
                    key={item.color}
                    type="button"
                    onClick={() => setColorCode(item.color)}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: 6,
                      padding: '6px 8px',
                      borderRadius: 8,
                      border: isSelected ? `2px solid ${item.color}` : '1px solid var(--border)',
                      background: isSelected ? 'var(--primary-bg)' : 'transparent',
                      cursor: 'pointer',
                      fontSize: '0.72rem',
                      fontWeight: isSelected ? 600 : 500,
                      color: 'var(--text)',
                      transition: 'all 0.15s',
                    }}
                  >
                    <span
                      style={{
                        width: 12,
                        height: 12,
                        borderRadius: '50%',
                        background: item.color,
                        flexShrink: 0,
                      }}
                    />
                    <span style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                      {item.label}
                    </span>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Footer Actions */}
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: category ? 'space-between' : 'flex-end',
              paddingTop: 12,
              borderTop: '1px solid var(--border-light)',
            }}
          >
            {category && onDelete && (
              <button
                type="button"
                onClick={() => {
                  setDeleteError(null);
                  setIsConfirmingDelete(true);
                }}
                disabled={loading}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 6,
                  color: '#EF4444',
                  background: 'none',
                  border: 'none',
                  fontSize: '0.85rem',
                  fontWeight: 600,
                  cursor: 'pointer',
                  padding: '6px 10px',
                  borderRadius: 6,
                }}
              >
                <Trash2 size={15} />
                <span>Xóa nhóm</span>
              </button>
            )}

            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <button
                type="button"
                onClick={onClose}
                disabled={loading}
                style={{
                  padding: '8px 16px',
                  borderRadius: 8,
                  border: '1px solid var(--border)',
                  background: 'transparent',
                  color: 'var(--text)',
                  fontSize: '0.85rem',
                  fontWeight: 500,
                  cursor: 'pointer',
                }}
              >
                Hủy
              </button>
              <button
                type="submit"
                disabled={loading || !name.trim()}
                style={{
                  padding: '8px 18px',
                  borderRadius: 8,
                  border: 'none',
                  background: 'var(--primary)',
                  color: '#fff',
                  fontSize: '0.85rem',
                  fontWeight: 600,
                  cursor: 'pointer',
                  opacity: loading || !name.trim() ? 0.6 : 1,
                }}
              >
                {loading ? 'Đang lưu...' : category ? 'Cập nhật' : 'Tạo nhóm'}
              </button>
            </div>
          </div>
        </form>
      )}
    </div>
  </div>
  );
}
