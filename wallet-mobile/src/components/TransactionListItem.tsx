import { Pressable, Text, View } from 'react-native';
import { Icon, IconName } from './Icon';
import { StatusBadgeStatus } from './StatusBadge';
import { colors } from '../theme/tokens';
import { formatCurrency } from '../utils/formatCurrency';
import { TransactionStatus } from '../types/api';
import { mapTransactionStatusToBadge, mapTransactionStatusToLabel } from '../utils/mapTransactionStatus';

type TransactionListItemProps = {
  icon: IconName;
  title: string;
  subtitle: string;
  amount: number;
  direction: 'in' | 'out';
  /** Trạng thái thật từ TransactionResponse/TransactionHistoryItemResponse — đủ 6 giá trị,
   * không còn rút gọn về 2 trạng thái (pendingApproval) như bản cũ. */
  status: TransactionStatus;
  onPress?: () => void;
};

// Cùng 4 tông với StatusBadge (success/warning/danger/default) nhưng vẽ dạng "dot + pill" riêng
// theo đúng code.html gốc của màn Lịch sử, không dùng chung component StatusBadge.
const pillClassNameByTone: Record<StatusBadgeStatus, string> = {
  success: 'bg-secondary-container/50',
  warning: 'bg-primary-fixed',
  danger: 'bg-error-container/60',
  default: 'bg-surface-container-highest',
};

const dotClassNameByTone: Record<StatusBadgeStatus, string> = {
  success: 'bg-secondary',
  warning: 'bg-primary',
  danger: 'bg-error',
  default: 'bg-outline',
};

const textClassNameByTone: Record<StatusBadgeStatus, string> = {
  success: 'text-secondary',
  warning: 'text-primary',
  danger: 'text-on-error-container',
  default: 'text-on-surface-variant',
};

/**
 * Dòng giao dịch dạng "dày đặc" dùng riêng cho danh sách nhóm theo ngày (Lịch sử giao dịch) —
 * khác `TransactionRow` (mỗi dòng là 1 card nổi riêng, dùng ở Trang chủ). Ở đây nhiều dòng nằm
 * chung 1 card, ngăn cách bằng đường kẻ mảnh — đúng bố cục trong code.html gốc của màn này.
 */
export function TransactionListItem({
  icon,
  title,
  subtitle,
  amount,
  direction,
  status,
  onPress,
}: TransactionListItemProps) {
  const tone = mapTransactionStatusToBadge(status);
  const label = mapTransactionStatusToLabel(status).toUpperCase();
  const amountColor = tone === 'danger' ? 'text-danger' : direction === 'in' ? 'text-secondary' : 'text-primary';
  const sign = direction === 'in' ? '+' : '-';
  const iconContainerClassName =
    tone !== 'success' ? 'bg-surface-container-highest' : direction === 'in' ? 'bg-secondary-container/60' : 'bg-surface-container';
  const iconColor = direction === 'in' && tone === 'success' ? colors.secondary : colors.primary;

  return (
    <Pressable onPress={onPress} className="flex-row items-center justify-between p-3.5">
      <View className="flex-row items-center gap-space-sm flex-1 min-w-0 pr-space-xs">
        <View className={`w-10 h-10 rounded-md items-center justify-center ${iconContainerClassName}`}>
          <Icon name={icon} size={20} color={iconColor} />
        </View>
        <View className="shrink">
          <Text numberOfLines={1} className="font-headline-sm text-headline-sm text-primary font-semibold">
            {title}
          </Text>
          <Text numberOfLines={1} className="font-body-sm text-body-sm text-on-surface-variant">
            {subtitle}
          </Text>
        </View>
      </View>
      <View className="items-end shrink-0 pl-space-xs">
        <Text className={`font-numeric-ledger text-numeric-ledger font-bold ${amountColor}`}>
          {sign}
          {formatCurrency(amount)}
        </Text>
        <View className={`flex-row items-center gap-1 mt-1 px-2 py-0.5 rounded-full ${pillClassNameByTone[tone]}`}>
          <View className={`w-1.5 h-1.5 rounded-full ${dotClassNameByTone[tone]}`} />
          <Text className={`font-label-caption text-label-caption font-bold ${textClassNameByTone[tone]}`}>{label}</Text>
        </View>
      </View>
    </Pressable>
  );
}
