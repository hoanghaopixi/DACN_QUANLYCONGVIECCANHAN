import { useState, useEffect } from 'react';
import {
  TrendingUp, CheckCircle2, Clock, AlertTriangle,
  BarChart3, RefreshCw, Calendar, ChevronLeft, ChevronRight,
  CheckSquare, ListTodo, Zap, ArrowUpRight
} from 'lucide-react';
import { ProductivityReport } from '../types';
import { productivityService } from '../services/productivityService';
import DateInput from './DateInput';

interface ProductivityDashboardProps {
  onShowToast: (msg: string) => void;
  onNavigateToCalendar: () => void;
}

export default function ProductivityDashboard({
  onShowToast,
  onNavigateToCalendar,
}: ProductivityDashboardProps) {
  // Format today as YYYY-MM-DD in local time
  const getTodayStr = () => {
    const d = new Date();
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  };

  const [selectedDate, setSelectedDate] = useState<string>(getTodayStr());
  const [dateDirection, setDateDirection] = useState<'previous' | 'next'>('next');
  const [report, setReport] = useState<ProductivityReport | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [activeTaskTab, setActiveTaskTab] = useState<'completed' | 'pending'>('completed');

  const changeSelectedDate = (nextDate: string) => {
    if (nextDate > getTodayStr()) return;
    if (nextDate === selectedDate) return;
    setDateDirection(nextDate < selectedDate ? 'previous' : 'next');
    setSelectedDate(nextDate);
  };

  const loadReport = async (dateStr: string) => {
    setIsLoading(true);
    try {
      const data = await productivityService.getDailyReport(dateStr);
      setReport(data);
    } catch (err) {
      console.error('Lỗi khi tải báo cáo năng suất:', err);
      onShowToast('Không thể tải báo cáo năng suất từ máy chủ');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadReport(selectedDate);
  }, [selectedDate]);

  const handlePrevDay = () => {
    const d = new Date(selectedDate);
    d.setDate(d.getDate() - 1);
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    changeSelectedDate(`${year}-${month}-${day}`);
  };

  const handleNextDay = () => {
    if (selectedDate >= getTodayStr()) return;
    const d = new Date(selectedDate);
    d.setDate(d.getDate() + 1);
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    changeSelectedDate(`${year}-${month}-${day}`);
  };

  const handleGoToday = () => {
    changeSelectedDate(getTodayStr());
  };

  // Format date display in Vietnamese: "Thứ Hai, 26 tháng 10, 2026"
  const formatVietnameseDate = (dateStr: string) => {
    try {
      const [year, month, day] = dateStr.split('-').map(Number);
      const d = new Date(year, month - 1, day);
      const days = ['Chủ Nhật', 'Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy'];
      const dayName = days[d.getDay()];
      return `${dayName}, ${String(day).padStart(2, '0')}/${String(month).padStart(2, '0')}/${year}`;
    } catch {
      return dateStr;
    }
  };

  const isToday = selectedDate === getTodayStr();
  const score = report?.productivityScore ?? 0;
  const scoreColor = score >= 80 ? '#10B981' : score >= 60 ? '#6C5CE7' : '#F59E0B';
  const scoreLabel = !report ? 'Chưa có dữ liệu' : score >= 85 ? 'Rất tốt' : score >= 70 ? 'Tốt' : score >= 50 ? 'Ổn định' : 'Cần cải thiện';

  const totalTasks = report?.totalTasks ?? 0;
  const completedTasks = report?.totalCompletedTasks ?? 0;
  const overdueTasks = report?.overdueTaskCount ?? 0;
  const focusMinutes = report?.totalFocusMinutes ?? 0;
  const onTimeRate = report?.onTimeRate ?? 0;

  const completionPct = totalTasks > 0 ? Math.min(100, Math.round((completedTasks / totalTasks) * 100)) : (completedTasks > 0 ? 100 : 0);

  // Category palette map
  const categoryColors: Record<string, string> = {
    'Công việc': '#8B5CF6',
    'Học tập': '#3B82F6',
    'Sức khỏe': '#10B981',
    'Giải trí': '#F59E0B',
    'Chung': '#6C5CE7',
    'Khác': '#64748B',
  };

  return (
    <div className="productivity-dashboard" style={{ padding: '24px 28px 48px', maxWidth: 1280, margin: '0 auto', width: '100%', overflowY: 'auto' }}>
      {/* ── Top Header Bar ── */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          flexWrap: 'wrap',
          gap: 16,
          marginBottom: 22,
        }}
      >
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <h1 style={{ fontSize: '1.45rem', fontWeight: 800, color: 'var(--text)', margin: 0 }}>
              Báo cáo Năng suất
            </h1>
          </div>
          <p style={{ fontSize: '0.86rem', color: 'var(--text-3)', marginTop: 4, marginBottom: 0 }}>
            Theo dõi tiến độ, thời gian tập trung và công việc theo từng ngày.
          </p>
        </div>

        {/* Date Navigator Controls */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              background: 'var(--surface)',
              border: '1px solid var(--border)',
              borderRadius: 10,
              padding: '3px 6px',
            }}
          >
            <button
              type="button"
              onClick={handlePrevDay}
              title="Ngày trước"
              style={{
                background: 'transparent',
                border: 'none',
                color: 'var(--text-2)',
                cursor: 'pointer',
                padding: '5px 7px',
                borderRadius: 6,
                display: 'flex',
                alignItems: 'center',
              }}
            >
              <ChevronLeft size={16} />
            </button>

            <DateInput value={selectedDate} onChange={changeSelectedDate} maxDate={getTodayStr()} id="productivity-date" className="productivity-date-input" ariaLabel="Chọn ngày báo cáo" />

            <button
              type="button"
              onClick={handleNextDay}
              disabled={isToday}
              title="Ngày tiếp theo"
              style={{
                background: 'transparent',
                border: 'none',
                color: isToday ? 'var(--text-muted)' : 'var(--text-2)',
                cursor: isToday ? 'not-allowed' : 'pointer',
                padding: '5px 7px',
                borderRadius: 6,
                display: 'flex',
                alignItems: 'center',
                opacity: isToday ? 0.45 : 1,
              }}
            >
              <ChevronRight size={16} />
            </button>
          </div>

          <div className={`productivity-today-action-slot ${!isToday ? 'is-visible' : ''}`} aria-hidden={isToday}>
            <button
              type="button"
              onClick={handleGoToday}
              disabled={isToday}
              tabIndex={isToday ? -1 : 0}
              className="productivity-today-action"
            >
              Hôm nay
            </button>
          </div>

          <button
            type="button"
            onClick={onNavigateToCalendar}
            className="productivity-header-calendar-button"
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 6,
              height: 42,
              padding: '0 13px',
              boxSizing: 'border-box',
              borderRadius: 10,
              border: '1px solid var(--border)',
              background: 'var(--surface)',
              color: 'var(--text)',
              fontSize: '0.82rem',
              fontWeight: 600,
              cursor: 'pointer',
            }}
          >
            <Calendar size={14} />
            <span>Lịch trình</span>
          </button>

          <button
            type="button"
            onClick={() => loadReport(selectedDate)}
            disabled={isLoading}
            title="Làm mới số liệu"
            className="productivity-header-refresh-button"
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              width: 42,
              height: 42,
              boxSizing: 'border-box',
              borderRadius: 10,
              border: '1px solid var(--border)',
              background: 'var(--surface)',
              color: 'var(--text)',
              cursor: isLoading ? 'not-allowed' : 'pointer',
            }}
          >
            <RefreshCw size={14} className={isLoading ? 'animate-spin' : ''} />
          </button>
        </div>
      </div>

      {/* Date Banner Label */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          background: 'var(--surface)',
          padding: '10px 18px',
          borderRadius: 12,
          border: '1px solid var(--border)',
          marginBottom: 20,
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, minWidth: 0 }}>
          <Calendar size={16} color="var(--primary)" />
          <div key={selectedDate} className={`productivity-date-readout ${dateDirection}`}>
            <span style={{ fontSize: '0.88rem', fontWeight: 700, color: 'var(--text)' }}>
            {formatVietnameseDate(selectedDate)}
            </span>
            {isToday && <span className="productivity-date-today-badge">Hôm nay</span>}
          </div>
        </div>
        <span style={{ fontSize: '0.76rem', color: 'var(--text-3)' }}>
          Thống kê từ lịch công việc và thời gian tập trung đã ghi nhận
        </span>
      </div>

      {/* ── Main Grid: Daily Summary & Productivity Score ── */}
      <div
        className="productivity-overview-grid"
        style={{
          display: 'grid',
          gridTemplateColumns: '1.25fr 0.75fr',
          gap: 20,
          marginBottom: 22,
        }}
      >
        <div className="productivity-summary-card">
          <div className="productivity-summary-heading">
            <div>
              <h3>Tổng quan trong ngày</h3>
              <p key={selectedDate} className={`productivity-date-readout ${dateDirection}`}>{formatVietnameseDate(selectedDate)}</p>
            </div>
            {isToday && <span className="productivity-today-badge">Hôm nay</span>}
          </div>
          <div className="productivity-summary-progress">
            <div className="productivity-summary-progress-copy">
              <strong>{completedTasks} / {totalTasks}</strong>
              <span>công việc hoàn thành</span>
              <b>{completionPct}%</b>
            </div>
            <div className="productivity-summary-progress-track">
              <div style={{ width: `${completionPct}%` }} />
            </div>
          </div>
          <p className="productivity-summary-note">
            {!report && isLoading
              ? 'Đang tải số liệu trong ngày…'
              : totalTasks === 0
                ? 'Chưa có công việc được ghi nhận trong ngày này.'
                : `${Math.max(0, totalTasks - completedTasks)} công việc còn lại trong kế hoạch.`}
          </p>
          {report?.aiFeedback && (
            <div
              style={{
                marginTop: 18,
                padding: '12px 14px',
                borderRadius: 12,
                border: '1px solid var(--border)',
                background: 'var(--primary-bg)',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: 7, color: 'var(--primary)', fontSize: '0.8rem', fontWeight: 750 }}>
                <Zap size={15} />
                <span>Đánh giá trong ngày</span>
              </div>
              <p style={{ margin: '7px 0 0', color: 'var(--text-2)', fontSize: '0.84rem', lineHeight: 1.55 }}>
                {report.aiFeedback}
              </p>
            </div>
          )}
        </div>

        {/* Right: Productivity Score Gauge */}
        <div
          style={{
            background: 'var(--surface)',
            borderRadius: 20,
            border: '1px solid var(--border)',
            padding: '22px 20px',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            textAlign: 'center',
            boxShadow: 'var(--shadow-sm)',
            position: 'relative',
          }}
        >
          <div
            style={{
              fontSize: '0.78rem',
              fontWeight: 700,
              color: 'var(--text-3)',
              textTransform: 'uppercase',
              letterSpacing: 0.6,
            }}
          >
            ĐIỂM NĂNG SUẤT
          </div>

          <div
            style={{
              position: 'relative',
              width: 146,
              height: 146,
              margin: '12px 0 10px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <svg width={146} height={146} style={{ transform: 'rotate(-90deg)' }}>
              <circle
                cx={73}
                cy={73}
                r={58}
                stroke="var(--border-light)"
                strokeWidth={11}
                fill="none"
              />
              <circle
                cx={73}
                cy={73}
                r={58}
                stroke={scoreColor}
                strokeWidth={11}
                strokeDasharray={2 * Math.PI * 58}
                strokeDashoffset={2 * Math.PI * 58 * (1 - score / 100)}
                strokeLinecap="round"
                fill="none"
                style={{ transition: 'stroke-dashoffset 0.8s ease' }}
              />
            </svg>
            <div style={{ position: 'absolute', textAlign: 'center' }}>
              <div style={{ fontSize: '2.5rem', fontWeight: 800, color: scoreColor, lineHeight: 1 }}>
                {score}
              </div>
              <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', fontWeight: 600, marginTop: 2 }}>
                / 100 điểm
              </div>
            </div>
          </div>

          <div
            style={{
              fontSize: '0.88rem',
              fontWeight: 700,
              color: scoreColor,
              padding: '3px 12px',
              borderRadius: 16,
              background: `${scoreColor}15`,
            }}
          >
            {scoreLabel}
          </div>

          <div style={{ fontSize: '0.75rem', color: 'var(--text-3)', marginTop: 8 }}>
            Đúng hạn: <strong>{onTimeRate}%</strong> • Tập trung: <strong>{focusMinutes}p</strong>
          </div>
        </div>
      </div>

      {/* ── 4 Detailed Metric Cards ── */}
      <div
        className="productivity-metrics-grid"
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(4, 1fr)',
          gap: 16,
          marginBottom: 24,
        }}
      >
        {/* Card 1: Completed Tasks */}
        <div
          style={{
            background: 'var(--surface)',
            border: '1px solid var(--border)',
            borderRadius: 16,
            padding: '16px 18px',
            boxShadow: 'var(--shadow-sm)',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <span style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-3)' }}>ĐÃ HOÀN THÀNH</span>
            <div
              style={{
                width: 32,
                height: 32,
                borderRadius: 8,
                background: 'rgba(16, 185, 129, 0.1)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <CheckCircle2 size={18} color="#10B981" />
            </div>
          </div>
          <div style={{ fontSize: '1.65rem', fontWeight: 800, color: 'var(--text)', marginTop: 8 }}>
            {completedTasks} <span style={{ fontSize: '0.88rem', color: 'var(--text-3)', fontWeight: 500 }}>/ {totalTasks} việc</span>
          </div>
          <div style={{ marginTop: 8 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.72rem', color: 'var(--text-muted)', marginBottom: 3 }}>
              <span>Tiến độ ngày</span>
              <span>{completionPct}%</span>
            </div>
            <div style={{ height: 5, borderRadius: 6, background: 'var(--border-light)', overflow: 'hidden' }}>
              <div style={{ width: `${completionPct}%`, height: '100%', background: '#10B981', borderRadius: 6, transition: 'width 0.5s ease' }} />
            </div>
          </div>
        </div>

        {/* Card 2: Focus Time */}
        <div
          style={{
            background: 'var(--surface)',
            border: '1px solid var(--border)',
            borderRadius: 16,
            padding: '16px 18px',
            boxShadow: 'var(--shadow-sm)',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <span style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-3)' }}>THỜI GIAN TẬP TRUNG</span>
            <div
              style={{
                width: 32,
                height: 32,
                borderRadius: 8,
                background: 'rgba(108, 92, 231, 0.1)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <Clock size={18} color="var(--primary)" />
            </div>
          </div>
          <div style={{ fontSize: '1.65rem', fontWeight: 800, color: 'var(--primary)', marginTop: 8 }}>
            {focusMinutes >= 60 ? `${Math.floor(focusMinutes / 60)}h ${focusMinutes % 60}p` : `${focusMinutes}p`}
          </div>
          <div style={{ fontSize: '0.74rem', color: 'var(--text-muted)', marginTop: 8 }}>
            {report?.totalFocusSessions || 0} phiên làm việc sâu ghi nhận
          </div>
        </div>

        {/* Card 3: On-time Rate */}
        <div
          style={{
            background: 'var(--surface)',
            border: '1px solid var(--border)',
            borderRadius: 16,
            padding: '16px 18px',
            boxShadow: 'var(--shadow-sm)',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <span style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-3)' }}>KỶ LUẬT ĐÚNG HẠN</span>
            <div
              style={{
                width: 32,
                height: 32,
                borderRadius: 8,
                background: 'rgba(59, 130, 246, 0.1)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <TrendingUp size={18} color="#3B82F6" />
            </div>
          </div>
          <div style={{ fontSize: '1.65rem', fontWeight: 800, color: '#3B82F6', marginTop: 8 }}>
            {onTimeRate}%
          </div>
          <div style={{ fontSize: '0.74rem', color: 'var(--text-muted)', marginTop: 8 }}>
            {onTimeRate >= 90 ? 'Kiểm soát hạn chót rất tốt' : 'Cần chú ý lịch biểu'}
          </div>
        </div>

        {/* Card 4: Overdue Tasks */}
        <div
          style={{
            background: 'var(--surface)',
            border: '1px solid var(--border)',
            borderRadius: 16,
            padding: '16px 18px',
            boxShadow: 'var(--shadow-sm)',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <span style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-3)' }}>VIỆC QUÁ HẠN</span>
            <div
              style={{
                width: 32,
                height: 32,
                borderRadius: 8,
                background: overdueTasks > 0 ? 'rgba(239, 68, 68, 0.1)' : 'rgba(16, 185, 129, 0.1)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <AlertTriangle size={18} color={overdueTasks > 0 ? '#EF4444' : '#10B981'} />
            </div>
          </div>
          <div style={{ fontSize: '1.65rem', fontWeight: 800, color: overdueTasks > 0 ? '#EF4444' : 'var(--text)', marginTop: 8 }}>
            {overdueTasks} <span style={{ fontSize: '0.88rem', color: 'var(--text-3)', fontWeight: 500 }}>việc</span>
          </div>
          <div style={{ fontSize: '0.74rem', color: 'var(--text-muted)', marginTop: 8 }}>
            {overdueTasks > 0 ? 'Cần dời lịch hoặc xử lý sớm' : 'Không có việc nào quá hạn'}
          </div>
        </div>
      </div>

      {/* ── 7-Day Trend Chart & Category Breakdown ── */}
      <div className="productivity-trends-grid" style={{ display: 'grid', gridTemplateColumns: '1.25fr 0.75fr', gap: 20, marginBottom: 24 }}>
        {/* 7-Day Interactive Trend Visual Bars */}
        <div
          style={{
            background: 'var(--surface)',
            border: '1px solid var(--border)',
            borderRadius: 18,
            padding: '22px 24px',
            boxShadow: 'var(--shadow-sm)',
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 20 }}>
            <h3 style={{ fontSize: '0.94rem', fontWeight: 800, color: 'var(--text)', display: 'flex', alignItems: 'center', gap: 8, margin: 0 }}>
              <BarChart3 size={17} color="var(--primary)" />
              <span>XU HƯỚNG 7 NGÀY GẦN NHẤT</span>
            </h3>
            <span style={{ fontSize: '0.74rem', color: 'var(--text-muted)' }}>
              Nhấp vào một ngày để xem chi tiết
            </span>
          </div>

          <div
            style={{
              display: 'flex',
              alignItems: 'flex-end',
              justifyContent: 'space-between',
              gap: 8,
              minHeight: 215,
              flex: 1,
              paddingTop: 14,
              borderBottom: '1px solid var(--border-light)',
              paddingBottom: 12,
            }}
          >
            {(report?.pastWeekTrends || []).map((d) => {
              const barHeight = Math.max(22, (d.score / 100) * 135);
              const isSelected = d.date === selectedDate;
              const isTodayColumn = d.date === getTodayStr();

              return (
                <div
                  key={d.date}
                  onClick={() => changeSelectedDate(d.date)}
                  style={{
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    gap: 8,
                    flex: 1,
                    cursor: 'pointer',
                    padding: '8px 4px',
                    borderRadius: 12,
                    background: isSelected ? 'var(--primary-bg)' : 'transparent',
                    transition: 'all 0.2s cubic-bezier(0.4, 0, 0.2, 1)',
                  }}
                  title={`Ngày ${d.date}: ${d.completedTasks} việc xong, ${d.focusMinutes}p tập trung, Điểm: ${d.score}/100`}
                >
                  <div
                    style={{
                      fontSize: '0.76rem',
                      fontWeight: 800,
                      color: isSelected ? 'var(--primary)' : 'var(--text-muted)',
                      transition: 'color 0.2s',
                    }}
                  >
                    {d.score}
                  </div>
                  <div
                    style={{
                      width: '62%',
                      maxWidth: 52,
                      minWidth: 40,
                      height: barHeight,
                      borderRadius: 10,
                      background: isSelected
                        ? 'linear-gradient(180deg, #6C5CE7 0%, #3B82F6 100%)'
                        : isTodayColumn
                        ? 'linear-gradient(180deg, rgba(108, 92, 231, 0.45) 0%, rgba(59, 130, 246, 0.45) 100%)'
                        : 'var(--border-light)',
                      border: isSelected ? '2px solid var(--primary)' : '1px solid var(--border)',
                      transition: 'height 0.4s cubic-bezier(0.34, 1.56, 0.64, 1), background 0.2s',
                      boxShadow: isSelected ? '0 4px 14px rgba(108, 92, 231, 0.35)' : 'none',
                    }}
                  />
                  <div
                    style={{
                      fontSize: '0.80rem',
                      fontWeight: isSelected ? 800 : 700,
                      color: isSelected ? 'var(--primary)' : 'var(--text-2)',
                      marginTop: 2,
                    }}
                  >
                    {d.dayOfWeek}
                  </div>
                  <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)', marginTop: -4, fontWeight: 500 }}>
                    {d.focusMinutes}p
                  </div>
                </div>
              );
            })}
          </div>

        </div>

        {/* Category Distribution */}
        <div
          style={{
            background: 'var(--surface)',
            border: '1px solid var(--border)',
            borderRadius: 18,
            padding: '20px 24px',
            boxShadow: 'var(--shadow-sm)',
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
          }}
        >
          <div>
            <h3 style={{ fontSize: '0.94rem', fontWeight: 800, color: 'var(--text)', marginBottom: 16, display: 'flex', alignItems: 'center', gap: 8, margin: 0 }}>
              <Calendar size={17} color="var(--primary)" />
              <span>PHÂN BỔ THEO NHÓM VIỆC</span>
            </h3>
            <p style={{ fontSize: '0.75rem', color: 'var(--text-3)', marginTop: 4, marginBottom: 16 }}>
              Tỷ trọng danh mục các công việc trong ngày
            </p>

            <div style={{ display: 'flex', flexDirection: 'column', gap: 13 }}>
              {Object.entries(report?.categoryDistribution || {}).map(([catName, count]) => {
                const totalCatCount = Object.values(report?.categoryDistribution || {}).reduce((a, b) => a + b, 0);
                const pct = totalCatCount > 0 ? Math.round((count / totalCatCount) * 100) : 0;
                const color = categoryColors[catName] || '#6C5CE7';

                return (
                  <div key={catName}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem', fontWeight: 600, marginBottom: 5 }}>
                      <span style={{ display: 'flex', alignItems: 'center', gap: 6, color: 'var(--text)' }}>
                        <span style={{ width: 8, height: 8, borderRadius: '50%', background: color }} />
                        {catName}
                      </span>
                      <span style={{ color: 'var(--text-3)' }}>{count} việc ({pct}%)</span>
                    </div>
                    <div style={{ height: 7, borderRadius: 10, background: 'var(--border-light)', overflow: 'hidden' }}>
                      <div
                        style={{
                          height: '100%',
                          width: `${pct}%`,
                          background: color,
                          borderRadius: 10,
                          transition: 'width 0.4s ease',
                        }}
                      />
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

        </div>
      </div>

      {/* ── Task Breakdown Section: Completed vs Scheduled Tasks for Date ── */}
      <div
        style={{
          background: 'var(--surface)',
          border: '1px solid var(--border)',
          borderRadius: 18,
          padding: '20px 24px',
          boxShadow: 'var(--shadow-sm)',
        }}
      >
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: 12,
            marginBottom: 16,
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <h3 style={{ fontSize: '0.96rem', fontWeight: 800, color: 'var(--text)', margin: 0, display: 'flex', alignItems: 'center', gap: 8 }}>
              <ListTodo size={18} color="var(--primary)" />
              <span>CHI TIẾT CÔNG VIỆC TRONG NGÀY</span>
            </h3>

            {/* Toggle tabs */}
            <div
              style={{
                display: 'flex',
                background: 'var(--border-light)',
                borderRadius: 10,
                padding: 3,
                gap: 4,
              }}
            >
              <button
                type="button"
                onClick={() => setActiveTaskTab('completed')}
                style={{
                  padding: '5px 12px',
                  borderRadius: 8,
                  border: 'none',
                  background: activeTaskTab === 'completed' ? 'var(--surface)' : 'transparent',
                  color: activeTaskTab === 'completed' ? 'var(--text)' : 'var(--text-3)',
                  fontSize: '0.78rem',
                  fontWeight: 700,
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  gap: 5,
                  boxShadow: activeTaskTab === 'completed' ? 'var(--shadow-xs)' : 'none',
                }}
              >
                <CheckSquare size={13} color="#10B981" />
                Đã hoàn thành ({report?.completedTaskItems?.length || completedTasks})
              </button>

              <button
                type="button"
                onClick={() => setActiveTaskTab('pending')}
                style={{
                  padding: '5px 12px',
                  borderRadius: 8,
                  border: 'none',
                  background: activeTaskTab === 'pending' ? 'var(--surface)' : 'transparent',
                  color: activeTaskTab === 'pending' ? 'var(--text)' : 'var(--text-3)',
                  fontSize: '0.78rem',
                  fontWeight: 700,
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  gap: 5,
                  boxShadow: activeTaskTab === 'pending' ? 'var(--shadow-xs)' : 'none',
                }}
              >
                <Zap size={13} color="var(--primary)" />
                Kế hoạch / Cần làm ({report?.pendingTaskItems?.length || Math.max(0, totalTasks - completedTasks)})
              </button>
            </div>
          </div>

          <button
            type="button"
            onClick={onNavigateToCalendar}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 5,
              fontSize: '0.78rem',
              fontWeight: 600,
              color: 'var(--primary)',
              background: 'transparent',
              border: 'none',
              cursor: 'pointer',
            }}
          >
            Xem toàn bộ trên lịch trình <ArrowUpRight size={14} />
          </button>
        </div>

        {/* Task List Content */}
        {activeTaskTab === 'completed' ? (
          <div>
            {report?.completedTaskItems && report.completedTaskItems.length > 0 ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                {report.completedTaskItems.map((item) => (
                  <div
                    key={item.id}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      padding: '12px 16px',
                      borderRadius: 12,
                      background: 'rgba(16, 185, 129, 0.04)',
                      border: '1px solid rgba(16, 185, 129, 0.2)',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                      <CheckCircle2 size={18} color="#10B981" />
                      <div>
                        <div style={{ fontSize: '0.88rem', fontWeight: 700, color: 'var(--text)' }}>
                          {item.title}
                        </div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 3 }}>
                          <span
                            style={{
                              fontSize: '0.7rem',
                              padding: '2px 8px',
                              borderRadius: 10,
                              background: `${item.categoryColor || '#6C5CE7'}20`,
                              color: item.categoryColor || '#6C5CE7',
                              fontWeight: 700,
                            }}
                          >
                            {item.categoryName || 'Chung'}
                          </span>
                          <span style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>
                            Thời lượng: {item.durationMinutes} phút
                          </span>
                        </div>
                      </div>
                    </div>

                    <span
                      style={{
                        fontSize: '0.74rem',
                        fontWeight: 700,
                        color: '#10B981',
                        padding: '4px 10px',
                        borderRadius: 12,
                        background: 'rgba(16, 185, 129, 0.12)',
                      }}
                    >
                      Đã xong
                    </span>
                  </div>
                ))}
              </div>
            ) : (
              <div
                style={{
                  textAlign: 'center',
                  padding: '32px 16px',
                  color: 'var(--text-3)',
                }}
              >
                <CheckCircle2 size={36} color="var(--border)" style={{ marginBottom: 8 }} />
                <div style={{ fontSize: '0.88rem', fontWeight: 600, color: 'var(--text-2)' }}>
                  Chưa có công việc nào hoàn thành trong ngày {selectedDate}
                </div>
                <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)', marginTop: 4 }}>
                  Hãy hoàn thành một công việc trong lịch trình để bắt đầu ghi nhận dữ liệu!
                </div>
              </div>
            )}
          </div>
        ) : (
          <div>
            {report?.pendingTaskItems && report.pendingTaskItems.length > 0 ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                {report.pendingTaskItems.map((item) => (
                  <div
                    key={item.id}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      padding: '12px 16px',
                      borderRadius: 12,
                      background: 'var(--surface)',
                      border: '1px solid var(--border)',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                      <div
                        style={{
                          width: 18,
                          height: 18,
                          borderRadius: 5,
                          border: '2px solid var(--border)',
                        }}
                      />
                      <div>
                        <div style={{ fontSize: '0.88rem', fontWeight: 700, color: 'var(--text)' }}>
                          {item.title}
                        </div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 3 }}>
                          <span
                            style={{
                              fontSize: '0.7rem',
                              padding: '2px 8px',
                              borderRadius: 10,
                              background: `${item.categoryColor || '#6C5CE7'}20`,
                              color: item.categoryColor || '#6C5CE7',
                              fontWeight: 700,
                            }}
                          >
                            {item.categoryName || 'Chung'}
                          </span>
                          <span style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>
                            Dự kiến: {item.durationMinutes} phút • Giờ: {item.dueTimeStr}
                          </span>
                        </div>
                      </div>
                    </div>

                    <span
                      style={{
                        fontSize: '0.72rem',
                        fontWeight: 700,
                        padding: '4px 9px',
                        borderRadius: 12,
                        background: item.priority === 'HIGH' || item.priority === 'URGENT'
                          ? 'rgba(239, 68, 68, 0.1)'
                          : 'var(--border-light)',
                        color: item.priority === 'HIGH' || item.priority === 'URGENT'
                          ? '#EF4444'
                          : 'var(--text-2)',
                      }}
                    >
                      {item.priority === 'URGENT' ? 'Khẩn cấp' : item.priority === 'HIGH' ? 'Ưu tiên cao' : 'Bình thường'}
                    </span>
                  </div>
                ))}
              </div>
            ) : (
              <div
                style={{
                  textAlign: 'center',
                  padding: '32px 16px',
                  color: 'var(--text-3)',
                }}
              >
                <Zap size={36} color="var(--border)" style={{ marginBottom: 8 }} />
                <div style={{ fontSize: '0.88rem', fontWeight: 600, color: 'var(--text-2)' }}>
                  Không có công việc nào chưa hoàn thành trong ngày này
                </div>
                <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)', marginTop: 4 }}>
                  Tất cả kế hoạch đã được xử lý hoặc ngày này là khoảng nghỉ của bạn.
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
