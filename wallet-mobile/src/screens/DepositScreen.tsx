import { useState } from 'react';
import { Pressable, ScrollView, Text, TextInput, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { Icon, IconName } from '../components/Icon';
import { StackHeader } from '../components/StackHeader';
import { useToast } from '../hooks/useToast';
import { colors } from '../theme/tokens';
import { mockWallet } from '../api/mock/homeMock';
import { generateTransactionCode } from '../utils/transferHelpers';
import { RootStackParamList } from '../navigation/RootNavigator';

const MIN_AMOUNT = 10_000;
const QUICK_AMOUNTS = [100_000, 200_000, 500_000, 1_000_000, 2_000_000, 5_000_000];

type FundingSource = {
  id: string;
  icon: IconName;
  name: string;
  detail: string;
  tag?: string;
};

const FUNDING_SOURCES: FundingSource[] = [
  { id: 'vcb', icon: 'account_balance', name: 'Vietcombank', detail: '•••• 6868 • Hạn mức: 50.000.000 đ/ngày', tag: 'Ưu tiên' },
  { id: 'visa', icon: 'credit_card', name: 'Thẻ ghi nợ Quốc tế', detail: 'Visa / Mastercard •••• 9234' },
  { id: 'vietqr', icon: 'qr_code_2', name: 'Chuyển khoản VietQR', detail: 'Quét mã Napas 247 từ bất kỳ App Ngân hàng' },
];

export function DepositScreen() {
  const navigation = useNavigation<NativeStackNavigationProp<RootStackParamList>>();
  const { showToast } = useToast();

  const [selectedId, setSelectedId] = useState('vcb');
  const [amount, setAmount] = useState(500_000);
  const [note, setNote] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  function comingSoon() {
    showToast('Tính năng sắp ra mắt', 'info');
  }

  function handleAmountChange(text: string) {
    const digitsOnly = text.replace(/\D/g, '');
    setAmount(digitsOnly ? parseInt(digitsOnly, 10) : 0);
  }

  function handleSubmit() {
    if (amount < MIN_AMOUNT) {
      showToast(`Số tiền nạp tối thiểu là ${MIN_AMOUNT.toLocaleString('vi-VN')} đ`, 'danger');
      return;
    }
    const source = FUNDING_SOURCES.find((s) => s.id === selectedId)!;
    setIsSubmitting(true);
    setTimeout(() => {
      setIsSubmitting(false);
      navigation.navigate('TransactionResult', {
        kind: 'deposit',
        variant: 'completed',
        amount,
        counterpartyLabel: 'Nguồn tiền nạp',
        counterpartyName: source.name,
        counterpartyDetail: source.detail,
        sourceLabel: 'Nạp vào',
        sourceDetail: 'Ví chính PMPay',
        note,
        transactionCode: generateTransactionCode(),
        balanceAfter: mockWallet.balance + amount,
      });
    }, 1200);
  }

  return (
    <SafeAreaView className="flex-1 bg-background" edges={['top']}>
      <StackHeader
        title="Nạp Tiền"
        onBack={() => navigation.goBack()}
        avatarName="Nguyễn Văn An"
        rightAccessory={
          <Pressable onPress={comingSoon} className="w-9 h-9 items-center justify-center">
            <Icon name="help_outline" size={22} color={colors.onSurfaceVariant} />
          </Pressable>
        }
      />
      <ScrollView className="flex-1 px-gutter-mobile" contentContainerStyle={{ paddingVertical: 16, gap: 16 }}>
        {/* Balance card */}
        <View className="rounded-md p-space-md overflow-hidden" style={{ backgroundColor: colors.primaryContainer }}>
          <View className="flex-row items-center justify-between">
            <View className="flex-row items-center gap-1.5">
              <Icon name="account_balance_wallet" size={18} color={colors.onPrimaryContainer} />
              <Text className="font-label-sm text-label-sm text-on-primary-container">Ví chính PMPay</Text>
            </View>
            <View className="flex-row items-center gap-1 px-2 py-0.5 rounded-full" style={{ backgroundColor: 'rgba(126,246,190,0.2)' }}>
              <Icon name="verified_user" size={13} color={colors.secondaryFixed} />
              <Text className="font-label-caption text-label-caption text-secondary-fixed">PMPay Shield Verified</Text>
            </View>
          </View>
          <Text className="font-body-sm text-body-sm text-on-primary-container mt-space-sm">Số dư khả dụng hiện tại</Text>
          <View className="flex-row items-baseline gap-1 mt-0.5">
            <Text className="font-numeric-balance-mobile text-numeric-balance-mobile text-on-primary font-bold">
              {mockWallet.balance.toLocaleString('vi-VN')}
            </Text>
            <Text className="font-headline-sm text-headline-sm text-on-primary-container">₫</Text>
          </View>
        </View>

        {/* Funding sources */}
        <View className="gap-space-xs">
          <View className="flex-row items-center justify-between">
            <Text className="font-headline-sm text-headline-sm text-on-surface">Nguồn tiền nạp</Text>
            <View className="flex-row items-center gap-0.5">
              <Icon name="bolt" size={16} color={colors.secondary} />
              <Text className="font-label-sm text-label-sm text-secondary">Miễn phí nạp</Text>
            </View>
          </View>

          <View className="gap-2">
            {FUNDING_SOURCES.map((source) => {
              const isSelected = source.id === selectedId;
              return (
                <Pressable
                  key={source.id}
                  onPress={() => setSelectedId(source.id)}
                  className={`p-space-md rounded-lg flex-row items-center justify-between ${
                    isSelected ? 'bg-surface-container-highest' : 'bg-surface-container-lowest'
                  }`}
                >
                  <View className="flex-row items-center gap-3 flex-1 min-w-0 pr-space-xs">
                    <View className="w-10 h-10 rounded-md bg-surface-container items-center justify-center">
                      <Icon name={source.icon} size={22} color={isSelected ? colors.secondary : colors.onSurface} />
                    </View>
                    <View className="shrink">
                      <View className="flex-row items-center gap-2">
                        <Text numberOfLines={1} className="font-headline-sm text-headline-sm text-on-surface">
                          {source.name}
                        </Text>
                        {source.tag && (
                          <View className="px-1.5 py-0.5 rounded-full" style={{ backgroundColor: 'rgba(0,108,73,0.1)' }}>
                            <Text className="font-label-caption text-label-caption text-secondary">{source.tag}</Text>
                          </View>
                        )}
                      </View>
                      <Text numberOfLines={1} className="font-body-sm text-body-sm text-on-surface-variant mt-0.5">
                        {source.detail}
                      </Text>
                    </View>
                  </View>
                  <View
                    className="w-5 h-5 rounded-full items-center justify-center"
                    style={{ backgroundColor: isSelected ? colors.primaryContainer : colors.surfaceContainerHigh }}
                  >
                    {isSelected && <Icon name="check_circle" size={16} color={colors.onPrimary} />}
                  </View>
                </Pressable>
              );
            })}
          </View>

          <Pressable
            onPress={comingSoon}
            className="py-2.5 px-3 rounded-lg bg-surface-container-low items-center justify-center flex-row gap-2 mt-space-xs"
          >
            <Icon name="add_circle" size={20} color={colors.primary} />
            <Text className="font-label-md text-label-md text-primary">Thêm liên kết tài khoản ngân hàng mới</Text>
          </Pressable>
        </View>

        {/* Amount section */}
        <View className="bg-surface-container-lowest rounded-lg p-space-md gap-space-md">
          <View className="flex-row items-center justify-between">
            <Text className="font-headline-sm text-headline-sm text-on-surface">Số tiền cần nạp</Text>
            <Pressable onPress={() => setAmount(0)}>
              <Text className="font-label-sm text-label-sm text-on-surface-variant">Xóa</Text>
            </Pressable>
          </View>

          <View className="bg-surface-container-low rounded-lg p-3 flex-row items-center justify-between">
            <View className="flex-row items-baseline gap-1 flex-1">
              <TextInput
                value={amount ? amount.toLocaleString('vi-VN') : ''}
                onChangeText={handleAmountChange}
                keyboardType="number-pad"
                placeholder="0"
                placeholderTextColor={colors.outline}
                className="font-numeric-balance-mobile text-numeric-balance-mobile text-primary font-bold flex-1"
              />
              <Text className="font-headline-lg text-headline-lg text-on-surface-variant font-semibold">₫</Text>
            </View>
            <Icon name="edit" size={22} color={colors.outlineVariant} />
          </View>

          <View className="gap-1.5">
            <Text className="font-label-sm text-label-sm text-on-surface-variant">Chọn nhanh số tiền</Text>
            <View className="gap-2">
              {[QUICK_AMOUNTS.slice(0, 3), QUICK_AMOUNTS.slice(3)].map((row, rowIndex) => (
                <View key={rowIndex} className="flex-row gap-2">
                  {row.map((value) => {
                    const isActive = amount === value;
                    return (
                      <Pressable
                        key={value}
                        onPress={() => setAmount(value)}
                        className="flex-1 py-2 px-2 rounded-md items-center justify-center"
                        style={{ backgroundColor: isActive ? colors.primaryContainer : colors.surfaceContainer }}
                      >
                        <Text
                          className="font-label-md text-label-md"
                          style={{ color: isActive ? colors.onPrimary : colors.onSurface }}
                        >
                          +{value.toLocaleString('vi-VN')} đ
                        </Text>
                      </Pressable>
                    );
                  })}
                </View>
              ))}
            </View>
          </View>
        </View>

        {/* Transaction details */}
        <View className="bg-surface-container-lowest rounded-lg p-space-md gap-2">
          <Text className="font-headline-sm text-headline-sm text-on-surface mb-1">Chi tiết giao dịch</Text>
          <View className="flex-row items-center justify-between">
            <Text className="font-body-md text-body-md text-on-surface-variant">Phí giao dịch</Text>
            <Text className="font-label-md text-label-md text-secondary font-semibold">Miễn phí (0 ₫)</Text>
          </View>
          <View className="flex-row items-center justify-between">
            <Text className="font-body-md text-body-md text-on-surface-variant">Thời gian xử lý</Text>
            <Text className="font-label-md text-label-md text-on-surface">Tức thì (24/7)</Text>
          </View>
          <View className="flex-row items-center justify-between pt-1">
            <Text className="font-body-md text-body-md text-on-surface font-semibold">Tiền thực nhận vào ví</Text>
            <Text className="font-numeric-ledger text-numeric-ledger text-primary font-bold">
              {amount.toLocaleString('vi-VN')} ₫
            </Text>
          </View>

          <View className="pt-1">
            <View className="flex-row items-center justify-between mb-1">
              <Text className="font-label-sm text-label-sm text-on-surface-variant">Ghi chú giao dịch (Tùy chọn)</Text>
              <Text className="font-label-caption text-label-caption text-outline">{note.length}/100</Text>
            </View>
            <TextInput
              value={note}
              onChangeText={(text) => setNote(text.slice(0, 100))}
              placeholder="Ví dụ: Nạp tiền sinh hoạt tuần..."
              placeholderTextColor={colors.outlineVariant}
              className="bg-surface-container-low px-3 py-2.5 rounded-md font-body-md text-body-md text-on-surface"
            />
          </View>
        </View>

        <View className="flex-row items-center justify-center gap-3">
          <View className="flex-row items-center gap-1">
            <Icon name="lock" size={14} color={colors.outline} />
            <Text className="font-label-caption text-label-caption text-outline">PCI-DSS Level 1</Text>
          </View>
          <Text className="text-outline">•</Text>
          <View className="flex-row items-center gap-1">
            <Icon name="shield" size={14} color={colors.outline} />
            <Text className="font-label-caption text-label-caption text-outline">256-Bit TLS Encryption</Text>
          </View>
        </View>

        <View className="gap-space-xs">
          <Pressable
            onPress={handleSubmit}
            disabled={isSubmitting}
            className="h-12 rounded-md items-center justify-center flex-row gap-2 bg-primary-container"
            style={{ opacity: isSubmitting ? 0.8 : 1 }}
          >
            {isSubmitting ? (
              <Text className="font-headline-sm text-headline-sm text-on-primary">Đang xử lý nạp tiền...</Text>
            ) : (
              <>
                <Icon name="add_card" size={22} color={colors.onPrimary} />
                <Text className="font-headline-sm text-headline-sm text-on-primary">Nạp tiền ngay</Text>
                <Icon name="arrow_forward" size={18} color={colors.onPrimary} />
              </>
            )}
          </Pressable>
          <Text className="font-label-caption text-label-caption text-outline text-center">
            Bằng cách tiếp tục, bạn đồng ý với Điều khoản nạp tiền của PMPay
          </Text>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}
