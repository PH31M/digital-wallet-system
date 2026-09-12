import { IconName } from '../../components/Icon';

const today = new Date();
const yesterday = new Date(today);
yesterday.setDate(today.getDate() - 1);
const lastWeek = new Date(today);
lastWeek.setDate(today.getDate() - 4);

function atTime(date: Date, hours: number, minutes: number): string {
  const d = new Date(date);
  d.setHours(hours, minutes, 0, 0);
  return d.toISOString();
}

export type NotificationCategory = 'balance' | 'security' | 'promo';

export type NotificationFooter =
  | { kind: 'text'; label: string; icon?: IconName; color: string }
  | { kind: 'button'; label: string; icon: IconName };

export type BodySegment = { text: string; emphasis?: boolean };

export type MockNotification = {
  id: string;
  category: NotificationCategory;
  icon: IconName;
  iconBg: string;
  iconColor: string;
  title: string;
  titleColor: string;
  amount?: { value: number; direction: 'in' | 'out' };
  body: BodySegment[];
  createdAt: string;
  footer: NotificationFooter;
  isRead: boolean;
};

/**
 * Khớp 1:1 nội dung `UI Digital wallet system/Thông báo/code.html`. Lưu ý: bản gốc dùng màu
 * đỏ (text-error) cho CẢ tiền ra bình thường ở màn này (khác quy tắc "không dùng danger cho
 * tiền ra" áp dụng ở Trang chủ/Lịch sử) — giữ nguyên đúng như ảnh đã duyệt cho màn Thông báo,
 * không tự sửa lại cho khớp quy tắc chung.
 */
export const mockNotifications: MockNotification[] = [
  {
    id: 'n-1',
    category: 'balance',
    icon: 'arrow_circle_up',
    iconBg: '#0f2c59',
    iconColor: '#ffffff',
    title: 'Chuyển tiền thành công',
    titleColor: '#00173b',
    amount: { value: 1_200_000, direction: 'out' },
    body: [
      { text: 'Bạn đã chuyển thành công ' },
      { text: '1.200.000 đ', emphasis: true },
      { text: ' đến ' },
      { text: 'TRẦN THỊ HƯƠNG', emphasis: true },
      { text: ' (AP-883921). Số dư mới: 23.650.000 đ.' },
    ],
    createdAt: atTime(today, 14, 32),
    footer: { kind: 'text', label: 'Phí 0đ', icon: 'check_circle', color: '#006c49' },
    isRead: false,
  },
  {
    id: 'n-2',
    category: 'security',
    icon: 'shield_lock',
    iconBg: '#ffdad6',
    iconColor: '#ba1a1a',
    title: 'Đăng nhập thiết bị mới',
    titleColor: '#ba1a1a',
    body: [
      { text: 'Tài khoản đăng nhập trên ' },
      { text: 'iPhone 15 Pro Max', emphasis: true },
      { text: ' tại Hà Nội (IP: 118.70.12.*). Nếu không phải bạn, vui lòng bảo vệ tài khoản ngay.' },
    ],
    createdAt: atTime(today, 9, 15),
    footer: { kind: 'button', label: 'Kiểm tra phiên', icon: 'security' },
    isRead: false,
  },
  {
    id: 'n-3',
    category: 'balance',
    icon: 'arrow_circle_down',
    iconBg: '#7ef6be',
    iconColor: '#00714c',
    title: 'Nạp tiền thành công',
    titleColor: '#00173b',
    amount: { value: 5_000_000, direction: 'in' },
    body: [
      { text: 'Nguồn tiền liên kết từ ngân hàng ' },
      { text: 'Vietcombank (*8839)', emphasis: true },
      { text: '. Giao dịch trực tuyến miễn phí qua cổng Napas 247.' },
    ],
    createdAt: atTime(yesterday, 11, 20),
    footer: { kind: 'text', label: 'Hoàn tất', color: '#006c49' },
    isRead: true,
  },
  {
    id: 'n-4',
    category: 'balance',
    icon: 'receipt_long',
    iconBg: '#e2e7ff',
    iconColor: '#00173b',
    title: 'Hóa đơn điện lực EVN',
    titleColor: '#00173b',
    amount: { value: 620_000, direction: 'out' },
    body: [
      { text: 'Thanh toán tự động hóa đơn điện kỳ T08/2026 thành công cho Mã người dùng ' },
      { text: 'PE0200384', emphasis: true },
      { text: '.' },
    ],
    createdAt: atTime(yesterday, 8, 30),
    footer: { kind: 'text', label: 'Hóa đơn điện tử #8391', color: '#747780' },
    isRead: true,
  },
  {
    id: 'n-5',
    category: 'promo',
    icon: 'campaign',
    iconBg: '#d8e2ff',
    iconColor: '#00173b',
    title: 'Bảo trì nâng cấp hạ tầng 2FA',
    titleColor: '#00173b',
    body: [
      {
        text: 'Hệ thống ngân hàng thực hiện nâng cấp định kỳ bảo mật từ 02:00 - 04:00 ngày 15/09/2026. Quẹt thẻ vật lý và chuyển khoản nội bộ vẫn khả dụng.',
      },
    ],
    createdAt: atTime(lastWeek, 10, 0),
    footer: { kind: 'text', label: 'Thông tin hệ thống', color: '#00173b' },
    isRead: true,
  },
];
