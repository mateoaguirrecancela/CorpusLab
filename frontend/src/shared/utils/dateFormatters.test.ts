import { describe, expect, it } from 'vitest';
import { formatDate } from '@/shared/utils/dateFormatters';

describe('formatDate', () => {
  it('returns the default fallback for null/undefined/empty values', () => {
    expect(formatDate(null, 'en', {})).toBe('-');
    expect(formatDate(undefined, 'en', {})).toBe('-');
    expect(formatDate('', 'en', {})).toBe('-');
  });

  it('returns a custom fallback when provided', () => {
    expect(formatDate(null, 'en', { fallback: 'n/a' })).toBe('n/a');
  });

  it('returns the fallback for an invalid date string', () => {
    expect(formatDate('not-a-date', 'en', { fallback: 'bad' })).toBe('bad');
  });

  it('formats a valid date using Intl.DateTimeFormat', () => {
    const formatted = formatDate('2026-01-15T00:00:00Z', 'en-US', {
      year: 'numeric',
      month: 'short',
      day: '2-digit',
    });

    expect(formatted).not.toBe('-');
    expect(formatted).toContain('2026');
  });
});
