/**
 * Dữ liệu hiển thị riêng cho Hồ sơ cá nhân — không nằm trong UserProfileResponse thật (đó chỉ
 * có id/email/full_name/phone_number/is_verified), các field dưới đây sẽ đến từ những API
 * khác (ví, bảo mật, thiết bị...) khi nối thật ở phase sau.
 */
export const mockProfileDetails = {
  walletPublicId: 'AP-8829-9102',
  tier: 'Bạch Kim',
  maskedEmail: 'an.nguyen***@email.com',
  maskedPhone: '098****321',
  ekycTierLabel: 'Tài khoản đã định danh mức 2 (Hạn mức tối đa)',
  twoFactorEnabled: true,
  dailyLimit: 100_000_000,
  linkedBanksCount: 2,
  linkedBanksSummary: 'Vietcombank •••• 8839, BIDV',
  devicesCount: 2,
  devicesSummary: 'iPhone 15 Pro (Hiện tại), iPad Air',
  passwordLastChangedDaysAgo: 30,
  pinSmartOtpEnabled: true,
  appVersion: '3.4.2 (Build 2609)',
};
