package com.paymentflow.account.api;

import com.paymentflow.account.application.AccountBalanceCacheService;
import com.paymentflow.account.application.AccountService;
import com.paymentflow.account.domain.Account;
import com.paymentflow.identity.infrastructure.AuthenticatedUser;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;
    private final AccountBalanceCacheService balanceCacheService;

    public AccountController(AccountService accountService, AccountBalanceCacheService balanceCacheService) {
        this.accountService = accountService;
        this.balanceCacheService = balanceCacheService;
    }

    public record CreateAccountRequest(@NotBlank String currency) {
    }

    public record AccountResponse(String id, String ownerId, String currency, long balanceMinorUnits, String status) {
        static AccountResponse from(Account a) {
            return new AccountResponse(a.getId().toString(), a.getOwnerId().toString(), a.getCurrency(),
                    a.getBalanceMinorUnits(), a.getStatus().name());
        }
    }

    @PostMapping
    public AccountResponse create(@AuthenticationPrincipal AuthenticatedUser caller,
                                   @RequestBody CreateAccountRequest request) {
        return AccountResponse.from(accountService.openAccount(caller.userId(), request.currency()));
    }

    @GetMapping
    public List<AccountResponse> listOwn(@AuthenticationPrincipal AuthenticatedUser caller) {
        return accountService.listOwnAccounts(caller.userId()).stream()
                .map(AccountResponse::from)
                .collect(Collectors.toList());
    }

    @GetMapping("/{accountId}")
    public AccountResponse get(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable UUID accountId) {
        Account account = accountService.getOwned(accountId, caller);
        // Balance here is read straight from the entity just loaded (not the cache) to
        // keep this endpoint's contract simple; AccountBalanceCacheService is exercised
        // by callers that only need the balance figure at higher read volume.
        return AccountResponse.from(account);
    }
}
