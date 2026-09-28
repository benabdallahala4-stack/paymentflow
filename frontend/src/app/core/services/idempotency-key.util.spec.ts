import { generateIdempotencyKey } from './idempotency-key.util';

describe('generateIdempotencyKey', () => {
  it('generates a valid UUID string', () => {
    const key = generateIdempotencyKey();
    expect(key).toMatch(/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i);
  });

  it('generates a different key on each call (new submission gets a new key)', () => {
    const first = generateIdempotencyKey();
    const second = generateIdempotencyKey();
    expect(first).not.toBe(second);
  });
});
