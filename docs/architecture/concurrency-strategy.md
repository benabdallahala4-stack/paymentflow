# Concurrency Strategy

Status: Phase 0 — this document surveys options and states a **working hypothesis only**.
The final decision belongs to **ADR-004**, made after Phase 2 runs the actual experiments
against a real PostgreSQL instance (Testcontainers), the same way
`postgres-concurrency-control` did.

## Why this needs a dedicated document

PaymentFlow's core correctness risk is the lost-update anomaly on account balances under
concurrent transfers, plus the write-skew risk on any multi-row invariant we might add
later (e.g. "an account cannot be closed while it has a pending transaction"). The sibling
project `postgres-concurrency-control` (`/home/ala/gitlab/postgres-concurrency-control`)
is a standalone, runnable proof of exactly this problem and its three standard fixes —
ADR-004 should reference it directly as prior art rather than re-deriving the arguments.

## Options surveyed (adapted from postgres-concurrency-control)

| | Naive (no control) | Optimistic locking | Pessimistic locking | SERIALIZABLE isolation |
|---|---|---|---|---|
| Mental model | Read-modify-write, hope | Assume no conflict, check at write time | Assume conflict, lock before reading | Let PostgreSQL verify the whole schedule |
| Mechanism | none | `version` column, conditional `UPDATE` | `SELECT ... FOR UPDATE` | Serializable Snapshot Isolation |
| On conflict | **silently loses a write** | 2nd writer gets an exception, must retry | 2nd writer waits | one txn aborted with `40001`, must retry |
| Blocks? | no | never | yes | no |
| Deadlock risk | no | no | yes (needs consistent lock ordering) | no (aborts instead) |
| Catches write skew? | no | no | only if the right row is locked | yes |
| Works across HTTP think-time | n/a | yes | no | no |
| Measured throughput under hot-row contention (reference project, 8 threads × 25 ops on 1 row) | not measured (broken) | 272 ops/s, 12 dropped after retry budget | 539 ops/s, 0 dropped | 386 ops/s, 6 dropped |

The throughput row is copied from `postgres-concurrency-control`'s
`ContentionBenchmarkTest` (laptop numbers, shape not absolute values) and is cited as
representative prior evidence, not as a PaymentFlow-specific measurement — Phase 2 will
re-run an equivalent benchmark against PaymentFlow's actual schema before ADR-004 is
finalized.

## Working hypothesis (NOT YET DECIDED)

> **To be validated in Phase 2, not yet decided.** The current working hypothesis is:
> **optimistic locking (`@Version` on `accounts`) as the default control for transfers**,
> with **pessimistic locking (`SELECT ... FOR UPDATE`, `NOWAIT`) reserved for the hot
> debit path** if a specific account proves to be a genuine contention hotspot (e.g. a
> shared "house" or "fee collection" account touched by many concurrent transfers).

Rationale for the hypothesis, pending validation:
- Most transfers in a payments app are per-user, low-contention — optimistic locking is
  the cheapest correct default (per the reference project's decision procedure, §"Choosing,
  as a decision procedure").
- A small number of accounts (system/treasury/fee accounts) are structurally hot and
  known in advance — those are candidates for pessimistic `FOR UPDATE ... NOWAIT` so a
  saturated row degrades as fast `409`s rather than growing retry storms.
- Multi-row invariants beyond simple balance checks (none currently in scope, but e.g. a
  future "daily transfer limit across all of a user's accounts") would be candidates for
  SERIALIZABLE rather than hand-rolled locking, per the write-skew argument in
  `postgres-concurrency-control` §4.
- Consistent lock ordering (`Math.min`/`Math.max` on account IDs) must be adopted for any
  pessimistic path touching two accounts, to avoid the deadlock scenario documented in
  the reference project.

## What Phase 2 must produce before ADR-004 is finalized

1. A `LostUpdateTest`-equivalent proving the naive approach loses money on PaymentFlow's
   actual `accounts`/`transactions` schema.
2. Equivalent tests for optimistic, pessimistic, and SERIALIZABLE strategies against that
   schema.
3. A contention benchmark (low vs. high contention) measured on PaymentFlow's own code,
   not assumed from the reference project.
4. An idempotency test proving retries triggered by any of the above are safe (no double
   payment), following the `IdempotencyTest` pattern.

Only after these experiments exist does ADR-004 record a final, production decision.
Until then, this document — not the ADR — is the source of truth for "what we currently
believe", explicitly labeled as unvalidated.
