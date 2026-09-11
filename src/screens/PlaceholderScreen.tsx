import { ReactNode } from 'react';
import { View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { AppHeader } from '../components/AppHeader';
import { EmptyState } from '../components/EmptyState';
import { IconName } from '../components/Icon';

type PlaceholderScreenProps = {
  title: string;
  icon: IconName;
  description: string;
  footer?: ReactNode;
};

/** Dùng tạm cho các tab chưa có brief/screen được duyệt để build thật (Lịch sử, Thông báo, Hồ sơ). */
export function PlaceholderScreen({ title, icon, description, footer }: PlaceholderScreenProps) {
  return (
    <SafeAreaView className="flex-1 bg-background" edges={['top']}>
      <AppHeader title={title} avatarName="Nguyễn Văn An" />
      <View className="flex-1 items-center justify-center">
        <EmptyState icon={icon} title={`${title} sắp ra mắt`} description={description} />
      </View>
      {footer && <View className="px-gutter-mobile pb-space-lg">{footer}</View>}
    </SafeAreaView>
  );
}
