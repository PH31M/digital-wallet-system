import { Pressable, Text, View } from 'react-native';
import { Icon, IconName } from './Icon';
import { StatusBadge, StatusBadgeStatus } from './StatusBadge';
import { colors } from '../theme/tokens';
import { formatCurrency } from '../utils/formatCurrency';

type TransactionRowProps = {
  icon: IconName;
  title: string;
  subtitle: string;
  amount: number;
  direction: 'in' | 'out';
  status?: StatusBadgeStatus;
  onPress?: () => void;
};

export function TransactionRow({ icon, title, subtitle, amount, direction, status = 'success', onPress }: TransactionRowProps) {
  const isFailed = status === 'danger';
  const amountClassName = isFailed ? 'text-danger' : direction === 'in' ? 'text-secondary' : 'text-on-surface';
  const sign = direction === 'in' ? '+' : '-';

  return (
    <Pressable
      onPress={onPress}
      className="flex-row items-center justify-between p-space-md rounded-lg bg-surface-container-lowest min-h-[68px]"
    >
      <View className="flex-row items-center flex-1 mr-space-sm">
        <View
          className={`w-11 h-11 rounded-full items-center justify-center mr-space-md ${
            direction === 'in' ? 'bg-secondary-container' : 'bg-surface-container-high'
          }`}
        >
          <Icon name={icon} size={22} color={direction === 'in' ? colors.onSecondaryContainer : colors.primary} />
        </View>
        <View className="flex-1">
          <Text numberOfLines={1} className="font-headline-sm text-headline-sm text-on-surface">
            {title}
          </Text>
          <Text numberOfLines={1} className="font-body-sm text-body-sm text-on-surface-variant">
            {subtitle}
          </Text>
        </View>
      </View>
      <View className="items-end">
        <Text className={`font-numeric-ledger text-numeric-ledger ${amountClassName}`}>
          {sign}
          {formatCurrency(amount)}
        </Text>
        <View className="mt-space-xs">
          <StatusBadge status={status} />
        </View>
      </View>
    </Pressable>
  );
}
