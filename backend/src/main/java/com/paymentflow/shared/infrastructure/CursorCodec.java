package com.paymentflow.shared.infrastructure;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Opaque keyset-pagination cursor encoding (createdAt, id) per ADR-008.
 * Clients must treat this as opaque; do not let them construct arbitrary offsets.
 */
public final class CursorCodec {

    private CursorCodec() {
    }

    public record Cursor(Instant createdAt, UUID id) {
    }

    public static String encode(Instant createdAt, UUID id) {
        String raw = createdAt.toEpochMilli() + ":" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static Cursor decode(String cursor) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            int sep = raw.indexOf(':');
            Instant createdAt = Instant.ofEpochMilli(Long.parseLong(raw.substring(0, sep)));
            UUID id = UUID.fromString(raw.substring(sep + 1));
            return new Cursor(createdAt, id);
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid cursor", e);
        }
    }
}
