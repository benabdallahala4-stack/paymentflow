package com.paymentflow.payment.api;

import com.paymentflow.account.application.AccountService;
import com.paymentflow.identity.infrastructure.AuthenticatedUser;
import com.paymentflow.payment.application.TransferService;
import com.paymentflow.payment.domain.Transaction;
import com.paymentflow.payment.infrastructure.TransactionRepository;
import com.paymentflow.shared.domain.ForbiddenException;
import com.paymentflow.shared.domain.NotFoundException;
import com.paymentflow.shared.infrastructure.CursorCodec;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final TransferService transferService;
    private final TransactionRepository transactionRepository;
    private final AccountService accountService;

    public PaymentController(TransferService transferService, TransactionRepository transactionRepository,
                              AccountService accountService) {
        this.transferService = transferService;
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
    }

    public record CreatePaymentRequest(@NotNull UUID sourceAccountId, @NotNull UUID destinationAccountId,
                                        @Positive long amountMinorUnits, @NotNull String currency) {
    }

    public record PaymentResponse(String transactionId, String status, boolean replayed) {
    }

    public record TransactionResponse(String id, String sourceAccountId, String destinationAccountId,
                                       long amountMinorUnits, String currency, String status, String createdAt) {
        static TransactionResponse from(Transaction t) {
            return new TransactionResponse(t.getId().toString(), t.getSourceAccountId().toString(),
                    t.getDestinationAccountId().toString(), t.getAmountMinorUnits(), t.getCurrency(),
                    t.getStatus().name(), t.getCreatedAt().toString());
        }
    }

    @PostMapping
    public PaymentResponse create(@AuthenticationPrincipal AuthenticatedUser caller,
                                   @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
                                   @RequestBody CreatePaymentRequest request) {
        var result = transferService.transfer(caller.userId(), idempotencyKey,
                new TransferService.TransferRequest(request.sourceAccountId(), request.destinationAccountId(),
                        request.amountMinorUnits(), request.currency()));
        return new PaymentResponse(result.transactionId().toString(), result.status(), result.replayed());
    }

    @GetMapping("/{transactionId}")
    public TransactionResponse get(@AuthenticationPrincipal AuthenticatedUser caller,
                                    @PathVariable UUID transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new NotFoundException("transaction not found: " + transactionId));
        boolean isParty = transaction.getInitiatedByUserId().equals(caller.userId());
        if (!isParty && !"ADMIN".equals(caller.role())) {
            throw new ForbiddenException("caller is not a party to transaction " + transactionId);
        }
        return TransactionResponse.from(transaction);
    }

    /**
     * Cursor-paginated (ADR-008) list of the caller's payments: after (opaque cursor) +
     * limit (default 20, max 100), ordered by created_at DESC, id DESC - never OFFSET.
     */
    @GetMapping
    public List<TransactionResponse> list(@AuthenticationPrincipal AuthenticatedUser caller,
                                           @RequestParam(required = false) String after,
                                           @RequestParam(defaultValue = "20") int limit) {
        int pageSize = Math.min(Math.max(limit, 1), 100);
        List<Transaction> page;
        if (after == null || after.isBlank()) {
            page = transactionRepository.findFirstPage(caller.userId(), PageRequest.of(0, pageSize));
        } else {
            CursorCodec.Cursor cursor = CursorCodec.decode(after);
            page = transactionRepository.findPageAfter(caller.userId(), cursor.createdAt(), cursor.id(),
                    PageRequest.of(0, pageSize));
        }
        return page.stream().map(TransactionResponse::from).toList();
    }
}
