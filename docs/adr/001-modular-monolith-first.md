# ADR-001: Modular Monolith First

## Status
Proposed

## Context
PaymentFlow needs a backend architecture. The obvious "portfolio-impressive" choice is a
microservices split (identity-service, account-service, payment-service, ledger-service).
But the core correctness requirement — a transfer and its ledger postings commit
atomically — is a single-database-transaction problem, and splitting it across services
would force a distributed transaction or saga to solve a problem the domain doesn't have
yet, purely for architectural appearance.

## Decision
Build PaymentFlow as a single Spring Boot deployable, internally divided into modules
(`identity`, `account`, `payment`, `ledger`, `shared`) with hexagonal layering per module
and dependency rules enforced by ArchUnit tests. See
`docs/architecture/modular-monolith.md` for the full module/dependency table.

## Alternatives Considered
- **Microservices from day one**: rejected — introduces distributed transactions/sagas
  for a consistency requirement that a single DB transaction solves cleanly; adds
  operational overhead (service discovery, network failure handling, distributed tracing)
  before there is any team-scaling or independent-deployment need to justify it.
- **Single unstructured Spring Boot app (no module boundaries)**: rejected — would let
  `payment` reach directly into `ledger`'s JPA entities, making the double-entry invariant
  easy to violate accidentally and hard to test in isolation.

## Consequences
- Faster to build correctly: one database transaction spans account/ledger/outbox writes.
- Module boundaries are enforced at compile/test time (ArchUnit), not at network
  boundaries, so a violation is a fast local test failure, not a production incident.
- If the project ever needed to scale a specific module independently, `payment`+`ledger`
  is the natural extraction seam — documented but explicitly not planned in Phase 0-8.
- Risk: without discipline, module boundaries can erode over time; mitigated by
  ArchUnit tests from Phase 1 (`docs/architecture/testing-strategy.md`).
