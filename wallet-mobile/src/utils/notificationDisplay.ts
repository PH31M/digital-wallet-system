import { IconName } from '../components/Icon';
import { NotificationResponse, NotificationType } from '../types/api';

export type NotificationCategory = 'balance' | 'security' | 'promo';

type NotificationStyle = {
  category: NotificationCategory;
  icon: IconName;
  iconBg: string;
  iconColor: string;
  titleColor: string;
  /** Tiêu đề tiếng Việt — KHÔNG dùng `NotificationResponse.title` (tiếng Anh, xem types/api.ts). */
  title: string;
};

const STYLE_BY_TYPE: Record<NotificationType, NotificationStyle> = {
  TRANSACTION_SENT: {
    category: 'balance',
    icon: 'arrow_circle_up',
    iconBg: '#0f2c59',
    iconColor: '#ffffff',
    titleColor: '#00173b',
    title: 'Chuyển tiền thành công',
  },
  TRANSACTION_RECEIVED: {
    category: 'balance',
    icon: 'arrow_circle_down',
    iconBg: '#7ef6be',
    iconColor: '#00714c',
    titleColor: '#00173b',
    title: 'Nhận tiền thành công',
  },
  WITHDRAWAL_APPROVED: {
    category: 'balance',
    icon: 'check_circle',
    iconBg: '#7ef6be',
    iconColor: '#00714c',
    titleColor: '#00173b',
    title: 'Yêu cầu rút tiền đã được duyệt',
  },
  WITHDRAWAL_REJECTED: {
    category: 'balance',
    icon: 'cancel',
    iconBg: '#ffdad6',
    iconColor: '#ba1a1a',
    titleColor: '#ba1a1a',
    title: 'Yêu cầu rút tiền bị từ chối',
  },
  FRAUD_ALERT: {
    category: 'security',
    icon: 'shield_lock',
    iconBg: '#ffdad6',
    iconColor: '#ba1a1a',
    titleColor: '#ba1a1a',
    title: 'Cảnh báo bảo mật giao dịch',
  },
  SYSTEM_ANNOUNCEMENT: {
    category: 'promo',
    icon: 'campaign',
    iconBg: '#d8e2ff',
    iconColor: '#00173b',
    titleColor: '#00173b',
    title: 'Thông báo hệ thống',
  },
};

export function getNotificationStyle(type: NotificationType): NotificationStyle {
  return STYLE_BY_TYPE[type] ?? STYLE_BY_TYPE.SYSTEM_ANNOUNCEMENT;
}

/**
 * Backend không có message tiếng Việt lẫn số tiền (`metadata` không chứa amount) — tự dựng
 * câu mô tả ngắn từ `type` + mã tham chiếu (nếu có), không bịa số tiền/tên đối tác không có
 * trong dữ liệu thật.
 */
export function getNotificationBody(notification: NotificationResponse): string {
  const ref = notification.metadata?.referenceNumber;
  switch (notification.type) {
    case 'TRANSACTION_SENT':
      return ref ? `Giao dịch chuyển tiền ${ref} đã được gửi thành công.` : 'Giao dịch chuyển tiền đã được gửi thành công.';
    case 'TRANSACTION_RECEIVED':
      return ref ? `Bạn vừa nhận được 1 khoản tiền — mã GD ${ref}.` : 'Bạn vừa nhận được 1 khoản tiền.';
    case 'WITHDRAWAL_APPROVED':
      return ref ? `Yêu cầu rút tiền ${ref} đã được duyệt và xử lý.` : 'Yêu cầu rút tiền của bạn đã được duyệt.';
    case 'WITHDRAWAL_REJECTED':
      return ref ? `Yêu cầu rút tiền ${ref} bị từ chối.` : 'Yêu cầu rút tiền của bạn bị từ chối.';
    case 'FRAUD_ALERT':
      return ref
        ? `Giao dịch ${ref} đang được kiểm tra an toàn bổ sung trước khi xử lý.`
        : 'Một giao dịch của bạn đang được kiểm tra an toàn bổ sung.';
    case 'SYSTEM_ANNOUNCEMENT':
    default:
      return 'Bạn có thông báo mới từ hệ thống ví PMPay.';
  }
}
