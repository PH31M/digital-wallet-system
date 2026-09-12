import { Text, View } from 'react-native';

export type StatusBadgeStatus = 'success' | 'warning' | 'danger' | 'default';

type StatusBadgeProps = {
  status: StatusBadgeStatus;
  label?: string;
};

const defaultLabelByStatus: Record<StatusBadgeStatus, string> = {
  success: 'Thành công',
  warning: 'Đang chờ',
  danger: 'Thất bại',
  default: '',
};

// success/danger dùng đúng cặp container/on-container mà Stitch đã bake vào code trang chủ
// (vd. badge "Hoàn tất" dùng bg-secondary-container). `warning` không có trong bộ M3 token
// nên fallback về kiểu nền nhạt 10% opacity theo mô tả ui-frontend-design-spec.md.
const classNameByStatus: Record<StatusBadgeStatus, string> = {
  success: 'bg-secondary-container',
  warning: 'bg-warning/10',
  danger: 'bg-error-container',
  default: 'bg-surface-container-high',
};

const textClassNameByStatus: Record<StatusBadgeStatus, string> = {
  success: 'text-on-secondary-container',
  warning: 'text-warning',
  danger: 'text-on-error-container',
  default: 'text-on-surface-variant',
};

export function StatusBadge({ status, label }: StatusBadgeProps) {
  const text = label ?? defaultLabelByStatus[status];

  return (
    <View className={`h-6 px-[10px] rounded-full items-center justify-center self-start ${classNameByStatus[status]}`}>
      <Text className={`font-label-caption text-label-caption uppercase ${textClassNameByStatus[status]}`}>{text}</Text>
    </View>
  );
}
