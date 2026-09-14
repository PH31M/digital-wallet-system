import { AxiosError } from 'axios';
import { ApiResponse } from '../types/api';

/**
 * Đọc `error.code`/`error.message` thật từ ApiResponse envelope (ErrorInfo.java) khi 1 lời
 * gọi apiClient reject — dùng để hiện toast đúng nội dung backend trả về thay vì thông báo
 * chung chung, và để so khớp theo `code` (vd "ACCOUNT_LOCKED", "OTP_EXPIRED") khi cần rẽ nhánh
 * UX khác nhau theo loại lỗi.
 */
export function getApiErrorCode(error: unknown): string | null {
  const body = (error as AxiosError<ApiResponse<unknown>>)?.response?.data;
  return body?.error?.code ?? null;
}

export function getApiErrorMessage(error: unknown, fallback: string): string {
  const body = (error as AxiosError<ApiResponse<unknown>>)?.response?.data;
  return body?.error?.message ?? fallback;
}
