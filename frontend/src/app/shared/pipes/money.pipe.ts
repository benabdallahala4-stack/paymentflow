import { Pipe, PipeTransform } from '@angular/core';

/**
 * Formats a BIGINT amount stored as integer minor units (e.g. cents) as a currency
 * string, e.g. `minorUnitsToCurrency(10050, 'EUR')` -> "€100.50".
 *
 * The backend never stores money as a float (see docs/database/schema.md); this is the
 * single place on the frontend that converts minor units to a display string, so no
 * component should do `amount / 100` inline.
 */
export function minorUnitsToCurrency(amountMinorUnits: number, currency = 'EUR'): string {
  if (!Number.isFinite(amountMinorUnits)) {
    return '—';
  }
  const major = amountMinorUnits / 100;
  try {
    return new Intl.NumberFormat('en-IE', {
      style: 'currency',
      currency,
      currencyDisplay: 'symbol',
    }).format(major);
  } catch {
    // Unknown/invalid currency code fallback.
    return `${major.toFixed(2)} ${currency}`;
  }
}

@Pipe({
  name: 'money',
  standalone: true,
})
export class MoneyPipe implements PipeTransform {
  transform(amountMinorUnits: number | null | undefined, currency = 'EUR'): string {
    if (amountMinorUnits === null || amountMinorUnits === undefined) {
      return '—';
    }
    return minorUnitsToCurrency(amountMinorUnits, currency);
  }
}
