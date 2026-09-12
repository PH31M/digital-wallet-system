import Svg, { Circle, Defs, LinearGradient, Path, Rect, Stop } from 'react-native-svg';

type LogoProps = {
  size?: number;
};

/** Port 1:1 từ `UI Digital wallet system/Logo-M-PMPay/code.html` (SVG gốc đã duyệt). */
export function Logo({ size = 40 }: LogoProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 100 100">
      <Defs>
        <LinearGradient id="mGrad" x1="0%" y1="0%" x2="100%" y2="100%">
          <Stop offset="0%" stopColor="#FFFFFF" />
          <Stop offset="100%" stopColor="#E2E8F0" />
        </LinearGradient>
      </Defs>
      <Rect width="100" height="100" rx="24" fill="#0F2C59" />
      <Path
        d="M27 72 V30 C27 28.5 28.5 27.5 29.8 28.2 L50 43.5 L70.2 28.2 C71.5 27.5 73 28.5 73 30 V72 C73 73.1 72.1 74 71 74 H64 C62.9 74 62 73.1 62 72 V43.5 L53 50.5 C51.2 51.9 48.8 51.9 47 50.5 L38 43.5 V72 C38 73.1 37.1 74 36 74 H29 C27.9 74 27 73.1 27 72 Z"
        fill="url(#mGrad)"
      />
      <Circle cx="78" cy="24" r="6" fill="#0E9F6E" />
      <Circle cx="78" cy="24" r="3" fill="#34D399" />
      <Rect x="27" y="78" width="46" height="4" rx="2" fill="#0E9F6E" />
    </Svg>
  );
}
