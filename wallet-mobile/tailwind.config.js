/**
 * Nguồn màu (M3 role tokens) và scale spacing/radius lấy nguyên từ `Design.md` (bản Stitch
 * export đã được duyệt) — xem src/theme/tokens.ts để biết bản JS thuần tương ứng, PHẢI khớp
 * 100% giữa 2 file này khi chỉnh sửa.
 *
 * `warning` và `info` không tồn tại trong bộ token M3 của Design.md (M3 chỉ có `error`), nên
 * lấy đúng giá trị đã ghi trong phần mô tả prose của Design.md / ui-frontend-design-spec.md.
 */
const colors = {
  surface: '#faf8ff',
  'surface-dim': '#d2d9f4',
  'surface-bright': '#faf8ff',
  'surface-container-lowest': '#ffffff',
  'surface-container-low': '#f2f3ff',
  'surface-container': '#eaedff',
  'surface-container-high': '#e2e7ff',
  'surface-container-highest': '#dae2fd',
  'surface-variant': '#dae2fd',
  'on-surface': '#131b2e',
  'on-surface-variant': '#44474f',
  'inverse-surface': '#283044',
  'inverse-on-surface': '#eef0ff',
  outline: '#747780',
  'outline-variant': '#c4c6d0',
  'surface-tint': '#465e8e',
  background: '#faf8ff',
  'on-background': '#131b2e',

  primary: '#00173b',
  'on-primary': '#ffffff',
  'primary-container': '#0f2c59',
  'on-primary-container': '#7c94c8',
  'inverse-primary': '#aec7fd',
  'primary-fixed': '#d8e2ff',
  'primary-fixed-dim': '#aec7fd',
  'on-primary-fixed': '#001a41',
  'on-primary-fixed-variant': '#2d4674',

  secondary: '#006c49',
  'on-secondary': '#ffffff',
  'secondary-container': '#7ef6be',
  'on-secondary-container': '#00714c',
  'secondary-fixed': '#81f9c1',
  'secondary-fixed-dim': '#63dca6',
  'on-secondary-fixed': '#002113',
  'on-secondary-fixed-variant': '#005236',

  tertiary: '#001545',
  'on-tertiary': '#ffffff',
  'tertiary-container': '#002771',
  'on-tertiary-container': '#688fff',
  'tertiary-fixed': '#dbe1ff',
  'tertiary-fixed-dim': '#b4c5ff',
  'on-tertiary-fixed': '#00174b',
  'on-tertiary-fixed-variant': '#003ea8',

  error: '#ba1a1a',
  'on-error': '#ffffff',
  'error-container': '#ffdad6',
  'on-error-container': '#93000a',
  danger: '#ba1a1a',

  warning: '#F59E0B',
  info: '#2563EB',
};

const typography = {
  'display-hero': ['40px', { lineHeight: '48px', letterSpacing: '-0.8px', fontWeight: '700' }],
  'display-hero-mobile': ['30px', { lineHeight: '38px', letterSpacing: '-0.45px', fontWeight: '700' }],
  'headline-xl': ['32px', { lineHeight: '40px', letterSpacing: '-0.64px', fontWeight: '700' }],
  'headline-lg': ['24px', { lineHeight: '32px', letterSpacing: '-0.36px', fontWeight: '600' }],
  'headline-md': ['20px', { lineHeight: '28px', letterSpacing: '-0.2px', fontWeight: '600' }],
  'headline-sm': ['16px', { lineHeight: '24px', fontWeight: '600' }],
  'body-lg': ['16px', { lineHeight: '24px', fontWeight: '400' }],
  'body-md': ['14px', { lineHeight: '20px', fontWeight: '400' }],
  'body-sm': ['12px', { lineHeight: '16px', fontWeight: '400' }],
  'numeric-balance': ['36px', { lineHeight: '44px', letterSpacing: '-0.72px', fontWeight: '700' }],
  'numeric-balance-mobile': ['28px', { lineHeight: '36px', letterSpacing: '-0.28px', fontWeight: '700' }],
  'numeric-ledger': ['15px', { lineHeight: '20px', fontWeight: '600' }],
  'label-md': ['14px', { lineHeight: '18px', fontWeight: '500' }],
  'label-sm': ['12px', { lineHeight: '16px', letterSpacing: '0.24px', fontWeight: '600' }],
  'label-caption': ['10px', { lineHeight: '14px', letterSpacing: '0.4px', fontWeight: '600' }],
};

const fontFamily = {
  'display-hero': ['BeVietnamPro_700Bold'],
  'display-hero-mobile': ['BeVietnamPro_700Bold'],
  'headline-xl': ['BeVietnamPro_700Bold'],
  'headline-lg': ['BeVietnamPro_600SemiBold'],
  'headline-md': ['BeVietnamPro_600SemiBold'],
  'headline-sm': ['BeVietnamPro_600SemiBold'],
  'body-lg': ['BeVietnamPro_400Regular'],
  'body-md': ['BeVietnamPro_400Regular'],
  'body-sm': ['BeVietnamPro_400Regular'],
  'numeric-balance': ['BeVietnamPro_700Bold'],
  'numeric-balance-mobile': ['BeVietnamPro_700Bold'],
  'numeric-ledger': ['BeVietnamPro_600SemiBold'],
  'label-md': ['BeVietnamPro_500Medium'],
  'label-sm': ['BeVietnamPro_600SemiBold'],
  'label-caption': ['BeVietnamPro_600SemiBold'],
};

/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./App.tsx', './src/**/*.{js,jsx,ts,tsx}'],
  presets: [require('nativewind/preset')],
  theme: {
    extend: {
      colors,
      fontSize: typography,
      fontFamily,
      borderRadius: {
        sm: '4px',
        DEFAULT: '8px',
        md: '12px',
        lg: '16px',
        xl: '24px',
        full: '9999px',
      },
      spacing: {
        gutter: '24px',
        margin: '32px',
        'gutter-mobile': '16px',
        'margin-mobile': '16px',
        'space-xs': '4px',
        'space-sm': '8px',
        'space-md': '16px',
        'space-lg': '24px',
        'space-xl': '32px',
        'space-2xl': '48px',
      },
    },
  },
  plugins: [],
};
