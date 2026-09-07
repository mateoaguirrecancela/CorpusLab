import { describe, expect, it } from 'vitest';
import { type TFunction } from 'i18next';
import {
  formatProfileBirth,
  formatProfileGender,
  formatProfileValue,
} from '@/modules/auth/profile/utils/profileFormatters';

const t = ((key: string) => key) as TFunction;

describe('formatProfileValue', () => {
  it('returns a dash for null', () => {
    expect(formatProfileValue(null)).toBe('-');
  });

  it('returns a dash for blank strings', () => {
    expect(formatProfileValue('   ')).toBe('-');
  });

  it('returns the value unchanged when non-blank', () => {
    expect(formatProfileValue('Coruna')).toBe('Coruna');
  });
});

describe('formatProfileGender', () => {
  it('returns a dash for null or empty', () => {
    expect(formatProfileGender(null, t)).toBe('-');
    expect(formatProfileGender('', t)).toBe('-');
  });

  it('translates a known gender', () => {
    expect(formatProfileGender('MALE', t)).toBe('auth.gender.male');
  });

  it('falls back to the raw value for an unknown gender', () => {
    expect(formatProfileGender('OTHER_VALUE', t)).toBe('OTHER_VALUE');
  });
});

describe('formatProfileBirth', () => {
  it('returns a dash for null', () => {
    expect(formatProfileBirth(null, 'en')).toBe('-');
  });

  it('falls back to the raw value for an invalid date', () => {
    expect(formatProfileBirth('not-a-date', 'en')).toBe('not-a-date');
  });

  it('formats a valid date', () => {
    expect(formatProfileBirth('2000-01-01', 'en')).not.toBe('2000-01-01');
  });
});
