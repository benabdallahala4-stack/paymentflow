# ADR-012: Observability Stack

## Status
Proposed

## Context
A payment system needs to be debuggable in terms of throughput, failure rates, latency,
outbox backlog, and Kafka consumer lag — none of which are implemented or measured yet
(Phase 0/1). The master spec calls for a full observability stack
(Micrometer/Prometheus/Grafana/OpenTelemetry/Jaeger, structured JSON logs) to be
demonstrated, but not until the core payment/ledger/outbox/Kafka machinery exists to
observe.

## Decision
Adopt Micrometer (metrics facade) + Prometheus (scrape/storage) + Grafana (dashboards) +
OpenTelemetry (traces) + Jaeger (trace storage/viewer) + structured JSON logs, all
introduced in **Phase 6**, after Phases 1-5 (core payments, concurrency lab, idempotency,
outbox, Kafka) exist to generate meaningful signals. See
`docs/system-design/observability-roadmap.md` for the concrete metric list and phasing.

## Alternatives Considered
- **Build observability in from Phase 1**: rejected for sequencing reasons, not
  technical ones — instrumenting a system that doesn't yet have the behaviors worth
  observing (contention, outbox backlog, consumer lag) produces dashboards with nothing
  interesting to show. Correctness (Phases 1-3) comes before visibility into it.
- **A hosted SaaS observability platform (Datadog, New Relic)**: rejected — requires an
  external account/cost and contradicts "runs fully locally without AWS/cloud
  dependencies"; the OSS stack (Prometheus/Grafana/Jaeger) runs entirely in Docker
  Compose.
- **Logs only, no metrics/traces**: rejected — insufficient to answer "why is this
  payment slow" across module/DB/Kafka boundaries; distributed tracing is specifically
  useful once the outbox/Kafka async hop exists.

## Consequences
- No dashboards, alerts, or trace data exist before Phase 6; nothing in Phases 1-5 should
  be blocked on observability tooling being present.
- Every phase 1-5 component should still emit sensible structured log lines from the
  start (cheap, no new infrastructure) even though metrics/traces wiring waits for
  Phase 6 — documented in `docs/system-design/observability-roadmap.md`.
- Concrete metrics to track (payment throughput, failure rate, latency percentiles,
  outbox backlog size, Kafka consumer lag, DB connection pool utilization) are listed in
  the observability roadmap, not implemented here.
