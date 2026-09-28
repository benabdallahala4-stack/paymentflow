package com.paymentflow.shared.infrastructure;

import com.paymentflow.shared.domain.ServiceUnavailableException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Component;

/**
 * Retry-on-conflict helper for optimistic locking (ADR-004), pattern reused from the
 * postgres-concurrency-control reference project's RetryExecutor: every attempt must
 * run in a brand-new transaction (achieved by having callers invoke a @Transactional
 * self-proxy method from outside any existing transaction), and backoff uses jitter so
 * two colliding retries don't collide again in lockstep.
 */
@Component
public class RetryExecutor {

    private static final Logger log = LoggerFactory.getLogger(RetryExecutor.class);
    private static final int DEFAULT_MAX_ATTEMPTS = 5;
    private static final long BASE_BACKOFF_MILLIS = 10;

    public <T> T execute(Supplier<T> action) {
        return execute(action, DEFAULT_MAX_ATTEMPTS);
    }

    public <T> T execute(Supplier<T> action, int maxAttempts) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return action.get();
            } catch (ConcurrencyFailureException e) {
                last = e;
                log.debug("attempt {}/{} lost a write conflict, retrying", attempt, maxAttempts);
                backoff(attempt);
            }
        }
        throw new ServiceUnavailableException(
                "gave up after " + maxAttempts + " attempts due to write conflicts: "
                        + (last == null ? "" : last.getMessage()));
    }

    private void backoff(int attempt) {
        long ceiling = BASE_BACKOFF_MILLIS * (1L << Math.min(attempt - 1, 6));
        long sleep = ThreadLocalRandom.current().nextLong(1, ceiling + 1);
        try {
            Thread.sleep(sleep);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while backing off", e);
        }
    }
}
