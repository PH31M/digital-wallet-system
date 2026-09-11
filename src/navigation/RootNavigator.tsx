import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { ComponentShowcaseScreen } from '../screens/ComponentShowcaseScreen';
import { MainTabs } from './MainTabs';

export type RootStackParamList = {
  Main: undefined;
  ComponentShowcase: undefined;
};

const Stack = createNativeStackNavigator<RootStackParamList>();

export function RootNavigator() {
  return (
    <Stack.Navigator initialRouteName="Main" screenOptions={{ headerShown: false }}>
      <Stack.Screen name="Main" component={MainTabs} />
      <Stack.Screen name="ComponentShowcase" component={ComponentShowcaseScreen} />
    </Stack.Navigator>
  );
}
