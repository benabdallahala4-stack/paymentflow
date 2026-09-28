# ADR-002: PostgreSQL as the System of Record

## Status
Proposed

## Decision
PostgreSQL is the single system of record for all financial state (users, accounts,
transactions, ledger entries, idempotency keys, outbox events). No other datastore
(Redis, Kafka, a document store) may ever be the source of truth for a balance or a
ledger entry.

## Context
PaymentFlow needs strong transactional guarantees for money movement: atomic
multi-table writes (balance + ledger + idempotency key + outbox row), constraint
enforcement (`CHECK (balance >= 0)`), and a proven concurrency-control story. The sibling
project `postgres-concurrency-control` demonstrates in a runnable form that PostgreSQL's
MVCC, row locking, and Serializable Snapshot Isolation are sufficient to build correct
financial primitives without external coordination (e.g. a distributed lock service).

## Alternatives Considered
- **A NoSQL document store (e.g. MongoDB)** for accounts/ledger: rejected — multi-document
  transactional guarantees are weaker/more awkward than PostgreSQL's, and there is no
  compelling read/write pattern here (extreme write throughput, flexible schema) that
  would justify giving up `CHECK` constraints, foreign keys, and mature isolation levels.
- **Event-sourcing the ledger as the only store, no relational balance table**: rejected
  for Phase 1 — the immutable `ledger_entries` table already gives most event-sourcing
  benefits (auditability, replayability) while `accounts.balance_minor_units` stays as a
  fast, ordinary read model reconciled against it. A pure event store adds projection
  infrastructure not justified at this scale.
- **MySQL**: rejected — SERIALIZABLE isolation in MySQL is lock-based and behaves
  differently from PostgreSQL's SSI (see `postgres-concurrency-control` README, "a
  PostgreSQL-specific comfort level"); we want the SSI behavior specifically.

## Consequences
- All financial invariants (CHECK constraints, FK integrity, uniqueness) are enforced by
  the database, not solely by application code.
- Concurrency-control experiments (ADR-004) can reuse the exact same techniques already
  proven in `postgres-concurrency-control`.
- Schema evolution goes exclusively through Flyway migrations (see ADR-011).
- Scaling beyond a single primary (read replicas, partitioning, sharding) is documented
  as a later-stage evolution, not implemented now — see
  `docs/system-design/scaling-evolution.md`.
