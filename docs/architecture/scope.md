# PaymentFlow (LedgerFlow) — Scope

Status: Phase 0 — planning only. Nothing described here is implemented.

## 1. What this project is

PaymentFlow (internal codename **LedgerFlow**) is a simplified digital payment platform
built around a **double-entry ledger**: every money movement between accounts is recorded
as at least two balanced ledger entries (one debit, one credit) rather than as a single
mutable balance field. The goal is to demonstrate correct handling of money under
concurrency, auditability, and idempotent retries in a modular-monolith Spring Boot
backend with a thin frontend, not to build a production payment processor.

This is a portfolio project. Scope is deliberately bounded: no card networks, no real bank
rails, no multi-currency FX, no real KYC/AML. It simulates a closed-loop wallet system
(think "internal ledger for an app", not "connects to SWIFT").

## 2. Functional requirements (FRs)

**Identity & accounts**
- FR-1: A person can register and authenticate (email + password) and receive a session
  (JWT) usable for subsequent requests.
- FR-2: A user can open one or more accounts (e.g. checking/savings-style wallets)
  denominated in a single currency (EUR for the reference implementation).
- FR-3: A user can view the balance and details of accounts they own; they cannot view or
  act on accounts they do not own (except ADMIN).

**Payments**
- FR-4: A user can transfer money from an account they own to another account, either one
  of their own accounts or another user's account.
- FR-5: A payment request is submitted with a client-generated `Idempotency-Key`; retried
  submissions with the same key and same payload return the original result rather than
  creating a second transfer.
- FR-6: The system prevents overdraft: a transfer that would take a source account below
  its allowed minimum balance is rejected, including under concurrent competing transfers
  against the same account.
- FR-7: A user can query the status of a payment (PENDING / COMPLETED / FAILED / REJECTED).
- FR-8: A user can list the transaction history of an account they own, paginated.

**Ledger**
- FR-9: Every completed payment produces balanced, immutable ledger entries (sum of debits
  equals sum of credits for the transaction) that can be replayed to reconstruct account
  balances independently of any cached/materialized balance field.
- FR-10: Ledger entries, once written, are never updated or deleted; corrections are made
  by posting new, reversing entries.

**Admin / operational**
- FR-11: An ADMIN role can inspect any account, any payment, and the raw ledger for
  support and audit purposes, but cannot directly mutate balances outside the payment flow.
- FR-12: Outbound domain events (e.g. `PaymentCompleted`) are published reliably via a
  transactional outbox, at-least-once, for eventual consumers (notification, analytics —
  simulated in later phases).

## 3. Non-functional requirements (NFRs)

These are **goals to design toward**, not measured facts — nothing is implemented or
benchmarked yet.

- **Consistency**: a transfer is atomic — money is never observed as debited from the
  source without eventually being credited to the destination, and never double-spent
  under concurrent requests. This is the primary correctness goal of the project.
- **Auditability**: every balance must be explainable as the sum of immutable ledger
  entries; no financial state exists only as a mutable counter with no paper trail.
- **Idempotency**: any client-facing write that can be retried by a network client must be
  safe to submit more than once.
- **Latency (goal, not measured)**: p95 for a payment creation call under light load should
  target < 300 ms once implemented; this will be validated by load-tests/, not assumed.
- **Availability (goal)**: no single slow lock should be able to stall unrelated accounts;
  a hot-row conflict on one account must not degrade throughput for other accounts.
- **Observability**: every payment must be traceable end-to-end (API → DB → outbox → Kafka)
  once Phase 6 lands; Phase 0 only documents the plan (see
  `docs/system-design/observability-roadmap.md`).
- **Security**: authentication and authorization are enforced on every account/payment
  endpoint; customers can never read or act on another customer's data.
- **Portability**: the whole system must run locally via Docker Compose with no dependency
  on a cloud account (see `docs/system-design/infrastructure-roadmap.md`).

## 4. Explicit non-goals (Phase 0 decision)

- No real payment rails, card processing, or bank integration.
- No multi-currency conversion (single currency, minor-units integer, see
  `docs/database/schema.md`).
- No production deployment to AWS — infrastructure beyond Phase 1 (Kubernetes, Terraform,
  AWS services) is documented/simulated only, per `docs/system-design/infrastructure-roadmap.md`.
- No custom cryptography (see `docs/architecture/security-strategy.md`).

## 5. Open questions carried into Phase 1

- Final concurrency control strategy for the hot debit path — see ADR-004, informed by
  `postgres-concurrency-control`.
- Whether `transactions` and `ledger_entries` are one table or two — see
  `docs/database/schema.md` §"Open question".
