# ADR-007: Authentication and Authorization Approach

## Status
Proposed

## Context
PaymentFlow needs to distinguish CUSTOMER from ADMIN access, and must guarantee a
customer can never read or act on another customer's accounts/payments. It must not
implement custom cryptography.

## Decision
Use Spring Security with JWT bearer tokens for authentication (stateless, one token per
session, short-lived access token + refresh token) and role-based + ownership-based
authorization for access control. Password hashing uses Spring Security's provided
`PasswordEncoder` (BCrypt), never a custom hash. Ownership checks (does this JWT's user
own this account/payment) are enforced in the `application` layer of each module, not
only via role checks. Full detail in `docs/architecture/security-strategy.md`.

## Alternatives Considered
- **Session-cookie based auth**: rejected — a stateless API with a JS frontend and future
  service-to-service calls fits bearer tokens better, and JWT keeps the API stateless
  (no server-side session store needed).
- **Rolling a custom token/crypto scheme**: rejected outright — explicit hard rule
  (AGENTS.md: "no custom crypto"); Spring Security's battle-tested primitives are used
  throughout.
- **OAuth2/OIDC via an external identity provider (Keycloak, Auth0)**: appealing for a
  "real" system, but out of scope for Phase 0-8 — adds an external dependency to run
  locally with no corresponding FR that needs third-party login. Left as a possible
  future extension, not planned.

## Consequences
- `identity` module owns password hashing, token issuance, and token validation.
- Every other module's `api` layer relies on a shared authentication filter (in `shared`)
  to resolve the caller's identity and role before any controller method executes.
- Ownership checks are unit-testable independently of the web layer (they live in
  `application`, not in a controller `if` statement).
- Token secret/signing key management is a Phase 1 concern; no secrets are committed to
  the repo (AGENTS.md).
