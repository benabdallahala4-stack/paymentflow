# Testing Strategy

Status: Phase 0 — plan only; no tests exist yet.

## Testing pyramid

1. **Unit tests** — domain and application layer logic in isolation (e.g. `Money`
   arithmetic, invariant checks, use-case orchestration with mocked ports). No Spring
   context, fast, the bulk of the test count.
2. **Repository/integration tests** — real PostgreSQL via **Testcontainers** (never H2 or
   a mock, matching `postgres-concurrency-control`'s explicit rationale: "none of these
   anomalies reproduce on H2 or on a mock — they are properties of PostgreSQL's MVCC
   implementation"). Covers repository queries, Flyway migrations applying cleanly, and
   constraint enforcement (`CHECK`, unique, FK).
3. **Concurrency tests (first-class category, not a subset of integration tests)** —
   dedicated tests proving lost-update prevention, write-skew handling (if applicable),
   deadlock-safe lock ordering, and idempotent-retry safety, following the reference
   project's pattern precisely:
   - A rendezvous barrier (`CyclicBarrier`) placed at the exact seam between read and
     write so a bad interleaving is guaranteed, not left to luck.
   - No `@Transactional` on the test class (would wrap every thread in one shared
     transaction, defeating the test).
   - A contention benchmark test (low-contention vs. high-contention) measuring ops/sec,
     rejected count, and retry count per concurrency strategy, run against PaymentFlow's
     own schema (feeding ADR-004).
4. **Architecture tests (ArchUnit)** — mechanically enforce the module dependency rules
   and hexagonal layering from `docs/architecture/modular-monolith.md` (e.g. `domain`
   packages must not depend on Spring/JPA; `payment` may depend on `ledger` but not vice
   versa). Run in CI on every build.
5. **Frontend tests** — component/unit tests for the minimal frontend (ADR-009); no
   heavy investment given the frontend's deliberately small scope.
6. **End-to-end tests** — a small number of full-stack scenarios (register → open account
   → transfer → verify ledger balances) run against a docker-compose'd stack in CI,
   exercising the real HTTP API rather than in-process calls.

## Concurrency tests as first-class

Concurrency correctness is the project's headline risk, so concurrency tests are not
treated as "extra" integration tests — they get their own test package
(`test/.../architecture` is for ArchUnit; concurrency tests live alongside each module's
integration tests but are tagged/reported separately) and their own CI stage, so a
concurrency regression cannot be silently absorbed into a general "integration tests
passed" signal.

## What "done" looks like per module (Phase 1 exit criteria)

- `account`: unit tests for balance invariants, repository tests for `CHECK` constraint
  enforcement, at least one concurrency test proving overdraft cannot happen under
  concurrent transfers.
- `payment`: idempotency test suite mirroring `IdempotencyTest` (duplicate submissions,
  different-payload rejection, concurrent duplicate submissions).
- `ledger`: unit tests for balance-equals-debits-minus-credits reconciliation.
- `identity`: unit tests for password hashing/verification, authorization/ownership
  checks.
