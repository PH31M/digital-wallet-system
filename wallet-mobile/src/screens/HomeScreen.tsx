import { useState } from 'react';
import { Pressable, ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import * as Clipboard from 'expo-clipboard';
import { LinearGradient } from 'expo-linear-gradient';
import { useQuery } from '@tanstack/react-query';
import { useNavigation } from '@react-navigation/native';
import { BottomTabNavigationProp } from '@react-navigation/bottom-tabs';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { AppHeader } from '../components/AppHeader';
import { Card } from '../components/Card';
import { EmptyState } from '../components/EmptyState';
import { Icon } from '../components/Icon';
import { LoadingSkeleton } from '../components/LoadingSkeleton';
import { TransactionRow } from '../components/TransactionRow';
import { useAuth } from '../auth/AuthContext';
import { useToast } from '../hooks/useToast';
import { colors } from '../theme/tokens';
import { getCurrentUserWalletApi, getTransactionHistoryApi } from '../api/walletApi';
import { getUnreadNotificationCountApi } from '../api/notificationApi';
import { formatRelativeTime } from '../utils/formatRelativeTime';
import { maskWalletId } from '../utils/formatWalletId';
import { mapTransactionStatusToBadge, mapTransactionStatusToLabel } from '../utils/mapTransactionStatus';
import { getTransactionIcon, getTransactionTypeLabel } from '../utils/transactionDisplay';
import { MainTabsParamList } from '../navigation/MainTabs';
import { RootStackParamList } from '../navigation/RootNavigator';

const RECENT_TRANSACTIONS_SIZE = 5;

type QuickAction = {
  label: string;
  icon: 'sync_alt' | 'add_circle' | 'arrow_circle_down' | 'history';
  onPress: () => void;
};

export function HomeScreen() {
  const navigation = useNavigation<BottomTabNavigationProp<MainTabsParamList>>();
  const { user } = useAuth();
  const { showToast } = useToast();
  const [isBalanceHidden, setIsBalanceHidden] = useState(false);
  const [justCopied, setJustCopied] = useState(false);

  const walletQuery = useQuery({ queryKey: ['wallet', 'me'], queryFn: getCurrentUserWalletApi });
  const recentTransactionsQuery = useQuery({
    queryKey: ['transactions', 'history', 'recent'],
    queryFn: () => getTransactionHistoryApi({ size: RECENT_TRANSACTIONS_SIZE }),
  });
  const unreadCountQuery = useQuery({
    queryKey: ['notifications', 'unread-count'],
    queryFn: getUnreadNotificationCountApi,
  });
  const hasUnreadNotifications = (unreadCountQuery.data ?? 0) > 0;
  const wallet = walletQuery.data;
  const recentTransactions = recentTransactionsQuery.data?.content ?? [];

  async function handleCopyWalletId() {
    if (!wallet) return;
    await Clipboard.setStringAsync(wallet.id);
    setJustCopied(true);
    setTimeout(() => setJustCopied(false), 1500);
  }

  function comingSoon() {
    showToast('Tính năng sắp ra mắt', 'info');
  }

  const rootNavigation = navigation.getParent<NativeStackNavigationProp<RootStackParamList>>();

  const quickActions: QuickAction[] = [
    { label: 'Chuyển tiền', icon: 'sync_alt', onPress: () => rootNavigation?.navigate('Transfer') },
    { label: 'Nạp tiền', icon: 'add_circle', onPress: () => rootNavigation?.navigate('Deposit') },
    { label: 'Rút tiền', icon: 'arrow_circle_down', onPress: () => rootNavigation?.navigate('Withdraw') },
    { label: 'Lịch sử', icon: 'history', onPress: () => navigation.navigate('LichSu') },
  ];

  return (
    <SafeAreaView className="flex-1 bg-background" edges={['top']}>
      <AppHeader
        title="Trang Chủ"
        avatarName={user?.fullName ?? ''}
        onAvatarPress={() => navigation.navigate('HoSo')}
      />
      <ScrollView className="flex-1 px-gutter-mobile" contentContainerStyle={{ paddingVertical: 16, gap: 24 }}>
        {/* Top Greeting Bar */}
        <View className="flex-row items-center justify-between">
          <View className="shrink">
            <View className="flex-row items-center gap-space-xs">
              <Text numberOfLines={1} className="font-headline-sm text-headline-sm text-on-surface font-semibold">
                Xin chào, {user?.fullName}
              </Text>
              {user?.isVerified && <Icon name="verified_user" size={18} color={colors.secondary} />}
            </View>
            <Text className="font-label-caption text-label-caption text-outline uppercase tracking-wider">
              Tài khoản chuẩn eKYC
            </Text>
          </View>
          <Pressable
            onPress={() => navigation.navigate('ThongBao')}
            className="w-11 h-11 items-center justify-center rounded-full bg-surface-container-low"
          >
            <Icon name="notifications" size={22} color={colors.onSurfaceVariant} />
            {hasUnreadNotifications && (
              <View className="absolute top-2.5 right-2.5 w-2 h-2 rounded-full bg-error" />
            )}
          </Pressable>
        </View>

        {/* Balance Card */}
        <View className="rounded-xl overflow-hidden justify-between" style={{ minHeight: 196 }}>
          <LinearGradient
            colors={[colors.primaryContainer, colors.primary]}
            start={{ x: 0, y: 0 }}
            end={{ x: 1, y: 1 }}
            style={{ position: 'absolute', top: 0, left: 0, right: 0, bottom: 0 }}
          />
          <View
            pointerEvents="none"
            className="absolute rounded-full"
            style={{ width: 176, height: 176, right: -48, top: -48, backgroundColor: 'rgba(255,255,255,0.05)' }}
          />
          <View pointerEvents="none" className="absolute right-6 bottom-3" style={{ opacity: 0.1 }}>
            <Icon name="shield_lock" size={108} color={colors.onPrimary} />
          </View>
          <View className="flex-1 justify-between p-space-lg">

          <View className="flex-row items-center justify-between">
            <View className="flex-row items-center gap-space-xs">
              <Text className="font-label-sm text-label-sm text-on-primary-container font-medium">
                Số dư khả dụng
              </Text>
              <Pressable onPress={() => setIsBalanceHidden((v) => !v)} className="w-8 h-8 items-center justify-center">
                <Icon name={isBalanceHidden ? 'visibility_off' : 'visibility'} size={18} color={colors.onPrimaryContainer} />
              </Pressable>
            </View>
            <View
              className="flex-row items-center gap-1.5 px-2.5 py-1 rounded-full"
              style={{ backgroundColor: 'rgba(255,255,255,0.1)' }}
            >
              <Icon name="lock" size={14} color={colors.secondaryFixed} />
              <Text className="font-label-caption text-label-caption text-surface-container-lowest font-medium">
                Bảo vệ bởi 2FA
              </Text>
            </View>
          </View>

          <View className="my-space-xs">
            {walletQuery.isLoading ? (
              <LoadingSkeleton width={160} height={36} radius={6} />
            ) : walletQuery.isError ? (
              <Pressable onPress={() => walletQuery.refetch()} className="flex-row items-center gap-1.5">
                <Icon name="warning_circle" size={18} color={colors.onPrimary} />
                <Text className="font-body-sm text-body-sm text-on-primary">Không tải được số dư — chạm để thử lại</Text>
              </Pressable>
            ) : isBalanceHidden ? (
              <Text className="font-headline-xl text-headline-xl font-bold tracking-tight text-on-primary">
                ••••••••••
              </Text>
            ) : (
              <View className="flex-row items-baseline gap-space-xs">
                <Text className="font-headline-xl text-headline-xl font-bold tracking-tight text-on-primary">
                  {wallet?.balance.toLocaleString('vi-VN')}
                </Text>
                <Text className="font-headline-md text-headline-md text-on-primary-container font-semibold">₫</Text>
              </View>
            )}
          </View>

          <View className="flex-row items-center justify-between pt-space-xs">
            <View className="flex-row items-center gap-space-xs">
              <Text className="font-label-caption text-label-caption text-on-primary-container">Số ví:</Text>
              {wallet ? (
                <>
                  <Text className="font-numeric-ledger text-numeric-ledger text-surface-container-lowest tracking-wider font-semibold">
                    {maskWalletId(wallet.id)}
                  </Text>
                  <Pressable onPress={handleCopyWalletId} className="w-7 h-7 items-center justify-center rounded">
                    <Icon name={justCopied ? 'check_circle' : 'content_copy'} size={16} color={colors.onPrimaryContainer} />
                  </Pressable>
                </>
              ) : (
                <Text className="font-numeric-ledger text-numeric-ledger text-surface-container-lowest tracking-wider font-semibold">
                  ••••
                </Text>
              )}
            </View>
            <Text className="font-label-caption text-label-caption text-secondary-fixed font-semibold tracking-wide uppercase">
              Hạng Bạch Kim
            </Text>
          </View>
          </View>
        </View>

        {/* Quick Action Grid */}
        <View className="flex-row justify-between">
          {quickActions.map((action) => (
            <Pressable key={action.label} onPress={action.onPress} className="items-center gap-space-xs">
              <View className="w-14 h-14 rounded-full bg-surface-container-high items-center justify-center">
                <Icon name={action.icon} size={24} color={colors.primary} />
              </View>
              <Text className="font-label-sm text-label-sm text-on-surface font-semibold">{action.label}</Text>
            </Pressable>
          ))}
        </View>

        {/* Trust Security Banner */}
        <View className="flex-row items-center gap-space-md p-space-md rounded-md bg-surface-container-low">
          <View className="w-10 h-10 rounded-full bg-secondary-container items-center justify-center">
            <Icon name="verified" size={22} color={colors.onSecondaryContainer} />
          </View>
          <View className="flex-1">
            <Text className="font-label-md text-label-md text-primary font-semibold">Bảo mật chuẩn quốc tế</Text>
            <Text className="font-body-sm text-body-sm text-on-surface-variant">
              Tài khoản được bảo vệ & giám sát an toàn 24/7 bởi hệ thống Fraud Detection.
            </Text>
          </View>
        </View>

        {/* Recent Transactions */}
        <View className="gap-space-sm">
          <View className="flex-row items-center justify-between">
            <Text className="font-headline-md text-headline-md text-on-surface font-semibold">
              Giao dịch gần đây
            </Text>
            <Pressable onPress={() => navigation.navigate('LichSu')}>
              <Text className="font-label-md text-label-md text-primary font-semibold">Xem tất cả</Text>
            </Pressable>
          </View>

          {recentTransactionsQuery.isLoading ? (
            <View className="gap-space-xs">
              {[1, 2, 3].map((i) => (
                <LoadingSkeleton key={i} height={68} radius={12} />
              ))}
            </View>
          ) : recentTransactionsQuery.isError ? (
            <Card>
              <EmptyState
                icon="warning_circle"
                title="Không tải được giao dịch"
                description="Vui lòng thử lại."
              />
              <Pressable onPress={() => recentTransactionsQuery.refetch()} className="mt-space-sm self-center">
                <Text className="font-label-md text-label-md text-primary font-semibold">Thử lại</Text>
              </Pressable>
            </Card>
          ) : recentTransactions.length === 0 ? (
            <Card>
              <EmptyState icon="history" title="Chưa có giao dịch nào" />
            </Card>
          ) : (
            <View className="gap-space-xs">
              {recentTransactions.map((tx) => {
                const title = getTransactionTypeLabel(tx.type, tx.direction);
                const icon = getTransactionIcon(tx.type, tx.direction);
                // `tx.description` thực chất là reference number của giao dịch (backend gán
                // nhầm tên field — xem docs/api-endpoints-thuc-te-tren-dev-14-09.md mục 2),
                // dùng làm mã GD hiển thị, không phải mô tả tự do.
                const subtitle = `Mã GD: ${tx.description}`;
                return (
                  <TransactionRow
                    key={tx.id}
                    icon={icon}
                    title={title}
                    subtitle={`${subtitle} • ${formatRelativeTime(tx.createdAt)}`}
                    amount={tx.amount}
                    direction={tx.direction === 'CREDIT' ? 'in' : 'out'}
                    status={mapTransactionStatusToBadge(tx.status)}
                    statusLabel={mapTransactionStatusToLabel(tx.status)}
                    onPress={() =>
                      rootNavigation?.navigate('TransactionDetail', {
                        title,
                        subtitle,
                        icon,
                        amount: tx.amount,
                        direction: tx.direction === 'CREDIT' ? 'in' : 'out',
                        status: mapTransactionStatusToBadge(tx.status),
                        createdAt: tx.createdAt,
                        transactionCode: tx.description,
                      })
                    }
                  />
                );
              })}
            </View>
          )}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}
