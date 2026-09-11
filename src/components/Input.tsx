import { ReactNode, useState } from 'react';
import { Pressable, Text, TextInput, TextInputProps, View } from 'react-native';
import { Icon, IconName } from './Icon';
import { colors } from '../theme/tokens';

type InputProps = TextInputProps & {
  label: string;
  error?: string;
  helperText?: string;
  leadingIcon?: IconName;
  /** Icon/nội dung phụ đặt trước nút hiện mật khẩu (vd. dấu tích khớp mật khẩu xác nhận). */
  trailingAdornment?: ReactNode;
};

export function Input({ label, error, helperText, leadingIcon, trailingAdornment, secureTextEntry, ...rest }: InputProps) {
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
        {leadingIcon && <Icon name={leadingIcon} size={20} color={colors.outline} />}
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
          className={`flex-1 font-body-lg text-body-lg text-on-surface ${leadingIcon ? 'ml-space-sm' : ''}`}
        />
        {trailingAdornment}
        {isPasswordField && (
          <Pressable onPress={() => setIsPasswordVisible((v) => !v)} hitSlop={8} className="ml-space-xs">
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
