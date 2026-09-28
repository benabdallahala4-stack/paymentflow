package com.paymentflow.account.infrastructure;

import com.paymentflow.account.domain.Account;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    List<Account> findByOwnerId(UUID ownerId);

    // Default production path: plain read + @Version-checked write (optimistic locking).
    Optional<Account> findById(UUID id);

    /**
     * Pessimistic-locking alternative (ADR-004 concurrency-lab), reserved for
     * identified hot rows / experiment code - not the default production path. Mirrors
     * the postgres-concurrency-control FOR UPDATE pattern. When locking two accounts in
     * one transfer, callers must acquire locks in a consistent order (min(id), max(id))
     * to avoid deadlocks.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id")
    Optional<Account> findByIdForUpdate(UUID id);
}
