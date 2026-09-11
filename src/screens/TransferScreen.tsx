import { useState } from 'react';
import { Pressable, ScrollView, Text, TextInput, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { Icon } from '../components/Icon';
import { StackHeader } from '../components/StackHeader';
import { useToast } from '../hooks/useToast';
import { colors } from '../theme/tokens';
import { mockWallet } from '../api/mock/homeMock';
import { mockRecipient } from '../api/mock/transferMock';
import { generateTransactionCode, getAmountInWords } from '../utils/transferHelpers';
import { RootStackParamList } from '../navigation/RootNavigator';

const OTP_REQUIRED_AMOUNT = 5_000_000; // khớp wallet.otp.required-amount (mặc định) bên backend

const QUICK_AMOUNTS_ROW1 = [100_000, 200_000, 500_000];
const QUICK_AMOUNTS_ROW2 = [1_000_000, 2_000_000];

type ConfirmState = 'idle' | 'loading' | 'biometric';

function StepBadge({ step }: { step: number }) {
  return (
    <View className="w-5 h-5 rounded-full bg-primary items-center justify-center">
      <Text className="font-label-caption text-label-caption text-on-primary font-bold">{step}</Text>
    </View>
  );
}

export function TransferScreen() {
  const navigation = useNavigation<NativeStackNavigationProp<RootStackParamList>>();
  const { showToast } = useToast();

  const [recipientQuery, setRecipientQuery] = useState('');
  const [recipient, setRecipient] = useState<typeof mockRecipient | null>(null);
  const [amount, setAmount] = useState(1_200_000);
  const [note, setNote] = useState('');
  const [confirmState, setConfirmState] = useState<ConfirmState>('idle');

  function handleSearch() {
    if (!recipientQuery.trim()) {
      showToast('Vui lòng nhập số ví / email / số điện thoại', 'danger');
      return;
    }
    setRecipient(mockRecipient);
  }

  function handleAmountChange(text: string) {
    const digitsOnly = text.replace(/\D/g, '');
    setAmount(digitsOnly ? parseInt(digitsOnly, 10) : 0);
  }

  function handleConfirm() {
    if (!recipient) {
      showToast('Vui lòng chọn người nhận', 'danger');
      return;
    }
    if (amount <= 0) {
      showToast('Vui lòng nhập số tiền cần chuyển', 'danger');
      return;
    }

    setConfirmState('loading');
    setTimeout(() => {
      setConfirmState('biometric');
      setTimeout(() => {
        setConfirmState('idle');
        const draft = {
          amount,
          recipientName: recipient.name,
          recipientWalletCode: recipient.walletCode,
          recipientWalletLabel: recipient.walletLabel,
          note,
        };
        if (amount >= OTP_REQUIRED_AMOUNT) {
          navigation.navigate('OtpConfirm', draft);
        } else {
          navigation.navigate('TransactionResult', {
            ...draft,
            variant: 'completed',
            transactionCode: generateTransactionCode(),
            balanceAfter: mockWallet.balance - amount,
          });
        }
      }, 600);
    }, 600);
  }

  return (
    <SafeAreaView className="flex-1 bg-background" edges={['top']}>
      <StackHeader title="Chuyển Tiền" onBack={() => navigation.goBack()} avatarName="Nguyễn Văn An" />
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
                  Số dư khả dụng: <Text className="font-semibold text-primary">{mockWallet.balance.toLocaleString('vi-VN')} đ</Text>
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
                onChangeText={setRecipientQuery}
                placeholder="Số ví / Email / SĐT người nhận"
                placeholderTextColor={colors.outline}
                className="h-12 rounded-md bg-surface-container-low text-on-surface font-body-md text-body-md"
                style={{ paddingLeft: 40, paddingRight: 12 }}
              />
            </View>
            <Pressable onPress={handleSearch} className="h-9 px-3.5 rounded bg-primary-container flex-row items-center gap-1">
              <Text className="font-label-md text-label-md text-on-primary font-semibold">Tìm</Text>
              <Icon name="arrow_forward" size={16} color={colors.onPrimary} />
            </Pressable>
          </View>

          {recipient && (
            <View className="rounded-md p-3.5 flex-row items-center justify-between" style={{ backgroundColor: 'rgba(242,243,255,0.8)' }}>
              <View className="flex-row items-center gap-3 flex-1 min-w-0">
                <View>
                  <View className="w-12 h-12 rounded-full bg-primary-fixed items-center justify-center">
                    <Text className="font-headline-sm text-headline-sm text-on-primary-fixed font-bold">
                      {recipient.initials}
                    </Text>
                  </View>
                  <View className="absolute -bottom-0.5 -right-0.5 w-4 h-4 bg-secondary rounded-full items-center justify-center">
                    <Icon name="check" size={11} color={colors.onSecondary} />
                  </View>
                </View>
                <View className="shrink">
                  <Text numberOfLines={1} className="font-headline-sm text-headline-sm text-on-surface font-bold">
                    {recipient.name}
                  </Text>
                  <Text numberOfLines={1} className="font-body-sm text-body-sm text-on-surface-variant">
                    {recipient.walletCode} • {recipient.walletLabel}
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
          disabled={confirmState !== 'idle'}
          className="h-12 rounded-md bg-primary items-center justify-center flex-row gap-2"
          style={{ opacity: confirmState !== 'idle' ? 0.8 : 1 }}
        >
          {confirmState === 'idle' && (
            <>
              <Text className="font-headline-sm text-headline-sm text-on-primary font-bold">Xác nhận chuyển tiền</Text>
              <Icon name="check_circle" size={20} color={colors.onPrimary} />
            </>
          )}
          {confirmState === 'loading' && (
            <Text className="font-headline-sm text-headline-sm text-on-primary font-bold">Đang chuẩn bị xác thực...</Text>
          )}
          {confirmState === 'biometric' && (
            <>
              <Icon name="fingerprint" size={20} color={colors.onPrimary} />
              <Text className="font-headline-sm text-headline-sm text-on-primary font-bold">Xác thực vân tay / FaceID</Text>
            </>
          )}
        </Pressable>
      </ScrollView>
    </SafeAreaView>
  );
}
