# API Design (Initial Outline)

Status: Phase 0 — outline only, no implementation. Endpoints are named/shaped for
planning; request/response bodies will be finalized in Phase 1.

## Conventions

- JSON over HTTPS (HTTP locally in Phase 1). Base path `/api/v1`.
- AuthN: `Authorization: Bearer <JWT>` issued by the auth endpoints.
- AuthZ roles: `CUSTOMER`, `ADMIN` (see `docs/architecture/security-strategy.md`).
- Idempotent writes require an `Idempotency-Key` header (required, not optional — see
  `postgres-concurrency-control` README §6 on why this must not be optional).
- List endpoints use **cursor pagination**: query params `after` (opaque cursor encoding
  the last seen `(created_at, id)` pair) and `limit` (default 20, max 100), ordered by
  `created_at DESC, id DESC` to give a stable total order even when multiple rows share a
  timestamp.

## Endpoints

| Method | Path | Auth role | Purpose |
|---|---|---|---|
| POST | `/auth/register` | none | Create a new user (CUSTOMER by default). |
| POST | `/auth/login` | none | Authenticate, return JWT access token. |
| POST | `/auth/refresh` | authenticated | Exchange a refresh token for a new access token. |
| GET | `/accounts` | CUSTOMER | List accounts owned by the caller. |
| POST | `/accounts` | CUSTOMER | Open a new account for the caller. |
| GET | `/accounts/{accountId}` | CUSTOMER (owner only) | Get one account's details/balance. |
| GET | `/accounts/{accountId}/transactions` | CUSTOMER (owner only) | Cursor-paginated transaction history for an account (`after`, `limit`). |
| POST | `/payments` | CUSTOMER | Create a payment/transfer. Requires `Idempotency-Key` header. Body: sourceAccountId, destinationAccountId, amountMinorUnits, currency. |
| GET | `/payments/{transactionId}` | CUSTOMER (party to txn) | Get payment status and details. |
| GET | `/payments` | CUSTOMER | Cursor-paginated list of the caller's payments (`after`, `limit`). |
| GET | `/admin/accounts/{accountId}` | ADMIN | Inspect any account (support/audit). |
| GET | `/admin/accounts/{accountId}/ledger-entries` | ADMIN | Raw ledger entries for an account, cursor-paginated. |
| GET | `/admin/transactions/{transactionId}` | ADMIN | Inspect any transaction end-to-end, including outbox event status. |
| GET | `/admin/transactions/{transactionId}/ledger-entries` | ADMIN | Ledger postings for a specific transaction (should sum to zero net). |

## Notes on specific endpoints

**`POST /payments`**
- `Idempotency-Key` is required; a retry with the same key + same body returns the
  original outcome (200 with `replayed: true`); same key + different body returns `422`.
- Synchronous response returns the transaction in `PENDING` or a terminal state if the
  synchronous path completes fast enough; clients should poll `GET /payments/{id}` or
  (Phase 4+) subscribe to a webhook/SSE for completion, since ledger posting happens
  inside a single DB transaction but the outbox-driven notification is async by design.
- Error surfacing mirrors the pattern in `postgres-concurrency-control`: `409` for a
  write conflict the client may retry, `422` for a business rule rejection (insufficient
  funds, reused idempotency key with different body), `503` once an internal retry budget
  is exhausted under contention (see ADR-004).

**Transaction history pagination**
- `GET /accounts/{id}/transactions?after=<cursor>&limit=20`
- Cursor encodes `(created_at, id)` of the last row of the previous page; ordering is
  `created_at DESC, id DESC` so pagination is stable under concurrent inserts (no
  offset-based `LIMIT/OFFSET`, which drifts under writes).

**Admin inspection endpoints**
- Read-only. ADMIN can never directly mutate a balance or ledger entry through the API —
  the only way money moves is through `POST /payments`, preserving the invariant that all
  balance changes are ledger-backed (see `docs/architecture/domain-model.md`).
