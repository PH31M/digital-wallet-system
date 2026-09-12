import { Pressable, Text, View } from 'react-native';
import { Avatar } from './Avatar';
import { Icon } from './Icon';
import { colors } from '../theme/tokens';

type DetailHeaderProps = {
  title: string;
  onBack: () => void;
  avatarName?: string;
  avatarUri?: string;
  onAvatarPress?: () => void;
};

/**
 * Header cho các trang chi tiết cài đặt (Chỉnh sửa hồ sơ, Thiết bị đăng nhập...) — khác
 * `StackHeader` ở chỗ không có logo, và tiêu đề canh giữa tuyệt đối thay vì căn trái.
 */
export function DetailHeader({ title, onBack, avatarName, avatarUri, onAvatarPress }: DetailHeaderProps) {
  return (
    <View
      className="h-16 px-gutter-mobile flex-row items-center justify-between bg-surface border-b border-surface-container-high"
      style={{ position: 'relative' }}
    >
      <Pressable onPress={onBack} className="w-11 h-11 items-center justify-center -ml-2">
        <Icon name="chevron_left" size={26} color={colors.onSurface} />
      </Pressable>
      <Text
        numberOfLines={1}
        className="text-center font-headline-md text-headline-md text-on-surface"
        style={{ position: 'absolute', left: 64, right: 64 }}
      >
        {title}
      </Text>
      <Pressable onPress={onAvatarPress} className="w-9 h-9 items-center justify-center">
        <Avatar name={avatarName} uri={avatarUri} size={32} />
      </Pressable>
    </View>
  );
}
