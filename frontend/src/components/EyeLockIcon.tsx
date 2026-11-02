interface EyeLockIconProps {
  locked?: boolean;
  size?: number;
  className?: string;
  color?: string;
  strokeWidth?: number;
}

/**
 * Single unified icon combining Padlock and Eye.
 * - When locked: Shackle is closed, lock body contains eye with a protective slash.
 * - When unlocked: Shackle is open, lock body contains an open eye.
 */
export default function EyeLockIcon({
  locked = true,
  size = 15,
  className = '',
  color = 'currentColor',
  strokeWidth = 2,
}: EyeLockIconProps) {
  if (locked) {
    return (
      <svg
        width={size}
        height={size}
        viewBox="0 0 24 24"
        fill="none"
        stroke={color}
        strokeWidth={strokeWidth}
        strokeLinecap="round"
        strokeLinejoin="round"
        className={className}
        style={{ display: 'inline-block', verticalAlign: 'middle', flexShrink: 0 }}
      >
        {/* Shackle closed */}
        <path d="M7 10V7a5 5 0 0 1 10 0v3" />
        {/* Lock body */}
        <rect x="3.5" y="10" width="17" height="11.5" rx="2.5" />
        {/* Eye contour inside lock body */}
        <path d="M6.8 15.8c1.1-1.8 3-2.9 5.2-2.9s4.1 1.1 5.2 2.9c-1.1 1.8-3 2.9-5.2 2.9s-4.1-1.1-5.2-2.9z" strokeWidth={strokeWidth * 0.8} />
        {/* Pupil */}
        <circle cx="12" cy="15.8" r="1.3" fill={color} stroke="none" />
        {/* Protective slash through eye */}
        <line x1="5.5" y1="12.5" x2="18.5" y2="19" strokeWidth={strokeWidth * 0.9} />
      </svg>
    );
  }

  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke={color}
      strokeWidth={strokeWidth}
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
      style={{ display: 'inline-block', verticalAlign: 'middle', flexShrink: 0 }}
    >
      {/* Shackle open */}
      <path d="M7 10V7a5 5 0 0 1 9.5-2.2" />
      {/* Lock body */}
      <rect x="3.5" y="10" width="17" height="11.5" rx="2.5" />
      {/* Eye open contour inside lock body */}
      <path d="M6.8 15.8c1.1-1.8 3-2.9 5.2-2.9s4.1 1.1 5.2 2.9c-1.1 1.8-3 2.9-5.2 2.9s-4.1-1.1-5.2-2.9z" strokeWidth={strokeWidth * 0.8} />
      {/* Pupil */}
      <circle cx="12" cy="15.8" r="1.4" fill={color} stroke="none" />
    </svg>
  );
}
