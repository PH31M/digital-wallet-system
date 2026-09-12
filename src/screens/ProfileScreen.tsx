import { useState } from 'react';
import * as Clipboard from 'expo-clipboard';
import { Pressable, ScrollView, Switch, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { CommonActions, useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { AppHeader } from '../components/AppHeader';
import { Avatar } from '../components/Avatar';
import { Icon } from '../components/Icon';
import { SettingsMenuRow } from '../components/SettingsMenuRow';
import { useToast } from '../hooks/useToast';
import { colors } from '../theme/tokens';
import { mockUserProfile } from '../api/mock/homeMock';
import { mockProfileDetails } from '../api/mock/profileMock';
import { RootStackParamList } from '../navigation/RootNavigator';

function SectionTitle({ children }: { children: string }) {
  return (
    <Text className="font-label-caption text-label-caption text-on-surface-variant uppercase tracking-wider px-space-xs">
      {children}
    </Text>
  );
}

export function ProfileScreen() {
  const navigation = useNavigation();
  const rootNavigation = navigation.getParent<NativeStackNavigationProp<RootStackParamList>>();
  const { showToast } = useToast();

  const [isBiometricEnabled, setIsBiometricEnabled] = useState(true);

  function comingSoon() {
    showToast('Tính năng sắp ra mắt', 'info');
  }

  async function handleCopyWalletPublicId() {
    await Clipboard.setStringAsync(mockProfileDetails.walletPublicId);
    showToast('Đã sao chép mã ví', 'success');
  }

  function handleLogout() {
    rootNavigation?.dispatch(CommonActions.reset({ index: 0, routes: [{ name: 'Login' }] }));
  }

  return (
    <SafeAreaView className="flex-1 bg-background" edges={['top']}>
      <AppHeader title="Hồ Sơ Cá Nhân" avatarName={mockUserProfile.full_name} />
      <ScrollView className="flex-1 px-gutter-mobile" contentContainerStyle={{ paddingVertical: 16, gap: 16 }}>
        {/* Profile summary */}
        <View className="bg-surface-container-lowest rounded-lg p-space-md">
          <View className="flex-row items-start gap-space-md">
            <View style={{ position: 'relative' }}>
              <Avatar name={mockUserProfile.full_name} size={64} />
              <View
                className="w-6 h-6 rounded-full bg-secondary items-center justify-center"
                style={{ position: 'absolute', bottom: -4, right: -4 }}
              >
                <Icon name="verified" size={14} color={colors.onSecondary} />
              </View>
            </View>
            <View className="flex-1 min-w-0">
              <View className="flex-row items-center gap-space-xs flex-wrap">
                <Text className="font-headline-md text-headline-md text-primary font-bold">
                  {mockUserProfile.full_name}
                </Text>
                <View className="flex-row items-center gap-1 bg-surface-container-high px-2 py-0.5 rounded-full">
                  <Icon name="workspace_premium" size={13} color={colors.surfaceTint} />
                  <Text className="font-label-caption text-label-caption text-primary font-semibold">
                    {mockProfileDetails.tier}
                  </Text>
                </View>
              </View>
              <Pressable
                onPress={handleCopyWalletPublicId}
                className="mt-1 flex-row items-center gap-1.5 bg-surface-container-low px-2.5 py-1 rounded-md self-start"
              >
                <Text className="font-label-caption text-label-caption text-on-surface-variant">Ví ID:</Text>
                <Text className="font-numeric-ledger text-body-sm text-primary font-semibold tracking-wide">
                  {mockProfileDetails.walletPublicId}
                </Text>
                <Icon name="content_copy" size={15} color={colors.onSurfaceVariant} />
              </Pressable>
              <Text numberOfLines={1} className="mt-1 font-body-sm text-body-sm text-on-surface-variant">
                {mockProfileDetails.maskedEmail} • {mockProfileDetails.maskedPhone}
              </Text>
            </View>
          </View>

          {/* eKYC banner */}
          <Pressable
            onPress={comingSoon}
            className="mt-space-md rounded-md p-space-sm flex-row items-center gap-space-sm"
            style={{ backgroundColor: 'rgba(126,246,190,0.3)' }}
          >
            <View className="w-7 h-7 rounded-full bg-secondary items-center justify-center shrink-0">
              <Icon name="verified_user" size={16} color={colors.onSecondary} />
            </View>
            <View className="flex-1 min-w-0">
              <View className="flex-row items-center gap-1">
                <Text className="font-label-sm text-label-sm text-on-secondary-container font-semibold">
                  eKYC Hoàn tất
                </Text>
                <Icon name="check_circle" size={14} color={colors.secondary} />
              </View>
              <Text numberOfLines={1} className="font-label-caption text-label-caption text-on-surface-variant">
                {mockProfileDetails.ekycTierLabel}
              </Text>
            </View>
            <Text className="font-label-caption text-label-caption text-secondary font-semibold">Chi tiết</Text>
          </Pressable>
        </View>

        {/* Mini stat cards */}
        <View className="flex-row gap-space-sm">
          <View className="flex-1 bg-surface-container-lowest rounded-lg p-space-md justify-between">
            <View className="flex-row items-center justify-between">
              <View className="w-8 h-8 rounded-md bg-surface-container items-center justify-center">
                <Icon name="security" size={18} color={colors.primary} />
              </View>
              <View className="w-2 h-2 rounded-full bg-secondary" />
            </View>
            <View className="mt-space-sm">
              <Text className="font-label-caption text-label-caption text-on-surface-variant uppercase tracking-wider">
                Bảo vệ 2 lớp (2FA)
              </Text>
              <Text className="font-headline-sm text-headline-sm text-secondary font-bold">Đang kích hoạt</Text>
            </View>
          </View>
          <View className="flex-1 bg-surface-container-lowest rounded-lg p-space-md justify-between">
            <View className="flex-row items-center justify-between">
              <View className="w-8 h-8 rounded-md bg-surface-container items-center justify-center">
                <Icon name="speed" size={18} color={colors.primary} />
              </View>
              <Pressable onPress={comingSoon}>
                <Text className="font-label-caption text-label-caption text-primary font-semibold">Tăng</Text>
              </Pressable>
            </View>
            <View className="mt-space-sm">
              <Text className="font-label-caption text-label-caption text-on-surface-variant uppercase tracking-wider">
                Hạn mức ngày
              </Text>
              <Text className="font-numeric-ledger text-headline-sm text-primary font-bold">
                {mockProfileDetails.dailyLimit.toLocaleString('vi-VN')} ₫
              </Text>
            </View>
          </View>
        </View>

        {/* Section: account settings */}
        <View className="gap-space-xs">
          <SectionTitle>Cài đặt tài khoản & thông tin</SectionTitle>
          <View className="bg-surface-container-lowest rounded-lg overflow-hidden">
            <SettingsMenuRow
              icon="manage_accounts"
              title="Chỉnh sửa hồ sơ"
              subtitle="Cập nhật tên hiển thị, SĐT liên hệ"
              onPress={() => rootNavigation?.navigate('EditProfile')}
            />
            <View className="h-px bg-surface-container-high mx-space-md" />
            <SettingsMenuRow
              icon="credit_card"
              title="Liên kết ngân hàng & Thẻ"
              subtitle={mockProfileDetails.linkedBanksSummary}
              onPress={comingSoon}
              trailing={
                <View className="flex-row items-center gap-2">
                  <View className="bg-surface-container px-1.5 py-0.5 rounded">
                    <Text className="font-label-caption text-label-caption text-primary">
                      {mockProfileDetails.linkedBanksCount} Thẻ
                    </Text>
                  </View>
                  <Icon name="chevron_right" size={20} color={colors.outline} />
                </View>
              }
            />
            <View className="h-px bg-surface-container-high mx-space-md" />
            <SettingsMenuRow
              icon="fingerprint"
              title="Xác thực sinh trắc học"
              subtitle="Face ID & Vân tay khi giao dịch"
              trailing={
                <Switch
                  value={isBiometricEnabled}
                  onValueChange={setIsBiometricEnabled}
                  trackColor={{ false: colors.surfaceContainerHigh, true: colors.primary }}
                  thumbColor={colors.onPrimary}
                />
              }
            />
          </View>
        </View>

        {/* Section: security & sessions */}
        <View className="gap-space-xs">
          <SectionTitle>Bảo mật & Phiên làm việc</SectionTitle>
          <View className="bg-surface-container-lowest rounded-lg overflow-hidden">
            <SettingsMenuRow
              icon="phonelink_setup"
              title="Thiết bị đăng nhập"
              subtitle={mockProfileDetails.devicesSummary}
              onPress={() => rootNavigation?.navigate('ActiveSessions')}
              trailing={
                <View className="flex-row items-center gap-2">
                  <View className="bg-surface-container-highest px-1.5 py-0.5 rounded-full">
                    <Text className="font-label-caption text-label-caption text-primary font-semibold">
                      {mockProfileDetails.devicesCount} thiết bị
                    </Text>
                  </View>
                  <Icon name="chevron_right" size={20} color={colors.outline} />
                </View>
              }
            />
            <View className="h-px bg-surface-container-high mx-space-md" />
            <SettingsMenuRow
              icon="lock_reset"
              title="Đổi mật khẩu ví"
              subtitle={`Đổi lần cuối ${mockProfileDetails.passwordLastChangedDaysAgo} ngày trước`}
              onPress={comingSoon}
            />
            <View className="h-px bg-surface-container-high mx-space-md" />
            <SettingsMenuRow
              icon="verified_user"
              title="Mã PIN & Smart OTP"
              subtitle="Bảo vệ giao dịch tự động không chờ SMS"
              onPress={comingSoon}
              trailing={
                <View className="flex-row items-center gap-2">
                  <View className="bg-secondary-container px-1.5 py-0.5 rounded-full">
                    <Text className="font-label-caption text-label-caption text-on-secondary-container font-semibold">
                      Bật
                    </Text>
                  </View>
                  <Icon name="chevron_right" size={20} color={colors.outline} />
                </View>
              }
            />
          </View>
        </View>

        {/* Section: app & support */}
        <View className="gap-space-xs">
          <SectionTitle>Ứng dụng & Hỗ trợ</SectionTitle>
          <View className="bg-surface-container-lowest rounded-lg overflow-hidden">
            <SettingsMenuRow icon="notifications_active" title="Cài đặt thông báo" subtitle="Biến động số dư, tin tức ưu đãi ví" onPress={comingSoon} />
            <View className="h-px bg-surface-container-high mx-space-md" />
            <SettingsMenuRow
              icon="support_agent"
              title="Trung tâm hỗ trợ & FAQ"
              subtitle="Hotline miễn cước 1900 8888"
              onPress={comingSoon}
              trailing={
                <View className="flex-row items-center gap-2">
                  <View className="bg-secondary px-1.5 py-0.5 rounded">
                    <Text className="font-label-caption text-label-caption text-on-secondary font-semibold">24/7</Text>
                  </View>
                  <Icon name="chevron_right" size={20} color={colors.outline} />
                </View>
              }
            />
            <View className="h-px bg-surface-container-high mx-space-md" />
            <SettingsMenuRow
              icon="gavel"
              title="Điều khoản & An toàn thông tin"
              subtitle="Chính sách bảo mật người dùng PMPay"
              onPress={comingSoon}
            />
          </View>
        </View>

        {/* Logout & footer */}
        <View className="gap-space-md items-center">
          <Pressable
            onPress={handleLogout}
            className="w-full h-12 rounded-md items-center justify-center flex-row gap-space-sm"
            style={{ backgroundColor: 'rgba(255,218,214,0.5)' }}
          >
            <Icon name="logout" size={20} color={colors.error} />
            <Text className="font-headline-sm text-headline-sm font-semibold" style={{ color: colors.error }}>
              Đăng xuất tài khoản
            </Text>
          </Pressable>
          <View className="items-center gap-1">
            <Text className="font-label-caption text-label-caption text-outline">
              PMPay Version {mockProfileDetails.appVersion}
            </Text>
            <View className="flex-row items-center gap-1.5">
              <Icon name="verified" size={14} color={colors.secondary} />
              <Text className="font-label-caption text-label-caption text-outline">
                Bảo mật chuẩn quốc tế PCI-DSS Level 1
              </Text>
            </View>
          </View>
          <Pressable onPress={() => rootNavigation?.navigate('ComponentShowcase')}>
            <Text className="font-label-md text-label-md text-primary text-center">→ Component Showcase (dev)</Text>
          </Pressable>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}
