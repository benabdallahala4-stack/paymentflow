# ADR-008: Cursor-Based Pagination for Transaction History

## Status
Proposed

## Context
`GET /accounts/{id}/transactions` and `GET /payments` return potentially large,
continuously-growing lists. Offset-based pagination (`LIMIT/OFFSET`) drifts under
concurrent inserts (a new row pushes every subsequent page's items back by one, causing
skipped or duplicated rows) and gets slower as the offset grows (`OFFSET` still scans and
discards rows).

## Decision
Use cursor-based (keyset) pagination: query params `after` (an opaque cursor encoding the
last-seen `(created_at, id)` pair) and `limit`, ordered by `created_at DESC, id DESC`.
The `id` tiebreaker guarantees a total order even when multiple rows share a `created_at`
timestamp (possible at millisecond granularity under load). See
`docs/architecture/api-design.md` for the endpoint shapes.

## Alternatives Considered
- **Offset/limit pagination**: rejected — drifts under concurrent writes (exactly the
  condition this system is designed to handle correctly) and degrades in performance for
  deep pages.
- **Page-number pagination**: same drift problem as offset/limit, just relabeled.
- **Cursor encoding the primary key alone**: rejected — `created_at, id` ordering is
  needed because `id` (UUID) has no natural chronological order by itself; using it alone
  as the sort key would return history in an order meaningless to a user.

## Consequences
- Every paginated list endpoint needs a composite index supporting
  `(relevant_filter_column, created_at DESC, id DESC)` — tracked as a deferred,
  evidence-driven index in `docs/database/schema.md` rather than added speculatively now.
- Cursors are opaque to clients (base64 of the tuple); the API must not let clients
  construct arbitrary offsets, simplifying the contract.
- "Jump to page N" UX is not supported by this scheme — acceptable, as no FR requires it.
