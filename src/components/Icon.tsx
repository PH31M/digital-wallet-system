import MaterialCommunityIcons from '@expo/vector-icons/MaterialCommunityIcons';
import { colors } from '../theme/tokens';

/**
 * Bộ icon thực tế: Google Material Symbols (đã chọn) không render được qua ligature trên
 * RN Text một cách ổn định đa nền tảng, nên map 1:1 sang MaterialCommunityIcons (cũng là
 * icon font của Google, cùng hệ outline) bằng glyph tương đương gần nhất. Key của map dùng
 * đúng tên icon Material Symbols trong code HTML export của từng màn hình (design-export) để
 * tra cứu nhanh khi port các màn hình thật ở Phase 2.
 */
export const iconMap = {
  visibility: 'eye-outline',
  visibility_off: 'eye-off-outline',
  content_copy: 'content-copy',
  notifications: 'bell-outline',
  verified_user: 'shield-check',
  shield_lock: 'shield-lock-outline',
  lock: 'lock-outline',
  sync_alt: 'swap-horizontal',
  add_circle: 'plus-circle-outline',
  arrow_circle_down: 'arrow-down-circle-outline',
  history: 'history',
  arrow_downward: 'arrow-down',
  arrow_upward: 'arrow-up',
  receipt_long: 'receipt-text-outline',
  account_balance: 'bank-outline',
  account_circle: 'account-circle-outline',
  check_circle: 'check-circle-outline',
  cancel: 'close-circle-outline',
  schedule: 'clock-outline',
  magnifying_glass: 'magnify',
  filter: 'filter-outline',
  device_mobile: 'cellphone',
  sign_out: 'logout',
  chevron_right: 'chevron-right',
  close: 'close',
  warning_circle: 'alert-circle-outline',
  account_balance_wallet: 'wallet-outline',
  verified: 'check-decagram-outline',
} as const;

export type IconName = keyof typeof iconMap;

type IconProps = {
  name: IconName;
  size?: number;
  color?: string;
};

export function Icon({ name, size = 24, color = colors.onSurface }: IconProps) {
  return <MaterialCommunityIcons name={iconMap[name]} size={size} color={color} />;
}
