import { ReactNode } from 'react';
import { Pressable, Text, View } from 'react-native';
import { Icon, IconName } from './Icon';
import { colors } from '../theme/tokens';

type SettingsMenuRowProps = {
  icon: IconName;
  title: string;
  subtitle: string;
  onPress?: () => void;
  /** Nội dung bên phải — mặc định mũi tên chevron; truyền vào để thay bằng switch/badge. */
  trailing?: ReactNode;
};

/** Dòng menu cài đặt dùng chung cho Hồ sơ cá nhân và các màn cài đặt sau này. */
export function SettingsMenuRow({ icon, title, subtitle, onPress, trailing }: SettingsMenuRowProps) {
  return (
    <Pressable onPress={onPress} className="flex-row items-center gap-space-md p-space-md">
      <View className="w-10 h-10 rounded-md bg-surface-container-high items-center justify-center shrink-0">
        <Icon name={icon} size={22} color={colors.primary} />
      </View>
      <View className="flex-1 min-w-0">
        <Text numberOfLines={1} className="font-headline-sm text-headline-sm text-on-surface font-semibold">
          {title}
        </Text>
        <Text numberOfLines={1} className="font-body-sm text-body-sm text-on-surface-variant">
          {subtitle}
        </Text>
      </View>
      {trailing ?? <Icon name="chevron_right" size={20} color={colors.outline} />}
    </Pressable>
  );
}
