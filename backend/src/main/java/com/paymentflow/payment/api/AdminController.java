package com.paymentflow.payment.api;

import com.paymentflow.account.domain.Account;
import com.paymentflow.account.infrastructure.AccountRepository;
import com.paymentflow.ledger.domain.LedgerEntry;
import com.paymentflow.ledger.infrastructure.LedgerEntryRepository;
import com.paymentflow.payment.domain.Transaction;
import com.paymentflow.payment.infrastructure.TransactionRepository;
import com.paymentflow.shared.domain.NotFoundException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** ADMIN-only, read-only inspection endpoints (api-design.md). Never mutates a balance or ledger entry. */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public AdminController(AccountRepository accountRepository, TransactionRepository transactionRepository,
                            LedgerEntryRepository ledgerEntryRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    public record LedgerEntryResponse(String id, String accountId, String direction, long amountMinorUnits) {
        static LedgerEntryResponse from(LedgerEntry e) {
            return new LedgerEntryResponse(e.getId().toString(), e.getAccountId().toString(),
                    e.getDirection().name(), e.getAmountMinorUnits());
        }
    }

    @GetMapping("/accounts/{accountId}")
    public Account getAccount(@PathVariable UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("account not found: " + accountId));
    }

    @GetMapping("/accounts/{accountId}/ledger-entries")
    public List<LedgerEntryResponse> accountLedgerEntries(@PathVariable UUID accountId) {
        return ledgerEntryRepository.findByAccountIdOrderByCreatedAtDesc(accountId).stream()
                .map(LedgerEntryResponse::from).toList();
    }

    @GetMapping("/transactions/{transactionId}")
    public Transaction getTransaction(@PathVariable UUID transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new NotFoundException("transaction not found: " + transactionId));
    }

    @GetMapping("/transactions/{transactionId}/ledger-entries")
    public List<LedgerEntryResponse> transactionLedgerEntries(@PathVariable UUID transactionId) {
        return ledgerEntryRepository.findByTransactionId(transactionId).stream()
                .map(LedgerEntryResponse::from).toList();
    }

    /**
     * Demonstrates the N+1 fix (rule 9): batch-fetches ledger entries for ALL of a
     * user's transactions with one IN-query (findByTransactionIdIn), then groups them
     * back in memory. The naive version this replaces would have been:
     * {@code for (Transaction t : transactions) ledgerEntryRepository.findByTransactionId(t.getId())}
     * - one query per transaction, i.e. N+1 queries for N transactions.
     */
    @GetMapping("/users/{userId}/payments-with-entries")
    public List<Map<String, Object>> paymentsWithLedgerEntries(@PathVariable UUID userId) {
        List<Transaction> transactions = transactionRepository.findFirstPage(userId,
                org.springframework.data.domain.PageRequest.of(0, 100));
        List<UUID> transactionIds = transactions.stream().map(Transaction::getId).toList();
        Map<UUID, List<LedgerEntryResponse>> entriesByTransaction = ledgerEntryRepository
                .findByTransactionIdIn(transactionIds).stream()
                .map(e -> Map.entry(e.getTransactionId(), LedgerEntryResponse.from(e)))
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

        return transactions.stream()
                .map(t -> Map.<String, Object>of(
                        "transactionId", t.getId().toString(),
                        "status", t.getStatus().name(),
                        "ledgerEntries", entriesByTransaction.getOrDefault(t.getId(), List.of())))
                .toList();
    }
}
