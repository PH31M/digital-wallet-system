import { useState } from 'react';
import { Pressable, Text, TextInput, TextInputProps, View } from 'react-native';
import { Icon } from './Icon';
import { colors } from '../theme/tokens';

type InputProps = TextInputProps & {
  label: string;
  error?: string;
  helperText?: string;
};

export function Input({ label, error, helperText, secureTextEntry, ...rest }: InputProps) {
  const [isFocused, setIsFocused] = useState(false);
  const [isPasswordVisible, setIsPasswordVisible] = useState(false);
  const isPasswordField = !!secureTextEntry;

  const borderColor = error ? colors.error : isFocused ? colors.primaryContainer : colors.outlineVariant;
  const borderWidth = isFocused ? 2 : 1;

  return (
    <View>
      <Text className="font-label-md text-label-md text-on-surface-variant mb-space-xs">{label}</Text>
      <View
        className="h-12 rounded-md bg-surface-container-lowest flex-row items-center px-space-md"
        style={{ borderColor, borderWidth }}
      >
        <TextInput
          {...rest}
          secureTextEntry={isPasswordField && !isPasswordVisible}
          onFocus={(e) => {
            setIsFocused(true);
            rest.onFocus?.(e);
          }}
          onBlur={(e) => {
            setIsFocused(false);
            rest.onBlur?.(e);
          }}
          placeholderTextColor={colors.onSurfaceVariant}
          className="flex-1 font-body-lg text-body-lg text-on-surface"
        />
        {isPasswordField && (
          <Pressable onPress={() => setIsPasswordVisible((v) => !v)} hitSlop={8}>
            <Icon name={isPasswordVisible ? 'visibility_off' : 'visibility'} size={20} color={colors.onSurfaceVariant} />
          </Pressable>
        )}
      </View>
      {(error || helperText) && (
        <Text className={`font-body-sm text-body-sm mt-space-xs ${error ? 'text-danger' : 'text-on-surface-variant'}`}>
          {error || helperText}
        </Text>
      )}
    </View>
  );
}
