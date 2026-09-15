import { useEffect, useState } from 'react';
import { ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useQueryClient } from '@tanstack/react-query';
import { useNavigation, useRoute, RouteProp } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { Button } from '../components/Button';
import { Icon } from '../components/Icon';
import { OtpEntryPanel } from '../components/OtpEntryPanel';
import { StackHeader } from '../components/StackHeader';
import { useToast } from '../hooks/useToast';
import { getApiErrorMessage } from '../api/apiError';
import { confirmTransactionOtpApi } from '../api/walletApi';
import { colors } from '../theme/tokens';
import { formatCurrency } from '../utils/formatCurrency';
import { RootStackParamList, TransactionKind } from '../navigation/RootNavigator';

const OTP_LENGTH = 6;
const RESEND_SECONDS = 45;
const OTP_REQUIRED_AMOUNT = 5_000_000; // khớp wallet.otp.required-amount (mặc định) bên backend

const KIND_LABEL: Record<TransactionKind, string> = {
  transfer: 'Chuyển tiền',
  deposit: 'Nạp tiền',
  withdraw: 'Rút tiền',
};

/**
 * Dùng chung cho mọi giao dịch rơi vào PENDING_OTP_CONFIRMATION (rút/chuyển >= ngưỡng OTP) —
 * nhận `transactionId` THẬT (không phải draft dựng tay), gọi đúng
 * `POST /transactions/{id}/confirm-otp {otp}`. Không còn hiện tên/mã ví người nhận vì
 * `TransactionResponse` không có field đó (giống lý do TransactionHistoryItemResponse không
 * có tên đối tác — xem transactionDisplay.ts).
 */
export function OtpConfirmScreen() {
  const navigation = useNavigation<NativeStackNavigationProp<RootStackParamList>>();
  const route = useRoute<RouteProp<RootStackParamList, 'OtpConfirm'>>();
  const { showToast } = useToast();
  const queryClient = useQueryClient();
  const { transactionId, kind, amount } = route.params;

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
    // Không có endpoint "gửi lại OTP giao dịch" riêng — mã đã gửi lúc tạo giao dịch
    // (PENDING_OTP_CONFIRMATION), hết hạn thì phải huỷ và thử lại giao dịch từ đầu.
    showToast('Mã cũ có thể đã hết hạn — nếu không nhận được, vui lòng quay lại và thử lại giao dịch', 'info');
    setSecondsLeft(RESEND_SECONDS);
  }

  async function handleConfirm() {
    setSubmitting(true);
    try {
      const transaction = await confirmTransactionOtpApi(transactionId, otp);
      queryClient.invalidateQueries({ queryKey: ['wallet', 'me'] });
      queryClient.invalidateQueries({ queryKey: ['transactions', 'history'] });
      navigation.navigate('TransactionResult', {
        kind,
        variant: 'completed',
        amount: transaction.amount,
        counterpartyLabel: kind === 'withdraw' ? 'Tài khoản nhận tiền' : 'Người nhận',
        counterpartyName: 'Đã xác thực OTP',
        counterpartyDetail: transaction.referenceNumber,
        sourceLabel: kind === 'withdraw' ? 'Nguồn tiền trích nợ' : 'Nguồn tiền',
        sourceDetail: 'Ví chính PMPay',
        transactionCode: transaction.referenceNumber,
      });
    } catch (error) {
      showToast(getApiErrorMessage(error, 'Mã OTP không đúng hoặc đã hết hạn'), 'danger');
      setOtp('');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <SafeAreaView className="flex-1 bg-background" edges={['top']}>
      <StackHeader
        title="Xác Thực Bảo Mật Otp"
        onBack={() => navigation.goBack()}
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
                -{amount.toLocaleString('vi-VN')}
              </Text>
              <Text className="font-headline-sm text-headline-sm text-primary font-bold">₫</Text>
            </View>
          </View>

          <View className="gap-2 pt-1">
            <View className="flex-row items-center justify-between">
              <Text className="font-body-sm text-body-sm text-on-surface-variant">Loại giao dịch</Text>
              <Text className="font-label-md text-label-md text-primary font-semibold">{KIND_LABEL[kind]}</Text>
            </View>
            <View className="flex-row items-center justify-between">
              <Text className="font-body-sm text-body-sm text-on-surface-variant">Nguồn thanh toán</Text>
              <View className="flex-row items-center gap-1.5">
                <Icon name="account_balance_wallet" size={16} color={colors.secondary} />
                <Text className="font-label-md text-label-md text-on-surface font-medium">Ví chính PMPay</Text>
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
          description={<>Mã OTP 6 chữ số vừa được gửi đến <Text className="font-semibold text-on-surface">email của bạn</Text></>}
        />

        <Button
          label="Xác nhận giao dịch"
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
