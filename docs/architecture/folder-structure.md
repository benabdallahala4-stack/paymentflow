# Folder Structure

Status: Phase 0 — matches the directories already scaffolded in the repo, plus the
proposed internal structure for `backend/` and `frontend/` (not yet created — Phase 1).

## Repo root (already scaffolded)

```
paymentflow/
├── AGENTS.md
├── README.md
├── backend/
├── frontend/
├── docs/
│   ├── architecture/
│   ├── adr/
│   ├── database/
│   ├── diagrams/
│   └── system-design/
├── infrastructure/
│   ├── terraform/
│   ├── kubernetes/
│   └── kustomize/
├── observability/
│   ├── prometheus/
│   ├── grafana/
│   ├── opentelemetry/
│   └── elastic/
├── load-tests/
└── .github/
    └── workflows/
```

## Proposed `backend/` internal structure (Phase 1, not yet created)

Mirrors the modular-monolith design in `docs/architecture/modular-monolith.md`: one
Maven/Gradle module tree, packages per bounded-context module, hexagonal layers within
each.

```
backend/
├── pom.xml                              (Phase 1 — not created in Phase 0)
└── src/
    ├── main/
    │   ├── java/com/paymentflow/
    │   │   ├── shared/
    │   │   │   ├── domain/              Money value object, ids, errors
    │   │   │   └── infrastructure/      clock, correlation-id filter, config
    │   │   ├── identity/
    │   │   │   ├── domain/
    │   │   │   ├── application/
    │   │   │   ├── infrastructure/
    │   │   │   └── api/
    │   │   ├── account/
    │   │   │   ├── domain/
    │   │   │   ├── application/
    │   │   │   ├── infrastructure/
    │   │   │   └── api/
    │   │   ├── payment/
    │   │   │   ├── domain/
    │   │   │   ├── application/
    │   │   │   ├── infrastructure/
    │   │   │   └── api/
    │   │   └── ledger/
    │   │       ├── domain/
    │   │       ├── application/
    │   │       ├── infrastructure/
    │   │       └── api/
    │   └── resources/
    │       └── db/migration/            Flyway migrations (V1__init.sql, ...)
    └── test/
        └── java/com/paymentflow/
            ├── identity/
            ├── account/
            ├── payment/
            ├── ledger/
            └── architecture/            ArchUnit rules for the dependency table above
```

## Proposed `frontend/` internal structure (Phase 1, not yet created)

```
frontend/
├── package.json                         (Phase 1 — not created in Phase 0)
└── src/
    ├── features/
    │   ├── auth/
    │   ├── accounts/
    │   └── payments/
    ├── api/                             typed HTTP client for the backend API
    └── shared/
```

No implementation files exist in `backend/` or `frontend/` yet — both directories are
empty placeholders at the end of Phase 0.
