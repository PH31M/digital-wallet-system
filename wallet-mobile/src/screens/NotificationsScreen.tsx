import { useMemo, useState } from 'react';
import { ActivityIndicator, Pressable, ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useInfiniteQuery, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigation } from '@react-navigation/native';
import { BottomTabNavigationProp } from '@react-navigation/bottom-tabs';
import { AppHeader } from '../components/AppHeader';
import { Card } from '../components/Card';
import { EmptyState } from '../components/EmptyState';
import { Icon } from '../components/Icon';
import { useAuth } from '../auth/AuthContext';
import { useToast } from '../hooks/useToast';
import { colors } from '../theme/tokens';
import { getApiErrorMessage } from '../api/apiError';
import {
  getNotificationsApi,
  getUnreadNotificationCountApi,
  markAllNotificationsReadApi,
  markNotificationReadApi,
} from '../api/notificationApi';
import { NotificationResponse } from '../types/api';
import { dateGroupKey, formatTimeOfDay } from '../utils/formatRelativeTime';
import { getNotificationBody, getNotificationStyle, NotificationCategory } from '../utils/notificationDisplay';
import { MainTabsParamList } from '../navigation/MainTabs';

const PAGE_SIZE = 20;

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

function groupByDay(items: NotificationResponse[]) {
  const order: string[] = [];
  const map = new Map<string, NotificationResponse[]>();
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

/**
 * `NotificationResponse` thật KHÔNG có số tiền/tên đối tác (metadata chỉ có
 * transactionId/referenceNumber/transactionType) và title/message backend build sẵn bằng
 * TIẾNG ANH (xem NotificationService.title()/message()) — khác hẳn bản mock cũ (câu tiếng
 * Việt có nhấn mạnh số tiền/tên người). Tiêu đề/nội dung ở đây tự dựng lại tiếng Việt từ
 * `type` qua `notificationDisplay.ts`, không hiển thị thẳng field thật của backend.
 */
export function NotificationsScreen() {
  const navigation = useNavigation<BottomTabNavigationProp<MainTabsParamList>>();
  const { user } = useAuth();
  const { showToast } = useToast();
  const queryClient = useQueryClient();
  const [activeFilter, setActiveFilter] = useState<NotificationCategory | 'all'>('all');

  const notificationsQuery = useInfiniteQuery({
    queryKey: ['notifications', 'list'],
    queryFn: ({ pageParam }) => getNotificationsApi({ page: pageParam, size: PAGE_SIZE }),
    initialPageParam: 0,
    getNextPageParam: (lastPage) => (lastPage.last ? undefined : lastPage.number + 1),
  });
  const unreadCountQuery = useQuery({
    queryKey: ['notifications', 'unread-count'],
    queryFn: getUnreadNotificationCountApi,
  });

  const loadedItems = useMemo(
    () => notificationsQuery.data?.pages.flatMap((page) => page.content) ?? [],
    [notificationsQuery.data],
  );
  const unreadCount = unreadCountQuery.data ?? 0;

  function invalidateNotifications() {
    queryClient.invalidateQueries({ queryKey: ['notifications'] });
  }

  async function handleMarkAllRead() {
    try {
      await markAllNotificationsReadApi();
      invalidateNotifications();
    } catch (error) {
      showToast(getApiErrorMessage(error, 'Không đánh dấu đã đọc được, vui lòng thử lại'), 'danger');
    }
  }

  async function handleOpenNotification(notification: NotificationResponse) {
    if (notification.read) return;
    try {
      await markNotificationReadApi(notification.publicId);
      invalidateNotifications();
    } catch {
      // Không chặn thao tác xem — chỉ là đánh dấu đã đọc, thất bại thì lần sau thử lại.
    }
  }

  const filtered = useMemo(
    () =>
      loadedItems.filter((n) => activeFilter === 'all' || getNotificationStyle(n.type).category === activeFilter),
    [loadedItems, activeFilter],
  );

  const groups = useMemo(() => groupByDay(filtered), [filtered]);

  return (
    <SafeAreaView className="flex-1 bg-background" edges={['top']}>
      <AppHeader title="Thông Báo" avatarName={user?.fullName ?? ''} onAvatarPress={() => navigation.navigate('HoSo')} />
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
        {notificationsQuery.isLoading ? (
          <View className="items-center py-space-xl">
            <ActivityIndicator color={colors.primary} />
          </View>
        ) : notificationsQuery.isError ? (
          <Card>
            <EmptyState icon="warning_circle" title="Không tải được thông báo" description="Vui lòng thử lại." />
            <Pressable onPress={() => notificationsQuery.refetch()} className="mt-space-sm self-center">
              <Text className="font-label-md text-label-md text-primary font-semibold">Thử lại</Text>
            </Pressable>
          </Card>
        ) : groups.length === 0 ? (
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
              {group.items.map((item) => {
                const style = getNotificationStyle(item.type);
                return (
                  <Pressable
                    key={item.publicId}
                    onPress={() => handleOpenNotification(item)}
                    className="rounded-lg p-space-md flex-row gap-space-md"
                    style={{ backgroundColor: item.read ? colors.surfaceContainerLowest : colors.surfaceContainerLow }}
                  >
                    <View
                      className="w-10 h-10 rounded-full items-center justify-center shrink-0"
                      style={{ backgroundColor: style.iconBg }}
                    >
                      <Icon name={style.icon} size={22} color={style.iconColor} />
                    </View>
                    <View className="flex-1 min-w-0">
                      <View className="flex-row items-start justify-between gap-2">
                        <Text
                          numberOfLines={1}
                          className="flex-1 font-headline-sm text-headline-sm leading-tight"
                          style={{ color: style.titleColor }}
                        >
                          {style.title}
                        </Text>
                        {!item.read && (
                          <View
                            className="w-2 h-2 rounded-full shrink-0 mt-1.5"
                            style={{ backgroundColor: style.category === 'security' ? colors.error : colors.secondary }}
                          />
                        )}
                      </View>
                      <Text className="font-body-sm text-body-sm text-on-surface-variant mt-1 leading-relaxed">
                        {getNotificationBody(item)}
                      </Text>
                      <Text className="font-label-caption text-label-caption text-outline mt-2.5 pt-1">
                        {formatTimeOfDay(item.createdAt)} • {formatGroupDate(item.createdAt)}
                      </Text>
                    </View>
                  </Pressable>
                );
              })}
            </View>
          ))
        )}

        {notificationsQuery.hasNextPage && (
          <Pressable
            onPress={() => notificationsQuery.fetchNextPage()}
            disabled={notificationsQuery.isFetchingNextPage}
            className="self-center flex-row items-center gap-space-xs px-5 py-2.5 rounded-md bg-surface-container-high mt-space-sm"
          >
            {notificationsQuery.isFetchingNextPage ? (
              <ActivityIndicator color={colors.primary} size="small" />
            ) : (
              <Icon name="history" size={18} color={colors.primary} />
            )}
            <Text className="font-label-md text-label-md text-primary font-semibold">
              {notificationsQuery.isFetchingNextPage ? 'Đang tải...' : 'Tải thêm thông báo'}
            </Text>
          </Pressable>
        )}
      </ScrollView>
    </SafeAreaView>
  );
}
