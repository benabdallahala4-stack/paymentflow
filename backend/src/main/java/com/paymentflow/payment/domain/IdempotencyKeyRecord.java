package com.paymentflow.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

/**
 * ADR-005: PK is (userId, idempotencyKey); a unique-constraint violation on concurrent
 * duplicate INSERTs (not check-then-insert) is what arbitrates the race - see
 * TransferService for the catch-and-replay handling. Uses persist() semantics (via
 * JpaRepository.saveAndFlush() on a new/detached entity with an assigned @EmbeddedId,
 * which Spring Data JPA routes to persist() because the entity is new) to guarantee a
 * real INSERT, per ADR-005's called-out pitfall.
 */
@Entity
@Table(name = "idempotency_keys")
public class IdempotencyKeyRecord {

    @EmbeddedId
    private Key id;

    @Column(name = "request_fingerprint", nullable = false)
    private String requestFingerprint;

    @Column(name = "transaction_id")
    private UUID transactionId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected IdempotencyKeyRecord() {
    }

    public IdempotencyKeyRecord(UUID userId, String idempotencyKey, String requestFingerprint, UUID transactionId) {
        this.id = new Key(userId, idempotencyKey);
        this.requestFingerprint = requestFingerprint;
        this.transactionId = transactionId;
    }

    public void linkTransaction(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public Key getId() {
        return id;
    }

    public String getRequestFingerprint() {
        return requestFingerprint;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    @Embeddable
    public static class Key implements Serializable {
        @Column(name = "user_id", nullable = false)
        private UUID userId;

        @Column(name = "idempotency_key", nullable = false)
        private String idempotencyKey;

        protected Key() {
        }

        public Key(UUID userId, String idempotencyKey) {
            this.userId = userId;
            this.idempotencyKey = idempotencyKey;
        }

        public UUID getUserId() {
            return userId;
        }

        public String getIdempotencyKey() {
            return idempotencyKey;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key key)) return false;
            return Objects.equals(userId, key.userId) && Objects.equals(idempotencyKey, key.idempotencyKey);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, idempotencyKey);
        }
    }
}
