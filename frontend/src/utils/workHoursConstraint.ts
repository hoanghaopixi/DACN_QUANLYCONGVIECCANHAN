/**
 * Tiện ích kiểm tra ràng buộc giờ làm việc & khoảng nghỉ theo thiết lập người dùng.
 * Hỗ trợ hiển thị cảnh báo: "Bạn có muốn tạo việc làm ngoài giờ nghỉ?"
 */

export interface WorkHoursValidationResult {
  isOutsideWorkHours: boolean;
  reason?: string;
  promptTitle: string;
  promptMessage: string;
  workStart: string;
  workEnd: string;
  lunchStart: string;
  lunchEnd: string;
}

export function getUserSettings() {
  const DEFAULT_SETTINGS = {
    workingDays: ['MON', 'TUE', 'WED', 'THU', 'FRI'],
    workHours: '07:00 - 20:00',
    lunchBreak: '12:00 - 13:00',
    bufferMinutes: '15 phút',
    allowWeekend: true,
  };

  try {
    const saved = localStorage.getItem('smarttask_settings_full');
    if (saved) {
      const parsed = JSON.parse(saved);
      return { ...DEFAULT_SETTINGS, ...parsed };
    }
    const legacy = localStorage.getItem('smarttask_preferences');
    if (legacy) {
      const p = JSON.parse(legacy);
      return {
        ...DEFAULT_SETTINGS,
        workingDays: p.workingDays || DEFAULT_SETTINGS.workingDays,
        workHours: `${p.workStartTime || '07:00'} - ${p.workEndTime || '20:00'}`,
        bufferMinutes: `${p.bufferTime || 15} phút`,
      };
    }
  } catch (e) {
    console.error('Error reading user settings:', e);
  }
  return DEFAULT_SETTINGS;
}

function timeStringToMinutes(timeStr: string): number {
  if (!timeStr) return 0;
  const parts = timeStr.trim().split(':');
  const h = parseInt(parts[0], 10) || 0;
  const m = parseInt(parts[1], 10) || 0;
  return h * 60 + m;
}

/**
 * Kiểm tra xem công việc có vi phạm ràng buộc giờ làm việc, nghỉ trưa hoặc ngày nghỉ cuối tuần không
 */
export function checkWorkHoursConstraint(
  startTimeIsoOrStr: string,
  endTimeIsoOrStr: string,
  categoryType: string = 'WORK'
): WorkHoursValidationResult {
  const settings = getUserSettings();

  const [workStart, workEnd] = (settings.workHours || '07:00 - 20:00')
    .split('-')
    .map((s: string) => s.trim() || '07:00');
  const [lunchStart, lunchEnd] = (settings.lunchBreak || '12:00 - 13:00')
    .split('-')
    .map((s: string) => s.trim() || '12:00');

  const workStartMin = timeStringToMinutes(workStart || '07:00');
  const workEndMin = timeStringToMinutes(workEnd || '20:00');
  const lunchStartMin = timeStringToMinutes(lunchStart || '12:00');
  const lunchEndMin = timeStringToMinutes(lunchEnd || '13:00');

  const startDate = new Date(startTimeIsoOrStr);
  const endDate = new Date(endTimeIsoOrStr);

  const taskStartMin = !isNaN(startDate.getTime())
    ? startDate.getHours() * 60 + startDate.getMinutes()
    : 0;
  let taskEndMin = !isNaN(endDate.getTime())
    ? endDate.getHours() * 60 + endDate.getMinutes()
    : 0;

  if (taskEndMin === 0 && !isNaN(endDate.getTime()) && !isNaN(startDate.getTime()) && endDate.getDate() !== startDate.getDate()) {
    taskEndMin = 24 * 60; // 24:00
  }

  const dayOfWeekIndex = !isNaN(startDate.getTime()) ? startDate.getDay() : 1; // 0 = SUN, 1 = MON, ..., 6 = SAT
  const dayCodeMap = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'];
  const currentDayCode = dayCodeMap[dayOfWeekIndex];

  const isWorkingDay = (settings.workingDays || []).includes(currentDayCode);
  const isWeekend = dayOfWeekIndex === 0 || dayOfWeekIndex === 6;

  let isOutside = false;
  let reason = '';

  const isPersonalOrLeisure = categoryType === 'HEALTH' || categoryType === 'ENTERTAINMENT';

  // Đối với hoạt động Thể dục, Sức khỏe (HEALTH) và Giải trí (ENTERTAINMENT):
  // Đây là sinh hoạt cá nhân, hoàn toàn bình thường khi diễn ra sáng sớm (từ 05:00) hoặc tối (đến 22:30)
  if (isPersonalOrLeisure) {
    if (taskStartMin < 300) { // Trước 05:00 sáng
      isOutside = true;
      reason = 'Lịch trình diễn ra vào sáng rất sớm (trước 05:00 sáng).';
    } else if (taskEndMin > 1350) { // Sau 22:30 đêm
      isOutside = true;
      reason = 'Lịch trình diễn ra khá muộn vào ban đêm (sau 22:30). Bạn nhớ dành thời gian ngủ đủ giấc nhé.';
    } else if (taskStartMin < lunchEndMin && taskEndMin > lunchStartMin && categoryType === 'HEALTH') {
      // Tập thể dục vào đúng 12:00 - 13:00 trưa
      isOutside = true;
      reason = `Thời gian vận động rơi vào giờ nghỉ trưa (${lunchStart} – ${lunchEnd}).`;
    }
  } else {
    // Đối với WORK và STUDY (Công việc & Học tập chính):
    // 1. Kiểm tra kết thúc sau giờ làm việc
    if (taskEndMin > workEndMin) {
      isOutside = true;
      const taskEndStr = `${String(Math.floor(taskEndMin / 60)).padStart(2, '0')}:${String(taskEndMin % 60).padStart(2, '0')}`;
      reason = `Công việc kết thúc lúc ${taskEndStr}, sau khung giờ làm việc thông thường (${workEnd}).`;
    }
    // 2. Bắt đầu trước giờ làm việc
    else if (taskStartMin < workStartMin) {
      isOutside = true;
      const taskStartStr = `${String(Math.floor(taskStartMin / 60)).padStart(2, '0')}:${String(taskStartMin % 60).padStart(2, '0')}`;
      reason = `Công việc bắt đầu lúc ${taskStartStr}, sớm hơn giờ làm việc thông thường (${workStart}).`;
    }
    // 3. Trùng giờ nghỉ trưa
    else if (taskStartMin < lunchEndMin && taskEndMin > lunchStartMin) {
      isOutside = true;
      reason = `Công việc rơi vào khung giờ nghỉ trưa (${lunchStart} – ${lunchEnd}).`;
    }
    // 4. Ngày nghỉ cuối tuần (T7, CN) cho công việc
    else if (!isWorkingDay && isWeekend && categoryType === 'WORK') {
      isOutside = true;
      reason = 'Hôm nay là ngày nghỉ cuối tuần. Bạn có muốn xếp thêm việc làm không?';
    }
  }

  return {
    isOutsideWorkHours: isOutside,
    reason,
    promptTitle: 'Lưu ý về thời gian',
    promptMessage: isOutside ? 'Bạn có muốn tiếp tục lưu lịch vào khung giờ này không?' : '',
    workStart,
    workEnd,
    lunchStart,
    lunchEnd,
  };
}
