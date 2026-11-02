import React from 'react';
import { createPortal } from 'react-dom';
import EyeLockIcon from './EyeLockIcon';
import PrivacyCalloutFrame, { CalloutPlacement, CalloutArrowAlign } from './PrivacyCalloutFrame';

interface PrivacyCalloutTooltipProps {
  placement?: CalloutPlacement;
  arrowAlign?: CalloutArrowAlign;
  targetRect?: DOMRect | null;
  onClick?: (e: React.MouseEvent) => void;
  style?: React.CSSProperties;
  title?: string;
}

export default function PrivacyCalloutTooltip({
  placement = 'top',
  arrowAlign = 'center',
  targetRect,
  onClick,
  style = {},
  title = 'Vui lòng nhập mật khẩu để mở khóa lịch trình này!',
}: PrivacyCalloutTooltipProps) {
  const width = 295;
  const height = 72;

  let computedPlacement = placement;
  let computedArrowOffset: number | undefined = undefined;

  let posStyle: React.CSSProperties = {
    position: 'absolute',
    width: `${width}px`,
    height: `${height}px`,
    zIndex: 99999,
    cursor: 'pointer',
    filter: 'drop-shadow(0 12px 28px rgba(0, 0, 0, 0.45))',
    animation: 'privacyModalScaleUp 0.18s cubic-bezier(0.16, 1.25, 0.3, 1)',
    pointerEvents: 'none',
  };

  if (targetRect) {
    // Dynamic viewport-based positioning via Portal (không bao giờ bị scroll container hay header che khuất)
    const cardCenterX = targetRect.left + targetRect.width / 2;

    // Nếu khoảng trống phía trên card < 200px -> đặt bên dưới ('bottom'), mũi tên chĩa lên
    // Nếu phía trên đủ chỗ -> đặt bên trên ('top'), mũi tên chĩa xuống
    computedPlacement = targetRect.top < 200 ? 'bottom' : 'top';

    let top = 0;
    if (computedPlacement === 'bottom') {
      top = targetRect.bottom + 8;
    } else {
      top = targetRect.top - height - 8;
    }

    // Căn giữa theo card nhưng giữ an toàn trong màn hình (cách mép ít nhất 12px)
    let left = cardCenterX - width / 2;
    left = Math.max(12, Math.min(window.innerWidth - width - 12, left));

    // Mũi tên trỏ chính xác vào tâm card
    computedArrowOffset = cardCenterX - left;

    posStyle = {
      ...posStyle,
      position: 'fixed',
      top: `${top}px`,
      left: `${left}px`,
    };
  } else {
    // Relative fallback
    if (placement === 'bottom') {
      posStyle.top = 'calc(100% + 8px)';
    } else if (placement === 'left') {
      posStyle.right = 'calc(100% + 8px)';
      posStyle.top = '50%';
      posStyle.transform = 'translateY(-50%)';
    } else if (placement === 'right') {
      posStyle.left = 'calc(100% + 8px)';
      posStyle.top = '50%';
      posStyle.transform = 'translateY(-50%)';
    } else {
      posStyle.bottom = 'calc(100% + 8px)';
    }

    if (placement === 'top' || placement === 'bottom') {
      if (arrowAlign === 'start') {
        posStyle.left = '0px';
      } else if (arrowAlign === 'end') {
        posStyle.right = '0px';
      } else {
        posStyle.left = '50%';
        posStyle.transform = 'translateX(-50%)';
      }
    }
  }

  // Padding adjustment depending on whether beak is at the top or bottom
  const contentPadding =
    computedPlacement === 'bottom'
      ? '16px 14px 8px 14px' // arrow is at top => more top padding
      : '8px 14px 16px 14px'; // arrow is at bottom => more bottom padding

  const content = (
    <div
      className="privacy-callout-tooltip"
      onClick={onClick}
      style={{
        ...posStyle,
        ...style,
      }}
      title={title}
    >
      <PrivacyCalloutFrame
        width={width}
        height={height}
        placement={computedPlacement}
        arrowAlign={arrowAlign}
        arrowOffset={computedArrowOffset}
        fill="#141B2D"
        stroke="#6C5CE7"
        strokeWidth={2}
      />

      <div
        style={{
          position: 'relative',
          zIndex: 2,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          textAlign: 'center',
          height: '100%',
          boxSizing: 'border-box',
          padding: contentPadding,
          userSelect: 'none',
          gap: 3,
        }}
      >
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: 6,
            fontSize: '0.78rem',
            fontWeight: 700,
            color: '#FFFFFF',
            lineHeight: 1.35,
          }}
        >
          <EyeLockIcon locked={true} size={15} color="#EF4444" />
          <span>{title}</span>
        </div>

        <span
          style={{
            fontSize: '0.7rem',
            color: '#94A3B8',
            fontWeight: 500,
            opacity: 0.9,
          }}
        >
          (Nhấp để nhập mật khẩu)
        </span>
      </div>
    </div>
  );

  if (targetRect && typeof document !== 'undefined') {
    return createPortal(content, document.body);
  }

  return content;
}
