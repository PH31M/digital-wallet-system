import { useMemo, useState } from 'react';
import { ActivityIndicator, Pressable, ScrollView, Text, TextInput, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useInfiniteQuery } from '@tanstack/react-query';
import { useNavigation } from '@react-navigation/native';
import { BottomTabNavigationProp } from '@react-navigation/bottom-tabs';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { AppHeader } from '../components/AppHeader';
import { Card } from '../components/Card';
import { EmptyState } from '../components/EmptyState';
import { Icon } from '../components/Icon';
import { TransactionListItem } from '../components/TransactionListItem';
import { useAuth } from '../auth/AuthContext';
import { useToast } from '../hooks/useToast';
import { colors } from '../theme/tokens';
import { getTransactionHistoryApi } from '../api/walletApi';
import { TransactionHistoryItemResponse } from '../types/api';
import { formatCurrency } from '../utils/formatCurrency';
import { dateGroupKey, formatDateGroupLabel, formatTimeOfDay } from '../utils/formatRelativeTime';
import { mapTransactionStatusToBadge } from '../utils/mapTransactionStatus';
import { getTransactionIcon, getTransactionTypeLabel } from '../utils/transactionDisplay';
import { MainTabsParamList } from '../navigation/MainTabs';
import { RootStackParamList } from '../navigation/RootNavigator';

const PAGE_SIZE = 20;

/**
 * `TransactionHistoryFilter` thật (`type`/`status`) chỉ nhận ĐÚNG 1 giá trị enum mỗi lần —
 * không lọc được theo chiều tiền (không có field `direction` nào trong filter), không lọc
 * được nhiều status cùng lúc (không thể "type IN (DEPOSIT, WITHDRAW)" cho chip "Nạp / Rút",
 * hay "status IN (4 trạng thái pending)" cho chip "Chờ duyệt" bằng 1 lời gọi) — xem
 * `LedgerEntrySpecifications.typeEquals/statusEquals` (equality thuần, không phải IN-list).
 * Vì 4/6 chip không thể lọc server-side bằng 1 request, toàn bộ filter chip + tìm kiếm ở đây
 * lọc phía client trên dữ liệu ĐÃ TẢI (giống hệt cách bản mock cũ hoạt động, chỉ khác nguồn dữ
 * liệu giờ là API thật có phân trang) — hệ quả: 1 chip hẹp có thể cần bấm "Tải thêm" nhiều lần
 * mới thấy đủ kết quả, không lọc được xuyên suốt toàn bộ lịch sử ngay lập tức.
 */
type HistoryFilterCategory = 'all' | 'inflow' | 'outflow' | 'transfer' | 'topup' | 'pending';

const FILTER_CHIPS: { key: HistoryFilterCategory; label: string }[] = [
  { key: 'all', label: 'Tất cả' },
  { key: 'inflow', label: 'Tiền vào (+)' },
  { key: 'outflow', label: 'Tiền ra (-)' },
  { key: 'transfer', label: 'Chuyển tiền' },
  { key: 'topup', label: 'Nạp / Rút' },
  { key: 'pending', label: 'Chờ duyệt' },
];

function matchesCategory(item: TransactionHistoryItemResponse, category: HistoryFilterCategory): boolean {
  switch (category) {
    case 'all':
      return true;
    case 'inflow':
      return item.direction === 'CREDIT';
    case 'outflow':
      return item.direction === 'DEBIT';
    case 'transfer':
      return item.type === 'TRANSFER';
    case 'topup':
      return item.type === 'DEPOSIT' || item.type === 'WITHDRAW';
    case 'pending':
      return item.status !== 'COMPLETED' && item.status !== 'FAILED';
    default:
      return true;
  }
}

function groupByDate(items: TransactionHistoryItemResponse[]) {
  const order: string[] = [];
  const map = new Map<string, TransactionHistoryItemResponse[]>();

  for (const item of items) {
    const key = dateGroupKey(item.createdAt);
    if (!map.has(key)) {
      map.set(key, []);
      order.push(key);
    }
    map.get(key)!.push(item);
  }

  return order.map((key) => {
    const groupItems = map.get(key)!;
    return { key, label: formatDateGroupLabel(groupItems[0].createdAt), items: groupItems };
  });
}

const ONE_WEEK_MS = 7 * 24 * 60 * 60 * 1000;

export function HistoryScreen() {
  const navigation = useNavigation<BottomTabNavigationProp<MainTabsParamList>>();
  const rootNavigation = navigation.getParent<NativeStackNavigationProp<RootStackParamList>>();
  const { user } = useAuth();
  const { showToast } = useToast();
  const [search, setSearch] = useState('');
  const [activeCategory, setActiveCategory] = useState<HistoryFilterCategory>('all');

  const historyQuery = useInfiniteQuery({
    queryKey: ['transactions', 'history'],
    queryFn: ({ pageParam }) => getTransactionHistoryApi({ page: pageParam, size: PAGE_SIZE }),
    initialPageParam: 0,
    getNextPageParam: (lastPage) => (lastPage.last ? undefined : lastPage.number + 1),
  });

  const loadedItems = useMemo(
    () => historyQuery.data?.pages.flatMap((page) => page.content) ?? [],
    [historyQuery.data],
  );

  function comingSoon() {
    showToast('Tính năng sắp ra mắt', 'info');
  }

  const filteredItems = useMemo(() => {
    const term = search.trim().toLowerCase();
    return loadedItems.filter((item) => {
      const matchCategory = matchesCategory(item, activeCategory);
      const haystack = `${getTransactionTypeLabel(item.type, item.direction)} ${item.description} ${formatCurrency(item.amount)}`.toLowerCase();
      const matchSearch = !term || haystack.includes(term);
      return matchCategory && matchSearch;
    });
  }, [loadedItems, search, activeCategory]);

  const groups = useMemo(() => groupByDate(filteredItems), [filteredItems]);

  // Không có API tổng hợp "dòng tiền tuần này" cho user (chỉ admin dashboard có aggregate) —
  // tính best-effort trên dữ liệu ĐÃ TẢI, có thể thiếu nếu giao dịch trong tuần nằm ở trang
  // chưa bấm "Tải thêm" tới.
  const weeklyInflow = useMemo(() => {
    const since = Date.now() - ONE_WEEK_MS;
    return loadedItems
      .filter((item) => item.direction === 'CREDIT' && item.status === 'COMPLETED' && new Date(item.createdAt).getTime() >= since)
      .reduce((sum, item) => sum + item.amount, 0);
  }, [loadedItems]);
  const weeklyOutflow = useMemo(() => {
    const since = Date.now() - ONE_WEEK_MS;
    return loadedItems
      .filter((item) => item.direction === 'DEBIT' && item.status === 'COMPLETED' && new Date(item.createdAt).getTime() >= since)
      .reduce((sum, item) => sum + item.amount, 0);
  }, [loadedItems]);

  return (
    <SafeAreaView className="flex-1 bg-background" edges={['top']}>
      <AppHeader title="Lịch Sử" avatarName={user?.fullName ?? ''} onAvatarPress={() => navigation.navigate('HoSo')} />
      <ScrollView className="flex-1 px-gutter-mobile" contentContainerStyle={{ paddingVertical: 16, gap: 16 }}>
        {/* Title row */}
        <View className="flex-row items-center justify-between">
          <View>
            <Text className="font-headline-lg text-headline-lg text-primary font-bold">Lịch sử giao dịch</Text>
            <Text className="font-label-sm text-label-sm text-on-surface-variant font-medium">
              Báo cáo tài khoản & sao kê điện tử
            </Text>
          </View>
          <Pressable onPress={comingSoon} className="w-11 h-11 rounded-md bg-surface-container-high items-center justify-center">
            <Icon name="filter_list" size={22} color={colors.primary} />
          </Pressable>
        </View>

        {/* Search */}
        <View className="relative justify-center">
          <View className="absolute left-space-md z-10">
            <Icon name="search" size={20} color={colors.outline} />
          </View>
          <TextInput
            value={search}
            onChangeText={setSearch}
            placeholder="Tìm kiếm theo loại giao dịch, mã GD..."
            placeholderTextColor={colors.outline}
            className="h-12 rounded-md bg-surface-container-lowest text-on-surface font-body-md text-body-md"
            style={{ paddingLeft: 44, paddingRight: search ? 40 : 16 }}
          />
          {search.length > 0 && (
            <Pressable onPress={() => setSearch('')} className="absolute right-space-sm w-8 h-8 items-center justify-center">
              <Icon name="cancel" size={18} color={colors.outline} />
            </Pressable>
          )}
        </View>

        {/* Filter chips */}
        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ gap: 8 }}>
          {FILTER_CHIPS.map((chip) => {
            const isActive = chip.key === activeCategory;
            return (
              <Pressable
                key={chip.key}
                onPress={() => setActiveCategory(chip.key)}
                className={`h-8 px-4 rounded-full items-center justify-center ${
                  isActive ? 'bg-primary' : 'bg-surface-container-high'
                }`}
              >
                <Text
                  className={`font-label-caption text-label-caption ${
                    isActive ? 'text-on-primary font-bold' : 'text-on-surface-variant font-medium'
                  }`}
                >
                  {chip.label}
                </Text>
              </Pressable>
            );
          })}
        </ScrollView>

        {/* Weekly cash flow summary */}
        <View className="flex-row items-center justify-between px-space-xs">
          <Text className="font-label-caption text-label-caption uppercase tracking-wider text-on-surface-variant font-semibold">
            Dòng tiền tuần này
          </Text>
          <View className="flex-row items-center gap-space-sm">
            <Text className="font-label-caption text-label-caption text-secondary font-semibold">
              +{formatCurrency(weeklyInflow)}
            </Text>
            <Text className="font-label-caption text-label-caption text-outline">/</Text>
            <Text className="font-label-caption text-label-caption text-on-surface font-semibold">
              -{formatCurrency(weeklyOutflow)}
            </Text>
          </View>
        </View>

        {/* Grouped transaction list */}
        {historyQuery.isLoading ? (
          <View className="items-center py-space-xl">
            <ActivityIndicator color={colors.primary} />
          </View>
        ) : historyQuery.isError ? (
          <Card>
            <EmptyState icon="warning_circle" title="Không tải được lịch sử giao dịch" description="Vui lòng thử lại." />
            <Pressable onPress={() => historyQuery.refetch()} className="mt-space-sm self-center">
              <Text className="font-label-md text-label-md text-primary font-semibold">Thử lại</Text>
            </Pressable>
          </Card>
        ) : groups.length === 0 ? (
          <Card>
            <EmptyState
              icon="receipt_long"
              title="Không tìm thấy giao dịch"
              description="Hãy thử từ khóa khác hoặc điều chỉnh bộ lọc để xem các biến động số dư trước đó."
            />
          </Card>
        ) : (
          groups.map((group) => (
            <View key={group.key} className="gap-space-xs">
              <View className="flex-row items-center justify-between px-space-xs py-1">
                <Text className="font-label-caption text-label-caption uppercase tracking-wider text-on-surface-variant font-bold">
                  {group.label}
                </Text>
                <Text className="font-label-caption text-label-caption text-outline">
                  {group.items.length} giao dịch
                </Text>
              </View>
              <View className="bg-surface-container-lowest rounded-md overflow-hidden">
                {group.items.map((item, index) => {
                  const title = getTransactionTypeLabel(item.type, item.direction);
                  const icon = getTransactionIcon(item.type, item.direction);
                  const subtitle = `Mã GD: ${item.description}`;
                  return (
                    <View key={item.id}>
                      {index > 0 && <View className="h-px bg-surface-container mx-3.5" />}
                      <TransactionListItem
                        icon={icon}
                        title={title}
                        subtitle={`${subtitle} • ${formatTimeOfDay(item.createdAt)}`}
                        amount={item.amount}
                        direction={item.direction === 'CREDIT' ? 'in' : 'out'}
                        status={item.status}
                        onPress={() =>
                          rootNavigation?.navigate('TransactionDetail', {
                            title,
                            subtitle,
                            icon,
                            amount: item.amount,
                            direction: item.direction === 'CREDIT' ? 'in' : 'out',
                            status: mapTransactionStatusToBadge(item.status),
                            createdAt: item.createdAt,
                            transactionCode: item.description,
                          })
                        }
                      />
                    </View>
                  );
                })}
              </View>
            </View>
          ))
        )}

        {historyQuery.hasNextPage && (
          <Pressable
            onPress={() => historyQuery.fetchNextPage()}
            disabled={historyQuery.isFetchingNextPage}
            className="self-center flex-row items-center gap-space-xs px-5 py-2.5 rounded-md bg-surface-container-high mt-space-sm"
          >
            {historyQuery.isFetchingNextPage ? (
              <ActivityIndicator color={colors.primary} size="small" />
            ) : (
              <Icon name="history" size={18} color={colors.primary} />
            )}
            <Text className="font-label-md text-label-md text-primary font-semibold">
              {historyQuery.isFetchingNextPage ? 'Đang tải...' : 'Tải thêm giao dịch'}
            </Text>
          </Pressable>
        )}

        <Pressable
          onPress={comingSoon}
          className="self-center flex-row items-center gap-space-xs px-5 py-2.5 rounded-md bg-surface-container-high mt-space-sm"
        >
          <Icon name="download" size={18} color={colors.primary} />
          <Text className="font-label-md text-label-md text-primary font-semibold">Tải sao kê điện tử (.PDF)</Text>
        </Pressable>
      </ScrollView>
    </SafeAreaView>
  );
}
