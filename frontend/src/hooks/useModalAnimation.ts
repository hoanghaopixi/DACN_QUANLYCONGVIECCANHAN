import { useState, useEffect, useRef } from 'react';

/**
 * Hook điều khiển chu trình sống của Modal:
 * - Khi mở (isOpen: true): Ngay lập tức mount DOM (shouldRender = true) và kích hoạt animation in (isClosing = false).
 * - Khi đóng (isOpen: false): Giữ DOM mounted trong durationMs (mặc định 200ms) với cờ isClosing = true để chạy mượt mà animation out (slideDown / fadeOut) rồi mới unmount khỏi DOM.
 */
export function useModalAnimation(isOpen: boolean, durationMs: number = 200) {
  const [shouldRender, setShouldRender] = useState(isOpen);
  const [isClosing, setIsClosing] = useState(false);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    if (timerRef.current) {
      clearTimeout(timerRef.current);
      timerRef.current = null;
    }

    if (isOpen) {
      setShouldRender(true);
      setIsClosing(false);
    } else if (shouldRender) {
      setIsClosing(true);
      timerRef.current = setTimeout(() => {
        setShouldRender(false);
        setIsClosing(false);
        timerRef.current = null;
      }, durationMs);
    }

    return () => {
      if (timerRef.current) {
        clearTimeout(timerRef.current);
        timerRef.current = null;
      }
    };
  }, [isOpen, shouldRender, durationMs]);

  return { shouldRender, isClosing };
}
