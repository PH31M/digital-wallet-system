import { Pressable, Text, View } from 'react-native';
import { Icon } from './Icon';
import { colors } from '../theme/tokens';

type CheckboxProps = {
  checked: boolean;
  onChange: (checked: boolean) => void;
  label?: string;
};

/** Selection control theo đúng mô tả trong Design.md — 20px, bo 4px, fill primary-container khi chọn. */
export function Checkbox({ checked, onChange, label }: CheckboxProps) {
  return (
    <Pressable onPress={() => onChange(!checked)} className="flex-row items-center gap-space-sm">
      <View
        className="w-5 h-5 rounded items-center justify-center"
        style={{
          backgroundColor: checked ? colors.primaryContainer : colors.surfaceContainerHigh,
        }}
      >
        {checked && <Icon name="check" size={14} color={colors.onPrimary} />}
      </View>
      {label && <Text className="font-body-md text-body-md text-on-surface-variant">{label}</Text>}
    </Pressable>
  );
}
