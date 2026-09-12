import { useEffect, useRef } from 'react';
import { Animated } from 'react-native';
import { colors } from '../theme/tokens';

type LoadingSkeletonProps = {
  width?: number | `${number}%`;
  height?: number;
  radius?: number;
};

export function LoadingSkeleton({ width = '100%', height = 16, radius = 8 }: LoadingSkeletonProps) {
  const opacity = useRef(new Animated.Value(0.4)).current;

  useEffect(() => {
    const pulse = Animated.loop(
      Animated.sequence([
        Animated.timing(opacity, { toValue: 1, duration: 700, useNativeDriver: true }),
        Animated.timing(opacity, { toValue: 0.4, duration: 700, useNativeDriver: true }),
      ]),
    );
    pulse.start();
    return () => pulse.stop();
  }, [opacity]);

  return (
    <Animated.View
      style={{ width, height, borderRadius: radius, backgroundColor: colors.outlineVariant, opacity }}
    />
  );
}
