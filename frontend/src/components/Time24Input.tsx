import React, { useState, useRef, useEffect } from 'react';
import { Clock } from 'lucide-react';
import { useModalAnimation } from '../hooks/useModalAnimation';

interface Time24InputProps {
  value: string; // "HH:mm" (24h)
  onChange: (val: string) => void;
  id?: string;
  name?: string;
  disabled?: boolean;
  align?: 'left' | 'right' | 'auto';
  style?: React.CSSProperties;
  className?: string;
}

const HOURS = Array.from({ length: 24 }, (_, i) => String(i).padStart(2, '0'));
const MINUTES = ['00', '05', '10', '15', '20', '25', '30', '35', '40', '45', '50', '55'];

export default function Time24Input({
  value,
  onChange,
  id,
  name,
  disabled = false,
  align = 'left',
  style = {},
  className = '',
}: Time24InputProps) {
  const [isOpen, setIsOpen] = useState(false);
  const { shouldRender, isClosing } = useModalAnimation(isOpen, 150);
  const [textValue, setTextValue] = useState(value || '09:00');
  const containerRef = useRef<HTMLDivElement>(null);
  const hoursColRef = useRef<HTMLDivElement>(null);
  const minutesColRef = useRef<HTMLDivElement>(null);
  const hoverCloseTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => () => {
    if (hoverCloseTimerRef.current) clearTimeout(hoverCloseTimerRef.current);
  }, []);

  // Sync prop value
  useEffect(() => {
    if (value) {
      setTextValue(value);
    }
  }, [value]);

  // Handle click outside to close popover
  useEffect(() => {
    if (!isOpen) return;
    const handleClickOutside = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setIsOpen(false);
    };
    document.addEventListener('mousedown', handleClickOutside);
    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen]);

  // Scroll active hour/minute into view when opening
  useEffect(() => {
    if (isOpen) {
      const parts = (value || textValue || '09:00').split(':');
      const curHour = parts[0] || '09';
      const curMin = parts[1] || '00';

      const timer = setTimeout(() => {
        if (hoursColRef.current) {
          const activeHourEl = hoursColRef.current.querySelector(`[data-hour="${curHour}"]`) as HTMLElement;
          if (activeHourEl) {
            hoursColRef.current.scrollTop = activeHourEl.offsetTop - hoursColRef.current.offsetTop - 40;
          }
        }
        if (minutesColRef.current) {
          const activeMinEl = minutesColRef.current.querySelector(`[data-min="${curMin}"]`) as HTMLElement;
          if (activeMinEl) {
            minutesColRef.current.scrollTop = activeMinEl.offsetTop - minutesColRef.current.offsetTop - 40;
          }
        }
      }, 30);
      return () => clearTimeout(timer);
    }
  }, [isOpen, value, textValue]);

  const currentHour = (textValue || '09:00').split(':')[0] || '09';
  const currentMinute = (textValue || '09:00').split(':')[1] || '00';

  const handleSelectHour = (h: string) => {
    const nextVal = `${h}:${currentMinute}`;
    setTextValue(nextVal);
    onChange(nextVal);
  };

  const handleSelectMinute = (m: string) => {
    const nextVal = `${currentHour}:${m}`;
    setTextValue(nextVal);
    onChange(nextVal);
    setIsOpen(false);
  };

  const handleTextChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    let raw = e.target.value.replace(/[^0-9:]/g, '');
    if (raw.length > 5) raw = raw.slice(0, 5);
    setTextValue(raw);

    // Auto-colon if user types 2 digits
    if (raw.length === 2 && !raw.includes(':') && e.target.value.length > textValue.length) {
      raw = raw + ':';
      setTextValue(raw);
    }

    // If matches complete HH:mm
    if (/^([01]\d|2[0-3]):[0-5]\d$/.test(raw)) {
      onChange(raw);
    }
  };

  const handleBlur = () => {
    // Sanitize on blur
    const raw = textValue.trim();
    if (!raw) {
      setTextValue(value || '09:00');
      return;
    }

    if (/^([01]\d|2[0-3]):[0-5]\d$/.test(raw)) {
      onChange(raw);
      return;
    }

    // Try parsing single number e.g. "9" -> "09:00", "14" -> "14:00"
    const num = parseInt(raw.replace(':', ''), 10);
    if (!isNaN(num)) {
      if (raw.length <= 2 && num >= 0 && num <= 23) {
        const formatted = `${String(num).padStart(2, '0')}:00`;
        setTextValue(formatted);
        onChange(formatted);
        return;
      } else if (raw.length === 3 || raw.length === 4) {
        const digits = raw.replace(':', '').padStart(4, '0');
        const h = Math.min(23, parseInt(digits.slice(0, 2), 10));
        const m = Math.min(59, parseInt(digits.slice(2, 4), 10));
        const formatted = `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
        setTextValue(formatted);
        onChange(formatted);
        return;
      }
    }

    // Fallback to previous valid value
    setTextValue(value || '09:00');
  };

  return (
    <div
      ref={containerRef}
      className={`time24-input-container ${className}`}
      onMouseEnter={() => {
        if (hoverCloseTimerRef.current) clearTimeout(hoverCloseTimerRef.current);
        if (!disabled) setIsOpen(true);
      }}
      onMouseLeave={() => {
        if (hoverCloseTimerRef.current) clearTimeout(hoverCloseTimerRef.current);
        hoverCloseTimerRef.current = setTimeout(() => setIsOpen(false), 220);
      }}
      style={{
        position: 'relative',
        display: 'inline-block',
        width: '100%',
        boxSizing: 'border-box',
      }}
    >
      <div style={{ position: 'relative', width: '100%', display: 'flex', alignItems: 'center' }}>
        <input
          type="text"
          id={id}
          name={name}
          disabled={disabled}
          value={textValue}
          onChange={handleTextChange}
          onBlur={handleBlur}
          onFocus={() => setIsOpen(true)}
          placeholder="HH:mm"
          maxLength={5}
          autoComplete="off"
          style={{
            width: '100%',
            padding: '6px 28px 6px 10px',
            border: '1px solid var(--border)',
            borderRadius: 8,
            fontSize: '0.84rem',
            fontWeight: 600,
            background: 'var(--surface)',
            color: 'var(--text)',
            outline: 'none',
            boxSizing: 'border-box',
            fontFamily: 'inherit',
            letterSpacing: '0.5px',
            cursor: 'pointer',
            ...style,
          }}
        />
        <button
          type="button"
          tabIndex={-1}
          disabled={disabled}
          onClick={() => setIsOpen(true)}
          style={{
            position: 'absolute',
            right: 7,
            top: '50%',
            transform: 'translateY(-50%)',
            background: 'none',
            border: 'none',
            padding: 3,
            cursor: 'pointer',
            color: 'var(--text-muted)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            borderRadius: 4,
            transition: 'color 0.15s ease, transform 0.15s ease',
          }}
          onMouseEnter={(e) => {
            e.currentTarget.style.color = 'var(--primary, #6C5CE7)';
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.color = 'var(--text-muted)';
          }}
          title="Chọn giờ"
        >
          <Clock size={15} />
        </button>
      </div>

      {/* 24-Hour Picker Dropdown Popover */}
      {shouldRender && !disabled && (
        <div
          className="time24-dropdown-popover"
          style={{
            position: 'absolute',
            top: 'calc(100% + 5px)',
            left: align === 'right' ? 'auto' : 0,
            right: align === 'right' ? 0 : 'auto',
            width: 215,
            background: 'var(--surface, #FFFFFF)',
            border: '1px solid var(--border)',
            borderRadius: 12,
            boxShadow: '0 12px 28px -4px rgba(0, 0, 0, 0.35), 0 6px 12px rgba(0, 0, 0, 0.15)',
            zIndex: 999999,
            padding: 8,
            boxSizing: 'border-box',
            animation: isClosing
              ? 'modalSlideDown 0.15s cubic-bezier(0.4, 0, 1, 1) forwards'
              : 'modalSlideUp 0.15s cubic-bezier(0.16, 1, 0.3, 1)',
            pointerEvents: isClosing ? 'none' : 'auto',
          }}
        >
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              padding: '2px 8px 6px 8px',
              borderBottom: '1px solid var(--border-light)',
              marginBottom: 6,
              fontSize: '0.74rem',
              fontWeight: 700,
              color: 'var(--text-muted)',
            }}
          >
            <span>GIỜ</span>
            <span>PHÚT</span>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 6, height: 165 }}>
            {/* Hours Column (00 giờ to 23 giờ) */}
            <div
              ref={hoursColRef}
              style={{
                overflowY: 'auto',
                display: 'flex',
                flexDirection: 'column',
                gap: 2,
                paddingRight: 2,
              }}
            >
              {HOURS.map((h) => {
                const isSelected = h === currentHour;
                return (
                  <button
                    key={h}
                    type="button"
                    data-hour={h}
                    onClick={() => handleSelectHour(h)}
                    style={{
                      padding: '5px 6px',
                      borderRadius: 6,
                      border: 'none',
                      background: isSelected ? 'var(--primary, #6C5CE7)' : 'transparent',
                      color: isSelected ? 'var(--on-primary)' : 'var(--text)',
                      fontSize: '0.78rem',
                      fontWeight: isSelected ? 700 : 500,
                      cursor: 'pointer',
                      textAlign: 'center',
                      transition: 'all 0.12s ease',
                      whiteSpace: 'nowrap',
                    }}
                    onMouseEnter={(e) => {
                      if (!isSelected) {
                        e.currentTarget.style.background = 'var(--border-light)';
                      }
                    }}
                    onMouseLeave={(e) => {
                      if (!isSelected) {
                        e.currentTarget.style.background = 'transparent';
                      }
                    }}
                  >
                    {h} giờ
                  </button>
                );
              })}
            </div>

            {/* Minutes Column (00 phút, 05 phút, ..., 55 phút) */}
            <div
              ref={minutesColRef}
              style={{
                overflowY: 'auto',
                display: 'flex',
                flexDirection: 'column',
                gap: 2,
                paddingRight: 2,
              }}
            >
              {MINUTES.map((m) => {
                const isSelected = m === currentMinute;
                return (
                  <button
                    key={m}
                    type="button"
                    data-min={m}
                    onClick={() => handleSelectMinute(m)}
                    style={{
                      padding: '5px 6px',
                      borderRadius: 6,
                      border: 'none',
                      background: isSelected ? 'var(--primary, #6C5CE7)' : 'transparent',
                      color: isSelected ? 'var(--on-primary)' : 'var(--text)',
                      fontSize: '0.78rem',
                      fontWeight: isSelected ? 700 : 500,
                      cursor: 'pointer',
                      textAlign: 'center',
                      transition: 'all 0.12s ease',
                      whiteSpace: 'nowrap',
                    }}
                    onMouseEnter={(e) => {
                      if (!isSelected) {
                        e.currentTarget.style.background = 'var(--border-light)';
                      }
                    }}
                    onMouseLeave={(e) => {
                      if (!isSelected) {
                        e.currentTarget.style.background = 'transparent';
                      }
                    }}
                  >
                    {m} phút
                  </button>
                );
              })}
            </div>
          </div>

          <div
            style={{
              borderTop: '1px solid var(--border-light)',
              marginTop: 6,
              paddingTop: 6,
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
            }}
          >
            <span style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>
              Đang chọn: <strong style={{ color: 'var(--primary, #6C5CE7)' }}>{currentHour}:{currentMinute}</strong>
            </span>
            <button
              type="button"
              onClick={() => setIsOpen(false)}
              style={{
                padding: '3px 8px',
                borderRadius: 4,
                border: 'none',
                background: 'var(--border-light)',
                color: 'var(--text)',
                fontSize: '0.72rem',
                fontWeight: 600,
                cursor: 'pointer',
              }}
            >
              Xong
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
