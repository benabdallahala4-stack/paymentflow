package com.paymentflow.shared.domain;

import java.util.Objects;

/**
 * Money value object: BIGINT minor units + currency (ADR-003).
 *
 * <p>Never a floating point type. Arithmetic across different currencies is a domain
 * error, checked explicitly here since {@code long} alone can't prevent it.
 */
public record Money(long amountMinorUnits, String currency) {

    public Money {
        Objects.requireNonNull(currency, "currency");
        if (currency.length() != 3) {
            throw new IllegalArgumentException("currency must be a 3-letter ISO code: " + currency);
        }
    }

    public static Money of(long amountMinorUnits, String currency) {
        return new Money(amountMinorUnits, currency);
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(Math.addExact(this.amountMinorUnits, other.amountMinorUnits), currency);
    }

    public Money subtract(Money other) {
        requireSameCurrency(other);
        return new Money(Math.subtractExact(this.amountMinorUnits, other.amountMinorUnits), currency);
    }

    public boolean isPositive() {
        return amountMinorUnits > 0;
    }

    public boolean isGreaterThanOrEqualTo(Money other) {
        requireSameCurrency(other);
        return this.amountMinorUnits >= other.amountMinorUnits;
    }

    private void requireSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "cannot operate on different currencies: " + this.currency + " vs " + other.currency);
        }
    }

    /** Presentation-boundary-only conversion (2 decimal places for EUR/USD). */
    public double toMajorUnitsForDisplay() {
        return amountMinorUnits / 100.0;
    }
}
