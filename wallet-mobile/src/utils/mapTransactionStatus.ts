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
