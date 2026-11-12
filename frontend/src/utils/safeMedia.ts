import { useState, useEffect } from 'react';

// Bộ nhớ đệm URL Blob để không tải lại cùng một tệp phương tiện nhiều lần
const blobCache = new Map<string, string>();

/**
 * Chuyển đổi URL video/ảnh thành Blob URL (`blob:http://...`).
 * Khi thẻ <video src="blob:..."> phát, tiện ích mở rộng IDM (Internet Download Manager)
 * hoàn toàn không thể đánh chặn (intercept) hay tự động kích hoạt tải xuống tệp video nền.
 */
export function useSafeMediaBlob(url?: string | null, isVideo: boolean = false): string | null {
  const [blobUrl, setBlobUrl] = useState<string | null>(() => {
    if (!url) return null;
    if (url.startsWith('blob:') || url.startsWith('data:')) return url;
    return blobCache.get(url) || null;
  });

  useEffect(() => {
    if (!url) {
      setBlobUrl(null);
      return;
    }

    if (url.startsWith('blob:') || url.startsWith('data:')) {
      setBlobUrl(url);
      return;
    }

    // Nếu đã có trong cache
    if (blobCache.has(url)) {
      setBlobUrl(blobCache.get(url)!);
      return;
    }

    let isMounted = true;
    const controller = new AbortController();

    // Tải tệp dưới dạng blob trong JavaScript ngầm
    fetch(url, { signal: controller.signal })
      .then((res) => {
        if (!res.ok) throw new Error('Không thể tải media blob');
        return res.blob();
      })
      .then((blob) => {
        if (!isMounted) return;
        const objectUrl = URL.createObjectURL(blob);
        blobCache.set(url, objectUrl);
        setBlobUrl(objectUrl);
      })
      .catch((_err) => {
        // Fallback về url gốc nếu fetch gặp lỗi (ví dụ offline hoặc cross-origin)
        if (isMounted) {
          setBlobUrl(url);
        }
      });

    return () => {
      isMounted = false;
      controller.abort();
    };
  }, [url, isVideo]);

  return blobUrl;
}
