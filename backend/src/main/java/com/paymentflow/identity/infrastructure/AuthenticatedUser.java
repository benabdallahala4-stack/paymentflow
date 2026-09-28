package com.paymentflow.identity.infrastructure;

import java.util.UUID;

/** Principal resolved from the JWT by {@link JwtAuthFilter}. */
public record AuthenticatedUser(UUID userId, String email, String role) {
}
