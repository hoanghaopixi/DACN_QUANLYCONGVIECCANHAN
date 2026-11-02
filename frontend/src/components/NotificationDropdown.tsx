import React, { useState, useEffect, useCallback } from 'react';
import { Bell, CheckCheck, Clock, Sparkles, CheckCircle2, AlertTriangle, Trash2, X, RefreshCw } from 'lucide-react';
import { notificationService, NotificationItem } from '../services/notificationService';
import { formatDateDDMMYYYY } from '../utils/dateTimeFormat';

export type { NotificationItem };

interface NotificationDropdownProps {
  isOpen: boolean;
  onClose: () => void;
  onSelectTask?: (taskId: number) => void;
  onUnreadCountChange?: (count: number) => void;
}

function formatRelativeTime(dateStr?: string): string {
  if (!dateStr) return 'Gần đây';
  try {
    const created = new Date(dateStr);
    const now = new Date();
    const diffMs = now.getTime() - created.getTime();
    if (diffMs < 0) return 'Vừa xong';
    const diffSec = Math.floor(diffMs / 1000);
    const diffMin = Math.floor(diffSec / 60);
    const diffHours = Math.floor(diffMin / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffMin < 1) return 'Vừa xong';
    if (diffMin < 60) return `${diffMin} phút trước`;
    if (diffHours < 24) return `${diffHours} giờ trước`;
    if (diffDays === 1) return 'Hôm qua';
    if (diffDays < 7) return `${diffDays} ngày trước`;
    return formatDateDDMMYYYY(created);
  } catch {
    return 'Gần đây';
  }
}

export default function NotificationDropdown({
  isOpen,
  onClose,
  onSelectTask,
  onUnreadCountChange,
}: NotificationDropdownProps) {
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [isEvaluating, setIsEvaluating] = useState(false);

  const fetchNotifications = useCallback(async () => {
    try {
      setLoading(true);
      const list = await notificationService.getNotifications();
      setNotifications(list);
      const unread = list.filter((n) => !n.isRead).length;
      onUnreadCountChange?.(unread);
    } catch (err) {
      console.error('Lỗi khi tải thông báo từ CSDL:', err);
    } finally {
      setLoading(false);
    }
  }, [onUnreadCountChange]);

  const handleEvaluate = async () => {
    try {
      setIsEvaluating(true);
      const list = await notificationService.evaluateNotifications();
      setNotifications(list);
      const unread = list.filter((n) => !n.isRead).length;
      onUnreadCountChange?.(unread);
    } catch (err) {
      console.error('Lỗi chạy thuật toán thông báo:', err);
    } finally {
      setIsEvaluating(false);
    }
  };

  const [shouldRender, setShouldRender] = useState(isOpen);
  const [isExiting, setIsExiting] = useState(false);

  useEffect(() => {
    if (isOpen) {
      setShouldRender(true);
      setIsExiting(false);
      fetchNotifications();
    } else if (shouldRender) {
      setIsExiting(true);
      const timer = setTimeout(() => {
        setShouldRender(false);
        setIsExiting(false);
      }, 200);
      return () => clearTimeout(timer);
    }
  }, [isOpen, fetchNotifications, shouldRender]);

  if (!shouldRender) return null;

  const unreadCount = notifications.filter((n) => !n.isRead).length;

  const handleMarkAllRead = async () => {
    try {
      await notificationService.markAllAsRead();
      setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
      onUnreadCountChange?.(0);
    } catch (err) {
      console.error('Lỗi đánh dấu tất cả thông báo là đã đọc:', err);
    }
  };

  const handleItemClick = async (item: NotificationItem) => {
    if (!item.isRead) {
      try {
        await notificationService.markAsRead(item.id);
        const updated = notifications.map((n) =>
          n.id === item.id ? { ...n, isRead: true } : n
        );
        setNotifications(updated);
        const newUnread = updated.filter((n) => !n.isRead).length;
        onUnreadCountChange?.(newUnread);
      } catch (err) {
        console.error('Lỗi cập nhật trạng thái đọc:', err);
      }
    }
    if (item.taskId && onSelectTask) {
      onSelectTask(item.taskId);
      onClose();
    }
  };

  const handleDeleteItem = async (e: React.MouseEvent, id: number) => {
    e.stopPropagation();
    try {
      await notificationService.deleteNotification(id);
      const updated = notifications.filter((n) => n.id !== id);
      setNotifications(updated);
      const newUnread = updated.filter((n) => !n.isRead).length;
      onUnreadCountChange?.(newUnread);
    } catch (err) {
      console.error('Lỗi xóa thông báo khỏi CSDL:', err);
    }
  };

  const handleClearAll = async () => {
    try {
      await notificationService.clearAll();
      setNotifications([]);
      onUnreadCountChange?.(0);
    } catch (err) {
      console.error('Lỗi xóa toàn bộ thông báo khỏi CSDL:', err);
    }
  };

  const getIcon = (type: string) => {
    switch (type) {
      case 'reminder':
        return <Clock size={16} style={{ color: '#3B82F6' }} />;
      case 'ai':
        return <Sparkles size={16} style={{ color: '#8B5CF6' }} />;
      case 'alert':
        return <AlertTriangle size={16} style={{ color: '#F59E0B' }} />;
      case 'success':
        return <CheckCircle2 size={16} style={{ color: '#10B981' }} />;
      default:
        return <Bell size={16} style={{ color: 'var(--primary)' }} />;
    }
  };

  return (
    <div
      style={{
        position: 'absolute',
        top: 'calc(100% + 10px)',
        right: 0,
        width: 390,
        maxWidth: '92vw',
        background: 'var(--surface)',
        borderRadius: 16,
        boxShadow: 'var(--shadow-lg), 0 20px 30px rgba(0,0,0,0.14)',
        border: '1px solid var(--border)',
        zIndex: 1000,
        overflow: 'hidden',
        animation: isExiting
          ? 'dropdownFadeOut 0.2s cubic-bezier(0.16, 1, 0.3, 1) forwards'
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
          padding: '14px 16px',
          borderBottom: '1px solid var(--border-light)',
          background: 'var(--surface)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <Bell size={17} style={{ color: 'var(--primary)' }} />
          <span style={{ fontWeight: 700, fontSize: '0.92rem', color: 'var(--text)' }}>
            Thông báo
          </span>
          {unreadCount > 0 && (
            <span
              style={{
                fontSize: '0.72rem',
                fontWeight: 700,
                background: '#EF4444',
                color: '#fff',
                padding: '1px 7px',
                borderRadius: 10,
              }}
            >
              {unreadCount} mới
            </span>
          )}
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          {/* Quét phân tích thuật toán AI */}
          <button
            onClick={handleEvaluate}
            disabled={isEvaluating}
            title="Kích hoạt Thuật toán Phân tích Lịch trình & Deadline AI"
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 4,
              background: 'var(--primary-bg)',
              border: '1px solid rgba(108, 92, 231, 0.25)',
              fontSize: '0.74rem',
              fontWeight: 600,
              color: 'var(--primary)',
              cursor: 'pointer',
              padding: '3px 8px',
              borderRadius: 6,
              transition: 'all 0.15s ease',
            }}
          >
            <RefreshCw size={12} style={{ animation: isEvaluating ? 'spin 1s linear infinite' : 'none' }} />
            <span>{isEvaluating ? 'Đang quét...' : 'Quét AI'}</span>
          </button>

          {unreadCount > 0 && (
            <button
              onClick={handleMarkAllRead}
              title="Đánh dấu tất cả là đã đọc (Lưu vào CSDL)"
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 4,
                background: 'none',
                border: 'none',
                fontSize: '0.74rem',
                fontWeight: 600,
                color: 'var(--text-2)',
                cursor: 'pointer',
                padding: '3px 6px',
              }}
            >
              <CheckCheck size={14} />
              <span>Đọc hết</span>
            </button>
          )}

          {notifications.length > 0 && (
            <button
              onClick={handleClearAll}
              title="Xóa toàn bộ thông báo khỏi CSDL"
              style={{
                background: 'none',
                border: 'none',
                cursor: 'pointer',
                color: 'var(--text-muted)',
                padding: 4,
                borderRadius: 4,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <Trash2 size={14} />
            </button>
          )}

          <button
            onClick={onClose}
            title="Đóng bảng thông báo"
            style={{
              background: 'none',
              border: 'none',
              cursor: 'pointer',
              color: 'var(--text-muted)',
              padding: 4,
              borderRadius: 4,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <X size={15} />
          </button>
        </div>
      </div>

      {/* List */}
      <div style={{ maxHeight: 370, overflowY: 'auto' }}>
        {loading && notifications.length === 0 ? (
          <div style={{ padding: '28px 20px', textAlign: 'center', color: 'var(--text-muted)', fontSize: '0.85rem' }}>
            Đang tải thông báo từ CSDL...
          </div>
        ) : notifications.length === 0 ? (
          <div
            style={{
              padding: '36px 20px',
              textAlign: 'center',
              color: 'var(--text-muted)',
              fontSize: '0.85rem',
            }}
          >
            <Bell size={32} style={{ margin: '0 auto 10px', opacity: 0.35 }} />
            <div>Không có thông báo!</div>
          </div>
        ) : (
          notifications.map((item) => (
            <div
              key={item.id}
              onClick={() => handleItemClick(item)}
              style={{
                display: 'flex',
                alignItems: 'flex-start',
                gap: 12,
                padding: '12px 16px',
                borderBottom: '1px solid var(--border-light)',
                cursor: 'pointer',
                background: item.isRead ? 'var(--surface)' : 'var(--primary-bg)',
                transition: 'background-color 0.15s ease',
                position: 'relative',
              }}
            >
              {/* Type Icon */}
              <div
                style={{
                  width: 32,
                  height: 32,
                  borderRadius: 8,
                  background: 'var(--surface)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  boxShadow: '0 1px 3px rgba(0,0,0,0.06)',
                  flexShrink: 0,
                  marginTop: 2,
                }}
              >
                {getIcon(item.type)}
              </div>

              {/* Text info */}
              <div style={{ flex: 1, minWidth: 0 }}>
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    gap: 8,
                  }}
                >
                  <span
                    style={{
                      fontSize: '0.84rem',
                      fontWeight: item.isRead ? 600 : 700,
                      color: 'var(--text)',
                    }}
                  >
                    {item.title}
                  </span>
                  <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)', whiteSpace: 'nowrap' }}>
                    {formatRelativeTime(item.createdAt)}
                  </span>
                </div>

                <p
                  style={{
                    fontSize: '0.78rem',
                    color: 'var(--text-3)',
                    margin: '3px 0 0',
                    lineHeight: 1.45,
                  }}
                >
                  {item.message}
                </p>
              </div>

              {/* Status and Delete Action */}
              <div style={{ display: 'flex', alignItems: 'center', gap: 6, flexShrink: 0, marginTop: 4 }}>
                {!item.isRead && (
                  <span
                    title="Chưa đọc"
                    style={{
                      width: 8,
                      height: 8,
                      borderRadius: '50%',
                      background: '#EF4444',
                      display: 'inline-block',
                    }}
                  />
                )}
                <button
                  type="button"
                  onClick={(e) => handleDeleteItem(e, item.id)}
                  title="Xóa thông báo này khỏi CSDL"
                  style={{
                    background: 'none',
                    border: 'none',
                    color: 'var(--text-muted)',
                    cursor: 'pointer',
                    padding: 3,
                    borderRadius: 4,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    opacity: 0.6,
                    transition: 'opacity 0.15s',
                  }}
                  onMouseEnter={(e) => (e.currentTarget.style.opacity = '1')}
                  onMouseLeave={(e) => (e.currentTarget.style.opacity = '0.6')}
                >
                  <Trash2 size={13} />
                </button>
              </div>
            </div>
          ))
        )}
      </div>

      {/* Footer Info */}
      <div
        style={{
          padding: '8px 16px',
          background: 'var(--border-light)',
          fontSize: '0.72rem',
          color: 'var(--text-muted)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
        }}
      >

        <span style={{ fontWeight: 600 }}>{notifications.length} thông báo</span>
      </div>
    </div>
  );
}
