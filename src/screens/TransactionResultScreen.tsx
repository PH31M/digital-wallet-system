import { useState } from 'react';
import * as Clipboard from 'expo-clipboard';
import { Pressable, ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { CommonActions, useNavigation, useRoute, RouteProp } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { Icon, IconName } from '../components/Icon';
import { StackHeader } from '../components/StackHeader';
import { useToast } from '../hooks/useToast';
import { colors } from '../theme/tokens';
import { RootStackParamList, TransactionKind, TransactionResultVariant } from '../navigation/RootNavigator';

type VariantConfig = {
  headerTitle: string;
  heroIcon: IconName;
  heroIconColor: string;
  heroIconBg: string;
  heroHaloBg: string;
  amountColor: string;
  statusLabel: string;
  statusBg: string;
  statusTextColor: string;
};

const VARIANT_CONFIG: Record<TransactionResultVariant, VariantConfig> = {
  completed: {
    headerTitle: 'Chi Tiết Giao Dịch',
    heroIcon: 'check_circle',
    heroIconColor: colors.onSecondary,
    heroIconBg: colors.secondary,
    heroHaloBg: 'rgba(126,246,190,0.3)',
    amountColor: colors.onSurface,
    statusLabel: 'Thành công',
    statusBg: 'rgba(126,246,190,0.4)',
    statusTextColor: colors.secondary,
  },
  pending: {
    headerTitle: 'Kết Quả Giao Dịch',
    heroIcon: 'hourglass_top',
    heroIconColor: '#ffffff',
    heroIconBg: '#F59E0B',
    heroHaloBg: 'rgba(245,158,11,0.15)',
    amountColor: colors.primary,
    statusLabel: 'Chờ xét duyệt',
    statusBg: '#FEF3C7',
    statusTextColor: '#92400E',
  },
  failed: {
    headerTitle: 'Kết Quả Giao Dịch',
    heroIcon: 'cancel',
    heroIconColor: colors.onError,
    heroIconBg: colors.error,
    heroHaloBg: '#FEE2E2',
    amountColor: colors.error,
    statusLabel: 'Thất bại',
    statusBg: colors.errorContainer,
    statusTextColor: colors.error,
  },
};

const KIND_VERB: Record<TransactionKind, string> = {
  transfer: 'Chuyển tiền',
  deposit: 'Nạp tiền',
  withdraw: 'Rút tiền',
};

function getHeroTitle(kind: TransactionKind, variant: TransactionResultVariant): string {
  if (variant === 'completed') return `${KIND_VERB[kind]} thành công`;
  if (variant === 'failed') return 'Giao dịch thất bại';
  return 'Giao dịch đang chờ duyệt';
}

/** Nạp tiền là tiền vào ví (+), chuyển/rút tiền là tiền ra khỏi ví (-); pending/failed không hiện dấu. */
function getAmountPrefix(kind: TransactionKind, variant: TransactionResultVariant): string {
  if (variant !== 'completed') return '';
  return kind === 'deposit' ? '+' : '-';
}

function formatTimestamp(date: Date): string {
  const pad = (n: number) => n.toString().padStart(2, '0');
  return `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())} — ${pad(date.getDate())}/${pad(
    date.getMonth() + 1,
  )}/${date.getFullYear()}`;
}

export function TransactionResultScreen() {
  const navigation = useNavigation<NativeStackNavigationProp<RootStackParamList>>();
  const route = useRoute<RouteProp<RootStackParamList, 'TransactionResult'>>();
  const { showToast } = useToast();
  const params = route.params;
  const config = VARIANT_CONFIG[params.variant];
  const heroTitle = getHeroTitle(params.kind, params.variant);
  const amountPrefix = getAmountPrefix(params.kind, params.variant);
  const [timestamp] = useState(() => formatTimestamp(new Date()));
  const [copied, setCopied] = useState(false);

  async function handleCopyCode() {
    await Clipboard.setStringAsync(params.transactionCode);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }

  function handleGoHome() {
    navigation.dispatch(
      CommonActions.reset({
        index: 0,
        routes: [{ name: 'Main' }],
      }),
    );
  }

  const counterpartyInitial = params.counterpartyName.trim().charAt(0).toUpperCase();

  return (
    <SafeAreaView className="flex-1 bg-background" edges={['top']}>
      <StackHeader title={config.headerTitle} onBack={() => navigation.goBack()} avatarName="Nguyễn Văn An" />
      <ScrollView className="flex-1 px-gutter-mobile" contentContainerStyle={{ paddingVertical: 16, gap: 16 }}>
        {/* Hero status */}
        <View className="items-center pt-1 pb-2">
          <View
            className="w-20 h-20 rounded-full items-center justify-center mb-space-sm"
            style={{ backgroundColor: config.heroHaloBg }}
          >
            <View className="w-14 h-14 rounded-full items-center justify-center" style={{ backgroundColor: config.heroIconBg }}>
              <Icon name={config.heroIcon} size={32} color={config.heroIconColor} />
            </View>
          </View>
          <Text
            className="font-headline-md text-headline-md font-bold text-center"
            style={{ color: params.variant === 'failed' ? colors.error : colors.primary }}
          >
            {heroTitle}
          </Text>
          <View className="flex-row items-baseline mt-space-xs">
            <Text
              className="font-numeric-balance-mobile text-numeric-balance-mobile font-bold"
              style={{ color: config.amountColor }}
            >
              {amountPrefix}
              {params.amount.toLocaleString('vi-VN')} đ
            </Text>
          </View>
          <View className="flex-row items-center gap-space-xs mt-space-xs">
            <Icon name="schedule" size={16} color={colors.onSurfaceVariant} />
            <Text className="font-label-sm text-label-sm text-on-surface-variant">{timestamp}</Text>
          </View>
        </View>

        {/* Pending safety banner */}
        {params.variant === 'pending' && (
          <View className="rounded-md p-space-md flex-row items-start gap-space-sm" style={{ backgroundColor: '#FFFBEB', borderWidth: 1, borderColor: '#FDE68A' }}>
            <Icon name="shield_with_heart" size={22} color="#D97706" />
            <View className="flex-1 gap-1">
              <Text className="font-label-md font-semibold" style={{ color: '#78350F' }}>
                Kiểm tra an toàn tự động
              </Text>
              <Text className="font-body-sm text-body-sm leading-relaxed" style={{ color: '#92400E' }}>
                Giao dịch vượt hạn mức thông thường hoặc kích hoạt quy tắc an toàn. Chuyên viên Quản trị rủi ro PMPay
                đang xử lý (dự kiến trong 3 - 5 phút). Tiền trong ví tạm thời được giữ an toàn.
              </Text>
            </View>
          </View>
        )}

        {/* Receipt card */}
        <View className="bg-surface-container-lowest rounded-md p-space-md gap-space-md">
          <View className="flex-row items-center justify-between">
            <View className="shrink pr-space-xs">
              <Text className="font-label-caption text-label-caption text-on-surface-variant uppercase tracking-wider">
                Mã giao dịch
              </Text>
              <View className="flex-row items-center gap-space-xs mt-0.5">
                <Text numberOfLines={1} className="font-numeric-ledger text-numeric-ledger text-primary font-semibold">
                  {params.transactionCode}
                </Text>
                <Pressable onPress={handleCopyCode} className="w-7 h-7 rounded-full bg-surface-container-high items-center justify-center">
                  <Icon name={copied ? 'check' : 'content_copy'} size={15} color={colors.primary} />
                </Pressable>
              </View>
            </View>
            <View className="flex-row items-center px-2.5 py-1 rounded-full" style={{ backgroundColor: config.statusBg }}>
              <View className="w-1.5 h-1.5 rounded-full mr-1.5" style={{ backgroundColor: config.statusTextColor }} />
              <Text
                className="font-label-caption text-label-caption font-bold tracking-wider uppercase"
                style={{ color: config.statusTextColor }}
              >
                {config.statusLabel}
              </Text>
            </View>
          </View>

          <View className="h-px bg-surface-container-high" />

          <View className="gap-space-sm">
            <View className="flex-row items-center justify-between py-1">
              <Text className="font-body-md text-body-md text-on-surface-variant">{params.counterpartyLabel}</Text>
              <View className="flex-row items-center gap-space-xs">
                <View className="w-5 h-5 rounded-full bg-primary-fixed items-center justify-center">
                  <Text style={{ fontSize: 10 }} className="text-on-primary-fixed font-bold">
                    {counterpartyInitial}
                  </Text>
                </View>
                <Text className="font-headline-sm text-headline-sm text-on-surface font-semibold">
                  {params.counterpartyName}
                </Text>
              </View>
            </View>
            <View className="flex-row items-center justify-between py-1">
              <Text className="font-body-md text-body-md text-on-surface-variant">
                {params.kind === 'transfer' ? 'Ví nhận' : 'Chi tiết'}
              </Text>
              <Text className="font-body-md text-body-md text-on-surface font-medium">{params.counterpartyDetail}</Text>
            </View>
            <View className="flex-row items-center justify-between py-1">
              <Text className="font-body-md text-body-md text-on-surface-variant">{params.sourceLabel}</Text>
              <View className="flex-row items-center gap-1.5">
                <Icon name="account_balance_wallet" size={16} color={colors.primary} />
                <Text className="font-body-md text-body-md text-on-surface font-medium">{params.sourceDetail}</Text>
              </View>
            </View>
            {!!params.note && (
              <View className="flex-row items-start justify-between py-1">
                <Text className="font-body-md text-body-md text-on-surface-variant">Nội dung</Text>
                <Text className="font-body-md text-body-md text-on-surface font-medium text-right" style={{ maxWidth: '65%' }}>
                  {params.note}
                </Text>
              </View>
            )}
            <View className="flex-row items-center justify-between py-1">
              <Text className="font-body-md text-body-md text-on-surface-variant">Phí dịch vụ</Text>
              <Text className="font-body-md text-body-md text-secondary font-semibold">
                0 đ ({params.variant === 'failed' ? 'Hoàn phí' : 'Miễn phí'})
              </Text>
            </View>
            <View className="h-px bg-surface-container-high" />
            {params.variant === 'completed' && (
              <View className="flex-row items-center justify-between pt-1">
                <Text className="font-label-md text-label-md text-on-surface-variant">Số dư còn lại</Text>
                <Text className="font-headline-sm text-headline-sm text-primary font-bold">
                  {params.balanceAfter.toLocaleString('vi-VN')} đ
                </Text>
              </View>
            )}
            {params.variant === 'pending' && (
              <View className="flex-row items-center justify-between pt-1">
                <Text className="font-label-md text-label-md text-on-surface-variant">Trạng thái dòng tiền</Text>
                <View className="items-end">
                  <Text className="font-label-md text-label-md font-semibold" style={{ color: '#B45309' }}>
                    Tạm giữ bảo đảm
                  </Text>
                  <Text className="font-numeric-ledger text-numeric-ledger text-primary font-bold">
                    {params.amount.toLocaleString('vi-VN')} đ
                  </Text>
                </View>
              </View>
            )}
            {params.variant === 'failed' && (
              <View className="flex-row items-center justify-between pt-1">
                <Text className="font-label-md text-label-md text-on-surface-variant">Tiền trong ví</Text>
                <Text className="font-headline-sm text-headline-sm text-primary font-bold">Không bị trừ (Bảo toàn)</Text>
              </View>
            )}
          </View>
        </View>

        {/* Failed reason banner */}
        {params.variant === 'failed' && (
          <View
            className="p-space-md rounded-md flex-row items-start gap-space-sm"
            style={{ backgroundColor: 'rgba(255,218,214,0.3)', borderWidth: 1, borderColor: colors.errorContainer }}
          >
            <View className="w-8 h-8 rounded-full items-center justify-center shrink-0" style={{ backgroundColor: colors.errorContainer }}>
              <Icon name="error" size={20} color={colors.error} />
            </View>
            <View className="flex-1 gap-1">
              <Text className="font-label-md text-label-md font-semibold" style={{ color: colors.error }}>
                Lý do: Vượt hạn mức ngày hoặc số dư không đủ (ERR-4032)
              </Text>
              <Text className="font-body-sm text-body-sm text-on-surface-variant leading-relaxed">
                Vui lòng nạp thêm tiền vào ví PMPay hoặc kiểm tra lại hạn mức chuyển khoản sinh trắc học theo QĐ 2345.
              </Text>
            </View>
          </View>
        )}

        {/* Trust / verified tag (completed only) */}
        {params.variant === 'completed' && (
          <View className="flex-row items-center justify-between px-space-md py-space-sm bg-surface-container-low rounded-md">
            <View className="flex-row items-center gap-space-sm">
              <View className="w-8 h-8 rounded-full bg-secondary-fixed items-center justify-center">
                <Icon name="verified_user" size={18} color={colors.onSecondaryFixed} />
              </View>
              <View>
                <Text className="font-label-md text-label-md text-primary font-semibold">Giao dịch bảo mật</Text>
                <Text className="font-body-sm text-body-sm text-on-surface-variant">
                  Được mã hóa 256-bit chuẩn PM Shield
                </Text>
              </View>
            </View>
            <Icon name="chevron_right" size={18} color={colors.onSurfaceVariant} />
          </View>
        )}
        {params.variant === 'pending' && (
          <View className="flex-row items-center justify-between px-space-md py-space-sm bg-surface-container-low rounded-md">
            <View className="flex-row items-center gap-space-sm">
              <View className="w-8 h-8 rounded-full items-center justify-center" style={{ backgroundColor: '#FEF3C7' }}>
                <Icon name="verified_user" size={18} color="#B45309" />
              </View>
              <View>
                <Text className="font-label-md text-label-md text-primary font-semibold">Bảo vệ giao dịch 2 lớp</Text>
                <Text className="font-body-sm text-body-sm text-on-surface-variant">
                  Kiểm duyệt an toàn bảo mật bởi PM Shield
                </Text>
              </View>
            </View>
            <Icon name="chevron_right" size={18} color={colors.onSurfaceVariant} />
          </View>
        )}

        {/* Actions */}
        <View className="gap-space-sm">
          {params.variant === 'failed' ? (
            <>
              <Pressable
                onPress={() => navigation.goBack()}
                className="h-12 rounded-md items-center justify-center flex-row gap-space-xs bg-primary-container"
              >
                <Icon name="refresh" size={19} color={colors.onPrimary} />
                <Text className="font-label-md text-label-md text-on-primary font-semibold">Thử lại giao dịch</Text>
              </Pressable>
              <Pressable
                onPress={handleGoHome}
                className="h-12 rounded-md items-center justify-center flex-row gap-space-xs bg-surface-container-high"
              >
                <Icon name="home" size={19} color={colors.primary} />
                <Text className="font-label-md text-label-md text-primary font-semibold">Về trang chủ</Text>
              </Pressable>
            </>
          ) : (
            <>
              <Pressable
                onPress={() => showToast('Tính năng sắp ra mắt', 'info')}
                className="h-12 rounded-md items-center justify-center flex-row gap-space-xs bg-surface-container-high"
              >
                <Icon name={params.variant === 'pending' ? 'timeline' : 'share'} size={19} color={colors.primary} />
                <Text className="font-label-md text-label-md text-primary font-semibold">
                  {params.variant === 'pending' ? 'Xem tiến trình duyệt' : 'Xem biên lai chi tiết / Chia sẻ'}
                </Text>
              </Pressable>
              <Pressable
                onPress={handleGoHome}
                className="h-12 rounded-md items-center justify-center flex-row gap-space-xs bg-primary-container"
              >
                <Icon name="home" size={19} color={colors.onPrimary} />
                <Text className="font-label-md text-label-md text-on-primary font-semibold">Về trang chủ</Text>
              </Pressable>
            </>
          )}
        </View>

        <Text className="font-body-sm text-body-sm text-on-surface-variant text-center leading-relaxed">
          Mọi thắc mắc vui lòng liên hệ <Text className="text-primary font-semibold">Hotline 1900 8899</Text> hoặc{' '}
          <Text className="text-primary font-semibold">Trung tâm hỗ trợ PMPay</Text>.
        </Text>
      </ScrollView>
    </SafeAreaView>
  );
}
