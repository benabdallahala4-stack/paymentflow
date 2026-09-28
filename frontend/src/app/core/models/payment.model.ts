export type PaymentStatus = 'PENDING' | 'COMPLETED' | 'FAILED' | 'REJECTED';

export interface CreatePaymentRequest {
  sourceAccountId: string;
  destinationAccountId: string;
  amountMinorUnits: number;
  currency: string;
}

export interface Payment {
  transactionId: string;
  sourceAccountId: string;
  destinationAccountId: string;
  amountMinorUnits: number;
  currency: string;
  status: PaymentStatus;
  createdAt: string;
  replayed?: boolean;
}

export interface OutboxEventStatus {
  eventId: string;
  eventType: string;
  status: 'PENDING' | 'PUBLISHED' | 'FAILED';
  createdAt: string;
  publishedAt?: string;
}

// GET /admin/transactions/{id}: inspect a transaction end-to-end, including outbox status.
export interface AdminTransactionDetail extends Payment {
  outboxEvents: OutboxEventStatus[];
}
