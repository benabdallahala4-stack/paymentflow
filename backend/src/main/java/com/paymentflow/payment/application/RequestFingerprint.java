package com.paymentflow.payment.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Hashes the normalized request body so a reused Idempotency-Key with a different payload is rejected (ADR-005). */
final class RequestFingerprint {

    private RequestFingerprint() {
    }

    static String of(String sourceAccountId, String destinationAccountId, long amountMinorUnits, String currency) {
        String normalized = sourceAccountId + "|" + destinationAccountId + "|" + amountMinorUnits + "|" + currency;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
