import { useState } from 'react';
import { Modal, Pressable, ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { Icon } from '../components/Icon';
import { useToast } from '../hooks/useToast';
import { colors } from '../theme/tokens';
import { formatSessionActivity, mockCurrentDevice, mockOtherSessions, MockSession } from '../api/mock/sessionsMock';
import { RootStackParamList } from '../navigation/RootNavigator';

type RevokeTarget = { kind: 'all' } | { kind: 'single'; session: MockSession };

function DetailRow({ icon, color, children }: { icon: Parameters<typeof Icon>[0]['name']; color: string; children: React.ReactNode }) {
  return (
    <View className="flex-row items-center gap-2">
      <Icon name={icon} size={16} color={color} />
      {children}
    </View>
  );
}

export function ActiveSessionsScreen() {
  const navigation = useNavigation<NativeStackNavigationProp<RootStackParamList>>();
  const { showToast } = useToast();

  const [sessions, setSessions] = useState(mockOtherSessions);
  const [revokeTarget, setRevokeTarget] = useState<RevokeTarget | null>(null);

  function comingSoon() {
    showToast('Tính năng sắp ra mắt', 'info');
  }

  function handleConfirmRevoke() {
    if (!revokeTarget) return;
    if (revokeTarget.kind === 'all') {
      setSessions([]);
      showToast('Đã đăng xuất toàn bộ thiết bị khác thành công', 'success');
    } else {
      setSessions((prev) => prev.filter((s) => s.id !== revokeTarget.session.id));
      showToast('Đã thu hồi quyền thiết bị thành công', 'success');
    }
    setRevokeTarget(null);
  }

  return (
    <SafeAreaView className="flex-1 bg-background" edges={['top']}>
      <View className="h-16 px-gutter-mobile flex-row items-center justify-between bg-surface border-b border-surface-container-high">
        <View className="flex-row items-center gap-space-sm flex-1 min-w-0">
          <Pressable onPress={() => navigation.goBack()} className="w-11 h-11 items-center justify-center -ml-2">
            <Icon name="arrow_back" size={24} color={colors.onSurface} />
          </Pressable>
          <Text numberOfLines={1} className="font-headline-sm text-headline-sm text-primary font-semibold flex-1">
            Thiết Bị Đăng Nhập
          </Text>
        </View>
        <Pressable onPress={comingSoon} className="w-11 h-11 items-center justify-center -mr-2">
          <Icon name="info" size={22} color={colors.onSurfaceVariant} />
        </Pressable>
      </View>

      <ScrollView className="flex-1 px-gutter-mobile" contentContainerStyle={{ paddingVertical: 16, gap: 20 }}>
        <Text className="font-body-md text-body-md text-on-surface-variant">
          Quản lý các phiên đăng nhập vào ví PMPay
        </Text>

        {/* Security banner */}
        <View className="bg-surface-container-low rounded-xl p-space-md shadow-sm">
          <View className="flex-row items-start gap-space-sm">
            <View className="w-10 h-10 rounded-full items-center justify-center shrink-0" style={{ backgroundColor: 'rgba(0,108,73,0.1)' }}>
              <Icon name="verified_user" size={24} color={colors.secondary} />
            </View>
            <View className="flex-1 gap-1">
              <Text className="font-headline-sm text-headline-sm text-primary">Bảo vệ tài khoản đa tầng</Text>
              <Text className="font-body-sm text-body-sm text-on-surface-variant leading-relaxed">
                Để bảo vệ tài khoản ví, hãy thu hồi quyền truy cập đối với các thiết bị lạ hoặc không còn sử dụng.
                Nếu phát hiện truy cập bất thường, đổi mật khẩu ngay.
              </Text>
              <Pressable
                onPress={() => sessions.length > 0 && setRevokeTarget({ kind: 'all' })}
                disabled={sessions.length === 0}
                className="flex-row items-center gap-1.5 mt-1.5"
                style={{ opacity: sessions.length === 0 ? 0.4 : 1 }}
              >
                <Icon name="logout" size={18} color={colors.error} />
                <Text className="font-label-md text-label-md" style={{ color: colors.error }}>
                  Đăng xuất tất cả thiết bị khác
                </Text>
              </Pressable>
            </View>
          </View>
        </View>

        {/* Current device */}
        <View className="gap-space-xs">
          <View className="flex-row items-center justify-between px-1">
            <Text className="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wider">
              Thiết bị hiện tại
            </Text>
            <View className="flex-row items-center gap-1.5">
              <View className="w-2 h-2 rounded-full bg-secondary" />
              <Text className="font-label-caption text-label-caption text-secondary">Đang hoạt động</Text>
            </View>
          </View>

          <View className="bg-surface-container-lowest rounded-xl p-space-md shadow-sm">
            <View className="flex-row items-center gap-space-sm mb-3">
              <View className="w-12 h-12 rounded-xl bg-surface-container-low items-center justify-center shrink-0">
                <Icon name={mockCurrentDevice.icon} size={26} color={colors.primary} />
              </View>
              <View className="flex-1 min-w-0">
                <View className="flex-row items-center gap-2 flex-wrap">
                  <Text numberOfLines={1} className="font-headline-sm text-headline-sm text-primary">
                    {mockCurrentDevice.name}
                  </Text>
                  <View className="px-2 py-0.5 rounded-full" style={{ backgroundColor: 'rgba(0,108,73,0.1)' }}>
                    <Text className="font-label-caption text-label-caption text-secondary">Thiết bị này</Text>
                  </View>
                </View>
                <Text numberOfLines={1} className="font-body-sm text-body-sm text-on-surface-variant">
                  {mockCurrentDevice.appVersion}
                </Text>
              </View>
            </View>

            <View className="gap-2 pt-2 bg-surface-container-low/50 rounded-lg p-space-sm">
              <DetailRow icon="location_on" color={colors.outline}>
                <Text className="font-label-md text-label-md text-on-surface">{mockCurrentDevice.location}</Text>
                <Text className="font-body-sm text-body-sm text-outline">(IP: {mockCurrentDevice.ip})</Text>
              </DetailRow>
              <DetailRow icon="security" color={colors.outline}>
                <Text className="font-body-sm text-body-sm text-on-surface-variant">Phương thức:</Text>
                <Text className="font-label-md text-label-md text-on-surface">{mockCurrentDevice.authMethod}</Text>
              </DetailRow>
              <DetailRow icon="schedule" color={colors.secondary}>
                <Text className="font-body-sm text-body-sm text-on-surface-variant">Trạng thái:</Text>
                <Text className="font-label-md text-label-md text-secondary">Ngay bây giờ</Text>
              </DetailRow>
            </View>
          </View>
        </View>

        {/* Other sessions */}
        <View className="gap-space-xs">
          <View className="flex-row items-center justify-between px-1">
            <Text className="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wider">
              Các phiên đăng nhập khác
            </Text>
            <View className="px-2 py-0.5 rounded-full bg-surface-container-high">
              <Text className="font-label-caption text-label-caption text-on-surface-variant">
                {sessions.length} thiết bị
              </Text>
            </View>
          </View>

          {sessions.length === 0 ? (
            <View className="bg-surface-container-lowest rounded-xl p-space-xl items-center">
              <View className="w-14 h-14 rounded-full items-center justify-center mb-space-sm" style={{ backgroundColor: 'rgba(0,108,73,0.1)' }}>
                <Icon name="check_circle" size={32} color={colors.secondary} />
              </View>
              <Text className="font-headline-sm text-headline-sm text-primary text-center">
                Tài khoản được bảo vệ tối đa
              </Text>
              <Text className="font-body-sm text-body-sm text-on-surface-variant mt-1 text-center">
                Không có phiên đăng nhập ngoài luồng nào khác. Chỉ thiết bị này đang có quyền truy cập.
              </Text>
            </View>
          ) : (
            <View className="gap-space-sm">
              {sessions.map((session) => (
                <View key={session.id} className="bg-surface-container-lowest rounded-xl p-space-md shadow-sm">
                  <View className="flex-row items-start gap-space-sm">
                    <View className="w-10 h-10 rounded-xl bg-surface-container-low items-center justify-center shrink-0 mt-0.5">
                      <Icon name={session.icon} size={22} color={colors.primary} />
                    </View>
                    <View className="flex-1 min-w-0">
                      <View className="flex-row items-center gap-2 flex-wrap">
                        <Text numberOfLines={1} className="font-headline-sm text-headline-sm text-primary">
                          {session.name}
                        </Text>
                        {!session.isActive && (
                          <View className="px-2 py-0.5 rounded-full bg-surface-container-high">
                            <Text className="font-label-caption text-label-caption text-on-surface-variant">
                              Không hoạt động
                            </Text>
                          </View>
                        )}
                      </View>
                      <Text numberOfLines={1} className="font-body-sm text-body-sm text-on-surface-variant">
                        {session.subtitle}
                      </Text>
                      <View className="mt-2 gap-1">
                        {session.location && (
                          <DetailRow icon="pin_drop" color={colors.outline}>
                            <Text className="font-body-sm text-body-sm text-on-surface-variant">{session.location}</Text>
                            <Text className="font-body-sm text-body-sm text-outline">(IP: {session.ip})</Text>
                          </DetailRow>
                        )}
                        <DetailRow icon="history" color={colors.outline}>
                          <Text className="font-body-sm text-body-sm text-on-surface-variant">
                            {session.isActive
                              ? formatSessionActivity(session.lastActiveAt, 'relative')
                              : `Hoạt động cuối: ${formatSessionActivity(session.lastActiveAt, 'absolute')}`}
                          </Text>
                        </DetailRow>
                      </View>
                    </View>
                  </View>
                  <View className="mt-space-md pt-space-xs items-end">
                    <Pressable
                      onPress={() => setRevokeTarget({ kind: 'single', session })}
                      className="h-9 px-space-md rounded-lg bg-surface-container-low flex-row items-center gap-1.5"
                    >
                      <Icon name="delete" size={18} color={colors.error} />
                      <Text className="font-label-md text-label-md" style={{ color: colors.error }}>
                        Thu hồi quyền
                      </Text>
                    </Pressable>
                  </View>
                </View>
              ))}
            </View>
          )}
        </View>

        {/* Footer */}
        <View className="gap-space-sm pt-1">
          <View className="flex-row items-start gap-2.5 px-1">
            <Icon name="info" size={20} color={colors.outline} />
            <Text className="flex-1 font-body-sm text-body-sm text-on-surface-variant leading-relaxed">
              Khi bấm <Text className="font-semibold text-on-surface">Thu hồi</Text>, hệ thống sẽ ngay lập tức vô
              hiệu hoá Refresh Token của thiết bị đó. Mọi yêu cầu giao dịch đang chờ từ thiết bị bị huỷ bỏ ngay lập
              tức.
            </Text>
          </View>
          <View className="bg-surface-container-low rounded-xl p-space-md flex-row items-center justify-center gap-2">
            <Icon name="verified" size={20} color={colors.secondary} />
            <Text className="font-label-md text-label-md text-primary tracking-tight">
              Bảo mật đa tầng bởi PM Shield Fraud Detection
            </Text>
          </View>
        </View>
      </ScrollView>

      {/* Revoke confirmation bottom sheet */}
      <Modal visible={revokeTarget !== null} transparent animationType="slide" onRequestClose={() => setRevokeTarget(null)}>
        <Pressable
          className="flex-1 justify-end"
          style={{ backgroundColor: 'rgba(10,31,61,0.45)' }}
          onPress={() => setRevokeTarget(null)}
        >
          <Pressable className="bg-surface-container-lowest rounded-t-2xl p-space-lg" onPress={(e) => e.stopPropagation()}>
            <View className="w-10 h-1.5 bg-outline-variant rounded-full self-center mb-space-md" />
            <View className="w-12 h-12 rounded-full items-center justify-center self-center mb-space-sm" style={{ backgroundColor: colors.errorContainer }}>
              <Icon name="logout" size={28} color={colors.error} />
            </View>
            <Text className="font-headline-md text-headline-md text-primary text-center">
              Xác nhận thu hồi quyền?
            </Text>
            <Text className="font-body-md text-body-md text-on-surface-variant text-center mt-2 mb-space-lg">
              {revokeTarget?.kind === 'all' ? (
                <>
                  Bạn có chắc muốn đăng xuất khỏi <Text className="font-semibold text-on-surface">tất cả thiết bị khác</Text>?
                  Chỉ thiết bị hiện tại sẽ tiếp tục hoạt động.
                </>
              ) : (
                <>
                  Bạn có chắc muốn thu hồi quyền truy cập của thiết bị{' '}
                  <Text className="font-semibold text-on-surface">{revokeTarget?.session.name}</Text>? Refresh Token
                  sẽ bị hủy ngay lập tức.
                </>
              )}
            </Text>
            <View className="gap-space-xs">
              <Pressable
                onPress={handleConfirmRevoke}
                className="w-full h-12 rounded-xl items-center justify-center flex-row gap-2"
                style={{ backgroundColor: colors.error }}
              >
                <Icon name="check" size={20} color={colors.onError} />
                <Text className="font-label-md text-label-md" style={{ color: colors.onError }}>
                  Đồng ý thu hồi
                </Text>
              </Pressable>
              <Pressable
                onPress={() => setRevokeTarget(null)}
                className="w-full h-12 rounded-xl bg-surface-container-low items-center justify-center"
              >
                <Text className="font-label-md text-label-md text-primary">Huỷ bỏ</Text>
              </Pressable>
            </View>
          </Pressable>
        </Pressable>
      </Modal>
    </SafeAreaView>
  );
}
