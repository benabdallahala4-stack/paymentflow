import { KeyValuePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AccountService } from '../../core/services/account.service';
import { AuthService } from '../../core/services/auth.service';
import { Account } from '../../core/models/account.model';
import { MoneyPipe } from '../../shared/pipes/money.pipe';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, MoneyPipe, KeyValuePipe],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css',
})
export class DashboardComponent implements OnInit {
  private readonly accountService = inject(AccountService);
  readonly auth = inject(AuthService);

  readonly accounts = signal<Account[]>([]);
  readonly loading = signal(true);

  readonly totalBalanceByCurrency = signal<Record<string, number>>({});

  ngOnInit(): void {
    this.accountService.listAccounts().subscribe({
      next: (accounts) => {
        this.accounts.set(accounts);
        this.loading.set(false);
        this.totalBalanceByCurrency.set(
          accounts.reduce<Record<string, number>>((totals, account) => {
            totals[account.currency] = (totals[account.currency] ?? 0) + account.balanceMinorUnits;
            return totals;
          }, {}),
        );
      },
      error: () => this.loading.set(false),
    });
  }
}
