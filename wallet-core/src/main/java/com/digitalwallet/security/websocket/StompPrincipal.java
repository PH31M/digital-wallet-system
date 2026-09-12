package com.digitalwallet.security.websocket;

import java.security.Principal;
import java.util.UUID;

public class StompPrincipal implements Principal {

    private final UUID publicId;

    public StompPrincipal(UUID publicId) {
        this.publicId = publicId;
    }

    @Override
    public String getName() {
        return publicId.toString();
    }

    public UUID getPublicId() {
        return publicId;
    }
}