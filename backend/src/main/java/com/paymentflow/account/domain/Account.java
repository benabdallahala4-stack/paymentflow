package com.paymentflow.account.domain;

import com.paymentflow.shared.domain.BusinessRuleViolationException;
import com.paymentflow.shared.domain.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Account read model (docs/architecture/domain-model.md): balanceMinorUnits is a fast
 * read model, reconstructible as SUM(ledger_entries) for this account - not the system
 * of record. @Version enables optimistic locking (ADR-004): the default production
 * concurrency strategy for balance updates, with retry-with-backoff at the application
 * service layer (see payment.application.TransferService).
 */
@Entity
@Table(name = "accounts")
public class Account {

    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.CHAR)
    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "balance_minor_units", nullable = false)
    private long balanceMinorUnits;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;

    @Version
    @Column(nullable = false)
    private long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Account() {
    }

    public Account(UUID ownerId, String currency) {
        this.id = UUID.randomUUID();
        this.ownerId = ownerId;
        this.currency = currency;
        this.balanceMinorUnits = 0L;
        this.status = AccountStatus.ACTIVE;
    }

    public void debit(Money amount) {
        requireActive();
        requireSameCurrency(amount);
        if (balanceMinorUnits < amount.amountMinorUnits()) {
            throw new BusinessRuleViolationException("insufficient funds on account " + id);
        }
        this.balanceMinorUnits -= amount.amountMinorUnits();
    }

    public void credit(Money amount) {
        requireActive();
        requireSameCurrency(amount);
        this.balanceMinorUnits += amount.amountMinorUnits();
    }

    private void requireActive() {
        if (status != AccountStatus.ACTIVE) {
            throw new BusinessRuleViolationException("account " + id + " is not ACTIVE (status=" + status + ")");
        }
    }

    private void requireSameCurrency(Money amount) {
        if (!this.currency.equals(amount.currency())) {
            throw new BusinessRuleViolationException("currency mismatch on account " + id);
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getCurrency() {
        return currency;
    }

    public long getBalanceMinorUnits() {
        return balanceMinorUnits;
    }

    public Money getBalance() {
        return Money.of(balanceMinorUnits, currency);
    }

    public AccountStatus getStatus() {
        return status;
    }

    public long getVersion() {
        return version;
    }
}
