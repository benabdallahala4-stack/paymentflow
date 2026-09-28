# Implementation Phases

Status: Phase 0 (this document itself). Restates the master spec's Phase 0-8 arc with
concrete PaymentFlow deliverables per phase.

## Phase 0 — Architecture (this phase)
- All documents under `docs/` (scope, domain model, schema, API design, modular monolith,
  folder structure, diagrams, concurrency strategy, ADRs 001-012, testing/security
  strategy, observability/infrastructure/scaling roadmaps, this document, git roadmap,
  technology classification), `AGENTS.md`, short root `README.md`.
- No code, no build files, no Dockerfiles.

## Phase 1 — Core backend: identity, accounts, payments, ledger (happy path)
- `backend/` scaffolded (Spring Boot, Maven), Flyway `V1__init.sql` implementing the
  schema in `docs/database/schema.md`.
- `identity`, `account`, `payment`, `ledger` modules with hexagonal layering; ArchUnit
  tests enforcing module boundaries.
- Register/login (JWT), open account, transfer money (happy path, no adversarial
  concurrency yet), view balance and transaction history (cursor-paginated).
- Idempotency-Key enforced on payment creation from day one (ADR-005), even before the
  concurrency lab, since it is independent of which locking strategy wins.
- `docker-compose.yml` with PostgreSQL for local dev.

## Phase 2 — Concurrency lab
- PaymentFlow-specific equivalents of `postgres-concurrency-control`'s test suite:
  lost-update proof, optimistic/pessimistic/SERIALIZABLE implementations and tests,
  write-skew scenario (if a relevant multi-row invariant is added), contention benchmark.
- ADR-004 revised from "Proposed/deferred" to a final decision with measured numbers.
- `accounts.version` (already in the Phase 1 schema) put to use; hot-path pessimistic
  locking added if evidenced.

## Phase 3 — Idempotency & resilience hardening
- Idempotency test suite matched to `IdempotencyTest` (duplicate/concurrent-duplicate/
  different-fingerprint cases).
- Redis introduced strictly for non-financial concerns (e.g. login rate limiting), per
  ADR and the invariant that no financial correctness depends on it.
- Retry/backoff policy for optimistic/SERIALIZABLE conflicts finalized in code.

## Phase 4 — Outbox & Kafka
- `outbox_events` table populated inside the payment transaction (ADR-006).
- Outbox poller/publisher process; Kafka added to Docker Compose (ADR-010).
- Simulated downstream consumer(s) (e.g. a notification-log consumer) proving
  at-least-once delivery and consumer-side idempotency.

## Phase 5 — API completeness & frontend
- Remaining API surface from `docs/architecture/api-design.md` (admin inspection
  endpoints, payment status polling) completed.
- Minimal frontend (ADR-009): login, accounts list, balance/history view, submit payment,
  payment status.

## Phase 6 — Observability
- Micrometer + Prometheus + Grafana dashboards; OpenTelemetry + Jaeger tracing;
  structured JSON logging pipeline. Concrete metrics per
  `docs/system-design/observability-roadmap.md` wired to real, running code.

## Phase 7 — Load testing & performance validation
- `load-tests/` scripts (e.g. k6/Gatling) exercising payment creation under load,
  validating (or revising) the latency/throughput goals stated in `docs/architecture/scope.md`.
- Query-plan-driven indexing pass (Stage 2 of `docs/system-design/scaling-evolution.md`)
  applied for real if load tests surface it.

## Phase 8 — Kubernetes/Terraform documentation & polish
- `infrastructure/kubernetes/` and `infrastructure/kustomize/` manifests modeling a
  cluster deployment (not applied to a live cluster except optionally locally via
  kind/minikube).
- `infrastructure/terraform/` describing the AWS target architecture
  (`docs/system-design/infrastructure-roadmap.md` Stage 3) — documented only, not applied.
- Portfolio-facing polish (full README, architecture diagrams finalized) — explicitly
  out of scope for Phase 0's short README (see repo root `README.md`).
