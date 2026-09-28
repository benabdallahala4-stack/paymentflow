# ADR-005: Idempotency Key Design for Payments

## Status
Proposed

## Context
Any client that times out waiting for `POST /payments` has no way to know if the transfer
happened, and will retry. Without protection, a retry can create a duplicate transfer.
`postgres-concurrency-control`'s `IdempotentPayoutService`/`IdempotencyTest` demonstrate
the three rules that make this safe: store the key in the same transaction as the
operation, let a unique constraint (not a check-then-insert) arbitrate concurrent
duplicates, and fingerprint the request body to reject a reused key with a different
payload.

## Decision
Adopt the same pattern for PaymentFlow: `Idempotency-Key` is a **required** HTTP header
on `POST /payments`. The `idempotency_keys` table (PK on `(user_id, idempotency_key)`,
see `docs/database/schema.md`) is written in the **same database transaction** as the
`transactions`/`ledger_entries` rows it guards. A unique-violation (`23505`) on concurrent
duplicate submissions is caught by the application and turned into a replay lookup rather
than an error.

## Alternatives Considered
- **Optional idempotency key**: rejected — per the reference project, "make it optional
  and the dangerous path becomes the default path."
- **Application-level check-then-insert (`SELECT` then `INSERT` if absent)**: rejected —
  race condition between concurrent replays; the reference project shows this must be
  arbitrated by the database's unique constraint, not application logic.
- **Deduplicating on payload hash instead of a client-supplied key**: rejected — would
  incorrectly collapse two genuine, identical-amount payments into one; only the client
  knows whether a request is a retry or a new instruction.

## Consequences
- Every write path that can be retried by a network client (currently: payment creation)
  must persist its idempotency record in the same transaction as its side effect.
- `persist()` semantics (not `save()`/`merge()` on an assigned ID) must be used when
  inserting the idempotency record, to guarantee an `INSERT` (and thus a real unique
  violation) rather than a silently successful `UPDATE` — this is a specific bug the
  reference project shipped and caught; called out here so Phase 1 doesn't repeat it.
- A reused key with a different request fingerprint returns `422`, a documented and
  tested business error, not a silent overwrite.
- Non-database side effects (none yet planned for Phase 1; outbox events in later phases)
  must happen after commit, never inside the guarded transaction.
