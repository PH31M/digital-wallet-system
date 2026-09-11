import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { IconName } from '../components/Icon';
import { StatusBadgeStatus } from '../components/StatusBadge';
import { ComponentShowcaseScreen } from '../screens/ComponentShowcaseScreen';
import { LoginScreen } from '../screens/LoginScreen';
import { RegisterScreen } from '../screens/RegisterScreen';
import { RegisterOtpScreen } from '../screens/RegisterOtpScreen';
import { TransferScreen } from '../screens/TransferScreen';
import { DepositScreen } from '../screens/DepositScreen';
import { WithdrawScreen } from '../screens/WithdrawScreen';
import { OtpConfirmScreen } from '../screens/OtpConfirmScreen';
import { TransactionResultScreen } from '../screens/TransactionResultScreen';
import { TransactionDetailScreen } from '../screens/TransactionDetailScreen';
import { EditProfileScreen } from '../screens/EditProfileScreen';
import { MainTabs } from './MainTabs';

export type TransactionResultVariant = 'completed' | 'pending' | 'failed';
export type TransactionKind = 'transfer' | 'deposit' | 'withdraw';

export type TransferDraft = {
  amount: number;
  recipientName: string;
  recipientWalletCode: string;
  recipientWalletLabel: string;
  note: string;
};

/**
 * Tham số chung cho màn Kết quả giao dịch, dùng cho cả 3 luồng (chuyển tiền/nạp/rút) —
 * đặt tên trung tính (counterparty/source) vì mỗi luồng có khái niệm "đối tác" khác nhau:
 * người nhận (transfer), nguồn tiền nạp (deposit), tài khoản nhận tiền (withdraw).
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
  transactionCode: string;
  balanceAfter: number;
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
  Login: undefined;
  Register: undefined;
  RegisterOtp: { phone: string };
  Main: undefined;
  ComponentShowcase: undefined;
  Transfer: undefined;
  Deposit: undefined;
  Withdraw: undefined;
  OtpConfirm: TransferDraft;
  TransactionResult: TransactionResultParams;
  TransactionDetail: TransactionDetailParams;
  EditProfile: undefined;
};

const Stack = createNativeStackNavigator<RootStackParamList>();

export function RootNavigator() {
  return (
    <Stack.Navigator initialRouteName="Login" screenOptions={{ headerShown: false }}>
      <Stack.Screen name="Login" component={LoginScreen} />
      <Stack.Screen name="Register" component={RegisterScreen} />
      <Stack.Screen name="RegisterOtp" component={RegisterOtpScreen} />
      <Stack.Screen name="Main" component={MainTabs} />
      <Stack.Screen name="ComponentShowcase" component={ComponentShowcaseScreen} />
      <Stack.Screen name="Transfer" component={TransferScreen} />
      <Stack.Screen name="Deposit" component={DepositScreen} />
      <Stack.Screen name="Withdraw" component={WithdrawScreen} />
      <Stack.Screen name="OtpConfirm" component={OtpConfirmScreen} />
      <Stack.Screen name="TransactionResult" component={TransactionResultScreen} />
      <Stack.Screen name="TransactionDetail" component={TransactionDetailScreen} />
      <Stack.Screen name="EditProfile" component={EditProfileScreen} />
    </Stack.Navigator>
  );
}
