import { Pressable, Text, View } from 'react-native';
import { Icon, IconName } from './Icon';
import { colors } from '../theme/tokens';
import { formatCurrency } from '../utils/formatCurrency';

type TransactionListItemProps = {
  icon: IconName;
  title: string;
  subtitle: string;
  amount: number;
  direction: 'in' | 'out';
  /** Trạng thái "chờ duyệt admin" (rút tiền lớn) khác PENDING_REVIEW gian lận thông thường — dùng
   * đúng badge chấm nhấp nháy riêng theo `code.html` gốc, không dùng chung StatusBadge. */
  pendingApproval?: boolean;
  onPress?: () => void;
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
  pendingApproval = false,
  onPress,
}: TransactionListItemProps) {
  const amountColor = direction === 'in' ? 'text-secondary' : 'text-primary';
  const sign = direction === 'in' ? '+' : '-';
  const iconContainerClassName = pendingApproval
    ? 'bg-surface-container-highest'
    : direction === 'in'
      ? 'bg-secondary-container/60'
      : 'bg-surface-container';
  const iconColor = direction === 'in' && !pendingApproval ? colors.secondary : colors.primary;

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
        {pendingApproval ? (
          <View className="flex-row items-center gap-1 mt-1 px-2 py-0.5 rounded-full bg-primary-fixed">
            <View className="w-1.5 h-1.5 rounded-full bg-primary" />
            <Text className="font-label-caption text-label-caption text-primary font-bold">CHỜ DUYỆT</Text>
          </View>
        ) : (
          <View className="mt-1 px-2 py-0.5 rounded-full bg-secondary-container/50">
            <Text className="font-label-caption text-label-caption text-secondary font-bold">THÀNH CÔNG</Text>
          </View>
        )}
      </View>
    </Pressable>
  );
}
