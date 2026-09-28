package com.paymentflow.ledger.domain;

import java.util.List;

/**
 * Double-entry balance invariant: for any transaction, SUM(DEBIT) == SUM(CREDIT).
 * Postgres itself only enforces column-level CHECKs (amount > 0, direction in
 * (DEBIT,CREDIT)); the cross-row sum invariant is application logic, verified here and
 * exercised by unit + integration tests (per rule 5's documented scope decision).
 */
public final class LedgerInvariants {

    private LedgerInvariants() {
    }

    public static boolean isBalanced(List<LedgerEntry> entries) {
        long debits = entries.stream()
                .filter(e -> e.getDirection() == Direction.DEBIT)
                .mapToLong(LedgerEntry::getAmountMinorUnits)
                .sum();
        long credits = entries.stream()
                .filter(e -> e.getDirection() == Direction.CREDIT)
                .mapToLong(LedgerEntry::getAmountMinorUnits)
                .sum();
        return debits == credits && debits > 0;
    }
}
