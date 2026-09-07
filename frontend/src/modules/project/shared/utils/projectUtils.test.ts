import { describe, expect, it } from 'vitest';
import {
  completionBadgeClassName,
  completionColor,
  formatDate,
  isNerCompatibleDataset,
  isNerCompatibleDatasetItem,
  normalizeCompletionPercentage,
} from '@/modules/project/shared/utils/projectUtils';

describe('formatDate', () => {
  it('returns a dash for an invalid date', () => {
    expect(formatDate('not-a-date')).toBe('-');
  });

  it('formats a valid date', () => {
    expect(formatDate('2026-01-15T00:00:00Z')).not.toBe('-');
  });
});

describe('normalizeCompletionPercentage', () => {
  it('clamps to the 0-100 range and rounds', () => {
    expect(normalizeCompletionPercentage(-5)).toBe(0);
    expect(normalizeCompletionPercentage(150)).toBe(100);
    expect(normalizeCompletionPercentage(49.6)).toBe(50);
  });

  it('returns 0 for non-finite values', () => {
    expect(normalizeCompletionPercentage(Number.NaN)).toBe(0);
  });
});

describe('completionBadgeClassName', () => {
  it('picks the tier matching the percentage', () => {
    expect(completionBadgeClassName(100)).toContain('emerald');
    expect(completionBadgeClassName(50)).toContain('amber');
    expect(completionBadgeClassName(10)).toContain('sky');
  });
});

describe('completionColor', () => {
  it('picks the tier matching the percentage', () => {
    expect(completionColor(100).bar).toContain('emerald');
    expect(completionColor(75).bar).toContain('amber');
    expect(completionColor(0).bar).toContain('sky');
  });
});

describe('isNerCompatibleDatasetItem', () => {
  it('accepts plain text and json mime types', () => {
    expect(isNerCompatibleDatasetItem({ mimeType: 'text/plain', fileName: 'a.dat' })).toBe(true);
    expect(isNerCompatibleDatasetItem({ mimeType: 'application/json', fileName: 'a.dat' })).toBe(
      true,
    );
    expect(
      isNerCompatibleDatasetItem({ mimeType: 'application/vnd.api+json', fileName: 'a' }),
    ).toBe(true);
  });

  it('accepts supported extensions regardless of mime type', () => {
    expect(
      isNerCompatibleDatasetItem({ mimeType: 'application/octet-stream', fileName: 'a.csv' }),
    ).toBe(true);
  });

  it('rejects unsupported mime types and extensions', () => {
    expect(isNerCompatibleDatasetItem({ mimeType: 'image/png', fileName: 'a.png' })).toBe(false);
  });
});

describe('isNerCompatibleDataset', () => {
  it('is false for an empty dataset', () => {
    expect(isNerCompatibleDataset([])).toBe(false);
  });

  it('is true only when every item is compatible', () => {
    expect(
      isNerCompatibleDataset([
        { mimeType: 'text/plain', fileName: 'a.txt' },
        { mimeType: 'application/json', fileName: 'b.json' },
      ]),
    ).toBe(true);

    expect(
      isNerCompatibleDataset([
        { mimeType: 'text/plain', fileName: 'a.txt' },
        { mimeType: 'image/png', fileName: 'b.png' },
      ]),
    ).toBe(false);
  });
});
