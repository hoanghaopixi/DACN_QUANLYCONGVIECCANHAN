import { useEffect } from 'react';
import { createPortal } from 'react-dom';
import { AlertTriangle, Trash2, X } from 'lucide-react';
import { useModalAnimation } from '../hooks/useModalAnimation';

interface ConfirmModalProps {
  isOpen: boolean;
  title: string;
  message: string;
  warningNote?: string | null;
  errorMessage?: string | null;
  confirmText?: string;
  cancelText?: string;
  isDanger?: boolean;
  isLoading?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}

export default function ConfirmModal({
  isOpen,
  title,
  message,
  warningNote,
  errorMessage,
  confirmText = 'Xóa dữ liệu',
  cancelText = 'Hủy bỏ',
  isDanger = true,
  isLoading = false,
  onConfirm,
  onCancel,
}: ConfirmModalProps) {
  const { shouldRender, isClosing } = useModalAnimation(isOpen, 180);

  useEffect(() => {
    if (!shouldRender) return;
    const prevOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = prevOverflow;
    };
  }, [shouldRender]);

  if (!shouldRender || typeof document === 'undefined') return null;

  return createPortal(
    <div
      style={{
        position: 'fixed',
        inset: 0,
        width: '100vw',
        height: '100vh',
        backgroundColor: 'rgba(15, 23, 42, 0.55)',
        backdropFilter: 'blur(4px)',
        zIndex: 10000,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: 20,
        boxSizing: 'border-box',
        animation: isClosing ? 'modalBackdropFadeOut 0.18s ease-in forwards' : 'modalBackdropFadeIn 0.18s ease-out',
        pointerEvents: isClosing ? 'none' : 'auto',
      }}
      onClick={onCancel}
    >
      <div
        style={{
          background: 'var(--surface)',
          borderRadius: 16,
          width: '100%',
          maxWidth: 420,
          boxShadow: 'var(--shadow-lg), 0 20px 25px -5px rgba(0,0,0,0.2)',
          border: '1px solid var(--border)',
          overflow: 'hidden',
          display: 'flex',
          flexDirection: 'column',
          animation: isClosing
            ? 'slideDownToast 0.18s cubic-bezier(0.4, 0, 1, 1) forwards'
            : 'slideUpToast 0.2s cubic-bezier(0.16, 1, 0.3, 1)',
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
                width: 36,
                height: 36,
                borderRadius: 10,
                background: isDanger ? 'rgba(239, 68, 68, 0.12)' : 'var(--primary-bg)',
                color: isDanger ? '#EF4444' : 'var(--primary)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                flexShrink: 0,
              }}
            >
              {isDanger ? <AlertTriangle size={18} /> : <Trash2 size={18} />}
            </div>
            <h3 style={{ fontSize: '1rem', fontWeight: 700, color: 'var(--text)', margin: 0 }}>
              {title}
            </h3>
          </div>

          <button
            type="button"
            onClick={onCancel}
            style={{
              width: 30,
              height: 30,
              borderRadius: 6,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: 'var(--text-muted)',
              cursor: 'pointer',
              background: 'none',
              border: 'none',
            }}
          >
            <X size={16} />
          </button>
        </div>

        {/* Content Body */}
        <div style={{ padding: '20px', display: 'flex', flexDirection: 'column', gap: 12 }}>
          <p style={{ fontSize: '0.88rem', color: 'var(--text-2)', lineHeight: 1.55, margin: 0 }}>
            {message}
          </p>

          {warningNote && (
            <div
              style={{
                fontSize: '0.78rem',
                color: '#B45309',
                background: '#FEF3C7',
                border: '1px solid #FDE68A',
                borderRadius: 8,
                padding: '8px 12px',
                lineHeight: 1.45,
              }}
            >
              {warningNote}
            </div>
          )}

          {errorMessage && (
            <div
              style={{
                fontSize: '0.8rem',
                color: '#DC2626',
                background: '#FEE2E2',
                border: '1px solid #FECACA',
                borderRadius: 8,
                padding: '10px 12px',
                lineHeight: 1.45,
                fontWeight: 600,
                display: 'flex',
                alignItems: 'center',
                gap: 8,
              }}
            >
              <AlertTriangle size={16} style={{ flexShrink: 0 }} />
              <span>{errorMessage}</span>
            </div>
          )}
        </div>

        {/* Actions Footer */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'flex-end',
            gap: 10,
            padding: '14px 20px',
            borderTop: '1px solid var(--border-light)',
            background: 'var(--border-light)',
          }}
        >
          <button
            type="button"
            onClick={onCancel}
            disabled={isLoading}
            style={{
              padding: '8px 16px',
              borderRadius: 8,
              border: '1px solid var(--border)',
              background: 'var(--surface)',
              color: 'var(--text)',
              fontSize: '0.84rem',
              fontWeight: 600,
              cursor: 'pointer',
            }}
          >
            {cancelText}
          </button>

          <button
            type="button"
            onClick={onConfirm}
            disabled={isLoading}
            style={{
              padding: '8px 18px',
              borderRadius: 8,
              border: 'none',
              background: isDanger ? '#EF4444' : 'var(--primary)',
              color: '#FFFFFF',
              fontSize: '0.84rem',
              fontWeight: 600,
              cursor: isLoading ? 'not-allowed' : 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: 6,
              boxShadow: isDanger ? '0 2px 8px rgba(239,68,68,0.35)' : '0 2px 8px rgba(108,92,231,0.35)',
              opacity: isLoading ? 0.7 : 1,
            }}
          >
            {isDanger && <Trash2 size={14} />}
            <span>{isLoading ? 'Đang xử lý...' : confirmText}</span>
          </button>
        </div>
      </div>
    </div>,
    document.body
  );
}
