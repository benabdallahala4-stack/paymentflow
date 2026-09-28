# Git Commit Roadmap (Proposed)

Status: Phase 0 — **plan only, no commits have been made from this list.** Commit format
is `[LF-XXX] description` per AGENTS.md, using the LF- prefix (LedgerFlow) as the
established convention even though the repository folder is named `paymentflow`.

## Phase 0
- `[LF-001] add architecture and ADR documentation (Phase 0)`

## Phase 1 — core backend
- `[LF-002] scaffold backend Maven project and module skeleton`
- `[LF-003] add Flyway V1 migration: users, accounts, transactions, ledger_entries, idempotency_keys, outbox_events`
- `[LF-004] implement identity module: registration, login, JWT issuance`
- `[LF-005] implement Spring Security JWT authentication filter`
- `[LF-006] implement account module: open account, get account, ownership checks`
- `[LF-007] implement ledger module: post balanced entries, reconstruct balance`
- `[LF-008] implement payment module happy path: create transfer, orchestrate account + ledger`
- `[LF-009] add Idempotency-Key handling to payment creation`
- `[LF-010] implement transaction history endpoint with cursor pagination`
- `[LF-011] add ArchUnit tests enforcing module dependency rules`
- `[LF-012] add docker-compose.yml for local PostgreSQL`

## Phase 2 — concurrency lab
- `[LF-013] add LostUpdateTest reproducing lost update on account balance`
- `[LF-014] implement optimistic locking (@Version) on accounts + retry loop`
- `[LF-015] implement pessimistic locking (SELECT ... FOR UPDATE, NOWAIT) for hot debit path`
- `[LF-016] implement SERIALIZABLE isolation variant for comparison`
- `[LF-017] add ContentionBenchmarkTest and record results in ADR-004`
- `[LF-018] finalize ADR-004 with measured decision`

## Phase 3 — idempotency & resilience
- `[LF-019] add IdempotencyTest suite: duplicate, concurrent duplicate, fingerprint mismatch`
- `[LF-020] add Redis for login rate limiting (non-financial only)`
- `[LF-021] add retry/backoff policy for optimistic and SERIALIZABLE conflicts`

## Phase 4 — outbox & Kafka
- `[LF-022] add outbox_events table population inside payment transaction`
- `[LF-023] implement outbox poller/publisher`
- `[LF-024] add Kafka to docker-compose and wire publisher`
- `[LF-025] add simulated notification consumer with idempotent processing`

## Phase 5 — API completeness & frontend
- `[LF-026] implement admin inspection endpoints`
- `[LF-027] scaffold minimal frontend: login, accounts, payment form`
- `[LF-028] wire frontend to transaction history and payment status`

## Phase 6+ (beyond this roadmap's detail)
- `[LF-029] wire Micrometer/Prometheus/Grafana metrics`
- `[LF-030] add OpenTelemetry tracing and Jaeger export`

Further commits (load testing, Kubernetes/Terraform documentation, polish) are not
enumerated here — this roadmap covers Phase 1-3 in full detail and Phase 4-6 at a
milestone level, per the scope of this Phase 0 planning pass.
