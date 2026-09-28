# Observability Roadmap

Status: Phase 0 — plan only. Nothing in this document is wired up; see ADR-012 for the
reasoning behind deferring this to **Phase 6**.

## Phasing

Observability tooling is deliberately introduced *after* the behaviors worth observing
exist (core payments Phase 1, concurrency lab Phase 2, idempotency Phase 3, outbox/Kafka
Phase 4-5). Phase 6 wires up:

- **Metrics**: Micrometer as the facade inside Spring Boot, scraped by Prometheus.
- **Tracing**: OpenTelemetry SDK/agent, exported to Jaeger for trace visualization.
- **Logging**: structured JSON logs (one line per event, correlation/trace id included),
  shippable to the `observability/elastic/` stack (Elasticsearch + Kibana) if pursued.
- **Dashboards**: Grafana, backed by Prometheus for metrics and (optionally) Loki/Elastic
  for logs.

## Concrete metrics to track (Phase 6, not measured yet)

- **Payment throughput** — payments completed per second/minute.
- **Payment failure rate** — split by reason: `REJECTED` (business rule, e.g.
  insufficient funds) vs. `FAILED` (system error) vs. concurrency-conflict retries
  exhausted.
- **Payment latency** — p50/p95/p99 for `POST /payments`, end-to-end and broken down by
  the DB-transaction portion specifically (to separate "slow because of lock contention"
  from "slow because of network/serialization").
- **Concurrency conflict rate** — retries as a fraction of attempts, per the reference
  project's stated practical rule ("measure your conflict rate... if it climbs past a few
  percent, the row is too hot for optimistic control") — this metric directly feeds a
  decision to move a specific account/row to pessimistic locking.
- **Outbox backlog** — count and age of `outbox_events` rows with `published_at IS NULL`;
  alerts if backlog age exceeds a threshold (publisher stalled).
- **Kafka consumer lag** — per consumer group, standard Kafka metric exported via the
  consumer's own Micrometer binder.
- **DB connection pool utilization** — active/idle/pending connections (HikariCP metrics),
  since pessimistic-locking contention manifests first as pool exhaustion (per the
  reference project's cost analysis of pessimistic locking).
- **JVM/HTTP baseline metrics** — request rate/latency/error rate per endpoint, GC pauses,
  heap usage — standard Spring Boot Actuator/Micrometer defaults.

## What exists before Phase 6

Structured JSON log lines (no metrics/traces backend) are emitted from Phase 1 onward at
key points (payment created, payment completed/rejected, concurrency conflict/retry,
outbox event written/published) — cheap, no new infrastructure, and gives Phase 6 a
running start once the metrics/tracing stack is wired in.
