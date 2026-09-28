package com.paymentflow.payment.infrastructure;

import com.paymentflow.payment.domain.IdempotencyKeyRecord;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKeyRecord, IdempotencyKeyRecord.Key> {

    @Query("select r from IdempotencyKeyRecord r where r.id.userId = :userId and r.id.idempotencyKey = :idempotencyKey")
    Optional<IdempotencyKeyRecord> findByUserIdAndIdempotencyKey(@Param("userId") UUID userId,
                                                                  @Param("idempotencyKey") String idempotencyKey);
}
