package com.paymentflow.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.paymentflow.ledger.domain.Direction;
import com.paymentflow.ledger.domain.LedgerEntry;
import com.paymentflow.ledger.domain.LedgerInvariants;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LedgerInvariantsTest {

    @Test
    void balancedDebitAndCreditPassesInvariant() {
        UUID txnId = UUID.randomUUID();
        LedgerEntry debit = new LedgerEntry(txnId, UUID.randomUUID(), Direction.DEBIT, 1000);
        LedgerEntry credit = new LedgerEntry(txnId, UUID.randomUUID(), Direction.CREDIT, 1000);

        assertThat(LedgerInvariants.isBalanced(List.of(debit, credit))).isTrue();
    }

    @Test
    void unbalancedEntriesFailInvariant() {
        UUID txnId = UUID.randomUUID();
        LedgerEntry debit = new LedgerEntry(txnId, UUID.randomUUID(), Direction.DEBIT, 1000);
        LedgerEntry credit = new LedgerEntry(txnId, UUID.randomUUID(), Direction.CREDIT, 999);

        assertThat(LedgerInvariants.isBalanced(List.of(debit, credit))).isFalse();
    }

    @Test
    void zeroSumEntriesAreNotConsideredBalanced() {
        assertThat(LedgerInvariants.isBalanced(List.of())).isFalse();
    }

    @Test
    void ledgerEntryRejectsNonPositiveAmount() {
        assertThatThrownBy(() -> new LedgerEntry(UUID.randomUUID(), UUID.randomUUID(), Direction.DEBIT, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LedgerEntry(UUID.randomUUID(), UUID.randomUUID(), Direction.DEBIT, -5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ledgerEntryHasNoMutationMethods() {
        // Immutability is structural: assert there are no setter-shaped public methods
        // on LedgerEntry beyond the constructor and getters (no update/delete path).
        long mutatorCount = java.util.Arrays.stream(LedgerEntry.class.getDeclaredMethods())
                .filter(m -> m.getName().startsWith("set"))
                .count();
        assertThat(mutatorCount).isZero();
    }
}
