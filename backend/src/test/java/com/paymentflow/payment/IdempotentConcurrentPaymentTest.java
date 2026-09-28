package com.paymentflow.payment;

import static org.assertj.core.api.Assertions.assertThat;

import com.paymentflow.account.domain.Account;
import com.paymentflow.account.infrastructure.AccountRepository;
import com.paymentflow.identity.domain.Role;
import com.paymentflow.identity.domain.User;
import com.paymentflow.identity.infrastructure.UserRepository;
import com.paymentflow.ledger.infrastructure.LedgerEntryRepository;
import com.paymentflow.payment.application.TransferService;
import com.paymentflow.payment.infrastructure.TransactionRepository;
import com.paymentflow.shared.domain.Money;
import com.paymentflow.support.PostgresTestBase;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Idempotency test (rule 10): fires the SAME Idempotency-Key concurrently twice and
 * asserts only one payment/transaction/ledger-entry-pair was ever created - the race is
 * arbitrated by the idempotency_keys unique constraint (ADR-005), not a
 * check-then-insert, per TransferService.replayOrReject.
 */
class IdempotentConcurrentPaymentTest extends PostgresTestBase {

    @Autowired
    TransferService transferService;
    @Autowired
    UserRepository userRepository;
    @Autowired
    AccountRepository accountRepository;
    @Autowired
    TransactionRepository transactionRepository;
    @Autowired
    LedgerEntryRepository ledgerEntryRepository;

    @Test
    void concurrentIdenticalRequestsProduceExactlyOnePayment() throws Exception {
        User owner = userRepository.save(new User("idem-" + UUID.randomUUID() + "@example.com", "hash", Role.CUSTOMER));
        Account source = accountRepository.save(new Account(owner.getId(), "EUR"));
        Account destination = accountRepository.save(new Account(owner.getId(), "EUR"));
        Account loaded = accountRepository.findById(source.getId()).orElseThrow();
        loaded.credit(Money.of(5_000, "EUR"));
        accountRepository.save(loaded);

        String sharedKey = "shared-idem-key-" + UUID.randomUUID();
        var request = new TransferService.TransferRequest(source.getId(), destination.getId(), 1_000, "EUR");

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch startLine = new CountDownLatch(1);
        AtomicReference<TransferService.TransferResult> resultA = new AtomicReference<>();
        AtomicReference<TransferService.TransferResult> resultB = new AtomicReference<>();

        Future<?> f1 = pool.submit(() -> {
            awaitStart(startLine);
            resultA.set(transferService.transfer(owner.getId(), sharedKey, request));
        });
        Future<?> f2 = pool.submit(() -> {
            awaitStart(startLine);
            resultB.set(transferService.transfer(owner.getId(), sharedKey, request));
        });

        startLine.countDown();
        f1.get(30, TimeUnit.SECONDS);
        f2.get(30, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(resultA.get()).isNotNull();
        assertThat(resultB.get()).isNotNull();
        assertThat(resultA.get().transactionId()).isEqualTo(resultB.get().transactionId());
        // Exactly one of the two calls actually created the payment; the other replayed it.
        assertThat(resultA.get().replayed() ^ resultB.get().replayed()).isTrue();

        List<com.paymentflow.payment.domain.Transaction> all = transactionRepository.findAll();
        long matchingTransactions = all.stream()
                .filter(t -> t.getSourceAccountId().equals(source.getId())
                        && t.getDestinationAccountId().equals(destination.getId()))
                .count();
        assertThat(matchingTransactions).isEqualTo(1);

        var entries = ledgerEntryRepository.findByTransactionId(resultA.get().transactionId());
        assertThat(entries).hasSize(2);

        Account finalSource = accountRepository.findById(source.getId()).orElseThrow();
        assertThat(finalSource.getBalanceMinorUnits()).isEqualTo(4_000); // debited exactly once
    }

    private void awaitStart(CountDownLatch latch) {
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
