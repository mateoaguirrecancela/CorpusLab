import { describe, expect, it } from 'vitest';
import { isAtLeast16YearsOld } from '@/modules/auth/utils/validation';

function isoDateYearsAgo(years: number, dayOffset = 0): string {
  const date = new Date();
  date.setFullYear(date.getFullYear() - years);
  date.setDate(date.getDate() + dayOffset);

  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

describe('isAtLeast16YearsOld', () => {
  it('accepts a birth date exactly 16 years ago', () => {
    expect(isAtLeast16YearsOld(isoDateYearsAgo(16))).toBe(true);
  });

  it('accepts a birth date older than 16 years', () => {
    expect(isAtLeast16YearsOld(isoDateYearsAgo(20))).toBe(true);
  });

  it('rejects a birth date one day short of 16 years', () => {
    expect(isAtLeast16YearsOld(isoDateYearsAgo(16, 1))).toBe(false);
  });

  it('rejects a birth date under 16 years', () => {
    expect(isAtLeast16YearsOld(isoDateYearsAgo(10))).toBe(false);
  });

  it('rejects a malformed date string', () => {
    expect(isAtLeast16YearsOld('not-a-date')).toBe(false);
    expect(isAtLeast16YearsOld('2000/01/01')).toBe(false);
    expect(isAtLeast16YearsOld('')).toBe(false);
  });

  it('rejects a calendar date that does not exist', () => {
    expect(isAtLeast16YearsOld('2000-02-30')).toBe(false);
  });
});
