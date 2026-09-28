import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { AccountService } from '../../core/services/account.service';
import { Account } from '../../core/models/account.model';
import { MoneyPipe } from '../../shared/pipes/money.pipe';
import { DataListComponent } from '../../shared/components/data-list/data-list.component';

@Component({
  selector: 'app-accounts',
  standalone: true,
  imports: [RouterLink, ReactiveFormsModule, MoneyPipe, DataListComponent],
  templateUrl: './accounts.component.html',
  styleUrl: './accounts.component.css',
})
export class AccountsComponent implements OnInit {
  private readonly accountService = inject(AccountService);
  private readonly fb = inject(FormBuilder);

  readonly accounts = signal<Account[]>([]);
  readonly loading = signal(true);
  readonly creating = signal(false);

  readonly form = this.fb.nonNullable.group({
    currency: ['EUR', Validators.required],
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.accountService.listAccounts().subscribe({
      next: (accounts) => {
        this.accounts.set(accounts);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  createAccount(): void {
    if (this.form.invalid || this.creating()) return;
    this.creating.set(true);
    this.accountService
      .createAccount(this.form.getRawValue())
      .pipe(finalize(() => this.creating.set(false)))
      .subscribe({
        next: () => this.load(),
      });
  }
}
