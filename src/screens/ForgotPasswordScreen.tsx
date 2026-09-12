import { useEffect, useMemo, useState } from 'react';
import { Pressable, ScrollView, Text, TextInput, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { CommonActions, useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { Button } from '../components/Button';
import { Icon, IconName } from '../components/Icon';
import { OtpEntryPanel } from '../components/OtpEntryPanel';
import { StackHeader } from '../components/StackHeader';
import { useToast } from '../hooks/useToast';
import { colors } from '../theme/tokens';
import { RootStackParamList } from '../navigation/RootNavigator';

type Step = 1 | 2 | 3;
type StrengthScore = 0 | 1 | 2 | 3 | 4;

const OTP_LENGTH = 6;
const RESEND_SECONDS = 45;

const PHONE_PATTERN = /(84|0[3|5|7|8|9])+([0-9]{8})\b/;
const EMAIL_PATTERN = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;

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

function validateIdentity(raw: string): { valid: boolean; type: 'phone' | 'email' | 'unknown'; label: string } {
  const clean = raw.trim();
  if (PHONE_PATTERN.test(clean.replace(/\s+/g, ''))) {
    return { valid: true, type: 'phone', label: 'Số điện thoại hợp lệ' };
  }
  if (EMAIL_PATTERN.test(clean)) {
    return { valid: true, type: 'email', label: 'Email hợp lệ' };
  }
  return { valid: false, type: 'unknown', label: '' };
}

const STEPS: { n: Step; label: string }[] = [
  { n: 1, label: 'Thông tin' },
  { n: 2, label: 'Mã OTP' },
  { n: 3, label: 'Mật khẩu mới' },
];

function StepWizard({ step }: { step: Step }) {
  return (
    <View className="bg-surface-container-lowest p-space-md rounded-xl shadow-sm">
      <View className="flex-row items-start">
        {STEPS.map((s, i) => (
          <View
            key={s.n}
            className="flex-row items-start"
            style={i < STEPS.length - 1 ? { flexGrow: 1, flexShrink: 1, flexBasis: 0 } : undefined}
          >
            <View className="items-center gap-space-xs" style={{ width: 72 }}>
              <View
                className="w-8 h-8 rounded-full items-center justify-center"
                style={{ backgroundColor: s.n <= step ? colors.primary : colors.surfaceContainer }}
              >
                <Text
                  className="font-label-sm text-label-sm font-semibold"
                  style={{ color: s.n <= step ? colors.onPrimary : colors.outline }}
                >
                  {s.n}
                </Text>
              </View>
              <Text
                className="font-label-caption text-label-caption text-center font-semibold"
                style={{ color: s.n <= step ? colors.primary : colors.outline }}
              >
                {s.label}
              </Text>
            </View>
            {i < STEPS.length - 1 && (
              <View
                style={{
                  flexGrow: 1,
                  flexShrink: 1,
                  flexBasis: 0,
                  height: 2,
                  marginTop: 15,
                  backgroundColor: s.n < step ? colors.primaryContainer : colors.surfaceContainer,
                }}
              />
            )}
          </View>
        ))}
      </View>
    </View>
  );
}

export function ForgotPasswordScreen() {
  const navigation = useNavigation<NativeStackNavigationProp<RootStackParamList>>();
  const { showToast } = useToast();

  const [step, setStep] = useState<Step>(1);

  // Step 1: định danh (SĐT/Email)
  const [identity, setIdentity] = useState('');
  const [showIdentityError, setShowIdentityError] = useState(false);
  const [isSendingOtp, setIsSendingOtp] = useState(false);
  const identityCheck = useMemo(() => validateIdentity(identity), [identity]);
  const prefixIcon: IconName =
    identityCheck.valid && identityCheck.type === 'phone'
      ? 'phone_iphone'
      : identityCheck.valid && identityCheck.type === 'email'
        ? 'mail'
        : identity.trim().length > 3
          ? /^[0-9+]/.test(identity)
            ? 'phone_iphone'
            : 'alternate_email'
          : 'contact_mail';

  // Step 2: OTP
  const [otp, setOtp] = useState('');
  const [secondsLeft, setSecondsLeft] = useState(RESEND_SECONDS);

  useEffect(() => {
    if (step !== 2 || secondsLeft <= 0) return;
    const timer = setTimeout(() => setSecondsLeft((s) => s - 1), 1000);
    return () => clearTimeout(timer);
  }, [step, secondsLeft]);

  function handleResendOtp() {
    if (secondsLeft > 0) return;
    setSecondsLeft(RESEND_SECONDS);
    setOtp('');
    showToast('Đã gửi lại mã OTP', 'success');
  }

  // Step 3: mật khẩu mới
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const strength = useMemo(() => evaluatePassword(newPassword), [newPassword]);
  const passwordsMatch = confirmPassword.length > 0 && newPassword === confirmPassword;

  function comingSoon() {
    showToast('Tính năng sắp ra mắt', 'info');
  }

  function handleSubmitIdentity() {
    if (!identityCheck.valid) {
      setShowIdentityError(true);
      return;
    }
    setShowIdentityError(false);
    setIsSendingOtp(true);
    setTimeout(() => {
      setIsSendingOtp(false);
      setStep(2);
    }, 900);
  }

  function handleConfirmOtp() {
    // Demo: mọi mã 6 số đều được coi là hợp lệ (chưa nối API xác thực OTP thật).
    setStep(3);
  }

  function handleResetPassword() {
    if (!newPassword || !confirmPassword) {
      showToast('Vui lòng nhập đầy đủ mật khẩu mới', 'danger');
      return;
    }
    if (!passwordsMatch) {
      showToast('Mật khẩu xác nhận không khớp', 'danger');
      return;
    }
    showToast('Đặt lại mật khẩu thành công, vui lòng đăng nhập lại', 'success');
    navigation.dispatch(CommonActions.reset({ index: 0, routes: [{ name: 'Login' }] }));
  }

  const titleByStep: Record<Step, string> = {
    1: 'Quên Mật Khẩu',
    2: 'Xác Thực OTP',
    3: 'Mật Khẩu Mới',
  };

  return (
    <SafeAreaView className="flex-1 bg-background" edges={['top']}>
      <StackHeader
        title={titleByStep[step]}
        onBack={() => (step === 1 ? navigation.goBack() : setStep((s) => (s - 1) as Step))}
        avatarName="Nguyễn Văn An"
        rightAccessory={
          <Pressable onPress={comingSoon} hitSlop={8}>
            <Icon name="help_outline" size={22} color={colors.onSurfaceVariant} />
          </Pressable>
        }
      />
      <ScrollView className="flex-1 px-gutter-mobile" contentContainerStyle={{ paddingVertical: 16, gap: 16 }}>
        <StepWizard step={step} />

        {step === 1 && (
          <>
            <View className="bg-surface-container-lowest rounded-xl p-space-lg gap-space-lg">
              <View className="items-center gap-space-sm">
                <View style={{ position: 'relative' }}>
                  <View className="w-16 h-16 rounded-full bg-surface-container-low items-center justify-center shadow-sm">
                    <Icon name="lock_reset" size={32} color={colors.primary} />
                  </View>
                  <View
                    className="w-6 h-6 rounded-full bg-secondary items-center justify-center shadow-sm"
                    style={{ position: 'absolute', bottom: -4, right: -4 }}
                  >
                    <Icon name="verified_user" size={14} color={colors.onSecondary} />
                  </View>
                </View>
                <View className="items-center gap-space-xs mt-space-xs">
                  <Text className="font-headline-md text-headline-md text-primary font-bold text-center">
                    Quên mật khẩu đăng nhập?
                  </Text>
                  <Text className="font-body-md text-body-md text-on-surface-variant text-center leading-relaxed">
                    Vui lòng nhập số điện thoại hoặc email đã đăng ký tài khoản PMPay để nhận mã xác thực OTP khôi
                    phục.
                  </Text>
                </View>
              </View>

              <View className="gap-space-xs">
                <View className="flex-row justify-between items-center">
                  <Text className="font-label-sm text-label-sm text-primary font-semibold">
                    Số điện thoại hoặc Email
                  </Text>
                  {identityCheck.valid && (
                    <View
                      className="px-2 py-0.5 rounded-full"
                      style={{ backgroundColor: 'rgba(126,246,190,0.25)' }}
                    >
                      <Text className="font-label-caption text-label-caption text-secondary font-medium">
                        {identityCheck.label}
                      </Text>
                    </View>
                  )}
                </View>
                <View
                  className="flex-row items-center h-12 rounded-xl bg-surface-container-low px-3.5"
                  style={{
                    borderWidth: showIdentityError ? 1.5 : 0,
                    borderColor: colors.error,
                  }}
                >
                  <Icon
                    name={prefixIcon}
                    size={20}
                    color={identityCheck.valid ? colors.secondary : colors.outline}
                  />
                  <TextInput
                    value={identity}
                    onChangeText={(text) => {
                      setIdentity(text);
                      setShowIdentityError(false);
                    }}
                    placeholder="Ví dụ: 0912 345 678 hoặc user@pmpay.vn"
                    placeholderTextColor={colors.outline}
                    autoCapitalize="none"
                    className="flex-1 h-12 ml-2.5 font-body-md text-body-md text-on-surface"
                  />
                  {identity.length > 0 && (
                    <Pressable onPress={() => setIdentity('')} hitSlop={8}>
                      <Icon name="cancel" size={18} color={colors.outline} />
                    </Pressable>
                  )}
                </View>
                {showIdentityError && (
                  <View className="flex-row items-center gap-1 mt-0.5">
                    <Icon name="error" size={14} color={colors.error} />
                    <Text className="font-body-sm text-body-sm" style={{ color: colors.error }}>
                      Vui lòng nhập đúng số điện thoại (10 số) hoặc địa chỉ email hợp lệ.
                    </Text>
                  </View>
                )}
              </View>

              <View className="bg-surface-container-low p-space-md rounded-xl flex-row items-start gap-space-sm">
                <Icon name="shield" size={20} color={colors.secondary} />
                <Text className="flex-1 font-body-sm text-body-sm text-on-surface-variant leading-normal">
                  <Text className="font-semibold text-on-surface">Lưu ý an ninh: </Text>
                  PMPay cam kết không bao giờ yêu cầu bạn cung cấp mật khẩu, mã PIN hoặc mã OTP qua điện thoại hay
                  tin nhắn.
                </Text>
              </View>

              <Button
                label={isSendingOtp ? 'Đang gửi mã OTP...' : 'Gửi mã xác thực OTP'}
                onPress={handleSubmitIdentity}
                loading={isSendingOtp}
              />
            </View>

            <View className="items-center gap-space-md">
              <View className="flex-row items-center gap-space-xs bg-surface-container-lowest/60 px-space-md py-2 rounded-full shadow-sm">
                <Icon name="support_agent" size={18} color={colors.outline} />
                <Text className="font-body-sm text-body-sm text-on-surface-variant">Gặp sự cố khi nhận mã?</Text>
                <Text className="font-body-sm text-body-sm text-primary font-semibold">Tổng đài 1900 8899</Text>
              </View>
              <Pressable
                onPress={() => navigation.goBack()}
                className="flex-row items-center gap-1"
              >
                <Icon name="keyboard_backspace" size={18} color={colors.primary} />
                <Text className="font-label-md text-label-md text-primary font-semibold">Quay lại Đăng nhập</Text>
              </Pressable>
              <View className="flex-row items-center justify-center gap-space-lg" style={{ opacity: 0.75 }}>
                <View className="flex-row items-center gap-1">
                  <Icon name="verified" size={16} color={colors.secondary} />
                  <Text className="font-label-caption text-label-caption text-on-surface-variant uppercase tracking-wider">
                    PCI-DSS Level 1
                  </Text>
                </View>
                <View className="w-1 h-1 rounded-full bg-outline-variant" />
                <View className="flex-row items-center gap-1">
                  <Icon name="enhanced_encryption" size={16} color={colors.primary} />
                  <Text className="font-label-caption text-label-caption text-on-surface-variant uppercase tracking-wider">
                    SHA-256 Bit
                  </Text>
                </View>
              </View>
            </View>
          </>
        )}

        {step === 2 && (
          <>
            <OtpEntryPanel
              value={otp}
              onChange={setOtp}
              secondsLeft={secondsLeft}
              onResend={handleResendOtp}
              description={
                <>
                  Mã OTP 6 chữ số vừa được gửi đến{' '}
                  <Text className="font-semibold text-on-surface">{identity || 'liên hệ đã đăng ký'}</Text>
                </>
              }
            />
            <Button label="Xác nhận mã" onPress={handleConfirmOtp} disabled={otp.length !== OTP_LENGTH} />
          </>
        )}

        {step === 3 && (
          <View className="bg-surface-container-lowest rounded-xl p-space-lg gap-space-md">
            <View className="items-center gap-space-xs pb-space-xs">
              <View className="w-14 h-14 rounded-full bg-surface-container-low items-center justify-center">
                <Icon name="lock_reset" size={28} color={colors.primary} />
              </View>
              <Text className="font-headline-md text-headline-md text-primary font-bold text-center">
                Đặt mật khẩu mới
              </Text>
              <Text className="font-body-sm text-body-sm text-on-surface-variant text-center">
                Mật khẩu mới cần khác với mật khẩu cũ để đảm bảo an toàn tài khoản.
              </Text>
            </View>

            <View>
              <Text className="font-label-md text-label-md text-primary mb-space-xs">Mật khẩu mới *</Text>
              <View className="flex-row items-center h-12 rounded-xl bg-surface-container-low px-3.5">
                <Icon name="lock" size={20} color={colors.outline} />
                <TextInput
                  value={newPassword}
                  onChangeText={setNewPassword}
                  placeholder="••••••••"
                  placeholderTextColor={colors.outline}
                  secureTextEntry
                  className="flex-1 h-12 ml-2.5 font-body-md text-body-md text-on-surface"
                />
              </View>
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
              <Text className="font-label-md text-label-md text-primary mb-space-xs">Xác nhận mật khẩu mới *</Text>
              <View className="flex-row items-center h-12 rounded-xl bg-surface-container-low px-3.5">
                <Icon name="lock_reset" size={20} color={colors.outline} />
                <TextInput
                  value={confirmPassword}
                  onChangeText={setConfirmPassword}
                  placeholder="••••••••"
                  placeholderTextColor={colors.outline}
                  secureTextEntry
                  className="flex-1 h-12 ml-2.5 font-body-md text-body-md text-on-surface"
                />
                {passwordsMatch && <Icon name="check_circle" size={20} color={colors.secondary} />}
              </View>
              {passwordsMatch && (
                <View className="flex-row items-center gap-1 mt-1">
                  <Icon name="check" size={14} color={colors.secondary} />
                  <Text className="font-label-caption text-label-caption text-secondary">
                    Mật khẩu xác nhận hoàn toàn trùng khớp
                  </Text>
                </View>
              )}
            </View>

            <Button label="Đặt lại mật khẩu" onPress={handleResetPassword} />
          </View>
        )}
      </ScrollView>
    </SafeAreaView>
  );
}
