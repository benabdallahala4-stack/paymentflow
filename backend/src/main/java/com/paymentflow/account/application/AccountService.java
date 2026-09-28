package com.paymentflow.account.application;

import com.paymentflow.account.domain.Account;
import com.paymentflow.account.infrastructure.AccountRepository;
import com.paymentflow.identity.infrastructure.AuthenticatedUser;
import com.paymentflow.shared.domain.ForbiddenException;
import com.paymentflow.shared.domain.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public Account openAccount(UUID ownerId, String currency) {
        Account account = new Account(ownerId, currency);
        return accountRepository.save(account);
    }

    public List<Account> listOwnAccounts(UUID ownerId) {
        return accountRepository.findByOwnerId(ownerId);
    }

    public Account getOwned(UUID accountId, AuthenticatedUser caller) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("account not found: " + accountId));
        requireOwnerOrAdmin(account, caller);
        return account;
    }

    public void requireOwnerOrAdmin(Account account, AuthenticatedUser caller) {
        boolean isAdmin = "ADMIN".equals(caller.role());
        boolean isOwner = account.getOwnerId().equals(caller.userId());
        if (!isAdmin && !isOwner) {
            throw new ForbiddenException("caller does not own account " + account.getId());
        }
    }
}
