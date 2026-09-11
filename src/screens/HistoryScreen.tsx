import { useMemo, useState } from 'react';
import { Pressable, ScrollView, Text, TextInput, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useNavigation } from '@react-navigation/native';
import { BottomTabNavigationProp } from '@react-navigation/bottom-tabs';
import { AppHeader } from '../components/AppHeader';
import { Card } from '../components/Card';
import { EmptyState } from '../components/EmptyState';
import { Icon } from '../components/Icon';
import { TransactionListItem } from '../components/TransactionListItem';
import { useToast } from '../hooks/useToast';
import { colors } from '../theme/tokens';
import { HistoryFilterCategory, MockHistoryItem, mockHistoryTransactions } from '../api/mock/historyMock';
import { formatCurrency } from '../utils/formatCurrency';
import { dateGroupKey, formatDateGroupLabel, formatTimeOfDay } from '../utils/formatRelativeTime';
import { MainTabsParamList } from '../navigation/MainTabs';

const FILTER_CHIPS: { key: HistoryFilterCategory; label: string }[] = [
  { key: 'all', label: 'Tất cả' },
  { key: 'inflow', label: 'Tiền vào (+)' },
  { key: 'outflow', label: 'Tiền ra (-)' },
  { key: 'transfer', label: 'Chuyển tiền' },
  { key: 'topup', label: 'Nạp / Rút' },
  { key: 'pending', label: 'Chờ duyệt' },
];

function groupByDate(items: MockHistoryItem[]) {
  const order: string[] = [];
  const map = new Map<string, MockHistoryItem[]>();

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

export function HistoryScreen() {
  const navigation = useNavigation<BottomTabNavigationProp<MainTabsParamList>>();
  const { showToast } = useToast();
  const [search, setSearch] = useState('');
  const [activeCategory, setActiveCategory] = useState<HistoryFilterCategory>('all');

  function comingSoon() {
    showToast('Tính năng sắp ra mắt', 'info');
  }

  const filteredItems = useMemo(() => {
    const term = search.trim().toLowerCase();
    return mockHistoryTransactions.filter((item) => {
      const matchCategory = activeCategory === 'all' || item.categories.includes(activeCategory);
      const haystack = `${item.title} ${item.subtitle} ${formatCurrency(item.amount)}`.toLowerCase();
      const matchSearch = !term || haystack.includes(term);
      return matchCategory && matchSearch;
    });
  }, [search, activeCategory]);

  const groups = useMemo(() => groupByDate(filteredItems), [filteredItems]);

  const weeklyInflow = useMemo(
    () =>
      mockHistoryTransactions
        .filter((item) => item.direction === 'CREDIT' && item.status === 'COMPLETED')
        .reduce((sum, item) => sum + item.amount, 0),
    [],
  );
  const weeklyOutflow = useMemo(
    () =>
      mockHistoryTransactions
        .filter((item) => item.direction === 'DEBIT' && item.status === 'COMPLETED')
        .reduce((sum, item) => sum + item.amount, 0),
    [],
  );

  return (
    <SafeAreaView className="flex-1 bg-background" edges={['top']}>
      <AppHeader title="Lịch Sử" avatarName="Nguyễn Văn An" onAvatarPress={() => navigation.navigate('HoSo')} />
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
            placeholder="Tìm kiếm theo tên người nhận, mã GD..."
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
        {groups.length === 0 ? (
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
                {group.items.map((item, index) => (
                  <View key={item.id}>
                    {index > 0 && <View className="h-px bg-surface-container mx-3.5" />}
                    <TransactionListItem
                      icon={item.icon}
                      title={item.title}
                      subtitle={`${item.subtitle} • ${formatTimeOfDay(item.createdAt)}`}
                      amount={item.amount}
                      direction={item.direction === 'CREDIT' ? 'in' : 'out'}
                      pendingApproval={item.status === 'PENDING_REVIEW'}
                    />
                  </View>
                ))}
              </View>
            </View>
          ))
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
