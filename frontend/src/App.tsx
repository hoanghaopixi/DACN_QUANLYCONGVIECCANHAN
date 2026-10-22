import { useState, useEffect, useCallback, useMemo } from 'react';
import Sidebar from './components/Sidebar';
import TopBar from './components/TopBar';
import HeroHeader from './components/HeroHeader';
import CalendarControls from './components/CalendarControls';
import WeeklyCalendar from './components/WeeklyCalendar';
import MonthCalendar from './components/MonthCalendar';
import StatsBar from './components/StatsBar';
import AIAssistantPanel from './components/AIAssistantPanel';
import TaskModal from './components/TaskModal';
import CategoryModal from './components/CategoryModal';
import AuthPage from './pages/AuthPage';
import TasksView from './components/TasksView';
import SettingsPage from './pages/settings/SettingsPage';

import { taskService } from './services/taskService';
import { categoryService } from './services/categoryService';
import { authService, AuthResponse } from './services/authService';
import { WorkspaceItem } from './services/workspaceService';
import { ViewMode, TimeBlock, Category, UserStats } from './types';
import { matchesSearch } from './utils/vietnameseSearch';

export default function App() {
  // ── Theme State (Light / Dark) ──
  const [theme, setTheme] = useState<'light' | 'dark'>(() => {
    return (localStorage.getItem('smarttask_theme') as 'light' | 'dark') || 'light';
  });

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('smarttask_theme', theme);
  }, [theme]);

  const toggleTheme = () => {
    setTheme((prev) => (prev === 'light' ? 'dark' : 'light'));
  };

  // ── Current User State (Auth) ──
  const [currentUser, setCurrentUser] = useState<AuthResponse | null>(() => {
    return authService.getCurrentUser();
  });

  // ── Active Workspace State ──
  const [activeWorkspace, setActiveWorkspace] = useState<WorkspaceItem | null>(null);

  // ── Toast Notification State ──
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => {
      setToastMessage(null);
    }, 2800);
  };

  // ── Calendar & Navigation State ──
  const [activeNav, setActiveNav] = useState('calendar'); // 'calendar' | 'tasks' | 'ai'
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [viewMode, setViewMode] = useState<ViewMode>('week');
  const [searchQuery, setSearchQuery] = useState('');
  // Calendar navigation date state (defaults to real current time: new Date())
  const [currentDate, setCurrentDate] = useState<Date>(() => new Date());

  // Category Modal states
  const [isCategoryModalOpen, setIsCategoryModalOpen] = useState(false);
  const [editingCategory, setEditingCategory] = useState<Category | null>(null);

  // ── Real Data States from Database ──
  const [categories, setCategories] = useState<Category[]>([]);
  const [timeBlocks, setTimeBlocks] = useState<(TimeBlock & { isRecurring?: boolean; subtext?: string; colorOverride?: string; priority?: string })[]>([]);

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingBlock, setEditingBlock] = useState<any>(null);

  // ── Load Real Data from PostgreSQL via Spring Boot Backend for the Active Workspace ──
  const loadData = useCallback(async (wsId?: number) => {
    const targetWsId = wsId ?? activeWorkspace?.id;
    try {
      const [cats, tasks] = await Promise.all([
        categoryService.getCategories(targetWsId).catch(() => []),
        taskService.getTasks(targetWsId).catch(() => []),
      ]);

      // When switching workspaces, if empty, set to empty []
      setCategories(cats || []);
      setTimeBlocks(tasks || []);
    } catch (err) {
      console.error('Lỗi khi tải dữ liệu từ máy chủ:', err);
    }
  }, [activeWorkspace?.id]);

  useEffect(() => {
    loadData(activeWorkspace?.id);
  }, [loadData, currentUser, activeWorkspace?.id]);

  const handleLoginSuccess = (user: AuthResponse) => {
    setCurrentUser(user);
    showToast(`Chào mừng ${user.fullName} đã đăng nhập!`);
  };

  const handleLogout = () => {
    authService.logout();
    setCurrentUser(null);
    setActiveWorkspace(null);
    setCategories([]);
    setTimeBlocks([]);
    showToast('Đã đăng xuất thành công.');
  };

  const handleUpdateProfile = (updated: { fullName: string; phone?: string }) => {
    if (currentUser) {
      const newUser = { ...currentUser, fullName: updated.fullName };
      setCurrentUser(newUser);
      localStorage.setItem('smarttask_current_user', JSON.stringify(newUser));
      showToast('Đã lưu thông tin tài khoản thành công!');
    }
  };

  // Filter by search query if any (supports Vietnamese diacritics / tones)
  const displayedBlocks = timeBlocks.filter((b) => {
    if (!searchQuery.trim()) return true;
    return matchesSearch(b.title, searchQuery) || matchesSearch(b.description, searchQuery);
  });

  // Calculate today's tasks count dynamically (GMT+7 today)
  const now = new Date();
  const currentYMD = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
  const todayTasksCount = timeBlocks.filter((b) => {
    const d = new Date(b.startTime);
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}` === currentYMD;
  }).length;

  // ── Date Navigation & Range Label ──
  const dayNames = ['Chủ Nhật', 'Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy'];

  const getMonday = (d: Date) => {
    const date = new Date(d);
    const day = date.getDay();
    const diff = date.getDate() - day + (day === 0 ? -6 : 1);
    const mon = new Date(date.setDate(diff));
    mon.setHours(0, 0, 0, 0);
    return mon;
  };

  const currentWeekText = useMemo(() => {
    const year = currentDate.getFullYear();
    const month = currentDate.getMonth() + 1;

    if (viewMode === 'day') {
      const dayName = dayNames[currentDate.getDay()];
      return `${dayName}, ${currentDate.getDate()} tháng ${month}, ${year}`;
    }

    if (viewMode === 'month') {
      return `Tháng ${month}, ${year}`;
    }

    // Week mode
    const monday = getMonday(currentDate);
    const sunday = new Date(monday);
    sunday.setDate(monday.getDate() + 6);

    const mMonth = monday.getMonth() + 1;
    const sMonth = sunday.getMonth() + 1;
    const sYear = sunday.getFullYear();

    if (mMonth === sMonth) {
      return `${monday.getDate()} – ${sunday.getDate()} tháng ${mMonth}, ${sYear}`;
    } else {
      return `${monday.getDate()}/${mMonth} – ${sunday.getDate()}/${sMonth}, ${sYear}`;
    }
  }, [currentDate, viewMode]);

  const activeDays = useMemo(() => {
    const realTodayYMD = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;

    if (viewMode === 'day') {
      const ymd = `${currentDate.getFullYear()}-${String(currentDate.getMonth() + 1).padStart(2, '0')}-${String(currentDate.getDate()).padStart(2, '0')}`;
      return [
        {
          name: dayNames[currentDate.getDay()],
          dateNum: currentDate.getDate(),
          dateStr: ymd,
          isToday: ymd === realTodayYMD,
        },
      ];
    }

    // Week mode: 7 days Monday to Sunday
    const monday = getMonday(currentDate);
    return Array.from({ length: 7 }, (_, i) => {
      const d = new Date(monday);
      d.setDate(monday.getDate() + i);
      const ymd = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
      return {
        name: dayNames[d.getDay()],
        dateNum: d.getDate(),
        dateStr: ymd,
        isToday: ymd === realTodayYMD,
      };
    });
  }, [currentDate, viewMode]);

  // ── Compute Accurate Real-time Week Stats for the Currently Viewed Week ──
  const computedWeekStats = useMemo<UserStats>(() => {
    // 1. Identify start & end of the active week (Monday 00:00:00 to Sunday 23:59:59.999)
    const monday = getMonday(currentDate);
    const sunday = new Date(monday);
    sunday.setDate(monday.getDate() + 6);
    sunday.setHours(23, 59, 59, 999);

    const monTime = monday.getTime();
    const sunTime = sunday.getTime();

    // 2. Filter tasks that belong to this week
    const weekBlocks = timeBlocks.filter((b) => {
      const start = new Date(b.startTime).getTime();
      const end = b.endTime ? new Date(b.endTime).getTime() : start;
      return (start <= sunTime && end >= monTime) || (start >= monTime && start <= sunTime);
    });

    const totalTasksThisWeek = weekBlocks.length;
    const completedTasks = weekBlocks.filter((b) => b.status === 'COMPLETED').length;

    // 3. Compute focus duration in hours (sum of scheduled task duration in this week)
    let totalMinutes = 0;
    const categoryMinutes: Record<string, number> = {};

    weekBlocks.forEach((b) => {
      const s = Math.max(new Date(b.startTime).getTime(), monTime);
      const e = Math.min(new Date(b.endTime).getTime(), sunTime);
      const diffMins = Math.max(0, Math.round((e - s) / 60000));
      totalMinutes += diffMins;

      const cat = b.categoryType || 'WORK';
      categoryMinutes[cat] = (categoryMinutes[cat] || 0) + diffMins;
    });

    const rawHours = totalMinutes / 60;
    const focusHours = Math.round(rawHours * 10) / 10;

    // Determine dynamic focus label
    let focusLabel = 'Học tập & Công việc';
    if (totalTasksThisWeek === 0) {
      focusLabel = 'Chưa có lịch tuần này';
    } else {
      const topCatEntries = Object.entries(categoryMinutes).sort((a, b) => b[1] - a[1]);
      if (topCatEntries.length > 0) {
        const catMap: Record<string, string> = {
          WORK: 'Công việc',
          STUDY: 'Học tập',
          PERSONAL: 'Cá nhân',
          HEALTH: 'Sức khỏe',
          ENTERTAINMENT: 'Giải trí',
          BREAK: 'Nghỉ ngơi',
        };
        const catNames = topCatEntries
          .slice(0, 2)
          .map(([k]) => catMap[k] || k);
        focusLabel = catNames.join(' & ');
      }
    }

    // 4. Calculate actual suitable free slots (khoảng trống phù hợp) across the 7 days of this week
    // Active daytime hours: 08:00 to 21:00 (13 hours/day)
    let freeSlotCount = 0;

    for (let dayIdx = 0; dayIdx < 7; dayIdx++) {
      const dayStart = new Date(monday);
      dayStart.setDate(monday.getDate() + dayIdx);
      dayStart.setHours(8, 0, 0, 0);

      const dayEnd = new Date(dayStart);
      dayEnd.setHours(21, 0, 0, 0);

      // Tasks on this day overlapping 08:00 - 21:00
      const dayTasks = weekBlocks
        .map((b) => ({
          start: Math.max(new Date(b.startTime).getTime(), dayStart.getTime()),
          end: Math.min(new Date(b.endTime).getTime(), dayEnd.getTime()),
        }))
        .filter((b) => b.end > dayStart.getTime() && b.start < dayEnd.getTime() && b.end > b.start)
        .sort((a, b) => a.start - b.start);

      // Merge overlapping/contiguous task intervals
      const merged: { start: number; end: number }[] = [];
      for (const t of dayTasks) {
        if (merged.length === 0) {
          merged.push({ ...t });
        } else {
          const last = merged[merged.length - 1];
          if (t.start <= last.end) {
            last.end = Math.max(last.end, t.end);
          } else {
            merged.push({ ...t });
          }
        }
      }

      // Calculate free gaps between tasks
      let currentPointer = dayStart.getTime();
      for (const m of merged) {
        const gapMins = (m.start - currentPointer) / 60000;
        if (gapMins >= 60) {
          const slots = Math.max(1, Math.floor(gapMins / 90));
          freeSlotCount += slots;
        }
        currentPointer = Math.max(currentPointer, m.end);
      }

      // Trailing gap until 21:00
      const finalGapMins = (dayEnd.getTime() - currentPointer) / 60000;
      if (finalGapMins >= 60) {
        const slots = Math.max(1, Math.floor(finalGapMins / 90));
        freeSlotCount += slots;
      }
    }

    return {
      totalTasksThisWeek,
      completedTasks,
      focusHours,
      focusLabel,
      freeSlots: freeSlotCount,
    };
  }, [currentDate, timeBlocks]);

  const handlePrevDate = () => {
    setCurrentDate((prev) => {
      const next = new Date(prev);
      if (viewMode === 'day') {
        next.setDate(next.getDate() - 1);
      } else if (viewMode === 'month') {
        next.setMonth(next.getMonth() - 1);
      } else {
        next.setDate(next.getDate() - 7);
      }
      return next;
    });
  };

  const handleNextDate = () => {
    setCurrentDate((prev) => {
      const next = new Date(prev);
      if (viewMode === 'day') {
        next.setDate(next.getDate() + 1);
      } else if (viewMode === 'month') {
        next.setMonth(next.getMonth() + 1);
      } else {
        next.setDate(next.getDate() + 7);
      }
      return next;
    });
  };

  const handleToday = () => {
    setCurrentDate(new Date());
    showToast('Đã chuyển đến ngày hôm nay');
  };

  // Handle adding task from slot click
  const handleAddAtSlot = (dayIndex: number, hour: number) => {
    const targetDay = activeDays[dayIndex] || activeDays[0];
    const today = new Date();
    const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
    const selectedDate = targetDay ? targetDay.dateStr : todayStr;
    const startHourStr = hour.toString().padStart(2, '0');
    const endHourStr = hour === 23 ? '23' : (hour + 1).toString().padStart(2, '0');
    const endMinStr = hour === 23 ? '59' : '00';

    setEditingBlock({
      title: '',
      categoryType: 'WORK',
      startTime: `${selectedDate}T${startHourStr}:00:00+07:00`,
      endTime: `${selectedDate}T${endHourStr}:${endMinStr}:00+07:00`,
      priority: 'MEDIUM',
      status: 'TODO',
    });
    setIsModalOpen(true);
  };

  const handleAddAtDate = (dateStr: string) => {
    setEditingBlock({
      title: '',
      categoryType: 'WORK',
      startTime: `${dateStr}T09:00:00+07:00`,
      endTime: `${dateStr}T10:00:00+07:00`,
      priority: 'MEDIUM',
      status: 'TODO',
    });
    setIsModalOpen(true);
  };

  // ── Category CRUD Handlers ──
  const handleOpenAddCategory = () => {
    setEditingCategory(null);
    setIsCategoryModalOpen(true);
  };

  const handleOpenEditCategory = (cat: Category) => {
    setEditingCategory(cat);
    setIsCategoryModalOpen(true);
  };

  const handleSaveCategory = async (data: { id?: number; name: string; colorCode: string }) => {
    try {
      if (data.id) {
        await categoryService.updateCategory(data.id, {
          name: data.name,
          colorCode: data.colorCode,
        });
        showToast(`✓ Đã cập nhật nhóm "${data.name}"!`);
      } else {
        await categoryService.createCategory({
          name: data.name,
          colorCode: data.colorCode,
          workspaceId: activeWorkspace?.id || 1,
        });
        showToast(`✓ Đã thêm nhóm mới "${data.name}"!`);
      }
      await loadData(activeWorkspace?.id);
    } catch (err) {
      console.error('Lỗi khi lưu nhóm:', err);
      showToast('Lỗi khi lưu nhóm vào máy chủ!');
    }
  };

  const handleDeleteCategory = async (id: number) => {
    try {
      await categoryService.deleteCategory(id);
      showToast('✓ Đã xóa nhóm công việc thành công!');
      if (selectedCategory) {
        const cat = categories.find((c) => c.id === id);
        if (cat && selectedCategory === cat.name) {
          setSelectedCategory(null);
        }
      }
      await loadData(activeWorkspace?.id);
    } catch (err: any) {
      console.error('Lỗi khi xóa nhóm:', err);
      const serverMsg = err.response?.data?.message || 'Không thể xóa nhóm này vì đang có công việc đang sử dụng!';
      showToast(`⚠️ ${serverMsg}`);
      throw err;
    }
  };

  // Handle select block to view / edit
  const handleSelectBlock = (block: TimeBlock) => {
    setEditingBlock(block);
    setIsModalOpen(true);
  };

  // Handle saving task to Backend Database
  const handleSaveTask = async (taskData: any) => {
    try {
      if (taskData.id) {
        await taskService.updateTask(taskData.id, {
          ...taskData,
          workspaceId: taskData.workspaceId || activeWorkspace?.id || 1,
        });
        showToast('✓ Đã cập nhật công việc!');
      } else {
        await taskService.createTask({
          ...taskData,
          workspaceId: activeWorkspace?.id || 1,
        });
        showToast('✓ Đã thêm công việc mới vào cơ sở dữ liệu!');
      }
      await loadData(activeWorkspace?.id);
    } catch (err) {
      console.error('Lỗi khi lưu công việc vào cơ sở dữ liệu:', err);
      showToast('Lỗi khi lưu công việc vào máy chủ');
    }
  };

  // Handle deleting task from Backend Database
  const handleDeleteTask = async (id: number) => {
    try {
      await taskService.deleteTask(id);
      showToast('✓ Đã xóa công việc khỏi lịch!');
      await loadData(activeWorkspace?.id);
    } catch (err) {
      console.error('Lỗi khi xóa công việc khỏi cơ sở dữ liệu:', err);
      showToast('Lỗi khi xóa công việc khỏi máy chủ');
    }
  };

  // Handle Kéo thả để dời giờ (Drag and Drop Reschedule)
  const handleRescheduleTask = async (blockId: number, newStartTime: string, newEndTime: string) => {
    // 1. Optimistic instant UI update
    setTimeBlocks((prev) =>
      prev.map((b) => (b.id === blockId ? { ...b, startTime: newStartTime, endTime: newEndTime } : b))
    );

    const s = new Date(newStartTime);
    const dayNames = ['Chủ Nhật', 'Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy'];
    const timeLabel = `${dayNames[s.getDay()]} lúc ${s.getHours().toString().padStart(2, '0')}:${s.getMinutes().toString().padStart(2, '0')}`;
    showToast(`✓ Đã dời công việc sang ${timeLabel} thành công!`);

    try {
      const currentTask = timeBlocks.find((b) => b.id === blockId);
      // 2. Persist in PostgreSQL via API
      await taskService.updateTask(blockId, {
        title: currentTask?.title,
        categoryType: currentTask?.categoryType,
        priority: currentTask?.priority,
        status: currentTask?.status,
        startTime: newStartTime,
        endTime: newEndTime,
        workspaceId: currentTask?.workspaceId || activeWorkspace?.id || 1,
      });
      await loadData(activeWorkspace?.id);
    } catch (err) {
      console.error('Lỗi khi dời giờ công việc:', err);
      showToast('Lỗi khi cập nhật thời gian vào máy chủ!');
      await loadData(activeWorkspace?.id);
    }
  };

  // Handle toggling complete status from Tasks view
  const handleToggleStatus = async (task: TimeBlock) => {
    const nextStatus = task.status === 'COMPLETED' ? 'TODO' : 'COMPLETED';
    setTimeBlocks((prev) =>
      prev.map((b) => (b.id === task.id ? { ...b, status: nextStatus as any } : b))
    );
    showToast(nextStatus === 'COMPLETED' ? '✓ Đã hoàn thành công việc!' : 'Đã chuyển trạng thái sang Cần làm');
    try {
      await taskService.updateTask(task.id, { title: task.title, status: nextStatus });
      await loadData(activeWorkspace?.id);
    } catch (err) {
      console.error('Lỗi khi cập nhật trạng thái:', err);
      await loadData(activeWorkspace?.id);
    }
  };

  // Handle scheduling from AI Assistant
  const handleScheduleFromAI = async (newTask: Partial<TimeBlock>) => {
    try {
      const now = new Date();
      const todayStr = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
      const created = await taskService.createTask({
        title: newTask.title || 'Công việc mới',
        note: newTask.note || (newTask as any).description,
        description: newTask.note || (newTask as any).description,
        startTime: newTask.startTime || `${todayStr}T15:00:00+07:00`,
        endTime: newTask.endTime || `${todayStr}T16:00:00+07:00`,
        categoryType: newTask.categoryType || 'WORK',
        priority: (newTask as any).priority || 'MEDIUM',
        status: newTask.status || 'TODO',
        isAutoScheduled: true,
        workspaceId: activeWorkspace?.id || 1,
      });
      showToast('✓ Trợ lý AI đã thêm công việc vào lịch thành công!');
      await loadData(activeWorkspace?.id);
      return created;
    } catch (err) {
      console.error('Lỗi xếp lịch AI:', err);
      showToast('Lỗi khi lưu công việc từ Trợ lý AI');
    }
  };

  const displayName = currentUser ? currentUser.fullName : 'Khách';

  // ── Dedicated Full-Page Authentication View (Not a modal/popup) ──
  if (!currentUser) {
    return (
      <AuthPage
        onLoginSuccess={handleLoginSuccess}
        theme={theme}
        onToggleTheme={toggleTheme}
      />
    );
  }

  return (
    <div className="app-layout">
      {/* ── Left Sidebar ── */}
      <Sidebar
        categories={categories}
        userName={displayName}
        activeNav={activeNav}
        onNavChange={setActiveNav}
        selectedCategory={selectedCategory}
        onSelectCategory={setSelectedCategory}
        onAddCategory={handleOpenAddCategory}
        onEditCategory={handleOpenEditCategory}
        onDeleteCategory={handleDeleteCategory}
        currentUser={currentUser}
        onOpenAuthModal={handleLogout}
        onOpenSettingsModal={() => setActiveNav('settings')}
        onLogout={handleLogout}
        activeWorkspace={activeWorkspace}
        onWorkspaceChange={setActiveWorkspace}
        onShowToast={showToast}
      />

      {/* ── Center Main Content ── */}
      <main className="app-main">
        {activeNav === 'settings' ? (
          <div className="view-transition-container" style={{ flex: 1, overflowY: 'auto', display: 'flex', flexDirection: 'column' }}>
            <SettingsPage
              currentUser={currentUser}
              onUpdateProfile={handleUpdateProfile}
              onLogout={handleLogout}
              onShowToast={showToast}
              onBackToCalendar={() => setActiveNav('calendar')}
            />
          </div>
        ) : (
          <>
            {/* Top bar header with Theme Toggle & Auth */}
            <TopBar
              searchQuery={searchQuery}
              onSearchChange={setSearchQuery}
              theme={theme}
              onToggleTheme={toggleTheme}
              currentUser={currentUser}
              onOpenAuthModal={() => setActiveNav('settings')}
              workspaceName={activeWorkspace?.name || 'Không gian cá nhân'}
              activeNavTitle={activeNav === 'tasks' ? 'Công việc' : activeNav === 'ai' ? 'Trợ lý AI' : 'Lịch của tôi'}
              onSelectTask={(taskId) => {
                const task = timeBlocks.find((t) => t.id === taskId);
                if (task) {
                  handleSelectBlock(task);
                } else {
                  showToast(`Đã chọn thông báo liên quan đến công việc #${taskId}`);
                }
              }}
            />

            {/* Content Body: Calendar section OR Tasks View */}
            <div className="content-body">
              {activeNav === 'tasks' ? (
                /* ── Full Task Management View (Tab "Công việc") ── */
                <div style={{ flex: 1, overflowY: 'auto' }} className="view-transition-container">
                  <TasksView
                    tasks={displayedBlocks}
                    categories={categories}
                    onAddTask={() => {
                      setEditingBlock(null);
                      setIsModalOpen(true);
                    }}
                    onEditTask={handleSelectBlock}
                    onDeleteTask={handleDeleteTask}
                    onToggleStatus={handleToggleStatus}
                  />
                </div>
              ) : (
                /* ── Weekly / Monthly Calendar Section (Tab "Lịch của tôi" & "Trợ lý AI") ── */
                <div className="calendar-section view-transition-container">
                  {/* Hero Header with Add Task Button */}
                  <div className="page-content" style={{ padding: '20px 0 0' }}>
                    <HeroHeader
                      userName={displayName}
                      todayTasksCount={todayTasksCount}
                      timeBlocks={timeBlocks}
                      onAddTaskClick={() => {
                        setEditingBlock(null);
                        setIsModalOpen(true);
                      }}
                      onSelectTask={handleSelectBlock}
                    />

                    {/* Calendar Controls & Legend */}
                    <CalendarControls
                      currentWeekText={currentWeekText}
                      viewMode={viewMode}
                      onViewModeChange={setViewMode}
                      onPrevWeek={handlePrevDate}
                      onNextWeek={handleNextDate}
                      onToday={handleToday}
                      categories={categories}
                      selectedCategory={selectedCategory}
                      onToggleCategory={(catName) => {
                        setSelectedCategory((prev) => (prev === catName ? null : catName));
                      }}
                      statusFilter={statusFilter}
                      onStatusFilterChange={setStatusFilter}
                    />
                  </div>

                  {/* Dynamic View: Month Grid OR Weekly/Daily Hourly Timeline Grid */}
                  <div key={`${viewMode}-${currentDate.toISOString().slice(0, 10)}`} className="calendar-transition-wrapper">
                    {viewMode === 'month' ? (
                      <MonthCalendar
                        currentDate={currentDate}
                        timeBlocks={displayedBlocks}
                        categories={categories}
                        selectedCategory={selectedCategory}
                        statusFilter={statusFilter}
                        onSelectBlock={handleSelectBlock}
                        onAddAtDate={handleAddAtDate}
                      />
                    ) : (
                      <WeeklyCalendar
                        days={activeDays}
                        timeBlocks={displayedBlocks}
                        categories={categories}
                        selectedCategory={selectedCategory}
                        statusFilter={statusFilter}
                        onSelectBlock={handleSelectBlock}
                        onAddAtSlot={handleAddAtSlot}
                        onRescheduleTask={handleRescheduleTask}
                      />
                    )}
                  </div>

                  {/* Bottom 3 Stats Cards: accurately computed for active week */}
                  <StatsBar stats={computedWeekStats} />
                </div>
              )}

              {/* ── Right Panel: AI Assistant SmartTask ── */}
              <AIAssistantPanel
                currentDate={currentDate}
                onScheduleTask={handleScheduleFromAI}
                onEditTaskDetails={(info) => {
                  setEditingBlock({
                    id: info.id,
                    taskId: info.id,
                    title: info.title,
                    startTime: `${info.date}T${info.startTime}:00+07:00`,
                    endTime: `${info.date}T${info.endTime}:00+07:00`,
                    priority: info.priority || 'MEDIUM',
                    categoryType: info.categoryType || 'WORK',
                    status: 'TODO',
                  });
                  setIsModalOpen(true);
                }}
              />
            </div>
          </>
        )}
      </main>

      {/* ── Add / Edit / Delete Task Modal ── */}
      <TaskModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSave={handleSaveTask}
        onDelete={handleDeleteTask}
        categories={categories}
        initialData={editingBlock}
      />

      {/* ── Category Management Modal (Add / Edit / Delete) ── */}
      <CategoryModal
        isOpen={isCategoryModalOpen}
        onClose={() => setIsCategoryModalOpen(false)}
        onSave={handleSaveCategory}
        onDelete={handleDeleteCategory}
        category={editingCategory}
      />

      {/* ── Floating Toast Notification ── */}
      {toastMessage && (
        <div
          style={{
            position: 'fixed',
            bottom: 24,
            right: 24,
            zIndex: 99999,
            background: 'var(--text)',
            color: 'var(--surface)',
            padding: '12px 20px',
            borderRadius: 12,
            boxShadow: '0 10px 25px rgba(0,0,0,0.25)',
            fontSize: '0.86rem',
            fontWeight: 600,
            display: 'flex',
            alignItems: 'center',
            gap: 10,
            animation: 'slideUpToast 0.25s cubic-bezier(0.16, 1, 0.3, 1)',
            border: '1px solid rgba(255,255,255,0.1)',
          }}
        >
          <span>{toastMessage}</span>
        </div>
      )}
    </div>
  );
}
