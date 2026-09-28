import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { environment } from '../../../environments/environment';
import { TransferComponent } from './transfer.component';

describe('TransferComponent', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TransferComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  function createComponent() {
    const fixture = TestBed.createComponent(TransferComponent);
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiBaseUrl}/accounts`).flush([]);
    return fixture;
  }

  it('rejects a submission with a zero or negative amount', () => {
    const fixture = createComponent();
    const component = fixture.componentInstance;
    component.form.setValue({
      sourceAccountId: 'acc-1',
      destinationAccountId: 'acc-2',
      amount: '0',
      currency: 'EUR',
    });
    expect(component.form.invalid).toBe(true);
    expect(component.form.controls.amount.errors).toEqual({ positiveAmount: true });
  });

  it('accepts a submission with a positive amount and required fields', () => {
    const fixture = createComponent();
    const component = fixture.componentInstance;
    component.form.setValue({
      sourceAccountId: 'acc-1',
      destinationAccountId: 'acc-2',
      amount: '25.50',
      currency: 'EUR',
    });
    expect(component.form.valid).toBe(true);
  });

  it('requires source and destination account ids', () => {
    const fixture = createComponent();
    const component = fixture.componentInstance;
    component.form.setValue({
      sourceAccountId: '',
      destinationAccountId: '',
      amount: '10',
      currency: 'EUR',
    });
    expect(component.form.invalid).toBe(true);
  });

  it('reuses the same Idempotency-Key header when retrying a failed submission, and rotates it after a successful one', () => {
    const fixture = createComponent();
    const component = fixture.componentInstance;
    component.form.setValue({
      sourceAccountId: 'acc-1',
      destinationAccountId: 'acc-2',
      amount: '10',
      currency: 'EUR',
    });

    const initialKey = component.currentIdempotencyKey;

    // First attempt fails (e.g. network/server error) -> key must stay the same for a retry.
    component.submit();
    const firstReq = httpMock.expectOne(`${environment.apiBaseUrl}/payments`);
    expect(firstReq.request.headers.get('Idempotency-Key')).toBe(initialKey);
    firstReq.flush({ message: 'conflict' }, { status: 409, statusText: 'Conflict' });

    expect(component.currentIdempotencyKey).toBe(initialKey);

    // Retry of the SAME attempt must send the identical key.
    component.submit();
    const retryReq = httpMock.expectOne(`${environment.apiBaseUrl}/payments`);
    expect(retryReq.request.headers.get('Idempotency-Key')).toBe(initialKey);
    retryReq.flush({
      transactionId: 'tx-1',
      sourceAccountId: 'acc-1',
      destinationAccountId: 'acc-2',
      amountMinorUnits: 1000,
      currency: 'EUR',
      status: 'COMPLETED',
      createdAt: new Date().toISOString(),
    });

    // After success, a brand-new logical transfer must get a fresh key.
    expect(component.currentIdempotencyKey).not.toBe(initialKey);
  });
});
