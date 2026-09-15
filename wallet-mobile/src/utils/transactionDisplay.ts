import { IconName } from '../components/Icon';
import { TransactionDirection, TransactionType } from '../types/api';

/**
 * `TransactionHistoryItemResponse` thật KHÔNG có tên đối tác giao dịch (không phải lỗi FE bỏ
 * sót — DTO chỉ có `walletId` của chính ví mình, không có thông tin ví/người còn lại). Mock cũ
 * dùng tên người ("TRẦN THỊ HƯƠNG"...) — không thể tái tạo khi nối API thật, nên tiêu đề dòng
 * giao dịch đổi sang label theo loại giao dịch thay vì tên người.
 */
export function getTransactionTypeLabel(type: TransactionType, direction: TransactionDirection): string {
  switch (type) {
    case 'DEPOSIT':
      return 'Nạp tiền vào ví';
    case 'WITHDRAW':
      return 'Rút tiền từ ví';
    case 'TRANSFER':
      return direction === 'CREDIT' ? 'Nhận tiền chuyển khoản' : 'Chuyển tiền';
    case 'REVERSAL':
      return 'Hoàn tiền giao dịch';
    default:
      return 'Giao dịch';
  }
}

export function getTransactionIcon(type: TransactionType, direction: TransactionDirection): IconName {
  switch (type) {
    case 'DEPOSIT':
      return 'arrow_downward';
    case 'WITHDRAW':
      return 'account_balance';
    case 'TRANSFER':
      return direction === 'CREDIT' ? 'arrow_downward' : 'arrow_upward';
    case 'REVERSAL':
      return 'replay';
    default:
      return 'sync_alt';
  }
}
