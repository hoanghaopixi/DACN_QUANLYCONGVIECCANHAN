import React from 'react';

export type CalloutPlacement = 'top' | 'bottom' | 'left' | 'right';
export type CalloutArrowAlign = 'start' | 'center' | 'end';

interface PrivacyCalloutFrameProps {
  width: number;
  height: number;
  placement?: CalloutPlacement;
  arrowAlign?: CalloutArrowAlign;
  arrowOffset?: number;
  strokeWidth?: number;
  borderRadius?: number;
  beakWidth?: number;
  beakHeight?: number;
  fill?: string;
  stroke?: string;
  className?: string;
  style?: React.CSSProperties;
}

export default function PrivacyCalloutFrame({
  width,
  height,
  placement = 'top',
  arrowAlign = 'center',
  arrowOffset,
  strokeWidth = 2,
  borderRadius = 14,
  beakWidth = 20,
  beakHeight = 11,
  fill = 'var(--privacy-callout-bg, #141B2D)',
  stroke = 'var(--privacy-callout-stroke, #64748B)',
  className = '',
  style = {},
}: PrivacyCalloutFrameProps) {
  const r = Math.min(borderRadius, Math.floor(Math.min(width, height) / 3));
  const beakW = beakWidth;
  const beakH = beakHeight;
  const pad = strokeWidth / 2 + 0.5;

  let svgPath = '';

  if (placement === 'bottom') {
    // Bubble is BELOW target => arrow is at the TOP pointing UP
    const baseY = beakH + pad;
    const bottomY = height - pad;
    const leftX = pad;
    const rightX = width - pad;

    const defaultCenterX =
      arrowAlign === 'start'
        ? Math.max(r + beakW / 2 + 4, 34)
        : arrowAlign === 'end'
        ? Math.min(width - r - beakW / 2 - 4, width - 34)
        : width / 2;

    const centerX =
      typeof arrowOffset === 'number'
        ? Math.max(r + beakW / 2 + 4, Math.min(width - r - beakW / 2 - 4, arrowOffset))
        : defaultCenterX;

    const beakLeft = centerX - beakW / 2;
    const beakRight = centerX + beakW / 2;
    const beakTipX = centerX;
    const beakTipY = pad;

    svgPath = `
      M ${beakRight} ${baseY}
      L ${rightX - r} ${baseY}
      A ${r} ${r} 0 0 1 ${rightX} ${baseY + r}
      L ${rightX} ${bottomY - r}
      A ${r} ${r} 0 0 1 ${rightX - r} ${bottomY}
      L ${leftX + r} ${bottomY}
      A ${r} ${r} 0 0 1 ${leftX} ${bottomY - r}
      L ${leftX} ${baseY + r}
      A ${r} ${r} 0 0 1 ${leftX + r} ${baseY}
      L ${beakLeft} ${baseY}
      L ${beakTipX} ${beakTipY}
      L ${beakRight} ${baseY}
      Z
    `;
  } else if (placement === 'left') {
    // Bubble is to the LEFT of target => arrow is at the RIGHT pointing RIGHT
    const topY = pad;
    const bottomY = height - pad;
    const leftX = pad;
    const baseX = width - beakH - pad;

    const centerY =
      arrowAlign === 'start'
        ? Math.max(r + beakW / 2 + 4, 26)
        : arrowAlign === 'end'
        ? Math.min(height - r - beakW / 2 - 4, height - 26)
        : height / 2;

    const beakTop = centerY - beakW / 2;
    const beakBottom = centerY + beakW / 2;
    const beakTipX = width - pad;
    const beakTipY = centerY;

    svgPath = `
      M ${leftX + r} ${topY}
      L ${baseX - r} ${topY}
      A ${r} ${r} 0 0 1 ${baseX} ${topY + r}
      L ${baseX} ${beakTop}
      L ${beakTipX} ${beakTipY}
      L ${baseX} ${beakBottom}
      L ${baseX} ${bottomY - r}
      A ${r} ${r} 0 0 1 ${baseX - r} ${bottomY}
      L ${leftX + r} ${bottomY}
      A ${r} ${r} 0 0 1 ${leftX} ${bottomY - r}
      L ${leftX} ${topY + r}
      A ${r} ${r} 0 0 1 ${leftX + r} ${topY}
      Z
    `;
  } else if (placement === 'right') {
    // Bubble is to the RIGHT of target => arrow is at the LEFT pointing LEFT
    const topY = pad;
    const bottomY = height - pad;
    const baseX = beakH + pad;
    const rightX = width - pad;

    const centerY =
      arrowAlign === 'start'
        ? Math.max(r + beakW / 2 + 4, 26)
        : arrowAlign === 'end'
        ? Math.min(height - r - beakW / 2 - 4, height - 26)
        : height / 2;

    const beakTop = centerY - beakW / 2;
    const beakBottom = centerY + beakW / 2;
    const beakTipX = pad;
    const beakTipY = centerY;

    svgPath = `
      M ${baseX + r} ${topY}
      L ${rightX - r} ${topY}
      A ${r} ${r} 0 0 1 ${rightX} ${topY + r}
      L ${rightX} ${bottomY - r}
      A ${r} ${r} 0 0 1 ${rightX - r} ${bottomY}
      L ${baseX + r} ${bottomY}
      A ${r} ${r} 0 0 1 ${baseX} ${bottomY - r}
      L ${baseX} ${beakBottom}
      L ${beakTipX} ${beakTipY}
      L ${baseX} ${beakTop}
      L ${baseX} ${topY + r}
      A ${r} ${r} 0 0 1 ${baseX + r} ${topY}
      Z
    `;
  } else {
    // Default: placement === 'top' (Bubble is ABOVE target => arrow is at the BOTTOM pointing DOWN)
    const topY = pad;
    const baseY = height - beakH - pad;
    const leftX = pad;
    const rightX = width - pad;

    const defaultCenterX =
      arrowAlign === 'start'
        ? Math.max(r + beakW / 2 + 4, 34)
        : arrowAlign === 'end'
        ? Math.min(width - r - beakW / 2 - 4, width - 34)
        : width / 2;

    const centerX =
      typeof arrowOffset === 'number'
        ? Math.max(r + beakW / 2 + 4, Math.min(width - r - beakW / 2 - 4, arrowOffset))
        : defaultCenterX;

    const beakLeft = centerX - beakW / 2;
    const beakRight = centerX + beakW / 2;
    const beakTipX = centerX;
    const beakTipY = height - pad;

    svgPath = `
      M ${leftX + r} ${topY}
      L ${rightX - r} ${topY}
      A ${r} ${r} 0 0 1 ${rightX} ${topY + r}
      L ${rightX} ${baseY - r}
      A ${r} ${r} 0 0 1 ${rightX - r} ${baseY}
      L ${beakRight} ${baseY}
      L ${beakTipX} ${beakTipY}
      L ${beakLeft} ${baseY}
      L ${leftX + r} ${baseY}
      A ${r} ${r} 0 0 1 ${leftX} ${baseY - r}
      L ${leftX} ${topY + r}
      A ${r} ${r} 0 0 1 ${leftX + r} ${topY}
      Z
    `;
  }

  return (
    <svg
      className={`privacy-callout-svg ${className}`}
      width={width}
      height={height}
      viewBox={`0 0 ${width} ${height}`}
      style={{
        position: 'absolute',
        top: 0,
        left: 0,
        width: '100%',
        height: '100%',
        pointerEvents: 'none',
        zIndex: 1,
        ...style,
      }}
    >
      <path
        d={svgPath}
        fill={fill}
        stroke={stroke}
        strokeWidth={strokeWidth}
        strokeLinejoin="round"
        strokeLinecap="round"
      />
    </svg>
  );
}
