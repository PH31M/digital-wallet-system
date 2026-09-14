import { apiClient } from './client';
import { ApiResponse, AuthResponse, UserProfileResponse } from '../types/api';

/**
 * Body request gửi lên backend PHẢI đúng JSON key thật mà từng DTO Java đọc (không đi qua
 * transform — transform chỉ áp cho response nhận về). Đã đối chiếu tay từng field với
 * `@JsonProperty` trong `wallet-core/.../api/dto/request/*.java`, ghi rõ field nào backend
 * KHÔNG annotate (giữ nguyên camelCase) vì đây là chỗ dễ gõ nhầm thành snake_case cả cụm:
 * - RegisterRequest: `full_name` có @JsonProperty, nhưng `phoneNumber` KHÔNG — giữ camelCase.
 * - RefreshTokenRequest: `refreshToken` KHÔNG có @JsonProperty — giữ camelCase (không phải
 *   `refresh_token` như tên gợi ý).
 * - ResetPasswordRequest/VerifyEmailRequest/ResendVerificationRequest: `new_password`/`user_id`
 *   có @JsonProperty snake_case thật.
 */

function unwrap<T>(data: ApiResponse<T>): T {
  return data.data;
}

export async function loginApi(email: string, password: string): Promise<AuthResponse> {
  const response = await apiClient.post<ApiResponse<AuthResponse>>('/auth/login', { email, password });
  return unwrap(response.data);
}

export async function registerApi(params: {
  email: string;
  fullName: string;
  phoneNumber: string;
  password: string;
}): Promise<AuthResponse> {
  const response = await apiClient.post<ApiResponse<AuthResponse>>('/auth/register', {
    email: params.email,
    full_name: params.fullName,
    phoneNumber: params.phoneNumber,
    password: params.password,
  });
  return unwrap(response.data);
}

export async function logoutApi(refreshToken: string): Promise<void> {
  await apiClient.post<ApiResponse<null>>('/auth/logout', { refreshToken });
}

export async function verifyMfaApi(email: string, otp: string): Promise<AuthResponse> {
  const response = await apiClient.post<ApiResponse<AuthResponse>>('/auth/mfa/verify', { email, otp });
  return unwrap(response.data);
}

export async function forgotPasswordApi(email: string): Promise<void> {
  await apiClient.post<ApiResponse<null>>('/auth/forgot-password', { email });
}

export async function resetPasswordApi(email: string, otp: string, newPassword: string): Promise<void> {
  await apiClient.post<ApiResponse<null>>('/auth/reset-password', {
    email,
    otp,
    new_password: newPassword,
  });
}

export async function verifyEmailApi(userId: string, otp: string): Promise<void> {
  await apiClient.post<ApiResponse<null>>('/auth/verify-email', { user_id: userId, otp });
}

export async function resendVerificationApi(userId: string, email: string): Promise<void> {
  await apiClient.post<ApiResponse<null>>('/auth/resend-verification', { user_id: userId, email });
}

export async function getProfileApi(): Promise<UserProfileResponse> {
  const response = await apiClient.get<ApiResponse<UserProfileResponse>>('/users/me');
  return unwrap(response.data);
}
