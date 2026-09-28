# ADR-003: Money Represented as Minor-Units BIGINT

## Status
Proposed

## Context
Money must never be represented as floating point (a hard invariant, see AGENTS.md).
The two safe alternatives are a fixed-point decimal column (`NUMERIC(19,4)`, paired with
`BigDecimal` in Java) or an integer count of minor units (`BIGINT` cents, paired with
`long`). `postgres-concurrency-control` uses the `BigDecimal`/`NUMERIC` approach
successfully and correctly.

## Decision
PaymentFlow uses **`BIGINT` storing minor units** (e.g. cents) for every money column,
mapped to Java `long` in the domain layer. Currency's minor-unit exponent is applied only
at API presentation boundaries. See `docs/database/schema.md` §"Money representation" for
full justification and the `_minor_units` naming convention.

## Alternatives Considered
- **`NUMERIC(19,4)` + `BigDecimal`** (the reference project's approach): valid and
  proven, but requires a `RoundingMode`/scale policy wherever arithmetic happens and a
  disciplined `BigDecimal` usage convention across the codebase (never `.doubleValue()`).
  We diverge here because PaymentFlow's ledger reconstructs balances via `SUM()` over
  many rows, and integer aggregation removes any scale/rounding policy from that hot path
  entirely.
- **`double`/`float`**: rejected outright — not exact, explicitly forbidden by AGENTS.md.

## Consequences
- Every money-handling type in the domain layer is a `Money` value object wrapping a
  `long` minor-units amount plus a currency code; no raw `long`/`BigDecimal` crosses
  module boundaries.
- Presentation (API JSON, UI) must consistently divide/format by the currency's minor-unit
  exponent (2 for EUR/USD) — a single shared utility in the `shared` module owns this.
- Multi-currency arithmetic (adding two different currencies) is a domain error, checked
  explicitly since the type system alone won't prevent it with a plain `long`.
- This is a Phase 0 decision not yet stress-tested against real code; flagged as an open
  question worth a second look after Phase 1 implementation experience.
