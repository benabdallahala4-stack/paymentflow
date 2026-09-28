# PaymentFlow (LedgerFlow)

A production-style digital payment and double-entry ledger platform, built to demonstrate
correct handling of money under concurrency: preventing lost updates and overdraft on
concurrent transfers, keeping an immutable, auditable ledger, and making retries safe via
idempotency keys — backed by a modular-monolith Spring Boot backend over PostgreSQL, with
an Angular frontend and a documented (not deployed) cloud-native operations story.

**Status: basic implementation of every phase is in place and runnable locally.** This is
a portfolio project, not a production deployment — see [Scope and honesty notes](#scope-and-honesty-notes)
below for exactly what that means.

## Why this project exists

Most CRUD portfolio projects don't touch the two things that actually separate a junior
from a senior backend engineer: what happens when two requests race for the same money,
and what happens when a network call fails halfway through a multi-step write. PaymentFlow
is built around those two questions, proven with real concurrent-thread tests against a
real PostgreSQL (via Testcontainers), not asserted in prose.

## What's actually implemented

- **Financial core** ([`backend/`](backend)) — accounts, payments, and a double-entry
  ledger (every transfer writes a balanced DEBIT/CREDIT pair; ledger entries are
  immutable) on Flyway-migrated PostgreSQL. Money is `BIGINT` minor units, never a float.
- **Concurrency** — optimistic locking (`@Version`) is the production strategy, with
  retry-with-backoff on conflict; a `ConcurrentTransferOverdraftTest` fires two real
  concurrent transfers against one account and proves the balance never goes negative.
  Pessimistic locking (`SELECT ... FOR UPDATE`) exists as a clearly separate,
  documented experiment, not the default path — see
  [ADR-004](docs/adr/004-concurrency-control-strategy.md).
- **Idempotency** — `POST /api/v1/payments` requires an `Idempotency-Key` header; a
  dedicated concurrency test fires the same key from two threads and proves exactly one
  payment is ever created.
- **Transactional outbox** — the payment transaction, ledger entries, and an outbox row
  commit in one PostgreSQL transaction; a separate scheduled poller publishes to Kafka,
  so a Kafka outage never blocks or corrupts a payment.
- **Redis** — cache-aside for balance *reads* only; the write path never touches it, so
  financial correctness never depends on cache state.
- **Cursor pagination** — transaction history uses `(created_at, id)` keyset pagination,
  never `OFFSET`.
- **Frontend** ([`frontend/`](frontend)) — Angular (standalone components, signals):
  login/register, dashboard, accounts, transfer (client-generated, correctly-rotated
  idempotency key), payments + history (cursor pagination), admin inspection.
- **Local infra** — `docker compose up` brings up Postgres, Redis, Kafka, the backend,
  the frontend, and a Prometheus/Grafana/Jaeger observability stack.
- **CI** ([`.github/workflows/ci.yml`](.github/workflows/ci.yml)) — backend tests
  (Testcontainers) → frontend build/tests → Docker image build, gated, no image push.

## Documented but not deployed

Kubernetes/Kustomize manifests, a Terraform AWS module (VPC/ALB/ECS/RDS/ElastiCache), and
the read-replica/partitioning/sharding scaling story exist as real, validated-but-never-applied
configuration — see [`infrastructure/`](infrastructure) and
[`docs/system-design/`](docs/system-design). Nothing here has ever touched a real cloud
account; that distinction is intentional and documented in
[`docs/architecture/technology-classification.md`](docs/architecture/technology-classification.md).

## Start here

- [`docs/architecture/scope.md`](docs/architecture/scope.md) — functional and
  non-functional requirements.
- [`docs/adr/`](docs/adr/) — architecture decision records (ADR-001 through ADR-012).
- [`AGENTS.md`](AGENTS.md) — constraints and conventions for anyone (human or AI)
  contributing to this repo.
- [`docs/diagrams/payment-sequence.md`](docs/diagrams/payment-sequence.md) — the core
  transfer flow, Client → API → Ledger → Outbox → Kafka.

## Running locally

```bash
docker compose up -d postgres redis kafka   # infra only
cd backend && mvn spring-boot:run           # fast edit-compile-run loop
# in another shell:
cd frontend && npm install && npm start
```

Or run the whole stack containerized: `docker compose up -d`.

## Scope and honesty notes

This repository intentionally does **not**:
- deploy to AWS, run `terraform apply`, or provision any real cloud resource,
- run against a real Kubernetes cluster,
- claim to have been load-tested at production scale.

Every claim about what's *implemented* above is backed by a test or a file that actually
exists in this tree. Everything under "documented but not deployed" is exactly that —
real configuration a reader can inspect, explicitly not exercised end-to-end. See
[`docs/architecture/technology-classification.md`](docs/architecture/technology-classification.md)
for the full IMPLEMENTED / IMPLEMENT LATER / DOCUMENT-ONLY breakdown, and
[`docs/architecture/implementation-phases.md`](docs/architecture/implementation-phases.md)
for the phase plan this was built against.

## Repository layout

```
paymentflow/
├── AGENTS.md
├── README.md
├── docker-compose.yml
├── backend/                   Spring Boot modular monolith (identity, account, payment, ledger)
├── frontend/                  Angular app
├── docs/
│   ├── architecture/          scope, domain model, API design, modular monolith,
│   │                          folder structure, concurrency strategy, testing/security
│   │                          strategy, implementation phases, git roadmap,
│   │                          technology classification
│   ├── adr/                   001-012, architecture decision records
│   ├── database/              schema.md — PostgreSQL schema
│   ├── diagrams/              system context + payment sequence (Mermaid)
│   └── system-design/         observability / infrastructure / scaling roadmaps
├── infrastructure/
│   ├── terraform/             AWS module skeleton — validated, never applied
│   └── kubernetes/            Kustomize base + overlays — never deployed
├── observability/
│   ├── prometheus/            scrape config for backend Actuator metrics
│   ├── grafana/                datasource + dashboard provisioning
│   └── opentelemetry/         tracing wiring instructions (config-only)
└── .github/workflows/         CI: backend tests, frontend build/tests, Docker build
```
