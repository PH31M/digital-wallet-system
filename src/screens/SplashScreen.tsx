import { useEffect, useRef } from 'react';
import { Animated, Easing, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { CommonActions, useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { Icon } from '../components/Icon';
import { Logo } from '../components/Logo';
import { colors } from '../theme/tokens';
import { RootStackParamList } from '../navigation/RootNavigator';

const SPLASH_DURATION_MS = 1800;

/**
 * Màn chờ có thương hiệu, hiển thị 1 lần khi app khởi động (sau khi native splash của Expo
 * đã ẩn) rồi tự chuyển sang Đăng nhập — khác với `expo-splash-screen` (chỉ che lúc load font).
 * Các vòng tròn glow nền và hiệu ứng backdrop-blur trong code.html được thay bằng khối màu
 * bán trong suốt đơn giản (RN không hỗ trợ CSS blur trên native, giống quyết định đã áp dụng
 * cho AppHeader).
 */
export function SplashScreen() {
  const navigation = useNavigation<NativeStackNavigationProp<RootStackParamList>>();
  const progress = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    Animated.timing(progress, {
      toValue: 1,
      duration: SPLASH_DURATION_MS,
      easing: Easing.out(Easing.cubic),
      useNativeDriver: false,
    }).start();

    const timer = setTimeout(() => {
      navigation.dispatch(CommonActions.reset({ index: 0, routes: [{ name: 'Login' }] }));
    }, SPLASH_DURATION_MS);
    return () => clearTimeout(timer);
  }, [navigation, progress]);

  const barWidth = progress.interpolate({ inputRange: [0, 1], outputRange: ['12%', '100%'] });

  return (
    <View className="flex-1 bg-primary" style={{ position: 'relative', overflow: 'hidden' }}>
      <View
        style={{ position: 'absolute', top: -110, left: -110, width: 300, height: 300, borderRadius: 150, backgroundColor: colors.primaryContainer, opacity: 0.4 }}
      />
      <View
        style={{ position: 'absolute', top: 160, right: -100, width: 260, height: 260, borderRadius: 130, backgroundColor: colors.tertiary, opacity: 0.35 }}
      />
      <View
        style={{ position: 'absolute', bottom: -110, left: 40, width: 300, height: 300, borderRadius: 150, backgroundColor: colors.secondary, opacity: 0.15 }}
      />

      <SafeAreaView className="flex-1 justify-between" edges={['top', 'bottom']}>
        <View className="items-end px-gutter-mobile pt-space-sm">
          <View
            className="flex-row items-center gap-1.5 px-3 py-1.5 rounded-full"
            style={{ backgroundColor: 'rgba(255,255,255,0.1)' }}
          >
            <View className="w-2 h-2 rounded-full" style={{ backgroundColor: colors.secondaryFixed }} />
            <Text
              className="font-label-caption text-label-caption uppercase tracking-wider"
              style={{ color: colors.surfaceContainerHighest }}
            >
              Hệ thống sẵn sàng
            </Text>
          </View>
        </View>

        <View className="items-center px-margin-mobile">
          <View className="items-center justify-center mb-space-lg" style={{ position: 'relative' }}>
            <View
              style={{
                position: 'absolute',
                width: 150,
                height: 150,
                borderRadius: 75,
                backgroundColor: colors.secondary,
                opacity: 0.3,
              }}
            />
            <Logo size={96} />
          </View>

          <View className="flex-row items-center gap-1.5 mb-space-xs">
            <Text className="font-headline-xl text-headline-xl font-bold" style={{ color: colors.onPrimary }}>
              PMPay
            </Text>
            <View className="w-2 h-2 rounded-full" style={{ backgroundColor: colors.secondaryFixed }} />
          </View>
          <Text
            className="font-body-md text-body-md text-center leading-relaxed"
            style={{ color: colors.surfaceContainerHigh, maxWidth: 260, opacity: 0.9 }}
          >
            Ví điện tử & Thanh toán số an toàn
          </Text>

          <View className="items-center gap-2" style={{ marginTop: 48 }}>
            <View
              className="rounded-full overflow-hidden"
              style={{ width: 144, height: 4, backgroundColor: 'rgba(255,255,255,0.15)' }}
            >
              <Animated.View
                style={{ height: '100%', borderRadius: 9999, backgroundColor: colors.secondaryFixed, width: barWidth }}
              />
            </View>
            <Text className="font-label-caption text-label-caption" style={{ color: colors.surfaceContainerHighest, opacity: 0.75 }}>
              Đang thiết lập kết nối mã hóa TLS 1.3
            </Text>
          </View>
        </View>

        <View className="items-center px-gutter-mobile pb-space-sm gap-space-sm">
          <View
            className="flex-row items-center gap-2 px-4 py-2 rounded-full"
            style={{ backgroundColor: 'rgba(255,255,255,0.05)' }}
          >
            <Icon name="verified_user" size={18} color={colors.secondaryFixed} />
            <Text
              className="font-label-caption text-label-caption tracking-wide"
              style={{ color: colors.surfaceContainerHighest }}
            >
              Bảo mật đa tầng • Chuẩn PCI-DSS Level 1 & Napas 247
            </Text>
          </View>
          <View className="flex-row items-center gap-2">
            <Text className="font-label-caption text-label-caption" style={{ color: colors.surfaceContainerHighest, opacity: 0.6 }}>
              Ngân hàng liên kết chuyển khoản tức thì
            </Text>
            <View className="w-1 h-1 rounded-full" style={{ backgroundColor: colors.surfaceContainerHighest, opacity: 0.4 }} />
            <Text className="font-label-caption text-label-caption" style={{ color: colors.surfaceContainerHighest, opacity: 0.6 }}>
              Phiên bản v3.4.2
            </Text>
          </View>
        </View>
      </SafeAreaView>
    </View>
  );
}
