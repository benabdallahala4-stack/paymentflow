# Frontend

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 22.2.0.

PaymentFlow (LedgerFlow) portfolio project — Phase 5 frontend, built against the REST
contract documented in `docs/architecture/api-design.md`.

## Backend URL configuration

The API base URL is set in `src/environments/environment.ts` (`apiBaseUrl`, used for
`ng serve`) and `src/environments/environment.prod.ts` (used for `ng build`). Both default
to `http://localhost:8080/api/v1`. Change these two files if the backend runs on a
different host/port (e.g. via Docker Compose).

## Scope notes / assumptions

- `api-design.md` specifies `Authorization: Bearer <JWT>` but does not finalize the
  `/auth/login` response body; this frontend assumes
  `{ accessToken, refreshToken?, userId, email, role }` and stores it in `localStorage`
  (see `core/services/auth.service.ts`). Adjust `AuthResponse` in
  `core/models/auth.model.ts` once the backend's real response shape is confirmed.
- List/cursor-paginated responses are assumed to be shaped as
  `{ items: T[], nextCursor: string | null }` (`core/models/transaction.model.ts`); the
  cursor itself is treated as an opaque string per the documented `(created_at, id)`
  encoding and never parsed client-side.
- The Admin monitoring page reuses `GET /payments` for its "recent payments" list, since
  the doc does not define a separate admin-wide payments list endpoint; per-transaction
  outbox inspection uses the documented `GET /admin/transactions/{id}`.
- **E2E tests are deliberately out of scope for this phase** (per the task brief) — only
  unit/component tests are included (money pipe, idempotency key generation, transfer
  form validation and idempotency-key retry semantics). Add Playwright/Cypress in a later
  phase once the real backend is available to test against.

## Development server

To start a local development server, run:

```bash
ng serve
```

Once the server is running, open your browser and navigate to `http://localhost:4200/`. The application will automatically reload whenever you modify any of the source files.

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Running unit tests

To execute unit tests with the [Vitest](https://vitest.dev/) test runner, use the following command:

```bash
ng test
```

## Running end-to-end tests

For end-to-end (e2e) testing, run:

```bash
ng e2e
```

Angular CLI does not come with an end-to-end testing framework by default. You can choose one that suits your needs.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.
