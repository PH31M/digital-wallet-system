import { View, ViewProps } from 'react-native';

export function Card({ className = '', style, children, ...rest }: ViewProps & { className?: string }) {
  return (
    <View
      className={`bg-surface-container-lowest rounded-lg p-space-md ${className}`}
      style={[{ shadowColor: '#0F172A', shadowOpacity: 0.06, shadowRadius: 3, shadowOffset: { width: 0, height: 1 }, elevation: 2 }, style]}
      {...rest}
    >
      {children}
    </View>
  );
}
