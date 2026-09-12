import { useState } from 'react';
import * as Clipboard from 'expo-clipboard';
import { Pressable, ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useNavigation, useRoute, RouteProp } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { Icon, IconName } from '../components/Icon';
import { StackHeader } from '../components/StackHeader';
import { useToast } from '../hooks/useToast';
import { colors } from '../theme/tokens';
import { RootStackParamList } from '../navigation/RootNavigator';
import { StatusBadgeStatus } from '../components/StatusBadge';

type HeroConfig = {
  icon: IconName;
  iconBg: string;
  haloBg: string;
  pillBg: string;
  pillTextColor: string;
  pillLabel: string;
  title: string;
};

const HERO_CONFIG: Record<StatusBadgeStatus, HeroConfig> = {
  success: {
    icon: 'check',
    iconBg: colors.secondary,
    haloBg: 'rgba(126,246,190,0.3)',
    pillBg: 'rgba(126,246,190,0.3)',
    pillTextColor: colors.secondary,
    pillLabel: 'Thành công',
    title: 'Giao dịch thành công',
  },
  warning: {
    icon: 'schedule',
    iconBg: '#F59E0B',
    haloBg: 'rgba(245,158,11,0.2)',
    pillBg: '#FEF3C7',
    pillTextColor: '#92400E',
    pillLabel: 'Đang xử lý',
    title: 'Giao dịch đang xử lý',
  },
  danger: {
    icon: 'close',
    iconBg: colors.error,
    haloBg: 'rgba(255,218,214,0.4)',
    pillBg: colors.errorContainer,
    pillTextColor: colors.error,
    pillLabel: 'Thất bại',
    title: 'Giao dịch thất bại',
  },
  default: {
    icon: 'schedule',
    iconBg: colors.outline,
    haloBg: colors.surfaceContainerHigh,
    pillBg: colors.surfaceContainerHigh,
    pillTextColor: colors.onSurfaceVariant,
    pillLabel: 'Không xác định',
    title: 'Giao dịch',
  },
};

function formatFullTimestamp(iso: string): string {
  const date = new Date(iso);
  const weekdays = ['Chủ Nhật', 'Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy'];
  const pad = (n: number) => n.toString().padStart(2, '0');
  return `${pad(date.getHours())}:${pad(date.getMinutes())} • ${weekdays[date.getDay()]}, ${pad(date.getDate())}/${pad(
    date.getMonth() + 1,
  )}/${date.getFullYear()}`;
}

export function TransactionDetailScreen() {
  const navigation = useNavigation<NativeStackNavigationProp<RootStackParamList>>();
  const route = useRoute<RouteProp<RootStackParamList, 'TransactionDetail'>>();
  const { showToast } = useToast();
  const params = route.params;
  const hero = HERO_CONFIG[params.status];

  const [isSaved, setIsSaved] = useState(false);
  const [copiedCode, setCopiedCode] = useState(false);

  const amountColor = params.status === 'danger' ? colors.error : params.direction === 'in' ? colors.secondary : colors.onSurface;
  const amountSign = params.direction === 'in' ? '+' : '-';

  async function handleCopyCode() {
    await Clipboard.setStringAsync(params.transactionCode);
    setCopiedCode(true);
    showToast('Đã sao chép mã giao dịch', 'success');
    setTimeout(() => setCopiedCode(false), 2000);
  }

  function handleSaveContact() {
    setIsSaved(true);
    showToast(`Đã lưu ${params.title} vào danh bạ`, 'success');
  }

  function handleRepeat() {
    showToast('Đang chuyển hướng sang lệnh chuyển mới...', 'info');
  }

  function comingSoon() {
    showToast('Tính năng sắp ra mắt', 'info');
  }

  return (
    <SafeAreaView className="flex-1 bg-background" edges={['top']}>
      <StackHeader
        title="Chi Tiết Giao Dịch"
        onBack={() => navigation.goBack()}
        avatarName="Nguyễn Văn An"
        rightAccessory={
          <Pressable onPress={comingSoon} className="w-9 h-9 items-center justify-center">
            <Icon name="help_outline" size={22} color={colors.onSurfaceVariant} />
          </Pressable>
        }
      />
      <ScrollView className="flex-1 px-gutter-mobile" contentContainerStyle={{ paddingVertical: 16, gap: 16 }}>
        {/* Top action bar */}
        <View className="flex-row items-center justify-between">
          <View className="flex-row items-center gap-space-xs">
            <View className="w-2.5 h-2.5 rounded-full bg-secondary" />
            <Text className="font-label-sm text-label-sm text-secondary uppercase tracking-wider">
              Hệ thống PMPay
            </Text>
          </View>
          <View className="flex-row items-center gap-space-sm">
            <Pressable onPress={comingSoon} className="h-9 px-space-md rounded-full bg-surface-container-high flex-row items-center gap-1">
              <Icon name="share" size={18} color={colors.primary} />
              <Text className="font-label-md text-label-md text-primary">Chia sẻ</Text>
            </Pressable>
            <Pressable onPress={comingSoon} className="w-9 h-9 rounded-full bg-surface-container-high items-center justify-center">
              <Icon name="print" size={18} color={colors.primary} />
            </Pressable>
          </View>
        </View>

        {/* Receipt card */}
        <View className="bg-surface-container-lowest rounded-lg p-space-lg items-center">
          <View className="w-16 h-16 rounded-full items-center justify-center mb-space-sm" style={{ backgroundColor: hero.haloBg }}>
            <View className="w-12 h-12 rounded-full items-center justify-center" style={{ backgroundColor: hero.iconBg }}>
              <Icon name={hero.icon} size={28} color="#ffffff" />
            </View>
          </View>
          <View className="px-3 py-1 rounded-full mb-space-xs" style={{ backgroundColor: hero.pillBg }}>
            <Text
              className="font-label-caption text-label-caption uppercase tracking-wider"
              style={{ color: hero.pillTextColor }}
            >
              {hero.pillLabel}
            </Text>
          </View>
          <Text className="font-headline-md text-headline-md text-primary text-center">{hero.title}</Text>
          <Text className="font-body-sm text-body-sm text-on-surface-variant mt-0.5">
            {formatFullTimestamp(params.createdAt)}
          </Text>

          <View className="items-center mt-space-md mb-space-sm">
            <View className="flex-row items-baseline">
              <Text className="font-numeric-balance-mobile text-numeric-balance-mobile" style={{ color: amountColor }}>
                {amountSign}
                {params.amount.toLocaleString('vi-VN')}{' '}
              </Text>
              <Text className="font-headline-md text-headline-md font-medium text-on-surface-variant">đ</Text>
            </View>
            <View className="flex-row items-center gap-1 px-2.5 py-0.5 mt-1 rounded-full bg-surface-container">
              <Icon name="bolt" size={14} color={colors.onSurfaceVariant} />
              <Text className="font-label-caption text-label-caption text-on-surface-variant">
                Chuyển tiền nhanh 24/7
              </Text>
            </View>
          </View>

          <View className="w-full bg-surface-container-low rounded-md p-space-sm mt-space-sm flex-row items-center justify-between">
            <View className="min-w-0 pr-2">
              <Text className="font-label-caption text-label-caption text-on-surface-variant uppercase tracking-wider">
                Mã giao dịch PMPay
              </Text>
              <Text numberOfLines={1} className="font-label-md text-label-md text-primary font-semibold">
                {params.transactionCode}
              </Text>
            </View>
            <Pressable
              onPress={handleCopyCode}
              className="px-3 py-1.5 rounded-md bg-surface-container-lowest flex-row items-center gap-1"
            >
              <Icon name={copiedCode ? 'check' : 'content_copy'} size={16} color={colors.primary} />
              <Text className="font-label-sm text-label-sm text-primary">Sao chép</Text>
            </Pressable>
          </View>

          <View className="w-full h-2 bg-surface-container-high/40 rounded-full my-space-md" />

          {/* Counterparty info */}
          <View className="w-full gap-space-sm">
            <View className="flex-row items-center gap-space-xs mb-space-xs">
              <Icon name="account_balance" size={20} color={colors.primary} />
              <Text className="font-headline-sm text-headline-sm text-primary">Thông tin đối tác</Text>
            </View>
            <View className="bg-surface-container-low rounded-md p-space-md gap-space-sm">
              <View className="flex-row items-start gap-space-sm">
                <View className="w-11 h-11 rounded-md bg-surface-container-lowest items-center justify-center shrink-0">
                  <Icon name={params.icon} size={26} color={colors.secondary} />
                </View>
                <View className="flex-1 min-w-0">
                  <Text numberOfLines={1} className="font-headline-sm text-headline-sm text-primary font-bold">
                    {params.title}
                  </Text>
                  <Text numberOfLines={1} className="font-body-sm text-body-sm text-on-surface-variant">
                    {params.subtitle}
                  </Text>
                </View>
              </View>
            </View>
          </View>

          <View className="w-full h-2 bg-surface-container-high/40 rounded-full my-space-md" />

          {/* Payment details */}
          <View className="w-full gap-space-sm">
            <View className="flex-row items-center gap-space-xs mb-space-xs">
              <Icon name="receipt_long" size={20} color={colors.primary} />
              <Text className="font-headline-sm text-headline-sm text-primary">Chi tiết thanh toán</Text>
            </View>
            <View className="bg-surface-container-low rounded-md p-space-md gap-space-xs">
              <View className="flex-row items-center justify-between py-1">
                <Text className="font-body-sm text-body-sm text-on-surface-variant">Nguồn tiền trích nợ</Text>
                <View className="flex-row items-center gap-1.5">
                  <View className="w-2 h-2 rounded-full bg-secondary" />
                  <Text className="font-label-md text-label-md text-primary">Ví chính PMPay</Text>
                </View>
              </View>
              <View className="flex-row items-center justify-between py-1">
                <Text className="font-body-sm text-body-sm text-on-surface-variant">Phí giao dịch</Text>
                <Text className="font-label-md text-label-md text-secondary font-semibold">Miễn phí (0 đ)</Text>
              </View>
              <View className="flex-row items-center justify-between py-1">
                <Text className="font-body-sm text-body-sm text-on-surface-variant">
                  {params.direction === 'in' ? 'Tổng cộng vào ví' : 'Tổng trừ ví'}
                </Text>
                <Text className="font-numeric-ledger text-numeric-ledger text-primary font-bold">
                  {params.amount.toLocaleString('vi-VN')} đ
                </Text>
              </View>
              <View className="flex-row items-center justify-between py-1">
                <Text className="font-body-sm text-body-sm text-on-surface-variant">Phương thức bảo mật</Text>
                <View className="flex-row items-center gap-1">
                  <Icon name="verified_user" size={16} color={colors.secondary} />
                  <Text className="font-label-md text-label-md text-primary">Face ID & PMPay Shield</Text>
                </View>
              </View>
            </View>
          </View>
        </View>

        {/* Action buttons */}
        <View className="flex-row gap-space-sm">
          <Pressable
            onPress={handleSaveContact}
            className="flex-1 h-12 px-space-sm rounded-md items-center justify-center flex-row gap-1.5"
            style={{ backgroundColor: isSaved ? 'rgba(126,246,190,0.3)' : colors.surfaceContainerLowest }}
          >
            <Icon name="person_add" size={20} color={colors.secondary} />
            <Text
              numberOfLines={1}
              className="font-label-md text-label-md"
              style={{ color: isSaved ? colors.secondary : colors.primary }}
            >
              {isSaved ? 'Đã lưu danh bạ' : 'Lưu danh bạ thụ hưởng'}
            </Text>
          </Pressable>
          <Pressable
            onPress={handleRepeat}
            className="flex-1 h-12 px-space-sm rounded-md items-center justify-center flex-row gap-1.5 bg-primary"
          >
            <Icon name="replay" size={20} color={colors.onPrimary} />
            <Text numberOfLines={1} className="font-label-md text-label-md text-on-primary">
              Thực hiện lại
            </Text>
          </Pressable>
        </View>

        <View className="gap-space-xs">
          <Pressable
            onPress={comingSoon}
            className="h-12 rounded-md bg-surface-container-high items-center justify-center flex-row gap-2"
          >
            <Icon name="download" size={20} color={colors.primary} />
            <Text className="font-label-md text-label-md text-primary">Tải biên lai PDF hợp chuẩn</Text>
          </Pressable>
          <Pressable
            onPress={comingSoon}
            className="h-11 rounded-md bg-surface-container-low items-center justify-center flex-row gap-2"
          >
            <Icon name="report_problem" size={18} color={colors.error} />
            <Text className="font-label-md text-label-md" style={{ color: colors.error }}>
              Báo cáo sự cố hoặc tra soát
            </Text>
          </Pressable>
        </View>

        {/* Trust footer */}
        <View className="bg-surface-container-lowest rounded-lg p-space-md items-center gap-space-sm">
          <View className="flex-row items-center gap-space-sm">
            <Text className="font-headline-sm text-headline-sm text-primary font-bold">PMPay</Text>
            <View className="w-1 h-1 rounded-full bg-outline-variant" />
            <View className="flex-row items-center gap-1">
              <Icon name="lock" size={14} color={colors.secondary} />
              <Text className="font-label-caption text-label-caption text-secondary font-semibold">
                PCI-DSS Level 1
              </Text>
            </View>
            <View className="w-1 h-1 rounded-full bg-outline-variant" />
            <Text className="font-label-caption text-label-caption text-on-surface-variant">SHA-256</Text>
          </View>
          <Text className="font-body-sm text-body-sm text-on-surface-variant text-center" style={{ maxWidth: 280 }}>
            Giao dịch được xử lý và bảo hộ bởi Ngân hàng Nhà nước Việt Nam. Biên lai điện tử có giá trị pháp lý tương
            đương chứng từ ngân hàng.
          </Text>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}
