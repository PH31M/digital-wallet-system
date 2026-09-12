import { useState } from 'react';
import { Pressable, ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import * as Clipboard from 'expo-clipboard';
import { LinearGradient } from 'expo-linear-gradient';
import { useNavigation } from '@react-navigation/native';
import { BottomTabNavigationProp } from '@react-navigation/bottom-tabs';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { AppHeader } from '../components/AppHeader';
import { Card } from '../components/Card';
import { EmptyState } from '../components/EmptyState';
import { Icon } from '../components/Icon';
import { TransactionRow } from '../components/TransactionRow';
import { useToast } from '../hooks/useToast';
import { colors } from '../theme/tokens';
import { mockRecentTransactions, mockUserProfile, mockWallet } from '../api/mock/homeMock';
import { formatRelativeTime } from '../utils/formatRelativeTime';
import { maskWalletId } from '../utils/formatWalletId';
import { mapTransactionStatusToBadge } from '../utils/mapTransactionStatus';
import { MainTabsParamList } from '../navigation/MainTabs';
import { RootStackParamList } from '../navigation/RootNavigator';

type QuickAction = {
  label: string;
  icon: 'sync_alt' | 'add_circle' | 'arrow_circle_down' | 'history';
  onPress: () => void;
};

export function HomeScreen() {
  const navigation = useNavigation<BottomTabNavigationProp<MainTabsParamList>>();
  const { showToast } = useToast();
  const [isBalanceHidden, setIsBalanceHidden] = useState(false);
  const [justCopied, setJustCopied] = useState(false);

  async function handleCopyWalletId() {
    await Clipboard.setStringAsync(maskWalletId(mockWallet.id).replace('•••• ', ''));
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
        avatarName={mockUserProfile.full_name}
        onAvatarPress={() => navigation.navigate('HoSo')}
      />
      <ScrollView className="flex-1 px-gutter-mobile" contentContainerStyle={{ paddingVertical: 16, gap: 24 }}>
        {/* Top Greeting Bar */}
        <View className="flex-row items-center justify-between">
          <View className="shrink">
            <View className="flex-row items-center gap-space-xs">
              <Text numberOfLines={1} className="font-headline-sm text-headline-sm text-on-surface font-semibold">
                Xin chào, {mockUserProfile.full_name}
              </Text>
              {mockUserProfile.is_verified && <Icon name="verified_user" size={18} color={colors.secondary} />}
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
            <View className="absolute top-2.5 right-2.5 w-2 h-2 rounded-full bg-error" />
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
            {isBalanceHidden ? (
              <Text className="font-headline-xl text-headline-xl font-bold tracking-tight text-on-primary">
                ••••••••••
              </Text>
            ) : (
              <View className="flex-row items-baseline gap-space-xs">
                <Text className="font-headline-xl text-headline-xl font-bold tracking-tight text-on-primary">
                  {mockWallet.balance.toLocaleString('vi-VN')}
                </Text>
                <Text className="font-headline-md text-headline-md text-on-primary-container font-semibold">₫</Text>
              </View>
            )}
          </View>

          <View className="flex-row items-center justify-between pt-space-xs">
            <View className="flex-row items-center gap-space-xs">
              <Text className="font-label-caption text-label-caption text-on-primary-container">Số ví:</Text>
              <Text className="font-numeric-ledger text-numeric-ledger text-surface-container-lowest tracking-wider font-semibold">
                {maskWalletId(mockWallet.id)}
              </Text>
              <Pressable onPress={handleCopyWalletId} className="w-7 h-7 items-center justify-center rounded">
                <Icon name={justCopied ? 'check_circle' : 'content_copy'} size={16} color={colors.onPrimaryContainer} />
              </Pressable>
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

          {mockRecentTransactions.length === 0 ? (
            <Card>
              <EmptyState icon="history" title="Chưa có giao dịch nào" />
            </Card>
          ) : (
            <View className="gap-space-xs">
              {mockRecentTransactions.map((tx) => (
                <TransactionRow
                  key={tx.id}
                  icon={tx.icon}
                  title={tx.counterpartyName}
                  subtitle={`${tx.description} • ${formatRelativeTime(tx.createdAt)}`}
                  amount={tx.amount}
                  direction={tx.direction === 'CREDIT' ? 'in' : 'out'}
                  status={mapTransactionStatusToBadge(tx.status)}
                  onPress={() =>
                    rootNavigation?.navigate('TransactionDetail', {
                      title: tx.counterpartyName,
                      subtitle: tx.description,
                      icon: tx.icon,
                      amount: tx.amount,
                      direction: tx.direction === 'CREDIT' ? 'in' : 'out',
                      status: mapTransactionStatusToBadge(tx.status),
                      createdAt: tx.createdAt,
                      transactionCode: tx.transactionCode,
                    })
                  }
                />
              ))}
            </View>
          )}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}
