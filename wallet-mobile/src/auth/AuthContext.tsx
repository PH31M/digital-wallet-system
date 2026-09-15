import { createContext, ReactNode, useCallback, useContext, useEffect, useState } from 'react';
import { setSessionExpiredHandler } from '../api/client';
import { tokenStorage } from '../api/tokenStorage';
import {
  forgotPasswordApi,
  getProfileApi,
  loginApi,
  logoutApi,
  registerApi,
  resendVerificationApi,
  resetPasswordApi,
  verifyEmailApi,
  verifyMfaApi,
} from '../api/authApi';
import { UserProfileResponse } from '../types/api';

export type AuthStatus = 'bootstrapping' | 'signedOut' | 'signedIn';

export type LoginResult = { mfaRequired: true } | { mfaRequired: false; user: UserProfileResponse };

type RegisterParams = {
  email: string;
  fullName: string;
  phoneNumber: string;
  password: string;
};

type AuthContextValue = {
  status: AuthStatus;
  user: UserProfileResponse | null;
  login: (email: string, password: string) => Promise<LoginResult>;
  verifyMfa: (email: string, otp: string) => Promise<void>;
  register: (params: RegisterParams) => Promise<UserProfileResponse>;
  verifyEmail: (userId: string, otp: string) => Promise<void>;
  resendVerification: (userId: string, email: string) => Promise<void>;
  forgotPassword: (email: string) => Promise<void>;
  resetPassword: (email: string, otp: string, newPassword: string) => Promise<void>;
  logout: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

/**
 * Nguồn sự thật duy nhất cho trạng thái đăng nhập — bọc quanh `RootNavigator` (xem App.tsx).
 * `status` quyết định Splash điều hướng vào Login hay Main (xem SplashScreen.tsx).
 *
 * XỬ LÝ tài khoản chưa xác thực email (bug thật gặp lúc test: đăng ký xong bỏ dở OTP, không
 * còn đường nào quay lại verify — `login()` vẫn cấp token bình thường vì AuthService.login
 * không chặn theo `emailVerifiedAt`, chỉ ví `wallet.status=PENDING_VERIFICATION` mới chặn giao
 * dịch tiền, và message lỗi lúc đó là "Wallet is frozen" rất khó hiểu):
 * - `login()`/`register()` trả kèm `user.isVerified` — LoginScreen/RegisterScreen tự điều
 *   hướng sang RegisterOtp thay vì Main nếu `isVerified=false`.
 * - Bootstrap (mở lại app với token cũ, tài khoản vẫn chưa verify) do `SplashScreen` tự kiểm
 *   tra `user.isVerified` sau khi `status` thành 'signedIn' rồi quyết định route — xem
 *   SplashScreen.tsx, không xử lý ở đây vì AuthContext không tự điều hướng.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>('bootstrapping');
  const [user, setUser] = useState<UserProfileResponse | null>(null);

  const forceSignedOut = useCallback(() => {
    setUser(null);
    setStatus('signedOut');
  }, []);

  // Đăng ký handler cho client.ts gọi khi refresh token cũng hết hạn/không còn hợp lệ (401 xảy
  // ra ở BẤT KỲ request nào, không riêng lúc bootstrap) — xem client.ts:handleResponseError.
  useEffect(() => {
    setSessionExpiredHandler(forceSignedOut);
    return () => setSessionExpiredHandler(null);
  }, [forceSignedOut]);

  useEffect(() => {
    let cancelled = false;

    (async () => {
      // Bọc TOÀN BỘ khối bootstrap, không chỉ phần gọi getProfileApi() — nếu
      // tokenStorage.getRefreshToken() tự nó throw (vd nền tảng không hỗ trợ storage nào đó),
      // thiếu try/catch ở đây sẽ để `status` kẹt mãi ở 'bootstrapping', Splash không bao giờ
      // điều hướng đi đâu cả (bug thật đã gặp khi test: SecureStore không chạy trên Expo Web).
      try {
        const refreshToken = await tokenStorage.getRefreshToken();
        if (!refreshToken) {
          if (!cancelled) setStatus('signedOut');
          return;
        }
        const profile = await getProfileApi();
        if (!cancelled) {
          setUser(profile);
          setStatus('signedIn');
        }
      } catch {
        await tokenStorage.clearTokens().catch(() => undefined);
        if (!cancelled) setStatus('signedOut');
      }
    })();

    return () => {
      cancelled = true;
    };
  }, []);

  const login = useCallback(async (email: string, password: string): Promise<LoginResult> => {
    const response = await loginApi(email, password);
    if (response.mfaRequired) {
      // Không có access/refresh token trong nhánh này (AuthService.login trả null, null khi
      // mfaEnabled=true) — không lưu gì cả, chờ verifyMfa() hoàn tất đăng nhập thật.
      return { mfaRequired: true };
    }
    await tokenStorage.setTokens(response.accessToken, response.refreshToken);
    setUser(response.user);
    setStatus('signedIn');
    return { mfaRequired: false, user: response.user };
  }, []);

  const verifyMfa = useCallback(async (email: string, otp: string) => {
    const response = await verifyMfaApi(email, otp);
    await tokenStorage.setTokens(response.accessToken, response.refreshToken);
    setUser(response.user);
    setStatus('signedIn');
  }, []);

  const register = useCallback(async (params: RegisterParams) => {
    const response = await registerApi(params);
    await tokenStorage.setTokens(response.accessToken, response.refreshToken);
    setUser(response.user);
    setStatus('signedIn');
    return response.user;
  }, []);

  const verifyEmail = useCallback(async (userId: string, otp: string) => {
    await verifyEmailApi(userId, otp);
    setUser((prev) => (prev ? { ...prev, isVerified: true } : prev));
  }, []);

  const logout = useCallback(async () => {
    const refreshToken = await tokenStorage.getRefreshToken();
    if (refreshToken) {
      // Best-effort — logout vẫn phải dọn state cục bộ dù request thất bại (mất mạng...).
      await logoutApi(refreshToken).catch(() => undefined);
    }
    await tokenStorage.clearTokens();
    forceSignedOut();
  }, [forceSignedOut]);

  return (
    <AuthContext.Provider
      value={{
        status,
        user,
        login,
        verifyMfa,
        register,
        verifyEmail,
        resendVerification: resendVerificationApi,
        forgotPassword: forgotPasswordApi,
        resetPassword: resetPasswordApi,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth phải được dùng bên trong AuthProvider');
  return ctx;
}
