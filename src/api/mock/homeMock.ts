import { IconName } from '../../components/Icon';
import { TransactionHistoryItemResponse, UserProfileResponse, WalletResponse } from '../../types/api';

/**
 * Dữ liệu giả lập cho Trang chủ — khớp 1:1 với demo data trong
 * `UI Digital wallet system/Trang chủ/code.html`, dùng tạm cho tới khi nối API thật
 * (`GET /api/v1/wallets/me`, `GET /api/v1/transactions/history?size=5`).
 */

export const mockUserProfile: UserProfileResponse = {
  id: 'a1b2c3d4-0000-4000-8000-000000000001',
  email: 'an.nguyen@example.com',
  full_name: 'Nguyễn Văn An',
  phone_number: '0987654321',
  is_verified: true,
};

export const mockWallet: WalletResponse = {
  id: 'f1e2d3c4-b5a6-4000-8000-000000008829',
  currency: 'VND',
  status: 'ACTIVE',
  balance: 24_850_000,
};

export type MockTransactionItem = TransactionHistoryItemResponse & {
  /** Chỉ dùng cho mock/hiển thị — API thật không có field icon, Phase 2 cần tự suy ra icon theo type. */
  counterpartyName: string;
  icon: IconName;
};

export const mockRecentTransactions: MockTransactionItem[] = [
  {
    id: 'tx-1',
    transactionId: 'tx-1',
    walletId: mockWallet.id,
    type: 'DEPOSIT',
    direction: 'CREDIT',
    amount: 1_500_000,
    balanceAfter: 24_850_000,
    status: 'COMPLETED',
    description: 'Nạp ví / Chuyển khoản',
    createdAt: '2026-09-11T14:32:00+07:00',
    counterpartyName: 'Trần Thị Mai',
    icon: 'arrow_downward',
  },
  {
    id: 'tx-2',
    transactionId: 'tx-2',
    walletId: mockWallet.id,
    type: 'TRANSFER',
    direction: 'DEBIT',
    amount: 340_000,
    balanceAfter: 23_350_000,
    status: 'COMPLETED',
    description: 'Thanh toán dịch vụ',
    createdAt: '2026-09-11T10:15:00+07:00',
    counterpartyName: 'Hóa đơn điện nước thoại',
    icon: 'receipt_long',
  },
  {
    id: 'tx-3',
    transactionId: 'tx-3',
    walletId: mockWallet.id,
    type: 'TRANSFER',
    direction: 'DEBIT',
    amount: 185_000,
    balanceAfter: 23_690_000,
    status: 'COMPLETED',
    description: 'Tiền ăn trưa',
    createdAt: '2026-09-10T12:45:00+07:00',
    counterpartyName: 'Lê Hoàng Nam',
    icon: 'arrow_upward',
  },
  {
    id: 'tx-4',
    transactionId: 'tx-4',
    walletId: mockWallet.id,
    type: 'WITHDRAW',
    direction: 'DEBIT',
    amount: 5_000_000,
    balanceAfter: 23_875_000,
    status: 'COMPLETED',
    description: 'Rút tiền về thẻ',
    createdAt: '2026-09-08T15:00:00+07:00',
    counterpartyName: 'Ngân hàng Vietcombank (*9012)',
    icon: 'account_balance',
  },
];
