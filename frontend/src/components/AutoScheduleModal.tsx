import { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';
import {
  X, Sparkles, CheckCircle2,
  Loader2, Zap
} from 'lucide-react';
import { AutoScheduleResponse } from '../types';
import { autoScheduleService } from '../services/autoScheduleService';
import { aiPrivacyService } from '../services/aiPrivacyService';

interface AutoScheduleModalProps {
  isOpen: boolean;
  onClose: () => void;
  workspaceId?: number;
  onApplied: () => void;
  onShowToast: (msg: string) => void;
}

export default function AutoScheduleModal({
  isOpen,
  onClose,
  workspaceId,
  onApplied,
  onShowToast,
}: AutoScheduleModalProps) {
  const getTodayStr = () => {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  };

  const getTomorrowStr = () => {
    const d = new Date();
    d.setDate(d.getDate() + 1);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  };

  const [targetDate, setTargetDate] = useState<string>(getTodayStr());
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [isApplying, setIsApplying] = useState<boolean>(false);
  const [previewResult, setPreviewResult] = useState<AutoScheduleResponse | null>(null);
  const [previewContext, setPreviewContext] = useState<{
    targetDate: string;
    workspaceId?: number;
    breakStartTime: string;
    breakEndTime: string;
  } | null>(null);

  const getBreakTimeSettings = () => {
    try {
      const stored = localStorage.getItem('smarttask_settings_full');
      const settings = stored ? JSON.parse(stored) as { lunchBreak?: string } : null;
      const match = settings?.lunchBreak?.match(/^\s*(\d{2}:\d{2})\s*-\s*(\d{2}:\d{2})\s*$/);
      if (match) return { breakStartTime: match[1], breakEndTime: match[2] };
    } catch {
      // Use the app's standard lunch window when stored settings are unavailable.
    }
    return { breakStartTime: '12:00', breakEndTime: '13:00' };
  };

  const handleTargetDateChange = (date: string) => {
    setTargetDate(date);
    setPreviewResult(null);
    setPreviewContext(null);
  };

  useEffect(() => {
    if (!isOpen) return;
    const prevOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = prevOverflow;
    };
  }, [isOpen]);

  if (!isOpen || typeof document === 'undefined') return null;

  const isWorkspaceAllowed = aiPrivacyService.isWorkspaceAllowed(workspaceId);

  const handleGeneratePreview = async () => {
    if (!isWorkspaceAllowed) {
      onShowToast(' Không gian này đang bị khóa quyền riêng tư để bảo vệ dữ liệu bí mật.');
      return;
    }
    setIsLoading(true);
    setPreviewResult(null);
    setPreviewContext(null);
    try {
      const context = { targetDate, workspaceId, ...getBreakTimeSettings() };
      const res = await autoScheduleService.autoSchedule({
        ...context,
        applyImmediately: false,
      });
      setPreviewResult(res);
      setPreviewContext(context);
      if (res.proposals.length === 0) {
        onShowToast('Không có công việc nào cần xếp lịch hoặc không còn khoảng trống phù hợp!');
      } else {
        onShowToast(` Đã tìm thấy ${res.proposals.length} vị trí sắp xếp tối ưu!`);
      }
    } catch (err) {
      console.error('Lỗi khi chạy tự động xếp lịch:', err);
      onShowToast('Lỗi khi gọi trợ lý AI xếp lịch!');
    } finally {
      setIsLoading(false);
    }
  };

  const handleApplySchedule = async () => {
    if (!previewResult || !previewContext || previewResult.proposals.length === 0) return;
    setIsApplying(true);
    try {
      const appliedResult = await autoScheduleService.autoSchedule({
        ...previewContext,
        targetDate: previewResult.targetDate,
        taskIds: previewResult.proposals.map((proposal) => proposal.taskId),
        applyImmediately: true,
      });
      if (!appliedResult.applied || !appliedResult.scheduledCount) {
        setPreviewResult(null);
        setPreviewContext(null);
        onShowToast('Lịch đã thay đổi nên kế hoạch cũ không còn áp dụng được. Hãy tạo bản xem trước mới nhé.');
        return;
      }
      onShowToast(`🎉 Đã áp dụng ${appliedResult.scheduledCount || 0} công việc vào lịch trình thành công!`);
      onApplied();
      onClose();
    } catch (err) {
      console.error('Lỗi khi áp dụng lịch trình:', err);
      onShowToast('Lỗi khi lưu lịch trình vào máy chủ!');
    } finally {
      setIsApplying(false);
    }
  };

  const formatSlotTime = (startStr: string, endStr: string) => {
    const s = new Date(startStr);
    const e = new Date(endStr);
    return `${s.getHours().toString().padStart(2, '0')}:${s.getMinutes().toString().padStart(2, '0')} – ${e.getHours().toString().padStart(2, '0')}:${e.getMinutes().toString().padStart(2, '0')}`;
  };

  return createPortal(
    <div
      style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: 'rgba(15, 23, 42, 0.65)',
        backdropFilter: 'blur(5px)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 9999,
        padding: 16,
      }}
    >
      <div
        style={{
          background: 'var(--surface)',
          borderRadius: 20,
          border: '1px solid var(--border)',
          width: '100%',
          maxWidth: 620,
          maxHeight: '90vh',
          display: 'flex',
          flexDirection: 'column',
          boxShadow: 'var(--shadow-lg)',
          overflow: 'hidden',
          animation: 'modalSlideUp 0.25s cubic-bezier(0.16, 1, 0.3, 1)',
        }}
      >
        {/* Header */}
        <div
          style={{
            padding: '18px 24px',
            borderBottom: '1px solid var(--border)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            background: 'linear-gradient(135deg, rgba(108, 92, 231, 0.08), rgba(59, 130, 246, 0.05))',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <div
              style={{
                width: 36,
                height: 36,
                borderRadius: 10,
                background: 'linear-gradient(135deg, #6C5CE7, #3B82F6)',
                color: '#fff',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <Zap size={20} />
            </div>
            <div>
              <h2 style={{ fontSize: '1.15rem', fontWeight: 800, color: 'var(--text)' }}>
                Tự động xếp lịch thông minh 1 chạm
              </h2>
              <p style={{ fontSize: '0.78rem', color: 'var(--text-3)' }}>
                AI phân tích thời gian trống và xếp việc theo Ma trận Eisenhower & Giờ làm việc.
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            style={{
              padding: 6,
              borderRadius: 8,
              border: 'none',
              background: 'transparent',
              color: 'var(--text-muted)',
              cursor: 'pointer',
            }}
          >
            <X size={18} />
          </button>
        </div>

        {/* Body content */}
        <div style={{ padding: '20px 24px', overflowY: 'auto', flex: 1 }}>
          {/* Target Date Selector */}
          <div style={{ marginBottom: 18 }}>
            <label style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--text)', display: 'block', marginBottom: 8 }}>
              CHỌN NGÀY CẦN XẾP LỊCH:
            </label>
            <div style={{ display: 'flex', gap: 10 }}>
              <button
                type="button"
                onClick={() => handleTargetDateChange(getTodayStr())}
                style={{
                  flex: 1,
                  padding: '10px 14px',
                  borderRadius: 10,
                  border: targetDate === getTodayStr() ? '1.5px solid var(--primary)' : '1px solid var(--border)',
                  background: targetDate === getTodayStr() ? 'var(--primary-bg)' : 'var(--surface)',
                  color: targetDate === getTodayStr() ? 'var(--primary)' : 'var(--text)',
                  fontWeight: 700,
                  fontSize: '0.84rem',
                  cursor: 'pointer',
                }}
              >
                📅 Hôm nay ({getTodayStr()})
              </button>
              <button
                type="button"
                onClick={() => handleTargetDateChange(getTomorrowStr())}
                style={{
                  flex: 1,
                  padding: '10px 14px',
                  borderRadius: 10,
                  border: targetDate === getTomorrowStr() ? '1.5px solid var(--primary)' : '1px solid var(--border)',
                  background: targetDate === getTomorrowStr() ? 'var(--primary-bg)' : 'var(--surface)',
                  color: targetDate === getTomorrowStr() ? 'var(--primary)' : 'var(--text)',
                  fontWeight: 700,
                  fontSize: '0.84rem',
                  cursor: 'pointer',
                }}
              >
                🚀 Ngày mai ({getTomorrowStr()})
              </button>
            </div>
          </div>

          {/* Action Trigger Button */}
          {!previewResult && (
            <button
              type="button"
              onClick={handleGeneratePreview}
              disabled={isLoading}
              style={{
                width: '100%',
                padding: '12px 20px',
                borderRadius: 12,
                background: 'linear-gradient(135deg, var(--primary), #3B82F6)',
                color: '#fff',
                fontSize: '0.92rem',
                fontWeight: 700,
                border: 'none',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: 10,
                cursor: isLoading ? 'not-allowed' : 'pointer',
                boxShadow: '0 4px 14px rgba(108, 92, 231, 0.3)',
                marginBottom: 16,
              }}
            >
              {isLoading ? (
                <>
                  <Loader2 size={18} className="spin-animate" />
                  <span>AI đang tìm khoảng trống & tối ưu ma trận...</span>
                </>
              ) : (
                <>
                  <Sparkles size={18} />
                  <span>Phân tích & Tự động tạo lịch trình</span>
                </>
              )}
            </button>
          )}

          {/* AI Strategy Preview */}
          {previewResult && (
            <div>
              <div
                style={{
                  padding: '12px 16px',
                  borderRadius: 12,
                  background: 'var(--primary-bg)',
                  border: '1px solid rgba(108, 92, 231, 0.2)',
                  marginBottom: 16,
                  fontSize: '0.84rem',
                  color: 'var(--primary-dark)',
                  display: 'flex',
                  alignItems: 'flex-start',
                  gap: 10,
                }}
              >
                <Sparkles size={18} style={{ flexShrink: 0, marginTop: 2, color: 'var(--primary)' }} />
                <div>
                  <div style={{ fontWeight: 700, marginBottom: 2 }}>Kế hoạch xếp lịch của AI:</div>
                  <div>{previewResult.aiStrategySummary}</div>
                </div>
              </div>

              <div style={{ fontSize: '0.82rem', fontWeight: 700, color: 'var(--text)', marginBottom: 8, display: 'flex', justifyContent: 'space-between' }}>
                <span>DANH SÁCH {previewResult.proposals.length} CÔNG VIỆC ĐƯỢC XẾP:</span>
                <button
                  type="button"
                  onClick={handleGeneratePreview}
                  style={{ color: 'var(--primary)', cursor: 'pointer', fontSize: '0.78rem' }}
                >
                  Xếp lại
                </button>
              </div>

              {previewResult.proposals.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '24px 0', color: 'var(--text-muted)', fontSize: '0.84rem' }}>
                  Không còn công việc chờ xử lý trong danh sách.
                </div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 10, maxHeight: 300, overflowY: 'auto', paddingRight: 4 }}>
                  {previewResult.proposals.map((p, idx) => (
                    <div
                      key={idx}
                      style={{
                        padding: '12px 14px',
                        borderRadius: 12,
                        background: 'var(--border-light)',
                        border: '1px solid var(--border)',
                        display: 'flex',
                        flexDirection: 'column',
                        gap: 6,
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                        <div style={{ fontWeight: 700, fontSize: '0.88rem', color: 'var(--text)' }}>
                          {p.title}
                        </div>
                        <div
                          style={{
                            fontSize: '0.74rem',
                            fontWeight: 700,
                            padding: '3px 8px',
                            borderRadius: 6,
                            background: 'var(--surface)',
                            color: 'var(--primary)',
                            border: '1px solid var(--border)',
                          }}
                        >
                          ⏰ {formatSlotTime(p.startTime, p.endTime)} ({p.durationMinutes}p)
                        </div>
                      </div>

                      <div style={{ fontSize: '0.76rem', color: 'var(--text-3)', display: 'flex', alignItems: 'center', gap: 6 }}>
                        <span
                          style={{
                            padding: '2px 6px',
                            borderRadius: 4,
                            background: p.eisenhowerQuadrant === 'DO_FIRST' ? '#FEE2E2' : '#EDE9FE',
                            color: p.eisenhowerQuadrant === 'DO_FIRST' ? '#DC2626' : '#4F46E5',
                            fontWeight: 700,
                            fontSize: '0.7rem',
                          }}
                        >
                          {p.eisenhowerQuadrant === 'DO_FIRST' ? 'Q1: Làm ngay' : 'Q2: Lên lịch'}
                        </span>
                        <span>• {p.slotRationale}</span>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>

        {/* Footer */}
        <div
          style={{
            padding: '14px 24px',
            borderTop: '1px solid var(--border)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'flex-end',
            gap: 12,
            background: 'var(--surface)',
          }}
        >
          <button
            type="button"
            onClick={onClose}
            style={{
              padding: '9px 18px',
              borderRadius: 10,
              border: '1px solid var(--border)',
              background: 'transparent',
              color: 'var(--text-3)',
              fontWeight: 600,
              fontSize: '0.86rem',
              cursor: 'pointer',
            }}
          >
            Đóng
          </button>

          {previewResult && previewResult.proposals.length > 0 && (
            <button
              type="button"
              onClick={handleApplySchedule}
              disabled={isApplying || isLoading}
              style={{
                padding: '9px 24px',
                borderRadius: 10,
                border: 'none',
                background: '#10B981',
                color: '#fff',
                fontWeight: 700,
                fontSize: '0.88rem',
                display: 'flex',
                alignItems: 'center',
                gap: 8,
                cursor: isApplying || isLoading ? 'not-allowed' : 'pointer',
                boxShadow: '0 4px 12px rgba(16, 185, 129, 0.3)',
              }}
            >
              {isApplying ? (
                <>
                  <Loader2 size={16} className="spin-animate" />
                  <span>Đang ghi vào lịch trình...</span>
                </>
              ) : (
                <>
                  <CheckCircle2 size={16} />
                  <span>Xác nhận áp dụng vào Lịch trình</span>
                </>
              )}
            </button>
          )}
        </div>
      </div>
    </div>,
    document.body
  );
}
