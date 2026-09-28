# ADR-006: Transactional Outbox for Domain Events

## Status
Proposed

## Context
PaymentFlow wants to publish domain events (e.g. `PaymentCompleted`) to Kafka for
downstream consumers (notifications, analytics — Phase 4+), without ever risking a
committed payment whose event silently never gets published, or a published event for a
payment that then rolls back. Publishing to Kafka directly inside the same database
transaction is not possible (Kafka is not transactional with PostgreSQL), and publishing
after commit with no persisted record risks losing the event if the process crashes
between commit and publish.

## Decision
Use the transactional outbox pattern: write an `outbox_events` row in the **same**
database transaction as the ledger postings (see the sequence in
`docs/diagrams/payment-sequence.md`). A separate poller/publisher process (Phase 4) reads
unpublished rows (`WHERE published_at IS NULL`, indexed) and publishes them to Kafka,
marking them published on success. Delivery is at-least-once; consumers must be
idempotent.

## Alternatives Considered
- **Dual write (write DB, then publish to Kafka directly)**: rejected — not atomic; a
  crash between the two steps loses the event or, worse, publishes an event for a
  transaction that then fails to commit.
- **Change Data Capture (e.g. Debezium reading the WAL)**: viable and arguably more
  robust long-term, but adds operational complexity (Kafka Connect, Debezium connector)
  not justified until Phase 4/5; the outbox table + simple poller is documented as the
  Phase 1-4 approach, with CDC left as a possible later upgrade, not a current plan.
- **No async events at all, purely synchronous API responses**: rejected — real payment
  systems need to notify other systems asynchronously; the outbox pattern is included
  specifically to demonstrate this without coupling correctness to Kafka's availability.

## Consequences
- No financial correctness ever depends on Kafka being up or a consumer being healthy
  (per AGENTS.md's invariant, extended from Redis to Kafka).
- The outbox table grows unboundedly unless pruned; a cleanup job (delete rows older than
  N days where `published_at IS NOT NULL`) is a Phase 4 implementation detail, not
  designed in Phase 0.
- Publisher must be idempotent-safe on the Kafka side (at-least-once delivery, consumers
  dedupe on event id).
- The poller introduces publish latency (poll interval) between commit and Kafka
  delivery — acceptable because nothing synchronous waits on it.
