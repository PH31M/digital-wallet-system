import { IconName } from '../../components/Icon';
import { TransactionHistoryItemResponse } from '../../types/api';

/**
 * Dữ liệu giả lập cho Lịch sử giao dịch — khớp 1:1 nội dung/số tiền trong
 * `UI Digital wallet system/Lịch sử giao dịch/code.html`. Ngày tạo được dịch sang khung
 * "hôm nay/hôm qua" tương đối so với ngày chạy thật (thay vì giữ cứng 10/09/2026 trong ảnh
 * gốc) để label "Hôm nay"/"Hôm qua" luôn đúng khi demo, không lệ thuộc thời điểm xem.
 */

export type HistoryFilterCategory = 'all' | 'inflow' | 'outflow' | 'transfer' | 'topup' | 'pending';

export type MockHistoryItem = TransactionHistoryItemResponse & {
  title: string;
  subtitle: string;
  icon: IconName;
  categories: Exclude<HistoryFilterCategory, 'all'>[];
};

const today = new Date();
const yesterday = new Date(today);
yesterday.setDate(today.getDate() - 1);
const threeDaysAgo = new Date(today);
threeDaysAgo.setDate(today.getDate() - 3);

function atTime(date: Date, hours: number, minutes: number): string {
  const d = new Date(date);
  d.setHours(hours, minutes, 0, 0);
  return d.toISOString();
}

export const mockHistoryTransactions: MockHistoryItem[] = [
  {
    id: 'h-1',
    transactionId: 'h-1',
    walletId: 'wallet-1',
    type: 'TRANSFER',
    direction: 'DEBIT',
    amount: 1_200_000,
    balanceAfter: 0,
    status: 'COMPLETED',
    description: 'Chuyen tien thanh toan',
    createdAt: atTime(today, 14, 32),
    title: 'TRẦN THỊ HƯƠNG',
    subtitle: 'Chuyen tien thanh toan',
    icon: 'arrow_upward',
    categories: ['outflow', 'transfer'],
  },
  {
    id: 'h-2',
    transactionId: 'h-2',
    walletId: 'wallet-1',
    type: 'DEPOSIT',
    direction: 'CREDIT',
    amount: 5_000_000,
    balanceAfter: 0,
    status: 'COMPLETED',
    description: 'Vietcombank (*8839)',
    createdAt: atTime(today, 11, 20),
    title: 'Nạp tiền từ VCB',
    subtitle: 'Vietcombank (*8839)',
    icon: 'arrow_downward',
    categories: ['inflow', 'topup'],
  },
  {
    id: 'h-3',
    transactionId: 'h-3',
    walletId: 'wallet-1',
    type: 'TRANSFER',
    direction: 'DEBIT',
    amount: 620_000,
    balanceAfter: 0,
    status: 'COMPLETED',
    description: 'Mã KH: PE0200384',
    createdAt: atTime(today, 9, 15),
    title: 'Hóa đơn Điện lực EVN',
    subtitle: 'Mã KH: PE0200384',
    icon: 'bolt',
    categories: ['outflow'],
  },
  {
    id: 'h-4',
    transactionId: 'h-4',
    walletId: 'wallet-1',
    type: 'WITHDRAW',
    direction: 'DEBIT',
    amount: 2_000_000,
    balanceAfter: 0,
    status: 'COMPLETED',
    description: 'Thẻ ghi nợ (*4412)',
    createdAt: atTime(yesterday, 16, 45),
    title: 'Rút tiền về Thẻ BIDV',
    subtitle: 'Thẻ ghi nợ (*4412)',
    icon: 'credit_card',
    categories: ['outflow', 'topup'],
  },
  {
    id: 'h-5',
    transactionId: 'h-5',
    walletId: 'wallet-1',
    type: 'TRANSFER',
    direction: 'CREDIT',
    amount: 250_000,
    balanceAfter: 0,
    status: 'COMPLETED',
    description: 'Đơn hàng #8839218',
    createdAt: atTime(yesterday, 10, 0),
    title: 'Hoàn tiền ShopeePay',
    subtitle: 'Đơn hàng #8839218',
    icon: 'currency_exchange',
    categories: ['inflow'],
  },
  {
    id: 'h-6',
    transactionId: 'h-6',
    walletId: 'wallet-1',
    type: 'WITHDRAW',
    direction: 'DEBIT',
    amount: 15_000_000,
    balanceAfter: 0,
    status: 'PENDING_REVIEW',
    description: 'Đang xác minh bảo mật',
    createdAt: atTime(threeDaysAgo, 15, 30),
    title: 'Giao dịch rút tiền lớn',
    subtitle: 'Đang xác minh bảo mật',
    icon: 'security_update_warning',
    categories: ['outflow', 'topup', 'pending'],
  },
];
