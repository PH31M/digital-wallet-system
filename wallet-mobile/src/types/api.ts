/**
 * Kiểu dữ liệu khớp với DTO thật bên backend (wallet-core) SAU KHI đi qua lớp transform
 * snake_case -> camelCase toàn cục ở `src/api/client.ts` — mọi field ở đây dùng camelCase,
 * kể cả những field backend trả về snake_case qua JSON (`full_name`, `access_token`...).
 * Đối chiếu trực tiếp source Java tại `digital-wallet-system/wallet-core/.../api/dto/`,
 * xem chi tiết endpoint tại `wallet-mobile/docs/api-endpoints-thuc-te-tren-dev-14-09.md`.
 */

export type TransactionType = 'DEPOSIT' | 'WITHDRAW' | 'TRANSFER' | 'REVERSAL';

export type TransactionDirection = 'CREDIT' | 'DEBIT';

export type TransactionStatus =
  | 'PENDING'
  | 'PROCESSING'
  | 'PENDING_REVIEW'
  | 'PENDING_OTP_CONFIRMATION'
  | 'COMPLETED'
  | 'FAILED';

/** Envelope chung mọi response — ApiResponse.java. */
export type ApiResponse<T> = {
  success: boolean;
  requestId: string;
  timestamp: string;
  data: T;
  error: ApiErrorInfo | null;
};

/** ErrorInfo.java */
export type ApiErrorInfo = {
  code: string;
  message: string;
  path: string | null;
  timestamp: string;
  field: string | null;
};

/** PageResponse<T>.java — dùng chung cho mọi endpoint phân trang (history, notifications...). */
export type PageResponse<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
};

/** WalletResponse.java */
export type WalletResponse = {
  id: string;
  currency: string;
  status: string;
  balance: number;
};

/** TransactionResponse.java — trả về từ deposit/withdraw/transfer/confirm-otp. */
export type TransactionResponse = {
  id: string;
  referenceNumber: string;
  senderWalletId: string | null;
  receiverWalletId: string | null;
  transactionType: TransactionType;
  amount: number;
  status: TransactionStatus;
  createdAt: string;
  completedAt: string | null;
  failedAt: string | null;
};

/**
 * TransactionHistoryItemResponse.java — GET /transactions/history
 * ĐÃ ĐỐI CHIẾU với `TransactionService.toResponse()` (không chỉ đọc tên field DTO) — 2 điểm
 * KHÁC với ý nghĩa tên field gợi ý, đừng bịa UI dựa trên tên field:
 * - `balanceAfter` LUÔN LÀ `null` — code truyền cứng `null` ở vị trí này (dòng
 *   `entry.getWallet().getId()` xong tới `null` cho balanceAfter), không có logic tính số dư
 *   sau giao dịch nào cả. Không dùng field này để hiện "Số dư sau GD" trên UI, sẽ luôn trống.
 * - `description` thực chất được gán bằng `entry.getTransaction().getReferenceNumber()`
 *   (dạng "TXN-2026...") — KHÔNG PHẢI mô tả người dùng nhập (memo/ghi chú). Nếu hiện thẳng
 *   field này làm phụ đề dòng giao dịch, user sẽ thấy mã tham chiếu thay vì "Chuyển tiền cho
 *   X" như bản mock hiện tại — cần map lại ở FE (vd tự suy diễn text theo `type`) hoặc yêu cầu
 *   backend sửa, không nối thẳng.
 */
export type TransactionHistoryItemResponse = {
  id: string;
  transactionId: string;
  walletId: string;
  type: TransactionType;
  direction: TransactionDirection;
  amount: number;
  /** Luôn `null` ở backend hiện tại — xem ghi chú trên. */
  balanceAfter: number | null;
  status: TransactionStatus;
  /** Thực chất là reference number của transaction, không phải mô tả — xem ghi chú trên. */
  description: string;
  createdAt: string;
};

/** MomoDepositInitiateResponse.java — POST /wallets/me/deposits/momo. */
export type MomoDepositInitiateResponse = {
  payUrl: string;
  transactionId: string;
  momoOrderId: string;
};

/**
 * WalletLookupResponse.java — GET /wallets/lookup?identifier=... — tra cứu ví người nhận
 * theo email/SĐT cho màn Chuyển tiền. Chỉ trả field tối thiểu để xác nhận đúng người, KHÔNG
 * có email/SĐT/số dư (giống cách app ngân hàng che tên khi tra số tài khoản).
 */
export type WalletLookupResponse = {
  walletId: string;
  maskedName: string;
  status: string;
};

/** LedgerEntryResponse.java — GET /wallets/transactions/{id}/ledger */
export type LedgerEntryResponse = {
  id: string;
  transactionId: string;
  walletId: string | null;
  accountType: 'USER_WALLET' | 'CASH_ACCOUNT';
  debitAmount: number;
  creditAmount: number;
  createdAt: string;
};

/** UserProfileResponse.java */
export type UserProfileResponse = {
  id: string;
  email: string;
  fullName: string;
  phoneNumber: string | null;
  isVerified: boolean;
};

/** AuthResponse.java — login/register/refresh/mfa/verify. */
export type AuthResponse = {
  accessToken: string;
  refreshToken: string;
  user: UserProfileResponse;
  mfaRequired: boolean;
};

/** UserSessionResponse.java — GET /users/me/sessions */
export type UserSessionResponse = {
  id: string;
  deviceName: string | null;
  ipAddress: string | null;
  userAgent: string | null;
  lastUsedAt: string;
  expiresAt: string;
};

/** NotificationType.java — đúng 6 giá trị thật. */
export type NotificationType =
  | 'TRANSACTION_SENT'
  | 'TRANSACTION_RECEIVED'
  | 'WITHDRAWAL_APPROVED'
  | 'WITHDRAWAL_REJECTED'
  | 'FRAUD_ALERT'
  | 'SYSTEM_ANNOUNCEMENT';

/**
 * NotificationResponse.java
 * `title`/`message` do backend build sẵn BẰNG TIẾNG ANH, không có số tiền (xem
 * `NotificationService.title()/message()` — vd "Transaction TXN-xxx was received
 * successfully.", không nội suy được số tiền/tên đối tác). `metadata` chỉ có
 * `transactionId/referenceNumber/transactionType` (không có amount) khi thông báo gắn với 1
 * giao dịch, `null` khi không gắn (SYSTEM_ANNOUNCEMENT). FE tự dựng lại tiêu đề/nội dung tiếng
 * Việt từ `type` — xem `src/utils/notificationDisplay.ts` — không hiển thị thẳng
 * `title`/`message` tiếng Anh của backend.
 */
export type NotificationResponse = {
  publicId: string;
  type: NotificationType;
  title: string;
  message: string;
  metadata: { transactionId?: string; referenceNumber?: string; transactionType?: TransactionType } | null;
  read: boolean;
  readAt: string | null;
  createdAt: string;
};
