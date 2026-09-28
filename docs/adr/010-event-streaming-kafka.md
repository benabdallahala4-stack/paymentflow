# ADR-010: Kafka for Event Streaming

## Status
Proposed

## Context
Once the transactional outbox (ADR-006) needs a real transport to deliver events to
downstream consumers, a message broker is required. The master spec calls for
demonstrating event-driven patterns (outbox → broker → consumers) as part of the
project's later phases.

## Decision
Use Kafka (run locally via Docker Compose in Phase 4; a managed offering like AWS MSK is
documented only, never actually deployed — see
`docs/system-design/infrastructure-roadmap.md`) as the event bus for outbox events
published by the `payment` module and consumed by simulated downstream
consumers (notification/analytics stand-ins).

## Alternatives Considered
- **RabbitMQ**: a reasonable alternative for at-least-once delivery; Kafka is chosen
  instead because the master spec's technology list and later observability work
  (consumer lag as a tracked metric) are Kafka-specific and Kafka's log-based retention
  fits an audit-adjacent event stream (PaymentCompleted history) more naturally.
- **No broker — consumers poll the outbox table directly**: rejected as the permanent
  design (fine as an interim Phase 1-3 stand-in, which is exactly what the outbox +
  future-poller diagram in `docs/diagrams/system-context.md` marks it as) — doesn't
  demonstrate the event-streaming skills the project intends to show, and doesn't scale
  to multiple independent consumer groups.
- **AWS SNS/SQS**: rejected for local-first development — would require AWS to run the
  project at all, violating the "must run fully locally without AWS" requirement.

## Consequences
- Kafka is introduced only in Phase 4, not before — earlier phases run without it, and
  the outbox table alone is sufficient proof of the transactional-write half of the
  pattern.
- Consumer lag, publish latency, and broker health become tracked metrics only once
  Phase 6 observability work lands (`docs/system-design/observability-roadmap.md`).
- Local development requires Kafka in Docker Compose from Phase 4 onward; nothing before
  that phase needs it running.
