import { Platform } from 'react-native';
import * as SecureStore from 'expo-secure-store';

/**
 * JWT access/refresh token PHẢI lưu bằng expo-secure-store (mã hoá), không dùng AsyncStorage
 * thuần — quyết định đã chốt từ Phase 0 cho app tài chính (xem task-brief Phase 0-1). Quyết
 * định đó áp dụng cho build native (iOS/Android) — nơi thật sự chạy production.
 *
 * PHÁT HIỆN khi test qua Expo Web (Browser tool — cách kiểm thử trực quan đã dùng suốt từ
 * Phase 1 tới giờ, xem báo cáo hiện trạng): `expo-secure-store` KHÔNG có implementation cho
 * web, gọi bất kỳ method nào cũng throw `ExpoSecureStore.default.getValueWithKeyAsync is not
 * a function`. Không fallback thì toàn bộ luồng Auth không test được qua Expo Web nữa — chặn
 * đúng cách kiểm thử đã thiết lập cho cả dự án, không chỉ ảnh hưởng phiên này.
 * Fallback sang `localStorage` CHỈ khi `Platform.OS === 'web'` (không hạ chuẩn bảo mật trên
 * native) — web ở đây chỉ là môi trường dev/preview, không phải mục tiêu production của app.
 */
const ACCESS_TOKEN_KEY = 'accessToken';
const REFRESH_TOKEN_KEY = 'refreshToken';

const isWeb = Platform.OS === 'web';

function webGet(key: string): string | null {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}

function webSet(key: string, value: string): void {
  try {
    localStorage.setItem(key, value);
  } catch {
    // Private browsing / site data bị chặn — bỏ qua, không phải môi trường production.
  }
}

function webRemove(key: string): void {
  try {
    localStorage.removeItem(key);
  } catch {
    // no-op
  }
}

export const tokenStorage = {
  async getAccessToken(): Promise<string | null> {
    return isWeb ? webGet(ACCESS_TOKEN_KEY) : SecureStore.getItemAsync(ACCESS_TOKEN_KEY);
  },

  async getRefreshToken(): Promise<string | null> {
    return isWeb ? webGet(REFRESH_TOKEN_KEY) : SecureStore.getItemAsync(REFRESH_TOKEN_KEY);
  },

  async setTokens(accessToken: string, refreshToken: string): Promise<void> {
    if (isWeb) {
      webSet(ACCESS_TOKEN_KEY, accessToken);
      webSet(REFRESH_TOKEN_KEY, refreshToken);
      return;
    }
    await Promise.all([
      SecureStore.setItemAsync(ACCESS_TOKEN_KEY, accessToken),
      SecureStore.setItemAsync(REFRESH_TOKEN_KEY, refreshToken),
    ]);
  },

  async clearTokens(): Promise<void> {
    if (isWeb) {
      webRemove(ACCESS_TOKEN_KEY);
      webRemove(REFRESH_TOKEN_KEY);
      return;
    }
    await Promise.all([
      SecureStore.deleteItemAsync(ACCESS_TOKEN_KEY),
      SecureStore.deleteItemAsync(REFRESH_TOKEN_KEY),
    ]);
  },
};
