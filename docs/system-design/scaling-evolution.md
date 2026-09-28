# Scaling Evolution

Status: Phase 0 — **DOCUMENT/SIMULATE ONLY.** None of the five stages below are
implemented. This document exists to demonstrate the reasoning process for scaling a
ledger system, not to schedule the work.

## Stage 1 — Single PostgreSQL instance (the actual Phase 1-5 target)

One primary PostgreSQL database holds everything: users, accounts, transactions, ledger
entries. This is sufficient for the project's real load (a portfolio demo, load-tested at
modest concurrency in `load-tests/`). No scaling work is justified until this stage is
demonstrably insufficient — which it will not be, in this project's real usage.

## Stage 2 — Indexing, evidence-driven

Add composite/covering indexes only where `EXPLAIN ANALYZE` on real query patterns shows a
sequential scan or heap-fetch cost that matters (see `docs/database/schema.md` §"Indexes
we will add later"), following the reference project's methodology: check
estimated-vs-actual row counts, run `ANALYZE`, look at scan node types, before adding
anything. Simulated here as the first lever, before reaching for more infrastructure.

## Stage 3 — Read replicas

For read-heavy endpoints (transaction history, admin inspection queries), a
PostgreSQL read replica could take load off the primary. Problem this introduces:
**replication lag** — a client that just completed a payment and immediately queries
history via a replica might not see it yet. Mitigation pattern (documented, not built):
read-your-writes routing (route a user's own recent-history read to the primary for a
short window) or accept eventual consistency for history views specifically, since the
authoritative balance always comes from the primary.

## Stage 4 — Partitioning

`ledger_entries` (and possibly `transactions`) partitioned by `created_at` (range,
monthly), following the `audit_event` pattern in `postgres-concurrency-control` §7:
inserts land in one small hot partition, queries with a date filter prune to one
partition, and archival becomes `DROP TABLE` on an old partition instead of a `DELETE`.
Problem this introduces: any query without a `created_at` filter scans every partition —
the partition key must match how the data is actually queried, which for a ledger (nearly
always queried "for this account, recent first" or "for this transaction") aligns well
with time-based partitioning but requires deliberate query discipline.

## Stage 5 — Sharding (if justified — a large "if")

Only if a single PostgreSQL primary's write throughput became the actual bottleneck (not
expected for this project) would sharding by, e.g., `account_id` hash or range be
considered. Problems this introduces, documented deliberately because they are the
interesting part:

- **Shard key choice**: `account_id` is the natural candidate (most queries are
  account-scoped), but a transfer *always* touches two accounts, which may land on
  different shards.
- **Cross-shard transactions**: a transfer between accounts on different shards cannot
  use a single local ACID transaction. Options (documented only): a two-phase commit
  protocol (operationally painful), or a saga with compensating actions (reintroduces the
  distributed-transaction complexity that ADR-001 specifically avoided by staying a
  monolith against one database) — this is precisely why sharding is listed as "if
  justified" rather than a planned stage: it reintroduces the hardest problem the
  monolith-first, single-DB decision was made to sidestep.
- **Ledger global invariants**: "debits equal credits system-wide" becomes a cross-shard
  reconciliation job rather than a single `CHECK`/transaction guarantee, requiring
  periodic audit reconciliation instead of real-time enforcement.

## Explicit framing

Stages 3-5 are **not on the implementation roadmap** (`docs/architecture/implementation-phases.md`
stops well short of them). They are included to show the evolution has been thought
through, not to imply the project will build them.
