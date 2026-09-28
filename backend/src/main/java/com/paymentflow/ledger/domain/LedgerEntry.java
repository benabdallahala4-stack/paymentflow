package com.paymentflow.ledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Immutable, append-only double-entry posting (domain-model.md). No setters, no update
 * path anywhere in the codebase: {@link com.paymentflow.ledger.infrastructure.LedgerEntryRepository}
 * deliberately exposes no update/delete methods. Corrections must be new reversing
 * entries referencing the original transaction, never a mutation of this row.
 */
@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {

    @Id
    private UUID id;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Direction direction;

    @Column(name = "amount_minor_units", nullable = false)
    private long amountMinorUnits;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected LedgerEntry() {
    }

    public LedgerEntry(UUID transactionId, UUID accountId, Direction direction, long amountMinorUnits) {
        if (amountMinorUnits <= 0) {
            throw new IllegalArgumentException("ledger entry amount must be positive");
        }
        this.id = UUID.randomUUID();
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.direction = direction;
        this.amountMinorUnits = amountMinorUnits;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public Direction getDirection() {
        return direction;
    }

    public long getAmountMinorUnits() {
        return amountMinorUnits;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
