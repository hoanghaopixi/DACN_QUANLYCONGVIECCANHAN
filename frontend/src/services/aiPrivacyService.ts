import { WorkspaceItem } from './workspaceService';

const STORAGE_KEY = 'smarttask_ai_allowed_workspaces';
const EVENT_NAME = 'smarttask_ai_privacy_changed';

export interface AiPrivacyState {
  allowedWorkspaceIds: number[];
}

export const aiPrivacyService = {
  /**
   * Lấy danh sách ID các không gian được phép cho AI truy cập.
   * Nếu người dùng chưa từng cấu hình, mặc định cho phép tất cả các không gian hiện có.
   */
  getAllowedWorkspaceIds(allWorkspaces?: WorkspaceItem[]): number[] {
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      if (stored !== null) {
        const parsed = JSON.parse(stored);
        if (Array.isArray(parsed)) {
          return parsed.map(Number);
        }
      }
    } catch (e) {
      console.warn('Lỗi đọc cấu hình quyền AI:', e);
    }
    // Mặc định: Nếu chưa cấu hình, cho phép tất cả workspaces hiện có
    if (allWorkspaces && allWorkspaces.length > 0) {
      return allWorkspaces.map((w) => w.id);
    }
    return [];
  },

  /**
   * Lưu danh sách ID không gian được phép truy cập
   */
  setAllowedWorkspaceIds(ids: number[]): void {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(ids));
      window.dispatchEvent(new CustomEvent(EVENT_NAME, { detail: { allowedIds: ids } }));
    } catch (e) {
      console.error('Lỗi lưu cấu hình quyền AI:', e);
    }
  },

  /**
   * Kiểm tra một không gian cụ thể có được phép AI truy cập không
   */
  isWorkspaceAllowed(workspaceId: number | undefined | null, allWorkspaces?: WorkspaceItem[]): boolean {
    if (workspaceId == null) return true; // Default workspace
    const allowed = this.getAllowedWorkspaceIds(allWorkspaces);
    return allowed.includes(Number(workspaceId));
  },

  /**
   * Bật/tắt quyền AI cho một không gian làm việc
   */
  toggleWorkspace(workspaceId: number, allWorkspaces?: WorkspaceItem[]): boolean {
    const current = this.getAllowedWorkspaceIds(allWorkspaces);
    let next: number[];
    const idNum = Number(workspaceId);
    if (current.includes(idNum)) {
      next = current.filter((id) => id !== idNum);
    } else {
      next = [...current, idNum];
    }
    this.setAllowedWorkspaceIds(next);
    return next.includes(idNum);
  },

  /**
   * Cho phép tất cả không gian
   */
  allowAll(allWorkspaces: WorkspaceItem[]): void {
    const ids = allWorkspaces.map((w) => w.id);
    this.setAllowedWorkspaceIds(ids);
  },

  /**
   * Khóa tất cả ngoại trừ 1 không gian (hoặc khóa hết)
   */
  lockAllExcept(exceptWorkspaceId?: number): void {
    const ids = exceptWorkspaceId != null ? [Number(exceptWorkspaceId)] : [];
    this.setAllowedWorkspaceIds(ids);
  },

  /**
   * Đăng ký lắng nghe sự kiện thay đổi quyền AI
   */
  subscribe(callback: (allowedIds: number[]) => void): () => void {
    const handler = (e: any) => {
      callback(e.detail?.allowedIds || []);
    };
    window.addEventListener(EVENT_NAME, handler);
    return () => window.removeEventListener(EVENT_NAME, handler);
  },
};
