package com.digitalwallet.util;

import java.time.Instant;
import java.util.UUID;

/**
 * Utility for generating stable reference numbers.
 */
public final class ReferenceNumberGenerator {

    private ReferenceNumberGenerator() {
    }

    public static String generateReference() {
        return "REF-" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
