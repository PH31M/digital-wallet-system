import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { ComponentShowcaseScreen } from '../screens/ComponentShowcaseScreen';

export type RootStackParamList = {
  ComponentShowcase: undefined;
};

const Stack = createNativeStackNavigator<RootStackParamList>();

export function RootNavigator() {
  return (
    <Stack.Navigator screenOptions={{ headerShown: false }}>
      <Stack.Screen name="ComponentShowcase" component={ComponentShowcaseScreen} />
    </Stack.Navigator>
  );
}
