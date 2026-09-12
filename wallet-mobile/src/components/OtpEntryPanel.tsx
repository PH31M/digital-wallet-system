import { ReactNode } from 'react';
import { Text, View } from 'react-native';
import { Icon } from './Icon';
import { NumericKeypad } from './NumericKeypad';
import { useToast } from '../hooks/useToast';
import { colors } from '../theme/tokens';

const OTP_LENGTH = 6;

type OtpEntryPanelProps = {
  value: string;
  onChange: (value: string) => void;
  secondsLeft: number;
  onResend: () => void;
  description: ReactNode;
};

/**
 * Khối nhập OTP dùng chung (icon khiên + tiêu đề + 6 ô + đếm ngược + bàn phím số) — tách ra
 * từ màn Xác nhận OTP giao dịch để dùng lại cho OTP kích hoạt tài khoản (Đăng ký) mà không
 * lặp lại phần cơ chế nhập, chỉ đổi mô tả/nội dung xung quanh theo từng luồng.
 */
export function OtpEntryPanel({ value, onChange, secondsLeft, onResend, description }: OtpEntryPanelProps) {
  const { showToast } = useToast();

  function handleDigit(digit: string) {
    if (value.length >= OTP_LENGTH) return;
    onChange(value + digit);
  }

  function handleBackspace() {
    onChange(value.slice(0, -1));
  }

  const countdownLabel = `00:${secondsLeft.toString().padStart(2, '0')}`;

  return (
    <>
      <View className="bg-surface-container-lowest rounded-lg p-space-md items-center">
        <View className="w-14 h-14 rounded-full bg-surface-container-high items-center justify-center mb-3">
          <Icon name="shield_lock" size={30} color={colors.primaryContainer} />
        </View>
        <Text className="font-headline-md text-headline-md text-primary font-bold text-center">
          Nhập mã xác thực OTP
        </Text>
        <Text className="font-body-sm text-body-sm text-on-surface-variant mt-1 text-center" style={{ maxWidth: 280 }}>
          {description}
        </Text>

        <View className="flex-row items-center justify-center gap-2 mt-5 w-full">
          {Array.from({ length: OTP_LENGTH }, (_, i) => {
            const digit = value[i];
            const isActive = i === value.length;
            return (
              <View
                key={i}
                className="w-11 h-14 rounded-md items-center justify-center"
                style={{
                  backgroundColor: colors.surfaceContainerLow,
                  borderWidth: isActive ? 2 : 0,
                  borderColor: colors.primary,
                }}
              >
                {digit ? (
                  <Text className="font-numeric-balance-mobile text-numeric-balance-mobile text-primary font-bold">
                    {digit}
                  </Text>
                ) : (
                  <View className="w-2 h-2 rounded-full bg-outline-variant" />
                )}
              </View>
            );
          })}
        </View>

        <View className="mt-4 items-center gap-1.5">
          <View className="flex-row items-center gap-1.5">
            <Icon name="schedule" size={16} color={colors.onSurfaceVariant} />
            <Text className="font-label-md text-label-md text-on-surface-variant">
              Gửi lại mã sau:{' '}
              <Text className="font-semibold text-primary font-numeric-ledger text-numeric-ledger">
                {countdownLabel}
              </Text>
            </Text>
          </View>
          <View className="flex-row items-center gap-3 pt-1">
            <Text
              onPress={() => showToast('Tính năng sắp ra mắt', 'info')}
              className="font-label-sm text-label-sm text-primary font-semibold"
            >
              Gọi lấy mã thoại
            </Text>
            <View className="w-1 h-1 rounded-full bg-outline-variant" />
            <Text
              onPress={onResend}
              className="font-label-sm text-label-sm"
              style={{ color: secondsLeft > 0 ? colors.outline : colors.primary }}
            >
              {secondsLeft > 0 ? 'Đổi số nhận OTP' : 'Gửi lại mã'}
            </Text>
          </View>
        </View>
      </View>

      <View className="bg-surface-container-lowest rounded-lg p-3">
        <NumericKeypad onDigitPress={handleDigit} onBackspace={handleBackspace} />
      </View>
    </>
  );
}
