package com.digitalwallet.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Optional;

/**
 * Xác thực Google Sign-In ID token cho POST /api/auth/google — KHÔNG phải OAuth2 authorization
 * code flow đầy đủ (không cần redirect/callback ở backend): mobile client tự lấy id_token từ
 * Google (qua expo-auth-session), backend chỉ verify chữ ký/issuer/audience/hạn của token đó.
 * Dùng thư viện chính thức của Google thay vì tự parse JWT + tự fetch/cache JWKS, vì xác thực
 * sai ở bước này là lỗ hổng bảo mật thật (vd. quên check audience sẽ cho phép token của app
 * Google khác đăng nhập vào đây).
 */
@Service
public class GoogleTokenVerifierService {

    private final GoogleIdTokenVerifier verifier;
    private final boolean configured;

    public GoogleTokenVerifierService(@Value("${google.client-id:}") String clientId) throws GeneralSecurityException, IOException {
        this.configured = clientId != null && !clientId.isBlank();
        this.verifier = configured
                ? new GoogleIdTokenVerifier.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance())
                        .setAudience(Collections.singletonList(clientId))
                        .build()
                : null;
    }

    /** Rỗng nếu chưa cấu hình `GOOGLE_CLIENT_ID`, token không hợp lệ, hoặc email chưa được Google xác thực. */
    public Optional<GooglePayload> verify(String idTokenString) {
        if (!configured) {
            return Optional.empty();
        }
        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                return Optional.empty();
            }
            GoogleIdToken.Payload payload = idToken.getPayload();
            if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
                return Optional.empty();
            }
            String name = (String) payload.get("name");
            return Optional.of(new GooglePayload(payload.getEmail(), name, payload.getSubject()));
        } catch (GeneralSecurityException | IOException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public record GooglePayload(String email, String name, String subject) {
    }
}
