import { useState } from 'react';
import { ActivityIndicator, Pressable, ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { CommonActions, useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { AuthHeader } from '../components/AuthHeader';
import { Button } from '../components/Button';
import { Checkbox } from '../components/Checkbox';
import { GoogleIcon } from '../components/GoogleIcon';
import { Icon } from '../components/Icon';
import { Input } from '../components/Input';
import { Logo } from '../components/Logo';
import { useToast } from '../hooks/useToast';
import { colors } from '../theme/tokens';
import { RootStackParamList } from '../navigation/RootNavigator';

type LoginState = 'idle' | 'loading' | 'success';

export function LoginScreen() {
  const navigation = useNavigation<NativeStackNavigationProp<RootStackParamList>>();
  const { showToast } = useToast();

  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [rememberMe, setRememberMe] = useState(true);
  const [loginState, setLoginState] = useState<LoginState>('idle');

  function comingSoon() {
    showToast('Tính năng sắp ra mắt', 'info');
  }

  function goToApp() {
    navigation.dispatch(CommonActions.reset({ index: 0, routes: [{ name: 'Main' }] }));
  }

  function handleLogin() {
    if (!identifier.trim() || !password) {
      showToast('Vui lòng nhập đầy đủ thông tin đăng nhập', 'danger');
      return;
    }
    setLoginState('loading');
    setTimeout(() => {
      setLoginState('success');
      setTimeout(goToApp, 500);
    }, 800);
  }

  return (
    <SafeAreaView className="flex-1 bg-background" edges={['top']}>
      <AuthHeader
        onBack={navigation.canGoBack() ? () => navigation.goBack() : undefined}
        onHelpPress={comingSoon}
      />

      <ScrollView className="flex-1 px-margin-mobile" contentContainerStyle={{ paddingVertical: 16 }}>
        <View className="items-center gap-space-md mb-space-lg">
          <View className="w-16 h-16 rounded-md bg-surface-container-low items-center justify-center">
            <Logo size={48} />
          </View>
          <View className="items-center gap-space-xs">
            <Text className="font-headline-lg text-headline-lg text-primary tracking-tight">Đăng nhập tài khoản</Text>
            <Text className="font-body-md text-body-md text-on-surface-variant text-center" style={{ maxWidth: 280 }}>
              Chào mừng trở lại với hệ thống thanh toán số an toàn PMPay
            </Text>
          </View>
        </View>

        <View className="gap-space-md">
          <Input
            label="Email hoặc số điện thoại"
            leadingIcon="alternate_email"
            placeholder="Ví dụ: 0987 654 321 hoặc an.nguyen@email.com"
            autoCapitalize="none"
            value={identifier}
            onChangeText={setIdentifier}
          />
          <Input
            label="Mật khẩu"
            leadingIcon="lock"
            placeholder="Nhập mật khẩu an toàn"
            secureTextEntry
            value={password}
            onChangeText={setPassword}
          />

          <View className="flex-row items-center justify-between mt-space-xs">
            <Checkbox checked={rememberMe} onChange={setRememberMe} label="Ghi nhớ đăng nhập" />
            <Pressable onPress={() => navigation.navigate('ForgotPassword')}>
              <Text className="font-label-md text-label-md text-primary-container">Quên mật khẩu?</Text>
            </Pressable>
          </View>

          <View className="mt-space-sm">
            {loginState === 'idle' && <Button label="Đăng nhập" onPress={handleLogin} />}
            {loginState === 'loading' && (
              <View className="h-12 rounded-md bg-primary-container items-center justify-center flex-row gap-space-sm">
                <ActivityIndicator color={colors.onPrimary} />
                <Text className="font-headline-sm text-headline-sm text-on-primary">Đang kết nối...</Text>
              </View>
            )}
            {loginState === 'success' && (
              <View className="h-12 rounded-md bg-primary-container items-center justify-center flex-row gap-space-sm">
                <Text className="font-headline-sm text-headline-sm text-on-primary">Đăng nhập thành công</Text>
                <Icon name="check_circle" size={20} color={colors.onPrimary} />
              </View>
            )}
          </View>
        </View>

        <Pressable
          onPress={comingSoon}
          className="mt-space-md flex-row items-center justify-center gap-space-sm py-3 px-space-md rounded-md bg-surface-container-low"
        >
          <View className="w-7 h-7 rounded-full bg-secondary-container items-center justify-center">
            <Icon name="fingerprint" size={18} color={colors.onSecondaryContainer} />
          </View>
          <Text className="font-label-md text-label-md text-primary font-semibold">
            Đăng nhập nhanh bằng Face ID / Vân tay
          </Text>
        </Pressable>

        <View className="flex-row items-center my-space-lg">
          <View className="flex-1 h-px bg-surface-container-highest" />
          <Text className="px-space-sm font-label-caption text-label-caption uppercase tracking-wider text-outline">
            hoặc đăng nhập bằng
          </Text>
          <View className="flex-1 h-px bg-surface-container-highest" />
        </View>

        <Pressable
          onPress={comingSoon}
          className="h-12 rounded-md bg-surface-container-lowest items-center justify-center flex-row gap-space-sm"
        >
          <GoogleIcon size={20} />
          <Text className="font-headline-sm text-headline-sm text-on-surface">Đăng nhập với Google</Text>
        </Pressable>

        <View className="mt-space-lg flex-row items-center justify-center gap-space-xs p-space-sm rounded-md bg-surface-container-low">
          <Icon name="verified_user" size={18} color={colors.secondary} />
          <Text className="font-body-sm text-body-sm text-on-surface-variant text-center">
            Được bảo vệ bởi <Text className="font-semibold text-secondary">PM Shield</Text> & chuẩn PCI-DSS Level 1
          </Text>
        </View>

        <View className="mt-space-lg flex-row items-center justify-center flex-wrap">
          <Text className="font-body-md text-body-md text-on-surface-variant">Chưa có tài khoản? </Text>
          <Pressable onPress={() => navigation.navigate('Register')}>
            <Text className="font-headline-sm text-headline-sm text-primary" style={{ textDecorationLine: 'underline' }}>
              Đăng ký ngay
            </Text>
          </Pressable>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}
