import { Pressable, Text, View } from 'react-native';
import { Avatar } from './Avatar';
import { Logo } from './Logo';

type AppHeaderProps = {
  title: string;
  avatarName?: string;
  avatarUri?: string;
  onAvatarPress?: () => void;
};

/**
 * Header dùng chung cho mọi screen (logo + tên app bên trái, tiêu đề trang + avatar bên
 * phải) — port từ `<header>` lặp lại giống hệt trong code.html của tất cả 18 màn.
 * Bỏ hiệu ứng backdrop-blur của bản web vì RN cần thêm dependency (expo-blur) mới làm được;
 * tạm dùng nền đặc `bg-surface` cho tới khi cần polish thêm.
 */
export function AppHeader({ title, avatarName, avatarUri, onAvatarPress }: AppHeaderProps) {
  return (
    <View className="h-16 px-gutter-mobile flex-row items-center justify-between bg-surface border-b border-surface-container-high">
      <View className="flex-row items-center gap-space-sm">
        <Logo size={32} />
        <Text className="font-headline-sm text-headline-sm text-primary font-bold">PMPay</Text>
      </View>
      <View className="flex-row items-center gap-space-md">
        <Text className="font-label-md text-label-md text-on-surface-variant font-medium">{title}</Text>
        <Pressable onPress={onAvatarPress} className="w-11 h-11 items-center justify-center rounded-full">
          <Avatar name={avatarName} uri={avatarUri} size={32} />
        </Pressable>
      </View>
    </View>
  );
}
