import { Modal, Text, View } from 'react-native';
import { Button } from './Button';

export type ConfirmOptions = {
  title: string;
  description?: string;
  confirmLabel?: string;
  cancelLabel?: string;
  destructive?: boolean;
};

type ConfirmationDialogProps = ConfirmOptions & {
  visible: boolean;
  onConfirm: () => void;
  onCancel: () => void;
};

export function ConfirmationDialog({
  visible,
  title,
  description,
  confirmLabel = 'Xác nhận',
  cancelLabel = 'Huỷ',
  destructive = false,
  onConfirm,
  onCancel,
}: ConfirmationDialogProps) {
  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onCancel}>
      <View className="flex-1 items-center justify-center px-space-xl" style={{ backgroundColor: 'rgba(10, 31, 61, 0.45)' }}>
        <View className="w-full bg-surface-container-lowest rounded-lg p-space-lg">
          <Text className="font-headline-md text-headline-md text-on-surface">{title}</Text>
          {description && (
            <Text className="font-body-md text-body-md text-on-surface-variant mt-space-sm">{description}</Text>
          )}
          <View className="flex-row gap-space-sm mt-space-lg">
            <View className="flex-1">
              <Button label={cancelLabel} variant="secondary" onPress={onCancel} />
            </View>
            <View className="flex-1">
              <Button
                label={confirmLabel}
                variant="primary"
                tone={destructive ? 'danger' : undefined}
                onPress={onConfirm}
              />
            </View>
          </View>
        </View>
      </View>
    </Modal>
  );
}
