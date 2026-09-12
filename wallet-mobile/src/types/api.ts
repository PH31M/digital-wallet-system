/**
 * Kiểu dữ liệu khớp với DTO thật bên backend (wallet-core), để khi nối API thật ở phase sau
 * chỉ cần đổi nguồn dữ liệu (mock -> react-query), không cần đổi lại shape.
 * Đối chiếu: WalletResponse.java, TransactionHistoryItemResponse.java, UserProfileResponse.java.
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

export type WalletResponse = {
  id: string;
  currency: string;
  status: string;
  balance: number;
};

export type TransactionHistoryItemResponse = {
  id: string;
  transactionId: string;
  walletId: string;
  type: TransactionType;
  direction: TransactionDirection;
  amount: number;
  balanceAfter: number;
  status: TransactionStatus;
  description: string;
  createdAt: string;
};

export type UserProfileResponse = {
  id: string;
  email: string;
  full_name: string;
  phone_number: string | null;
  is_verified: boolean;
};
