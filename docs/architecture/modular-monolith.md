# Modular Monolith Architecture

Status: Phase 0 — architecture decision recorded here and in ADR-001.

## Why monolith-first

Per the project's master-spec rule 3 ("avoid microservices-for-CV-padding"), PaymentFlow
is built as a **single deployable Spring Boot application internally divided into
modules**, not as separate services. Reasons:

- A payment/ledger system has one dominant consistency requirement — a transfer and its
  ledger postings must commit atomically. Splitting `payment` and `ledger` into separate
  services would immediately require a distributed transaction or a saga, solving a
  problem the domain does not yet have, purely to look "microservice-shaped" on a resume.
- Module boundaries enforced by package structure + build-time checks (ArchUnit, Phase 1)
  give most of the benefit of service boundaries (clear ownership, dependency direction,
  replaceability) without network calls, partial failure, or distributed tracing being
  mandatory from day one.
- Splitting later is a refactor of already-well-isolated modules, not a rewrite — the
  hexagonal layering below is specifically chosen so a module's `infrastructure`/`api`
  layers could be replaced by HTTP/gRPC clients with the `domain`/`application` layers
  largely untouched, if a real reason to extract a service ever appears.

## Modules

| Module | Owns | Depends on |
|---|---|---|
| `shared` | Cross-cutting types (Money/minor-units value object, clock abstraction, error types, tracing/correlation-id plumbing). | nothing internal |
| `identity` | Users, authentication, roles. Foundational — every other module depends on it for "who is calling". | `shared` |
| `account` | Account aggregate, the balance **read model**, ownership checks. | `shared`, `identity` |
| `ledger` | Append-only `LedgerEntry` postings, balance reconstruction/audit queries. | `shared`, `account` (reads account existence, not balance mutation) |
| `payment` | Orchestrates a transfer: validates via `account`, posts via `ledger`, writes the outbox event, owns `Transaction`/idempotency. | `shared`, `identity`, `account`, `ledger` |

## Dependency rules

- `identity` depends on nothing but `shared` — it must remain the foundation; no module
  may be required for identity to function.
- `account` owns the balance **read model** (the `balance_minor_units` column) and its
  own consistency (the `CHECK (balance_minor_units >= 0)` constraint and, from ADR-004,
  the chosen concurrency control). It does not know about `payment` orchestration.
- `ledger` never depends on `payment`. It exposes "post a balanced set of entries for a
  transaction" and "reconstruct balance for an account" — it has no notion of what a
  payment *is*.
- `payment` is the only module allowed to depend on all of `identity`, `account`, and
  `ledger` simultaneously, because it is the orchestrator. No other module may reach into
  `payment`.
- No module may depend on `payment` (it sits at the top). This prevents circular
  dependencies and keeps the ledger/account modules independently testable.
- Cross-module calls happen through each module's `application` layer (use-case/service
  interfaces), never by reaching into another module's `infrastructure` or JPA entities
  directly.

## Hexagonal layering (per module)

Each module (`identity`, `account`, `payment`, `ledger`) is internally layered:

```
module/
  domain/          entities, value objects, domain invariants — no Spring, no JPA annotations on business rules
  application/      use cases / application services, ports (interfaces) the domain needs (repositories, clock, event publisher)
  infrastructure/   JPA repositories, adapters implementing the application-layer ports, Kafka/outbox plumbing
  api/              REST controllers, request/response DTOs, mapping to/from application layer
```

- `domain` has no outward dependency on `infrastructure` or `api` — only `application`
  defines the ports that `infrastructure` implements (dependency inversion).
- `api` depends only on `application`, never directly on `infrastructure` or another
  module's `domain`.
- This layering is what ArchUnit tests (Phase 1, see `docs/architecture/testing-strategy.md`)
  will enforce mechanically, so the rule above is not just documentation.

## Future extraction path (not planned, just kept open)

If `payment` volume or team size ever justified extracting a service, the natural seam is
`payment` and `ledger` together (they change together) versus `identity`/`account`
(more stable, more reusable). This is explicitly **not** a Phase 0-8 goal — see
`docs/architecture/implementation-phases.md`.
