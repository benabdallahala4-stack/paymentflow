package com.paymentflow.payment;

import static org.assertj.core.api.Assertions.assertThat;

import com.paymentflow.account.domain.Account;
import com.paymentflow.account.infrastructure.AccountRepository;
import com.paymentflow.identity.domain.Role;
import com.paymentflow.identity.domain.User;
import com.paymentflow.identity.infrastructure.UserRepository;
import com.paymentflow.payment.application.TransferService;
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
 * THE most important test in the project (rule 10): reproduces the classic
 * two-concurrent-transfers race against a real Testcontainers PostgreSQL, with real
 * concurrent threads/transactions (no mocking), and proves optimistic locking
 * (@Version + RetryExecutor in TransferService) prevents overdraft.
 *
 * <p>Alice has EUR 100.00 (10 000 minor units). Two concurrent transfers of EUR 80.00
 * and EUR 70.00 fire in parallel from her account. Only one can succeed (100 >= 80,
 * but 100 - 80 = 20 < 70); the other must fail application-level validation (retried
 * on version conflict, then rejected as insufficient funds once it re-reads the
 * post-transfer balance) - it must NEVER be allowed to overdraw the account.
 */
class ConcurrentTransferOverdraftTest extends PostgresTestBase {

    @Autowired
    TransferService transferService;
    @Autowired
    UserRepository userRepository;
    @Autowired
    AccountRepository accountRepository;

    @Test
    void onlyOneOfTwoConcurrentTransfersSucceedsAndBalanceNeverGoesNegative() throws Exception {
        User alice = userRepository.save(new User("alice-conc-" + UUID.randomUUID() + "@example.com", "hash", Role.CUSTOMER));
        User bob = userRepository.save(new User("bob-conc-" + UUID.randomUUID() + "@example.com", "hash", Role.CUSTOMER));
        Account source = accountRepository.save(new Account(alice.getId(), "EUR"));
        Account dest1 = accountRepository.save(new Account(bob.getId(), "EUR"));
        Account dest2 = accountRepository.save(new Account(bob.getId(), "EUR"));

        Account loaded = accountRepository.findById(source.getId()).orElseThrow();
        loaded.credit(Money.of(10_000, "EUR")); // EUR 100.00
        accountRepository.save(loaded);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch startLine = new CountDownLatch(1);

        AtomicReference<Object> resultA = new AtomicReference<>();
        AtomicReference<Object> resultB = new AtomicReference<>();

        Future<?> f1 = pool.submit(() -> {
            awaitStart(startLine);
            try {
                resultA.set(transferService.transfer(alice.getId(), "conc-key-A-" + UUID.randomUUID(),
                        new TransferService.TransferRequest(source.getId(), dest1.getId(), 8_000, "EUR")));
            } catch (Exception e) {
                resultA.set(e);
            }
        });
        Future<?> f2 = pool.submit(() -> {
            awaitStart(startLine);
            try {
                resultB.set(transferService.transfer(alice.getId(), "conc-key-B-" + UUID.randomUUID(),
                        new TransferService.TransferRequest(source.getId(), dest2.getId(), 7_000, "EUR")));
            } catch (Exception e) {
                resultB.set(e);
            }
        });

        startLine.countDown(); // release both threads at (as close to) the same instant
        f1.get(30, TimeUnit.SECONDS);
        f2.get(30, TimeUnit.SECONDS);
        pool.shutdown();

        List<Object> results = List.of(resultA.get(), resultB.get());
        long successes = results.stream().filter(r -> r instanceof TransferService.TransferResult).count();
        long failures = results.stream().filter(r -> r instanceof Exception).count();

        assertThat(successes).isEqualTo(1);
        assertThat(failures).isEqualTo(1);

        Account finalSource = accountRepository.findById(source.getId()).orElseThrow();
        assertThat(finalSource.getBalanceMinorUnits()).isGreaterThanOrEqualTo(0);
        // Exactly one transfer went through: either 100-80=20 or 100-70=30 remain.
        assertThat(finalSource.getBalanceMinorUnits()).isIn(2_000L, 3_000L);
    }

    private void awaitStart(CountDownLatch latch) {
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
