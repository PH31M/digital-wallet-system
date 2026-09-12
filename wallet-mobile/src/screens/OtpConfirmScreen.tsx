import { useEffect, useState } from 'react';
import { ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useNavigation, useRoute, RouteProp } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { Button } from '../components/Button';
import { Icon } from '../components/Icon';
import { OtpEntryPanel } from '../components/OtpEntryPanel';
import { StackHeader } from '../components/StackHeader';
import { useToast } from '../hooks/useToast';
import { colors } from '../theme/tokens';
import { formatCurrency } from '../utils/formatCurrency';
import { buildTransferResultParams } from '../utils/transferHelpers';
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

  function handleResend() {
    if (secondsLeft > 0) return;
    setSecondsLeft(RESEND_SECONDS);
    setOtp('');
    showToast('Đã gửi lại mã OTP', 'success');
  }

  function handleConfirm() {
    // Demo: mọi mã 6 số đều được coi là hợp lệ (chưa nối API xác thực OTP thật).
    navigation.navigate(
      'TransactionResult',
      buildTransferResultParams(draft, 'completed', mockWallet.balance - draft.amount),
    );
  }

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

        <OtpEntryPanel
          value={otp}
          onChange={setOtp}
          secondsLeft={secondsLeft}
          onResend={handleResend}
          description={
            <>
              Mã OTP 6 chữ số vừa được gửi đến số điện thoại{' '}
              <Text className="font-semibold text-on-surface">+84 98***4321</Text> qua tin nhắn SMS
            </>
          }
        />

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
