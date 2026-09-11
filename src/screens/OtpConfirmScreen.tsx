import { useEffect, useState } from 'react';
import { ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useNavigation, useRoute, RouteProp } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { Button } from '../components/Button';
import { Icon } from '../components/Icon';
import { NumericKeypad } from '../components/NumericKeypad';
import { StackHeader } from '../components/StackHeader';
import { useToast } from '../hooks/useToast';
import { colors } from '../theme/tokens';
import { formatCurrency } from '../utils/formatCurrency';
import { generateTransactionCode } from '../utils/transferHelpers';
import { mockWallet } from '../api/mock/homeMock';
import { RootStackParamList } from '../navigation/RootNavigator';

const OTP_LENGTH = 6;
const RESEND_SECONDS = 45;
const OTP_REQUIRED_AMOUNT = 5_000_000;

export function OtpConfirmScreen() {
  const navigation = useNavigation<NativeStackNavigationProp<RootStackParamList>>();
  const route = useRoute<RouteProp<RootStackParamList, 'OtpConfirm'>>();
  const { showToast } = useToast();
  const draft = route.params;

  const [otp, setOtp] = useState('');
  const [secondsLeft, setSecondsLeft] = useState(RESEND_SECONDS);

  useEffect(() => {
    if (secondsLeft <= 0) return;
    const timer = setTimeout(() => setSecondsLeft((s) => s - 1), 1000);
    return () => clearTimeout(timer);
  }, [secondsLeft]);

  function handleDigit(digit: string) {
    if (otp.length >= OTP_LENGTH) return;
    setOtp((prev) => prev + digit);
  }

  function handleBackspace() {
    setOtp((prev) => prev.slice(0, -1));
  }

  function handleResend() {
    if (secondsLeft > 0) return;
    setSecondsLeft(RESEND_SECONDS);
    setOtp('');
    showToast('Đã gửi lại mã OTP', 'success');
  }

  function handleConfirm() {
    // Demo: mọi mã 6 số đều được coi là hợp lệ (chưa nối API xác thực OTP thật).
    navigation.navigate('TransactionResult', {
      ...draft,
      variant: 'completed',
      transactionCode: generateTransactionCode(),
      balanceAfter: mockWallet.balance - draft.amount,
    });
  }

  const countdownLabel = `00:${secondsLeft.toString().padStart(2, '0')}`;

  return (
    <SafeAreaView className="flex-1 bg-background" edges={['top']}>
      <StackHeader
        title="Xác Thực Bảo Mật Otp"
        onBack={() => navigation.goBack()}
        avatarName="Nguyễn Văn An"
        rightAccessory={<Icon name="help_outline" size={22} color={colors.onSurfaceVariant} />}
      />
      <ScrollView className="flex-1 px-gutter-mobile" contentContainerStyle={{ paddingVertical: 16, gap: 16 }}>
        {/* Security notice */}
        <View className="rounded-md p-3.5 flex-row items-center justify-between bg-surface-container-high/60">
          <View className="flex-row items-center gap-2.5 flex-1 min-w-0">
            <View className="w-8 h-8 rounded-full items-center justify-center" style={{ backgroundColor: 'rgba(15,44,89,0.1)' }}>
              <Icon name="verified_user" size={18} color={colors.primaryContainer} />
            </View>
            <View className="shrink">
              <Text className="font-label-caption text-label-caption text-secondary font-semibold uppercase tracking-wider">
                Xác thực 2 lớp nâng cao
              </Text>
              <Text numberOfLines={1} className="font-body-sm text-body-sm text-on-surface-variant">
                Giao dịch vượt hạn mức tiêu chuẩn {formatCurrency(OTP_REQUIRED_AMOUNT)}
              </Text>
            </View>
          </View>
          <View className="px-2 py-0.5 rounded-full bg-secondary-container">
            <Text className="font-label-caption text-label-caption text-on-secondary-container font-medium">
              2FA Active
            </Text>
          </View>
        </View>

        {/* Transaction summary */}
        <View className="bg-surface-container-lowest rounded-lg p-space-md gap-3.5">
          <View className="flex-row items-center justify-between pb-2.5">
            <View className="flex-row items-center gap-2">
              <Icon name="receipt_long" size={20} color={colors.primary} />
              <Text className="font-headline-sm text-headline-sm text-on-surface font-semibold">
                Tóm tắt giao dịch
              </Text>
            </View>
            <View className="px-2 py-0.5 rounded-full bg-surface-container-high">
              <Text className="font-label-caption text-label-caption text-on-surface-variant font-medium">
                Chờ xác nhận
              </Text>
            </View>
          </View>

          <View className="bg-surface-container-low rounded-md p-3.5 items-center">
            <Text className="font-label-caption text-label-caption text-on-surface-variant mb-0.5">
              Số tiền thanh toán
            </Text>
            <View className="flex-row items-baseline gap-1">
              <Text className="font-numeric-balance-mobile text-numeric-balance-mobile text-primary font-bold">
                -{draft.amount.toLocaleString('vi-VN')}
              </Text>
              <Text className="font-headline-sm text-headline-sm text-primary font-bold">₫</Text>
            </View>
          </View>

          <View className="gap-2 pt-1">
            <View className="flex-row items-center justify-between">
              <Text className="font-body-sm text-body-sm text-on-surface-variant">Người thụ hưởng</Text>
              <View className="items-end">
                <Text className="font-label-md text-label-md text-primary font-semibold">{draft.recipientName}</Text>
                <Text className="font-label-caption text-label-caption text-on-surface-variant">
                  Ví: {draft.recipientWalletCode}
                </Text>
              </View>
            </View>
            <View className="flex-row items-center justify-between">
              <Text className="font-body-sm text-body-sm text-on-surface-variant">Nguồn thanh toán</Text>
              <View className="flex-row items-center gap-1.5">
                <Icon name="account_balance_wallet" size={16} color={colors.secondary} />
                <Text className="font-label-md text-label-md text-on-surface font-medium">Ví chính (•••• 8829)</Text>
              </View>
            </View>
            <View className="flex-row items-center justify-between">
              <Text className="font-body-sm text-body-sm text-on-surface-variant">Phí giao dịch</Text>
              <Text className="font-label-md text-label-md text-secondary font-semibold">Miễn phí (0 ₫)</Text>
            </View>
          </View>
        </View>

        {/* OTP input */}
        <View className="bg-surface-container-lowest rounded-lg p-space-md items-center">
          <View className="w-14 h-14 rounded-full bg-surface-container-high items-center justify-center mb-3">
            <Icon name="shield_lock" size={30} color={colors.primaryContainer} />
          </View>
          <Text className="font-headline-md text-headline-md text-primary font-bold text-center">
            Nhập mã xác thực OTP
          </Text>
          <Text className="font-body-sm text-body-sm text-on-surface-variant mt-1 text-center" style={{ maxWidth: 280 }}>
            Mã OTP 6 chữ số vừa được gửi đến số điện thoại{' '}
            <Text className="font-semibold text-on-surface">+84 98***4321</Text> qua tin nhắn SMS
          </Text>

          <View className="flex-row items-center justify-center gap-2 mt-5 w-full">
            {Array.from({ length: OTP_LENGTH }, (_, i) => {
              const digit = otp[i];
              const isActive = i === otp.length;
              return (
                <View
                  key={i}
                  className="w-11 h-14 rounded-md items-center justify-center"
                  style={{
                    backgroundColor: colors.surfaceContainerLow,
                    borderWidth: isActive ? 2 : 0,
                    borderColor: colors.primary,
                  }}
                >
                  {digit ? (
                    <Text className="font-numeric-balance-mobile text-numeric-balance-mobile text-primary font-bold">
                      {digit}
                    </Text>
                  ) : (
                    <View className="w-2 h-2 rounded-full bg-outline-variant" />
                  )}
                </View>
              );
            })}
          </View>

          <View className="mt-4 items-center gap-1.5">
            <View className="flex-row items-center gap-1.5">
              <Icon name="schedule" size={16} color={colors.onSurfaceVariant} />
              <Text className="font-label-md text-label-md text-on-surface-variant">
                Gửi lại mã sau:{' '}
                <Text className="font-semibold text-primary font-numeric-ledger text-numeric-ledger">
                  {countdownLabel}
                </Text>
              </Text>
            </View>
            <View className="flex-row items-center gap-3 pt-1">
              <Text
                onPress={() => showToast('Tính năng sắp ra mắt', 'info')}
                className="font-label-sm text-label-sm text-primary font-semibold"
              >
                Gọi lấy mã thoại
              </Text>
              <View className="w-1 h-1 rounded-full bg-outline-variant" />
              <Text
                onPress={handleResend}
                className="font-label-sm text-label-sm"
                style={{ color: secondsLeft > 0 ? colors.outline : colors.primary }}
              >
                {secondsLeft > 0 ? 'Đổi số nhận OTP' : 'Gửi lại mã'}
              </Text>
            </View>
          </View>
        </View>

        {/* Numeric keypad */}
        <View className="bg-surface-container-lowest rounded-lg p-3">
          <NumericKeypad onDigitPress={handleDigit} onBackspace={handleBackspace} />
        </View>

        <Button
          label="Xác nhận giao dịch"
          onPress={handleConfirm}
          disabled={otp.length !== OTP_LENGTH}
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
