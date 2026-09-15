# wallet-mobile

Expo (React Native + TypeScript) client for the digital wallet — "PMPay". Talks to [`wallet-core`](../wallet-core) over its real REST API (no mock data). See the [root README](../README.md) for the project overview.

## Stack

Expo SDK 57 · React 19 · React Native 0.86 · TypeScript (strict) · NativeWind (Tailwind for RN) · React Navigation · TanStack Query · axios

## Structure

```
src/
  api/          axios client (JWT interceptor, single-flight token refresh), typed endpoint
                calls (authApi/walletApi/notificationApi), snake_case<->camelCase transform,
                secure token storage (expo-secure-store)
  auth/         AuthContext (session state) + useGoogleSignIn (expo-auth-session Google OIDC flow)
  screens/      19 screens: auth (login/register/OTP/forgot-password/MFA), home, transfer,
                deposit (incl. MoMo)/withdraw with OTP confirmation, history, notifications,
                profile, active sessions, transaction detail/result
  components/   shared UI (Button, Input, TransactionRow, OTP entry panel, headers...)
  navigation/   RootNavigator (stack) + MainTabs (bottom tabs)
  theme/        design tokens (colors, typography, spacing) — mirrors tailwind.config.js
  types/api.ts  types matching the backend DTOs exactly, including known backend quirks
                (documented inline — e.g. which fields are always null)
```

## Running it

```bash
npm install
npm run web       # Expo Web at http://localhost:8081 — fastest way to try it
npm run android   # Expo Go on an Android emulator/device
npm run ios       # Expo Go on iOS Simulator
```

`.env` controls the backend URL (`EXPO_PUBLIC_API_BASE_URL`). Leave it unset for the built-in default (Android emulator gets `10.0.2.2`, everything else gets `localhost`); set it explicitly to your machine's LAN IP for a real device on the same Wi-Fi, or to a tunnel URL (`ngrok http 8089`) for a real device on a different network. See the comment block in `src/api/client.ts` for the full rundown.

From SDK 57 onward, running in Expo Go also requires being logged in to both the Expo CLI (`npx expo login`) and the Expo Go app itself with the same account — that's an Expo platform requirement, not specific to this project.

"Đăng nhập với Google" needs `EXPO_PUBLIC_GOOGLE_CLIENT_ID` set to the same OAuth Client ID as `wallet-core`'s `GOOGLE_CLIENT_ID` (see that README) — leave it blank and the button just shows a "not configured" toast instead of erroring. Works on Expo Web out of the box (`src/auth/useGoogleSignIn.ts`); on native it needs a custom dev client, since Expo Go can't handle OAuth's custom redirect scheme at all.

## Testing

```bash
npx tsc --noEmit   # strict type-check
npx jest           # unit tests (case transform, single-flight refresh dedup)
```

Screen-level behavior has been verified manually through Expo Web/Expo Go for every flow (register → OTP → home, deposit/withdraw/transfer including the real OTP and fraud-review thresholds, MoMo top-up, notifications, session management) rather than with automated E2E tests — that's the main testing gap if you're picking this up next.

## Known gaps

- Native (App Store/Play Store) builds haven't been produced — only tested via Expo Web/Expo Go.
- No automated screen/E2E tests, only unit tests for the API layer.
- A handful of "coming soon" placeholders in Profile (bank linking, in-app password change, PIN/Smart OTP, notification settings) aren't wired up — they weren't part of the original 18-screen design export.
