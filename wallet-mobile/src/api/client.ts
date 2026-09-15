import { Platform } from 'react-native';
import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { toCamelCase } from './caseTransform';
import { createSingleFlight } from './singleFlight';
import { tokenStorage } from './tokenStorage';
import { ApiResponse, AuthResponse } from '../types/api';

/**
 * Base URL PHẢI trỏ tới prefix `/api` (đã chốt với backend — 2 controller có alias kép
 * `/api/...` và `/api/v1/...` hoàn toàn tương đương, chọn `/api` cho nhất quán).
 *
 * Nguồn giá trị, theo thứ tự ưu tiên:
 * 1. `EXPO_PUBLIC_API_BASE_URL` trong `.env` (hoặc `.env.local`) — Expo CLI tự inline biến
 *    `EXPO_PUBLIC_*` vào bundle, KHÔNG cần `expo-constants` (xem docs.expo.dev/guides/
 *    environment-variables — cách này thay thế cách cũ dùng expo-constants). Phải là
 *    `process.env.EXPO_PUBLIC_API_BASE_URL` viết tĩnh — dùng bracket notation hay biến trung
 *    gian sẽ KHÔNG được inline.
 * 2. Nếu không set (.env để trống/comment) → tự chọn default theo platform:
 *    - Android emulator (`Platform.OS === 'android'` và `__DEV__`) → `10.0.2.2:8089` (loopback
 *      đặc biệt của Android emulator trỏ về máy host — KHÔNG dùng `localhost`).
 *    - Còn lại (iOS Simulator, Expo Web, hoặc build production) → `localhost:8089`.
 *
 * Đổi base URL cho từng cách test — set `EXPO_PUBLIC_API_BASE_URL` trong `.env` rồi restart
 * `expo start` (biến chỉ đọc lúc build bundle, không hot-reload):
 * - Android Emulator: để trống — default `10.0.2.2:8089/api` đã đúng.
 * - iOS Simulator / Expo Web: để trống — default `localhost:8089/api` đã đúng.
 * - Thiết bị thật (cùng mạng LAN): `EXPO_PUBLIC_API_BASE_URL=http://<IP-LAN-cua-may-host>:8089/api`.
 * - Tunnel (ngrok, `expo start --tunnel`, thiết bị thật khác mạng):
 *   `EXPO_PUBLIC_API_BASE_URL=https://<tunnel-host>/api`.
 * Mọi lời gọi trong code (hooks/services) dùng path KHÔNG lặp lại `/api`, ví dụ
 * `apiClient.get('/wallets/me')`.
 */
function resolveApiBaseUrl(): string {
  const override = process.env.EXPO_PUBLIC_API_BASE_URL;
  if (override) {
    return override;
  }
  if (__DEV__ && Platform.OS === 'android') {
    return 'http://10.0.2.2:8089/api';
  }
  return 'http://localhost:8089/api';
}

const API_BASE_URL = resolveApiBaseUrl();

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
});

// Instance riêng, KHÔNG gắn interceptor — dùng để tự gọi /auth/refresh mà không bị vòng lặp
// (nếu dùng apiClient cho refresh, request refresh thất bại 401 sẽ tự gọi refresh chính nó).
const refreshClient = axios.create({
  baseURL: API_BASE_URL,
});

type SessionExpiredHandler = () => void;
let sessionExpiredHandler: SessionExpiredHandler | null = null;

/** Gọi từ AuthContext (Phase Auth) để biết khi nào cần điều hướng về Login + xoá state user. */
export function setSessionExpiredHandler(handler: SessionExpiredHandler | null): void {
  sessionExpiredHandler = handler;
}

apiClient.interceptors.request.use(async (config) => {
  const token = await tokenStorage.getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => {
    response.data = toCamelCase(response.data);
    return response;
  },
  (error: AxiosError) => handleResponseError(error),
);

type RetriableConfig = InternalAxiosRequestConfig & { _retried?: boolean };

async function refreshAccessToken(): Promise<string | null> {
  const refreshToken = await tokenStorage.getRefreshToken();
  if (!refreshToken) {
    return null;
  }

  try {
    const response = await refreshClient.post('/auth/refresh', { refreshToken });
    const body = toCamelCase<ApiResponse<AuthResponse>>(response.data);
    await tokenStorage.setTokens(body.data.accessToken, body.data.refreshToken);
    return body.data.accessToken;
  } catch {
    return null;
  }
}

// Nhiều request có thể 401 cùng lúc (vd. gọi song song lúc mở app) — createSingleFlight đảm
// bảo chỉ 1 lệnh gọi /auth/refresh thật, mọi request khác chờ chung kết quả (xem singleFlight.ts
// + test race condition tại __tests__/singleFlight.test.ts).
const refreshAccessTokenOnce = createSingleFlight(refreshAccessToken);

async function handleResponseError(error: AxiosError) {
  // Nhánh thành công đã transform response.data ở trên — nhánh lỗi (status != 2xx, đi qua
  // interceptor lỗi thay vì interceptor thành công) trước đây BỎ SÓT bước này, nghĩa là
  // error.response.data.error (ApiErrorInfo) chưa từng được camelCase hoá. Hiện các field của
  // ErrorInfo.java vốn không có dấu gạch dưới nên vô hại, nhưng transform ở đây để nhất quán,
  // phòng trường hợp backend thêm field mới.
  if (error.response) {
    error.response.data = toCamelCase(error.response.data);
  }

  const config = error.config as RetriableConfig | undefined;
  const isAuthEndpoint = config?.url?.includes('/auth/') ?? false;

  if (error.response?.status !== 401 || !config || config._retried || isAuthEndpoint) {
    return Promise.reject(error);
  }

  config._retried = true;
  const newAccessToken = await refreshAccessTokenOnce();

  if (!newAccessToken) {
    await tokenStorage.clearTokens();
    sessionExpiredHandler?.();
    return Promise.reject(error);
  }

  config.headers.Authorization = `Bearer ${newAccessToken}`;
  return apiClient(config);
}
