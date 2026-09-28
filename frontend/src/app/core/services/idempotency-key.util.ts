/**
 * Generates a client-side idempotency key for a single logical submission attempt.
 *
 * Core rule (api-design.md: `Idempotency-Key` is required on POST /payments):
 * - A NEW key must be generated once per *new* form submission (a fresh transfer the
 *   user is initiating).
 * - The SAME key must be reused if the client retries that exact same attempt (e.g. a
 *   network timeout and the app automatically/manually retries sending the same request),
 *   so the backend can recognize the retry and replay the original outcome instead of
 *   creating a second transfer.
 *
 * We use the standard Web Crypto `randomUUID()` (available in all evergreen browsers and
 * Node 19+) rather than a hand-rolled random string generator.
 */
export function generateIdempotencyKey(): string {
  return crypto.randomUUID();
}
