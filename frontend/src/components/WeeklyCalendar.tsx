import { useState, useRef, useEffect } from 'react';
import { RotateCw, MoreVertical, GripVertical, Trash2, Copy, Check, X } from 'lucide-react';
import { TimeBlock, Category, TaskStatus } from '../types';
import { checkWorkHoursConstraint } from '../utils/workHoursConstraint';
import { taskService } from '../services/taskService';
import PrivacyCalloutTooltip from './PrivacyCalloutTooltip';
import PrivacyCalloutPopover from './PrivacyCalloutPopover';
import EyeLockIcon from './EyeLockIcon';

interface WeeklyCalendarProps {
  timeBlocks: (TimeBlock & { isRecurring?: boolean; subtext?: string; colorOverride?: string })[];
  categories: Category[];
  selectedCategory: string | null;
  statusFilter: string;
  onSelectBlock: (block: TimeBlock) => void;
  onAddAtSlot: (dayIndex: number, hour: number) => void;
  onRescheduleTask?: (blockId: number, newStartTime: string, newEndTime: string) => void;
  days?: { name: string; dateNum: number; dateStr: string; isToday: boolean }[];
  onReload?: () => Promise<void> | void;
  onShowToast?: (msg: string) => void;
  onJumpToDate?: (date: Date) => void;
  onDeleteTask?: (taskId: number) => Promise<void> | void;
  onToggleTaskStatus?: (taskId: number, newStatus: TaskStatus) => Promise<void> | void;
  isPrivacyUnlocked?: boolean;
  unlockedScheduleIds?: Set<number>;
  onUnlockSingle?: (id: number) => void;
  onUnlockPrivacy?: () => void;
  onUnlockSuccess?: () => void;
  onTogglePrivacyLock?: () => void;
  focusedTaskId?: number | null;
}

// Full 24 hours: 0:00 to 23:59
const HOURS = Array.from({ length: 24 }, (_, i) => i);
// 60px per hour = exact 1px per minute
const HOUR_HEIGHT = 60;

const getBaseWeekDays = () => {
  const now = new Date();
  const day = now.getDay();
  const diff = now.getDate() - day + (day === 0 ? -6 : 1);
  const monday = new Date(now.getFullYear(), now.getMonth(), diff);
  const names = ['Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy', 'Chủ Nhật'];
  return Array.from({ length: 7 }, (_, i) => {
    const d = new Date(monday);
    d.setDate(monday.getDate() + i);
    const ymd = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
    return {
      name: names[i],
      dateNum: d.getDate(),
      dateStr: ymd,
    };
  });
};
const DAYS_BASE = getBaseWeekDays();

interface PositionedBlock {
  block: any;
  startHour: number;
  endHour: number;
  durationMs: number;
  top: number;
  height: number;
  colIndex: number;
  totalCols: number;
}

function computeLayoutBlocks(blocks: any[]): PositionedBlock[] {
  if (!blocks || blocks.length === 0) return [];

  const parsed = blocks.map((b) => {
    const s = new Date(b.startTime);
    const e = new Date(b.endTime);
    const startH = s.getHours() + s.getMinutes() / 60;
    let endH = e.getHours() + e.getMinutes() / 60;
    if (endH <= startH) endH = startH + 1;
    const durMs = Math.max(1800000, e.getTime() - s.getTime());
    return {
      block: b,
      startHour: startH,
      endHour: endH,
      durationMs: durMs,
      top: Math.round(startH * HOUR_HEIGHT),
      height: Math.max(36, Math.round((endH - startH) * HOUR_HEIGHT) - 2),
    };
  });

  parsed.sort((a, b) => {
    if (Math.abs(a.startHour - b.startHour) > 0.001) return a.startHour - b.startHour;
    return (b.endHour - b.startHour) - (a.endHour - a.startHour);
  });

  const clusters: (typeof parsed)[] = [];
  let currentCluster: typeof parsed = [];
  let clusterEnd = -1;

  for (const item of parsed) {
    if (currentCluster.length === 0) {
      currentCluster.push(item);
      clusterEnd = item.endHour;
    } else {
      if (item.startHour < clusterEnd - 0.001) {
        currentCluster.push(item);
        clusterEnd = Math.max(clusterEnd, item.endHour);
      } else {
        clusters.push(currentCluster);
        currentCluster = [item];
        clusterEnd = item.endHour;
      }
    }
  }
  if (currentCluster.length > 0) {
    clusters.push(currentCluster);
  }

  const result: PositionedBlock[] = [];

  for (const cluster of clusters) {
    const colEnds: number[] = [];
    const assigned: { item: typeof parsed[0]; colIndex: number }[] = [];

    for (const item of cluster) {
      let placedCol = -1;
      for (let c = 0; c < colEnds.length; c++) {
        if (item.startHour >= colEnds[c] - 0.001) {
          placedCol = c;
          colEnds[c] = item.endHour;
          break;
        }
      }
      if (placedCol === -1) {
        placedCol = colEnds.length;
        colEnds.push(item.endHour);
      }
      assigned.push({ item, colIndex: placedCol });
    }

    const totalCols = Math.max(1, colEnds.length);
    for (const a of assigned) {
      result.push({
        block: a.item.block,
        startHour: a.item.startHour,
        endHour: a.item.endHour,
        durationMs: a.item.durationMs,
        top: a.item.top,
        height: a.item.height,
        colIndex: a.colIndex,
        totalCols,
      });
    }
  }

  return result;
}

export default function WeeklyCalendar({
  timeBlocks,
  categories,
  selectedCategory,
  statusFilter,
  onSelectBlock,
  onAddAtSlot,
  onRescheduleTask,
  days,
  onReload,
  onShowToast,
  onJumpToDate,
  onDeleteTask,
  onToggleTaskStatus,
  isPrivacyUnlocked = false,
  unlockedScheduleIds,
  onUnlockSingle,
  onUnlockPrivacy,
  onUnlockSuccess,
  onTogglePrivacyLock,
  focusedTaskId,
}: WeeklyCalendarProps) {
  const [hoveredBlockId, setHoveredBlockId] = useState<number | null>(null);
  const [hoveredBlockRect, setHoveredBlockRect] = useState<DOMRect | null>(null);
  const [draggingBlockId, setDraggingBlockId] = useState<number | null>(null);
  const [activeLockedBlockId, setActiveLockedBlockId] = useState<number | null>(null);
  const [activeLockedBlockRect, setActiveLockedBlockRect] = useState<DOMRect | null>(null);

  useEffect(() => {
    const scrollEl = scrollRef.current;
    if (!scrollEl) return;
    const handleScroll = () => {
      if (hoveredBlockId !== null) {
        setHoveredBlockId(null);
        setHoveredBlockRect(null);
      }
    };
    scrollEl.addEventListener('scroll', handleScroll, { passive: true });
    return () => scrollEl.removeEventListener('scroll', handleScroll);
  }, [hoveredBlockId]);

  const handleToggleLockTask = async (e: React.MouseEvent, block: any) => {
    e.stopPropagation();
    const newLocked = !block.isLocked;
    try {
      await taskService.updateTask(block.id, { isLocked: newLocked });
      if (onReload) {
        await onReload();
      }
      if (onShowToast) {
        onShowToast(
          newLocked
            ? 'Đã khóa lịch trình này vào chế độ riêng tư!'
            : 'Đã hủy khóa riêng tư cho lịch trình này!'
        );
      }
    } catch (err: any) {
      if (onShowToast) {
        onShowToast('Lỗi khi cập nhật trạng thái khóa: ' + (err.message || ''));
      }
    }
  };

  // Global drag-end cleanup to ensure dragged card never stays faded (opacity 0.45)
  useEffect(() => {
    const handleClearDrag = () => {
      setDraggingBlockId(null);
    };
    window.addEventListener('dragend', handleClearDrag);
    window.addEventListener('drop', handleClearDrag);
    window.addEventListener('mouseup', handleClearDrag);
    return () => {
      window.removeEventListener('dragend', handleClearDrag);
      window.removeEventListener('drop', handleClearDrag);
      window.removeEventListener('mouseup', handleClearDrag);
    };
  }, []);

  // ── Quick Duplicate Popover (Bấm vào nút Nhân bản -> Hiện ngay bên cạnh thẻ lịch trình) ──
  const [duplicateBlockId, setDuplicateBlockId] = useState<number | null>(null);
  const [duplicateMode, setDuplicateMode] = useState<'days' | 'week' | 'day'>('days');
  const [customUnitsCount, setCustomUnitsCount] = useState<number>(1);
  const [selectedDaysOfWeek, setSelectedDaysOfWeek] = useState<number[]>([1, 3, 5]); // 1: T2, 3: T4, 5: T6
  const [duplicateTargetScope, setDuplicateTargetScope] = useState<'this_week' | 'next_week' | 'multi_week'>('next_week');
  const [duplicateCategoryType, setDuplicateCategoryType] = useState<string>('WORK');
  const [isDuplicating, setIsDuplicating] = useState<boolean>(false);
  const scrollRef = useRef<HTMLDivElement>(null);
  const [scrollbarWidth, setScrollbarWidth] = useState<number>(0);

  useEffect(() => {
    const el = scrollRef.current;
    if (!el) return;
    const updateScrollbar = () => {
      const sw = el.offsetWidth - el.clientWidth;
      setScrollbarWidth(Math.max(0, sw));
    };
    updateScrollbar();
    const ro = new ResizeObserver(updateScrollbar);
    ro.observe(el);
    window.addEventListener('resize', updateScrollbar);
    return () => {
      ro.disconnect();
      window.removeEventListener('resize', updateScrollbar);
    };
  }, []);

  // Close duplicate popover on outside click
  useEffect(() => {
    if (!duplicateBlockId) return;
    const handleDismiss = () => setDuplicateBlockId(null);
    window.addEventListener('click', handleDismiss);
    return () => window.removeEventListener('click', handleDismiss);
  }, [duplicateBlockId]);

  // ── Action Handlers: Delete, Complete, Duplicate ──
  const handleDeleteBlock = async (e: React.MouseEvent, block: any) => {
    e.stopPropagation();
    e.preventDefault();
    const targetId = Number(block.taskId || block.id);
    if (!targetId) return;

    // Reset local hover & duplicate states immediately
    setHoveredBlockId(null);
    setDuplicateBlockId(null);

    try {
      if (onDeleteTask) {
        await onDeleteTask(targetId);
      } else {
        await taskService.deleteTask(targetId);
        onShowToast?.(`✓ Đã xóa công việc "${block.title}" khỏi lịch!`);
        await onReload?.();
      }
    } catch (err) {
      console.error('Lỗi khi xóa lịch trình:', err);
      onShowToast?.('Lỗi khi xóa công việc khỏi máy chủ');
    }
  };

  const handleToggleComplete = async (e: React.MouseEvent, block: any) => {
    e.stopPropagation();
    const targetId = Number(block.taskId || block.id);
    const isCompleted = block.status === 'COMPLETED';
    const newStatus = isCompleted ? 'TODO' : 'COMPLETED';
    try {
      if (onToggleTaskStatus) {
        await onToggleTaskStatus(targetId, newStatus);
      } else {
        await taskService.updateTask(targetId, { status: newStatus });
        await onReload?.();
      }
      if (newStatus === 'COMPLETED') {
        onShowToast?.(`🎉 Đã hoàn tất công việc "${block.title}"!`);
      } else {
        onShowToast?.(`Đã chuyển công việc "${block.title}" về Cần làm`);
      }
    } catch (err) {
      console.error('Lỗi cập nhật trạng thái:', err);
      onShowToast?.('Lỗi khi cập nhật trạng thái công việc');
    }
  };

  const handleOpenDuplicate = (e: React.MouseEvent, block: any) => {
    e.stopPropagation();
    e.preventDefault();
    if (duplicateBlockId === block.id) {
      setDuplicateBlockId(null);
      return;
    }
    setDuplicateBlockId(block.id);
    setDuplicateMode('days'); // Mặc định mở tab "Theo Thứ" (cấu hình Thứ riêng biệt theo yêu cầu)
    setCustomUnitsCount(1);
    setDuplicateTargetScope('next_week');
    setDuplicateCategoryType(block.categoryType || 'WORK');

    const startD = new Date(block.startTime);
    const dayOfWeek = startD.getDay(); // 0 = CN, 1 = T2, ..., 6 = T7
    if (dayOfWeek === 1 || dayOfWeek === 3 || dayOfWeek === 5) {
      setSelectedDaysOfWeek([1, 3, 5]);
    } else if (dayOfWeek === 2 || dayOfWeek === 4 || dayOfWeek === 6) {
      setSelectedDaysOfWeek([2, 4, 6]);
    } else {
      setSelectedDaysOfWeek([dayOfWeek]);
    }
  };

  const formatOffset = (d: Date) => {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const date = String(d.getDate()).padStart(2, '0');
    const h = String(d.getHours()).padStart(2, '0');
    const min = String(d.getMinutes()).padStart(2, '0');
    const s = String(d.getSeconds()).padStart(2, '0');
    return `${y}-${m}-${date}T${h}:${min}:${s}+07:00`;
  };

  // ── Nhân bản theo THỨ & PHÂN LOẠI CÔNG VIỆC: Cho phép chọn riêng từng Thứ (T2, T3, T4, T5, T6, T7, CN) ──
  const executeDuplicateByDays = async (block: any) => {
    if (!block || selectedDaysOfWeek.length === 0) return;
    setDuplicateBlockId(null);
    setIsDuplicating(true);
    try {
      const startBase = new Date(block.startTime);
      const endBase = new Date(block.endTime);
      const startHour = startBase.getHours();
      const startMin = startBase.getMinutes();
      const endHour = endBase.getHours();
      const endMin = endBase.getMinutes();

      // Tìm ngày Thứ Hai của tuần gốc
      const baseDayOfWeek = startBase.getDay();
      const diffToMonday = baseDayOfWeek === 0 ? -6 : 1 - baseDayOfWeek;
      const baseMonday = new Date(startBase);
      baseMonday.setDate(startBase.getDate() + diffToMonday);
      baseMonday.setHours(0, 0, 0, 0);

      let startWeekOffset = 0;
      let totalWeeks = 1;
      if (duplicateTargetScope === 'this_week') {
        startWeekOffset = 0;
        totalWeeks = 1;
      } else if (duplicateTargetScope === 'next_week') {
        startWeekOffset = 1;
        totalWeeks = 1;
      } else {
        startWeekOffset = 1;
        totalWeeks = Math.max(1, customUnitsCount);
      }

      let createdCount = 0;
      let firstCreatedDate: Date | null = null;
      const now = new Date();

      for (let w = 0; w < totalWeeks; w++) {
        const weekOffset = startWeekOffset + w;
        const targetMonday = new Date(baseMonday);
        targetMonday.setDate(baseMonday.getDate() + weekOffset * 7);

        for (const targetDayOfWeek of selectedDaysOfWeek) {
          // targetDayOfWeek: 1: T2, 2: T3, 3: T4, 4: T5, 5: T6, 6: T7, 0: CN
          const daysFromMonday = targetDayOfWeek === 0 ? 6 : targetDayOfWeek - 1;
          const targetDate = new Date(targetMonday);
          targetDate.setDate(targetMonday.getDate() + daysFromMonday);

          const newStart = new Date(targetDate);
          newStart.setHours(startHour, startMin, 0, 0);

          const newEnd = new Date(targetDate);
          newEnd.setHours(endHour, endMin, 0, 0);

          // Nếu chọn tuần này và thời gian đã qua trong quá khứ -> bỏ qua
          if (duplicateTargetScope === 'this_week' && newStart.getTime() < now.getTime() - 3600000) {
            continue;
          }

          // Tránh tạo trùng lặp với chính block này nếu cùng thời gian
          if (Math.abs(newStart.getTime() - startBase.getTime()) < 60000) {
            continue;
          }

          // Kiểm tra xem đã có công việc cùng tên trong khung giờ đó chưa
          const isAlreadyScheduled = timeBlocks.some((existing) => {
            const existingStart = new Date(existing.startTime).getTime();
            return (
              Math.abs(existingStart - newStart.getTime()) < 60000 &&
              existing.title.trim().toLowerCase() === block.title.trim().toLowerCase()
            );
          });
          if (isAlreadyScheduled) continue;

          if (!firstCreatedDate) firstCreatedDate = newStart;

          // Tìm category tương ứng với duplicateCategoryType đã chọn
          const matchedCategory = categories.find(
            (c) => c.type === duplicateCategoryType || (c as any).categoryType === duplicateCategoryType || c.name.toLowerCase() === duplicateCategoryType.toLowerCase()
          );

          await taskService.createTask({
            title: block.title,
            description: block.description || block.note,
            note: block.note || block.description,
            categoryType: duplicateCategoryType || block.categoryType || 'WORK',
            categoryId: matchedCategory?.id || block.categoryId,
            priority: block.priority || 'MEDIUM',
            status: 'TODO',
            eisenhowerQuadrant: block.eisenhowerQuadrant || 'SCHEDULE',
            startTime: formatOffset(newStart),
            endTime: formatOffset(newEnd),
            estimatedDurationMinutes: block.estimatedDurationMinutes,
            colorCode: matchedCategory?.colorCode || block.colorOverride,
            workspaceId: block.workspaceId,
          });

          createdCount++;
        }
      }

      if (createdCount > 0) {
        const dayLabels = selectedDaysOfWeek.map((d) => ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'][d]).join(', ');
        onShowToast?.(`🎉 Đã nhân bản "${block.title}" sang [${dayLabels}] thành công (${createdCount} lịch mới)!`);
        if (firstCreatedDate && onJumpToDate) {
          onJumpToDate(firstCreatedDate);
        }
      } else {
        onShowToast?.(`Các khung giờ đã chọn cho "${block.title}" đã có sẵn lịch hoặc không hợp lệ.`);
      }

      await onReload?.();
    } catch (err) {
      console.error('Lỗi khi nhân bản công việc theo Thứ:', err);
      onShowToast?.('Lỗi khi nhân bản công việc theo Thứ');
    } finally {
      setIsDuplicating(false);
    }
  };

  // ── Nhân bản theo TUẦN: Sang tuần sau & các tuần tới (Cùng thứ & giờ này) ──
  const executeDuplicateWeekly = async (block: any, weeksCount: number) => {
    if (!block || weeksCount < 1) return;
    setDuplicateBlockId(null);
    setIsDuplicating(true);
    try {
      const startBase = new Date(block.startTime);
      const endBase = new Date(block.endTime);
      let createdCount = 0;
      let firstTargetDate: Date | null = null;

      for (let w = 1; w <= weeksCount; w++) {
        const newStart = new Date(startBase);
        newStart.setDate(startBase.getDate() + w * 7);

        const newEnd = new Date(endBase);
        newEnd.setDate(endBase.getDate() + w * 7);

        if (!firstTargetDate) firstTargetDate = newStart;

        // Tránh dồn cục: Kiểm tra xem ngày đó và khung giờ đó đã có công việc này chưa
        const isAlreadyScheduled = timeBlocks.some((existing) => {
          const existingStart = new Date(existing.startTime).getTime();
          const targetStart = newStart.getTime();
          return (
            Math.abs(existingStart - targetStart) < 60000 &&
            existing.title.trim().toLowerCase() === block.title.trim().toLowerCase()
          );
        });

        if (isAlreadyScheduled) continue; // Bỏ qua không tạo trùng

        await taskService.createTask({
          title: block.title,
          description: block.description || block.note,
          note: block.note || block.description,
          categoryType: block.categoryType || 'WORK',
          priority: block.priority || 'MEDIUM',
          status: 'TODO',
          eisenhowerQuadrant: block.eisenhowerQuadrant || 'SCHEDULE',
          startTime: formatOffset(newStart),
          endTime: formatOffset(newEnd),
          estimatedDurationMinutes: block.estimatedDurationMinutes,
          colorCode: block.colorOverride,
          workspaceId: block.workspaceId,
        });

        createdCount++;
      }

      if (createdCount > 0) {
        const dayNames = ['Chủ Nhật', 'Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy'];
        const targetDayName = dayNames[startBase.getDay()];
        if (weeksCount === 1) {
          onShowToast?.(`🎉 Đã nhân bản "${block.title}" sang tuần sau (${targetDayName})!`);
        } else {
          onShowToast?.(`🎉 Đã nhân bản "${block.title}" sang ${weeksCount} tuần tới (${targetDayName} hàng tuần)!`);
        }
        // Chuyển view lịch sang tuần sau để người dùng nhìn thấy ngay
        if (firstTargetDate && onJumpToDate) {
          onJumpToDate(firstTargetDate);
        }
      } else {
        onShowToast?.(`Công việc "${block.title}" đã có sẵn trong các tuần được chọn.`);
      }

      await onReload?.();
    } catch (err) {
      console.error('Lỗi khi nhân bản công việc theo tuần:', err);
      onShowToast?.('Lỗi khi nhân bản công việc sang tuần sau');
    } finally {
      setIsDuplicating(false);
    }
  };

  // ── Nhân bản theo NGÀY: Ngày mai hoặc các ngày liên tiếp ──
  const executeDuplicateDaily = async (block: any, daysCount: number) => {
    if (!block || daysCount < 1) return;
    setDuplicateBlockId(null);
    setIsDuplicating(true);
    try {
      const startBase = new Date(block.startTime);
      const endBase = new Date(block.endTime);
      let createdCount = 0;
      let targetCrossDate: Date | null = null;

      const currentWeekEndStr = DAYS[DAYS.length - 1]?.dateStr;

      for (let i = 1; i <= daysCount; i++) {
        const newStart = new Date(startBase);
        newStart.setDate(startBase.getDate() + i);

        const newEnd = new Date(endBase);
        newEnd.setDate(endBase.getDate() + i);

        // Tránh dồn cục: Kiểm tra trùng lịch
        const isAlreadyScheduled = timeBlocks.some((existing) => {
          const existingStart = new Date(existing.startTime).getTime();
          const targetStart = newStart.getTime();
          return (
            Math.abs(existingStart - targetStart) < 60000 &&
            existing.title.trim().toLowerCase() === block.title.trim().toLowerCase()
          );
        });

        if (isAlreadyScheduled) continue; // Bỏ qua không tạo trùng

        const ymd = `${newStart.getFullYear()}-${String(newStart.getMonth() + 1).padStart(2, '0')}-${String(newStart.getDate()).padStart(2, '0')}`;
        if (currentWeekEndStr && ymd > currentWeekEndStr && !targetCrossDate) {
          targetCrossDate = newStart;
        }

        await taskService.createTask({
          title: block.title,
          description: block.description || block.note,
          note: block.note || block.description,
          categoryType: block.categoryType || 'WORK',
          priority: block.priority || 'MEDIUM',
          status: 'TODO',
          eisenhowerQuadrant: block.eisenhowerQuadrant || 'SCHEDULE',
          startTime: formatOffset(newStart),
          endTime: formatOffset(newEnd),
          estimatedDurationMinutes: block.estimatedDurationMinutes,
          colorCode: block.colorOverride,
          workspaceId: block.workspaceId,
        });

        createdCount++;
      }

      if (createdCount > 0) {
        if (daysCount === 1) {
          onShowToast?.(`✓ Đã nhân bản "${block.title}" sang ngày mai!`);
        } else {
          onShowToast?.(`✓ Đã nhân bản "${block.title}" cho ${createdCount} ngày tới${targetCrossDate ? ' (đã qua tuần sau)' : ''}!`);
        }
        // Nếu nhân bản nhiều ngày vượt qua tuần sau, tự chuyển view để xem
        if (targetCrossDate && onJumpToDate && daysCount >= 7) {
          onJumpToDate(targetCrossDate);
        }
      } else {
        onShowToast?.(`Công việc "${block.title}" đã có sẵn trong các ngày được chọn.`);
      }

      await onReload?.();
    } catch (err) {
      console.error('Lỗi khi nhân bản công việc theo ngày:', err);
      onShowToast?.('Lỗi khi nhân bản công việc');
    } finally {
      setIsDuplicating(false);
    }
  };

  const now = new Date();
  const currentYMD = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
  const DAYS = days && days.length > 0 ? days : DAYS_BASE.map((day) => ({
    ...day,
    isToday: day.dateStr === currentYMD,
  }));

  const currentHour = now.getHours();
  const currentMinute = now.getMinutes();
  // Live GMT+7 line across full 24-hour range
  const currentTimeTop = currentHour * HOUR_HEIGHT + (currentMinute / 60) * HOUR_HEIGHT;

  // Auto-scroll to morning / current time on initial mount
  useEffect(() => {
    if (scrollRef.current) {
      const currentH = now.getHours();
      const targetHour = Math.max(0, Math.min(18, currentH >= 7 ? currentH - 1 : 7));
      scrollRef.current.scrollTop = targetHour * HOUR_HEIGHT;
    }
  }, []);

  useEffect(() => {
    if (focusedTaskId == null) return;
    const frame = window.requestAnimationFrame(() => {
      const scrollElement = scrollRef.current;
      const target = scrollElement?.querySelector<HTMLElement>(`[data-task-id="${focusedTaskId}"]`);
      if (!scrollElement || !target) return;
      const targetTop = scrollElement.scrollTop + target.getBoundingClientRect().top - scrollElement.getBoundingClientRect().top;
      scrollElement.scrollTo({
        top: Math.max(0, targetTop - scrollElement.clientHeight * 0.32),
        behavior: 'smooth',
      });
    });
    return () => window.cancelAnimationFrame(frame);
  }, [focusedTaskId, days, timeBlocks]);

  // Category / override color mapper
  const getCategoryStyles = (categoryType: string, colorOverride?: string) => {
    if (colorOverride === '#3B82F6') {
      return {
        borderLeft: '3px solid #3B82F6',
        backgroundColor: '#EFF6FF',
        titleColor: '#1E40AF',
        timeColor: '#3B82F6',
      };
    }
    if (colorOverride === '#8B5CF6') {
      return {
        borderLeft: '3px solid #8B5CF6',
        backgroundColor: '#F3E8FF',
        titleColor: '#5B21B6',
        timeColor: '#7C3AED',
      };
    }
    if (colorOverride === '#10B981') {
      return {
        borderLeft: '3px solid #10B981',
        backgroundColor: '#ECFDF5',
        titleColor: '#065F46',
        timeColor: '#059669',
      };
    }
    if (colorOverride === '#F59E0B') {
      return {
        borderLeft: '3px solid #F59E0B',
        backgroundColor: '#FFFBEB',
        titleColor: '#92400E',
        timeColor: '#D97706',
      };
    }
    switch (categoryType) {
      case 'STUDY':
        return {
          borderLeft: '3px solid #3B82F6',
          backgroundColor: '#EFF6FF',
          titleColor: '#1E40AF',
          timeColor: '#3B82F6',
        };
      case 'WORK':
        return {
          borderLeft: '3px solid #8B5CF6',
          backgroundColor: '#F3E8FF',
          titleColor: '#5B21B6',
          timeColor: '#7C3AED',
        };
      case 'HEALTH':
        return {
          borderLeft: '3px solid #10B981',
          backgroundColor: '#ECFDF5',
          titleColor: '#065F46',
          timeColor: '#059669',
        };
      case 'ENTERTAINMENT':
        return {
          borderLeft: '3px solid #F59E0B',
          backgroundColor: '#FFFBEB',
          titleColor: '#92400E',
          timeColor: '#D97706',
        };
      default:
        return {
          borderLeft: '3px solid #6C5CE7',
          backgroundColor: '#F0EDFF',
          titleColor: '#5A4BD1',
          timeColor: '#6C5CE7',
        };
    }
  };

  // Filter blocks
  const filteredBlocks = timeBlocks.filter((b) => {
    if (statusFilter !== 'ALL') {
      if (statusFilter === 'OVERDUE') {
        const isPast = b.status === 'OVERDUE' || (b.status !== 'COMPLETED' && new Date(b.endTime || b.startTime).getTime() < Date.now());
        if (!isPast) return false;
      } else if (b.status !== statusFilter) {
        return false;
      }
    }
    if (selectedCategory) {
      const cat = categories.find((c) => c.name === selectedCategory);
      if (cat && b.categoryType !== cat.type) return false;
    }
    return true;
  });

  return (
    <div className="weekly-calendar">
      {/* ── Days Header Wrapper (Khớp 100% cột dọc với bảng giờ bên dưới, bù chính xác bề rộng thanh cuộn scrollbar) ── */}
      <div
        className="calendar-header-wrapper"
        style={{
          display: 'flex',
          width: '100%',
          borderBottom: '1px solid var(--border)',
          flexShrink: 0,
          boxSizing: 'border-box',
        }}
      >
        <div
          className="calendar-header"
          style={{
            flex: 1,
            display: 'grid',
            gridTemplateColumns: `60px repeat(${DAYS.length}, minmax(0, 1fr))`,
            borderBottom: 'none',
          }}
        >
          <div className="time-gutter-header">GMT+7</div>
          {DAYS.map((day) => (
            <div key={day.dateStr} className={`day-header ${day.isToday ? 'today' : ''}`}>
              <span className="day-name">{day.name}</span>
              <span className={`day-number ${day.isToday ? 'today-number' : ''}`}>
                {day.dateNum}
              </span>
            </div>
          ))}
        </div>
        {scrollbarWidth > 0 && (
          <div
            className="calendar-scrollbar-spacer"
            style={{
              width: `${scrollbarWidth}px`,
              flexShrink: 0,
              borderLeft: '1px solid var(--border-light)',
            }}
          />
        )}
      </div>

      {/* ── Scrollable Grid Body ── */}
      <div className="calendar-scroll" ref={scrollRef}>
        <div className="calendar-body">
          {/* Time Gutter (0:00 to 23:59) */}
          <div className="time-gutter">
            {HOURS.map((hour) => (
              <div key={hour} className="time-label">
                {hour.toString().padStart(2, '0')}:00
              </div>
            ))}
            <div className="time-label time-label-end" style={{ height: 24, fontSize: '0.68rem', color: 'var(--text-muted)' }}>
              23:59
            </div>
          </div>

          {/* Days Columns */}
          <div className="days-grid" style={{ gridTemplateColumns: `repeat(${DAYS.length}, minmax(0, 1fr))` }}>
            {DAYS.map((day, dayIndex) => {
              // Find events on this day
              const dayBlocks = filteredBlocks.filter((b) => {
                const d = new Date(b.startTime);
                const y = d.getFullYear();
                const m = String(d.getMonth() + 1).padStart(2, '0');
                const date = String(d.getDate()).padStart(2, '0');
                return `${y}-${m}-${date}` === day.dateStr;
              });

              return (
                <div
                  key={day.dateStr}
                  className={`day-column ${day.isToday ? 'today-column' : ''}`}
                >
                  {/* Hour cells (clickable to add task, droppable for rescheduling) */}
                  {HOURS.map((hour) => (
                    <div
                      key={hour}
                      className="hour-cell"
                      onClick={() => onAddAtSlot(dayIndex, hour)}
                      onDragOver={(e) => {
                        e.preventDefault();
                        e.dataTransfer.dropEffect = 'move';
                        e.currentTarget.classList.add('drop-target');
                      }}
                      onDragLeave={(e) => {
                        e.currentTarget.classList.remove('drop-target');
                      }}
                      onDrop={(e) => {
                        e.preventDefault();
                        e.currentTarget.classList.remove('drop-target');
                        setDraggingBlockId(null);
                        try {
                          const raw = e.dataTransfer.getData('text/plain');
                          if (!raw) return;
                          const { id, durationMs } = JSON.parse(raw);
                          const targetDate = day.dateStr;
                          const startHourStr = hour.toString().padStart(2, '0');
                          const newStartIso = `${targetDate}T${startHourStr}:00:00+07:00`;

                          const startDate = new Date(newStartIso);
                          const endDate = new Date(startDate.getTime() + (durationMs || 3600000));
                          let endHourStr = endDate.getHours().toString().padStart(2, '0');
                          let endMinStr = endDate.getMinutes().toString().padStart(2, '0');
                          if (hour === 23 && endDate.getDate() !== startDate.getDate()) {
                            endHourStr = '23';
                            endMinStr = '59';
                          }
                          const newEndIso = `${targetDate}T${endHourStr}:${endMinStr}:00+07:00`;

                          const targetBlock = timeBlocks.find((b) => b.id === id);
                          const validation = checkWorkHoursConstraint(newStartIso, newEndIso, targetBlock?.categoryType || 'WORK');
                          if (validation.isOutsideWorkHours) {
                            const confirmed = window.confirm(`${validation.reason}\n\n${validation.promptMessage}`);
                            if (!confirmed) {
                              setDraggingBlockId(null);
                              return;
                            }
                          }

                          if (onRescheduleTask) {
                            setDraggingBlockId(null);
                            onRescheduleTask(id, newStartIso, newEndIso);
                          }
                        } catch (err) {
                          console.error('Error handling task drop:', err);
                        } finally {
                          setDraggingBlockId(null);
                        }
                      }}
                      title={`Nhấp để thêm công việc lúc ${hour}:00 · Hoặc thả công việc vào đây để dời giờ`}
                    />
                  ))}

                  {/* Current GMT+7 time indicator line on today */}
                  {day.isToday && currentTimeTop !== null && (
                    <div
                      style={{
                        position: 'absolute',
                        top: `${currentTimeTop}px`,
                        left: 0,
                        right: 0,
                        height: 2,
                        backgroundColor: '#EF4444',
                        zIndex: 25,
                        pointerEvents: 'none',
                        display: 'flex',
                        alignItems: 'center',
                      }}
                      title={`Thời gian hiện tại: ${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')} (GMT+7)`}
                    >
                      <div
                        style={{
                          width: 8,
                          height: 8,
                          borderRadius: '50%',
                          backgroundColor: '#EF4444',
                          marginLeft: -4,
                          boxShadow: '0 0 0 2px rgba(239,68,68,0.25)',
                        }}
                      />
                    </div>
                  )}

                  {/* Event Blocks - Tự động chia cột bên cạnh nhau khi trùng giờ, không chèn đè lên nhau */}
                  {computeLayoutBlocks(dayBlocks).map(({ block, top, height, durationMs, colIndex, totalCols }) => {
                    const start = new Date(block.startTime);
                    const end = new Date(block.endTime);
                    const styles = getCategoryStyles(block.categoryType, block.colorOverride);
                    const timeRangeStr = `${start.getHours().toString().padStart(2, '0')}:${start.getMinutes().toString().padStart(2, '0')}–${end.getHours().toString().padStart(2, '0')}:${end.getMinutes().toString().padStart(2, '0')}`;

                    const isShortBlock = height <= 44;
                    const isSuperTiny = height < 26;
                    const isHovered = hoveredBlockId === block.id || duplicateBlockId === block.id;
                    const isDragging = draggingBlockId === block.id;

                    const colWidthPercent = 100 / totalCols;
                    const leftPercent = colIndex * colWidthPercent;

                    // Tính toán độ scale linh động, vừa khít nội dung khi hover (đảm bảo không bao giờ tràn mép)
                    let calculatedTop = top;
                    let calculatedLeft: string | undefined = '3px';
                    let calculatedRight: string | undefined = 'auto';
                    let calculatedWidth = 'calc(100% - 6px)';
                    // CHỈ card ở cột cuối cùng sát mép phải (Chủ Nhật hoặc cột ngoài cùng bên phải) mới hiển thị lệch vào
                    const isRightmostEdge = DAYS.length > 1 && dayIndex === DAYS.length - 1;
                    const hoverCardWidth = 220;

                    const isScheduleUnlocked = isPrivacyUnlocked || Boolean(unlockedScheduleIds && unlockedScheduleIds.has(block.id));
                    const isLockedSchedule = Boolean(block.isLocked && !isScheduleUnlocked);

                    if (isHovered && !isLockedSchedule) {
                      calculatedTop = Math.max(0, top - 4);
                      calculatedWidth = `${hoverCardWidth}px`;

                      if (DAYS.length === 1) {
                        // Chế độ xem 1 ngày: cột rất rộng
                        if (totalCols > 1) {
                          calculatedLeft = `calc(${leftPercent}% + 2px)`;
                          calculatedRight = 'auto';
                          calculatedWidth = `max(calc(${colWidthPercent}% - 4px), ${hoverCardWidth}px)`;
                        } else {
                          calculatedLeft = '3px';
                          calculatedRight = 'auto';
                          calculatedWidth = 'calc(100% - 6px)';
                        }
                      } else if (isRightmostEdge) {
                        // CHỈ CARD CUỐI BÊN PHẢI (Chủ Nhật sát mép phải):
                        // Luôn neo mép phải '2px', mở rộng lệch sang TRÁI -> 100% không bao giờ tràn mép phải lịch!
                        calculatedRight = '2px';
                        calculatedLeft = 'auto';
                      } else if (dayIndex === 0) {
                        // Sát mép trái (Thứ Hai):
                        // Luôn neo mép trái '2px', mở rộng sang PHẢI -> 100% không bao giờ tràn vào cột giờ bên trái!
                        calculatedLeft = '2px';
                        calculatedRight = 'auto';
                      } else {
                        // Tất cả các ngày ở giữa (Thứ Ba, Thứ Tư, Thứ Năm, Thứ Sáu, Thứ Bảy):
                        // Căn giữa cột ngày để nở đều 2 bên, không bị lệch vào mép
                        calculatedLeft = `calc(50% - ${hoverCardWidth / 2}px)`;
                        calculatedRight = 'auto';
                      }
                    } else {
                      if (totalCols > 1) {
                        calculatedLeft = `calc(${leftPercent}% + 2px)`;
                        calculatedRight = 'auto';
                        calculatedWidth = `calc(${colWidthPercent}% - 4px)`;
                      } else {
                        calculatedLeft = '3px';
                        calculatedRight = 'auto';
                        calculatedWidth = 'calc(100% - 6px)';
                      }
                    }

                    if (isLockedSchedule) {
                      // Placement: Nếu card ở sát đỉnh lịch (< 170px) -> đặt popup bên dưới ('bottom'), mũi tên ở trên chĩa lên
                      // Nếu card ở dưới -> đặt popup bên trên ('top'), mũi tên ở dưới chĩa xuống
                      const isNearTop = calculatedTop < 170;
                      const dynamicPlacement: 'top' | 'bottom' = isNearTop ? 'bottom' : 'top';

                      // ArrowAlign: Linh động theo cột của ngày để mũi tên luôn chỉ vào card và không bao giờ tràn lề màn hình
                      let dynamicArrowAlign: 'start' | 'center' | 'end' = 'center';
                      if (DAYS.length === 1) {
                        if (totalCols > 1) {
                          dynamicArrowAlign = colIndex === 0 ? 'start' : (colIndex === totalCols - 1 ? 'end' : 'center');
                        } else {
                          dynamicArrowAlign = 'center';
                        }
                      } else {
                        // 7-day week view:
                        // Các ngày đầu tuần (Thứ Hai, Thứ Ba): Mũi tên lệch trái ('start'), thân bubble bung sang phải
                        if (dayIndex <= 1) {
                          dynamicArrowAlign = 'start';
                        }
                        // Các ngày cuối tuần (Thứ Bảy, Chủ Nhật): Mũi tên lệch phải ('end'), thân bubble bung sang trái
                        else if (dayIndex >= DAYS.length - 2) {
                          dynamicArrowAlign = 'end';
                        }
                        // Các ngày giữa tuần (Thứ Tư, Thứ Năm, Thứ Sáu): Căn giữa ('center')
                        else {
                          if (totalCols > 1) {
                            if (colIndex === 0 && dayIndex === 2) dynamicArrowAlign = 'start';
                            else if (colIndex === totalCols - 1 && dayIndex === 4) dynamicArrowAlign = 'end';
                            else dynamicArrowAlign = 'center';
                          } else {
                            dynamicArrowAlign = 'center';
                          }
                        }
                      }

                      const isLockPopoverOpen = activeLockedBlockId === block.id;

                      return (
                        <div
                          key={block.id}
                          data-task-id={block.id}
                          draggable={false}
                          className={`event-block locked-schedule-card ${focusedTaskId === block.id ? 'is-search-target' : ''} ${isShortBlock ? 'short-block' : ''} ${isHovered ? 'is-hovered' : ''}`}
                          style={{
                            position: 'absolute',
                            top: `${calculatedTop}px`,
                            height: `${height}px`,
                            minHeight: 32,
                            left: calculatedLeft,
                            right: calculatedRight,
                            width: calculatedWidth,
                            zIndex: isLockPopoverOpen ? 9999 : (isHovered ? 200 : (2 + colIndex)),
                            padding: height <= 44 ? '2px 4px' : (height < 75 ? '4px 6px' : '6px 8px'),
                            boxSizing: 'border-box',
                            borderRadius: '8px',
                            display: 'flex',
                            flexDirection: 'column',
                            justifyContent: 'center',
                            alignItems: 'center',
                            cursor: 'pointer',
                            overflow: 'hidden',
                          }}
                          onMouseEnter={(e) => {
                            setHoveredBlockId(block.id);
                            setHoveredBlockRect(e.currentTarget.getBoundingClientRect());
                          }}
                          onMouseLeave={() => {
                            setHoveredBlockId(null);
                            setHoveredBlockRect(null);
                          }}
                          onClick={(e) => {
                            e.stopPropagation();
                            const rect = e.currentTarget.getBoundingClientRect();
                            setActiveLockedBlockId(block.id);
                            setActiveLockedBlockRect(rect);
                            setHoveredBlockId(null);
                            setHoveredBlockRect(null);
                            onUnlockPrivacy?.();
                          }}
                          title="Lịch trình riêng tư đã bị khóa. Nhấp để mở khóa lịch trình này!"
                        >
                          {/* Nội dung bên trong thẻ ĐÃ KHÓA: Thẻ hẹp (tuần 7 ngày) hoặc ngắn thì chỉ hiện Đã khóa + Thời gian */}
                          {(height < 100 || DAYS.length > 3 || totalCols > 1) ? (
                            /* 1. Thẻ hẹp (chế độ tuần) hoặc ngắn: Hiển thị gọn gàng sạch sẽ Đã khóa và Thời gian */
                            <div
                              style={{
                                display: 'flex',
                                flexDirection: 'column',
                                alignItems: 'center',
                                justifyContent: 'center',
                                width: '100%',
                                minWidth: 0,
                                gap: 3,
                                padding: '2px 4px',
                                boxSizing: 'border-box',
                              }}
                            >
                              <span
                                className="locked-badge-pill"
                                style={{
                                  display: 'inline-flex',
                                  alignItems: 'center',
                                  justifyContent: 'center',
                                  gap: 4,
                                  fontSize: height < 55 ? '0.65rem' : '0.70rem',
                                  color: '#EF4444',
                                  background: 'rgba(239, 68, 68, 0.12)',
                                  border: '1px solid rgba(239, 68, 68, 0.25)',
                                  padding: '1px 6px',
                                  borderRadius: 4,
                                  fontWeight: 700,
                                  maxWidth: '100%',
                                  overflow: 'hidden',
                                  textOverflow: 'ellipsis',
                                  whiteSpace: 'nowrap',
                                }}
                              >
                                <EyeLockIcon locked={true} size={11} color="#EF4444" />
                                <span>Đã khóa</span>
                              </span>
                              <span
                                style={{
                                  fontSize: height < 55 ? '0.65rem' : '0.70rem',
                                  fontWeight: 600,
                                  opacity: 0.85,
                                  whiteSpace: 'nowrap',
                                  overflow: 'hidden',
                                  textOverflow: 'ellipsis',
                                  maxWidth: '100%',
                                }}
                              >
                                {timeRangeStr}
                              </span>
                            </div>
                          ) : (
                            /* 2. Thẻ rộng & cao (ví dụ chế độ Ngày, width rộng và height >= 100px): Hiển thị đầy đủ Badge + Thời gian + Dòng thông báo */
                            <div
                              style={{
                                display: 'flex',
                                flexDirection: 'column',
                                alignItems: 'center',
                                justifyContent: 'center',
                                width: '100%',
                                minWidth: 0,
                                gap: 4,
                                padding: '4px 6px',
                                boxSizing: 'border-box',
                              }}
                            >
                              <span
                                className="locked-badge-pill"
                                style={{
                                  display: 'inline-flex',
                                  alignItems: 'center',
                                  justifyContent: 'center',
                                  gap: 5,
                                  fontSize: '0.74rem',
                                  color: '#EF4444',
                                  background: 'rgba(239, 68, 68, 0.12)',
                                  border: '1px solid rgba(239, 68, 68, 0.25)',
                                  padding: '2px 7px',
                                  borderRadius: 5,
                                  fontWeight: 700,
                                  maxWidth: '100%',
                                  overflow: 'hidden',
                                  textOverflow: 'ellipsis',
                                  whiteSpace: 'nowrap',
                                }}
                              >
                                <EyeLockIcon locked={true} size={11} color="#EF4444" />
                                <span>Đã khóa</span>
                              </span>
                              <span
                                style={{
                                  fontSize: '0.72rem',
                                  fontWeight: 600,
                                  opacity: 0.85,
                                  whiteSpace: 'nowrap',
                                  overflow: 'hidden',
                                  textOverflow: 'ellipsis',
                                  maxWidth: '100%',
                                }}
                              >
                                {timeRangeStr}
                              </span>
                              <div
                                style={{
                                  display: '-webkit-box',
                                  WebkitLineClamp: 2,
                                  WebkitBoxOrient: 'vertical',
                                  overflow: 'hidden',
                                  textOverflow: 'ellipsis',
                                  textAlign: 'center',
                                  fontSize: '0.68rem',
                                  color: isHovered ? 'var(--primary, #6C5CE7)' : 'var(--text-muted, #64748B)',
                                  fontWeight: 600,
                                  lineHeight: 1.25,
                                  padding: '0 2px',
                                  maxWidth: '100%',
                                  marginTop: 2,
                                }}
                              >
                                Vui lòng nhập mật khẩu để mở khóa lịch trình này!
                              </div>
                            </div>
                          )}

                          {/* Hover Callout Tooltip - Mở từng lịch trình */}
                          {isHovered && !isLockPopoverOpen && (
                            <PrivacyCalloutTooltip
                              title="Vui lòng nhập mật khẩu để mở khóa lịch trình này!"
                              targetRect={hoveredBlockRect}
                              placement={dynamicPlacement}
                              arrowAlign={dynamicArrowAlign}
                              onClick={(e) => {
                                e.stopPropagation();
                                setActiveLockedBlockId(block.id);
                                setActiveLockedBlockRect(hoveredBlockRect);
                                setHoveredBlockId(null);
                                setHoveredBlockRect(null);
                                onUnlockPrivacy?.();
                              }}
                            />
                          )}

                          {/* Click Popover - Mở khóa riêng cho lịch trình này */}
                          {isLockPopoverOpen && (
                            <PrivacyCalloutPopover
                              isOpen={true}
                              title="Vui lòng nhập mật khẩu để mở khóa lịch trình này!"
                              targetRect={activeLockedBlockRect}
                              placement={dynamicPlacement}
                              arrowAlign={dynamicArrowAlign}
                              onClose={() => {
                                setActiveLockedBlockId(null);
                                setActiveLockedBlockRect(null);
                              }}
                              onSuccess={() => {
                                const idToUnlock = activeLockedBlockId;
                                setActiveLockedBlockId(null);
                                setActiveLockedBlockRect(null);
                                if (idToUnlock && onUnlockSingle) {
                                  onUnlockSingle(idToUnlock);
                                } else {
                                  onUnlockSuccess?.();
                                }
                              }}
                            />
                          )}
                        </div>
                      );
                    }

                    return (
                      <div
                        key={block.id}
                        data-task-id={block.id}
                        draggable={true}
                        onDragStart={(e) => {
                          e.dataTransfer.setData(
                            'text/plain',
                            JSON.stringify({ id: block.id, durationMs })
                          );
                          e.dataTransfer.effectAllowed = 'move';
                          setDraggingBlockId(block.id);
                        }}
                        onDragEnd={() => {
                          setDraggingBlockId(null);
                        }}
                        className={`event-block ${focusedTaskId === block.id ? 'is-search-target' : ''} ${isShortBlock ? 'short-block' : ''} ${isSuperTiny ? 'super-tiny-block' : ''} ${isHovered ? 'is-hovered' : ''} ${isDragging ? 'is-dragging' : ''}`}
                        style={{
                          top: `${calculatedTop}px`,
                          height: isHovered ? 'auto' : `${height}px`,
                          minHeight: undefined,
                          left: calculatedLeft,
                          right: calculatedRight,
                          width: calculatedWidth,
                          minWidth: isHovered ? '236px' : undefined,
                          maxWidth: isHovered ? '250px' : undefined,
                          zIndex: isDragging ? 120 : (duplicateBlockId === block.id ? 9999 : (isHovered ? 100 : 2 + colIndex)),
                          borderLeft: styles.borderLeft,
                          backgroundColor: styles.backgroundColor,
                          boxSizing: 'border-box',
                        }}
                        onMouseEnter={() => setHoveredBlockId(block.id)}
                        onMouseLeave={() => setHoveredBlockId(null)}
                        onClick={(e) => {
                          e.stopPropagation();
                          onSelectBlock(block);
                        }}
                        title={`${block.title} (${timeRangeStr})${block.note ? `\n💬 ${block.note}` : ''} · Kéo thả để dời giờ`}
                      >
                        {/* 1. Tên công việc & Icon ổ khóa bên cạnh dấu tick tròn */}
                        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 4 }}>
                          <div className="event-title" style={{ color: styles.titleColor, flex: 1, fontWeight: 700 }}>
                            {block.title}
                          </div>
                          <div style={{ display: 'flex', alignItems: 'center', gap: 4, flexShrink: 0 }}>
                            {block.isLocked && (
                              <button
                                type="button"
                                onClick={(e) => {
                                  e.stopPropagation();
                                  onTogglePrivacyLock?.();
                                }}
                                className="event-card-lock-btn is-locked"
                                title="Khóa lại để ẩn lịch trình này"
                                style={{
                                  display: 'inline-flex',
                                  alignItems: 'center',
                                  justifyContent: 'center',
                                  width: height <= 55 ? 18 : 20,
                                  height: height <= 55 ? 18 : 20,
                                  borderRadius: 6,
                                  border: 'none',
                                  background: 'rgba(239, 68, 68, 0.14)',
                                  color: '#EF4444',
                                  cursor: 'pointer',
                                  padding: 0,
                                  transition: 'transform 0.2s cubic-bezier(0.16, 1, 0.3, 1), box-shadow 0.2s cubic-bezier(0.16, 1, 0.3, 1)',
                                  transform: isHovered ? 'scale(1.18) translateY(-1px)' : 'scale(1)',
                                  boxShadow: isHovered ? '0 2px 8px rgba(0,0,0,0.18)' : 'none',
                                }}
                              >
                                <EyeLockIcon
                                  locked={true}
                                  size={height <= 55 ? 12 : 14}
                                  color="#EF4444"
                                />
                              </button>
                            )}

                            {block.status === 'COMPLETED' && (
                              <span
                                title="Đã hoàn thành"
                                style={{
                                  display: 'inline-flex',
                                  alignItems: 'center',
                                  justifyContent: 'center',
                                  width: height <= 55 ? 16 : 18,
                                  height: height <= 55 ? 16 : 18,
                                  borderRadius: '50%',
                                  backgroundColor: '#10B981',
                                  color: '#FFFFFF',
                                  flexShrink: 0,
                                  boxShadow: '0 1px 3px rgba(16, 185, 129, 0.35)',
                                }}
                              >
                                <Check size={height <= 55 ? 10 : 12} strokeWidth={3.2} />
                              </span>
                            )}
                          </div>
                        </div>

                        {/* 2. Thời gian: luôn hiển thị dưới tên công việc */}
                        <div className="event-time" style={{ color: styles.timeColor }}>
                          {timeRangeStr}
                          {block.isRecurring && (
                            <RotateCw
                              size={10}
                              style={{ marginLeft: 4, display: 'inline', verticalAlign: '-1px' }}
                            />
                          )}
                        </div>

                        {/* 3. Chú thích: Khi thẻ ngắn (<90px), chỉ hiện chú thích khi hover bung to ra để không làm chật thẻ */}
                        {block.note && block.note.trim() ? (
                          <div
                            className={`event-note ${height < 90 ? 'hover-only-note' : ''}`}
                            style={{ color: styles.timeColor }}
                            title={block.note}
                          >
                            <span style={{ fontStyle: 'normal', marginRight: 3 }}>💬</span>
                            <span>{block.note.trim()}</span>
                          </div>
                        ) : null}

                        {/* 4. Trạng thái / Subtext badge:
                            Chỉ hiện badge text đầy đủ ở đáy khi thẻ đủ cao (>55px) hoặc khi đang hover bung to.
                            Với thẻ thấp (<=55px như ảnh 1), dấu tick V tròn màu xanh đã nằm gọn gàng bên phải tiêu đề! */}
                        {block.subtext && (isHovered || height > 55) && (
                          <div
                            className={`event-status ${
                              block.status === 'COMPLETED'
                                ? 'status-completed'
                                : block.status === 'IN_PROGRESS'
                                ? 'status-in_progress'
                                : 'status-todo'
                            }`}
                            style={{ marginTop: 2 }}
                          >
                            {block.subtext}
                          </div>
                        )}

                        {/* 5. Micro Action Buttons Row on Hover: Luôn hiển thị Xóa, Nhân bản, Hoàn tất, và nếu lịch đã mở khóa thì kèm thêm nút Khóa lại */}
                        <div
                          className="event-hover-bottom-bar"
                          draggable={false}
                          onMouseDown={(e) => e.stopPropagation()}
                          onPointerDown={(e) => e.stopPropagation()}
                          style={{
                            display: 'flex',
                            alignItems: 'center',
                            width: '100%',
                            boxSizing: 'border-box',
                            gap: 3,
                            marginTop: 6,
                            paddingTop: 6,
                            borderTop: '1px dashed rgba(0,0,0,0.14)',
                          }}
                          onClick={(e) => e.stopPropagation()}
                        >
                          <button
                            type="button"
                            draggable={false}
                            onMouseDown={(e) => e.stopPropagation()}
                            onPointerDown={(e) => e.stopPropagation()}
                            onClick={(e) => {
                              e.stopPropagation();
                              handleDeleteBlock(e, block);
                            }}
                            className="event-btn-action event-btn-delete"
                            style={{ flex: '0 0 28px', minWidth: 0, padding: '3px 4px', fontSize: '0.68rem' }}
                            title="Xóa công việc khỏi lịch"
                            aria-label="Xóa công việc"
                          >
                            <Trash2 size={11} style={{ flexShrink: 0 }} />
                          </button>

                          <button
                            type="button"
                            draggable={false}
                            onMouseDown={(e) => e.stopPropagation()}
                            onPointerDown={(e) => e.stopPropagation()}
                            onClick={(e) => {
                              e.stopPropagation();
                              handleOpenDuplicate(e, block);
                            }}
                            className="event-btn-action event-btn-duplicate"
                            style={{ flex: '0 0 28px', minWidth: 0, padding: '3px 4px', fontSize: '0.68rem' }}
                            title="Nhân bản công việc sang các ngày tiếp theo"
                            aria-label="Nhân bản công việc"
                          >
                            <Copy size={11} style={{ flexShrink: 0 }} />
                          </button>

                          <button
                            type="button"
                            draggable={false}
                            onMouseDown={(e) => e.stopPropagation()}
                            onPointerDown={(e) => e.stopPropagation()}
                            onClick={(e) => {
                              e.stopPropagation();
                              handleToggleComplete(e, block);
                            }}
                            className={`event-btn-action event-btn-complete ${block.status === 'COMPLETED' ? 'is-done' : ''}`}
                            style={{ flex: '1.2 1 0', minWidth: 0, padding: '3px 4px', fontSize: '0.68rem', color: block.status === 'COMPLETED' ? '#059669' : undefined, fontWeight: block.status === 'COMPLETED' ? 700 : undefined }}
                            title="Đánh dấu hoàn tất công việc"
                          >
                            <Check size={11} style={{ flexShrink: 0 }} />
                            <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                              {block.status === 'COMPLETED' ? 'Đã xong' : 'Hoàn tất'}
                            </span>
                          </button>

                          {block.isLocked && isScheduleUnlocked && (
                            <button
                              type="button"
                              draggable={false}
                              onMouseDown={(e) => e.stopPropagation()}
                              onPointerDown={(e) => e.stopPropagation()}
                              onClick={(e) => {
                                e.stopPropagation();
                                handleToggleLockTask(e, block);
                              }}
                              className="event-btn-action event-btn-unlock"
                              style={{
                                flex: '1.2 1 0',
                                minWidth: 0,
                                padding: '3px 4px',
                                fontSize: '0.68rem',
                                color: '#10B981',
                                fontWeight: 700,
                                background: 'rgba(16, 185, 129, 0.12)',
                                border: '1px solid rgba(16, 185, 129, 0.35)',
                              }}
                              title="Mở khóa (hủy chế độ riêng tư cho lịch trình này)"
                            >
                              <EyeLockIcon locked={false} size={11} color="#10B981" />
                              <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>Mở khóa</span>
                            </button>
                          )}
                        </div>

                        {/* 6. Quick Duplicate Flyout Popover (Cấu hình riêng từng Thứ & Phân loại công việc) */}
                        {duplicateBlockId === block.id && (
                          <div
                            className="duplicate-popover-flyout"
                            onClick={(e) => e.stopPropagation()}
                            onMouseDown={(e) => e.stopPropagation()}
                            style={{
                              position: 'absolute',
                              ...(DAYS.length === 1
                                ? { top: 'calc(100% + 6px)', left: 0 }
                                : dayIndex >= DAYS.length - 2
                                ? { right: 'calc(100% + 6px)', ...(top > 1050 ? { bottom: 0 } : { top: 0 }) }
                                : { left: 'calc(100% + 6px)', ...(top > 1050 ? { bottom: 0 } : { top: 0 }) }),
                              width: 272,
                              zIndex: 99999,
                              background: 'var(--surface)',
                              borderRadius: 14,
                              border: '1.5px solid #FCD34D',
                              boxShadow: '0 16px 36px -6px rgba(217, 119, 6, 0.35), 0 4px 16px rgba(0,0,0,0.18)',
                              padding: '10px 10px 8px',
                              animation: 'popoverFadeIn 0.16s cubic-bezier(0.16, 1, 0.3, 1)',
                              cursor: 'default',
                            }}
                          >
                            {/* Popover Header */}
                            <div
                              style={{
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'space-between',
                                marginBottom: 8,
                                paddingBottom: 6,
                                borderBottom: '1px solid #FEF3C7',
                              }}
                            >
                              <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: '0.78rem', fontWeight: 800, color: '#B45309' }}>
                                <Copy size={13} />
                                <span>Nhân bản lịch trình</span>
                              </div>
                              <button
                                type="button"
                                onClick={(e) => {
                                  e.stopPropagation();
                                  setDuplicateBlockId(null);
                                }}
                                style={{
                                  background: 'transparent',
                                  border: 'none',
                                  color: 'var(--text-muted)',
                                  cursor: 'pointer',
                                  padding: 2,
                                  display: 'flex',
                                  alignItems: 'center',
                                  borderRadius: 4,
                                }}
                                title="Đóng"
                              >
                                <X size={14} />
                              </button>
                            </div>

                            {/* 3 Tabs: Theo Thứ (Mặc định) vs Theo tuần vs Ngày mai */}
                            <div
                              style={{
                                display: 'flex',
                                gap: 3,
                                background: 'rgba(0,0,0,0.05)',
                                padding: 2,
                                borderRadius: 8,
                                marginBottom: 8,
                              }}
                            >
                              <button
                                type="button"
                                onClick={(e) => {
                                  e.stopPropagation();
                                  setDuplicateMode('days');
                                }}
                                style={{
                                  flex: 1.2,
                                  padding: '4px 6px',
                                  fontSize: '0.69rem',
                                  fontWeight: duplicateMode === 'days' ? 800 : 600,
                                  borderRadius: 6,
                                  border: 'none',
                                  background: duplicateMode === 'days' ? '#FFFFFF' : 'transparent',
                                  color: duplicateMode === 'days' ? '#B45309' : 'var(--text-muted)',
                                  cursor: 'pointer',
                                  boxShadow: duplicateMode === 'days' ? '0 1px 3px rgba(0,0,0,0.1)' : 'none',
                                  transition: 'all 0.15s ease',
                                }}
                              >
                                Theo Thứ
                              </button>
                              <button
                                type="button"
                                onClick={(e) => {
                                  e.stopPropagation();
                                  setDuplicateMode('week');
                                  setCustomUnitsCount(1);
                                }}
                                style={{
                                  flex: 1,
                                  padding: '4px 6px',
                                  fontSize: '0.69rem',
                                  fontWeight: duplicateMode === 'week' ? 800 : 600,
                                  borderRadius: 6,
                                  border: 'none',
                                  background: duplicateMode === 'week' ? '#FFFFFF' : 'transparent',
                                  color: duplicateMode === 'week' ? '#B45309' : 'var(--text-muted)',
                                  cursor: 'pointer',
                                  boxShadow: duplicateMode === 'week' ? '0 1px 3px rgba(0,0,0,0.1)' : 'none',
                                  transition: 'all 0.15s ease',
                                }}
                              >
                                Theo tuần
                              </button>
                              <button
                                type="button"
                                onClick={(e) => {
                                  e.stopPropagation();
                                  setDuplicateMode('day');
                                  setCustomUnitsCount(1);
                                }}
                                style={{
                                  flex: 1,
                                  padding: '4px 6px',
                                  fontSize: '0.69rem',
                                  fontWeight: duplicateMode === 'day' ? 800 : 600,
                                  borderRadius: 6,
                                  border: 'none',
                                  background: duplicateMode === 'day' ? '#FFFFFF' : 'transparent',
                                  color: duplicateMode === 'day' ? '#B45309' : 'var(--text-muted)',
                                  cursor: 'pointer',
                                  boxShadow: duplicateMode === 'day' ? '0 1px 3px rgba(0,0,0,0.1)' : 'none',
                                  transition: 'all 0.15s ease',
                                }}
                              >
                                Ngày mai
                              </button>
                            </div>

                            {/* Tab Body */}
                            {duplicateMode === 'days' ? (
                              <div style={{ display: 'flex', flexDirection: 'column', gap: 7 }}>
                                {/* 1. Phân loại công việc */}
                                <div>
                                  <div style={{ fontSize: '0.68rem', fontWeight: 700, color: 'var(--text-3)', marginBottom: 4, textTransform: 'uppercase', letterSpacing: 0.3 }}>
                                    Phân loại công việc:
                                  </div>
                                  <div style={{ display: 'flex', gap: 4, flexWrap: 'wrap' }}>
                                    {(categories && categories.length > 0 ? categories.slice(0, 5) : [
                                      { id: 2, name: 'Công việc', type: 'WORK' as const, colorCode: '#6C5CE7' },
                                      { id: 1, name: 'Học tập', type: 'STUDY' as const, colorCode: '#3B82F6' },
                                      { id: 3, name: 'Sức khỏe', type: 'HEALTH' as const, colorCode: '#10B981' },
                                      { id: 4, name: 'Giải trí', type: 'ENTERTAINMENT' as const, colorCode: '#F59E0B' },
                                    ]).map((cat: any) => {
                                      const catType = cat.type || cat.categoryType || cat.name;
                                      const isCatActive = duplicateCategoryType === catType || duplicateCategoryType === cat.name;
                                      return (
                                        <button
                                          key={cat.id}
                                          type="button"
                                          onClick={(e) => {
                                            e.stopPropagation();
                                            setDuplicateCategoryType(catType);
                                          }}
                                          style={{
                                            display: 'flex',
                                            alignItems: 'center',
                                            gap: 4,
                                            padding: '3px 7px',
                                            borderRadius: 6,
                                            border: isCatActive ? `1.5px solid ${cat.colorCode}` : '1px solid var(--border)',
                                            background: isCatActive ? `${cat.colorCode}22` : 'var(--bg)',
                                            color: isCatActive ? cat.colorCode : 'var(--text)',
                                            fontSize: '0.68rem',
                                            fontWeight: isCatActive ? 800 : 500,
                                            cursor: 'pointer',
                                            transition: 'all 0.15s ease',
                                          }}
                                        >
                                          <span style={{ width: 6, height: 6, borderRadius: '50%', backgroundColor: cat.colorCode }} />
                                          <span>{cat.name}</span>
                                        </button>
                                      );
                                    })}
                                  </div>
                                </div>

                                {/* 2. Cài đặt các ngày Thứ */}
                                <div>
                                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 4 }}>
                                    <span style={{ fontSize: '0.68rem', fontWeight: 700, color: 'var(--text-3)', textTransform: 'uppercase', letterSpacing: 0.3 }}>
                                      Cài đặt ngày Thứ:
                                    </span>
                                    <div style={{ display: 'flex', gap: 3 }}>
                                      <button
                                        type="button"
                                        onClick={(e) => {
                                          e.stopPropagation();
                                          setSelectedDaysOfWeek([1, 3, 5]);
                                        }}
                                        style={{
                                          padding: '2px 5px',
                                          fontSize: '0.62rem',
                                          borderRadius: 4,
                                          border: '1px solid #FCD34D',
                                          background: '#FEF3C7',
                                          color: '#B45309',
                                          fontWeight: 700,
                                          cursor: 'pointer',
                                        }}
                                        title="Thứ Hai, Thứ Tư, Thứ Sáu"
                                      >
                                        2-4-6
                                      </button>
                                      <button
                                        type="button"
                                        onClick={(e) => {
                                          e.stopPropagation();
                                          setSelectedDaysOfWeek([2, 4, 6]);
                                        }}
                                        style={{
                                          padding: '2px 5px',
                                          fontSize: '0.62rem',
                                          borderRadius: 4,
                                          border: '1px solid #FCD34D',
                                          background: '#FEF3C7',
                                          color: '#B45309',
                                          fontWeight: 700,
                                          cursor: 'pointer',
                                        }}
                                        title="Thứ Ba, Thứ Năm, Thứ Bảy"
                                      >
                                        3-5-7
                                      </button>
                                      <button
                                        type="button"
                                        onClick={(e) => {
                                          e.stopPropagation();
                                          setSelectedDaysOfWeek([1, 2, 3, 4, 5]);
                                        }}
                                        style={{
                                          padding: '2px 5px',
                                          fontSize: '0.62rem',
                                          borderRadius: 4,
                                          border: '1px solid #BFDBFE',
                                          background: '#EFF6FF',
                                          color: '#1D4ED8',
                                          fontWeight: 700,
                                          cursor: 'pointer',
                                        }}
                                        title="Thứ Hai đến Thứ Sáu"
                                      >
                                        T2-T6
                                      </button>
                                    </div>
                                  </div>

                                  {/* 7 Days Button Row */}
                                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: 3 }}>
                                    {[
                                      { day: 1, label: 'T2' },
                                      { day: 2, label: 'T3' },
                                      { day: 3, label: 'T4' },
                                      { day: 4, label: 'T5' },
                                      { day: 5, label: 'T6' },
                                      { day: 6, label: 'T7' },
                                      { day: 0, label: 'CN' },
                                    ].map((item) => {
                                      const isDayActive = selectedDaysOfWeek.includes(item.day);
                                      return (
                                        <button
                                          key={item.day}
                                          type="button"
                                          onClick={(e) => {
                                            e.stopPropagation();
                                            setSelectedDaysOfWeek((prev) =>
                                              prev.includes(item.day) ? prev.filter((d) => d !== item.day) : [...prev, item.day]
                                            );
                                          }}
                                          style={{
                                            padding: '5px 0',
                                            fontSize: '0.72rem',
                                            fontWeight: isDayActive ? 800 : 600,
                                            borderRadius: 6,
                                            border: isDayActive ? '1px solid #D97706' : '1px solid var(--border)',
                                            background: isDayActive ? 'linear-gradient(135deg, #F59E0B, #D97706)' : 'var(--surface)',
                                            color: isDayActive ? '#17252A' : 'var(--text)',
                                            cursor: 'pointer',
                                            textAlign: 'center',
                                            boxShadow: isDayActive ? '0 1px 4px rgba(217,119,6,0.3)' : 'none',
                                            transition: 'all 0.15s ease',
                                          }}
                                        >
                                          {item.label}
                                        </button>
                                      );
                                    })}
                                  </div>
                                </div>

                                {/* 3. Phạm vi tuần */}
                                <div style={{ display: 'flex', gap: 4 }}>
                                  <button
                                    type="button"
                                    onClick={(e) => {
                                      e.stopPropagation();
                                      setDuplicateTargetScope('next_week');
                                    }}
                                    style={{
                                      flex: 1,
                                      padding: '4px 6px',
                                      fontSize: '0.68rem',
                                      fontWeight: duplicateTargetScope === 'next_week' ? 800 : 500,
                                      borderRadius: 6,
                                      border: duplicateTargetScope === 'next_week' ? '1.5px solid #D97706' : '1px solid var(--border)',
                                      background: duplicateTargetScope === 'next_week' ? '#FEF3C7' : 'var(--bg)',
                                      color: duplicateTargetScope === 'next_week' ? '#B45309' : 'var(--text-muted)',
                                      cursor: 'pointer',
                                    }}
                                  >
                                    Tuần tới
                                  </button>
                                  <button
                                    type="button"
                                    onClick={(e) => {
                                      e.stopPropagation();
                                      setDuplicateTargetScope('this_week');
                                    }}
                                    style={{
                                      flex: 1,
                                      padding: '4px 6px',
                                      fontSize: '0.68rem',
                                      fontWeight: duplicateTargetScope === 'this_week' ? 800 : 500,
                                      borderRadius: 6,
                                      border: duplicateTargetScope === 'this_week' ? '1.5px solid #D97706' : '1px solid var(--border)',
                                      background: duplicateTargetScope === 'this_week' ? '#FEF3C7' : 'var(--bg)',
                                      color: duplicateTargetScope === 'this_week' ? '#B45309' : 'var(--text-muted)',
                                      cursor: 'pointer',
                                    }}
                                  >
                                    Tuần này
                                  </button>
                                  <button
                                    type="button"
                                    onClick={(e) => {
                                      e.stopPropagation();
                                      setDuplicateTargetScope('multi_week');
                                      setCustomUnitsCount(2);
                                    }}
                                    style={{
                                      flex: 1,
                                      padding: '4px 6px',
                                      fontSize: '0.68rem',
                                      fontWeight: duplicateTargetScope === 'multi_week' ? 800 : 500,
                                      borderRadius: 6,
                                      border: duplicateTargetScope === 'multi_week' ? '1.5px solid #D97706' : '1px solid var(--border)',
                                      background: duplicateTargetScope === 'multi_week' ? '#FEF3C7' : 'var(--bg)',
                                      color: duplicateTargetScope === 'multi_week' ? '#B45309' : 'var(--text-muted)',
                                      cursor: 'pointer',
                                    }}
                                  >
                                    2 tuần tới
                                  </button>
                                </div>

                                {/* Nút thực thi nhân bản */}
                                <button
                                  type="button"
                                  disabled={isDuplicating || selectedDaysOfWeek.length === 0}
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    executeDuplicateByDays(block);
                                  }}
                                  style={{
                                    width: '100%',
                                    padding: '7px 10px',
                                    borderRadius: 8,
                                    border: 'none',
                                    background: selectedDaysOfWeek.length === 0 ? 'var(--border)' : 'linear-gradient(135deg, #F59E0B, #D97706)',
                                    color: '#17252A',
                                    fontSize: '0.76rem',
                                    fontWeight: 800,
                                    cursor: isDuplicating || selectedDaysOfWeek.length === 0 ? 'not-allowed' : 'pointer',
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    gap: 6,
                                    boxShadow: '0 2px 8px rgba(217, 119, 6, 0.35)',
                                    marginTop: 2,
                                  }}
                                >
                                  <Copy size={13} />
                                  <span>
                                    {isDuplicating
                                      ? 'Đang nhân bản...'
                                      : selectedDaysOfWeek.length === 0
                                      ? 'Hãy chọn ít nhất 1 Thứ'
                                      : `Nhân bản sang ${selectedDaysOfWeek.length} Thứ đã chọn`}
                                  </span>
                                </button>
                              </div>
                            ) : duplicateMode === 'week' ? (
                              <>
                                {/* Quick Weekly Options: Sang tuần sau / tuần khác */}
                                <div style={{ display: 'flex', flexDirection: 'column', gap: 4, marginBottom: 6 }}>
                                  {[
                                    { weeks: 1, label: '1 tuần', desc: 'Tuần sau (cùng thứ)' },
                                    { weeks: 2, label: '2 tuần', desc: '2 tuần tới' },
                                    { weeks: 4, label: '4 tuần', desc: 'Cả tháng tới' },
                                  ].map((item) => (
                                    <button
                                      key={item.weeks}
                                      type="button"
                                      onClick={(e) => {
                                        e.stopPropagation();
                                        executeDuplicateWeekly(block, item.weeks);
                                      }}
                                      disabled={isDuplicating}
                                      className="duplicate-popover-item"
                                      style={{
                                        display: 'flex',
                                        alignItems: 'center',
                                        justifyContent: 'space-between',
                                        padding: '6px 8px',
                                        borderRadius: 7,
                                        border: '1px solid transparent',
                                        background: 'var(--bg)',
                                        cursor: isDuplicating ? 'wait' : 'pointer',
                                        textAlign: 'left',
                                        transition: 'all 0.15s ease',
                                      }}
                                      title={`Bấm để nhân bản công việc sang ${item.desc}`}
                                    >
                                      <span style={{ fontSize: '0.78rem', fontWeight: 700, color: 'var(--text)' }}>
                                        + {item.label}
                                      </span>
                                      <span style={{ fontSize: '0.68rem', color: '#D97706', fontWeight: 600 }}>
                                        {item.desc}
                                      </span>
                                    </button>
                                  ))}
                                </div>

                                {/* Custom Weeks */}
                                <div
                                  style={{
                                    display: 'flex',
                                    alignItems: 'center',
                                    gap: 5,
                                    paddingTop: 5,
                                    borderTop: '1px dashed #FDE68A',
                                  }}
                                  onClick={(e) => e.stopPropagation()}
                                >
                                  <input
                                    type="number"
                                    min={1}
                                    max={12}
                                    value={customUnitsCount}
                                    onChange={(e) => setCustomUnitsCount(Math.max(1, Math.min(12, Number(e.target.value) || 1)))}
                                    onClick={(e) => e.stopPropagation()}
                                    onKeyDown={(e) => {
                                      if (e.key === 'Enter') {
                                        e.stopPropagation();
                                        executeDuplicateWeekly(block, customUnitsCount);
                                      }
                                    }}
                                    style={{
                                      width: 44,
                                      padding: '3px 4px',
                                      borderRadius: 6,
                                      border: '1.5px solid #FCD34D',
                                      fontSize: '0.78rem',
                                      fontWeight: 700,
                                      textAlign: 'center',
                                      color: 'var(--text)',
                                      background: 'var(--surface)',
                                      outline: 'none',
                                    }}
                                  />
                                  <span style={{ fontSize: '0.72rem', color: 'var(--text-muted)', fontWeight: 600, flex: 1 }}>tuần sau</span>
                                  <button
                                    type="button"
                                    onClick={(e) => {
                                      e.stopPropagation();
                                      executeDuplicateWeekly(block, customUnitsCount);
                                    }}
                                    disabled={isDuplicating}
                                    style={{
                                      padding: '4px 10px',
                                      borderRadius: 6,
                                      border: 'none',
                                      background: 'linear-gradient(135deg, #F59E0B, #D97706)',
                                      color: '#17252A',
                                      fontSize: '0.72rem',
                                      fontWeight: 700,
                                      cursor: isDuplicating ? 'wait' : 'pointer',
                                      boxShadow: '0 2px 6px rgba(245, 158, 11, 0.3)',
                                    }}
                                    title="Nhân bản sang các tuần tiếp theo"
                                  >
                                    {isDuplicating ? '...' : 'Thêm'}
                                  </button>
                                </div>
                              </>
                            ) : (
                              <>
                                {/* Quick Daily Options - Ngày mai, Ngày mốt (Tránh dồn dập các ngày) */}
                                <div style={{ display: 'flex', flexDirection: 'column', gap: 4, marginBottom: 6 }}>
                                  {[
                                    { days: 1, label: 'Ngày mai', desc: '+1 ngày' },
                                    { days: 2, label: 'Ngày mốt', desc: '+2 ngày' },
                                  ].map((item) => (
                                    <button
                                      key={item.days}
                                      type="button"
                                      onClick={(e) => {
                                        e.stopPropagation();
                                        executeDuplicateDaily(block, item.days);
                                      }}
                                      disabled={isDuplicating}
                                      className="duplicate-popover-item"
                                      style={{
                                        display: 'flex',
                                        alignItems: 'center',
                                        justifyContent: 'space-between',
                                        padding: '6px 8px',
                                        borderRadius: 7,
                                        border: '1px solid transparent',
                                        background: 'var(--bg)',
                                        cursor: isDuplicating ? 'wait' : 'pointer',
                                        textAlign: 'left',
                                        transition: 'all 0.15s ease',
                                      }}
                                      title={`Bấm để nhân bản công việc cho ${item.label}`}
                                    >
                                      <span style={{ fontSize: '0.78rem', fontWeight: 700, color: 'var(--text)' }}>
                                        {item.label}
                                      </span>
                                      <span style={{ fontSize: '0.68rem', color: '#D97706', fontWeight: 600 }}>
                                        {item.desc}
                                      </span>
                                    </button>
                                  ))}
                                </div>
                              </>
                            )}
                          </div>
                        )}

                        {/* 3 dots icon for active item */}
                        {block.status === 'IN_PROGRESS' && !isHovered && (
                          <MoreVertical
                            size={13}
                            style={{
                              position: 'absolute',
                              bottom: 4,
                              right: 4,
                              color: '#64748B',
                              cursor: 'pointer',
                            }}
                          />
                        )}
                      </div>
                    );
                  })}
                </div>
              );
            })}
          </div>
        </div>
      </div>

      {/* ── Footer Bar ── */}
      <div className="calendar-footer-bar">
        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <GripVertical size={14} />
          <span>Kéo thả để dời giờ · Rê chuột để Xóa / Nhân bản / Hoàn tất · Bấm ô trống để thêm việc</span>
        </div>
        <div>
          <span>{filteredBlocks.length} công việc · Đã đồng bộ</span>
        </div>
      </div>
    </div>
  );
}
