# OpenTelemetry (config-only, Phase 6)

Per `docs/architecture/technology-classification.md`, OpenTelemetry is **IMPLEMENT LATER
(Phase 6)**. This directory documents the concrete wiring so it can be dropped in without
further design work; it does not stand up an end-to-end trace pipeline as part of this
scaffolding pass.

## What this gives you

- The OpenTelemetry Java agent auto-instruments Spring MVC/WebFlux, JDBC, Kafka clients,
  and the Redis client with zero code changes, and exports traces via OTLP to Jaeger
  (already running as the `jaeger` service in the root `docker-compose.yml`).

## Steps to wire it up on the backend

1. Download the agent jar into the backend image (or bake it into `backend/Dockerfile`):

   ```dockerfile
   ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar /app/otel-javaagent.jar
   ```

2. Launch the backend with the agent attached, either by changing the Dockerfile
   `ENTRYPOINT` or via `JAVA_TOOL_OPTIONS`:

   ```dockerfile
   ENTRYPOINT ["java", "-javaagent:/app/otel-javaagent.jar", "-jar", "/app/app.jar"]
   ```

3. Configure the exporter via environment variables (add these to the `backend` service
   in `docker-compose.yml`):

   ```yaml
   environment:
     OTEL_SERVICE_NAME: paymentflow-backend
     OTEL_EXPORTER_OTLP_ENDPOINT: http://jaeger:4317
     OTEL_EXPORTER_OTLP_PROTOCOL: grpc
     OTEL_TRACES_EXPORTER: otlp
     OTEL_METRICS_EXPORTER: none   # metrics already handled by Micrometer/Prometheus
     OTEL_LOGS_EXPORTER: none      # logging handled by structured JSON logs, not OTLP
     OTEL_PROPAGATORS: tracecontext,baggage
   ```

   Jaeger's OTLP gRPC receiver listens on port `4317` (already exposed in
   `docker-compose.yml`); `4318` is the OTLP HTTP alternative if gRPC is unavailable.

4. View traces at the Jaeger UI: `http://localhost:16686`, service `paymentflow-backend`.

## Correlating traces with logs

Once enabled, the OTel Java agent injects `trace_id`/`span_id` into the MDC, which the
structured JSON log encoder (see `docs/system-design/observability-roadmap.md`) should
include as fields — giving "click from a slow trace straight to its log lines" without
extra plumbing.

## Why this is config/README only for now

- Distributed tracing is most useful once the outbox/Kafka async hop exists (Phase 4-5)
  to trace across; wiring it earlier would trace a system with no interesting async
  boundaries yet (ADR-012).
- No code in `backend/` currently depends on this; adding the javaagent line and env vars
  above is the entire integration once the backend exists.
