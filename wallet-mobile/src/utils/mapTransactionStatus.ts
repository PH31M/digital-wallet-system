import { StatusBadgeStatus } from '../components/StatusBadge';
import { TransactionStatus } from '../types/api';

export function mapTransactionStatusToBadge(status: TransactionStatus): StatusBadgeStatus {
  switch (status) {
    case 'COMPLETED':
      return 'success';
    case 'FAILED':
      return 'danger';
    case 'PENDING':
    case 'PROCESSING':
    case 'PENDING_REVIEW':
    case 'PENDING_OTP_CONFIRMATION':
      return 'warning';
    default:
      return 'default';
  }
}

/**
 * 4 trạng thái "warning" (PENDING/PROCESSING/PENDING_REVIEW/PENDING_OTP_CONFIRMATION) dùng
 * chung 1 tông màu ở StatusBadge nhưng ý nghĩa khác nhau với người dùng — label riêng để
 * không hiện "Đang chờ" mơ hồ cho cả rút tiền chờ admin duyệt lẫn chờ nhập OTP.
 */
export function mapTransactionStatusToLabel(status: TransactionStatus): string {
  switch (status) {
    case 'COMPLETED':
      return 'Thành công';
    case 'FAILED':
      return 'Thất bại';
    case 'PENDING_REVIEW':
      return 'Chờ duyệt';
    case 'PENDING_OTP_CONFIRMATION':
      return 'Chờ xác thực OTP';
    case 'PENDING':
    case 'PROCESSING':
      return 'Đang xử lý';
    default:
      return '';
  }
}
