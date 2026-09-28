# ADR-004: Concurrency Control Strategy

## Status
Proposed (decision deferred — see below)

## Context
Concurrent transfers against the same account must not lose updates or allow overdraft.
`postgres-concurrency-control` is a runnable, tested proof of the three standard fixes
(optimistic locking, pessimistic `SELECT ... FOR UPDATE`, SERIALIZABLE isolation) plus the
write-skew anomaly that only SERIALIZABLE catches, and a benchmark showing their relative
cost under contention. PaymentFlow reuses that project's mental models and comparison
table (`docs/architecture/concurrency-strategy.md`) rather than re-deriving them.

## Decision
**This ADR does not make a final production decision in Phase 0.** Per the instructions
governing this documentation pass, the decision is explicitly deferred to Phase 2, after
PaymentFlow runs its own equivalent of `postgres-concurrency-control`'s test suite
(`LostUpdateTest`, per-strategy tests, `ContentionBenchmarkTest`, `IdempotencyTest`)
against its own schema. The working hypothesis carried forward — **optimistic locking as
the default, pessimistic locking reserved for identified hot rows** — is documented in
`docs/architecture/concurrency-strategy.md` and is explicitly marked "to be validated in
Phase 2, not yet decided."

## Alternatives Considered
(To be re-evaluated with evidence in Phase 2; listed here as the candidate set.)
- **Optimistic locking everywhere** — cheapest default, but the reference project's
  benchmark shows an ~6% drop-after-retry-budget rate on a genuinely hot row.
- **Pessimistic locking everywhere** — best hot-row throughput and zero drops in the
  reference benchmark, but serializes all writes to one row and risks deadlocks/pool
  exhaustion if applied indiscriminately.
- **SERIALIZABLE everywhere** — simplest mental model (stop reasoning about anomalies),
  catches write skew, but mandates retries on every transaction including reads, and
  degrades as transaction size/read-set grows.
- **Naive (no control)** — the baseline the reference project proves loses money; not a
  real candidate, kept only as the regression test target.

## Consequences
- Phase 2 must produce PaymentFlow-specific versions of the reference project's tests
  before this ADR can move to "Accepted".
- Until Phase 2 completes, `accounts.version` (BIGINT) is included in the Phase 1 schema
  (`docs/database/schema.md`) so optimistic locking is available as the default without a
  later migration, keeping the door open for pessimistic locking on specific hot paths
  without a schema change.
- Any pessimistic locking path touching two accounts (a transfer) must acquire locks in a
  consistent order (`min(id), max(id)`) to avoid the deadlock scenario demonstrated in
  `postgres-concurrency-control`.
- This ADR will be revised (not superseded) once Phase 2 evidence exists, updating Status
  to "Accepted" with the final choice and measured numbers.
