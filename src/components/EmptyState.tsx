import { Text, View } from 'react-native';
import { Icon, IconName } from './Icon';
import { colors } from '../theme/tokens';

type EmptyStateProps = {
  icon: IconName;
  title: string;
  description?: string;
};

export function EmptyState({ icon, title, description }: EmptyStateProps) {
  return (
    <View className="items-center justify-center px-space-xl py-space-2xl">
      <Icon name={icon} size={56} color={colors.outline} />
      <Text className="font-headline-sm text-headline-sm text-on-surface mt-space-md text-center">{title}</Text>
      {description && (
        <Text className="font-body-sm text-body-sm text-on-surface-variant mt-space-xs text-center">{description}</Text>
      )}
    </View>
  );
}
