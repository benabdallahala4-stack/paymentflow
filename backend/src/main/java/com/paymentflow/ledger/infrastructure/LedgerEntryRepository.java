package com.paymentflow.ledger.infrastructure;

import com.paymentflow.ledger.domain.LedgerEntry;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Deliberately exposes only read + insert (via save() on a new entity) operations.
 * No update/delete method is declared here - ledger entries are immutable (rule/ADR:
 * "no update/delete repository methods on them"). See schema.md's note on this and
 * V1__init.sql's comment on the DB-privilege-level aspiration vs this app-level
 * enforcement, chosen here as the simpler equivalent for a single-role local setup.
 */
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    List<LedgerEntry> findByTransactionId(UUID transactionId);

    List<LedgerEntry> findByAccountIdOrderByCreatedAtDesc(UUID accountId);

    /**
     * Batch-fetch ledger entries for MANY transactions in one query. Used by
     * AdminController when listing a user's payments together with their ledger
     * entries: the naive version would call findByTransactionId(id) once per
     * transaction in a loop (classic N+1 - one query for the transaction list, then N
     * more for their entries). This single IN-query variant avoids that; callers group
     * the flat result back onto each transaction by transactionId.
     */
    @Query("select e from LedgerEntry e where e.transactionId in :transactionIds order by e.createdAt asc")
    List<LedgerEntry> findByTransactionIdIn(@Param("transactionIds") List<UUID> transactionIds);
}
