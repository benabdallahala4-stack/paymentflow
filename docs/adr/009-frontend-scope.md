# ADR-009: Frontend Scope

## Status
Proposed

## Context
PaymentFlow's primary demonstration value is backend correctness (concurrency, ledger
integrity, idempotency). A frontend is still useful to show the API is real and usable
end-to-end, but building a feature-rich SPA is not the point of this portfolio project
and would dilute effort away from the backend work Phase 1-5 focus on.

## Decision
Build a minimal frontend (framework choice deferred to Phase 1 implementation planning,
not Phase 0) covering exactly the customer-facing FRs: login/register, list accounts,
view balance/history (paginated), submit a payment, view payment status. No admin UI
beyond what's needed to demonstrate the admin inspection endpoints exist (a simple
read-only view is enough). No design system, no theming, no offline support.

## Alternatives Considered
- **No frontend at all, API + curl/Postman collection only**: rejected — a thin UI
  meaningfully improves the project's legibility to a reviewer without much added cost.
- **Full-featured SPA with polish (animations, i18n, responsive design system)**:
  rejected for this project's scope — time is better spent on the concurrency lab,
  outbox/Kafka wiring, and observability, which are the actual differentiators.

## Consequences
- Frontend work is explicitly scoped down in `docs/architecture/implementation-phases.md`
  and should not expand without revisiting this ADR.
- The API is designed (per `docs/architecture/api-design.md`) to be usable directly via
  HTTP tools, so the frontend is never the only way to exercise the system.
- Frontend framework, state management, and component structure are Phase 1
  implementation details, not decided in Phase 0.
