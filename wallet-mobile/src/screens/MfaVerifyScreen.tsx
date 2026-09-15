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
 * Màn MỚI, KHÔNG nằm trong 18 màn export từ Stitch — cần cho nhánh `mfaRequired: true` của
 * `POST /auth/login` (tài khoản đã bật MFA — `AuthService.login()` trả `AuthResponse(null,
 * null, user, true)`, không cấp token, phải gọi `POST /auth/mfa/verify {email, otp}` để hoàn
 * tất đăng nhập thật). Tái dùng `OtpEntryPanel` đúng theo comment sẵn có ở RegisterOtpScreen
 * ("dùng chung nhiều luồng: đăng ký / quên mật khẩu / MFA"), không dựng UI riêng mới.
 * Chưa có UI nào trong app bật được MFA thật ở backend (Profile chỉ có toggle biometric cục
 * bộ, không gọi `/users/me/mfa/enable`) nên nhánh này khó gặp trong demo hiện tại — vẫn xử lý
 * đúng cho đủ, không bỏ qua.
 */
export function MfaVerifyScreen() {
  const navigation = useNavigation<NativeStackNavigationProp<RootStackParamList>>();
  const route = useRoute<RouteProp<RootStackParamList, 'MfaVerify'>>();
  const { email } = route.params;
  const { verifyMfa } = useAuth();
  const { showToast } = useToast();

  const [otp, setOtp] = useState('');
  const [secondsLeft, setSecondsLeft] = useState(RESEND_SECONDS);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (secondsLeft <= 0) return;
    const timer = setTimeout(() => setSecondsLeft((s) => s - 1), 1000);
    return () => clearTimeout(timer);
  }, [secondsLeft]);

  function handleResend() {
    if (secondsLeft > 0) return;
    // Không có endpoint "gửi lại MFA OTP" riêng — backend tự gửi lại mã khi gọi login() lần
    // nữa (AuthService.login gọi emailOtpService.sendMfaOtp mỗi lần mfaEnabled=true), nên
    // "gửi lại" ở đây gọi lại chính login() bằng email đã có (cần cả password — không lưu
    // password ở FE vì lý do bảo mật, nên không tự resend được, phải quay lại Login nhập lại).
    showToast('Vui lòng quay lại màn Đăng nhập và đăng nhập lại để nhận mã mới', 'info');
  }

  async function handleConfirm() {
    setSubmitting(true);
    try {
      await verifyMfa(email, otp);
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
      <StackHeader title="Xác Thực 2 Lớp" onBack={() => navigation.goBack()} />
      <ScrollView className="flex-1 px-gutter-mobile" contentContainerStyle={{ paddingVertical: 16, gap: 16 }}>
        <View className="rounded-md p-3.5 flex-row items-center gap-2.5 bg-surface-container-high/60">
          <View className="w-8 h-8 rounded-full items-center justify-center" style={{ backgroundColor: 'rgba(15,44,89,0.1)' }}>
            <Icon name="shield_lock" size={18} color={colors.primaryContainer} />
          </View>
          <View className="flex-1">
            <Text className="font-label-caption text-label-caption text-secondary font-semibold uppercase tracking-wider">
              Xác thực 2 lớp (MFA)
            </Text>
            <Text className="font-body-sm text-body-sm text-on-surface-variant">
              Tài khoản của bạn đã bật bảo vệ 2 lớp qua email
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
              Mã OTP 6 chữ số vừa được gửi đến email <Text className="font-semibold text-on-surface">{email}</Text>
            </>
          }
        />

        <Button label="Xác nhận đăng nhập" onPress={handleConfirm} disabled={otp.length !== OTP_LENGTH} loading={submitting} />
      </ScrollView>
    </SafeAreaView>
  );
}
