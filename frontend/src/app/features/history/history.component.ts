import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { AccountService } from '../../core/services/account.service';
import { Account } from '../../core/models/account.model';
import { TransactionHistoryItem } from '../../core/models/transaction.model';
import { MoneyPipe } from '../../shared/pipes/money.pipe';
import { DataListComponent } from '../../shared/components/data-list/data-list.component';

@Component({
  selector: 'app-history',
  standalone: true,
  imports: [MoneyPipe, DataListComponent, DatePipe, FormsModule],
  templateUrl: './history.component.html',
  styleUrl: './history.component.css',
})
export class HistoryComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly accountService = inject(AccountService);

  readonly accounts = signal<Account[]>([]);
  readonly selectedAccountId = signal<string | null>(null);
  readonly items = signal<TransactionHistoryItem[]>([]);
  readonly loading = signal(false);
  readonly nextCursor = signal<string | null>(null);

  ngOnInit(): void {
    this.accountService.listAccounts().subscribe((accounts) => {
      this.accounts.set(accounts);
      const preselected = this.route.snapshot.queryParamMap.get('accountId');
      const initial = preselected ?? accounts[0]?.id ?? null;
      if (initial) {
        this.selectAccount(initial);
      }
    });
  }

  selectAccount(accountId: string): void {
    this.selectedAccountId.set(accountId);
    this.items.set([]);
    this.nextCursor.set(null);
    this.loadPage(null);
  }

  loadMore(): void {
    if (this.nextCursor()) {
      this.loadPage(this.nextCursor());
    }
  }

  private loadPage(after: string | null): void {
    const accountId = this.selectedAccountId();
    if (!accountId) return;
    this.loading.set(true);
    this.accountService.getTransactionHistory(accountId, after).subscribe({
      next: (page) => {
        this.items.set(after ? [...this.items(), ...page.items] : page.items);
        this.nextCursor.set(page.nextCursor);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
