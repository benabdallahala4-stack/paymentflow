package com.paymentflow.payment.infrastructure;

import com.paymentflow.payment.domain.Transaction;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    /**
     * Cursor (keyset) pagination per ADR-008: created_at DESC, id DESC, no OFFSET.
     * naive alternative would have been Pageable/OFFSET-LIMIT, which drifts under
     * concurrent inserts and gets slower as the offset grows - not used here.
     */
    @Query("""
            select t from Transaction t
            where t.initiatedByUserId = :userId
              and (t.createdAt < :createdAt or (t.createdAt = :createdAt and t.id < :id))
            order by t.createdAt desc, t.id desc
            """)
    List<Transaction> findPageAfter(@Param("userId") UUID userId,
                                     @Param("createdAt") Instant createdAt,
                                     @Param("id") UUID id,
                                     org.springframework.data.domain.Pageable pageable);

    @Query("""
            select t from Transaction t
            where t.initiatedByUserId = :userId
            order by t.createdAt desc, t.id desc
            """)
    List<Transaction> findFirstPage(@Param("userId") UUID userId, org.springframework.data.domain.Pageable pageable);
}
