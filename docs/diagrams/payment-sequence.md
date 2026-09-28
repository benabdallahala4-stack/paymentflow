# Payment Sequence Diagram

Status: Phase 0. Describes the target flow for `POST /payments`; nothing below is
implemented yet.

```mermaid
sequenceDiagram
    actor Client
    participant API as Payment API (controller)
    participant App as Payment application service
    participant Acct as Account module (validation)
    participant DB as PostgreSQL (single transaction)
    participant Ledger as Ledger module
    participant Outbox as outbox_events (same transaction)
    participant Pub as Kafka publisher (poller)
    participant Kafka
    participant Consumer as Async consumers

    Client->>API: POST /payments (Idempotency-Key, body)
    API->>App: handle(createPaymentCommand)
    App->>DB: check idempotency_keys for (user, key)
    alt key already used, same fingerprint
        DB-->>App: existing transaction
        App-->>API: 200, replayed=true
    else new key or different fingerprint
        App->>Acct: validate source account (ownership, sufficient funds)
        Acct-->>App: OK or rejection
        App->>DB: BEGIN transaction
        App->>DB: lock/version-check source account (ADR-004 strategy)
        App->>DB: debit source, credit destination (balance read model)
        App->>Ledger: post balanced ledger entries (debit + credit)
        Ledger->>DB: INSERT ledger_entries (immutable)
        App->>DB: INSERT idempotency_keys row
        App->>Outbox: write PaymentCompleted event (same transaction)
        App->>DB: COMMIT
        DB-->>App: success or 40001/conflict (see ADR-004)
        App-->>API: 201 Created, transaction=COMPLETED
    end
    API-->>Client: response

    Note over Pub,Kafka: Phase 4 — asynchronous, decoupled from the commit above
    Pub->>Outbox: poll unpublished rows
    Pub->>Kafka: publish PaymentCompleted
    Kafka->>Consumer: deliver (at-least-once)
```

## Explanation

- **Client → API → Payment application service → Account validation → PostgreSQL
  transaction → Ledger → Outbox → Commit** is all **Phase 1**, and all inside **one**
  database transaction: the balance update, the ledger postings, the idempotency-key
  insert, and the outbox row are committed atomically or not at all. This is the same
  "store the key in the same transaction as the operation" principle demonstrated by
  `IdempotentPayoutService` in `postgres-concurrency-control`.
- The **lock/version-check** step is where ADR-004's chosen concurrency strategy applies
  (optimistic `@Version` check, `SELECT ... FOR UPDATE`, or `SERIALIZABLE` — see
  `docs/architecture/concurrency-strategy.md`). A conflict here surfaces to the client as
  `409` (retryable) or `422` (business rejection), matching the HTTP semantics documented
  in `postgres-concurrency-control`.
- **Kafka publisher → Kafka → consumers** is **Phase 4**, drawn with a note to mark it
  explicitly decoupled and asynchronous — a slow or down consumer never blocks or reverses
  a committed payment.
- Retrying the whole request with the same `Idempotency-Key` re-enters this flow at the
  "check idempotency_keys" step and short-circuits to a replayed response, never
  re-executing the debit/credit.
