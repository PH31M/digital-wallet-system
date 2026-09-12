import { Pressable, Text, View } from 'react-native';
import { Icon } from './Icon';
import { Logo } from './Logo';
import { colors } from '../theme/tokens';

type AuthHeaderProps = {
  onBack?: () => void;
  onHelpPress?: () => void;
};

/**
 * Header riêng cho các màn auth (Đăng nhập/Đăng ký) — logo+PMPay canh giữa, back bên trái
 * (ẩn khi không có gì để quay lại), help bên phải. Khác `StackHeader` (title canh trái, có
 * avatar) và `AppHeader` (4 tab gốc).
 */
export function AuthHeader({ onBack, onHelpPress }: AuthHeaderProps) {
  return (
    <View className="h-16 px-gutter-mobile flex-row items-center justify-between bg-surface border-b border-surface-container-high">
      <Pressable
        onPress={onBack}
        className="w-11 h-11 items-center justify-center -ml-2"
        style={{ opacity: onBack ? 1 : 0 }}
      >
        <Icon name="arrow_back" size={24} color={colors.onSurface} />
      </Pressable>
      <View className="flex-row items-center gap-space-xs">
        <Logo size={28} />
        <Text className="font-headline-sm text-headline-sm text-primary font-bold">PMPay</Text>
      </View>
      <Pressable onPress={onHelpPress} className="w-11 h-11 items-center justify-center -mr-2">
        <Icon name="help_outline" size={22} color={colors.onSurfaceVariant} />
      </Pressable>
    </View>
  );
}
