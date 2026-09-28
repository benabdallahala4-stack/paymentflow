# Terraform (DOCUMENT/SIMULATE ONLY — NOT APPLIED)

Per `docs/architecture/technology-classification.md` and
`docs/system-design/infrastructure-roadmap.md` (Stage 3), this Terraform describes what a
production AWS deployment of PaymentFlow *would* look like. It is **never applied** as
part of this project:

- No `terraform plan` or `terraform apply` has been run, or should be run, against a real
  AWS account.
- No AWS credentials are assumed, configured, or required to read/understand this code.
- Costs implied by the instance types/services below are illustrative only, not a real
  budget.
- The project's actual runtime is `docker compose up` at the repo root — this directory
  is architecture documentation and a CV-relevant demonstration piece, not live
  infrastructure.

## Expected local check

The only command that should be run locally against this module is:

```bash
cd infrastructure/terraform
terraform init -backend=false
terraform validate
```

This checks the HCL is syntactically valid and internally consistent. Do **not** run
`terraform plan` or `terraform apply` — both would attempt to talk to the AWS provider
API and are out of scope for this project.

## Components modeled

Mirrors the component list in the infrastructure roadmap:

- VPC + public/private subnets
- Security groups (ALB -> ECS, ECS -> RDS/ElastiCache)
- Application Load Balancer (ALB)
- ECS Fargate service (compute target; EKS documented as an alternative in the roadmap,
  not modeled here to keep this skeleton small)
- RDS PostgreSQL (replaces Compose Postgres)
- ElastiCache Redis (replaces Compose Redis)

Not modeled in this basic skeleton (documented only in the roadmap): MSK, CloudWatch
dashboards/alarms, IAM policy documents, Secrets Manager entries, S3. These would be
natural follow-ups if this module were ever expanded, but are out of scope for "basic
infrastructure scaffolding."

## Files

- `variables.tf` — all inputs, with sane defaults, no secrets.
- `main.tf` — resource definitions.
- `outputs.tf` — illustrative outputs (ALB DNS name, RDS endpoint, etc.).
