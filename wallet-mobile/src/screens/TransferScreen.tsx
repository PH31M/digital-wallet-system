import { useState } from 'react';
import { Pressable, ScrollView, Text, TextInput, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { Icon } from '../components/Icon';
import { StackHeader } from '../components/StackHeader';
import { useAuth } from '../auth/AuthContext';
import { useToast } from '../hooks/useToast';
import { colors } from '../theme/tokens';
import { getApiErrorMessage } from '../api/apiError';
import { getCurrentUserWalletApi, lookupWalletApi, transferApi } from '../api/walletApi';
import { WalletLookupResponse } from '../types/api';
import { generateIdempotencyKey } from '../utils/idempotencyKey';
import { getAmountInWords } from '../utils/transferHelpers';
import { RootStackParamList } from '../navigation/RootNavigator';

const QUICK_AMOUNTS_ROW1 = [100_000, 200_000, 500_000];
const QUICK_AMOUNTS_ROW2 = [1_000_000, 2_000_000];

function StepBadge({ step }: { step: number }) {
  return (
    <View className="w-5 h-5 rounded-full bg-primary items-center justify-center">
      <Text className="font-label-caption text-label-caption text-on-primary font-bold">{step}</Text>
    </View>
  );
}

export function TransferScreen() {
  const navigation = useNavigation<NativeStackNavigationProp<RootStackParamList>>();
  const { user } = useAuth();
  const { showToast } = useToast();
  const queryClient = useQueryClient();
  const walletQuery = useQuery({ queryKey: ['wallet', 'me'], queryFn: getCurrentUserWalletApi });
  const balance = walletQuery.data?.balance ?? 0;

  const [recipientQuery, setRecipientQuery] = useState('');
  const [recipient, setRecipient] = useState<WalletLookupResponse | null>(null);
  const [isSearching, setIsSearching] = useState(false);
  const [amount, setAmount] = useState(1_200_000);
  const [note, setNote] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [idempotencyKey] = useState(() => generateIdempotencyKey());

  async function handleSearch() {
    const identifier = recipientQuery.trim();
    if (!identifier) {
      showToast('Vui lòng nhập email / số điện thoại người nhận', 'danger');
      return;
    }
    setIsSearching(true);
    try {
      const found = await lookupWalletApi(identifier);
      setRecipient(found);
    } catch (error) {
      setRecipient(null);
      showToast(getApiErrorMessage(error, 'Không tìm thấy người nhận'), 'danger');
    } finally {
      setIsSearching(false);
    }
  }

  function handleAmountChange(text: string) {
    const digitsOnly = text.replace(/\D/g, '');
    setAmount(digitsOnly ? parseInt(digitsOnly, 10) : 0);
  }

  async function handleConfirm() {
    if (!recipient) {
      showToast('Vui lòng tìm và chọn người nhận', 'danger');
      return;
    }
    if (amount <= 0) {
      showToast('Vui lòng nhập số tiền cần chuyển', 'danger');
      return;
    }
    if (amount > balance) {
      showToast('Số dư khả dụng không đủ', 'danger');
      return;
    }

    setIsSubmitting(true);
    try {
      const transaction = await transferApi(recipient.walletId, amount, idempotencyKey);
      queryClient.invalidateQueries({ queryKey: ['wallet', 'me'] });
      queryClient.invalidateQueries({ queryKey: ['transactions', 'history'] });

      if (transaction.status === 'PENDING_OTP_CONFIRMATION') {
        navigation.navigate('OtpConfirm', { transactionId: transaction.id, kind: 'transfer', amount });
        return;
      }

      navigation.navigate('TransactionResult', {
        kind: 'transfer',
        variant: transaction.status === 'COMPLETED' ? 'completed' : 'pending',
        amount,
        counterpartyLabel: 'Người nhận',
        counterpartyName: recipient.maskedName,
        counterpartyDetail: note || 'Chuyển tiền trong hệ thống PMPay',
        sourceLabel: 'Nguồn tiền',
        sourceDetail: 'Ví chính PMPay',
        note,
        transactionCode: transaction.referenceNumber,
        balanceAfter: transaction.status === 'COMPLETED' ? balance - amount : undefined,
      });
    } catch (error) {
      navigation.navigate('TransactionResult', {
        kind: 'transfer',
        variant: 'failed',
        amount,
        counterpartyLabel: 'Người nhận',
        counterpartyName: recipient.maskedName,
        counterpartyDetail: note || 'Chuyển tiền trong hệ thống PMPay',
        sourceLabel: 'Nguồn tiền',
        sourceDetail: 'Ví chính PMPay',
        note,
        failureReason: getApiErrorMessage(error, 'Chuyển tiền thất bại, vui lòng thử lại'),
      });
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <SafeAreaView className="flex-1 bg-background" edges={['top']}>
      <StackHeader title="Chuyển Tiền" onBack={() => navigation.goBack()} avatarName={user?.fullName ?? ''} />
      <ScrollView className="flex-1 px-gutter-mobile" contentContainerStyle={{ paddingVertical: 16, gap: 16 }}>
        {/* Transfer mode tabs */}
        <View className="flex-row p-1 bg-surface-container rounded-md">
          <View className="flex-1 py-2 rounded bg-primary items-center">
            <Text className="font-label-md text-label-md text-on-primary font-semibold">Trong hệ thống</Text>
          </View>
          <Pressable
            onPress={() => showToast('Chuyển liên ngân hàng sắp ra mắt', 'info')}
            className="flex-1 py-2 rounded items-center"
          >
            <Text className="font-label-md text-label-md text-on-surface-variant font-medium">
              Liên ngân hàng 24/7
            </Text>
          </Pressable>
        </View>

        {/* Source account */}
        <View className="bg-surface-container-low rounded-md p-space-md">
          <View className="flex-row items-center justify-between">
            <View className="flex-row items-center gap-space-sm">
              <View className="w-10 h-10 rounded-full bg-primary-container items-center justify-center">
                <Icon name="account_balance_wallet" size={20} color={colors.onPrimary} />
              </View>
              <View>
                <View className="flex-row items-center gap-1">
                  <Text className="font-headline-sm text-headline-sm text-primary font-semibold">
                    Ví chính PMPay
                  </Text>
                  <Icon name="verified" size={16} color={colors.secondary} />
                </View>
                <Text className="font-body-sm text-body-sm text-on-surface-variant">
                  Số dư khả dụng: <Text className="font-semibold text-primary">{balance.toLocaleString('vi-VN')} đ</Text>
                </Text>
              </View>
            </View>
            <View className="px-2.5 py-1 rounded-full" style={{ backgroundColor: 'rgba(0,108,73,0.1)' }}>
              <Text className="font-label-caption text-label-caption text-secondary uppercase tracking-wider font-semibold">
                Khả dụng
              </Text>
            </View>
          </View>
        </View>

        {/* Step 1: Recipient */}
        <View className="bg-surface-container-lowest rounded-md p-space-md gap-space-sm">
          <View className="flex-row items-center justify-between">
            <View className="flex-row items-center gap-1.5">
              <StepBadge step={1} />
              <Text className="font-headline-sm text-headline-sm text-primary font-semibold">Người nhận</Text>
            </View>
            <Pressable
              onPress={() => showToast('Danh bạ đã lưu sắp ra mắt', 'info')}
              className="flex-row items-center gap-0.5"
            >
              <Icon name="contacts" size={16} color={colors.secondary} />
              <Text className="font-label-sm text-label-sm text-secondary font-semibold">Danh bạ đã lưu</Text>
            </Pressable>
          </View>

          <View className="flex-row items-center gap-space-xs">
            <View className="flex-1 relative justify-center">
              <View className="absolute left-3 z-10">
                <Icon name="search" size={20} color={colors.outline} />
              </View>
              <TextInput
                value={recipientQuery}
                onChangeText={(text) => {
                  setRecipientQuery(text);
                  setRecipient(null);
                }}
                placeholder="Email / SĐT người nhận"
                placeholderTextColor={colors.outline}
                autoCapitalize="none"
                className="h-12 rounded-md bg-surface-container-low text-on-surface font-body-md text-body-md"
                style={{ paddingLeft: 40, paddingRight: 12 }}
              />
            </View>
            <Pressable
              onPress={handleSearch}
              disabled={isSearching}
              className="h-9 px-3.5 rounded bg-primary-container flex-row items-center gap-1"
              style={{ opacity: isSearching ? 0.7 : 1 }}
            >
              <Text className="font-label-md text-label-md text-on-primary font-semibold">
                {isSearching ? 'Đang tìm...' : 'Tìm'}
              </Text>
              {!isSearching && <Icon name="arrow_forward" size={16} color={colors.onPrimary} />}
            </Pressable>
          </View>

          {/* Tra cứu THẬT qua GET /wallets/lookup — không còn tìm theo "số ví" (mã ví không
              phải định danh tra cứu được, chỉ email/SĐT match đúng User.email/phoneNumber
              trong DB). Tên hiện ra đã bị che 1 phần (maskedName) — backend không trả tên đầy
              đủ/email/SĐT của người khác, đúng thiết kế bảo mật. */}
          {recipient && (
            <View className="rounded-md p-3.5 flex-row items-center justify-between" style={{ backgroundColor: 'rgba(242,243,255,0.8)' }}>
              <View className="flex-row items-center gap-3 flex-1 min-w-0">
                <View>
                  <View className="w-12 h-12 rounded-full bg-primary-fixed items-center justify-center">
                    <Text className="font-headline-sm text-headline-sm text-on-primary-fixed font-bold">
                      {recipient.maskedName.trim().charAt(0).toUpperCase()}
                    </Text>
                  </View>
                  <View className="absolute -bottom-0.5 -right-0.5 w-4 h-4 bg-secondary rounded-full items-center justify-center">
                    <Icon name="check" size={11} color={colors.onSecondary} />
                  </View>
                </View>
                <View className="shrink">
                  <Text numberOfLines={1} className="font-headline-sm text-headline-sm text-on-surface font-bold">
                    {recipient.maskedName}
                  </Text>
                  <Text numberOfLines={1} className="font-body-sm text-body-sm text-on-surface-variant">
                    {recipient.status === 'ACTIVE' ? 'Ví đang hoạt động' : recipient.status}
                  </Text>
                </View>
              </View>
              <View className="flex-row items-center gap-1 px-2 py-1 rounded-full bg-secondary-container">
                <Icon name="verified_user" size={13} color={colors.onSecondaryContainer} />
                <Text className="font-label-caption text-label-caption text-on-secondary-container font-semibold">
                  Đã xác thực
                </Text>
              </View>
            </View>
          )}
        </View>

        {/* Step 2: Amount */}
        <View className="bg-surface-container-lowest rounded-md p-space-md gap-space-md">
          <View className="flex-row items-center justify-between">
            <View className="flex-row items-center gap-1.5">
              <StepBadge step={2} />
              <Text className="font-headline-sm text-headline-sm text-primary font-semibold">Số tiền chuyển</Text>
            </View>
            <View className="flex-row items-center gap-0.5">
              <Icon name="electric_bolt" size={15} color={colors.secondary} />
              <Text className="font-label-sm text-label-sm text-secondary font-medium">Miễn phí phí sàn</Text>
            </View>
          </View>

          <View className="bg-surface-container-low rounded-md p-4 items-center">
            <View className="flex-row items-baseline justify-center gap-1.5">
              <Text className="font-headline-lg text-headline-lg text-on-surface-variant font-semibold">₫</Text>
              <TextInput
                value={amount ? amount.toLocaleString('vi-VN') : ''}
                onChangeText={handleAmountChange}
                keyboardType="number-pad"
                placeholder="0"
                placeholderTextColor={colors.outline}
                className="font-headline-xl text-headline-xl text-primary font-bold text-center"
                style={{ minWidth: 140 }}
              />
            </View>
            <Text className="font-body-sm text-body-sm text-on-surface-variant italic mt-1">
              {amount > 0 ? getAmountInWords(amount) : 'Nhập số tiền cần chuyển'}
            </Text>
          </View>

          <View className="gap-1.5">
            <View className="flex-row gap-2">
              {QUICK_AMOUNTS_ROW1.map((value) => (
                <Pressable
                  key={value}
                  onPress={() => setAmount(value)}
                  className="flex-1 py-2 rounded bg-surface-container-low items-center"
                >
                  <Text className="font-label-sm text-label-sm text-on-surface font-semibold">
                    {value.toLocaleString('vi-VN')} đ
                  </Text>
                </Pressable>
              ))}
            </View>
            <View className="flex-row gap-2">
              {QUICK_AMOUNTS_ROW2.map((value) => (
                <Pressable
                  key={value}
                  onPress={() => setAmount(value)}
                  className="flex-1 py-2 rounded bg-surface-container-low items-center"
                >
                  <Text className="font-label-sm text-label-sm text-on-surface font-semibold">
                    {value.toLocaleString('vi-VN')} đ
                  </Text>
                </Pressable>
              ))}
            </View>
          </View>

          <View className="gap-1.5">
            <View className="flex-row items-center justify-between">
              <Text className="font-label-md text-label-md text-on-surface font-medium">Nội dung chuyển tiền</Text>
              <Text className="font-label-caption text-label-caption text-outline">{note.length}/1000</Text>
            </View>
            <TextInput
              value={note}
              onChangeText={(text) => setNote(text.slice(0, 1000))}
              placeholder="Ghi chú (tối đa 1000 ký tự, không dùng mã HTML)"
              placeholderTextColor={colors.outline}
              multiline
              numberOfLines={2}
              className="p-3 rounded-md bg-surface-container-low text-on-surface font-body-md text-body-md"
              style={{ textAlignVertical: 'top', minHeight: 56 }}
            />
          </View>

          <View className="rounded-md p-3 gap-2" style={{ backgroundColor: 'rgba(242,243,255,0.6)' }}>
            <View className="flex-row justify-between items-center">
              <Text className="font-body-sm text-body-sm text-on-surface-variant">Phí giao dịch</Text>
              <View className="flex-row items-center gap-1">
                <Text className="font-body-sm text-body-sm text-outline" style={{ textDecorationLine: 'line-through' }}>
                  5.500 đ
                </Text>
                <Text className="font-body-sm text-body-sm text-secondary font-semibold">0 đ (Miễn phí)</Text>
              </View>
            </View>
            <View className="flex-row justify-between items-center">
              <Text className="font-body-sm text-body-sm text-on-surface-variant">Thời gian thực hiện</Text>
              <Text className="font-body-sm text-body-sm text-on-surface font-medium">Tức thì (Realtime 24/7)</Text>
            </View>
          </View>
        </View>

        {/* Trust banner */}
        <View className="rounded-md p-3 flex-row items-center gap-3" style={{ backgroundColor: 'rgba(126,246,190,0.3)' }}>
          <View className="w-8 h-8 rounded-md items-center justify-center" style={{ backgroundColor: 'rgba(0,108,73,0.15)' }}>
            <Icon name="shield" size={20} color={colors.secondary} />
          </View>
          <Text className="flex-1 font-label-caption text-label-caption text-on-surface-variant">
            Giao dịch được mã hoá 256-bit và bảo mật bởi <Text className="text-primary font-semibold">PMPay Trust Engine</Text>
          </Text>
          <Icon name="lock" size={18} color={colors.secondary} />
        </View>

        {/* Submit */}
        <Pressable
          onPress={handleConfirm}
          disabled={isSubmitting}
          className="h-12 rounded-md bg-primary items-center justify-center flex-row gap-2"
          style={{ opacity: isSubmitting ? 0.8 : 1 }}
        >
          <Text className="font-headline-sm text-headline-sm text-on-primary font-bold">
            {isSubmitting ? 'Đang xử lý...' : 'Xác nhận chuyển tiền'}
          </Text>
          {!isSubmitting && <Icon name="check_circle" size={20} color={colors.onPrimary} />}
        </Pressable>
      </ScrollView>
    </SafeAreaView>
  );
}
