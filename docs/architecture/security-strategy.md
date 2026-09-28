# Security Strategy

Status: Phase 0 — plan only; nothing implemented.

## Roles

- **CUSTOMER** — default role for a registered user. Can manage only their own accounts
  and payments.
- **ADMIN** — read-only inspection access across all accounts, transactions, and ledger
  entries for support/audit purposes (see `docs/architecture/api-design.md` admin
  endpoints). ADMIN cannot mutate a balance directly; the only mutation path is the
  ordinary payment flow, preserving the ledger-backed-balance invariant.

## Authentication vs. authorization

- **Authentication** answers "who is calling" — implemented via Spring Security with JWT
  bearer tokens (ADR-007). A request without a valid, unexpired token is rejected with
  `401` before it reaches any controller logic.
- **Authorization** answers "is this caller allowed to do this" — two layers:
  1. **Role-based**: admin endpoints require `ADMIN`; customer endpoints require
     `CUSTOMER` or `ADMIN`.
  2. **Ownership-based**: a `CUSTOMER` calling `GET /accounts/{id}` or
     `GET /payments/{id}` must own that account or be a party to that payment — checked
     in the `application` layer against the authenticated user's id, independent of role.
     A customer with a valid token but the wrong account id gets `403`, not `404` (so as
     not to leak existence, though this exact choice is left open for Phase 1 refinement).

## No custom cryptography

- Password hashing uses Spring Security's `PasswordEncoder` (BCrypt), never a hand-rolled
  hash or encryption routine.
- JWT signing uses a standard library (`spring-security-oauth2-jose` or `jjwt`), not a
  custom token format.
- This is a hard rule (AGENTS.md), not a preference — financial systems are exactly where
  home-grown crypto is most dangerous.

## Ownership checks in practice

- Every account/payment read or write endpoint resolves the authenticated user id from
  the security context and compares it against the resource's `owner_id` /
  `initiated_by_user_id` / being a party (`source_account_id` or
  `destination_account_id`'s owner) before returning data.
- Ownership checks are unit-testable in the `application` layer without spinning up the
  web layer, so they are covered by fast tests, not only integration tests.

## Other constraints carried from AGENTS.md

- No secrets committed to the repository; local secrets via `.env`/environment variables
  ignored by git, documented but not provided as real values.
- Rate limiting on authentication endpoints (login) is a candidate for Redis in Phase 3,
  explicitly a non-financial use of Redis (see `docs/diagrams/system-context.md`).
- All financial correctness must hold even if the security layer's optional/cache-backed
  pieces (rate limiting) are unavailable — auth itself must still work against
  PostgreSQL-backed user records.
