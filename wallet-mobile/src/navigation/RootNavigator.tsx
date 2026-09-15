import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { IconName } from '../components/Icon';
import { StatusBadgeStatus } from '../components/StatusBadge';
import { ComponentShowcaseScreen } from '../screens/ComponentShowcaseScreen';
import { SplashScreen } from '../screens/SplashScreen';
import { LoginScreen } from '../screens/LoginScreen';
import { ForgotPasswordScreen } from '../screens/ForgotPasswordScreen';
import { RegisterScreen } from '../screens/RegisterScreen';
import { RegisterOtpScreen } from '../screens/RegisterOtpScreen';
import { MfaVerifyScreen } from '../screens/MfaVerifyScreen';
import { TransferScreen } from '../screens/TransferScreen';
import { DepositScreen } from '../screens/DepositScreen';
import { WithdrawScreen } from '../screens/WithdrawScreen';
import { OtpConfirmScreen } from '../screens/OtpConfirmScreen';
import { TransactionResultScreen } from '../screens/TransactionResultScreen';
import { TransactionDetailScreen } from '../screens/TransactionDetailScreen';
import { EditProfileScreen } from '../screens/EditProfileScreen';
import { ActiveSessionsScreen } from '../screens/ActiveSessionsScreen';
import { MainTabs } from './MainTabs';

export type TransactionResultVariant = 'completed' | 'pending' | 'failed';
export type TransactionKind = 'transfer' | 'deposit' | 'withdraw';

/**
 * Tham số chung cho màn Kết quả giao dịch, dùng cho cả 3 luồng (chuyển tiền/nạp/rút) —
 * đặt tên trung tính (counterparty/source) vì mỗi luồng có khái niệm "đối tác" khác nhau:
 * người nhận (transfer), nguồn tiền nạp (deposit), tài khoản nhận tiền (withdraw).
 * `balanceAfter`/`transactionCode` optional — API thật không trả reference number hay số dư
 * mới khi giao dịch thất bại trước khi tạo xong (lỗi ném ra ở tầng validate/fraud, không có
 * `TransactionResponse` nào đi kèm response lỗi). `failureReason` là message lỗi THẬT từ
 * backend (ApiErrorInfo.message), thay cho banner lý do cố định trước đây.
 */
export type TransactionResultParams = {
  variant: TransactionResultVariant;
  kind: TransactionKind;
  amount: number;
  counterpartyLabel: string;
  counterpartyName: string;
  counterpartyDetail: string;
  sourceLabel: string;
  sourceDetail: string;
  note?: string;
  transactionCode?: string;
  balanceAfter?: number;
  failureReason?: string;
  /**
   * Lý do đứng sau biến thể 'pending' — mặc định 'fraudReview' (giữ đúng copy cũ "vượt hạn mức/
   * kích hoạt quy tắc an toàn", vốn chỉ đúng cho transfer/withdraw bị fraud CHALLENGE giữ lại).
   * 'externalGateway' dùng cho nạp qua MoMo: KHÔNG phải bị fraud giữ, mà đơn giản là tiền chưa
   * về (đang chờ MoMo xác nhận qua IPN) — 2 lý do khác nhau, không dùng chung 1 câu banner.
   */
  pendingReason?: 'fraudReview' | 'externalGateway';
};

/**
 * Tham số cho màn Chi tiết giao dịch — mở từ 1 dòng giao dịch có sẵn (Trang chủ/Lịch sử),
 * nên tái dùng đúng dữ liệu mock của dòng đó (không tự bịa số tài khoản ngân hàng khi
 * giao dịch là nội bộ ví, chỉ hiển thị counterparty/description đã có).
 */
export type TransactionDetailParams = {
  title: string;
  subtitle: string;
  icon: IconName;
  amount: number;
  direction: 'in' | 'out';
  status: StatusBadgeStatus;
  createdAt: string;
  transactionCode: string;
};

export type RootStackParamList = {
  Splash: undefined;
  Login: undefined;
  ForgotPassword: undefined;
  Register: undefined;
  RegisterOtp: { userId: string; email: string };
  MfaVerify: { email: string };
  Main: undefined;
  ComponentShowcase: undefined;
  Transfer: undefined;
  Deposit: undefined;
  Withdraw: undefined;
  /**
   * OTP xác nhận giao dịch (rút tiền/chuyển tiền >= wallet.otp.required-amount) — cần
   * `transactionId` THẬT trả về từ POST deposits/withdrawals/transfers khi status đã là
   * PENDING_OTP_CONFIRMATION, để gọi đúng POST /transactions/{id}/confirm-otp. Không dùng
   * TransferDraft nữa (không tái tạo được state trước khi có transaction thật).
   */
  OtpConfirm: { transactionId: string; kind: TransactionKind; amount: number };
  TransactionResult: TransactionResultParams;
  TransactionDetail: TransactionDetailParams;
  EditProfile: undefined;
  ActiveSessions: undefined;
};

const Stack = createNativeStackNavigator<RootStackParamList>();

export function RootNavigator() {
  return (
    <Stack.Navigator initialRouteName="Splash" screenOptions={{ headerShown: false }}>
      <Stack.Screen name="Splash" component={SplashScreen} />
      <Stack.Screen name="Login" component={LoginScreen} />
      <Stack.Screen name="ForgotPassword" component={ForgotPasswordScreen} />
      <Stack.Screen name="Register" component={RegisterScreen} />
      <Stack.Screen name="RegisterOtp" component={RegisterOtpScreen} />
      <Stack.Screen name="MfaVerify" component={MfaVerifyScreen} />
      <Stack.Screen name="Main" component={MainTabs} />
      <Stack.Screen name="ComponentShowcase" component={ComponentShowcaseScreen} />
      <Stack.Screen name="Transfer" component={TransferScreen} />
      <Stack.Screen name="Deposit" component={DepositScreen} />
      <Stack.Screen name="Withdraw" component={WithdrawScreen} />
      <Stack.Screen name="OtpConfirm" component={OtpConfirmScreen} />
      <Stack.Screen name="TransactionResult" component={TransactionResultScreen} />
      <Stack.Screen name="TransactionDetail" component={TransactionDetailScreen} />
      <Stack.Screen name="EditProfile" component={EditProfileScreen} />
      <Stack.Screen name="ActiveSessions" component={ActiveSessionsScreen} />
    </Stack.Navigator>
  );
}
