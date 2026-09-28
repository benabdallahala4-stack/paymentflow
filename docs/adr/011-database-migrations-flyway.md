# ADR-011: Database Migrations via Flyway

## Status
Proposed

## Context
The schema (`docs/database/schema.md`) will evolve across phases (initial tables, later
indexes, possible partitioning). Schema changes for a financial system must be
version-controlled, repeatable, and applied the same way in every environment (local,
CI, eventually a deployed environment) — hand-run DDL or JPA `ddl-auto: update` are not
acceptable for a system whose correctness depends on exact constraints (`CHECK`, unique
indexes) being present.

## Decision
All schema changes go through Flyway migrations (`backend/src/main/resources/db/migration/`,
`V1__init.sql`, `V2__...sql`, ...), applied automatically on application startup in local
and CI environments. `spring.jpa.hibernate.ddl-auto` is `validate` at most, never
`update`/`create`. This mirrors the reference project's own approach
(`V1__init.sql`, `V2__payroll_idempotency_and_indexing.sql`).

## Alternatives Considered
- **Hibernate `ddl-auto: update`**: rejected — unpredictable generated DDL, no migration
  history, cannot express `CHECK` constraints or partial indexes reliably, and directly
  violates AGENTS.md's "migrations use Flyway" rule.
- **Liquibase**: a valid alternative migration tool; Flyway is chosen for its plain-SQL
  migration files, which keep the schema history readable as ordinary SQL rather than
  XML/YAML changesets, and for consistency with the reference project's tooling.
- **Manual DDL scripts run by an operator**: rejected — not repeatable, not tracked, not
  safe for CI or any future deployment.

## Consequences
- Every schema change (including deferred indexes added later per evidence, see
  `docs/database/schema.md` §"Indexes we will add later") ships as a new numbered
  migration file, never edits to an already-applied one.
- CI (Phase 1 onward) runs migrations against a Testcontainers PostgreSQL as part of the
  test suite, catching migration errors before merge.
- Migration files become part of the audit trail for the schema itself, mirroring the
  immutability principle applied to `ledger_entries`.
