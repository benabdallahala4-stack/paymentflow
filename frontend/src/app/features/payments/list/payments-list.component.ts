import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PaymentService } from '../../../core/services/payment.service';
import { Payment } from '../../../core/models/payment.model';
import { MoneyPipe } from '../../../shared/pipes/money.pipe';
import { DataListComponent } from '../../../shared/components/data-list/data-list.component';

@Component({
  selector: 'app-payments-list',
  standalone: true,
  imports: [RouterLink, MoneyPipe, DataListComponent, DatePipe],
  templateUrl: './payments-list.component.html',
  styleUrl: './payments-list.component.css',
})
export class PaymentsListComponent implements OnInit {
  private readonly paymentService = inject(PaymentService);

  readonly payments = signal<Payment[]>([]);
  readonly loading = signal(true);
  readonly nextCursor = signal<string | null>(null);
  readonly hasLoadedOnce = signal(false);

  ngOnInit(): void {
    this.loadPage(null);
  }

  loadMore(): void {
    if (this.nextCursor()) {
      this.loadPage(this.nextCursor());
    }
  }

  private loadPage(after: string | null): void {
    this.loading.set(true);
    this.paymentService.listPayments(after).subscribe({
      next: (page) => {
        this.payments.set(after ? [...this.payments(), ...page.items] : page.items);
        this.nextCursor.set(page.nextCursor);
        this.loading.set(false);
        this.hasLoadedOnce.set(true);
      },
      error: () => this.loading.set(false),
    });
  }
}
