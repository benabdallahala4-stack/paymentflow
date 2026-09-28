import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Account, CreateAccountRequest, LedgerEntry } from '../models/account.model';
import { AdminTransactionDetail } from '../models/payment.model';
import { CursorPage, TransactionHistoryItem } from '../models/transaction.model';

@Injectable({ providedIn: 'root' })
export class AccountService {
  private readonly baseUrl = `${environment.apiBaseUrl}/accounts`;

  constructor(private readonly http: HttpClient) {}

  listAccounts(): Observable<Account[]> {
    return this.http.get<Account[]>(this.baseUrl);
  }

  getAccount(accountId: string): Observable<Account> {
    return this.http.get<Account>(`${this.baseUrl}/${accountId}`);
  }

  createAccount(request: CreateAccountRequest): Observable<Account> {
    return this.http.post<Account>(this.baseUrl, request);
  }

  getTransactionHistory(
    accountId: string,
    after: string | null,
    limit = 20,
  ): Observable<CursorPage<TransactionHistoryItem>> {
    let params = new HttpParams().set('limit', limit);
    if (after) {
      params = params.set('after', after);
    }
    return this.http.get<CursorPage<TransactionHistoryItem>>(
      `${this.baseUrl}/${accountId}/transactions`,
      { params },
    );
  }
}

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly baseUrl = `${environment.apiBaseUrl}/admin`;

  constructor(private readonly http: HttpClient) {}

  getAccount(accountId: string): Observable<Account> {
    return this.http.get<Account>(`${this.baseUrl}/accounts/${accountId}`);
  }

  getAccountLedgerEntries(
    accountId: string,
    after: string | null,
    limit = 20,
  ): Observable<CursorPage<LedgerEntry>> {
    let params = new HttpParams().set('limit', limit);
    if (after) params = params.set('after', after);
    return this.http.get<CursorPage<LedgerEntry>>(
      `${this.baseUrl}/accounts/${accountId}/ledger-entries`,
      { params },
    );
  }

  getTransaction(transactionId: string): Observable<AdminTransactionDetail> {
    return this.http.get<AdminTransactionDetail>(`${this.baseUrl}/transactions/${transactionId}`);
  }

  getTransactionLedgerEntries(transactionId: string): Observable<LedgerEntry[]> {
    return this.http.get<LedgerEntry[]>(
      `${this.baseUrl}/transactions/${transactionId}/ledger-entries`,
    );
  }
}
