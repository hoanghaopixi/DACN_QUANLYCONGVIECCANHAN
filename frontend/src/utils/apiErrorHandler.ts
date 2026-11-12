/**
 * Chuẩn hóa và bóc tách thông báo lỗi từ Axios / API response cho toàn bộ Web Frontend.
 * Đảm bảo mọi mã trạng thái HTTP hoặc ngoại lệ kết nối mạng đều hiển thị bằng tiếng Việt rõ ràng,
 * tuyệt đối không in ra các con số kỹ thuật (400, 401, 500...) hay raw exception strings.
 */
export function formatApiErrorMessage(err: any, fallbackMessage: string = 'Thao tác không thành công. Vui lòng thử lại.'): string {
  if (!err) return fallbackMessage;

  // 1. Nếu là chuỗi string trực tiếp
  if (typeof err === 'string') {
    return sanitizeErrorMessage(err, fallbackMessage);
  }

  // 2. Phản hồi JSON từ backend (message, error, detail)
  const data = err.response?.data;
  if (data) {
    if (typeof data === 'object') {
      const msg = data.message || data.error || data.detail;
      if (typeof msg === 'string' && msg.trim()) {
        const clean = sanitizeErrorMessage(msg, '');
        if (clean) return clean;
      }
    } else if (typeof data === 'string' && data.trim() && !data.startsWith('<')) {
      const clean = sanitizeErrorMessage(data, '');
      if (clean) return clean;
    }
  }

  // 3. Phân loại theo mã HTTP Status Code
  const status = err.response?.status;
  if (status === 400) {
    return 'Dữ liệu yêu cầu không hợp lệ hoặc không đúng định dạng.';
  }
  if (status === 401) {
    return 'Phiên làm việc đã hết hạn hoặc thông tin đăng nhập không chính xác.';
  }
  if (status === 403) {
    return 'Bạn không có quyền thực hiện thao tác này.';
  }
  if (status === 404) {
    return 'Không tìm thấy dữ liệu yêu cầu hoặc nội dung đã bị xóa.';
  }
  if (status === 409) {
    return 'Dữ liệu này đã tồn tại hoặc đang có ràng buộc dữ liệu.';
  }
  if (status === 422) {
    return 'Thông tin cung cấp chưa đầy đủ hoặc không đúng quy định xử lý.';
  }
  if (status === 429) {
    return 'Hệ thống đang nhận nhiều lượt truy cập cùng lúc. Vui lòng đợi trong giây lát.';
  }
  if (status && status >= 500) {
    return 'Hệ thống máy chủ đang gặp sự cố tạm thời. Vui lòng thử lại sau ít phút.';
  }

  // 4. Ngoại lệ mạng / kết nối
  if (err.code === 'ERR_NETWORK' || (typeof err.message === 'string' && err.message.includes('Network Error'))) {
    return 'Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối mạng.';
  }
  if (err.code === 'ECONNABORTED' || (typeof err.message === 'string' && err.message.includes('timeout'))) {
    return 'Thời gian phản hồi máy chủ quá lâu. Vui lòng thử lại sau ít phút.';
  }

  // 5. Chuỗi message từ Error object nếu không chứa status code / technical terms
  if (typeof err.message === 'string' && err.message.trim()) {
    const clean = sanitizeErrorMessage(err.message, '');
    if (clean) return clean;
  }

  return fallbackMessage;
}

function sanitizeErrorMessage(msg: string, fallback: string): string {
  if (isTechnical(msg)) {
    return fallback || 'Thao tác không thành công. Vui lòng thử lại.';
  }
  const clean = msg
    .replace(/\(?[Mm]ã\s*lỗi\s*[:=]?\s*\d+\)?/gi, '')
    .replace(/\(?[Ll]ỗi\s*\d+\)?/gi, '')
    .replace(/[Ss]tatus\s*[Cc]ode\s*[:=]?\s*\d+/gi, '')
    .replace(/\bHTTP\s*\d{3}\b/gi, '')
    .replace(/Request failed with status code \d+/gi, '')
    .trim();
  return clean || fallback;
}

function isTechnical(text: string): boolean {
  const lower = text.toLowerCase();
  return (
    lower.includes('internal server error') ||
    lower.includes('status code') ||
    lower.includes('bad request') ||
    lower.includes('failed to connect') ||
    lower.includes('nullpointer') ||
    lower.includes('sql') ||
    lower.includes('syntaxerror')
  );
}
