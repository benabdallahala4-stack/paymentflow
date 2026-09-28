export interface TransactionHistoryItem {
  id: string;
  accountId: string;
  transactionId: string;
  direction: 'DEBIT' | 'CREDIT';
  amountMinorUnits: number;
  currency: string;
  counterpartyAccountId?: string;
  createdAt: string;
}

// Cursor-paginated list response shape used across `after`/`limit` endpoints.
// The backend documents the cursor as an opaque encoding of (created_at, id) of the
// last row of the page; we treat it as an opaque string and never parse it client-side.
export interface CursorPage<T> {
  items: T[];
  nextCursor: string | null;
}
