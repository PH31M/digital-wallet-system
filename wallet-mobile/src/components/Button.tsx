import { ActivityIndicator, Pressable, Text } from 'react-native';
import { colors } from '../theme/tokens';

type ButtonVariant = 'primary' | 'secondary' | 'text';

type ButtonProps = {
  label: string;
  onPress?: () => void;
  variant?: ButtonVariant;
  /** Chỉ áp dụng cho variant="primary" — dùng cho nút "Xác nhận" phá huỷ trong ConfirmationDialog. */
  tone?: 'danger';
  loading?: boolean;
  disabled?: boolean;
  fullWidth?: boolean;
};

const containerBase = 'h-12 rounded-md items-center justify-center px-space-lg';

const containerByVariant: Record<ButtonVariant, string> = {
  primary: 'bg-primary-container',
  secondary: 'bg-transparent border-[1.5px] border-primary-container',
  text: 'bg-transparent',
};

const labelByVariant: Record<ButtonVariant, string> = {
  primary: 'text-on-primary',
  secondary: 'text-primary-container',
  text: 'text-primary-container',
};

const pressedBackgroundByVariant: Record<ButtonVariant, string | undefined> = {
  primary: colors.primary,
  secondary: colors.surfaceContainerHigh,
  text: colors.surfaceContainerHigh,
};

const spinnerColorByVariant: Record<ButtonVariant, string> = {
  primary: colors.onPrimary,
  secondary: colors.primaryContainer,
  text: colors.primaryContainer,
};

export function Button({
  label,
  onPress,
  variant = 'primary',
  tone,
  loading = false,
  disabled = false,
  fullWidth = true,
}: ButtonProps) {
  const isDisabled = disabled || loading;
  const isDanger = variant === 'primary' && tone === 'danger';

  return (
    <Pressable
      onPress={onPress}
      disabled={isDisabled}
      className={`${containerBase} ${isDanger ? 'bg-danger' : containerByVariant[variant]} ${
        fullWidth ? 'w-full' : ''
      } ${isDisabled ? 'opacity-40' : ''}`}
      style={({ pressed }) =>
        pressed && !isDisabled
          ? { backgroundColor: isDanger ? colors.onErrorContainer : pressedBackgroundByVariant[variant] }
          : undefined
      }
    >
      {loading ? (
        <ActivityIndicator color={spinnerColorByVariant[variant]} />
      ) : (
        <Text className={`font-label-md text-label-md ${labelByVariant[variant]}`}>{label}</Text>
      )}
    </Pressable>
  );
}
