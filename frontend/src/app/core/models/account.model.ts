export interface Account {
  id: string;
  ownerId: string;
  currency: string;
  balanceMinorUnits: number;
  createdAt: string;
}

export interface CreateAccountRequest {
  currency: string;
}

export interface LedgerEntry {
  id: string;
  accountId: string;
  transactionId: string;
  direction: 'DEBIT' | 'CREDIT';
  amountMinorUnits: number;
  createdAt: string;
}
