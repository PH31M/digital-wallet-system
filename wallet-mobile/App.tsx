import './global.css';
import { useCallback } from 'react';
import { View } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { NavigationContainer } from '@react-navigation/native';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import * as SplashScreen from 'expo-splash-screen';
import {
  useFonts,
  BeVietnamPro_400Regular,
  BeVietnamPro_500Medium,
  BeVietnamPro_600SemiBold,
  BeVietnamPro_700Bold,
} from '@expo-google-fonts/be-vietnam-pro';
import { RootNavigator } from './src/navigation/RootNavigator';
import { ToastProvider } from './src/hooks/useToast';
import { ConfirmProvider } from './src/hooks/useConfirm';
import { AuthProvider } from './src/auth/AuthContext';

SplashScreen.preventAutoHideAsync();

// Cài sẵn từ Phase 0, lần đầu thật sự dùng ở đây (Ví + số dư) — 1 instance dùng chung toàn app.
const queryClient = new QueryClient();

export default function App() {
  const [fontsLoaded] = useFonts({
    BeVietnamPro_400Regular,
    BeVietnamPro_500Medium,
    BeVietnamPro_600SemiBold,
    BeVietnamPro_700Bold,
  });

  const onLayoutRootView = useCallback(async () => {
    if (fontsLoaded) {
      await SplashScreen.hideAsync();
    }
  }, [fontsLoaded]);

  if (!fontsLoaded) {
    return null;
  }

  return (
    <SafeAreaProvider>
      <View style={{ flex: 1 }} onLayout={onLayoutRootView}>
        <QueryClientProvider client={queryClient}>
          <ToastProvider>
            <ConfirmProvider>
              <AuthProvider>
                <NavigationContainer>
                  <RootNavigator />
                </NavigationContainer>
              </AuthProvider>
            </ConfirmProvider>
          </ToastProvider>
        </QueryClientProvider>
      </View>
    </SafeAreaProvider>
  );
}
