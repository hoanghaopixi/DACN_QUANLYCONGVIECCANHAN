import { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { Shield, ShieldAlert, ShieldCheck, X, Check, Lock, Home } from 'lucide-react';
import { WorkspaceItem } from '../services/workspaceService';
import { aiPrivacyService } from '../services/aiPrivacyService';
import { useModalAnimation } from '../hooks/useModalAnimation';

interface AiPrivacyModalProps {
  isOpen: boolean;
  onClose: () => void;
  workspaces: WorkspaceItem[];
  activeWorkspace: WorkspaceItem | null;
  onShowToast?: (msg: string) => void;
}

export default function AiPrivacyModal({
  isOpen,
  onClose,
  workspaces,
  activeWorkspace,
  onShowToast,
}: AiPrivacyModalProps) {
  const { shouldRender, isClosing } = useModalAnimation(isOpen, 220);
  const [allowedIds, setAllowedIds] = useState<number[]>([]);

  useEffect(() => {
    if (shouldRender) {
      const currentAllowed = aiPrivacyService.getAllowedWorkspaceIds(workspaces);
      setAllowedIds(currentAllowed);
      const prevOverflow = document.body.style.overflow;
      document.body.style.overflow = 'hidden';
      return () => {
        document.body.style.overflow = prevOverflow;
      };
    }
  }, [shouldRender, workspaces]);

  if (!shouldRender || typeof document === 'undefined') return null;

  const handleToggle = (wsId: number) => {
    const isNowAllowed = aiPrivacyService.toggleWorkspace(wsId, workspaces);
    const updated = aiPrivacyService.getAllowedWorkspaceIds(workspaces);
    setAllowedIds(updated);

    const ws = workspaces.find((w) => w.id === wsId);
    const wsName = ws ? ws.name : `Không gian #${wsId}`;
    if (isNowAllowed) {
      onShowToast?.(`✓ Đã cho phép AI truy cập vào "${wsName}"!`);
    } else {
      onShowToast?.(`🔒 Đã khóa quyền AI tại "${wsName}" để bảo mật dữ liệu!`);
    }
  };

  const handleAllowAll = () => {
    aiPrivacyService.allowAll(workspaces);
    setAllowedIds(workspaces.map((w) => w.id));
    onShowToast?.('✓ Đã cho phép AI truy cập tất cả không gian làm việc!');
  };

  const handleLockAllExceptCurrent = () => {
    if (activeWorkspace) {
      aiPrivacyService.lockAllExcept(activeWorkspace.id);
      setAllowedIds([activeWorkspace.id]);
      onShowToast?.(`🔒 Đã khóa tất cả không gian khác, chỉ cho phép "${activeWorkspace.name}"!`);
    } else {
      aiPrivacyService.lockAllExcept();
      setAllowedIds([]);
      onShowToast?.('🔒 Đã khóa quyền AI đối với tất cả không gian làm việc!');
    }
  };

  const allowedCount = allowedIds.length;
  const totalCount = workspaces.length;

  return createPortal(
    <div
      className="modal-overlay"
      style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: 'rgba(15, 23, 42, 0.65)',
        backdropFilter: 'blur(6px)',
        WebkitBackdropFilter: 'blur(6px)',
        zIndex: 99999,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '20px',
        animation: isClosing ? 'modalBackdropFadeOut 0.22s ease-in forwards' : 'modalBackdropFadeIn 0.22s ease-out',
        pointerEvents: isClosing ? 'none' : 'auto',
        overflowY: 'auto',
      }}
      onClick={onClose}
    >
      <div
        className="modal-content"
        style={{
          width: '100%',
          maxWidth: '540px',
          background: 'var(--surface)',
          borderRadius: '16px',
          border: '1px solid var(--border)',
          boxShadow: '0 24px 48px -12px rgba(0, 0, 0, 0.25), 0 0 0 1px rgba(108, 92, 231, 0.15)',
          overflow: 'hidden',
          display: 'flex',
          flexDirection: 'column',
          maxHeight: 'min(90vh, 660px)',
          margin: 'auto',
          animation: isClosing
            ? 'modalSlideDown 0.22s cubic-bezier(0.4, 0, 1, 1) forwards'
            : 'modalSlideUp 0.28s cubic-bezier(0.16, 1, 0.3, 1)',
        }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div
          style={{
            padding: '18px 22px',
            borderBottom: '1px solid var(--border-light)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            background: 'linear-gradient(135deg, rgba(108, 92, 231, 0.08), rgba(99, 102, 241, 0.03))',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <div
              style={{
                width: 40,
                height: 40,
                borderRadius: 10,
                background: 'linear-gradient(135deg, #6C5CE7, #4F46E5)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#fff',
                boxShadow: '0 4px 12px rgba(108, 92, 231, 0.3)',
              }}
            >
              <Shield size={22} />
            </div>
            <div>
              <h3 style={{ margin: 0, fontSize: '1.08rem', fontWeight: 700, color: 'var(--text)' }}>
                Quyền AI truy cập Không gian
              </h3>
              <p style={{ margin: '2px 0 0', fontSize: '0.78rem', color: 'var(--text-muted)' }}>
                Kiểm soát bảo mật và ngăn ngừa rò rỉ bí mật công việc cá nhân
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            style={{
              background: 'none',
              border: 'none',
              color: 'var(--text-muted)',
              cursor: 'pointer',
              padding: 6,
              borderRadius: 8,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              transition: 'all 0.15s ease',
            }}
            title="Đóng"
          >
            <X size={18} />
          </button>
        </div>

        {/* Info Banner */}
        <div
          style={{
            margin: '16px 22px 8px',
            padding: '12px 14px',
            background: 'var(--border-light)',
            borderRadius: 12,
            border: '1px solid var(--border)',
            display: 'flex',
            alignItems: 'flex-start',
            gap: 10,
            fontSize: '0.8rem',
            color: 'var(--text-3)',
            lineHeight: 1.5,
          }}
        >
          <Lock size={16} style={{ color: '#F59E0B', flexShrink: 0, marginTop: 2 }} />
          <div>
            <strong style={{ color: 'var(--text)', display: 'block', marginBottom: 2 }}>
              Cơ chế bảo vệ dữ liệu bí mật:
            </strong>
            Khi một không gian bị <strong>khóa quyền</strong>, SmartTask AI sẽ không bao giờ đọc dữ liệu, không trả lời chi tiết công việc hoặc tự động xếp lịch trong không gian đó.
          </div>
        </div>

        {/* Quick Toolbar */}
        <div
          style={{
            padding: '8px 22px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: 8,
            fontSize: '0.8rem',
          }}
        >
          <span style={{ fontWeight: 600, color: 'var(--text-muted)' }}>
            Đang cho phép: <span style={{ color: 'var(--primary)' }}>{allowedCount}/{totalCount}</span> không gian
          </span>
          <div style={{ display: 'flex', gap: 6 }}>
            <button
              type="button"
              onClick={handleAllowAll}
              style={{
                fontSize: '0.74rem',
                padding: '4px 8px',
                borderRadius: 6,
                border: '1px solid var(--border)',
                background: 'var(--surface)',
                color: 'var(--text-3)',
                cursor: 'pointer',
                fontWeight: 500,
                transition: 'all 0.15s',
              }}
            >
              Cho phép tất cả
            </button>
            <button
              type="button"
              onClick={handleLockAllExceptCurrent}
              style={{
                fontSize: '0.74rem',
                padding: '4px 8px',
                borderRadius: 6,
                border: '1px solid var(--border)',
                background: 'var(--surface)',
                color: 'var(--text-3)',
                cursor: 'pointer',
                fontWeight: 500,
                transition: 'all 0.15s',
              }}
            >
              Chỉ không gian hiện tại
            </button>
          </div>
        </div>

        {/* Workspaces List */}
        <div
          style={{
            padding: '8px 22px 18px',
            overflowY: 'auto',
            display: 'flex',
            flexDirection: 'column',
            gap: 10,
          }}
        >
          {workspaces.map((ws) => {
            const isAllowed = allowedIds.includes(ws.id);
            const isCurrent = activeWorkspace?.id === ws.id;

            return (
              <div
                key={ws.id}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  padding: '12px 14px',
                  borderRadius: 12,
                  border: isAllowed ? '1px solid var(--border)' : '1px solid rgba(239, 68, 68, 0.3)',
                  background: isAllowed ? 'var(--surface)' : 'rgba(239, 68, 68, 0.03)',
                  transition: 'all 0.2s cubic-bezier(0.16, 1, 0.3, 1)',
                }}
              >
                {/* Left: Icon & Info */}
                <div style={{ display: 'flex', alignItems: 'center', gap: 12, flex: 1, minWidth: 0 }}>
                  <div
                    style={{
                      width: 36,
                      height: 36,
                      borderRadius: 9,
                      background: isAllowed ? 'var(--primary-bg)' : 'rgba(239, 68, 68, 0.12)',
                      color: isAllowed ? 'var(--primary)' : '#EF4444',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      flexShrink: 0,
                    }}
                  >
                    {isAllowed ? <Home size={18} /> : <Lock size={18} />}
                  </div>

                  <div style={{ minWidth: 0, flex: 1 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6, flexWrap: 'wrap' }}>
                      <span
                        style={{
                          fontWeight: 600,
                          fontSize: '0.88rem',
                          color: 'var(--text)',
                          whiteSpace: 'nowrap',
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                        }}
                      >
                        {ws.name}
                      </span>
                      {isCurrent && (
                        <span
                          style={{
                            fontSize: '0.68rem',
                            fontWeight: 600,
                            padding: '2px 6px',
                            borderRadius: 4,
                            background: 'var(--primary-bg)',
                            color: 'var(--primary)',
                            border: '1px solid rgba(108, 92, 231, 0.25)',
                          }}
                        >
                          Đang chọn
                        </span>
                      )}
                    </div>

                    <div style={{ fontSize: '0.74rem', color: isAllowed ? '#10B981' : '#EF4444', marginTop: 2, display: 'flex', alignItems: 'center', gap: 4 }}>
                      {isAllowed ? (
                        <>
                          <ShieldCheck size={13} />
                          <span>Cho phép AI đọc & lên lịch</span>
                        </>
                      ) : (
                        <>
                          <ShieldAlert size={13} />
                          <span>Đã khóa bảo mật (AI bị chặn)</span>
                        </>
                      )}
                    </div>
                  </div>
                </div>

                {/* Right: Toggle Switch */}
                <button
                  type="button"
                  onClick={() => handleToggle(ws.id)}
                  style={{
                    width: 44,
                    height: 24,
                    borderRadius: 12,
                    background: isAllowed ? '#10B981' : '#CBD5E1',
                    border: 'none',
                    cursor: 'pointer',
                    position: 'relative',
                    transition: 'background 0.2s ease',
                    flexShrink: 0,
                    padding: 0,
                    marginLeft: 12,
                  }}
                  title={isAllowed ? 'Nhấp để khóa quyền riêng tư với AI' : 'Nhấp để cho phép AI truy cập'}
                >
                  <span
                    style={{
                      position: 'absolute',
                      top: 2,
                      left: isAllowed ? 22 : 2,
                      width: 20,
                      height: 20,
                      borderRadius: 10,
                      background: '#fff',
                      boxShadow: '0 2px 4px rgba(0,0,0,0.2)',
                      transition: 'left 0.2s cubic-bezier(0.34, 1.56, 0.64, 1)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                    }}
                  >
                    {isAllowed ? (
                      <Check size={11} style={{ color: '#10B981' }} />
                    ) : (
                      <Lock size={10} style={{ color: '#64748B' }} />
                    )}
                  </span>
                </button>
              </div>
            );
          })}
        </div>

        {/* Footer */}
        <div
          style={{
            padding: '14px 22px',
            borderTop: '1px solid var(--border-light)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'flex-end',
            background: 'var(--surface)',
          }}
        >
          <button
            type="button"
            onClick={onClose}
            style={{
              padding: '8px 20px',
              borderRadius: 8,
              background: 'var(--primary)',
              color: '#fff',
              border: 'none',
              fontWeight: 600,
              fontSize: '0.84rem',
              cursor: 'pointer',
              transition: 'background 0.15s ease',
            }}
          >
            Hoàn tất
          </button>
        </div>
      </div>
    </div>,
    document.body
  );
}
