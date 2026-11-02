export const EMAIL_REGEX = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
export const PASSWORD_REGEX = /^.{1,50}$/;

export interface ValidationResult {
  isValid: boolean;
  message?: string;
  field?: 'email' | 'password' | 'fullName' | 'confirmPassword';
}

/**
 * Kiểm tra định dạng Email theo chuẩn Regex
 */
export function validateEmail(email: string): ValidationResult {
  const trimmed = (email || '').trim();
  if (!trimmed) {
    return {
      isValid: false,
      message: 'Vui lòng nhập địa chỉ email.',
      field: 'email',
    };
  }
  if (!EMAIL_REGEX.test(trimmed)) {
    return {
      isValid: false,
      message: 'Định dạng email không hợp lệ (Ví dụ: name@example.com).',
      field: 'email',
    };
  }
  return { isValid: true };
}

/**
 * Kiểm tra định dạng và độ dài Mật khẩu
 */
export function validatePassword(password: string): ValidationResult {
  const pass = password || '';
  if (!pass) {
    return {
      isValid: false,
      message: 'Vui lòng nhập mật khẩu.',
      field: 'password',
    };
  }
  if (pass.length > 50) {
    return {
      isValid: false,
      message: 'Mật khẩu không được vượt quá 50 ký tự.',
      field: 'password',
    };
  }
  return { isValid: true };
}

/**
 * Kiểm tra Họ và Tên khi đăng ký
 */
export function validateFullName(name: string): ValidationResult {
  const trimmed = (name || '').trim();
  if (!trimmed) {
    return {
      isValid: false,
      message: 'Vui lòng nhập họ và tên của bạn.',
      field: 'fullName',
    };
  }
  if (trimmed.length < 2) {
    return {
      isValid: false,
      message: 'Họ và tên phải có tối thiểu 2 ký tự.',
      field: 'fullName',
    };
  }
  if (trimmed.length > 100) {
    return {
      isValid: false,
      message: 'Họ và tên không được vượt quá 100 ký tự.',
      field: 'fullName',
    };
  }
  return { isValid: true };
}

/**
 * Kiểm tra toàn bộ Form Đăng Nhập
 */
export function validateLoginForm(email: string, password: string): ValidationResult {
  const emailRes = validateEmail(email);
  if (!emailRes.isValid) return emailRes;

  const passRes = validatePassword(password);
  if (!passRes.isValid) return passRes;

  return { isValid: true };
}

/**
 * Kiểm tra toàn bộ Form Đăng Ký
 */
export function validateRegisterForm(
  fullName: string,
  email: string,
  password: string,
  confirmPassword: string
): ValidationResult {
  const nameRes = validateFullName(fullName);
  if (!nameRes.isValid) return nameRes;

  const emailRes = validateEmail(email);
  if (!emailRes.isValid) return emailRes;

  const passRes = validatePassword(password);
  if (!passRes.isValid) return passRes;

  if (!confirmPassword) {
    return {
      isValid: false,
      message: 'Vui lòng nhập lại mật khẩu để xác nhận.',
      field: 'confirmPassword',
    };
  }
  if (password !== confirmPassword) {
    return {
      isValid: false,
      message: 'Mật khẩu xác nhận không trùng khớp.',
      field: 'confirmPassword',
    };
  }

  return { isValid: true };
}

/**
 * Xử lý lỗi trả về từ API Backend, loại bỏ hoàn toàn các chuỗi thô kiểu "Request failed with status code ..."
 * và trả về câu thông báo tiếng Việt chi tiết, thân thiện.
 */
export function formatAuthErrorMessage(err: any): string {
  if (!err) {
    return 'Đã xảy ra lỗi không xác định. Vui lòng thử lại.';
  }

  // 1. Phản hồi thông báo trực tiếp từ backend (nếu có message hoặc error)
  const backendMsg = err.response?.data?.message || err.response?.data?.error;
  if (backendMsg && typeof backendMsg === 'string' && backendMsg !== 'Internal Server Error') {
    return backendMsg;
  }

  if (
    typeof err.response?.data === 'string' &&
    err.response.data.trim().length > 0 &&
    !err.response.data.startsWith('<!DOCTYPE') &&
    !err.response.data.startsWith('<html')
  ) {
    return err.response.data;
  }

  // 2. Phân loại theo mã HTTP Status Code
  const status = err.response?.status;
  if (status === 401) {
    return 'Mật khẩu không chính xác. Vui lòng kiểm tra lại!';
  }
  if (status === 404) {
    return 'Tài khoản email này chưa được đăng ký trong hệ thống.';
  }
  if (status === 403) {
    return 'Tài khoản của bạn đã bị khóa hoặc chưa kích hoạt. Vui lòng liên hệ quản trị viên.';
  }
  if (status === 400) {
    return 'Thông tin đăng nhập không hợp lệ. Vui lòng kiểm tra lại email và mật khẩu.';
  }
  if (status === 409) {
    return 'Địa chỉ email này đã được sử dụng. Vui lòng đăng nhập hoặc dùng email khác.';
  }
  if (status >= 500) {
    return 'Máy chủ đang gặp sự cố tạm thời (Lỗi 500). Vui lòng thử lại sau.';
  }

  // 3. Phân loại theo lỗi mạng hoặc kết nối server
  if (err.code === 'ERR_NETWORK' || err.message?.includes('Network Error')) {
    return 'Không thể kết nối đến máy chủ. Vui lòng kiểm tra mạng hoặc đảm bảo backend đang chạy.';
  }
  if (err.code === 'ECONNABORTED' || err.message?.includes('timeout')) {
    return 'Thời gian phản hồi máy chủ quá lâu (Hết thời gian chờ). Vui lòng thử lại.';
  }

  return 'Đăng nhập không thành công. Vui lòng kiểm tra lại thông tin tài khoản!';
}

