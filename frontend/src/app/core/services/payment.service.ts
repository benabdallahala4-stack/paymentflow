import { HttpClient, HttpContext, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CreatePaymentRequest, Payment } from '../models/payment.model';
import { CursorPage } from '../models/transaction.model';
import { SKIP_ERROR_TOAST } from '../interceptors/error.interceptor';

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private readonly baseUrl = `${environment.apiBaseUrl}/payments`;

  constructor(private readonly http: HttpClient) {}

  /**
   * Creates a payment. `idempotencyKey` must be generated once per new submission
   * (see idempotency-key.util.ts) and reused verbatim for retries of that same attempt.
   */
  createPayment(request: CreatePaymentRequest, idempotencyKey: string): Observable<Payment> {
    return this.http.post<Payment>(this.baseUrl, request, {
      headers: { 'Idempotency-Key': idempotencyKey },
      // The transfer form shows its own inline error state for 409/422 business
      // rejections, so we suppress the generic global toast for this call only.
      context: new HttpContext().set(SKIP_ERROR_TOAST, true),
    });
  }

  getPayment(transactionId: string): Observable<Payment> {
    return this.http.get<Payment>(`${this.baseUrl}/${transactionId}`);
  }

  listPayments(after: string | null, limit = 20): Observable<CursorPage<Payment>> {
    let params = new HttpParams().set('limit', limit);
    if (after) params = params.set('after', after);
    return this.http.get<CursorPage<Payment>>(this.baseUrl, { params });
  }
}
