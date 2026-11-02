import { Sun, Moon } from 'lucide-react';

interface ThemeToggleProps {
  theme: 'light' | 'dark';
  onToggle: () => void;
}

export default function ThemeToggle({ theme, onToggle }: ThemeToggleProps) {
  const isDark = theme === 'dark';

  return (
    <button
      onClick={onToggle}
      className={`theme-toggle-btn ${isDark ? 'dark' : 'light'}`}
      title={isDark ? 'Chuyển sang Giao diện Sáng' : 'Chuyển sang Giao diện Tối'}
      aria-label="Toggle theme"
    >
      <div className="toggle-track">
        <div className="track-icon track-sun">
          <Sun size={11} strokeWidth={2.5} />
        </div>
        <div className="track-icon track-moon">
          <Moon size={11} strokeWidth={2.5} />
        </div>
        <div className="toggle-thumb">
          {isDark ? (
            <Moon size={10} strokeWidth={2.5} className="thumb-icon" />
          ) : (
            <Sun size={10} strokeWidth={2.5} className="thumb-icon" />
          )}
        </div>
      </div>
    </button>
  );
}
