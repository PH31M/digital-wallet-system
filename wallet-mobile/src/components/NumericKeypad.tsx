import { Pressable, Text, View } from 'react-native';
import { Icon } from './Icon';
import { colors } from '../theme/tokens';

type NumericKeypadProps = {
  onDigitPress: (digit: string) => void;
  onBackspace: () => void;
  onBiometricPress?: () => void;
};

const DIGIT_ROWS = [
  ['1', '2', '3'],
  ['4', '5', '6'],
  ['7', '8', '9'],
];

/** Bàn phím số tuỳ chỉnh (mục 2.2 spec) — dùng cho màn cần nhập số/OTP không qua bàn phím hệ thống. */
export function NumericKeypad({ onDigitPress, onBackspace, onBiometricPress }: NumericKeypadProps) {
  return (
    <View className="gap-space-sm">
      {DIGIT_ROWS.map((row, rowIndex) => (
        <View key={rowIndex} className="flex-row gap-space-sm">
          {row.map((digit) => (
            <Pressable
              key={digit}
              onPress={() => onDigitPress(digit)}
              className="flex-1 h-12 rounded-md bg-surface-container-low items-center justify-center"
            >
              <Text className="font-headline-md text-headline-md text-primary font-bold">{digit}</Text>
            </Pressable>
          ))}
        </View>
      ))}
      <View className="flex-row gap-space-sm">
        <Pressable
          onPress={onBiometricPress}
          disabled={!onBiometricPress}
          className="flex-1 h-12 rounded-md bg-surface items-center justify-center"
        >
          {onBiometricPress && <Icon name="fingerprint" size={24} color={colors.primaryContainer} />}
        </Pressable>
        <Pressable
          onPress={() => onDigitPress('0')}
          className="flex-1 h-12 rounded-md bg-surface-container-low items-center justify-center"
        >
          <Text className="font-headline-md text-headline-md text-primary font-bold">0</Text>
        </Pressable>
        <Pressable onPress={onBackspace} className="flex-1 h-12 rounded-md bg-surface items-center justify-center">
          <Icon name="backspace" size={22} color={colors.onSurface} />
        </Pressable>
      </View>
    </View>
  );
}
