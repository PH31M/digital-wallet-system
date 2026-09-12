import { useState } from 'react';
import { Pressable, ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { Avatar } from '../components/Avatar';
import { DetailHeader } from '../components/DetailHeader';
import { Icon } from '../components/Icon';
import { Input } from '../components/Input';
import { useToast } from '../hooks/useToast';
import { colors } from '../theme/tokens';
import { mockUserProfile } from '../api/mock/homeMock';
import { mockProfileDetails } from '../api/mock/profileMock';
import { RootStackParamList } from '../navigation/RootNavigator';

const NICKNAME_MAX = 30;

export function EditProfileScreen() {
  const navigation = useNavigation<NativeStackNavigationProp<RootStackParamList>>();
  const { showToast } = useToast();

  const [fullName, setFullName] = useState(mockUserProfile.full_name);
  const [phone, setPhone] = useState('0987 654 321');
  const [nickname, setNickname] = useState('An Nguyen PM');

  function comingSoon() {
    showToast('Tính năng sắp ra mắt', 'info');
  }

  function handleSave() {
    showToast('Đã lưu thay đổi hồ sơ', 'success');
    navigation.goBack();
  }

  return (
    <SafeAreaView className="flex-1 bg-background" edges={['top']}>
      <DetailHeader title="Chỉnh Sửa Hồ Sơ" onBack={() => navigation.goBack()} avatarName={mockUserProfile.full_name} />
      <ScrollView className="flex-1 px-gutter-mobile" contentContainerStyle={{ paddingVertical: 16, gap: 24 }}>
        {/* Avatar section */}
        <View className="items-center pt-1 pb-2">
          <View style={{ position: 'relative' }}>
            <Avatar name={mockUserProfile.full_name} size={96} />
            <Pressable
              onPress={comingSoon}
              className="w-8 h-8 rounded-full bg-primary-container items-center justify-center shadow-sm"
              style={{ position: 'absolute', bottom: 0, right: 0 }}
            >
              <Icon name="photo_camera" size={18} color={colors.onPrimary} />
            </Pressable>
          </View>
          <Text className="font-headline-sm text-headline-sm text-on-surface mt-space-sm">
            Chạm để thay đổi ảnh đại diện
          </Text>
          <Text className="font-body-sm text-body-sm text-on-surface-variant mt-space-xs">
            Hỗ trợ định dạng JPG, PNG (tối đa 5MB)
          </Text>
        </View>

        {/* Identity card (read-only) */}
        <View className="rounded-xl bg-surface-container-low p-space-md shadow-sm">
          <View className="flex-row items-center justify-between gap-space-xs mb-space-xs">
            <View className="flex-row items-center gap-space-xs flex-1 min-w-0">
              <Icon name="badge" size={20} color={colors.primaryContainer} />
              <Text className="font-label-sm text-label-sm text-on-surface-variant">Mã định danh:</Text>
              <Text className="font-numeric-ledger text-numeric-ledger text-primary font-bold tracking-tight">
                {mockProfileDetails.walletPublicId}
              </Text>
            </View>
            <View className="flex-row items-center gap-1 px-2 py-0.5 rounded-full bg-secondary-container shrink-0">
              <Icon name="verified" size={12} color={colors.onSecondaryContainer} />
              <Text className="font-label-caption text-label-caption text-on-secondary-container">eKYC Cấp 2</Text>
            </View>
          </View>
          <View className="flex-row items-start gap-space-xs mt-space-xs pt-space-xs">
            <Icon name="verified_user" size={16} color={colors.secondary} />
            <Text className="flex-1 font-body-sm text-body-sm text-on-surface-variant">
              Thông tin định danh CCCD/Hộ chiếu đã được xác thực an toàn bởi{' '}
              <Text className="font-label-sm text-label-sm text-primary">PM Shield</Text>.
            </Text>
          </View>
        </View>

        {/* Field 1: Họ và tên */}
        <Input
          label="Họ và tên *"
          value={fullName}
          onChangeText={setFullName}
          helperText="Vui lòng nhập họ tên trùng khớp với CCCD/Hộ chiếu đã định danh."
          labelRight={
            <View className="flex-row items-center gap-0.5">
              <Icon name="lock" size={13} color={colors.secondary} />
              <Text className="font-label-caption text-label-caption text-secondary">Khớp CCCD</Text>
            </View>
          }
          trailingAdornment={
            fullName.length > 0 ? (
              <Pressable onPress={() => setFullName('')} hitSlop={8}>
                <Icon name="cancel" size={18} color={colors.outline} />
              </Pressable>
            ) : undefined
          }
        />

        {/* Field 2: Số điện thoại liên hệ */}
        <View className="gap-space-xs">
          <View className="flex-row items-center justify-between">
            <Text className="font-label-md text-label-md text-on-surface">
              Số điện thoại liên hệ <Text style={{ color: colors.error }}>*</Text>
            </Text>
            <Pressable onPress={comingSoon} className="flex-row items-center gap-0.5">
              <Icon name="cell_tower" size={14} color={colors.primaryContainer} />
              <Text className="font-label-sm text-label-sm text-primary-container">Xác thực qua OTP</Text>
            </Pressable>
          </View>
          <View className="flex-row items-center gap-space-xs">
            <View className="h-12 px-3.5 rounded-xl bg-surface-container items-center justify-center shrink-0">
              <Text className="font-label-md text-label-md text-on-surface tracking-wide">🇻🇳 +84</Text>
            </View>
            <View className="flex-1">
              <Input
                label=""
                value={phone}
                onChangeText={setPhone}
                keyboardType="phone-pad"
                trailingAdornment={<Icon name="check_circle" size={20} color={colors.secondary} />}
              />
            </View>
          </View>
          <Text className="font-body-sm text-body-sm text-on-surface-variant">
            Số điện thoại dùng để nhận thông báo biến động số dư và mã OTP.
          </Text>
        </View>

        {/* Field 3: Email (read-only) */}
        <Input
          label="Địa chỉ Email"
          value={mockProfileDetails.maskedEmail}
          editable={false}
          labelRight={
            <View className="flex-row items-center gap-1">
              <Icon name="lock" size={12} color={colors.onSurfaceVariant} />
              <Text className="font-label-caption text-label-caption text-on-surface-variant">Bảo vệ cấp 1</Text>
            </View>
          }
          trailingAdornment={<Icon name="lock" size={20} color={colors.outline} />}
          helperText="Để thay đổi email bảo mật, vui lòng liên hệ Tổng đài 1900 8888 hoặc xác minh tại quầy."
        />

        {/* Field 4: Biệt danh / Tên hiển thị ví */}
        <Input
          label="Biệt danh / Tên hiển thị ví"
          value={nickname}
          onChangeText={(text) => setNickname(text.slice(0, NICKNAME_MAX))}
          maxLength={NICKNAME_MAX}
          helperText="Tên này sẽ hiển thị khi bạn thực hiện giao dịch chuyển tiền hoặc tạo mã QR nhận tiền."
          labelRight={
            <Text className="font-label-caption text-label-caption text-on-surface-variant">
              {nickname.length}/{NICKNAME_MAX}
            </Text>
          }
        />

        {/* Security & compliance banner */}
        <View className="rounded-xl bg-surface-container p-space-md shadow-sm">
          <View className="flex-row items-start gap-space-sm">
            <View className="w-8 h-8 rounded-lg bg-surface-container-highest items-center justify-center shrink-0">
              <Icon name="security" size={20} color={colors.primaryContainer} />
            </View>
            <View className="flex-1 gap-0.5">
              <Text className="font-label-md text-label-md text-primary-container">Lưu ý tuân thủ & bảo mật</Text>
              <Text className="font-body-sm text-body-sm text-on-surface-variant">
                Việc cập nhật thông tin sẽ được ghi lại trong Nhật ký kiểm toán hệ thống. PMPay cam kết bảo mật thông
                tin cá nhân theo tiêu chuẩn quốc tế{' '}
                <Text className="font-label-sm text-label-sm text-on-surface font-bold">PCI-DSS Level 1</Text>.
              </Text>
            </View>
          </View>
        </View>

        {/* Actions */}
        <View className="items-center pt-1 pb-space-lg gap-space-sm">
          <Pressable
            onPress={handleSave}
            className="w-full h-12 rounded-xl bg-primary-container items-center justify-center shadow-md"
          >
            <Text className="font-headline-sm text-headline-sm text-on-primary">Lưu thay đổi</Text>
          </Pressable>
          <Pressable onPress={() => navigation.goBack()} className="w-full h-11 items-center justify-center">
            <Text className="font-label-md text-label-md text-on-surface-variant">Hủy bỏ</Text>
          </Pressable>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}
