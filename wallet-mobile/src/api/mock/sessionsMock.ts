import { IconName } from '../../components/Icon';

const now = new Date();

function daysAgo(days: number, hours: number, minutes: number): Date {
  const d = new Date(now);
  d.setDate(d.getDate() - days);
  d.setHours(hours, minutes, 0, 0);
  return d;
}

function pad2(n: number): string {
  return n.toString().padStart(2, '0');
}

function formatDDMMYYYY(date: Date): string {
  return `${pad2(date.getDate())}/${pad2(date.getMonth() + 1)}/${date.getFullYear()}`;
}

function formatTime(date: Date): string {
  return `${pad2(date.getHours())}:${pad2(date.getMinutes())}`;
}

/** "Hôm nay, 10:24 (10/09/2026)" / "08/09/2026, 18:30" — khớp đúng 2 kiểu copy trong code.html. */
export function formatSessionActivity(date: Date, style: 'relative' | 'absolute'): string {
  const time = formatTime(date);
  const dateLabel = formatDDMMYYYY(date);
  if (style === 'absolute') return `${dateLabel}, ${time}`;

  const isToday = date.toDateString() === now.toDateString();
  return isToday ? `Hôm nay, ${time} (${dateLabel})` : `${dateLabel}, ${time}`;
}

export const mockCurrentDevice = {
  name: 'iPhone 15 Pro Max',
  appVersion: 'PMPay iOS v3.4.2',
  icon: 'stay_current_portrait' as IconName,
  location: 'Cầu Giấy, Hà Nội',
  ip: '118.70.12.*',
  authMethod: 'Xác thực FaceID & 2FA',
};

export type MockSession = {
  id: string;
  name: string;
  subtitle: string;
  icon: IconName;
  location?: string;
  ip?: string;
  lastActiveAt: Date;
  isActive: boolean;
};

export const mockOtherSessions: MockSession[] = [
  {
    id: 'session-macbook',
    name: 'MacBook Pro 14" (macOS Sonoma)',
    subtitle: 'Trình duyệt: Chrome 128.0 (Web Portal)',
    icon: 'laptop_mac',
    location: 'Ba Đình, Hà Nội',
    ip: '113.190.23.*',
    lastActiveAt: daysAgo(0, 10, 24),
    isActive: true,
  },
  {
    id: 'session-ipad',
    name: 'iPad Air 5 (iPadOS 17)',
    subtitle: 'Ứng dụng: PMPay Tablet Edition',
    icon: 'tablet_mac',
    location: 'TP. Hồ Chí Minh',
    ip: '14.161.45.*',
    lastActiveAt: daysAgo(2, 18, 30),
    isActive: true,
  },
  {
    id: 'session-windows',
    name: 'Windows PC (Edge 126)',
    subtitle: 'Đà Nẵng (IP: 42.119.88.*)',
    icon: 'desktop_windows',
    lastActiveAt: daysAgo(15, 9, 12),
    isActive: false,
  },
];
