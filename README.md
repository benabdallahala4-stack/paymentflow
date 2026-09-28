# PaymentFlow (LedgerFlow)

A simplified digital payment and double-entry ledger platform, built to demonstrate
correct handling of money under concurrency: preventing lost updates and overdraft on
concurrent transfers, keeping an immutable, auditable ledger, and making retries safe via
idempotency keys — backed by a modular-monolith Spring Boot backend over PostgreSQL.

**Status: Phase 0 — architecture only, no implementation yet.** `backend/` and
`frontend/` are empty scaffolding; everything currently in this repository is planning
and design documentation.

Start here:
- [`docs/architecture/scope.md`](docs/architecture/scope.md) — functional and
  non-functional requirements.
- [`docs/adr/`](docs/adr/) — architecture decision records (ADR-001 through ADR-012, all
  currently `Proposed`).
- [`AGENTS.md`](AGENTS.md) — constraints and conventions for anyone (human or AI)
  contributing to this repo.

## Repository layout

```
paymentflow/
├── AGENTS.md
├── README.md
├── backend/                  (empty — Phase 1)
├── frontend/                 (empty — Phase 5)
├── docs/
│   ├── architecture/         scope, domain model, API design, modular monolith,
│   │                         folder structure, concurrency strategy, testing/security
│   │                         strategy, implementation phases, git roadmap,
│   │                         technology classification
│   ├── adr/                  001-012, architecture decision records
│   ├── database/             schema.md — initial PostgreSQL schema
│   ├── diagrams/             system context + payment sequence (Mermaid)
│   └── system-design/        observability / infrastructure / scaling roadmaps
├── infrastructure/
│   ├── terraform/            (empty — documented only, Phase 8)
│   ├── kubernetes/           (empty — documented only, Phase 8)
│   └── kustomize/            (empty — documented only, Phase 8)
├── observability/
│   ├── prometheus/           (empty — Phase 6)
│   ├── grafana/              (empty — Phase 6)
│   ├── opentelemetry/        (empty — Phase 6)
│   └── elastic/              (empty — optional, Phase 6)
├── load-tests/                (empty — Phase 7)
└── .github/workflows/         (empty — CI added from Phase 1)
```

See [`docs/architecture/implementation-phases.md`](docs/architecture/implementation-phases.md)
for the full Phase 0-8 plan.
