/**
 * Nguồn sự thật (JS thuần) cho design tokens — PHẢI khớp 100% với `tailwind.config.js` ở root.
 * Dùng file này ở những chỗ cần giá trị JS trực tiếp (vd. truyền `color` cho icon), còn lại
 * ưu tiên dùng className NativeWind (`bg-primary-container`, `text-on-surface`...).
 */

export const colors = {
  surface: '#faf8ff',
  surfaceDim: '#d2d9f4',
  surfaceBright: '#faf8ff',
  surfaceContainerLowest: '#ffffff',
  surfaceContainerLow: '#f2f3ff',
  surfaceContainer: '#eaedff',
  surfaceContainerHigh: '#e2e7ff',
  surfaceContainerHighest: '#dae2fd',
  surfaceVariant: '#dae2fd',
  onSurface: '#131b2e',
  onSurfaceVariant: '#44474f',
  inverseSurface: '#283044',
  inverseOnSurface: '#eef0ff',
  outline: '#747780',
  outlineVariant: '#c4c6d0',
  surfaceTint: '#465e8e',
  background: '#faf8ff',
  onBackground: '#131b2e',

  primary: '#00173b',
  onPrimary: '#ffffff',
  primaryContainer: '#0f2c59',
  onPrimaryContainer: '#7c94c8',
  inversePrimary: '#aec7fd',
  primaryFixed: '#d8e2ff',
  primaryFixedDim: '#aec7fd',
  onPrimaryFixed: '#001a41',
  onPrimaryFixedVariant: '#2d4674',

  secondary: '#006c49',
  onSecondary: '#ffffff',
  secondaryContainer: '#7ef6be',
  onSecondaryContainer: '#00714c',
  secondaryFixed: '#81f9c1',
  secondaryFixedDim: '#63dca6',
  onSecondaryFixed: '#002113',
  onSecondaryFixedVariant: '#005236',

  tertiary: '#001545',
  onTertiary: '#ffffff',
  tertiaryContainer: '#002771',
  onTertiaryContainer: '#688fff',
  tertiaryFixed: '#dbe1ff',
  tertiaryFixedDim: '#b4c5ff',
  onTertiaryFixed: '#00174b',
  onTertiaryFixedVariant: '#003ea8',

  error: '#ba1a1a',
  onError: '#ffffff',
  errorContainer: '#ffdad6',
  onErrorContainer: '#93000a',
  danger: '#ba1a1a',

  // Không có trong bộ M3 token của Design.md — lấy từ prose Design.md / ui-frontend-design-spec.md
  warning: '#F59E0B',
  info: '#2563EB',
} as const;

export type TypographyStyle = {
  fontFamily: string;
  fontSize: number;
  lineHeight: number;
  letterSpacing: number;
  fontWeight: '400' | '500' | '600' | '700';
};

export const typography: Record<string, TypographyStyle> = {
  displayHero: { fontFamily: 'BeVietnamPro_700Bold', fontSize: 40, lineHeight: 48, letterSpacing: -0.8, fontWeight: '700' },
  displayHeroMobile: { fontFamily: 'BeVietnamPro_700Bold', fontSize: 30, lineHeight: 38, letterSpacing: -0.45, fontWeight: '700' },
  headlineXl: { fontFamily: 'BeVietnamPro_700Bold', fontSize: 32, lineHeight: 40, letterSpacing: -0.64, fontWeight: '700' },
  headlineLg: { fontFamily: 'BeVietnamPro_600SemiBold', fontSize: 24, lineHeight: 32, letterSpacing: -0.36, fontWeight: '600' },
  headlineMd: { fontFamily: 'BeVietnamPro_600SemiBold', fontSize: 20, lineHeight: 28, letterSpacing: -0.2, fontWeight: '600' },
  headlineSm: { fontFamily: 'BeVietnamPro_600SemiBold', fontSize: 16, lineHeight: 24, letterSpacing: 0, fontWeight: '600' },
  bodyLg: { fontFamily: 'BeVietnamPro_400Regular', fontSize: 16, lineHeight: 24, letterSpacing: 0, fontWeight: '400' },
  bodyMd: { fontFamily: 'BeVietnamPro_400Regular', fontSize: 14, lineHeight: 20, letterSpacing: 0, fontWeight: '400' },
  bodySm: { fontFamily: 'BeVietnamPro_400Regular', fontSize: 12, lineHeight: 16, letterSpacing: 0, fontWeight: '400' },
  numericBalance: { fontFamily: 'BeVietnamPro_700Bold', fontSize: 36, lineHeight: 44, letterSpacing: -0.72, fontWeight: '700' },
  numericBalanceMobile: { fontFamily: 'BeVietnamPro_700Bold', fontSize: 28, lineHeight: 36, letterSpacing: -0.28, fontWeight: '700' },
  numericLedger: { fontFamily: 'BeVietnamPro_600SemiBold', fontSize: 15, lineHeight: 20, letterSpacing: 0, fontWeight: '600' },
  labelMd: { fontFamily: 'BeVietnamPro_500Medium', fontSize: 14, lineHeight: 18, letterSpacing: 0, fontWeight: '500' },
  labelSm: { fontFamily: 'BeVietnamPro_600SemiBold', fontSize: 12, lineHeight: 16, letterSpacing: 0.24, fontWeight: '600' },
  labelCaption: { fontFamily: 'BeVietnamPro_600SemiBold', fontSize: 10, lineHeight: 14, letterSpacing: 0.4, fontWeight: '600' },
};

export const radius = {
  sm: 4,
  DEFAULT: 8,
  md: 12,
  lg: 16,
  xl: 24,
  full: 9999,
} as const;

export const spacing = {
  gutter: 24,
  margin: 32,
  gutterMobile: 16,
  marginMobile: 16,
  xs: 4,
  sm: 8,
  md: 16,
  lg: 24,
  xl: 32,
  '2xl': 48,
} as const;
