import { describe, expect, it } from 'vitest';
import {
  hasText,
  isPositiveId,
  optionalTrimmedText,
  toProjectPayload,
} from '@/modules/project/shared/utils/projectFormUtils';

describe('hasText', () => {
  it('is false for blank or whitespace-only strings', () => {
    expect(hasText('')).toBe(false);
    expect(hasText('   ')).toBe(false);
  });

  it('is true for non-blank strings', () => {
    expect(hasText(' a ')).toBe(true);
  });
});

describe('isPositiveId', () => {
  it('accepts positive finite numbers', () => {
    expect(isPositiveId(1)).toBe(true);
  });

  it('rejects zero, negative and non-finite values', () => {
    expect(isPositiveId(0)).toBe(false);
    expect(isPositiveId(-5)).toBe(false);
    expect(isPositiveId(Number.NaN)).toBe(false);
    expect(isPositiveId(Number.POSITIVE_INFINITY)).toBe(false);
  });
});

describe('optionalTrimmedText', () => {
  it('returns undefined for null, undefined or blank text', () => {
    expect(optionalTrimmedText(null)).toBeUndefined();
    expect(optionalTrimmedText(undefined)).toBeUndefined();
    expect(optionalTrimmedText('   ')).toBeUndefined();
  });

  it('returns the trimmed text otherwise', () => {
    expect(optionalTrimmedText('  hello  ')).toBe('hello');
  });
});

describe('toProjectPayload', () => {
  it('trims the name and converts a blank description to undefined', () => {
    expect(toProjectPayload({ name: ' Project ', description: '  ' })).toEqual({
      name: 'Project',
      description: undefined,
    });
  });

  it('keeps a non-blank trimmed description', () => {
    expect(toProjectPayload({ name: 'Project', description: ' desc ' })).toEqual({
      name: 'Project',
      description: 'desc',
    });
  });
});
