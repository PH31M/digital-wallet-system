import { createContext, ReactNode, useCallback, useContext, useRef, useState } from 'react';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Toast, ToastType } from '../components/Toast';

type ToastState = { message: string; type: ToastType } | null;

type ToastContextValue = {
  showToast: (message: string, type?: ToastType) => void;
};

const ToastContext = createContext<ToastContextValue | null>(null);

const AUTO_HIDE_MS = 3000;

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toast, setToast] = useState<ToastState>(null);
  const hideTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  const showToast = useCallback((message: string, type: ToastType = 'info') => {
    if (hideTimer.current) clearTimeout(hideTimer.current);
    setToast({ message, type });
    hideTimer.current = setTimeout(() => setToast(null), AUTO_HIDE_MS);
  }, []);

  return (
    <ToastContext.Provider value={{ showToast }}>
      {children}
      {toast && (
        <SafeAreaView pointerEvents="none" className="absolute bottom-0 left-0 right-0 px-space-md pb-space-md">
          <Toast message={toast.message} type={toast.type} />
        </SafeAreaView>
      )}
    </ToastContext.Provider>
  );
}

export function useToast(): ToastContextValue {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast phải được dùng bên trong ToastProvider');
  return ctx;
}
