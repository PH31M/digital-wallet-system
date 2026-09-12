import { useRef } from 'react';
import { TextInput, View } from 'react-native';
import { colors } from '../theme/tokens';

const OTP_LENGTH = 6;

type OtpInputProps = {
  value: string;
  onChange: (code: string) => void;
  onComplete?: (code: string) => void;
  error?: boolean;
};

export function OtpInput({ value, onChange, onComplete, error = false }: OtpInputProps) {
  const inputRefs = useRef<Array<TextInput | null>>([]);
  const digits = Array.from({ length: OTP_LENGTH }, (_, i) => value[i] ?? '');

  function setDigitAt(index: number, text: string) {
    const cleaned = text.replace(/[^0-9]/g, '');

    if (cleaned.length > 1) {
      // Paste toàn bộ mã cùng lúc
      const pasted = (value.slice(0, index) + cleaned).slice(0, OTP_LENGTH);
      onChange(pasted);
      const nextIndex = Math.min(pasted.length, OTP_LENGTH - 1);
      inputRefs.current[nextIndex]?.focus();
      if (pasted.length === OTP_LENGTH) onComplete?.(pasted);
      return;
    }

    const next = digits.slice();
    next[index] = cleaned;
    const nextValue = next.join('').slice(0, OTP_LENGTH);
    onChange(nextValue);

    if (cleaned && index < OTP_LENGTH - 1) {
      inputRefs.current[index + 1]?.focus();
    }
    if (nextValue.length === OTP_LENGTH) onComplete?.(nextValue);
  }

  function handleKeyPress(index: number, key: string) {
    if (key === 'Backspace' && !digits[index] && index > 0) {
      inputRefs.current[index - 1]?.focus();
      const next = digits.slice();
      next[index - 1] = '';
      onChange(next.join(''));
    }
  }

  return (
    <View className="flex-row justify-between">
      {digits.map((digit, index) => (
        <TextInput
          key={index}
          ref={(el) => {
            inputRefs.current[index] = el;
          }}
          value={digit}
          onChangeText={(text) => setDigitAt(index, text)}
          onKeyPress={({ nativeEvent }) => handleKeyPress(index, nativeEvent.key)}
          keyboardType="number-pad"
          maxLength={OTP_LENGTH}
          textAlign="center"
          className="w-12 h-14 rounded-md bg-surface-container-lowest font-headline-md text-headline-md text-on-surface"
          style={{
            borderWidth: digit ? 2 : 1,
            borderColor: error ? colors.error : digit ? colors.primaryContainer : colors.outlineVariant,
          }}
        />
      ))}
    </View>
  );
}
