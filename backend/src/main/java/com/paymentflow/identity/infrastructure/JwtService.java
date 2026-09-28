package com.paymentflow.identity.infrastructure;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Minimal JWT issuance/validation (ADR-007). A single HMAC-signed access token per
 * login; no refresh-token rotation store, no external IdP - the simplest reasonable
 * choice for a portfolio demo. Full OAuth2/OIDC is explicitly out of scope.
 */
@Component
public class JwtService {

    private final Key key;
    private final long expirationMinutes;

    public JwtService(
            @Value("${paymentflow.jwt.secret}") String secret,
            @Value("${paymentflow.jwt.expiration-minutes:60}") long expirationMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationMinutes = expirationMinutes;
    }

    public String issueToken(UUID userId, String email, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith((javax.crypto.SecretKey) key).build()
                .parseSignedClaims(token).getPayload();
    }
}
