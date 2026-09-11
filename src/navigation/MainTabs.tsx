import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { useNavigation } from '@react-navigation/native';
import { Pressable, Text, View } from 'react-native';
import { Icon, IconName } from '../components/Icon';
import { colors } from '../theme/tokens';
import { HomeScreen } from '../screens/HomeScreen';
import { HistoryScreen } from '../screens/HistoryScreen';
import { PlaceholderScreen } from '../screens/PlaceholderScreen';

export type MainTabsParamList = {
  TrangChu: undefined;
  LichSu: undefined;
  ThongBao: undefined;
  HoSo: undefined;
};

const Tab = createBottomTabNavigator<MainTabsParamList>();

function ThongBaoScreen() {
  return (
    <PlaceholderScreen
      title="Thông báo"
      icon="notifications"
      description="Thông báo giao dịch và bảo mật sẽ hiện ở đây."
    />
  );
}

function HoSoScreen() {
  const navigation = useNavigation();

  return (
    <PlaceholderScreen
      title="Hồ sơ"
      icon="account_circle"
      description="Thông tin tài khoản và cài đặt sẽ hiện ở đây."
      footer={
        <Pressable onPress={() => navigation.getParent()?.navigate('ComponentShowcase' as never)}>
          <Text className="font-label-md text-label-md text-primary text-center">
            → Component Showcase (dev)
          </Text>
        </Pressable>
      }
    />
  );
}

function TabIcon({ name, color, showDot = false }: { name: IconName; color: string; showDot?: boolean }) {
  return (
    <View>
      <Icon name={name} size={24} color={color} />
      {showDot && (
        <View className="absolute -top-0.5 -right-0.5 w-2 h-2 rounded-full bg-error border border-surface" />
      )}
    </View>
  );
}

export function MainTabs() {
  return (
    <Tab.Navigator
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: colors.primary,
        tabBarInactiveTintColor: colors.onSurfaceVariant,
        tabBarStyle: { height: 64, backgroundColor: colors.surface, borderTopColor: colors.surfaceContainerHigh },
        tabBarLabelStyle: { fontFamily: 'BeVietnamPro_600SemiBold', fontSize: 10 },
      }}
    >
      <Tab.Screen
        name="TrangChu"
        component={HomeScreen}
        options={{
          title: 'Trang chủ',
          tabBarIcon: ({ color }) => <TabIcon name="account_balance_wallet" color={color} />,
        }}
      />
      <Tab.Screen
        name="LichSu"
        component={HistoryScreen}
        options={{
          title: 'Lịch sử',
          tabBarIcon: ({ color }) => <TabIcon name="history" color={color} />,
        }}
      />
      <Tab.Screen
        name="ThongBao"
        component={ThongBaoScreen}
        options={{
          title: 'Thông báo',
          tabBarIcon: ({ color }) => <TabIcon name="notifications" color={color} showDot />,
        }}
      />
      <Tab.Screen
        name="HoSo"
        component={HoSoScreen}
        options={{
          title: 'Hồ sơ',
          tabBarIcon: ({ color }) => <TabIcon name="account_circle" color={color} />,
        }}
      />
    </Tab.Navigator>
  );
}
