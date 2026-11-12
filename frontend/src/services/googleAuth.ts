import { authService, AuthResponse } from './authService';

export const GOOGLE_CLIENT_ID = '465213819257-jqupte1qp12afa0bpngr44dsv49utdll.apps.googleusercontent.com';

export interface GoogleUserProfile {
  sub: string;
  name: string;
  given_name?: string;
  family_name?: string;
  picture?: string;
  email: string;
  email_verified?: boolean;
}

export async function fetchGoogleUserInfo(accessToken: string): Promise<GoogleUserProfile> {
  const res = await fetch('https://www.googleapis.com/oauth2/v3/userinfo', {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });
  if (!res.ok) {
    throw new Error('Không thể lấy thông tin tài khoản từ Google.');
  }
  return res.json();
}

/**
 * Mở cửa sổ trình duyệt (browser popup) của Google để người dùng chọn tài khoản và đăng nhập.
 * Tuyệt đối không dùng prompt hay hộp thoại hệ thống.
 */
export function openGoogleLoginPopup(): Promise<AuthResponse> {
  return new Promise((resolve, reject) => {
    const google = (window as any).google;

    // 1. Ưu tiên: Sử dụng Google Identity Services (GIS) Token Client
    if (google?.accounts?.oauth2?.initTokenClient) {
      try {
        let isSettled = false;
        let hasLostFocus = false;

        const cleanup = () => {
          window.removeEventListener('blur', handleBlur);
          window.removeEventListener('focus', handleFocus);
        };

        const handleBlur = () => {
          hasLostFocus = true;
        };

        const handleFocus = () => {
          if (hasLostFocus && !isSettled) {
            // Khi người dùng đóng popup và quay lại tab chính
            setTimeout(() => {
              if (!isSettled) {
                isSettled = true;
                cleanup();
                reject(new Error('Cửa sổ đăng nhập Google đã được đóng.'));
              }
            }, 600);
          }
        };

        window.addEventListener('blur', handleBlur);
        window.addEventListener('focus', handleFocus);

        const tokenClient = google.accounts.oauth2.initTokenClient({
          client_id: GOOGLE_CLIENT_ID,
          scope: 'email profile openid',
          prompt: 'select_account',
          callback: async (tokenResponse: any) => {
            if (isSettled) return;
            cleanup();
            if (tokenResponse?.error) {
              isSettled = true;
              reject(new Error(tokenResponse.error_description || tokenResponse.error));
              return;
            }
            if (tokenResponse?.access_token) {
              try {
                const userInfo = await fetchGoogleUserInfo(tokenResponse.access_token);
                const authResult = await authService.loginWithGoogle({
                  idToken: userInfo.sub || tokenResponse.access_token,
                  email: userInfo.email,
                  fullName: userInfo.name || userInfo.email.split('@')[0],
                  avatarUrl: userInfo.picture,
                });
                isSettled = true;
                resolve(authResult);
              } catch (err) {
                isSettled = true;
                reject(err);
              }
            } else {
              isSettled = true;
              reject(new Error('Không nhận được token xác thực từ Google.'));
            }
          },
          error_callback: (error: any) => {
            if (isSettled) return;
            isSettled = true;
            cleanup();
            reject(new Error(error?.message || 'Cửa sổ đăng nhập Google đã được đóng.'));
          },
        });

        tokenClient.requestAccessToken({ prompt: 'select_account' });
        return;
      } catch (err) {
        // Nếu GIS gặp lỗi, chuyển sang mở cửa sổ popup trực tiếp
      }
    }

    // 2. Dự phòng: Mở trực tiếp cửa sổ trình duyệt chuẩn OAuth 2.0 của Google (window.open)
    const width = 500;
    const height = 620;
    const left = window.screenX + Math.max(0, (window.outerWidth - width) / 2);
    const top = window.screenY + Math.max(0, (window.outerHeight - height) / 2);
    const redirectUri = window.location.origin;

    const oauthUrl = `https://accounts.google.com/o/oauth2/v2/auth?client_id=${GOOGLE_CLIENT_ID}&redirect_uri=${encodeURIComponent(
      redirectUri
    )}&response_type=token&scope=${encodeURIComponent('email profile openid')}&prompt=select_account`;

    const popup = window.open(
      oauthUrl,
      'google_oauth_window',
      `width=${width},height=${height},left=${left},top=${top},status=no,toolbar=no,menubar=no`
    );

    if (!popup) {
      reject(new Error('Trình duyệt đã chặn cửa sổ Popup. Vui lòng cho phép popup trên trang web này để đăng nhập Google.'));
      return;
    }

    // Kiểm tra định kỳ cửa sổ popup khi chuyển hướng về origin
    const interval = setInterval(async () => {
      try {
        if (popup.closed) {
          clearInterval(interval);
          reject(new Error('Cửa sổ đăng nhập Google đã được đóng.'));
          return;
        }

        if (popup.location && popup.location.origin === window.location.origin) {
          const hash = popup.location.hash;
          if (hash && hash.includes('access_token')) {
            clearInterval(interval);
            const params = new URLSearchParams(hash.substring(1));
            const accessToken = params.get('access_token');
            popup.close();

            if (accessToken) {
              const userInfo = await fetchGoogleUserInfo(accessToken);
              const authResult = await authService.loginWithGoogle({
                idToken: userInfo.sub || accessToken,
                email: userInfo.email,
                fullName: userInfo.name || userInfo.email.split('@')[0],
                avatarUrl: userInfo.picture,
              });
              resolve(authResult);
            } else {
              reject(new Error('Không tìm thấy token trong phản hồi từ Google.'));
            }
          }
        }
      } catch (e) {
        // Chờ Google chuyển hướng hoàn tất (cross-origin ở giai đoạn này là bình thường)
      }
    }, 500);
  });
}
