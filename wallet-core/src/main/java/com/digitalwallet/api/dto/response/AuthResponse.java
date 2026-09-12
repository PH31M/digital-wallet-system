package com.digitalwallet.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthResponse {

    @JsonProperty("access_token")
    private final String accessToken;

    @JsonProperty("refresh_token")
    private final String refreshToken;

    private final UserProfileResponse user;

    @JsonProperty("mfa_required")
    private final boolean mfaRequired;

    public AuthResponse(String accessToken, String refreshToken, UserProfileResponse user) {
        this(accessToken, refreshToken, user, false);
    }

    public AuthResponse(String accessToken, String refreshToken, UserProfileResponse user, boolean mfaRequired) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.user = user;
        this.mfaRequired = mfaRequired;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public UserProfileResponse getUser() {
        return user;
    }

    public boolean isMfaRequired() {
        return mfaRequired;
    }
}