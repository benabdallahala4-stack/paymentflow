package com.paymentflow.payment.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentflow.account.domain.Account;
import com.paymentflow.account.infrastructure.AccountRepository;
import com.paymentflow.ledger.domain.Direction;
import com.paymentflow.ledger.domain.LedgerEntry;
import com.paymentflow.ledger.domain.LedgerInvariants;
import com.paymentflow.ledger.infrastructure.LedgerEntryRepository;
import com.paymentflow.payment.domain.IdempotencyKeyRecord;
import com.paymentflow.payment.domain.OutboxEvent;
import com.paymentflow.payment.domain.Transaction;
import com.paymentflow.payment.infrastructure.IdempotencyKeyRepository;
import com.paymentflow.payment.infrastructure.OutboxEventRepository;
import com.paymentflow.payment.infrastructure.TransactionRepository;
import com.paymentflow.shared.domain.BusinessRuleViolationException;
import com.paymentflow.shared.domain.ForbiddenException;
import com.paymentflow.shared.domain.Money;
import com.paymentflow.shared.domain.NotFoundException;
import com.paymentflow.shared.domain.ServiceUnavailableException;
import com.paymentflow.shared.infrastructure.RetryExecutor;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The core transfer use case (rule 5). Everything - payment row, transaction row, the
 * two immutable ledger entries, the account balance updates (optimistic locking), and
 * the outbox event - happens in ONE @Transactional method (transactional outbox
 * pattern, ADR-006). Idempotency (ADR-005) is arbitrated by the idempotency_keys unique
 * constraint, not check-then-insert. OptimisticLockingFailureException from the
 * @Version-guarded account updates (ADR-004) is retried with backoff at this
 * application-service layer via RetryExecutor, mirroring
 * postgres-concurrency-control's OptimisticLockingAccountService pattern.
 */
@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);
    private static final int MAX_REPLAY_POLL_ATTEMPTS = 20;

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final RetryExecutor retryExecutor;
    private final ObjectMapper objectMapper;
    private final EntityManager entityManager;
    private final TransferService self;

    public TransferService(AccountRepository accountRepository,
                            TransactionRepository transactionRepository,
                            LedgerEntryRepository ledgerEntryRepository,
                            IdempotencyKeyRepository idempotencyKeyRepository,
                            OutboxEventRepository outboxEventRepository,
                            RetryExecutor retryExecutor,
                            ObjectMapper objectMapper,
                            EntityManager entityManager,
                            @Lazy TransferService self) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.retryExecutor = retryExecutor;
        this.objectMapper = objectMapper;
        this.entityManager = entityManager;
        this.self = self;
    }

    public record TransferRequest(UUID sourceAccountId, UUID destinationAccountId, long amountMinorUnits,
                                   String currency) {
    }

    public record TransferResult(UUID transactionId, String status, boolean replayed) {
    }

    /**
     * Public entry point. Not itself @Transactional: each attempt (including retries
     * after an OptimisticLockingFailureException) must start in a fresh transaction, so
     * the retry loop lives outside the transactional boundary - retrying inside a
     * transaction Postgres already marked rollback-only does nothing.
     */
    public TransferResult transfer(UUID callerUserId, String idempotencyKey, TransferRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessRuleViolationException("Idempotency-Key header is required");
        }
        String fingerprint = RequestFingerprint.of(request.sourceAccountId().toString(),
                request.destinationAccountId().toString(), request.amountMinorUnits(), request.currency());

        try {
            return retryExecutor.execute(() -> self.attemptTransfer(callerUserId, idempotencyKey, fingerprint, request));
        } catch (DataIntegrityViolationException | jakarta.persistence.PersistenceException raceOnIdempotencyKey) {
            // Two concurrent requests with the SAME key both tried to INSERT the
            // idempotency row; exactly one wins the unique constraint (user_id,
            // idempotency_key). The loser lands here and must resolve to the winner's
            // outcome, not to an error. Caught as PersistenceException (in addition to
            // Spring's translated DataIntegrityViolationException) because the
            // constraint violation surfaces from a raw EntityManager.flush() call, which
            // is not behind a Spring Data repository proxy and so is never translated by
            // PersistenceExceptionTranslationPostProcessor.
            if (!isUniqueConstraintViolation(raceOnIdempotencyKey)) {
                throw raceOnIdempotencyKey;
            }
            return self.replayOrReject(callerUserId, idempotencyKey, fingerprint);
        }
    }

    private boolean isUniqueConstraintViolation(RuntimeException e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException) {
                return true;
            }
            cause = cause.getCause();
        }
        return e instanceof DataIntegrityViolationException;
    }

    @Transactional
    public TransferResult attemptTransfer(UUID callerUserId, String idempotencyKey, String fingerprint,
                                           TransferRequest request) {
        // Claim the idempotency key FIRST, in this transaction, using EntityManager.persist()
        // directly (ADR-005's called-out pitfall: JpaRepository.save()/saveAndFlush() on an
        // entity with a manually-assigned, non-generated @EmbeddedId is NOT guaranteed to
        // issue a real INSERT - Spring Data's isNew() check sees a non-null id and routes to
        // merge(), which (a) does a SELECT-then-INSERT/UPDATE instead of a guaranteed INSERT,
        // and (b) returns a DIFFERENT managed entity instance than the one passed in, leaving
        // later mutations on the original reference silently un-persisted. persist() avoids
        // both problems: it forces a real INSERT (so a concurrent duplicate reliably raises a
        // unique-constraint violation) and keeps `record` itself as the managed instance.
        IdempotencyKeyRecord record = new IdempotencyKeyRecord(callerUserId, idempotencyKey, fingerprint, null);
        entityManager.persist(record);
        entityManager.flush();

        Account source = accountRepository.findById(request.sourceAccountId())
                .orElseThrow(() -> new NotFoundException("source account not found: " + request.sourceAccountId()));
        Account destination = accountRepository.findById(request.destinationAccountId())
                .orElseThrow(() -> new NotFoundException(
                        "destination account not found: " + request.destinationAccountId()));

        if (!source.getOwnerId().equals(callerUserId)) {
            throw new ForbiddenException("caller does not own source account " + source.getId());
        }

        Money amount = Money.of(request.amountMinorUnits(), request.currency());

        Transaction transaction = new Transaction(callerUserId, source.getId(), destination.getId(),
                amount.amountMinorUnits(), amount.currency());
        transactionRepository.save(transaction);

        // Insufficient funds / inactive account: source.debit()/destination.credit()
        // throw BusinessRuleViolationException, which rolls back this ENTIRE
        // transaction (including the transaction row and the idempotency-key claim
        // above) since it is an unchecked exception on a default @Transactional method.
        // That is deliberate: a rejected transfer never happened, so its idempotency
        // key must remain free for the client to retry with a corrected request.
        source.debit(amount);
        destination.credit(amount);
        accountRepository.save(source);
        accountRepository.save(destination);

        LedgerEntry debitEntry = new LedgerEntry(transaction.getId(), source.getId(), Direction.DEBIT,
                amount.amountMinorUnits());
        LedgerEntry creditEntry = new LedgerEntry(transaction.getId(), destination.getId(), Direction.CREDIT,
                amount.amountMinorUnits());
        List<LedgerEntry> entries = List.of(debitEntry, creditEntry);
        if (!LedgerInvariants.isBalanced(entries)) {
            // Defensive: should be unreachable given the construction above, but the
            // debit==credit invariant is an application-level guarantee (Postgres only
            // enforces per-row CHECKs), so it is asserted here as well as unit-tested.
            throw new IllegalStateException("ledger entries are not balanced for transaction " + transaction.getId());
        }
        ledgerEntryRepository.saveAll(entries);

        transaction.markCompleted();
        transactionRepository.save(transaction);

        record.linkTransaction(transaction.getId());

        outboxEventRepository.save(new OutboxEvent("Transaction", transaction.getId(), "payment.completed",
                toJson(transaction)));

        return new TransferResult(transaction.getId(), transaction.getStatus().name(), false);
    }

    /**
     * Runs in its own fresh, short, read-mostly transaction after losing the
     * idempotency-key race. The winning transaction may not have committed yet, so this
     * polls briefly with backoff rather than failing immediately - this is what makes
     * "concurrent identical requests result in exactly one payment" true under a real
     * race, not just under a check-then-insert best effort.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TransferResult replayOrReject(UUID callerUserId, String idempotencyKey, String fingerprint) {
        for (int attempt = 1; attempt <= MAX_REPLAY_POLL_ATTEMPTS; attempt++) {
            var existing = idempotencyKeyRepository.findByUserIdAndIdempotencyKey(callerUserId, idempotencyKey);
            if (existing.isPresent()) {
                IdempotencyKeyRecord winner = existing.get();
                if (!winner.getRequestFingerprint().equals(fingerprint)) {
                    throw new BusinessRuleViolationException(
                            "Idempotency-Key reused with a different request payload");
                }
                if (winner.getTransactionId() != null) {
                    Transaction transaction = transactionRepository.findById(winner.getTransactionId())
                            .orElseThrow(() -> new NotFoundException("transaction not found for replayed key"));
                    return new TransferResult(transaction.getId(), transaction.getStatus().name(), true);
                }
            }
            sleepBriefly(attempt);
        }
        throw new ServiceUnavailableException(
                "idempotency key is being processed by a concurrent request; retry later");
    }

    private void sleepBriefly(int attempt) {
        try {
            Thread.sleep(Math.min(5L * attempt, 100L));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private String toJson(Transaction transaction) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "transactionId", transaction.getId().toString(),
                    "sourceAccountId", transaction.getSourceAccountId().toString(),
                    "destinationAccountId", transaction.getDestinationAccountId().toString(),
                    "amountMinorUnits", transaction.getAmountMinorUnits(),
                    "currency", transaction.getCurrency(),
                    "status", transaction.getStatus().name()
            ));
        } catch (Exception e) {
            log.error("failed to serialize outbox payload for transaction {}", transaction.getId(), e);
            return "{}";
        }
    }
}
