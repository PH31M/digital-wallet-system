import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { ComponentShowcaseScreen } from '../screens/ComponentShowcaseScreen';
import { LoginScreen } from '../screens/LoginScreen';
import { RegisterScreen } from '../screens/RegisterScreen';
import { RegisterOtpScreen } from '../screens/RegisterOtpScreen';
import { TransferScreen } from '../screens/TransferScreen';
import { OtpConfirmScreen } from '../screens/OtpConfirmScreen';
import { TransactionResultScreen } from '../screens/TransactionResultScreen';
import { MainTabs } from './MainTabs';

export type TransactionResultVariant = 'completed' | 'pending' | 'failed';

export type TransferDraft = {
  amount: number;
  recipientName: string;
  recipientWalletCode: string;
  recipientWalletLabel: string;
  note: string;
};

export type TransactionResultParams = TransferDraft & {
  variant: TransactionResultVariant;
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
      <Stack.Screen name="OtpConfirm" component={OtpConfirmScreen} />
      <Stack.Screen name="TransactionResult" component={TransactionResultScreen} />
    </Stack.Navigator>
  );
}
