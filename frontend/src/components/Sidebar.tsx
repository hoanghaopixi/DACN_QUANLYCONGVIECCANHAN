import { useState, useEffect, useRef } from 'react';
import {
  Calendar, CheckSquare, Settings, ChevronDown, ChevronsUpDown,
  LogOut, UserCircle, Plus, Pencil, Trash2, Check, X,
  BarChart3, PanelLeftClose, PanelLeftOpen, Home, Layers
} from 'lucide-react';
import { Category } from '../types';
import { workspaceService, WorkspaceItem } from '../services/workspaceService';
import ConfirmModal from './ConfirmModal';

interface SidebarProps {
  categories: Category[];
  userName: string;
  activeNav: string;
  onNavChange: (nav: string) => void;
  selectedCategory: string | null;
  onSelectCategory: (categoryName: string | null) => void;
  onAddCategory: () => void;
  onEditCategory: (cat: Category) => void;
  onDeleteCategory: (id: number) => void;
  currentUser: { email: string; fullName: string; role: string; avatarUrl?: string } | null;
  onOpenAuthModal: () => void;
  onOpenSettingsModal: () => void;
  onLogout: () => void;
  activeWorkspace: WorkspaceItem | null;
  onWorkspaceChange: (ws: WorkspaceItem) => void;
  onShowToast?: (msg: string) => void;
}

export default function Sidebar({
  categories,
  userName,
  activeNav,
  onNavChange,
  selectedCategory,
  onSelectCategory,
  onAddCategory,
  onEditCategory,
  onDeleteCategory,
  currentUser,
  onOpenAuthModal,
  onOpenSettingsModal,
  onLogout,
  activeWorkspace,
  onWorkspaceChange,
  onShowToast,
}: SidebarProps) {
  const [isCollapsed, setIsCollapsed] = useState<boolean>(() => {
    try {
      return localStorage.getItem('smarttask_sidebar_collapsed') === 'true';
    } catch {
      return false;
    }
  });

  const handleToggleCollapse = () => {
    setIsCollapsed((prev) => {
      const next = !prev;
      try {
        localStorage.setItem('smarttask_sidebar_collapsed', String(next));
      } catch {}
      return next;
    });
  };

  const [showProfileMenu, setShowProfileMenu] = useState(false);
  const [showWorkspaceMenu, setShowWorkspaceMenu] = useState(false);
  const [showCategoryMenu, setShowCategoryMenu] = useState(false);
  const [workspaces, setWorkspaces] = useState<WorkspaceItem[]>([]);
  const [editingWsId, setEditingWsId] = useState<number | null>(null);
  const [editingWsText, setEditingWsText] = useState('');
  const [isAddingWs, setIsAddingWs] = useState(false);
  const [newWsText, setNewWsText] = useState('');

  const profileMenuRef = useRef<HTMLDivElement>(null);
  const workspaceMenuRef = useRef<HTMLDivElement>(null);
  const categoryMenuRef = useRef<HTMLDivElement>(null);

  const wsTimerRef = useRef<NodeJS.Timeout | null>(null);
  const catTimerRef = useRef<NodeJS.Timeout | null>(null);
  const profileTimerRef = useRef<NodeJS.Timeout | null>(null);

  const handleWsMouseEnter = () => {
    if (wsTimerRef.current) clearTimeout(wsTimerRef.current);
    setShowWorkspaceMenu(true);
  };
  const handleWsMouseLeave = () => {
    if (wsTimerRef.current) clearTimeout(wsTimerRef.current);
    wsTimerRef.current = setTimeout(() => {
      if (!isAddingWs && editingWsId === null) {
        setShowWorkspaceMenu(false);
      }
    }, 220);
  };

  const handleCatMouseEnter = () => {
    if (catTimerRef.current) clearTimeout(catTimerRef.current);
    if (isCollapsed) {
      setShowCategoryMenu(true);
    }
  };
  const handleCatMouseLeave = () => {
    if (catTimerRef.current) clearTimeout(catTimerRef.current);
    catTimerRef.current = setTimeout(() => {
      setShowCategoryMenu(false);
    }, 220);
  };

  const handleProfileMouseEnter = () => {
    if (profileTimerRef.current) clearTimeout(profileTimerRef.current);
    setShowProfileMenu(true);
  };
  const handleProfileMouseLeave = () => {
    if (profileTimerRef.current) clearTimeout(profileTimerRef.current);
    profileTimerRef.current = setTimeout(() => {
      setShowProfileMenu(false);
    }, 220);
  };

  // Close profile, workspace & category dropdown when clicking outside
  useEffect(() => {
    const handleOutsideClick = (event: MouseEvent) => {
      if (profileMenuRef.current && !profileMenuRef.current.contains(event.target as Node)) {
        setShowProfileMenu(false);
      }
      if (workspaceMenuRef.current && !workspaceMenuRef.current.contains(event.target as Node)) {
        setShowWorkspaceMenu(false);
        setIsAddingWs(false);
      }
      if (categoryMenuRef.current && !categoryMenuRef.current.contains(event.target as Node)) {
        setShowCategoryMenu(false);
      }
    };
    if (showProfileMenu || showWorkspaceMenu || showCategoryMenu) {
      document.addEventListener('mousedown', handleOutsideClick);
    }
    return () => {
      document.removeEventListener('mousedown', handleOutsideClick);
    };
  }, [showProfileMenu, showWorkspaceMenu, showCategoryMenu]);

  const loadWorkspaces = async () => {
    try {
      const list = await workspaceService.getWorkspaces();
      if (list && list.length > 0) {
        setWorkspaces(list);
        if (!activeWorkspace || !list.some((w) => w.id === activeWorkspace.id)) {
          onWorkspaceChange(list[0]);
        }
      } else {
        setWorkspaces([]);
      }
    } catch (err) {
      console.error('Lỗi khi tải không gian làm việc từ CSDL:', err);
    }
  };

  useEffect(() => {
    loadWorkspaces();
  }, [currentUser]);



  const handleStartEditWs = (ws: WorkspaceItem, e: React.MouseEvent) => {
    e.stopPropagation();
    setEditingWsId(ws.id);
    setEditingWsText(ws.name);
  };

  const handleSaveEditWs = async (ws: WorkspaceItem, e: React.MouseEvent) => {
    e.stopPropagation();
    if (!editingWsText.trim()) return;
    try {
      const updated = await workspaceService.updateWorkspace(ws.id, editingWsText.trim());
      if (activeWorkspace?.id === ws.id) {
        onWorkspaceChange(updated);
      }
      setEditingWsId(null);
      await loadWorkspaces();
      onShowToast?.(`✓ Đã đổi tên không gian thành "${updated.name}"!`);
    } catch (err) {
      console.error('Lỗi khi sửa không gian:', err);
      onShowToast?.('Lỗi khi sửa tên không gian làm việc!');
    }
  };

  // In-App Confirm Delete Modal State (for both Workspace and Category)
  const [deleteTarget, setDeleteTarget] = useState<{
    type: 'workspace' | 'category';
    id: number;
    name: string;
    title: string;
    message: string;
    warningNote?: string;
  } | null>(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  const handleExecuteDelete = async () => {
    if (!deleteTarget) return;
    setIsDeleting(true);
    setDeleteError(null);

    if (deleteTarget.type === 'workspace') {
      try {
        await workspaceService.deleteWorkspace(deleteTarget.id);
        const remaining = workspaces.filter((w) => w.id !== deleteTarget.id);
        setWorkspaces(remaining);
        if (activeWorkspace?.id === deleteTarget.id && remaining.length > 0) {
          onWorkspaceChange(remaining[0]);
        }
        await loadWorkspaces();
        onShowToast?.(`✓ Đã xóa không gian "${deleteTarget.name}" thành công!`);
        setDeleteTarget(null);
      } catch (err: any) {
        console.error('Lỗi khi xóa không gian:', err);
        const msg = err.response?.data?.message || 'Lỗi khi xóa không gian làm việc khỏi CSDL';
        setDeleteError(msg);
        onShowToast?.(`⚠️ ${msg}`);
      } finally {
        setIsDeleting(false);
      }
    } else {
      // Category delete
      try {
        await onDeleteCategory(deleteTarget.id);
        setDeleteTarget(null);
      } catch (err: any) {
        console.error('Lỗi khi xóa nhóm:', err);
        const msg = err.response?.data?.message || 'Không thể xóa nhóm này vì đang có công việc đang sử dụng!';
        setDeleteError(msg);
      } finally {
        setIsDeleting(false);
      }
    }
  };

  const handleCreateWs = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newWsText.trim()) return;
    try {
      const created = await workspaceService.createWorkspace(newWsText.trim());
      onWorkspaceChange(created);
      setNewWsText('');
      setIsAddingWs(false);
      await loadWorkspaces();
      onShowToast?.(`✓ Đã tạo không gian làm việc mới "${created.name}"!`);
    } catch (err) {
      console.error('Lỗi khi tạo không gian:', err);
      onShowToast?.('Lỗi khi tạo không gian làm việc!');
    }
  };

  return (
    <aside className={`sidebar ${isCollapsed ? 'collapsed' : ''}`}>
      {/* ── Brand Logo & Collapse Toggle Button ── */}
      <div className="sidebar-logo">
        {isCollapsed ? (
          <button
            type="button"
            className="sidebar-collapse-toggle-btn collapsed"
            onClick={handleToggleCollapse}
            title="Mở rộng thanh bên (Click để mở)"
            aria-label="Mở rộng thanh bên"
          >
            <PanelLeftOpen size={20} />
          </button>
        ) : (
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', width: '100%' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <div className="logo-icon-svg">
                {/* Sound wave icon matching SmartTask brand */}
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none">
                  <rect width="24" height="24" rx="7" fill="var(--primary)" />
                  <path d="M7 10V14M10 7V17M14 5V19M17 10V14" stroke="var(--on-primary)" strokeWidth="2" strokeLinecap="round" />
                </svg>
              </div>
              <span className="logo-text">SmartTask</span>
            </div>
            <button
              type="button"
              className="sidebar-collapse-toggle-btn"
              onClick={handleToggleCollapse}
              title="Thu gọn thanh bên"
              aria-label="Thu gọn thanh bên"
            >
              <PanelLeftClose size={18} />
            </button>
          </div>
        )}
      </div>

      {/* ── Workspace Selector with Add, Edit, Delete (Home icon when collapsed) ── */}
      <div
        className="workspace-hover-container"
        style={{ position: 'relative', marginBottom: 10, width: '100%' }}
        ref={workspaceMenuRef}
        onMouseEnter={handleWsMouseEnter}
        onMouseLeave={handleWsMouseLeave}
      >
        {isCollapsed ? (
          <div style={{ display: 'flex', justifyContent: 'center' }}>
            <button
              type="button"
              className={`workspace-collapsed-btn ${showWorkspaceMenu ? 'active' : ''}`}
              title={`Không gian làm việc: ${activeWorkspace?.name || 'Không gian cá nhân'}`}
              onClick={() => setShowWorkspaceMenu(!showWorkspaceMenu)}
              onMouseEnter={handleWsMouseEnter}
            >
              <Home size={20} />
            </button>
          </div>
        ) : (
          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <button
              type="button"
              className={`workspace-selector ${showWorkspaceMenu ? 'active' : ''}`}
              title="Chọn không gian làm việc"
              onClick={() => setShowWorkspaceMenu(!showWorkspaceMenu)}
              onMouseEnter={handleWsMouseEnter}
            >
              <span className="workspace-selector-name">
                {activeWorkspace?.name || 'Không gian cá nhân'}
              </span>
              <ChevronsUpDown
                size={15}
                className="workspace-selector-chevron"
              />
            </button>

            <button
              type="button"
              className="add-category-btn"
              onClick={() => {
                setShowWorkspaceMenu(true);
                setIsAddingWs(true);
              }}
              title="Thêm không gian làm việc mới"
            >
              <Plus size={16} />
            </button>
          </div>
        )}

        {/* Workspace Dropdown with Rename & Delete */}
        <div className={`workspace-dropdown-menu ${showWorkspaceMenu ? 'open' : ''}`}>
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '4px 8px 6px',
                borderBottom: '1px solid var(--border-light)',
                fontSize: '0.72rem',
                fontWeight: 700,
                color: 'var(--text-muted)',
                letterSpacing: '0.04em',
                textTransform: 'uppercase',
              }}
            >
              <span>Không gian làm việc</span>
              <button
                type="button"
                onClick={() => setIsAddingWs(!isAddingWs)}
                style={{
                  background: 'none',
                  border: 'none',
                  color: 'var(--primary)',
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  gap: 3,
                  fontSize: '0.72rem',
                  fontWeight: 600,
                  padding: '2px 6px',
                  borderRadius: 4,
                  transition: 'background 0.15s ease',
                }}
              >
                <Plus size={12} />
                <span>Thêm</span>
              </button>
            </div>

            {/* List of Workspaces */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: 2, maxHeight: 180, overflowY: 'auto' }}>
              {workspaces.map((ws) => {
                const isSelected = activeWorkspace?.id === ws.id;
                const isEditing = editingWsId === ws.id;

                if (isEditing) {
                  return (
                    <div
                      key={ws.id}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: 4,
                        padding: '4px 6px',
                        background: 'var(--border-light)',
                        borderRadius: 6,
                      }}
                    >
                      <input
                        type="text"
                        value={editingWsText}
                        onChange={(e) => setEditingWsText(e.target.value)}
                        autoFocus
                        style={{
                          flex: 1,
                          minWidth: 0,
                          fontSize: '0.8rem',
                          padding: '4px 6px',
                          border: '1px solid var(--primary)',
                          borderRadius: 4,
                          background: 'var(--surface)',
                          color: 'var(--text)',
                          outline: 'none',
                        }}
                        onKeyDown={(e) => {
                          if (e.key === 'Enter') handleSaveEditWs(ws, e as any);
                          if (e.key === 'Escape') setEditingWsId(null);
                        }}
                      />
                      <button
                        type="button"
                        onClick={(e) => handleSaveEditWs(ws, e)}
                        style={{ color: '#10B981', cursor: 'pointer', padding: '3px 4px', borderRadius: 4 }}
                        title="Lưu tên (Enter)"
                      >
                        <Check size={14} />
                      </button>
                      <button
                        type="button"
                        onClick={() => setEditingWsId(null)}
                        style={{ color: 'var(--text-muted)', cursor: 'pointer', padding: '3px 4px', borderRadius: 4 }}
                        title="Hủy (Esc)"
                      >
                        <X size={14} />
                      </button>
                    </div>
                  );
                }

                return (
                  <div
                    key={ws.id}
                    className={`workspace-dropdown-item ${isSelected ? 'selected' : ''}`}
                    onClick={() => {
                      onWorkspaceChange(ws);
                      setShowWorkspaceMenu(false);
                    }}
                  >
                    <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', flex: 1 }}>
                      {ws.name}
                    </span>

                    <div style={{ display: 'flex', alignItems: 'center', gap: 2, flexShrink: 0 }}>
                      {/* Pencil Edit button */}
                      <button
                        type="button"
                        className="cat-action-btn"
                        onClick={(e) => {
                          e.stopPropagation();
                          e.preventDefault();
                          handleStartEditWs(ws, e);
                        }}
                        title={`Sửa tên không gian "${ws.name}"`}
                        style={{
                          width: 24,
                          height: 24,
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          cursor: 'pointer',
                          borderRadius: 4,
                        }}
                      >
                        <Pencil size={13} />
                      </button>

                      {/* Trash Delete button */}
                      {workspaces.length > 1 && (
                        <button
                          type="button"
                          className="cat-action-btn delete-btn"
                          onClick={(e) => {
                            e.stopPropagation();
                            e.preventDefault();
                            setDeleteError(null);
                            setDeleteTarget({
                              type: 'workspace',
                              id: ws.id,
                              name: ws.name,
                              title: 'Xóa không gian làm việc',
                              message: `Bạn có chắc chắn muốn xóa không gian "${ws.name}" khỏi cơ sở dữ liệu không?`,
                              warningNote: 'Lưu ý: Không thể xóa không gian nếu đang có công việc thuộc không gian này.',
                            });
                          }}
                          title={`Xóa không gian "${ws.name}"`}
                          style={{
                            width: 24,
                            height: 24,
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            cursor: 'pointer',
                            borderRadius: 4,
                          }}
                        >
                          <Trash2 size={13} />
                        </button>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>

            {/* Inline Add Workspace form */}
            {isAddingWs && (
              <form
                onSubmit={handleCreateWs}
                style={{
                  marginTop: 4,
                  padding: '8px',
                  background: 'var(--border-light)',
                  borderRadius: 8,
                  display: 'flex',
                  flexDirection: 'column',
                  gap: 6,
                  animation: 'slideDownSmooth 0.2s cubic-bezier(0.16, 1, 0.3, 1)',
                }}
              >
                <input
                  type="text"
                  placeholder="Tên không gian mới..."
                  value={newWsText}
                  onChange={(e) => setNewWsText(e.target.value)}
                  autoFocus
                  style={{
                    width: '100%',
                    fontSize: '0.82rem',
                    padding: '6px 8px',
                    border: '1px solid var(--primary)',
                    borderRadius: 6,
                    background: 'var(--surface)',
                    color: 'var(--text)',
                    outline: 'none',
                    boxSizing: 'border-box',
                  }}
                />
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: 6 }}>
                  <button
                    type="button"
                    onClick={() => {
                      setIsAddingWs(false);
                      setNewWsText('');
                    }}
                    style={{
                      padding: '4px 8px',
                      fontSize: '0.75rem',
                      color: 'var(--text-muted)',
                      background: 'none',
                      border: 'none',
                      borderRadius: 4,
                      cursor: 'pointer',
                      fontWeight: 500,
                    }}
                  >
                    Hủy
                  </button>
                  <button
                    type="submit"
                    disabled={!newWsText.trim()}
                    style={{
                      background: newWsText.trim() ? 'var(--primary)' : '#94A3B8',
                      color: '#fff',
                      border: 'none',
                      borderRadius: 6,
                      padding: '4px 12px',
                      fontSize: '0.75rem',
                      fontWeight: 600,
                      cursor: newWsText.trim() ? 'pointer' : 'not-allowed',
                      transition: 'background 0.15s ease',
                    }}
                  >
                    Thêm không gian
                  </button>
                </div>
              </form>
            )}
          </div>
      </div>

      {/* ── Navigation ── */}
      <nav className="sidebar-nav">
        <button
          className={`nav-item ${activeNav === 'calendar' ? 'active' : ''}`}
          onClick={() => onNavChange('calendar')}
          title="Lịch của tôi"
        >
          <Calendar size={18} />
          <span>Lịch của tôi</span>
        </button>

        <button
          className={`nav-item ${activeNav === 'tasks' ? 'active' : ''}`}
          onClick={() => onNavChange('tasks')}
          title="Công việc"
        >
          <CheckSquare size={18} />
          <span>Công việc</span>
        </button>

        <button
          className={`nav-item ${activeNav === 'productivity' ? 'active' : ''}`}
          onClick={() => onNavChange('productivity')}
          title="Báo cáo năng suất"
        >
          <BarChart3 size={18} />
          <span>Báo cáo năng suất</span>
        </button>
      </nav>

      {/* ── Categories Section (NHÓM CỦA TÔI) ── */}
      {isCollapsed ? (
        <div
          className="category-hover-container"
          style={{ position: 'relative', width: '100%', marginBottom: 16, display: 'flex', justifyContent: 'center' }}
          ref={categoryMenuRef}
          onMouseEnter={handleCatMouseEnter}
          onMouseLeave={handleCatMouseLeave}
        >
          <button
            type="button"
            className={`category-collapsed-btn ${showCategoryMenu ? 'active' : ''}`}
            onClick={() => setShowCategoryMenu(!showCategoryMenu)}
            onMouseEnter={handleCatMouseEnter}
            title={selectedCategory ? `Nhóm của tôi (Đang lọc: ${selectedCategory})` : 'Nhóm của tôi'}
          >
            <Layers size={20} />
            {selectedCategory && (
              <span
                className="category-active-dot"
                style={{
                  background: categories.find((c) => c.name === selectedCategory)?.colorCode || 'var(--primary)',
                }}
              />
            )}
          </button>

          {/* Floating Category Dropdown when collapsed */}
          <div className={`category-dropdown-flyout ${showCategoryMenu ? 'open' : ''}`}>
              <div className="flyout-header">
                <span>Nhóm của tôi</span>
                <button
                  type="button"
                  className="flyout-add-btn"
                  onClick={(e) => {
                    e.stopPropagation();
                    onAddCategory();
                    setShowCategoryMenu(false);
                  }}
                  title="Thêm nhóm mới"
                >
                  <Plus size={12} />
                  <span>Thêm</span>
                </button>
              </div>

              {selectedCategory && (
                <div
                  className="flyout-item clear-filter-item"
                  onClick={() => {
                    onSelectCategory(null);
                    setShowCategoryMenu(false);
                  }}
                >
                  <span style={{ fontSize: '0.78rem', color: 'var(--primary)', fontWeight: 600 }}>
                    ✕ Bỏ lọc nhóm: "{selectedCategory}"
                  </span>
                </div>
              )}

              <div className="flyout-list">
                {categories.map((cat) => {
                  const isSelected = selectedCategory === cat.name;
                  return (
                    <div
                      key={cat.id}
                      className={`flyout-item ${isSelected ? 'selected' : ''}`}
                      onClick={() => {
                        onSelectCategory(isSelected ? null : cat.name);
                        setShowCategoryMenu(false);
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8, flex: 1, minWidth: 0 }}>
                        <span className="category-dot" style={{ background: cat.colorCode }} />
                        <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                          {cat.name}
                        </span>
                      </div>

                      <div className="category-actions" style={{ opacity: 1, display: 'flex', gap: 2 }}>
                        <button
                          type="button"
                          className="cat-action-btn"
                          title={`Chỉnh sửa "${cat.name}"`}
                          onClick={(e) => {
                            e.stopPropagation();
                            onEditCategory(cat);
                            setShowCategoryMenu(false);
                          }}
                        >
                          <Pencil size={12} />
                        </button>
                        <button
                          type="button"
                          className="cat-action-btn delete-btn"
                          title={`Xóa "${cat.name}"`}
                          onClick={(e) => {
                            e.stopPropagation();
                            e.preventDefault();
                            setDeleteError(null);
                            setDeleteTarget({
                              type: 'category',
                              id: cat.id,
                              name: cat.name,
                              title: 'Xóa nhóm công việc',
                              message: `Bạn có chắc chắn muốn xóa nhóm "${cat.name}" khỏi danh mục không?`,
                              warningNote: 'Lưu ý: Không thể xóa nhóm nếu đang có công việc sử dụng nhóm này.',
                            });
                            setShowCategoryMenu(false);
                          }}
                        >
                          <Trash2 size={12} />
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
        </div>
      ) : (
        <div className="sidebar-section">
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 8, paddingRight: 4 }}>
            <h3 className="section-label" style={{ margin: 0 }}>NHÓM CỦA TÔI</h3>
            <button
              type="button"
              className="add-category-btn"
              onClick={onAddCategory}
              title="Thêm nhóm công việc mới"
            >
              <Plus size={14} />
            </button>
          </div>

          <div className="category-list">
            {categories.map((cat) => {
              const isSelected = selectedCategory === cat.name;
              return (
                <div
                  key={cat.id}
                  className={`category-item ${isSelected ? 'selected' : ''}`}
                  onClick={() => onSelectCategory(isSelected ? null : cat.name)}
                  style={{ position: 'relative' }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8, flex: 1, minWidth: 0 }}>
                    <span className="category-dot" style={{ background: cat.colorCode }} />
                    <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {cat.name}
                    </span>
                  </div>

                  {/* Edit Pencil & Trash Delete Buttons */}
                  <div className="category-actions">
                    <button
                      type="button"
                      className="cat-action-btn"
                      title={`Chỉnh sửa nhóm "${cat.name}"`}
                      onClick={(e) => {
                        e.stopPropagation();
                        onEditCategory(cat);
                      }}
                    >
                      <Pencil size={12} />
                    </button>
                    <button
                      type="button"
                      className="cat-action-btn delete-btn"
                      title={`Xóa nhóm "${cat.name}"`}
                      onClick={(e) => {
                        e.stopPropagation();
                        e.preventDefault();
                        setDeleteError(null);
                        setDeleteTarget({
                          type: 'category',
                          id: cat.id,
                          name: cat.name,
                          title: 'Xóa nhóm công việc',
                          message: `Bạn có chắc chắn muốn xóa nhóm "${cat.name}" khỏi danh mục không?`,
                          warningNote: 'Lưu ý: Không thể xóa nhóm nếu đang có công việc sử dụng nhóm này.',
                        });
                      }}
                    >
                      <Trash2 size={12} />
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* ── Settings Link (mt-auto pushes to bottom) ── */}
      <button
        className={`nav-item settings-link ${activeNav === 'settings' ? 'active' : ''}`}
        onClick={() => {
          onNavChange('settings');
          onOpenSettingsModal?.();
        }}
        title="Tài khoản & Thiết lập"
        style={{ cursor: 'pointer', marginTop: 'auto' }}
      >
        <Settings size={18} className="settings-gear-icon" />
        <span>Tài khoản & Thiết lập</span>
      </button>

      {/* ── User Profile ── */}
      <div
        className="profile-hover-container"
        ref={profileMenuRef}
        style={{ position: 'relative', width: '100%' }}
        onMouseEnter={handleProfileMouseEnter}
        onMouseLeave={handleProfileMouseLeave}
      >
        <div
          className={`user-profile ${showProfileMenu ? 'active' : ''}`}
          onClick={() => setShowProfileMenu(!showProfileMenu)}
          onMouseEnter={handleProfileMouseEnter}
          style={{ cursor: 'pointer' }}
          title={isCollapsed ? `${userName} (${currentUser ? currentUser.role : 'Tài khoản cá nhân'})` : 'Nhấp để xem tài khoản / Đăng xuất'}
        >
          <div className="user-avatar" style={{ overflow: 'hidden' }}>
            {currentUser?.avatarUrl ? (
              <img
                src={currentUser.avatarUrl}
                alt={userName}
                style={{ width: '100%', height: '100%', objectFit: 'cover' }}
              />
            ) : (
              userName.split(' ').map((w) => w[0]).join('').slice(0, 2)
            )}
          </div>
          <div className="user-info">
            <span className="user-name">{userName}</span>
            <span className="user-role">
              {currentUser ? currentUser.role : 'Tài khoản cá nhân'}
            </span>
          </div>
          <ChevronDown
            size={16}
            className="user-chevron-icon"
            style={{
              marginLeft: 'auto',
              color: '#94A3B8',
              transform: showProfileMenu ? 'rotate(180deg)' : 'rotate(0deg)',
            }}
          />
        </div>

        {/* Dropdown menu */}
        <div className={`profile-dropdown-menu ${showProfileMenu ? 'open' : ''}`}>
            {currentUser && (
              <div style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '10px 12px', borderBottom: '1px solid var(--border-light)' }}>
                <div style={{ width: 36, height: 36, borderRadius: '50%', overflow: 'hidden', flexShrink: 0, background: 'var(--primary-subtle)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 600, color: 'var(--primary)', fontSize: '0.82rem', border: '1px solid var(--border)' }}>
                  {currentUser.avatarUrl ? (
                    <img src={currentUser.avatarUrl} alt={currentUser.fullName} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                  ) : (
                    userName.split(' ').map((w) => w[0]).join('').slice(0, 2)
                  )}
                </div>
                <div style={{ minWidth: 0, overflow: 'hidden' }}>
                  <div style={{ fontWeight: 600, color: 'var(--text)', fontSize: '0.82rem', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{currentUser.fullName}</div>
                  <div style={{ fontSize: '0.72rem', color: 'var(--text-3)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{currentUser.email}</div>
                </div>
              </div>
            )}
            <button
              onClick={() => {
                setShowProfileMenu(false);
                onNavChange('settings');
              }}
              className="dropdown-item"
            >
              <Settings size={16} style={{ color: 'var(--primary)' }} />
              <span>Tài khoản & Thiết lập</span>
            </button>
            <button
              onClick={() => {
                setShowProfileMenu(false);
                onOpenAuthModal();
              }}
              className="dropdown-item"
            >
              <UserCircle size={16} style={{ color: 'var(--primary)' }} />
              <span>{currentUser ? 'Chuyển tài khoản' : 'Đăng nhập / Đăng ký'}</span>
            </button>
            {currentUser && (
              <button
                onClick={() => {
                  setShowProfileMenu(false);
                  onLogout();
                }}
                className="dropdown-item danger-item"
              >
                <LogOut size={16} />
                <span>Đăng xuất</span>
              </button>
            )}
          </div>
      </div>

      {/* ── In-App Confirm Delete Modal ── */}
      <ConfirmModal
        isOpen={deleteTarget !== null}
        title={deleteTarget?.title || 'Xác nhận xóa'}
        message={deleteTarget?.message || ''}
        warningNote={deleteTarget?.warningNote}
        errorMessage={deleteError}
        confirmText={deleteTarget?.type === 'workspace' ? 'Xóa không gian' : 'Xóa nhóm việc'}
        isLoading={isDeleting}
        onConfirm={handleExecuteDelete}
        onCancel={() => {
          if (!isDeleting) {
            setDeleteTarget(null);
            setDeleteError(null);
          }
        }}
      />
    </aside>
  );
}
