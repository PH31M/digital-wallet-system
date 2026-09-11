import { useMemo, useState } from 'react';
import { Pressable, ScrollView, Text, TextInput, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useNavigation } from '@react-navigation/native';
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

type StrengthScore = 0 | 1 | 2 | 3 | 4;

const STRENGTH_LABEL: Record<StrengthScore, string> = {
  0: 'Chưa nhập',
  1: 'Rất yếu',
  2: 'Yếu',
  3: 'Khá',
  4: 'Mạnh & An toàn',
};

const STRENGTH_COLOR: Record<StrengthScore, string> = {
  0: colors.outline,
  1: colors.error,
  2: colors.error,
  3: colors.primaryContainer,
  4: colors.secondary,
};

function evaluatePassword(password: string) {
  const hasLength = password.length >= 8;
  const hasUpper = /[A-Z]/.test(password);
  const hasSpecialAndNumber = /[0-9]/.test(password) && /[^A-Za-z0-9]/.test(password);
  const score = (password.length > 0 ? 1 : 0) + Number(hasLength) + Number(hasUpper) + Number(hasSpecialAndNumber);
  return { hasLength, hasUpper, hasSpecialAndNumber, score: score as StrengthScore };
}

function RuleRow({ met, label }: { met: boolean; label: string }) {
  return (
    <View className="flex-row items-center gap-1.5">
      {met ? (
        <Icon name="check_circle" size={14} color={colors.secondary} />
      ) : (
        <View className="w-3.5 h-3.5 rounded-full border" style={{ borderColor: colors.outline }} />
      )}
      <Text
        className="font-label-caption text-label-caption"
        style={{ color: met ? colors.secondary : colors.onSurfaceVariant }}
      >
        {label}
      </Text>
    </View>
  );
}

export function RegisterScreen() {
  const navigation = useNavigation<NativeStackNavigationProp<RootStackParamList>>();
  const { showToast } = useToast();

  const [fullName, setFullName] = useState('');
  const [phone, setPhone] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [agreed, setAgreed] = useState(false);

  const strength = useMemo(() => evaluatePassword(password), [password]);
  const passwordsMatch = confirmPassword.length > 0 && password === confirmPassword;

  function comingSoon() {
    showToast('Tính năng sắp ra mắt', 'info');
  }

  function handleSubmit() {
    if (!fullName.trim() || !phone.trim() || !email.trim() || !password || !confirmPassword) {
      showToast('Vui lòng điền đầy đủ thông tin bắt buộc', 'danger');
      return;
    }
    if (!passwordsMatch) {
      showToast('Mật khẩu xác nhận không khớp', 'danger');
      return;
    }
    if (!agreed) {
      showToast('Vui lòng đồng ý Điều khoản sử dụng & Chính sách bảo mật', 'danger');
      return;
    }
    navigation.navigate('RegisterOtp', { phone });
  }

  return (
    <SafeAreaView className="flex-1 bg-background" edges={['top']}>
      <AuthHeader onBack={() => navigation.goBack()} onHelpPress={comingSoon} />
      <ScrollView className="flex-1 px-margin-mobile" contentContainerStyle={{ paddingVertical: 16, paddingBottom: 48 }}>
        <View className="items-center gap-space-xs mb-space-lg">
          <View className="flex-row items-center gap-space-xs mb-space-sm">
            <Logo size={32} />
            <Text className="font-headline-sm text-headline-sm text-primary font-bold">PMPay</Text>
          </View>
          <Text className="font-headline-lg text-headline-lg text-primary tracking-tight">Mở tài khoản ví mới</Text>
          <Text className="font-body-md text-body-md text-on-surface-variant text-center" style={{ maxWidth: 280 }}>
            Trải nghiệm thanh toán số bảo mật chuẩn ngân hàng trong 1 phút
          </Text>
        </View>

        <View className="bg-surface-container-lowest rounded-lg p-space-md gap-space-md">
          <Input
            label="Họ và tên *"
            leadingIcon="person"
            placeholder="Nguyễn Văn An"
            value={fullName}
            onChangeText={setFullName}
            helperText="Nhập đúng họ tên trên CCCD/Hộ chiếu"
          />

          <View className="gap-1.5">
            <View className="flex-row items-center justify-between">
              <Text className="font-label-md text-label-md text-primary">Số điện thoại *</Text>
              <View className="px-2 py-0.5 rounded-full" style={{ backgroundColor: 'rgba(0,108,73,0.1)' }}>
                <Text className="font-label-caption text-label-caption text-secondary">Sẽ nhận mã OTP xác thực</Text>
              </View>
            </View>
            <View className="flex-row gap-2">
              <View className="h-12 px-3 rounded-md bg-surface-container-low flex-row items-center gap-1.5">
                <Text style={{ fontSize: 18 }}>🇻🇳</Text>
                <Text className="font-numeric-ledger text-numeric-ledger text-primary font-bold">+84</Text>
              </View>
              <TextInput
                value={phone}
                onChangeText={setPhone}
                placeholder="0987 654 321"
                placeholderTextColor={colors.outline}
                keyboardType="phone-pad"
                className="flex-1 h-12 px-3 rounded-md bg-surface-container-low font-numeric-ledger text-numeric-ledger text-on-surface"
              />
            </View>
          </View>

          <Input
            label="Địa chỉ Email *"
            leadingIcon="mail"
            placeholder="an.nguyen@email.com"
            autoCapitalize="none"
            keyboardType="email-address"
            value={email}
            onChangeText={setEmail}
          />

          <View>
            <Input
              label="Mật khẩu *"
              leadingIcon="lock"
              placeholder="••••••••"
              secureTextEntry
              value={password}
              onChangeText={setPassword}
            />
            <View className="gap-1.5 mt-1.5 bg-surface-container-low/60 p-2.5 rounded-md">
              <View className="flex-row items-center justify-between">
                <Text className="font-label-caption text-label-caption text-on-surface-variant">Độ bảo mật:</Text>
                <Text
                  className="font-label-caption text-label-caption font-bold"
                  style={{ color: STRENGTH_COLOR[strength.score] }}
                >
                  {STRENGTH_LABEL[strength.score]}
                </Text>
              </View>
              <View className="flex-row gap-1 h-1.5 w-full bg-surface-container-highest rounded-full overflow-hidden">
                {[1, 2, 3, 4].map((bar) => (
                  <View
                    key={bar}
                    className="flex-1 h-full rounded-full"
                    style={{
                      backgroundColor: bar <= strength.score ? STRENGTH_COLOR[strength.score] : 'rgba(116,119,128,0.3)',
                    }}
                  />
                ))}
              </View>
              <View className="gap-1 mt-1">
                <RuleRow met={strength.hasLength} label="Tối thiểu 8 ký tự" />
                <RuleRow met={strength.hasUpper} label="Ít nhất 1 chữ hoa" />
                <RuleRow met={strength.hasSpecialAndNumber} label="Chữ số & ký tự đặc biệt (!@#$...)" />
              </View>
            </View>
          </View>

          <View>
            <Input
              label="Xác nhận mật khẩu *"
              leadingIcon="lock_reset"
              placeholder="••••••••"
              secureTextEntry
              value={confirmPassword}
              onChangeText={setConfirmPassword}
              trailingAdornment={
                passwordsMatch ? <Icon name="check_circle" size={20} color={colors.secondary} /> : undefined
              }
            />
            {passwordsMatch && (
              <View className="flex-row items-center gap-1 mt-1">
                <Icon name="check" size={14} color={colors.secondary} />
                <Text className="font-label-caption text-label-caption text-secondary">
                  Mật khẩu xác nhận hoàn toàn trùng khớp
                </Text>
              </View>
            )}
          </View>

          <View className="flex-row items-start gap-2.5 pt-1">
            <Checkbox checked={agreed} onChange={setAgreed} />
            <Pressable onPress={() => setAgreed((v) => !v)} className="flex-1">
              <Text className="font-body-sm text-body-sm text-on-surface-variant leading-tight">
                Tôi đã đọc và đồng ý với{' '}
                <Text onPress={comingSoon} className="text-primary font-bold">
                  Điều khoản sử dụng
                </Text>{' '}
                và{' '}
                <Text onPress={comingSoon} className="text-primary font-bold">
                  Chính sách bảo mật
                </Text>{' '}
                của PMPay.
              </Text>
            </Pressable>
          </View>

          <Button label="Tạo tài khoản" onPress={handleSubmit} />

          <View className="flex-row items-center gap-2 bg-surface-container-low p-2.5 rounded-md">
            <Icon name="verified_user" size={18} color={colors.primary} />
            <Text className="flex-1 font-label-caption text-label-caption text-on-surface-variant">
              Sau khi bấm Tạo tài khoản, hệ thống sẽ gửi mã OTP 6 chữ số đến số điện thoại của bạn để kích hoạt ví.
            </Text>
          </View>
        </View>

        <View className="flex-row items-center my-space-lg">
          <View className="flex-1 h-px bg-surface-container-highest" />
          <Text className="px-3 font-label-sm text-label-sm text-outline uppercase tracking-wider">hoặc</Text>
          <View className="flex-1 h-px bg-surface-container-highest" />
        </View>

        <Pressable
          onPress={comingSoon}
          className="h-12 rounded-md bg-surface-container-lowest items-center justify-center flex-row gap-space-sm"
        >
          <GoogleIcon size={20} />
          <Text className="font-label-md text-label-md text-on-surface font-semibold">Đăng ký nhanh bằng Google</Text>
        </Pressable>

        <View className="mt-space-xl flex-row items-center justify-center flex-wrap">
          <Text className="font-body-md text-body-md text-on-surface-variant">Đã có tài khoản? </Text>
          <Pressable onPress={() => navigation.navigate('Login')}>
            <Text className="font-headline-sm text-headline-sm text-primary" style={{ textDecorationLine: 'underline' }}>
              Đăng nhập ngay
            </Text>
          </Pressable>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}
