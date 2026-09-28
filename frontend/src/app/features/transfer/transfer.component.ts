import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AccountService } from '../../core/services/account.service';
import { PaymentService } from '../../core/services/payment.service';
import { Account } from '../../core/models/account.model';
import { Payment } from '../../core/models/payment.model';
import { generateIdempotencyKey } from '../../core/services/idempotency-key.util';
import { MoneyPipe } from '../../shared/pipes/money.pipe';

function positiveAmount(control: { value: unknown }): { positiveAmount: true } | null {
  const value = Number(control.value);
  return Number.isFinite(value) && value > 0 ? null : { positiveAmount: true };
}

@Component({
  selector: 'app-transfer',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, MoneyPipe],
  templateUrl: './transfer.component.html',
  styleUrl: './transfer.component.css',
})
export class TransferComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly accountService = inject(AccountService);
  private readonly paymentService = inject(PaymentService);

  readonly accounts = signal<Account[]>([]);
  readonly submitting = signal(false);
  readonly result = signal<Payment | null>(null);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    sourceAccountId: ['', Validators.required],
    destinationAccountId: ['', Validators.required],
    // Amount entered in major units (e.g. "10.50") by the user; converted to integer
    // minor units before it ever touches an HTTP request or the domain model.
    amount: ['', [Validators.required, positiveAmount]],
    currency: ['EUR', Validators.required],
  });

  /**
   * Idempotency key for the CURRENT submission attempt. Generated fresh whenever the
   * user starts a new transfer (see resetForNewAttempt/ngOnInit), and deliberately NOT
   * regenerated inside `submit()` on retry — a retry of the same attempt (e.g. the user
   * clicking "try again" after a network error on the same filled-in form) must reuse
   * the same key so the backend can recognize it and replay the original outcome
   * instead of creating a duplicate transfer.
   */
  private idempotencyKey = generateIdempotencyKey();

  ngOnInit(): void {
    this.accountService.listAccounts().subscribe((accounts) => this.accounts.set(accounts));
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    const amountMinorUnits = Math.round(Number(raw.amount) * 100);

    this.submitting.set(true);
    this.errorMessage.set(null);

    this.paymentService
      .createPayment(
        {
          sourceAccountId: raw.sourceAccountId,
          destinationAccountId: raw.destinationAccountId,
          amountMinorUnits,
          currency: raw.currency,
        },
        this.idempotencyKey,
      )
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: (payment) => {
          this.result.set(payment);
          // Success: this attempt is done. The *next* transfer the user starts is a new
          // logical submission, so it gets a brand-new key.
          this.startNewAttempt();
        },
        error: (err) => {
          this.errorMessage.set(
            err?.error?.message ?? 'Transfer failed. You can retry safely: it will not double-send.',
          );
          // Deliberately do NOT rotate the key here — a retry of this exact form state
          // must reuse the same Idempotency-Key.
        },
      });
  }

  /** Call when the user explicitly starts a fresh transfer (e.g. after a success, or an
   * explicit "start over" action), never on a same-attempt retry. */
  startNewAttempt(): void {
    this.idempotencyKey = generateIdempotencyKey();
    this.form.reset({ currency: 'EUR', sourceAccountId: '', destinationAccountId: '', amount: '' });
    this.result.set(null);
    this.errorMessage.set(null);
  }

  /** Exposed for tests only. */
  get currentIdempotencyKey(): string {
    return this.idempotencyKey;
  }
}
