import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { ComponentShowcaseScreen } from '../screens/ComponentShowcaseScreen';
import { LoginScreen } from '../screens/LoginScreen';
import { RegisterScreen } from '../screens/RegisterScreen';
import { RegisterOtpScreen } from '../screens/RegisterOtpScreen';
import { TransferScreen } from '../screens/TransferScreen';
import { DepositScreen } from '../screens/DepositScreen';
import { WithdrawScreen } from '../screens/WithdrawScreen';
import { OtpConfirmScreen } from '../screens/OtpConfirmScreen';
import { TransactionResultScreen } from '../screens/TransactionResultScreen';
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
    </Stack.Navigator>
  );
}
