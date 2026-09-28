package com.paymentflow.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.paymentflow.account.domain.Account;
import com.paymentflow.account.infrastructure.AccountRepository;
import com.paymentflow.identity.domain.Role;
import com.paymentflow.identity.domain.User;
import com.paymentflow.identity.infrastructure.UserRepository;
import com.paymentflow.ledger.infrastructure.LedgerEntryRepository;
import com.paymentflow.payment.application.TransferService;
import com.paymentflow.payment.infrastructure.TransactionRepository;
import com.paymentflow.shared.domain.BusinessRuleViolationException;
import com.paymentflow.support.PostgresTestBase;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * No @Transactional on these test methods deliberately: TransferService's own
 * transaction boundaries (fresh transaction per attempt, REQUIRES_NEW for replay) only
 * behave correctly when invoked from OUTSIDE an already-open transaction, exactly as in
 * production (a controller method, not a @Transactional test wrapping everything in one
 * connection).
 */
class TransferServiceIntegrationTest extends PostgresTestBase {

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
    void happyPathTransferMovesMoneyAndWritesBalancedLedgerEntries() {
        User owner = userRepository.save(new User("alice-" + UUID.randomUUID() + "@example.com", "hash", Role.CUSTOMER));
        Account source = accountRepository.save(new Account(owner.getId(), "EUR"));
        Account destination = accountRepository.save(new Account(owner.getId(), "EUR"));
        source.credit(com.paymentflow.shared.domain.Money.of(10_000, "EUR"));
        accountRepository.save(source);

        var result = transferService.transfer(owner.getId(), "key-" + UUID.randomUUID(),
                new TransferService.TransferRequest(source.getId(), destination.getId(), 3_000, "EUR"));

        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.replayed()).isFalse();

        Account refreshedSource = accountRepository.findById(source.getId()).orElseThrow();
        Account refreshedDestination = accountRepository.findById(destination.getId()).orElseThrow();
        assertThat(refreshedSource.getBalanceMinorUnits()).isEqualTo(7_000);
        assertThat(refreshedDestination.getBalanceMinorUnits()).isEqualTo(3_000);

        var entries = ledgerEntryRepository.findByTransactionId(UUID.fromString(result.transactionId().toString()));
        assertThat(entries).hasSize(2);
        assertThat(com.paymentflow.ledger.domain.LedgerInvariants.isBalanced(entries)).isTrue();
    }

    @Test
    void insufficientFundsIsRejectedAndBalancesUnchanged() {
        User owner = userRepository.save(new User("bob-" + UUID.randomUUID() + "@example.com", "hash", Role.CUSTOMER));
        Account source = accountRepository.save(new Account(owner.getId(), "EUR"));
        Account destination = accountRepository.save(new Account(owner.getId(), "EUR"));
        // source has zero balance

        assertThatThrownBy(() -> transferService.transfer(owner.getId(), "key-" + UUID.randomUUID(),
                new TransferService.TransferRequest(source.getId(), destination.getId(), 1_000, "EUR")))
                .isInstanceOf(BusinessRuleViolationException.class);

        Account refreshedSource = accountRepository.findById(source.getId()).orElseThrow();
        Account refreshedDestination = accountRepository.findById(destination.getId()).orElseThrow();
        assertThat(refreshedSource.getBalanceMinorUnits()).isZero();
        assertThat(refreshedDestination.getBalanceMinorUnits()).isZero();
    }
}
