# Infrastructure Roadmap

Status: Phase 0 — plan only. **Nothing is deployed anywhere.** The project must run fully
locally without any AWS account or cloud dependency at every phase; cloud deployment is
documented/simulated only, never a prerequisite for running or testing the system.

## Stage 1 (Phase 1-5): Docker Compose, local only

- A single `docker-compose.yml` at the repo root (not yet created) running: PostgreSQL,
  and, from the phase each is introduced, Kafka (+ Zookeeper or KRaft), Redis.
- The Spring Boot backend runs either via `mvn spring-boot:run` against Compose-provided
  dependencies, or itself containerized later in this stage.
- This is the **only** infrastructure required to develop, test, and demo the entire
  project end to end.

## Stage 2 (documented in `infrastructure/kubernetes/`, `infrastructure/kustomize/`):
Kubernetes manifests, later, not deployed

- Kustomize-overlaid Kubernetes manifests (Deployment, Service, ConfigMap, Secret
  references, HPA) for the backend, modeling how the app *would* run in a cluster.
- Purpose is to demonstrate Kubernetes literacy and give the manifests a real target to
  validate against a local cluster (kind/minikube) if desired — still local, still no
  cloud account required.
- No production Kubernetes cluster is provisioned as part of this project.

## Stage 3 (documented in `infrastructure/terraform/`): AWS, documented/simulated only

Terraform is written to describe what a production AWS deployment would look like, but is
explicitly **not applied** against a real AWS account as part of this project. Components
documented:

- **RDS (PostgreSQL)** — managed system of record, replacing the Compose PostgreSQL.
- **MSK (Managed Streaming for Kafka)** — replacing the Compose Kafka.
- **ElastiCache (Redis)** — replacing the Compose Redis, for the same non-financial uses.
- **ECS/EKS** — container orchestration target for the backend (choice left open,
  documented as a trade-off, not decided).
- **ALB** — load balancing / TLS termination in front of the backend.
- **S3** — for any static frontend hosting or export/report artifacts, if pursued.
- **CloudWatch** — logs/metrics sink, parallel to (not replacing) the
  Prometheus/Grafana/OpenTelemetry stack documented in the observability roadmap.
- **IAM** — least-privilege roles per component, documented as Terraform modules/policies.
- **Secrets Manager / Parameter Store** — for the secrets AGENTS.md forbids committing to
  the repo.

## Explicit statement

At the end of Phase 0 (and at the end of every phase through Phase 8 unless the user
decides otherwise), the entire system is runnable with `docker compose up` and requires
no AWS credentials, no cloud account, and no network access beyond pulling public Docker
images. The Terraform/Kubernetes artifacts are architecture documentation and CV-relevant
demonstration pieces, not a live deployment.
