import { ReactNode } from 'react';
import { Pressable, Text, View } from 'react-native';
import { Avatar } from './Avatar';
import { Icon } from './Icon';
import { Logo } from './Logo';
import { colors } from '../theme/tokens';

type StackHeaderProps = {
  title: string;
  onBack: () => void;
  avatarName?: string;
  avatarUri?: string;
  onAvatarPress?: () => void;
  /** Icon phụ giữa nút back và avatar (vd. nút trợ giúp ở màn OTP). */
  rightAccessory?: ReactNode;
};

/**
 * Header dùng cho các màn "stack" có nút quay lại (Chuyển tiền, Xác nhận OTP, Kết quả giao
 * dịch...) — khác `AppHeader` (dùng cho 4 tab gốc: logo bên trái, không có nút back).
 */
export function StackHeader({ title, onBack, avatarName, avatarUri, onAvatarPress, rightAccessory }: StackHeaderProps) {
  return (
    <View className="h-16 px-gutter-mobile flex-row items-center justify-between bg-surface border-b border-surface-container-high">
      <View className="flex-row items-center gap-space-sm flex-1 min-w-0">
        <Pressable onPress={onBack} className="w-11 h-11 items-center justify-center -ml-2">
          <Icon name="arrow_back" size={24} color={colors.onSurface} />
        </Pressable>
        <Logo size={28} />
        <Text numberOfLines={1} className="font-headline-sm text-headline-sm text-primary font-semibold flex-1">
          {title}
        </Text>
      </View>
      <View className="flex-row items-center gap-space-sm">
        {rightAccessory}
        {(avatarName || avatarUri) && (
          <Pressable onPress={onAvatarPress} className="w-9 h-9 items-center justify-center">
            <Avatar name={avatarName} uri={avatarUri} size={32} />
          </Pressable>
        )}
      </View>
    </View>
  );
}
