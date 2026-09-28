# Technology Classification

Status: Phase 0. Classifies every technology referenced in this planning pass into
**IMPLEMENT NOW** (Phase 1), **IMPLEMENT LATER** (named future phase), or
**DOCUMENT/SIMULATE ONLY** (never fully implemented in this project).

| Technology | Classification | Notes |
|---|---|---|
| Spring Boot | IMPLEMENT NOW (Phase 1) | Core application framework. |
| Spring Security | IMPLEMENT NOW (Phase 1) | AuthN/authZ, JWT, BCrypt. |
| Spring Data JPA / Hibernate | IMPLEMENT NOW (Phase 1) | Persistence layer. |
| PostgreSQL | IMPLEMENT NOW (Phase 1) | System of record (ADR-002). |
| Flyway | IMPLEMENT NOW (Phase 1) | Schema migrations (ADR-011). |
| JWT (jjwt / spring-security-oauth2-jose) | IMPLEMENT NOW (Phase 1) | Token auth (ADR-007). |
| Testcontainers | IMPLEMENT NOW (Phase 1) | Integration/concurrency test infra. |
| JUnit 5 / AssertJ | IMPLEMENT NOW (Phase 1) | Test framework. |
| ArchUnit | IMPLEMENT NOW (Phase 1) | Module boundary enforcement. |
| Docker / Docker Compose | IMPLEMENT NOW (Phase 1) | Local infra runtime. |
| `@Version` optimistic locking | IMPLEMENT NOW (Phase 1 schema column) / validated Phase 2 | Column added Phase 1, strategy validated Phase 2 (ADR-004). |
| `SELECT ... FOR UPDATE` pessimistic locking | IMPLEMENT LATER (Phase 2) | Concurrency lab. |
| `SERIALIZABLE` isolation | IMPLEMENT LATER (Phase 2) | Concurrency lab, comparison only unless ADR-004 selects it for a specific path. |
| Idempotency-Key pattern | IMPLEMENT NOW (Phase 1) | Required header from first payment endpoint (ADR-005). |
| Transactional outbox | IMPLEMENT LATER (Phase 4) | Table exists Phase 1 schema; publisher built Phase 4 (ADR-006). |
| Apache Kafka | IMPLEMENT LATER (Phase 4) | Event bus (ADR-010), local via Docker Compose only. |
| Redis | IMPLEMENT LATER (Phase 3) | Non-financial only (rate limiting). |
| React/Vue/Svelte (frontend framework, TBD) | IMPLEMENT LATER (Phase 5) | Minimal scope only (ADR-009). |
| Micrometer | IMPLEMENT LATER (Phase 6) | Metrics facade (ADR-012). |
| Prometheus | IMPLEMENT LATER (Phase 6) | Metrics storage/scrape. |
| Grafana | IMPLEMENT LATER (Phase 6) | Dashboards. |
| OpenTelemetry | IMPLEMENT LATER (Phase 6) | Distributed tracing SDK. |
| Jaeger | IMPLEMENT LATER (Phase 6) | Trace storage/viewer. |
| Structured JSON logging | IMPLEMENT LATER (baseline from Phase 1, full pipeline Phase 6) | Log lines from Phase 1; shipping/aggregation pipeline Phase 6. |
| Elasticsearch / Kibana (`observability/elastic/`) | DOCUMENT/SIMULATE ONLY | Optional log aggregation target; not required for the project's core demonstration. |
| k6 / Gatling (`load-tests/`) | IMPLEMENT LATER (Phase 7) | Load testing scripts. |
| Kubernetes manifests (`infrastructure/kubernetes/`) | DOCUMENT/SIMULATE ONLY | Modeled, optionally run against a local kind/minikube cluster; no managed cluster. |
| Kustomize (`infrastructure/kustomize/`) | DOCUMENT/SIMULATE ONLY | Overlay structure documented alongside the Kubernetes manifests. |
| Terraform (`infrastructure/terraform/`) | DOCUMENT/SIMULATE ONLY | AWS target architecture described, never applied. |
| AWS RDS | DOCUMENT/SIMULATE ONLY | Terraform-documented replacement for Compose PostgreSQL. |
| AWS MSK | DOCUMENT/SIMULATE ONLY | Terraform-documented replacement for Compose Kafka. |
| AWS ElastiCache | DOCUMENT/SIMULATE ONLY | Terraform-documented replacement for Compose Redis. |
| AWS ECS/EKS | DOCUMENT/SIMULATE ONLY | Container orchestration target, undecided, documented only. |
| AWS ALB | DOCUMENT/SIMULATE ONLY | Load balancing/TLS termination, documented only. |
| AWS S3 | DOCUMENT/SIMULATE ONLY | Static hosting/artifact storage, documented only. |
| AWS CloudWatch | DOCUMENT/SIMULATE ONLY | Parallel cloud logging/metrics sink, documented only. |
| AWS IAM | DOCUMENT/SIMULATE ONLY | Least-privilege roles, documented only. |
| AWS Secrets Manager / Parameter Store | DOCUMENT/SIMULATE ONLY | Secrets handling target, documented only. |
| Database sharding | DOCUMENT/SIMULATE ONLY | Stage 5 of `scaling-evolution.md`, explicitly "if justified", not built. |
| Read replicas | DOCUMENT/SIMULATE ONLY | Stage 3 of `scaling-evolution.md`, not built. |
| Table partitioning (ledger_entries by created_at) | DOCUMENT/SIMULATE ONLY (candidate for IMPLEMENT LATER if evidenced) | Stage 4 of `scaling-evolution.md`; only implemented if Phase 7 load testing shows a real need. |
| Change Data Capture / Debezium | DOCUMENT/SIMULATE ONLY | Mentioned as a possible outbox upgrade in ADR-006, not planned. |
| GitHub Actions (`.github/workflows/`) | IMPLEMENT LATER (Phase 1 onward, incrementally) | CI pipeline: build/test from Phase 1, expanded per phase (e.g. load tests in Phase 7). |
