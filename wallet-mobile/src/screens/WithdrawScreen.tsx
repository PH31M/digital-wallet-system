import { useState } from 'react';
import { Modal, Pressable, ScrollView, Text, TextInput, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { Icon } from '../components/Icon';
import { StackHeader } from '../components/StackHeader';
import { useToast } from '../hooks/useToast';
import { colors } from '../theme/tokens';
import { mockWallet } from '../api/mock/homeMock';
import { generateTransactionCode } from '../utils/transferHelpers';
import { RootStackParamList } from '../navigation/RootNavigator';

const MIN_AMOUNT = 50_000;
const DAILY_LIMIT = 50_000_000;
const DAILY_LIMIT_REMAINING = 48_000_000;
const FRAUD_REVIEW_THRESHOLD = 10_000_000;
const QUICK_AMOUNTS = [500_000, 1_000_000, 2_000_000, 5_000_000];

type BankAccount = {
  id: string;
  bankName: string;
  maskedNumber: string;
  ownerName: string;
};

const BANK_ACCOUNTS: BankAccount[] = [
  { id: 'vcb', bankName: 'Vietcombank', maskedNumber: '•••• 4567', ownerName: 'NGUYEN VAN AN' },
  { id: 'tcb', bankName: 'Techcombank', maskedNumber: '•••• 8821', ownerName: 'NGUYEN VAN AN' },
  { id: 'mb', bankName: 'MB Bank', maskedNumber: '•••• 1092', ownerName: 'NGUYEN VAN AN' },
];

export function WithdrawScreen() {
  const navigation = useNavigation<NativeStackNavigationProp<RootStackParamList>>();
  const { showToast } = useToast();

  const [selectedBankId, setSelectedBankId] = useState('vcb');
  const [amount, setAmount] = useState(2_000_000);
  const [isSheetOpen, setIsSheetOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const selectedBank = BANK_ACCOUNTS.find((b) => b.id === selectedBankId)!;

  function comingSoon() {
    showToast('Tính năng sắp ra mắt', 'info');
  }

  function handleAmountChange(text: string) {
    const digitsOnly = text.replace(/\D/g, '');
    setAmount(digitsOnly ? parseInt(digitsOnly, 10) : 0);
  }

  function handleSubmit() {
    if (amount < MIN_AMOUNT) {
      showToast(`Số tiền rút tối thiểu là ${MIN_AMOUNT.toLocaleString('vi-VN')} đ`, 'danger');
      return;
    }
    if (amount > DAILY_LIMIT) {
      showToast(`Số tiền vượt quá hạn mức tối đa trong ngày (${DAILY_LIMIT.toLocaleString('vi-VN')} đ)`, 'danger');
      return;
    }
    if (amount > mockWallet.balance) {
      showToast('Số dư khả dụng không đủ', 'danger');
      return;
    }

    setIsSubmitting(true);
    setTimeout(() => {
      setIsSubmitting(false);
      const isFraudReview = amount > FRAUD_REVIEW_THRESHOLD;
      navigation.navigate('TransactionResult', {
        kind: 'withdraw',
        variant: isFraudReview ? 'pending' : 'completed',
        amount,
        counterpartyLabel: 'Tài khoản nhận tiền',
        counterpartyName: selectedBank.bankName,
        counterpartyDetail: `${selectedBank.maskedNumber} • ${selectedBank.ownerName}`,
        sourceLabel: 'Nguồn tiền trích nợ',
        sourceDetail: 'Ví chính PMPay',
        transactionCode: generateTransactionCode(),
        balanceAfter: mockWallet.balance - amount,
      });
    }, 900);
  }

  return (
    <SafeAreaView className="flex-1 bg-background" edges={['top']}>
      <StackHeader
        title="Rút Tiền"
        onBack={() => navigation.goBack()}
        avatarName="Nguyễn Văn An"
        rightAccessory={
          <Pressable onPress={comingSoon} className="w-9 h-9 items-center justify-center">
            <Icon name="help_outline" size={22} color={colors.onSurfaceVariant} />
          </Pressable>
        }
      />
      <ScrollView className="flex-1 px-gutter-mobile" contentContainerStyle={{ paddingVertical: 16, gap: 16 }}>
        {/* Status banner */}
        <View className="flex-row items-center justify-between bg-surface-container-low px-space-md py-space-sm rounded-lg">
          <View className="flex-row items-center gap-space-xs">
            <Icon name="verified_user" size={18} color={colors.secondary} />
            <Text className="font-label-sm text-label-sm text-on-surface-variant">Kênh chuyển tiền Napas 247 tức thì</Text>
          </View>
          <Pressable onPress={comingSoon} className="flex-row items-center gap-0.5">
            <Text className="font-label-sm text-label-sm text-primary">Lịch sử rút</Text>
            <Icon name="chevron_right" size={16} color={colors.primary} />
          </Pressable>
        </View>

        {/* Source of funds */}
        <View className="bg-surface-container-lowest rounded-lg p-space-md">
          <View className="flex-row items-center justify-between">
            <View className="flex-row items-center gap-space-sm">
              <View className="w-10 h-10 rounded-md bg-primary items-center justify-center">
                <Icon name="account_balance_wallet" size={22} color={colors.onPrimary} />
              </View>
              <View>
                <Text className="font-label-caption text-label-caption text-on-surface-variant uppercase tracking-wider">
                  Nguồn tiền trích nợ
                </Text>
                <Text className="font-headline-sm text-headline-sm text-on-surface">Ví chính PMPay</Text>
              </View>
            </View>
            <Pressable
              onPress={() => setAmount(mockWallet.balance)}
              className="px-space-sm py-1.5 rounded-full bg-surface-container-high"
            >
              <Text className="font-label-sm text-label-sm text-primary">Rút tất cả</Text>
            </Pressable>
          </View>
          <View className="mt-space-md p-space-sm rounded-md bg-surface-container-low/60 flex-row items-center justify-between">
            <Text className="font-body-sm text-body-sm text-on-surface-variant">Số dư khả dụng</Text>
            <Text className="font-numeric-ledger text-numeric-ledger text-on-surface font-semibold">
              {mockWallet.balance.toLocaleString('vi-VN')} ₫
            </Text>
          </View>
        </View>

        {/* Destination account */}
        <View className="bg-surface-container-lowest rounded-lg p-space-md gap-space-sm">
          <View className="flex-row items-center justify-between">
            <View className="flex-row items-center gap-space-xs">
              <Text className="font-label-caption text-label-caption text-on-surface-variant uppercase tracking-wider">
                Tài khoản nhận tiền
              </Text>
              <Icon name="lock" size={15} color={colors.outline} />
            </View>
            <Pressable onPress={() => setIsSheetOpen(true)} className="flex-row items-center gap-0.5">
              <Text className="font-label-sm text-label-sm text-primary">Đổi tài khoản</Text>
              <Icon name="swap_horiz" size={16} color={colors.primary} />
            </Pressable>
          </View>
          <View className="flex-row items-start justify-between p-space-sm bg-surface-container-low rounded-lg">
            <View className="flex-row items-center gap-space-sm flex-1 min-w-0">
              <View className="w-11 h-11 rounded-md bg-surface-container-lowest items-center justify-center">
                <Icon name="account_balance" size={26} color={colors.secondary} />
              </View>
              <View className="shrink">
                <View className="flex-row items-center gap-1.5 flex-wrap">
                  <Text className="font-headline-sm text-headline-sm text-on-surface">{selectedBank.bankName}</Text>
                  <View className="flex-row items-center gap-1 px-2 py-0.5 rounded-full" style={{ backgroundColor: 'rgba(126,246,190,0.3)' }}>
                    <Icon name="verified" size={12} color={colors.secondary} />
                    <Text className="font-label-caption text-label-caption text-secondary">eKYC Chính chủ</Text>
                  </View>
                </View>
                <Text className="font-numeric-ledger text-numeric-ledger text-on-surface mt-0.5">
                  {selectedBank.maskedNumber}
                </Text>
              </View>
            </View>
          </View>
          <View className="flex-row items-center justify-between px-space-sm py-2 bg-surface-container-high/40 rounded-md">
            <View className="flex-row items-center gap-1.5">
              <Icon name="person" size={16} color={colors.outline} />
              <Text className="font-label-sm text-label-sm text-on-surface-variant">Người thụ hưởng:</Text>
              <Text className="font-label-sm text-label-sm text-on-surface font-semibold uppercase">
                {selectedBank.ownerName}
              </Text>
            </View>
            <Icon name="lock" size={18} color={colors.outline} />
          </View>
        </View>

        {/* Amount */}
        <View className="bg-surface-container-lowest rounded-lg p-space-md gap-space-md">
          <View className="flex-row items-center justify-between">
            <Text className="font-label-sm text-label-sm text-on-surface font-semibold">Số tiền muốn rút</Text>
            <Text className="font-label-caption text-label-caption text-on-surface-variant">
              Tối thiểu: {MIN_AMOUNT.toLocaleString('vi-VN')} ₫
            </Text>
          </View>
          <View className="relative flex-row items-center bg-surface-container-low rounded-lg px-space-md py-space-sm">
            <TextInput
              value={amount ? amount.toLocaleString('vi-VN') : ''}
              onChangeText={handleAmountChange}
              keyboardType="number-pad"
              placeholder="0"
              placeholderTextColor={colors.outline}
              className="flex-1 font-numeric-balance-mobile text-numeric-balance-mobile text-primary font-bold"
              style={{ paddingRight: 32 }}
            />
            <Text
              className="font-headline-lg text-headline-lg text-on-surface-variant font-medium"
              style={{ position: 'absolute', right: 16 }}
            >
              ₫
            </Text>
          </View>
          <View className="flex-row gap-space-xs">
            {QUICK_AMOUNTS.map((value) => {
              const isActive = amount === value;
              return (
                <Pressable
                  key={value}
                  onPress={() => setAmount(value)}
                  className="flex-1 py-2 px-1 rounded-md items-center"
                  style={{ backgroundColor: isActive ? colors.primary : colors.surfaceContainerLow }}
                >
                  <Text
                    className="font-label-sm text-label-sm"
                    style={{ color: isActive ? colors.onPrimary : colors.onSurface }}
                  >
                    {(value / 1000).toLocaleString('vi-VN')}k
                  </Text>
                </Pressable>
              );
            })}
          </View>
          <View className="flex-row items-start gap-2">
            <Icon name="info" size={18} color={colors.outlineVariant} />
            <Text className="flex-1 font-body-sm text-body-sm text-on-surface-variant">
              Hạn mức còn lại trong ngày: <Text className="text-on-surface font-semibold">{DAILY_LIMIT_REMAINING.toLocaleString('vi-VN')} ₫</Text> /{' '}
              {DAILY_LIMIT.toLocaleString('vi-VN')} ₫
            </Text>
          </View>
        </View>

        {/* Fee / policy */}
        <View className="bg-surface-container-lowest rounded-lg p-space-md gap-space-sm">
          <View className="flex-row items-center justify-between">
            <Text className="font-body-sm text-body-sm text-on-surface-variant">Phí rút tiền</Text>
            <View className="flex-row items-center gap-1.5">
              <Text className="font-body-sm text-body-sm text-secondary font-semibold">Miễn phí</Text>
              <Text className="font-body-sm text-body-sm text-on-surface-variant" style={{ fontSize: 11 }}>
                (còn 5 lần trong tháng)
              </Text>
            </View>
          </View>
          <View className="flex-row items-center justify-between">
            <Text className="font-body-sm text-body-sm text-on-surface-variant">Thời gian nhận</Text>
            <Text className="font-body-sm text-body-sm text-on-surface font-medium">Nhận ngay 1 - 3 phút</Text>
          </View>
          <View className="flex-row items-center justify-between">
            <Text className="font-body-sm text-body-sm text-on-surface-variant">Phương thức</Text>
            <Text className="font-body-sm text-body-sm text-on-surface font-medium">Chuyển khoản nhanh Napas 247</Text>
          </View>
          <View className="mt-space-xs p-space-sm bg-surface-container-low rounded-md flex-row items-start gap-space-sm">
            <Icon name="shield_with_heart" size={20} color={colors.primary} />
            <Text className="flex-1 font-label-sm text-label-sm text-on-surface-variant leading-relaxed">
              Giao dịch trên <Text className="text-on-surface font-semibold">{FRAUD_REVIEW_THRESHOLD.toLocaleString('vi-VN')} ₫</Text> hoặc
              khi hệ thống phát hiện dấu hiệu bất thường sẽ chuyển vào luồng Chờ duyệt an toàn (Fraud Review) tối đa 15
              phút để bảo vệ tài sản ví của bạn.
            </Text>
          </View>
        </View>

        {/* Submit */}
        <View className="gap-space-md">
          <Pressable
            onPress={handleSubmit}
            disabled={isSubmitting}
            className="h-12 rounded-md items-center justify-center flex-row gap-2 bg-primary-container"
            style={{ opacity: isSubmitting ? 0.8 : 1 }}
          >
            <Text className="font-headline-sm text-headline-sm text-on-primary font-semibold">
              {isSubmitting ? 'Đang xử lý...' : 'Tiếp tục rút tiền'}
            </Text>
            {!isSubmitting && <Icon name="arrow_forward" size={20} color={colors.onPrimary} />}
          </Pressable>
          <View className="flex-row items-center justify-center gap-space-lg">
            <View className="flex-row items-center gap-1">
              <Icon name="security" size={15} color={colors.secondary} />
              <Text className="font-label-caption text-label-caption text-on-surface-variant">PMPay Shield 256-bit</Text>
            </View>
            <View className="w-1 h-1 rounded-full bg-outline-variant" />
            <View className="flex-row items-center gap-1">
              <Icon name="verified" size={15} color={colors.primary} />
              <Text className="font-label-caption text-label-caption text-on-surface-variant">Chuẩn bảo mật PCI-DSS L1</Text>
            </View>
          </View>
        </View>
      </ScrollView>

      {/* Bank account bottom sheet */}
      <Modal visible={isSheetOpen} transparent animationType="slide" onRequestClose={() => setIsSheetOpen(false)}>
        <Pressable
          className="flex-1 justify-end"
          style={{ backgroundColor: 'rgba(15,44,89,0.4)' }}
          onPress={() => setIsSheetOpen(false)}
        >
          <Pressable className="bg-surface-container-lowest rounded-t-xl p-space-lg gap-space-md" onPress={(e) => e.stopPropagation()}>
            <View className="flex-row items-center justify-between">
              <Text className="font-headline-md text-headline-md text-on-surface font-semibold">
                Chọn tài khoản ngân hàng
              </Text>
              <Pressable onPress={() => setIsSheetOpen(false)} className="w-9 h-9 rounded-full items-center justify-center bg-surface-container-high">
                <Icon name="close" size={20} color={colors.onSurfaceVariant} />
              </Pressable>
            </View>
            <Text className="font-body-sm text-body-sm text-on-surface-variant">
              Chỉ cho phép chuyển về tài khoản chính chủ trùng tên đã hoàn tất định danh điện tử.
            </Text>
            <View className="gap-space-sm">
              {BANK_ACCOUNTS.map((bank) => {
                const isSelected = bank.id === selectedBankId;
                return (
                  <Pressable
                    key={bank.id}
                    onPress={() => setSelectedBankId(bank.id)}
                    className={`p-space-sm rounded-lg flex-row items-center justify-between ${
                      isSelected ? 'bg-surface-container-low' : 'bg-surface-container-lowest'
                    }`}
                  >
                    <View className="flex-row items-center gap-space-sm">
                      <View className="w-10 h-10 rounded-md bg-surface-container-low items-center justify-center">
                        <Icon name="account_balance" size={22} color={isSelected ? colors.secondary : colors.primary} />
                      </View>
                      <View>
                        <Text className="font-headline-sm text-headline-sm text-on-surface">
                          {bank.bankName} {bank.maskedNumber}
                        </Text>
                        <Text className="font-label-sm text-label-sm text-on-surface-variant">{bank.ownerName}</Text>
                      </View>
                    </View>
                    <Icon
                      name={isSelected ? 'check_circle' : 'account_circle'}
                      size={22}
                      color={isSelected ? colors.primary : colors.outlineVariant}
                    />
                  </Pressable>
                );
              })}
            </View>
            <Pressable
              onPress={() => setIsSheetOpen(false)}
              className="h-11 rounded-md bg-primary items-center justify-center mt-space-xs"
            >
              <Text className="font-headline-sm text-headline-sm text-on-primary font-semibold">Xác nhận tài khoản</Text>
            </Pressable>
          </Pressable>
        </Pressable>
      </Modal>
    </SafeAreaView>
  );
}
