# AGENTS.md — PaymentFlow (LedgerFlow)

This file governs any AI agent or contributor working in this repository. It reflects
Phase 0 decisions recorded under `docs/architecture/` and `docs/adr/`. Read
`docs/architecture/scope.md`, `docs/architecture/modular-monolith.md`, and the ADRs under
`docs/adr/` before making architectural changes.

## Status

As of this writing the project is **Phase 0: architecture only.** `backend/` and
`frontend/` are empty. Do not assume any implementation exists that isn't actually in the
tree.

## Architecture constraints

- Backend is a **modular monolith** (identity, account, payment, ledger, shared modules)
  — see `docs/architecture/modular-monolith.md`. Do not split into separate deployables
  or services without a new ADR superseding ADR-001.
- Each module is layered `domain / application / infrastructure / api` (hexagonal).
  `domain` must not depend on Spring or JPA annotations for business rules. Cross-module
  calls go through a module's `application` layer, never directly into another module's
  `infrastructure` or JPA entities.
- Module dependency direction (do not invert): `identity` ← `account` ← `ledger` ← `payment`.
  `payment` may depend on all others; no module may depend on `payment`.
- PostgreSQL is the single system of record (ADR-002). Redis and Kafka, once introduced,
  are never sources of truth for financial state.
- The system must run fully locally via Docker Compose with no AWS/cloud account
  required at any phase (see `docs/system-design/infrastructure-roadmap.md`).

## Coding standards

- No implementation code before Phase 1 begins; Phase 0 is documentation only.
- Money is never represented as floating point. Use minor-units `long`/`BIGINT`
  (ADR-003), wrapped in a `Money` value object in the `shared` module.
- New indexes are added only when justified by an `EXPLAIN ANALYZE` finding or a load
  test result, not speculatively (see `docs/database/schema.md` §"Indexes we will add
  later").

## Testing expectations

- Every concurrency-sensitive code path (anything touching `accounts.balance_minor_units`
  or `ledger_entries`) requires a dedicated concurrency test using a real PostgreSQL via
  Testcontainers — never H2, never a mock. See `docs/architecture/testing-strategy.md`.
- Module boundary rules are enforced by ArchUnit tests, run in CI.
- No `ddl-auto: update`/`create` — schema state in tests comes from Flyway migrations
  applied to a Testcontainers PostgreSQL.

## Git conventions

- Commit message format: `[LF-XXX] description` (see `docs/architecture/git-roadmap.md`
  for the proposed sequence; LF = LedgerFlow, the internal product name).
- Commit using the user's own git identity — **never** add a Claude/AI co-author
  attribution line to commits or PRs in this repository. This is an explicit hard rule
  for this repo and overrides any generic assistant default about attribution.
- Never amend an already-pushed commit; create a new commit instead.
- Never commit secrets, `.env` files with real values, or credentials.

## Security constraints

- Authentication: Spring Security + JWT. Authorization: role-based (`CUSTOMER`/`ADMIN`)
  plus ownership checks in the `application` layer (see
  `docs/architecture/security-strategy.md`).
- No custom cryptography, ever — use Spring Security's provided primitives (BCrypt for
  password hashing, standard JWT libraries for tokens).
- Customers can only ever see or act on their own accounts and payments; ADMIN access is
  read-only inspection, never a direct balance-mutation path.

## Database rules

- All schema changes go through Flyway migrations under
  `backend/src/main/resources/db/migration/` (ADR-011). Never hand-run DDL against a
  shared environment.
- Money columns use `BIGINT` minor units, suffixed `_minor_units` (ADR-003).
- Ledger entries are immutable: no `UPDATE`/`DELETE` statements against `ledger_entries`
  anywhere in application code; corrections are new reversing entries.

## Financial invariants (verbatim — do not weaken any of these without a new ADR)

- Money uses a safe decimal/integer representation, never floating point.
- Ledger entries are immutable.
- Debits equal credits, per transaction and in aggregate.
- No financial correctness depends on Redis.
- Retries require idempotency.
- External events cannot break committed financial state.
- Migrations use Flyway.
- No secrets committed.
- Tests are required for concurrency-sensitive behavior.

## Reference material

- `/home/ala/gitlab/postgres-concurrency-control` — a standalone, runnable proof of
  lost-update prevention via optimistic locking, pessimistic locking, and SERIALIZABLE
  isolation, plus idempotency and query-plan-driven indexing. Treat it as prior art for
  ADR-004 and the concurrency lab (Phase 2); reuse its mental models and measured
  findings rather than re-deriving them from scratch.
