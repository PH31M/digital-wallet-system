import { Text, View } from 'react-native';
import { Icon, IconName } from './Icon';

export type ToastType = 'success' | 'danger' | 'info';

type ToastProps = {
  message: string;
  type: ToastType;
};

const containerClassNameByType: Record<ToastType, string> = {
  success: 'bg-secondary',
  danger: 'bg-error',
  info: 'bg-info',
};

const iconByType: Record<ToastType, IconName> = {
  success: 'check_circle',
  danger: 'cancel',
  info: 'warning_circle',
};

export function Toast({ message, type }: ToastProps) {
  return (
    <View className={`flex-row items-center gap-space-sm px-space-md py-space-md rounded-md ${containerClassNameByType[type]}`}>
      <Icon name={iconByType[type]} size={20} color="#ffffff" />
      <Text className="flex-1 font-label-md text-label-md text-white">{message}</Text>
    </View>
  );
}
