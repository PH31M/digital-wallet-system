import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { ComponentShowcaseScreen } from '../screens/ComponentShowcaseScreen';
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
  Main: undefined;
  ComponentShowcase: undefined;
  Transfer: undefined;
  OtpConfirm: TransferDraft;
  TransactionResult: TransactionResultParams;
};

const Stack = createNativeStackNavigator<RootStackParamList>();

export function RootNavigator() {
  return (
    <Stack.Navigator initialRouteName="Main" screenOptions={{ headerShown: false }}>
      <Stack.Screen name="Main" component={MainTabs} />
      <Stack.Screen name="ComponentShowcase" component={ComponentShowcaseScreen} />
      <Stack.Screen name="Transfer" component={TransferScreen} />
      <Stack.Screen name="OtpConfirm" component={OtpConfirmScreen} />
      <Stack.Screen name="TransactionResult" component={TransactionResultScreen} />
    </Stack.Navigator>
  );
}
