import { MoneyPipe, minorUnitsToCurrency } from './money.pipe';

describe('minorUnitsToCurrency', () => {
  it('converts BIGINT minor units to a formatted currency string', () => {
    expect(minorUnitsToCurrency(10050, 'EUR')).toBe('€100.50');
  });

  it('handles zero', () => {
    expect(minorUnitsToCurrency(0, 'EUR')).toBe('€0.00');
  });

  it('handles negative amounts (e.g. a debit)', () => {
    expect(minorUnitsToCurrency(-500, 'EUR')).toBe('-€5.00');
  });

  it('never divides using floating point display artifacts for whole-cent amounts', () => {
    expect(minorUnitsToCurrency(199, 'EUR')).toBe('€1.99');
  });

  it('does not throw for a non-standard currency code', () => {
    expect(() => minorUnitsToCurrency(100, 'ZZZ')).not.toThrow();
  });
});

describe('MoneyPipe', () => {
  const pipe = new MoneyPipe();

  it('delegates to minorUnitsToCurrency', () => {
    expect(pipe.transform(10050, 'EUR')).toBe('€100.50');
  });

  it('returns an em dash for null/undefined', () => {
    expect(pipe.transform(null)).toBe('—');
    expect(pipe.transform(undefined)).toBe('—');
  });
});
