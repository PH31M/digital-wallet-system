import { useMemo, useState } from 'react';
import { Pressable, ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useNavigation } from '@react-navigation/native';
import { BottomTabNavigationProp } from '@react-navigation/bottom-tabs';
import { AppHeader } from '../components/AppHeader';
import { EmptyState } from '../components/EmptyState';
import { Icon } from '../components/Icon';
import { useToast } from '../hooks/useToast';
import { colors } from '../theme/tokens';
import { MockNotification, NotificationCategory, mockNotifications } from '../api/mock/notificationsMock';
import { formatCurrency } from '../utils/formatCurrency';
import { dateGroupKey, formatTimeOfDay } from '../utils/formatRelativeTime';
import { MainTabsParamList } from '../navigation/MainTabs';

const FILTERS: { key: NotificationCategory | 'all'; label: string }[] = [
  { key: 'all', label: 'Tất cả' },
  { key: 'balance', label: 'Biến động số dư' },
  { key: 'security', label: 'Bảo mật & OTP' },
  { key: 'promo', label: 'Ưu đãi & Tin tức' },
];

function pad2(n: number): string {
  return n.toString().padStart(2, '0');
}

function formatGroupDate(iso: string): string {
  const d = new Date(iso);
  return `${pad2(d.getDate())}/${pad2(d.getMonth() + 1)}/${d.getFullYear()}`;
}

function getGroupLabel(iso: string, now: Date): string {
  const date = new Date(iso);
  const diffDays = Math.floor((now.setHours(0, 0, 0, 0) - new Date(date).setHours(0, 0, 0, 0)) / 86_400_000);
  if (diffDays <= 0) return 'Hôm nay';
  if (diffDays === 1) return 'Hôm qua';
  return 'Tuần trước';
}

function groupByDay(items: MockNotification[]) {
  const order: string[] = [];
  const map = new Map<string, MockNotification[]>();
  for (const item of items) {
    const key = dateGroupKey(item.createdAt);
    if (!map.has(key)) {
      map.set(key, []);
      order.push(key);
    }
    map.get(key)!.push(item);
  }
  const now = new Date();
  return order.map((key) => {
    const groupItems = map.get(key)!;
    return {
      key,
      label: getGroupLabel(groupItems[0].createdAt, new Date(now)),
      date: formatGroupDate(groupItems[0].createdAt),
      items: groupItems,
    };
  });
}

export function NotificationsScreen() {
  const navigation = useNavigation<BottomTabNavigationProp<MainTabsParamList>>();
  const { showToast } = useToast();
  const [notifications, setNotifications] = useState(mockNotifications);
  const [activeFilter, setActiveFilter] = useState<NotificationCategory | 'all'>('all');

  const unreadCount = notifications.filter((n) => !n.isRead).length;

  function comingSoon() {
    showToast('Tính năng sắp ra mắt', 'info');
  }

  function handleMarkAllRead() {
    setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
  }

  const filtered = useMemo(
    () => notifications.filter((n) => activeFilter === 'all' || n.category === activeFilter),
    [notifications, activeFilter],
  );

  const groups = useMemo(() => groupByDay(filtered), [filtered]);

  return (
    <SafeAreaView className="flex-1 bg-background" edges={['top']}>
      <AppHeader title="Thông Báo" avatarName="Nguyễn Văn An" onAvatarPress={() => navigation.navigate('HoSo')} />
      <ScrollView className="flex-1 px-gutter-mobile" contentContainerStyle={{ paddingVertical: 16, gap: 16 }}>
        {/* Title row */}
        <View className="flex-row items-center justify-between">
          <View className="flex-row items-center gap-space-xs">
            <Text className="font-headline-md text-headline-md text-primary font-bold">Thông báo</Text>
            <View
              className="px-2 py-0.5 rounded-full"
              style={{ backgroundColor: unreadCount > 0 ? colors.primaryContainer : colors.surfaceContainerHigh }}
            >
              <Text
                className="font-label-caption text-label-caption font-semibold"
                style={{ color: unreadCount > 0 ? colors.onPrimary : colors.onSurfaceVariant }}
              >
                {unreadCount} mới
              </Text>
            </View>
          </View>
          <Pressable
            onPress={handleMarkAllRead}
            disabled={unreadCount === 0}
            className="flex-row items-center gap-1 py-1 px-2 rounded-md"
          >
            <Icon name={unreadCount === 0 ? 'check' : 'done_all'} size={18} color={unreadCount === 0 ? colors.secondary : colors.primary} />
            <Text
              className="font-label-sm text-label-sm font-semibold"
              style={{ color: unreadCount === 0 ? colors.secondary : colors.primary }}
            >
              {unreadCount === 0 ? 'Đã đọc xong' : 'Đọc tất cả'}
            </Text>
          </Pressable>
        </View>

        {/* Filter chips */}
        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ gap: 8 }}>
          {FILTERS.map((f) => {
            const isActive = f.key === activeFilter;
            return (
              <Pressable
                key={f.key}
                onPress={() => setActiveFilter(f.key)}
                className={`px-3.5 py-1.5 rounded-full ${isActive ? 'bg-primary-container' : 'bg-surface-container-low'}`}
              >
                <Text
                  className={`font-label-sm text-label-sm ${
                    isActive ? 'text-on-primary font-semibold' : 'text-on-surface-variant font-medium'
                  }`}
                >
                  {f.label}
                </Text>
              </Pressable>
            );
          })}
        </ScrollView>

        {/* Grouped notifications */}
        {groups.length === 0 ? (
          <EmptyState
            icon="notifications_paused"
            title="Không có thông báo mới"
            description="Các giao dịch và biến động hệ thống thuộc danh mục này sẽ xuất hiện tại đây."
          />
        ) : (
          groups.map((group) => (
            <View key={group.key} className="gap-space-xs">
              <View className="flex-row items-center justify-between px-1">
                <Text className="font-label-caption text-label-caption text-outline uppercase tracking-wider font-semibold">
                  {group.label}
                </Text>
                <Text className="font-label-caption text-label-caption text-on-surface-variant">{group.date}</Text>
              </View>
              {group.items.map((item) => (
                <View
                  key={item.id}
                  className="rounded-lg p-space-md flex-row gap-space-md"
                  style={{ backgroundColor: item.isRead ? colors.surfaceContainerLowest : colors.surfaceContainerLow }}
                >
                  <View
                    className="w-10 h-10 rounded-full items-center justify-center shrink-0"
                    style={{ backgroundColor: item.iconBg }}
                  >
                    <Icon name={item.icon} size={22} color={item.iconColor} />
                  </View>
                  <View className="flex-1 min-w-0">
                    <View className="flex-row items-start justify-between gap-2">
                      <Text
                        numberOfLines={1}
                        className="flex-1 font-headline-sm text-headline-sm leading-tight"
                        style={{ color: item.titleColor }}
                      >
                        {item.title}
                      </Text>
                      {!item.isRead && (
                        <View
                          className="w-2 h-2 rounded-full shrink-0 mt-1.5"
                          style={{ backgroundColor: item.category === 'security' ? colors.error : colors.secondary }}
                        />
                      )}
                    </View>
                    {item.amount && (
                      <Text
                        className="font-numeric-ledger text-numeric-ledger mt-0.5 tracking-tight"
                        style={{ color: item.amount.direction === 'in' ? colors.secondary : colors.error }}
                      >
                        {item.amount.direction === 'in' ? '+' : '-'}
                        {formatCurrency(item.amount.value)}
                      </Text>
                    )}
                    <Text className="font-body-sm text-body-sm text-on-surface-variant mt-1 leading-relaxed">
                      {item.body.map((seg, i) => (
                        <Text
                          key={i}
                          className={seg.emphasis ? 'font-semibold text-on-surface' : undefined}
                        >
                          {seg.text}
                        </Text>
                      ))}
                    </Text>
                    <View className="flex-row items-center justify-between mt-2.5 pt-1">
                      <Text className="font-label-caption text-label-caption text-outline">
                        {formatTimeOfDay(item.createdAt)} • {formatGroupDate(item.createdAt)}
                      </Text>
                      {item.footer.kind === 'button' ? (
                        <Pressable
                          onPress={comingSoon}
                          className="px-3 py-1 rounded-full bg-primary flex-row items-center gap-1"
                        >
                          <Icon name={item.footer.icon} size={14} color={colors.onPrimary} />
                          <Text className="font-label-caption text-label-caption text-on-primary font-semibold">
                            {item.footer.label}
                          </Text>
                        </Pressable>
                      ) : (
                        <View className="flex-row items-center gap-0.5">
                          {item.footer.icon && <Icon name={item.footer.icon} size={13} color={item.footer.color} />}
                          <Text
                            className="font-label-caption text-label-caption font-semibold"
                            style={{ color: item.footer.color }}
                          >
                            {item.footer.label}
                          </Text>
                        </View>
                      )}
                    </View>
                  </View>
                </View>
              ))}
            </View>
          ))
        )}
      </ScrollView>
    </SafeAreaView>
  );
}
