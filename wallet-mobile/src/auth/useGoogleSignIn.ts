import { useEffect } from 'react';
import * as AuthSession from 'expo-auth-session';
import * as WebBrowser from 'expo-web-browser';

WebBrowser.maybeCompleteAuthSession();

const GOOGLE_DISCOVERY = { authorizationEndpoint: 'https://accounts.google.com/o/oauth2/v2/auth' };
const GOOGLE_CLIENT_ID = process.env.EXPO_PUBLIC_GOOGLE_CLIENT_ID;

/**
 * expo-auth-session/providers/google (wrapper tiện lợi cho Google) đã bị Expo đánh dấu
 * deprecated — tài liệu hiện khuyến nghị dùng thư viện native riêng của Google
 * (@react-native-google-signin/google-signin), nhưng thư viện đó cần custom dev client (KHÔNG
 * chạy được trong Expo Go/Expo Web, xem AGENTS.md "Expo đã thay đổi"). Vì cách kiểm thử chính
 * của app này vẫn là Expo Web/Expo Go (chưa build native — xem README), dùng thẳng
 * `AuthSession.useAuthRequest` (API chung, KHÔNG bị deprecated) với discovery document của
 * Google + `responseType: IdToken` — chạy được trên Expo Web ngay, sẵn sàng cho native khi có
 * dev client (đã khai báo `scheme: "pmpay"` trong app.json).
 *
 * Google bắt buộc `nonce` cho response_type=id_token (OIDC implicit flow) mới chịu phát hành
 * token — sinh 1 chuỗi ngẫu nhiên mỗi lần mở màn hình. KHÔNG gửi lại nonce này cho backend để
 * verify lại (backend chỉ verify chữ ký/issuer/audience/hạn qua GoogleTokenVerifierService) —
 * đơn giản hoá có chủ đích cho quy mô project này, id_token dùng ngay lập tức trong 1 luồng
 * đồng bộ nên rủi ro replay thấp, không phải hệ thống ngân hàng thật cần chống replay tuyệt đối.
 */
export function useGoogleSignIn() {
  const [request, response, promptAsync] = AuthSession.useAuthRequest(
    {
      clientId: GOOGLE_CLIENT_ID ?? '',
      redirectUri: AuthSession.makeRedirectUri({ scheme: 'pmpay' }),
      scopes: ['openid', 'profile', 'email'],
      responseType: AuthSession.ResponseType.IdToken,
      extraParams: { nonce: generateNonce() },
    },
    GOOGLE_DISCOVERY,
  );

  return { request, response, promptAsync, isConfigured: Boolean(GOOGLE_CLIENT_ID) };
}

export function extractIdToken(response: AuthSession.AuthSessionResult | null): string | null {
  if (response?.type !== 'success') {
    return null;
  }
  return response.params.id_token ?? null;
}

function generateNonce(): string {
  return Math.random().toString(36).slice(2) + Date.now().toString(36);
}
