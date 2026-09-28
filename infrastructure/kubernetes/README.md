# Kubernetes manifests (DOCUMENT/SIMULATE ONLY)

Per `docs/architecture/technology-classification.md`, Kubernetes manifests and Kustomize
are **DOCUMENT/SIMULATE ONLY**: modeled here to demonstrate Kubernetes literacy and to
give the manifests a real target to validate against, optionally, a local `kind`/
`minikube` cluster if someone wants to try it. **Nothing here is deployed as part of this
project**, and no managed/cloud cluster is targeted or assumed
(`docs/system-design/infrastructure-roadmap.md`, Stage 2).

The project's actual runtime is `docker compose up` at the repo root — that is the only
infrastructure required to build, run, and demo PaymentFlow.

## Layout

```
infrastructure/kubernetes/
  base/                   # Deployment + Service + ConfigMap for backend & frontend
  overlays/
    local/                # For an optional local kind/minikube try-out
    staging/              # Aspirational, no real target
    production/           # Aspirational, no real target
```

## Validating (optional, local only)

```bash
kubectl kustomize infrastructure/kubernetes/overlays/local
# or, against a real local cluster (kind/minikube), if you want to try it:
kubectl apply -k infrastructure/kubernetes/overlays/local
```

`staging` and `production` overlays exist to show how replica counts/resources would be
patched per environment; they are not wired to any real staging/production cluster and
should not be applied anywhere.
