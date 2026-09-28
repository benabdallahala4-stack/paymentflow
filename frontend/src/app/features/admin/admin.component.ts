import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { PaymentService } from '../../core/services/payment.service';
import { AdminService } from '../../core/services/account.service';
import { Payment, AdminTransactionDetail } from '../../core/models/payment.model';
import { MoneyPipe } from '../../shared/pipes/money.pipe';
import { DataListComponent } from '../../shared/components/data-list/data-list.component';

/**
 * Basic ADMIN monitoring page: lists recent payments (reusing the customer-visible
 * `GET /payments` listing scoped by role server-side per security-strategy.md) and lets
 * an admin drill into one transaction's outbox event status via
 * `GET /admin/transactions/{id}`.
 */
@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [MoneyPipe, DataListComponent, DatePipe, FormsModule],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.css',
})
export class AdminComponent implements OnInit {
  private readonly paymentService = inject(PaymentService);
  private readonly adminService = inject(AdminService);

  readonly payments = signal<Payment[]>([]);
  readonly loading = signal(true);
  readonly nextCursor = signal<string | null>(null);

  readonly lookupId = signal('');
  readonly lookupResult = signal<AdminTransactionDetail | null>(null);
  readonly lookupError = signal<string | null>(null);
  readonly lookupLoading = signal(false);

  ngOnInit(): void {
    this.loadPage(null);
  }

  loadMore(): void {
    if (this.nextCursor()) {
      this.loadPage(this.nextCursor());
    }
  }

  setLookupId(value: string): void {
    this.lookupId.set(value);
  }

  inspectTransaction(): void {
    const id = this.lookupId().trim();
    if (!id) return;
    this.lookupLoading.set(true);
    this.lookupError.set(null);
    this.adminService.getTransaction(id).subscribe({
      next: (detail) => {
        this.lookupResult.set(detail);
        this.lookupLoading.set(false);
      },
      error: () => {
        this.lookupError.set('Transaction not found or you lack permission to view it.');
        this.lookupLoading.set(false);
      },
    });
  }

  private loadPage(after: string | null): void {
    this.loading.set(true);
    this.paymentService.listPayments(after).subscribe({
      next: (page) => {
        this.payments.set(after ? [...this.payments(), ...page.items] : page.items);
        this.nextCursor.set(page.nextCursor);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
