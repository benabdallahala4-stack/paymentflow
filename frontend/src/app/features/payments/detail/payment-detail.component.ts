import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { PaymentService } from '../../../core/services/payment.service';
import { Payment } from '../../../core/models/payment.model';
import { MoneyPipe } from '../../../shared/pipes/money.pipe';

@Component({
  selector: 'app-payment-detail',
  standalone: true,
  imports: [RouterLink, MoneyPipe, DatePipe],
  templateUrl: './payment-detail.component.html',
  styleUrl: './payment-detail.component.css',
})
export class PaymentDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly paymentService = inject(PaymentService);

  readonly payment = signal<Payment | null>(null);
  readonly loading = signal(true);
  readonly notFound = signal(false);

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id') ?? this.route.snapshot.queryParamMap.get('id');
    if (!id) {
      this.notFound.set(true);
      this.loading.set(false);
      return;
    }
    this.paymentService.getPayment(id).subscribe({
      next: (payment) => {
        this.payment.set(payment);
        this.loading.set(false);
      },
      error: () => {
        this.notFound.set(true);
        this.loading.set(false);
      },
    });
  }

  refresh(): void {
    const payment = this.payment();
    if (!payment) return;
    this.paymentService.getPayment(payment.transactionId).subscribe((p) => this.payment.set(p));
  }
}
