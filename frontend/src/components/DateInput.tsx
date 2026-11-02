import { useEffect, useRef, useState } from 'react';
import { Calendar } from 'lucide-react';
import { formatDateDDMMYYYY, parseDateDDMMYYYY } from '../utils/dateTimeFormat';
import './DateInput.css';

interface DateInputProps {
  value: string;
  onChange: (value: string) => void;
  id?: string;
  className?: string;
  required?: boolean;
  disabled?: boolean;
  maxDate?: string;
  ariaLabel?: string;
}

export default function DateInput({
  value,
  onChange,
  id,
  className = '',
  required = false,
  disabled = false,
  maxDate,
  ariaLabel = 'Ngày (DD/MM/YYYY)',
}: DateInputProps) {
  const pickerRef = useRef<HTMLInputElement>(null);
  const [text, setText] = useState(() => formatDateDDMMYYYY(value));

  useEffect(() => {
    setText(formatDateDDMMYYYY(value));
  }, [value]);

  const handleTextChange = (nextText: string) => {
    const isoDate = parseDateDDMMYYYY(nextText);
    if (isoDate && maxDate && isoDate > maxDate) {
      setText(formatDateDDMMYYYY(maxDate));
      onChange(maxDate);
      return;
    }
    setText(nextText);
    if (isoDate) onChange(isoDate);
  };

  const handleBlur = () => {
    const isoDate = parseDateDDMMYYYY(text);
    if (isoDate) {
      const validDate = maxDate && isoDate > maxDate ? maxDate : isoDate;
      setText(formatDateDDMMYYYY(validDate));
      onChange(validDate);
    } else {
      setText(formatDateDDMMYYYY(value));
    }
  };

  const openPicker = () => {
    const picker = pickerRef.current;
    if (!picker || disabled) return;
    try {
      picker.showPicker();
    } catch {
      picker.focus();
      picker.click();
    }
  };

  return (
    <div className={`date-input-control ${className}`}>
      <input
        id={id}
        type="text"
        inputMode="numeric"
        autoComplete="off"
        aria-label={ariaLabel}
        aria-describedby={`${id || 'date'}-format-hint`}
        pattern="\d{2}/\d{2}/\d{4}"
        title="Nhập ngày theo định dạng DD/MM/YYYY"
        placeholder="DD/MM/YYYY"
        value={text}
        onChange={(event) => handleTextChange(event.target.value)}
        onBlur={handleBlur}
        required={required}
        disabled={disabled}
        className="date-input-text"
      />
      <button
        type="button"
        className="date-input-calendar-button"
        onClick={openPicker}
        title="Chọn ngày"
        aria-label="Mở lịch chọn ngày"
        disabled={disabled}
      >
        <Calendar size={16} />
      </button>
      <input
        ref={pickerRef}
        type="date"
        value={value}
        max={maxDate}
        onChange={(event) => {
          if (event.target.value) onChange(event.target.value);
        }}
        tabIndex={-1}
        aria-hidden="true"
        className="date-input-native-picker"
      />
      <span id={`${id || 'date'}-format-hint`} className="date-input-sr-only">Định dạng ngày: ngày/tháng/năm</span>
    </div>
  );
}
