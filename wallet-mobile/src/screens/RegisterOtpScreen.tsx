import { useEffect, useState } from 'react';
import { ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { CommonActions, useNavigation, useRoute, RouteProp } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { Button } from '../components/Button';
import { Icon } from '../components/Icon';
import { OtpEntryPanel } from '../components/OtpEntryPanel';
import { StackHeader } from '../components/StackHeader';
import { useAuth } from '../auth/AuthContext';
import { useToast } from '../hooks/useToast';
import { getApiErrorMessage } from '../api/apiError';
import { colors } from '../theme/tokens';
import { RootStackParamList } from '../navigation/RootNavigator';

const OTP_LENGTH = 6;
const RESEND_SECONDS = 45;

/**
 * OTP kích hoạt tài khoản mới — dùng chung cơ chế nhập với OtpConfirmScreen (giao dịch) theo
 * đúng tinh thần spec 3.4 ("dùng chung nhiều luồng: đăng ký / quên mật khẩu / MFA"), chỉ đổi
 * phần nội dung xung quanh vì design-export không có bản riêng cho luồng đăng ký.
 *
 * SỬA so với bản mock: backend gửi OTP kích hoạt qua EMAIL (`AuthService.sendRegistrationOtp`
 * → `emailOtpService`), KHÔNG PHẢI SMS/số điện thoại như copy gốc — `POST /auth/verify-email`
 * nhận `{user_id, otp}`, `POST /auth/resend-verification` nhận `{user_id, email}`, cả 2 đều
 * không có tham số số điện thoại. Route param đổi từ `{ phone }` sang `{ userId, email }`.
 */
export function RegisterOtpScreen() {
  const navigation = useNavigation<NativeStackNavigationProp<RootStackParamList>>();
  const route = useRoute<RouteProp<RootStackParamList, 'RegisterOtp'>>();
  const { verifyEmail, resendVerification } = useAuth();
  const { showToast } = useToast();
  const { userId, email } = route.params;

  const [otp, setOtp] = useState('');
  const [secondsLeft, setSecondsLeft] = useState(RESEND_SECONDS);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (secondsLeft <= 0) return;
    const timer = setTimeout(() => setSecondsLeft((s) => s - 1), 1000);
    return () => clearTimeout(timer);
  }, [secondsLeft]);

  async function handleResend() {
    if (secondsLeft > 0) return;
    try {
      await resendVerification(userId, email);
      setSecondsLeft(RESEND_SECONDS);
      setOtp('');
      showToast('Đã gửi lại mã OTP qua email', 'success');
    } catch (error) {
      showToast(getApiErrorMessage(error, 'Không gửi lại được mã OTP, vui lòng thử lại sau'), 'danger');
    }
  }

  async function handleConfirm() {
    setSubmitting(true);
    try {
      await verifyEmail(userId, otp);
      navigation.dispatch(CommonActions.reset({ index: 0, routes: [{ name: 'Main' }] }));
    } catch (error) {
      showToast(getApiErrorMessage(error, 'Mã OTP không đúng hoặc đã hết hạn'), 'danger');
      setOtp('');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <SafeAreaView className="flex-1 bg-background" edges={['top']}>
      <StackHeader title="Kích Hoạt Tài Khoản" onBack={() => navigation.goBack()} />
      <ScrollView className="flex-1 px-gutter-mobile" contentContainerStyle={{ paddingVertical: 16, gap: 16 }}>
        <View className="rounded-md p-3.5 flex-row items-center gap-2.5 bg-surface-container-high/60">
          <View className="w-8 h-8 rounded-full items-center justify-center" style={{ backgroundColor: 'rgba(15,44,89,0.1)' }}>
            <Icon name="verified_user" size={18} color={colors.primaryContainer} />
          </View>
          <View className="flex-1">
            <Text className="font-label-caption text-label-caption text-secondary font-semibold uppercase tracking-wider">
              Xác thực email
            </Text>
            <Text className="font-body-sm text-body-sm text-on-surface-variant">
              Hoàn tất bước cuối để kích hoạt ví PMPay của bạn
            </Text>
          </View>
        </View>

        <OtpEntryPanel
          value={otp}
          onChange={setOtp}
          secondsLeft={secondsLeft}
          onResend={handleResend}
          description={
            <>
              Mã OTP 6 chữ số vừa được gửi đến email{' '}
              <Text className="font-semibold text-on-surface">{email}</Text>
            </>
          }
        />

        <Button
          label="Kích hoạt tài khoản"
          onPress={handleConfirm}
          disabled={otp.length !== OTP_LENGTH}
          loading={submitting}
        />

        <View className="flex-row items-center justify-center gap-1.5">
          <Icon name="lock" size={16} color={colors.secondary} />
          <Text className="font-label-caption text-label-caption text-on-surface-variant text-center">
            Bảo vệ bởi <Text className="font-semibold text-primary">PM Shield</Text> & mã hóa 256-bit chuẩn PCI-DSS
          </Text>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}
