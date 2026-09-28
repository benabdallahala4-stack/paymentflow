package com.paymentflow.payment.application;

import com.paymentflow.account.domain.Account;
import com.paymentflow.account.infrastructure.AccountRepository;
import com.paymentflow.shared.domain.Money;
import com.paymentflow.shared.domain.NotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Concurrency-lab / experiment code (ADR-004): pessimistic locking (SELECT ... FOR
 * UPDATE via AccountRepository.findByIdForUpdate) as an ALTERNATE, clearly-separated
 * path - not the default production path (that is TransferService's optimistic
 * @Version + retry). Kept for the concurrency comparison this project demonstrates,
 * mirroring postgres-concurrency-control's PessimisticLockingAccountService.
 *
 * <p>Locks are acquired in a consistent order (min(id), max(id)) to avoid the
 * classic two-accounts deadlock scenario.
 */
@Service
public class PessimisticTransferService {

    private final AccountRepository accountRepository;

    public PessimisticTransferService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public void transfer(UUID sourceAccountId, UUID destinationAccountId, Money amount) {
        UUID first = sourceAccountId.compareTo(destinationAccountId) < 0 ? sourceAccountId : destinationAccountId;
        UUID second = sourceAccountId.compareTo(destinationAccountId) < 0 ? destinationAccountId : sourceAccountId;

        Account firstLocked = accountRepository.findByIdForUpdate(first)
                .orElseThrow(() -> new NotFoundException("account not found: " + first));
        Account secondLocked = accountRepository.findByIdForUpdate(second)
                .orElseThrow(() -> new NotFoundException("account not found: " + second));

        Account source = firstLocked.getId().equals(sourceAccountId) ? firstLocked : secondLocked;
        Account destination = firstLocked.getId().equals(sourceAccountId) ? secondLocked : firstLocked;

        source.debit(amount);
        destination.credit(amount);
        accountRepository.save(source);
        accountRepository.save(destination);
    }
}
