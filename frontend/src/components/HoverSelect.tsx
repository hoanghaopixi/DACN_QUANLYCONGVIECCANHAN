import { Children, isValidElement, ReactElement, ReactNode, useEffect, useId, useLayoutEffect, useRef, useState } from 'react';
import { ChevronDown, Check } from 'lucide-react';
import { createPortal } from 'react-dom';

interface SelectOptionProps {
  value?: string | number;
  children?: ReactNode;
  disabled?: boolean;
}

interface HoverSelectProps {
  value: string | number;
  onChange: (event: { target: { value: string } }) => void;
  children: ReactNode;
  className?: string;
  style?: React.CSSProperties;
  disabled?: boolean;
  id?: string;
  title?: string;
  'aria-label'?: string;
}

export default function HoverSelect({
  value,
  onChange,
  children,
  className = '',
  style,
  disabled = false,
  id,
  title,
  'aria-label': ariaLabel,
}: HoverSelectProps) {
  const menuId = useId();
  const rootRef = useRef<HTMLDivElement>(null);
  const menuRef = useRef<HTMLDivElement>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const closeTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const [isOpen, setIsOpen] = useState(false);
  const [shouldRender, setShouldRender] = useState(false);
  const [isExiting, setIsExiting] = useState(false);
  const [menuPlacement, setMenuPlacement] = useState<'top' | 'bottom'>('bottom');
  const [menuPosition, setMenuPosition] = useState<React.CSSProperties | null>(null);

  const options = Children.toArray(children)
    .filter((child): child is ReactElement<SelectOptionProps> => isValidElement<SelectOptionProps>(child) && child.type === 'option')
    .map((option) => ({
      value: String(option.props.value ?? option.props.children ?? ''),
      label: option.props.children,
      disabled: option.props.disabled ?? false,
    }));
  const selectedOption = options.find((option) => option.value === String(value));

  useEffect(() => {
    if (isOpen) {
      setShouldRender(true);
      setIsExiting(false);
      return;
    }
    if (!shouldRender) return;
    setIsExiting(true);
    const timer = window.setTimeout(() => {
      setShouldRender(false);
      setIsExiting(false);
    }, 190);
    return () => window.clearTimeout(timer);
  }, [isOpen, shouldRender]);

  useEffect(() => () => {
    if (closeTimerRef.current) clearTimeout(closeTimerRef.current);
  }, []);

  useEffect(() => {
    if (!isOpen) return;
    const closeOutside = (event: MouseEvent) => {
      const target = event.target as Node;
      if (!rootRef.current?.contains(target) && !menuRef.current?.contains(target)) setIsOpen(false);
    };
    document.addEventListener('mousedown', closeOutside);
    return () => document.removeEventListener('mousedown', closeOutside);
  }, [isOpen]);

  useLayoutEffect(() => {
    if (!shouldRender) {
      setMenuPosition(null);
      return;
    }

    const updateMenuPosition = () => {
      const trigger = triggerRef.current;
      const menu = menuRef.current;
      if (!trigger || !menu) return;

      const rect = trigger.getBoundingClientRect();
      const viewportWidth = document.documentElement.clientWidth;
      const viewportHeight = window.innerHeight;
      const edge = 8;
      let lowerBoundary = viewportHeight - edge;
      const stickyFooter = document.querySelector<HTMLElement>('.settings-bottom-bar');
      if (stickyFooter) {
        const footerRect = stickyFooter.getBoundingClientRect();
        if (footerRect.top > 0 && footerRect.top < viewportHeight) {
          lowerBoundary = Math.min(lowerBoundary, footerRect.top - edge);
        }
      }

      const spaceBelow = Math.max(0, lowerBoundary - rect.bottom - 6);
      const spaceAbove = Math.max(0, rect.top - edge - 6);
      const fullMenuHeight = Math.min(menu.scrollHeight, 320);
      // Prefer below when it can show a useful portion of the menu; otherwise
      // use whichever side gives the list more room and let the list scroll.
      const usefulMenuHeight = Math.min(fullMenuHeight, 200);
      const placement: 'top' | 'bottom' = spaceBelow >= usefulMenuHeight || spaceBelow >= spaceAbove
        ? 'bottom'
        : 'top';
      const availableSpace = placement === 'bottom' ? spaceBelow : spaceAbove;
      const maxHeight = Math.max(48, Math.min(320, availableSpace));
      const height = Math.min(menu.scrollHeight, maxHeight);
      const width = Math.max(1, Math.min(rect.width, viewportWidth - edge * 2));
      const left = Math.max(edge, Math.min(rect.left, viewportWidth - width - edge));
      const top = placement === 'bottom'
        ? Math.max(edge, Math.min(rect.bottom + 6, lowerBoundary - height))
        : Math.max(edge, rect.top - height - 6);

      setMenuPlacement(placement);
      setMenuPosition({ position: 'fixed', top, left, width, maxHeight, zIndex: 11000 });
    };

    updateMenuPosition();
    window.addEventListener('resize', updateMenuPosition);
    window.addEventListener('scroll', updateMenuPosition, true);
    const observer = new ResizeObserver(updateMenuPosition);
    if (triggerRef.current) observer.observe(triggerRef.current);
    if (menuRef.current) observer.observe(menuRef.current);
    const stickyFooter = document.querySelector<HTMLElement>('.settings-bottom-bar');
    if (stickyFooter) observer.observe(stickyFooter);
    return () => {
      window.removeEventListener('resize', updateMenuPosition);
      window.removeEventListener('scroll', updateMenuPosition, true);
      observer.disconnect();
    };
  }, [shouldRender, options.length]);

  const openFromHover = () => {
    if (closeTimerRef.current) clearTimeout(closeTimerRef.current);
    if (!disabled) setIsOpen(true);
  };

  const closeAfterLeave = () => {
    if (closeTimerRef.current) clearTimeout(closeTimerRef.current);
    closeTimerRef.current = setTimeout(() => setIsOpen(false), 220);
  };

  const focusOption = (index: number) => {
    requestAnimationFrame(() => {
      const items = menuRef.current?.querySelectorAll<HTMLButtonElement>('[role="option"]:not(:disabled)');
      if (!items?.length) return;
      items[(index + items.length) % items.length]?.focus();
    });
  };

  const handleTriggerKeyDown = (event: React.KeyboardEvent<HTMLButtonElement>) => {
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault();
      setIsOpen(true);
      const selectedIndex = options.findIndex((option) => option.value === String(value));
      focusOption(selectedIndex >= 0 ? selectedIndex : 0);
    } else if (event.key === 'Escape') {
      setIsOpen(false);
    }
  };

  const handleOptionKeyDown = (event: React.KeyboardEvent<HTMLButtonElement>, index: number) => {
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault();
      focusOption(index + (event.key === 'ArrowDown' ? 1 : -1));
    } else if (event.key === 'Escape') {
      event.preventDefault();
      setIsOpen(false);
      triggerRef.current?.focus();
    }
  };

  const isTimeSelect = className.split(/\s+/).includes('time-select');
  const hasExplicitWidth = style?.width !== undefined || className.split(/\s+/).includes('form-select');
  const shellStyle: React.CSSProperties = {
    position: 'relative',
    display: hasExplicitWidth ? 'block' : 'inline-flex',
    flex: isTimeSelect ? '1 1 0%' : '0 1 auto',
    width: className.split(/\s+/).includes('form-select') ? '100%' : style?.width,
    minWidth: 0,
  };

  return (
    <div
      ref={rootRef}
      className={`hover-select ${isTimeSelect ? 'hover-select-time' : ''}`}
      style={shellStyle}
      onMouseEnter={openFromHover}
      onMouseLeave={closeAfterLeave}
    >
      <button
        ref={triggerRef}
        id={id}
        type="button"
        className={`hover-select-trigger${isTimeSelect ? ' is-time-select' : ''} ${className}`.trim()}
        style={{ ...style, width: '100%', display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8, minWidth: 0 }}
        disabled={disabled}
        title={title}
        aria-label={ariaLabel}
        aria-haspopup="listbox"
        aria-expanded={isOpen}
        aria-controls={menuId}
        onFocus={openFromHover}
        onClick={() => setIsOpen(true)}
        onKeyDown={handleTriggerKeyDown}
      >
        <span className="hover-select-value">{selectedOption?.label ?? String(value)}</span>
        <ChevronDown className={`hover-select-chevron${isOpen ? ' is-open' : ''}`} size={14} />
      </button>

      {shouldRender && typeof document !== 'undefined' && createPortal(
        <div
          ref={menuRef}
          id={menuId}
          className={`hover-select-menu ${menuPlacement === 'top' ? 'opens-up' : 'opens-down'}${isExiting ? ' is-exiting' : ''}`}
          style={menuPosition || { position: 'fixed', top: 0, left: 0, visibility: 'hidden' }}
          role="listbox"
          aria-label={ariaLabel}
          onMouseEnter={() => {
            if (closeTimerRef.current) clearTimeout(closeTimerRef.current);
          }}
          onMouseLeave={closeAfterLeave}
        >
          {options.map((option, index) => {
            const isSelected = option.value === String(value);
            return (
              <button
                key={`${option.value}-${index}`}
                type="button"
                className={`hover-select-option${isSelected ? ' is-selected' : ''}`}
                role="option"
                aria-selected={isSelected}
                disabled={option.disabled}
                onMouseDown={(event) => event.preventDefault()}
                onKeyDown={(event) => handleOptionKeyDown(event, index)}
                onClick={() => {
                  onChange({ target: { value: option.value } });
                  setIsOpen(false);
                }}
              >
                <span>{option.label}</span>
                {isSelected && <Check size={14} aria-hidden="true" />}
              </button>
            );
          })}
        </div>,
        document.body,
      )}
    </div>
  );
}
