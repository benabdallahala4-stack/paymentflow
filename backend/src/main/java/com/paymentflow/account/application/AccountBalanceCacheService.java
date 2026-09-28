package com.paymentflow.account.application;

import com.paymentflow.account.domain.Account;
import com.paymentflow.account.infrastructure.AccountRepository;
import com.paymentflow.shared.domain.NotFoundException;
import com.paymentflow.shared.infrastructure.RedisConfig;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Cache-aside for account balance READS only (rule 7).
 *
 * <p>This is deliberately the ONLY place a cached balance is ever returned, and it is
 * only ever used to serve GET requests. The payment transfer (write) path in
 * payment.application.TransferService NEVER calls this class - it always loads the
 * Account directly from AccountRepository (PostgreSQL) inside the transfer's
 * transaction, because a cached value could be stale by definition and trusting it for
 * an overdraft check would be a financial-correctness bug, not a performance one. Short
 * TTL (10s, see RedisConfig) bounds staleness for the read-only use case this serves.
 */
@Service
public class AccountBalanceCacheService {

    private final AccountRepository accountRepository;

    public AccountBalanceCacheService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Cacheable(cacheNames = RedisConfig.ACCOUNT_BALANCE_CACHE, key = "#accountId")
    public long getCachedBalanceMinorUnits(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("account not found: " + accountId));
        return account.getBalanceMinorUnits();
    }

    @CacheEvict(cacheNames = RedisConfig.ACCOUNT_BALANCE_CACHE, key = "#accountId")
    public void evict(UUID accountId) {
        // Called by the transfer service after commit so the cache doesn't serve a
        // stale balance for longer than necessary - a courtesy for read freshness,
        // not a correctness requirement (the TTL alone already bounds staleness).
    }
}
